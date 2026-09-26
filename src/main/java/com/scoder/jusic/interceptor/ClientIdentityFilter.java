package com.scoder.jusic.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Supplies a stable, opaque browser id without creating an HTTP session.
 */
@Component
public class ClientIdentityFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "JUSIC_CLIENT_ID";
    public static final String CLIENT_ID_ATTRIBUTE =
            ClientIdentityFilter.class.getName() + ".clientId";

    private static final int MAX_AGE_SECONDS = 365 * 24 * 60 * 60;
    private static final Pattern VALID_ID = Pattern.compile("[a-f0-9]{32}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String clientId = findClientId(request.getCookies());
        if (clientId == null) {
            clientId = UUID.randomUUID().toString().replace("-", "");
            response.addHeader("Set-Cookie", cookieHeader(clientId, request.isSecure()));
        }
        request.setAttribute(CLIENT_ID_ATTRIBUTE, clientId);
        filterChain.doFilter(request, response);
    }

    private String findClientId(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())
                    && cookie.getValue() != null
                    && VALID_ID.matcher(cookie.getValue()).matches()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String cookieHeader(String clientId, boolean secure) {
        return COOKIE_NAME + "=" + clientId
                + "; Path=/; Max-Age=" + MAX_AGE_SECONDS
                + "; HttpOnly; SameSite=Lax"
                + (secure ? "; Secure" : "");
    }
}
