package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicEnvironment;
import com.scoder.jusic.configuration.PresenceProperties;
import com.scoder.jusic.configuration.RedisConfiguration;
import com.scoder.jusic.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Opt-in serialization/script check against local Redis. It uses database 15
 * and unique keys, then removes only those keys in a finally block.
 */
@EnabledIfSystemProperty(named = "jusic.redis.integration", matches = "true")
class SessionPresenceStoreRedisIntegrationTest {

    @Test
    @SuppressWarnings("rawtypes")
    void atomicStoreRenewAndTwoStagePrunePreserveLegacySerialization()
            throws InterruptedException {
        RedisStandaloneConfiguration standalone =
                new RedisStandaloneConfiguration("127.0.0.1", 6379);
        standalone.setDatabase(15);
        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(standalone);
        factory.afterPropertiesSet();
        RedisTemplate template = new RedisConfiguration().redisTemplate(factory);
        template.afterPropertiesSet();

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String houseId = "integration/" + suffix;
        String sessionId = "session:" + suffix;
        String sessionHashPrefix = "codex:presence:legacy:" + suffix + ":";
        String hashKey = sessionHashPrefix + houseId;
        String orphanHouseId = "orphan-" + suffix;
        PresenceProperties properties = new PresenceProperties();
        properties.setNamespace("codex:presence:marker:" + suffix);
        properties.setTtl(60_000L);
        properties.setHeartbeatDelay(100L);
        properties.setCleanupDelay(50L);
        properties.setCandidateGrace(500L);
        properties.setCandidateTtl(3_000L);
        JusicEnvironment environment = mock(JusicEnvironment.class);
        when(environment.getServerPort()).thenReturn(49_317);
        SessionPresenceStore store =
                new SessionPresenceStore(template, properties, environment);
        String markerKey = store.markerKey(houseId, sessionId);
        String candidateKey = store.candidateKey(houseId, sessionId);
        com.scoder.jusic.configuration.JusicProperties.RedisKeys redisKeys =
                new com.scoder.jusic.configuration.JusicProperties.RedisKeys(
                        environment);
        String orphanHashKey = redisKeys.getSessionHash() + orphanHouseId;

        try {
            User user = new User();
            user.setHouseId(houseId);
            user.setSessionId(sessionId);
            user.setName("integration-user");

            store.storeSession(houseId, sessionId, hashKey, user);

            User stored = (User) template.opsForHash().get(hashKey, sessionId);
            assertNotNull(stored);
            assertEquals("integration-user", stored.getName());
            assertTrue(store.trackedHouseIds().contains(houseId));
            assertTrue(Boolean.TRUE.equals(template.hasKey(markerKey)));
            assertTrue(template.getExpire(markerKey) > 0L);
            assertTrue(Boolean.FALSE.equals(template.hasKey(candidateKey)));

            template.delete(markerKey);
            // A malformed internal candidate is fail-safe: cleanup resets its
            // firstSeen from Redis TIME instead of deleting the HASH.
            RedisSerializer keySerializer = template.getKeySerializer();
            template.execute((RedisCallback<Boolean>) connection ->
                    connection.set(
                            keySerializer.serialize(candidateKey),
                            "not-a-time".getBytes(StandardCharsets.US_ASCII)));
            // First valid observation physically keeps the old HASH during
            // grace. Candidate state is never reported online.
            assertEquals(0L, store.pruneHashFieldsIfExpired(
                    houseId, hashKey, Collections.singleton(sessionId)));
            assertNotNull(template.opsForHash().get(hashKey, sessionId));
            assertTrue(Boolean.TRUE.equals(template.hasKey(candidateKey)));
            assertTrue(!store.isAlive(houseId, sessionId));
            assertTrue(template.getExpire(
                    candidateKey, TimeUnit.MILLISECONDS) > 500L);
            // Repeated cleanup inside grace cannot accelerate physical delete.
            assertEquals(0L, store.pruneHashFieldsIfExpired(
                    houseId, hashKey, Collections.singleton(sessionId)));
            assertNotNull(template.opsForHash().get(hashKey, sessionId));

            Thread.sleep(650L);
            assertEquals(1L, store.pruneHashFieldsIfExpired(
                    houseId, hashKey, Collections.singleton(sessionId)));
            assertNull(template.opsForHash().get(hashKey, sessionId));
            assertTrue(Boolean.FALSE.equals(template.hasKey(candidateKey)));
            // If GC wins first, the next heartbeat reports the missing HASH
            // and cannot recreate a marker-only online user.
            assertTrue(store.refresh(sessionHashPrefix,
                    Collections.singletonMap(
                            houseId, Collections.singletonList(sessionId)))
                    .containsKey(houseId));
            assertTrue(Boolean.FALSE.equals(template.hasKey(markerKey)));

            // Store is one atomic state transition and clears any prior GC
            // candidate while publishing HASH + marker.
            store.storeSession(houseId, sessionId, hashKey, user);
            assertEquals(0L, store.pruneHashFieldsIfExpired(
                    houseId, hashKey, Collections.singleton(sessionId)));
            assertNotNull(template.opsForHash().get(hashKey, sessionId));
            assertTrue(Boolean.FALSE.equals(template.hasKey(candidateKey)));

            template.delete(markerKey);
            assertEquals(0L, store.pruneHashFieldsIfExpired(
                    houseId, hashKey, Collections.singleton(sessionId)));
            assertTrue(Boolean.TRUE.equals(template.hasKey(candidateKey)));
            // If heartbeat wins during grace, it renews the marker and clears
            // the candidate; a later marker loss starts a fresh grace window.
            assertTrue(store.refresh(sessionHashPrefix,
                    Collections.singletonMap(
                            houseId, Collections.singletonList(sessionId)))
                    .isEmpty());
            assertTrue(Boolean.FALSE.equals(template.hasKey(candidateKey)));
            assertTrue(Boolean.TRUE.equals(template.hasKey(markerKey)));
            template.delete(markerKey);
            assertEquals(0L, store.pruneHashFieldsIfExpired(
                    houseId, hashKey, Collections.singleton(sessionId)));
            assertTrue(Boolean.TRUE.equals(template.hasKey(candidateKey)));
            user.setName("restored-during-grace");
            store.storeSession(houseId, sessionId, hashKey, user);
            assertTrue(Boolean.FALSE.equals(template.hasKey(candidateKey)));
            assertTrue(Boolean.TRUE.equals(template.hasKey(markerKey)));
            assertEquals("restored-during-grace",
                    ((User) template.opsForHash().get(
                            hashKey, sessionId)).getName());

            // Heartbeat against a missing HASH clears both transient keys.
            assertTrue(store.refresh("codex:presence:missing:",
                    Collections.singletonMap(
                            houseId, Collections.singletonList(sessionId)))
                    .containsKey(houseId));
            assertTrue(Boolean.FALSE.equals(template.hasKey(markerKey)));
            assertTrue(Boolean.FALSE.equals(template.hasKey(candidateKey)));
            assertTrue(store.refresh(sessionHashPrefix,
                    Collections.singletonMap(
                            houseId, Collections.singletonList(sessionId)))
                    .isEmpty());

            // Atomic normal close also clears an already pending candidate.
            template.delete(markerKey);
            assertEquals(0L, store.pruneHashFieldsIfExpired(
                    houseId, hashKey, Collections.singleton(sessionId)));
            assertTrue(Boolean.TRUE.equals(template.hasKey(candidateKey)));
            assertEquals(1L, store.removeSession(
                    houseId, sessionId, hashKey));
            assertNull(template.opsForHash().get(hashKey, sessionId));
            assertTrue(Boolean.FALSE.equals(template.hasKey(markerKey)));
            assertTrue(Boolean.FALSE.equals(template.hasKey(candidateKey)));
            // A cleanup snapshot that raced after normal close must not
            // recreate a candidate for a HASH field that is already gone.
            assertEquals(0L, store.pruneHashFieldsIfExpired(
                    houseId, hashKey, Collections.singleton(sessionId)));
            assertTrue(Boolean.FALSE.equals(template.hasKey(candidateKey)));
            // If store wins after remove, all three values are present in the
            // new state rather than a HASH-without-marker split brain.
            store.storeSession(houseId, sessionId, hashKey, user);
            assertNotNull(template.opsForHash().get(hashKey, sessionId));
            assertTrue(Boolean.TRUE.equals(template.hasKey(markerKey)));

            template.opsForHash().put(orphanHashKey, sessionId, user);
            SessionRepositoryImpl repository = new SessionRepositoryImpl();
            ReflectionTestUtils.setField(repository, "redisKeys", redisKeys);
            ReflectionTestUtils.setField(repository, "redisTemplate", template);
            ReflectionTestUtils.setField(repository, "presenceStore", store);
            assertTrue(repository.findPresenceHouseIds().contains(orphanHouseId));
        } finally {
            store.untrackHouse(houseId);
            store.untrackHouse(orphanHouseId);
            template.delete(markerKey);
            template.delete(candidateKey);
            template.delete(hashKey);
            template.delete(orphanHashKey);
            factory.destroy();
        }
    }
}
