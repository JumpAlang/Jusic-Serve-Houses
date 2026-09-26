package com.scoder.jusic.security;

import com.scoder.jusic.configuration.AbuseProtectionProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbuseProtectionServiceTest {

    private static final Pattern REDIS_SLOT = Pattern.compile("\\{([0-9a-f]{24})\\}");
    private static final Pattern OPAQUE_KEY = Pattern.compile(
            "jusic:guard:v1:\\{[0-9a-f]{24}\\}:chat:(session|ip|room):[0-9a-f]{24}"
    );

    @Test
    void checkCombinesSessionIpAndRoomRulesUsingConfiguredPolicy() {
        CapturingRateLimitService limiter = new CapturingRateLimitService();
        AbuseProtectionService service = new AbuseProtectionService(new AbuseProtectionProperties(), limiter);

        RateLimitDecision decision = service.check(
                AbuseAction.CHAT,
                "session-sensitive-value",
                "203.0.113.88",
                "private-room-42"
        );

        assertTrue(decision.isAllowed());
        assertEquals(3, limiter.rules.size());
        assertRule(limiter.rules.get(0), ":chat:session:", 1, 2_000L);
        assertRule(limiter.rules.get(1), ":chat:ip:", 60, 60_000L);
        assertRule(limiter.rules.get(2), ":chat:room:", 100, 1_000L);
    }

    @Test
    void generatedKeysAreOpaqueAndShareTheRoomHashSlot() {
        CapturingRateLimitService limiter = new CapturingRateLimitService();
        AbuseProtectionService service = new AbuseProtectionService(new AbuseProtectionProperties(), limiter);
        String sessionId = "session-sensitive-value";
        String clientIp = "203.0.113.88";
        String houseId = "private-room-42";

        service.check(AbuseAction.CHAT, sessionId, clientIp, houseId);

        String expectedSlot = null;
        for (RateLimitRule rule : limiter.rules) {
            String key = rule.getKey();
            assertTrue(OPAQUE_KEY.matcher(key).matches(), "unexpected key format: " + key);
            assertFalse(key.contains(sessionId));
            assertFalse(key.contains(clientIp));
            assertFalse(key.contains(houseId));

            Matcher slotMatcher = REDIS_SLOT.matcher(key);
            assertTrue(slotMatcher.find());
            if (expectedSlot == null) {
                expectedSlot = slotMatcher.group(1);
            } else {
                assertEquals(expectedSlot, slotMatcher.group(1), "all atomic Lua keys must share a Redis slot");
            }
        }
    }

    @Test
    void redisFailureUsesPerActionFailOpenAndFailClosedPolicy() {
        ThrowingRateLimitService limiter = new ThrowingRateLimitService();
        AbuseProtectionService service = new AbuseProtectionService(new AbuseProtectionProperties(), limiter);

        RateLimitDecision chat = service.check(AbuseAction.CHAT, "s", "203.0.113.1", "h");
        RateLimitDecision pick = service.check(AbuseAction.PICK_MUSIC, "s", "203.0.113.1", "h");

        assertTrue(chat.isAllowed(), "chat is configured fail-open so Redis loss does not disconnect everyone");
        assertFalse(pick.isAllowed(), "high-cost point-song calls are configured fail-closed");
        assertEquals(1_000L, pick.getRetryAfterMs());
        assertEquals(2, limiter.calls);
    }

    @Test
    void disabledProtectionSkipsTheLimiterEntirely() {
        AbuseProtectionProperties properties = new AbuseProtectionProperties();
        properties.setEnabled(false);
        ThrowingRateLimitService limiter = new ThrowingRateLimitService();
        AbuseProtectionService service = new AbuseProtectionService(properties, limiter);

        assertTrue(service.check(AbuseAction.PICK_MUSIC, "s", "ip", "h").isAllowed());
        assertEquals(0, limiter.calls);
    }

    private static void assertRule(RateLimitRule rule,
                                   String expectedScope,
                                   int expectedMaximum,
                                   long expectedWindowMs) {
        assertTrue(rule.getKey().contains(expectedScope));
        assertEquals(expectedMaximum, rule.getMaxRequests());
        assertEquals(expectedWindowMs, rule.getWindowMs());
    }

    private static final class CapturingRateLimitService implements RateLimitService {
        private List<RateLimitRule> rules = new ArrayList<>();

        @Override
        public RateLimitDecision tryAcquire(List<RateLimitRule> rules) {
            this.rules = new ArrayList<>(rules);
            return RateLimitDecision.allowed();
        }
    }

    private static final class ThrowingRateLimitService implements RateLimitService {
        private int calls;

        @Override
        public RateLimitDecision tryAcquire(List<RateLimitRule> rules) {
            calls++;
            throw new IllegalStateException("Redis unavailable for unit test");
        }
    }
}
