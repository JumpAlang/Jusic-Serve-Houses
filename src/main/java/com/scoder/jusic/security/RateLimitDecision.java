package com.scoder.jusic.security;

public class RateLimitDecision {

    private static final RateLimitDecision ALLOWED = new RateLimitDecision(true, 0L);

    private final boolean allowed;
    private final long retryAfterMs;

    private RateLimitDecision(boolean allowed, long retryAfterMs) {
        this.allowed = allowed;
        this.retryAfterMs = Math.max(0L, retryAfterMs);
    }

    public static RateLimitDecision allowed() {
        return ALLOWED;
    }

    public static RateLimitDecision denied(long retryAfterMs) {
        return new RateLimitDecision(false, retryAfterMs);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }
}
