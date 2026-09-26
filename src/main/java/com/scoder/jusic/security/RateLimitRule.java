package com.scoder.jusic.security;

public class RateLimitRule {

    private final String key;
    private final int maxRequests;
    private final long windowMs;

    public RateLimitRule(String key, int maxRequests, long windowMs) {
        this.key = key;
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public String getKey() {
        return key;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public long getWindowMs() {
        return windowMs;
    }
}
