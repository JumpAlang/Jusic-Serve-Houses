package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicEnvironment;
import com.scoder.jusic.configuration.PresenceProperties;
import com.scoder.jusic.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionPresenceStoreTest {

    @Test
    @SuppressWarnings("rawtypes")
    void markerKeyUsesEncodedNamespacedParts() {
        RedisTemplate redisTemplate = mock(RedisTemplate.class);
        PresenceProperties properties = properties();
        properties.setNamespace("presence:test");
        JusicEnvironment environment = mock(JusicEnvironment.class);
        when(environment.getServerPort()).thenReturn(8_888);
        SessionPresenceStore store = new SessionPresenceStore(
                redisTemplate, properties, environment);

        String expectedKey = "presence:test:8888:cm9vbS9h:c2Vzc2lvbjpi";
        assertEquals(expectedKey, store.markerKey("room/a", "session:b"));
        assertEquals("presence:test:8888:gc:cm9vbS9h:c2Vzc2lvbjpi",
                store.candidateKey("room/a", "session:b"));
        assertNotEquals(store.markerKey("gc", "session"),
                store.candidateKey("gc", "session"));
        assertNotEquals(store.markerKey("rooms", "session"),
                store.roomIndexKey());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void refreshAtomicallyRenewsOnlySessionsWhoseHashStillExists() {
        RedisTemplate redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.getKeySerializer())
                .thenReturn(new StringRedisSerializer());
        when(redisTemplate.getHashKeySerializer())
                .thenReturn(new JdkSerializationRedisSerializer());
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn(java.util.Arrays.asList(
                        "1".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                        "0".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        PresenceProperties properties = properties();
        JusicEnvironment environment = mock(JusicEnvironment.class);
        when(environment.getServerPort()).thenReturn(8_888);
        SessionPresenceStore store = new SessionPresenceStore(
                redisTemplate, properties, environment);
        Map<String, java.util.Collection<String>> sessions = new LinkedHashMap<>();
        sessions.put("room", java.util.Arrays.asList("session-a", "session-b"));

        Map<String, List<String>> missing =
                store.refresh("jusic_session_8888_", sessions);

        assertEquals(Collections.singletonList("session-b"),
                missing.get("room"));
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void refreshUsesBoundedBatchesAndMergesMissingSessionsInOrder() {
        RedisTemplate redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.getKeySerializer())
                .thenReturn(new StringRedisSerializer());
        when(redisTemplate.getHashKeySerializer())
                .thenReturn(new JdkSerializationRedisSerializer());

        List<List<Long>> batchResults = Arrays.asList(
                Arrays.asList(1L, 0L),
                Arrays.asList(0L, 0L),
                Collections.singletonList(1L));
        AtomicInteger resultIndex = new AtomicInteger();
        List<Integer> keyCounts = new ArrayList<>();
        List<Integer> argumentCounts = new ArrayList<>();
        RedisConnection connection = (RedisConnection) Proxy.newProxyInstance(
                RedisConnection.class.getClassLoader(),
                new Class<?>[]{RedisConnection.class},
                (proxy, method, arguments) -> {
                    if ("eval".equals(method.getName())) {
                        keyCounts.add((Integer) arguments[2]);
                        byte[][] keysAndArguments = (byte[][]) arguments[3];
                        argumentCounts.add(keysAndArguments.length);
                        return batchResults.get(resultIndex.getAndIncrement());
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenAnswer(invocation -> {
                    RedisCallback callback = invocation.getArgument(0);
                    return callback.doInRedis(connection);
                });

        PresenceProperties properties = properties();
        properties.setRefreshBatchSize(2);
        JusicEnvironment environment = mock(JusicEnvironment.class);
        when(environment.getServerPort()).thenReturn(8_888);
        SessionPresenceStore store = new SessionPresenceStore(
                redisTemplate, properties, environment);
        Map<String, java.util.Collection<String>> sessions =
                new LinkedHashMap<>();
        sessions.put("room-a", Arrays.asList("a1", "a2", "a3"));
        sessions.put("room-b", Arrays.asList("b1", "b2"));

        Map<String, List<String>> missing =
                store.refresh("jusic_session_8888_", sessions);

        assertEquals(Arrays.asList("a2", "a3"), missing.get("room-a"));
        assertEquals(Collections.singletonList("b1"),
                missing.get("room-b"));
        assertEquals(Arrays.asList(6, 6, 3), keyCounts);
        assertEquals(Arrays.asList(10, 10, 6), argumentCounts);
        verify(redisTemplate, times(3))
                .execute(any(RedisCallback.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void storeAndConditionalPruneUseAtomicRedisScripts() {
        RedisTemplate redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.getKeySerializer())
                .thenReturn(new StringRedisSerializer());
        when(redisTemplate.getHashKeySerializer())
                .thenReturn(new JdkSerializationRedisSerializer());
        when(redisTemplate.getHashValueSerializer())
                .thenReturn(new JdkSerializationRedisSerializer());
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(1L);
        JusicEnvironment environment = mock(JusicEnvironment.class);
        when(environment.getServerPort()).thenReturn(8_888);
        SessionPresenceStore store = new SessionPresenceStore(
                redisTemplate, properties(), environment);
        User user = new User();
        user.setSessionId("session-a");

        store.storeSession("room", "session-a", "legacy-room", user);
        long removed = store.pruneHashFieldsIfExpired(
                "room", "legacy-room", Collections.singleton("session-a"));

        assertEquals(1L, removed);
        verify(redisTemplate, times(2)).execute(any(RedisCallback.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void visibilityReadsOnlyLiveMarkersWithoutWriting() {
        RedisTemplate redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.getKeySerializer())
                .thenReturn(new StringRedisSerializer());
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn(java.util.Arrays.asList(1L, 0L));
        JusicEnvironment environment = mock(JusicEnvironment.class);
        when(environment.getServerPort()).thenReturn(8_888);
        SessionPresenceStore store = new SessionPresenceStore(
                redisTemplate, properties(), environment);

        assertEquals(java.util.Arrays.asList(true, false),
                store.alive("room",
                        java.util.Arrays.asList("grace", "expired")));

        // One read-only Lua evaluation observes leases atomically; unlike
        // cleanup, this path never creates or advances a GC candidate.
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void atomicRemoveReturnsHashDeletionResult() {
        RedisTemplate redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.getKeySerializer())
                .thenReturn(new StringRedisSerializer());
        when(redisTemplate.getHashKeySerializer())
                .thenReturn(new JdkSerializationRedisSerializer());
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(1L);
        JusicEnvironment environment = mock(JusicEnvironment.class);
        when(environment.getServerPort()).thenReturn(8_888);
        SessionPresenceStore store = new SessionPresenceStore(
                redisTemplate, properties(), environment);

        assertEquals(1L, store.removeSession(
                "room", "session-a", "legacy-room"));
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void disabledPresenceIsBackwardCompatibleAndDoesNotTouchRedis() {
        RedisTemplate redisTemplate = mock(RedisTemplate.class);
        HashOperations hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.delete("legacy-room", "session"))
                .thenReturn(1L);
        PresenceProperties properties = properties();
        properties.setEnabled(false);
        SessionPresenceStore store = new SessionPresenceStore(
                redisTemplate, properties, mock(JusicEnvironment.class));

        assertTrue(store.refresh("jusic_session_8888_",
                Collections.singletonMap(
                        "room", Collections.singletonList("session")))
                .isEmpty());
        assertTrue(store.isAlive("room", "session"));
        store.remove("room", "session");
        assertEquals(1L, store.removeSession(
                "room", "session", "legacy-room"));

        // Disabled mode keeps the original HASH operation and does not use
        // any additive marker/candidate keys.
        verify(hashOperations).delete("legacy-room", "session");
    }

    @Test
    void leaseMustOutliveTwoHeartbeatIntervals() {
        PresenceProperties properties = properties();
        properties.setHeartbeatDelay(15_000L);
        properties.setTtl(30_000L);

        assertThrows(IllegalStateException.class, properties::requiredTtl);
    }

    private PresenceProperties properties() {
        PresenceProperties properties = new PresenceProperties();
        properties.setEnabled(true);
        properties.setNamespace("jusic:presence:v1");
        properties.setTtl(60_000L);
        properties.setHeartbeatDelay(15_000L);
        return properties;
    }
}
