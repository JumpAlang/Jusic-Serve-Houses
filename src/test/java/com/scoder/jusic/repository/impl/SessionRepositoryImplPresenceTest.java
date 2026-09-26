package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionRepositoryImplPresenceTest {

    private static final String HOUSE_ID = "room-1";
    private static final String HASH_KEY = "jusic_session_8888_" + HOUSE_ID;

    private RedisTemplate redisTemplate;
    private HashOperations hashOperations;
    private SessionPresenceStore presenceStore;
    private SessionRepositoryImpl repository;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        presenceStore = mock(SessionPresenceStore.class);
        JusicProperties.RedisKeys redisKeys = mock(JusicProperties.RedisKeys.class);
        when(redisKeys.getSessionHash()).thenReturn("jusic_session_8888_");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(presenceStore.isEnabled()).thenReturn(true);

        repository = new SessionRepositoryImpl();
        ReflectionTestUtils.setField(repository, "redisKeys", redisKeys);
        ReflectionTestUtils.setField(repository, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(repository, "presenceStore", presenceStore);
    }

    @Test
    void getByIdHidesGhostWithoutAdvancingGarbageCollection() {
        User ghost = user("ghost");
        when(hashOperations.get(HASH_KEY, "ghost")).thenReturn(ghost);
        when(presenceStore.isAlive(HOUSE_ID, "ghost")).thenReturn(false);

        assertNull(repository.getSession("ghost", HOUSE_ID));

        verify(presenceStore, org.mockito.Mockito.never())
                .pruneHashFieldsIfExpired(
                        HOUSE_ID,
                        HASH_KEY,
                        Collections.singletonList("ghost"));
    }

    @Test
    void getByIdReturnsUserWithLiveLease() {
        User user = user("session-a");
        when(hashOperations.get(HASH_KEY, "session-a")).thenReturn(user);
        when(presenceStore.isAlive(HOUSE_ID, "session-a"))
                .thenReturn(true);

        assertSame(user, repository.getSession("session-a", HOUSE_ID));
    }

    @Test
    void getRoomUsersDoesNotAdvanceGarbageCollection() {
        User active = user("active");
        User ghost = user("ghost");
        when(hashOperations.values(HASH_KEY))
                .thenReturn(Arrays.asList(active, ghost));
        when(presenceStore.alive(
                HOUSE_ID, Arrays.asList("active", "ghost")))
                .thenReturn(Arrays.asList(true, false));
        List<User> users = repository.getSession(HOUSE_ID);

        assertEquals(Collections.singletonList(active), users);
        verify(presenceStore, org.mockito.Mockito.never())
                .pruneHashFieldsIfExpired(
                        HOUSE_ID,
                        HASH_KEY,
                        Collections.singletonList("ghost"));
    }

    @Test
    void roomListDoesNotTreatGraceCandidateAsOnline() {
        User active = user("active");
        User revived = user("revived");
        when(hashOperations.values(HASH_KEY))
                .thenReturn(Arrays.asList(active, revived));
        when(presenceStore.alive(
                HOUSE_ID, Arrays.asList("active", "revived")))
                .thenReturn(Arrays.asList(true, false));

        assertEquals(Collections.singletonList(active),
                repository.getSession(HOUSE_ID));
    }

    @Test
    void sizeCountsOnlyUsersWithLivePresenceMarkers() {
        User active = user("active");
        User ghost = user("ghost");
        when(hashOperations.values(HASH_KEY))
                .thenReturn(Arrays.asList(active, ghost));
        when(presenceStore.alive(
                HOUSE_ID, Arrays.asList("active", "ghost")))
                .thenReturn(Arrays.asList(true, false));

        assertEquals(1L, repository.size(HOUSE_ID));
    }

    @Test
    void storedSizeUsesRawHashLengthWithoutPresenceFiltering() {
        when(hashOperations.size(HASH_KEY)).thenReturn(2L);

        assertEquals(2L, repository.storedSize(HOUSE_ID));

        verify(hashOperations).size(HASH_KEY);
    }

    @Test
    void cleanupAdvancesCandidatesButSkipsProtectedOpenSessions() {
        when(hashOperations.keys(HASH_KEY)).thenReturn(
                new LinkedHashSet<>(Arrays.asList(
                        "stale-a", "open", "stale-b")));
        when(presenceStore.pruneHashFieldsIfExpired(
                HOUSE_ID,
                HASH_KEY,
                Arrays.asList("stale-a", "stale-b")))
                .thenReturn(1L);

        assertEquals(1L, repository.pruneExpiredPresence(
                HOUSE_ID, Collections.singleton("open")));

        verify(presenceStore).pruneHashFieldsIfExpired(
                HOUSE_ID,
                HASH_KEY,
                Arrays.asList("stale-a", "stale-b"));
    }

    @Test
    void trackPresenceHouseAddsDurableIndexMember() {
        repository.trackPresenceHouse(HOUSE_ID);

        verify(presenceStore).trackHouses(
                Collections.singleton(HOUSE_ID));
    }

    @Test
    void setSessionPublishesLeaseAndHashInOneAtomicStore() {
        User user = user("session-1");

        repository.setSession(user, HOUSE_ID);

        verify(presenceStore).storeSession(
                HOUSE_ID, "session-1", HASH_KEY, user);
    }

    @Test
    void failedAtomicStorePropagatesWithoutPublishingPartialState() {
        User user = user("session-1");
        RuntimeException failure = new RuntimeException("redis write failed");
        doThrow(failure).when(presenceStore)
                .storeSession(HOUSE_ID, "session-1", HASH_KEY, user);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> repository.setSession(user, HOUSE_ID));

        assertSame(failure, thrown);
        verify(presenceStore).storeSession(
                HOUSE_ID, "session-1", HASH_KEY, user);
    }

    @Test
    void removeSessionAtomicallyDeletesHashMarkerAndCandidate() {
        when(presenceStore.removeSession(
                HOUSE_ID, "session-1", HASH_KEY)).thenReturn(1L);

        assertEquals(1L, repository.removeSession("session-1", HOUSE_ID));

        verify(presenceStore).removeSession(
                HOUSE_ID, "session-1", HASH_KEY);
        verify(hashOperations, org.mockito.Mockito.never())
                .delete(HASH_KEY, "session-1");
    }

    private User user(String sessionId) {
        User user = new User();
        user.setSessionId(sessionId);
        user.setHouseId(HOUSE_ID);
        return user;
    }
}
