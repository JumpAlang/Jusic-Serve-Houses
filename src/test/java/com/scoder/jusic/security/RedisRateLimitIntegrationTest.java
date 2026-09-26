package com.scoder.jusic.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in real Redis verification. It does not start Spring or read application
 * YAML (which could leak secrets through debug logging), and writes only known
 * random jusic:test keys.
 */
@EnabledIfSystemProperty(named = "jusic.redis.integration", matches = "true")
class RedisRateLimitIntegrationTest {

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RedisRateLimitService rateLimitService;
    private static String prefix;

    @BeforeAll
    static void connect() {
        RedisStandaloneConfiguration redis = new RedisStandaloneConfiguration();
        redis.setHostName(setting("jusic.test.redis.host", "JUSIC_TEST_REDIS_HOST", "127.0.0.1"));
        redis.setPort(Integer.parseInt(
                setting("jusic.test.redis.port", "JUSIC_TEST_REDIS_PORT", "6379")));
        redis.setDatabase(Integer.parseInt(
                setting("jusic.test.redis.database", "JUSIC_TEST_REDIS_DATABASE", "0")));
        String password = setting("jusic.test.redis.password", "JUSIC_TEST_REDIS_PASSWORD", "");
        if (password != null && !password.isEmpty()) {
            redis.setPassword(password);
        }
        connectionFactory = new LettuceConnectionFactory(redis);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        rateLimitService = new RedisRateLimitService(redisTemplate);
        prefix = "jusic:test:rate-limit:" + UUID.randomUUID();
    }

    @AfterAll
    static void disconnect() {
        if (redisTemplate != null && prefix != null) {
            redisTemplate.delete(Arrays.asList(
                    prefix + ":{concurrent}:session",
                    prefix + ":{atomic}:session",
                    prefix + ":{atomic}:room"
            ));
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void fixedWindowAllowsExactlyTheConfiguredConcurrentCount() throws Exception {
        String key = prefix + ":{concurrent}:session";
        RateLimitRule rule = new RateLimitRule(key, 7, 30_000L);
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < 64; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return rateLimitService.tryAcquire(Collections.singletonList(rule)).isAllowed();
                }));
            }
            start.countDown();
            int allowed = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    allowed++;
                }
            }
            assertEquals(7, allowed);
            assertTrue(redisTemplate.getExpire(key) > 0L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void deniedMultiScopeRequestDoesNotPartiallyConsumeOtherBuckets() {
        String sessionKey = prefix + ":{atomic}:session";
        String roomKey = prefix + ":{atomic}:room";
        List<RateLimitRule> rules = Arrays.asList(
                new RateLimitRule(sessionKey, 100, 30_000L),
                new RateLimitRule(roomKey, 1, 30_000L)
        );

        assertTrue(rateLimitService.tryAcquire(rules).isAllowed());
        assertFalse(rateLimitService.tryAcquire(rules).isAllowed());

        assertEquals("1", redisTemplate.opsForValue().get(sessionKey));
        assertEquals("1", redisTemplate.opsForValue().get(roomKey));
    }

    private static String setting(String systemProperty,
                                  String environmentVariable,
                                  String defaultValue) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isEmpty()) {
            value = System.getenv(environmentVariable);
        }
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}
