package com.scoder.jusic.security;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RedisRateLimitServiceTest {

    @Test
    void emptyRuleSetDoesNotTouchRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisRateLimitService service = new RedisRateLimitService(redisTemplate);

        assertTrue(service.tryAcquire(Collections.emptyList()).isAllowed());

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void serializesEveryArgumentAsStringAndParsesLongResult() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doReturn(Arrays.asList(0L, 2_500L)).when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any(), any(), any(), any());
        RedisRateLimitService service = new RedisRateLimitService(redisTemplate);
        List<RateLimitRule> rules = Arrays.asList(
                new RateLimitRule("key-session", 1, 2_000L),
                new RateLimitRule("key-room", 100, 1_000L)
        );

        RateLimitDecision decision = service.tryAcquire(rules);

        assertFalse(decision.isAllowed());
        assertEquals(2_500L, decision.getRetryAfterMs());
        ArgumentCaptor<List> keys = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), keys.capture(),
                eq("1"), eq("2000"), eq("100"), eq("1000"));
        assertEquals(Arrays.asList("key-session", "key-room"), keys.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void malformedScriptResultFailsClosedAtTheServiceBoundary() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doReturn(Collections.singletonList(1L)).when(redisTemplate)
                .execute(any(RedisScript.class), anyList(), any(), any());
        RedisRateLimitService service = new RedisRateLimitService(redisTemplate);

        assertThrows(IllegalStateException.class, () -> service.tryAcquire(
                Collections.singletonList(new RateLimitRule("key", 1, 1_000L))));
    }
}
