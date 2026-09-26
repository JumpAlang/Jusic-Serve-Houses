package com.scoder.jusic.security;

import com.scoder.jusic.configuration.AbuseProtectionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class AbuseProtectionService {

    private static final long FAILURE_LOG_INTERVAL_MS = 30_000L;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final AbuseProtectionProperties properties;
    private final RateLimitService rateLimitService;
    private final AtomicLong lastFailureLog = new AtomicLong(0L);

    public AbuseProtectionService(AbuseProtectionProperties properties,
                                  RateLimitService rateLimitService) {
        this.properties = properties;
        this.rateLimitService = rateLimitService;
    }

    public RateLimitDecision check(AbuseAction action,
                                   String sessionId,
                                   String clientIp,
                                   String houseId) {
        if (!properties.isEnabled()) {
            return RateLimitDecision.allowed();
        }

        AbuseProtectionProperties.ActionLimit policy = properties.policy(action);
        List<RateLimitRule> rules = buildRules(action, policy, sessionId, clientIp, houseId);
        if (rules.isEmpty()) {
            return RateLimitDecision.allowed();
        }

        try {
            return rateLimitService.tryAcquire(rules);
        } catch (RuntimeException ex) {
            logRedisFailure(action, ex);
            return policy.isFailOpen()
                    ? RateLimitDecision.allowed()
                    : RateLimitDecision.denied(1_000L);
        }
    }

    List<RateLimitRule> buildRules(AbuseAction action,
                                   AbuseProtectionProperties.ActionLimit policy,
                                   String sessionId,
                                   String clientIp,
                                   String houseId) {
        List<RateLimitRule> rules = new ArrayList<>(3);
        String slotBasis = hasText(houseId) ? houseId : (hasText(clientIp) ? clientIp : sessionId);
        String slotDigest = hasText(slotBasis) ? digest(slotBasis) : null;

        addRule(rules, action, "session", sessionId, slotDigest, policy.getSession());
        addRule(rules, action, "ip", clientIp, slotDigest, policy.getIp());
        addRule(rules, action, "room", houseId, slotDigest, policy.getRoom());
        return rules;
    }

    private void addRule(List<RateLimitRule> rules,
                         AbuseAction action,
                         String scope,
                         String subject,
                         String slotDigest,
                         AbuseProtectionProperties.ScopeLimit limit) {
        if (!hasText(subject) || !hasText(slotDigest) || limit == null || !limit.isEnabled()) {
            return;
        }
        String key = properties.getNamespace()
                + ":{" + slotDigest + "}:"
                + action.name().toLowerCase(Locale.ROOT)
                + ":" + scope
                + ":" + digest(subject);
        rules.add(new RateLimitRule(key, limit.getMaxRequests(), limit.getWindowMs()));
    }

    private void logRedisFailure(AbuseAction action, RuntimeException ex) {
        long now = System.currentTimeMillis();
        long previous = lastFailureLog.get();
        if (now - previous >= FAILURE_LOG_INTERVAL_MS
                && lastFailureLog.compareAndSet(previous, now)) {
            log.warn("abuse protection Redis check failed, action: {}, fail policy applied: {}",
                    action, ex.getMessage());
        }
    }

    private String digest(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            char[] result = new char[24];
            for (int i = 0; i < 12; i++) {
                int current = bytes[i] & 0xff;
                result[i * 2] = HEX[current >>> 4];
                result[i * 2 + 1] = HEX[current & 0x0f];
            }
            return new String(result);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
