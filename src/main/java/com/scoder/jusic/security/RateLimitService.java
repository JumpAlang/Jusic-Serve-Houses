package com.scoder.jusic.security;

import java.util.List;

public interface RateLimitService {

    RateLimitDecision tryAcquire(List<RateLimitRule> rules);
}
