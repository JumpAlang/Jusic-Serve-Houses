package com.scoder.jusic.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.security.AbuseAction;
import com.scoder.jusic.security.AbuseProtectionService;
import com.scoder.jusic.security.ClientIpResolver;
import com.scoder.jusic.security.RateLimitDecision;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Keeps the legacy HTTP response body shape while rejecting abusive requests
 * before they reach controllers and external providers.
 */
@Component
public class HttpAbuseProtectionInterceptor implements HandlerInterceptor {

    private final AbuseProtectionService abuseProtectionService;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    public HttpAbuseProtectionInterceptor(AbuseProtectionService abuseProtectionService,
                                          ClientIpResolver clientIpResolver,
                                          ObjectMapper objectMapper) {
        this.abuseProtectionService = abuseProtectionService;
        this.clientIpResolver = clientIpResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        AbuseAction action = AbuseAction.fromHttpPath(applicationPath(request));
        if (action == null) {
            return true;
        }

        HttpSession session = request.getSession(false);
        String sessionId = session == null ? null : session.getId();
        String clientIp = clientIpResolver.resolve(request);
        // Do not consume the JSON body merely to derive a room id. HTTP ingress
        // is guarded by session/IP here; room-specific checks stay in commands.
        RateLimitDecision decision = abuseProtectionService.check(action, sessionId, clientIp, null);
        if (decision.isAllowed()) {
            return true;
        }

        long retrySeconds = Math.max(1L, (decision.getRetryAfterMs() + 999L) / 1_000L);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(retrySeconds));
        objectMapper.writeValue(response.getWriter(),
                Response.failure((Object) null, "请求过于频繁，请在" + retrySeconds + "秒后重试"));
        return false;
    }

    private String applicationPath(HttpServletRequest request) {
        Object bestMatchingPattern = request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (bestMatchingPattern instanceof String) {
            return normalizePath((String) bestMatchingPattern);
        }
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (uri == null || contextPath == null || contextPath.isEmpty()) {
            return normalizePath(uri);
        }
        return normalizePath(uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri);
    }

    /** Mirrors Spring's trailing-slash and matrix-parameter path handling. */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        StringBuilder normalized = new StringBuilder(path.length());
        boolean matrixParameter = false;
        for (int index = 0; index < path.length(); index++) {
            char character = path.charAt(index);
            if (character == ';') {
                matrixParameter = true;
            } else if (character == '/') {
                matrixParameter = false;
                normalized.append(character);
            } else if (!matrixParameter) {
                normalized.append(character);
            }
        }
        while (normalized.length() > 1
                && normalized.charAt(normalized.length() - 1) == '/') {
            normalized.deleteCharAt(normalized.length() - 1);
        }
        return normalized.toString();
    }
}
