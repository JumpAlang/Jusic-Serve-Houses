package com.scoder.jusic.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Atomically checks every supplied fixed-window bucket and increments them only
 * when all buckets allow the request.
 */
@Service
public class RedisRateLimitService implements RateLimitService {

    private static final DefaultRedisScript<List> ACQUIRE_SCRIPT;

    static {
        String script =
                "local deniedTtl = 0 " +
                "for i, key in ipairs(KEYS) do " +
                "  local maxRequests = tonumber(ARGV[(i - 1) * 2 + 1]) " +
                "  local windowMs = tonumber(ARGV[(i - 1) * 2 + 2]) " +
                "  local current = tonumber(redis.call('GET', key) or '0') " +
                "  if current + 1 > maxRequests then " +
                "    local ttl = redis.call('PTTL', key) " +
                "    if ttl < 1 then " +
                "      redis.call('PEXPIRE', key, windowMs) " +
                "      ttl = windowMs " +
                "    end " +
                "    if ttl > deniedTtl then deniedTtl = ttl end " +
                "  end " +
                "end " +
                "if deniedTtl > 0 then return {0, deniedTtl} end " +
                "for i, key in ipairs(KEYS) do " +
                "  local windowMs = tonumber(ARGV[(i - 1) * 2 + 2]) " +
                "  local current = redis.call('INCR', key) " +
                "  if current == 1 or redis.call('PTTL', key) < 0 then " +
                "    redis.call('PEXPIRE', key, windowMs) " +
                "  end " +
                "end " +
                "return {1, 0}";
        ACQUIRE_SCRIPT = new DefaultRedisScript<>();
        ACQUIRE_SCRIPT.setScriptText(script);
        ACQUIRE_SCRIPT.setResultType(List.class);
    }

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RateLimitDecision tryAcquire(List<RateLimitRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return RateLimitDecision.allowed();
        }

        List<String> keys = new ArrayList<>(rules.size());
        List<String> args = new ArrayList<>(rules.size() * 2);
        for (RateLimitRule rule : rules) {
            keys.add(rule.getKey());
            args.add(String.valueOf(rule.getMaxRequests()));
            args.add(String.valueOf(rule.getWindowMs()));
        }

        List result = redisTemplate.execute(ACQUIRE_SCRIPT, keys, args.toArray());
        if (result == null || result.size() < 2) {
            throw new IllegalStateException("Redis rate-limit script returned an invalid result");
        }

        long allowed = number(result.get(0));
        long retryAfterMs = number(result.get(1));
        return allowed == 1L
                ? RateLimitDecision.allowed()
                : RateLimitDecision.denied(retryAfterMs);
    }

    private long number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
