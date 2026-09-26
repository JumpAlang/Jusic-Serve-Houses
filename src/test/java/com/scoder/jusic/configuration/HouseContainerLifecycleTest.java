package com.scoder.jusic.configuration;

import com.scoder.jusic.model.House;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.repository.HousesRespository;
import com.scoder.jusic.repository.MusicBlackRepository;
import com.scoder.jusic.repository.MusicDefaultRepository;
import com.scoder.jusic.repository.MusicPickRepository;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.repository.RetainKeyRepository;
import com.scoder.jusic.repository.SessionBlackRepository;
import com.scoder.jusic.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseContainerLifecycleTest {

    private static final String HOUSE_ID = "temporary-room";

    @Mock
    private ConfigRepository configRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private MusicDefaultRepository musicDefaultRepository;
    @Mock
    private MusicPlayingRepository musicPlayingRepository;
    @Mock
    private MusicPickRepository musicPickRepository;
    @Mock
    private MusicVoteRepository musicVoteRepository;
    @Mock
    private MusicBlackRepository musicBlackRepository;
    @Mock
    private SessionBlackRepository sessionBlackRepository;
    @Mock
    private HousesRespository housesRespository;
    @Mock
    private RetainKeyRepository retainKeyRepository;

    private JusicProperties properties;
    private HouseOperationLocks operationLocks;
    private HouseContainer houseContainer;

    @BeforeEach
    void setUp() {
        properties = new JusicProperties();
        operationLocks = new HouseOperationLocks();
        houseContainer = new HouseContainer(
                configRepository,
                sessionRepository,
                musicDefaultRepository,
                musicPlayingRepository,
                musicPickRepository,
                musicVoteRepository,
                musicBlackRepository,
                sessionBlackRepository,
                properties,
                housesRespository,
                retainKeyRepository,
                operationLocks);
    }

    @Test
    void concurrentConditionalDestroyCleansOneTemporaryRoomOnlyOnce() throws Exception {
        setRooms(temporaryHouse(HOUSE_ID));
        properties.getSessions(HOUSE_ID);
        when(sessionRepository.size(HOUSE_ID)).thenReturn(0L);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> {
                start.await();
                return houseContainer.destroyIfTemporaryAndEmpty(HOUSE_ID);
            });
            Future<Boolean> second = executor.submit(() -> {
                start.await();
                return houseContainer.destroyIfTemporaryAndEmpty(HOUSE_ID);
            });
            start.countDown();

            int successfulCalls = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, successfulCalls);
        } finally {
            executor.shutdownNow();
        }

        assertNull(houseContainer.get(HOUSE_ID));
        assertFalse(properties.getSession().get().containsKey(HOUSE_ID));
        verify(sessionRepository).size(HOUSE_ID);
        verifyCompleteCleanup(1);
    }

    @Test
    void conditionalDestroyRechecksLocalSessionsAfterAcquiringLifecycleLock() throws Exception {
        House house = temporaryHouse(HOUSE_ID);
        setRooms(house);
        ReentrantLock lock = operationLocks.getLock(HOUSE_ID);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result;
        CountDownLatch destroyStarted = new CountDownLatch(1);
        lock.lock();
        try {
            result = executor.submit(() -> {
                destroyStarted.countDown();
                return houseContainer.destroyIfTemporaryAndEmpty(HOUSE_ID);
            });
            assertTrue(destroyStarted.await(2, java.util.concurrent.TimeUnit.SECONDS));
            properties.getSessions(HOUSE_ID).put(
                    "session-1", org.mockito.Mockito.mock(WebSocketSession.class));
        } finally {
            lock.unlock();
        }

        try {
            assertFalse(result.get());
        } finally {
            executor.shutdownNow();
        }

        assertSame(house, houseContainer.get(HOUSE_ID));
        verify(sessionRepository, never()).size(HOUSE_ID);
        verifyNoCleanup();
    }

    @Test
    void conditionalDestroyRechecksDistributedSessionsAfterAcquiringLifecycleLock()
            throws Exception {
        House house = temporaryHouse(HOUSE_ID);
        setRooms(house);
        AtomicBoolean anotherNodeIsPresent = new AtomicBoolean(false);
        when(sessionRepository.size(HOUSE_ID))
                .thenAnswer(ignored -> anotherNodeIsPresent.get() ? 1L : 0L);
        ReentrantLock lock = operationLocks.getLock(HOUSE_ID);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> result;
        CountDownLatch destroyStarted = new CountDownLatch(1);
        lock.lock();
        try {
            result = executor.submit(() -> {
                destroyStarted.countDown();
                return houseContainer.destroyIfTemporaryAndEmpty(HOUSE_ID);
            });
            assertTrue(destroyStarted.await(2, java.util.concurrent.TimeUnit.SECONDS));
            anotherNodeIsPresent.set(true);
        } finally {
            lock.unlock();
        }

        try {
            assertFalse(result.get());
        } finally {
            executor.shutdownNow();
        }

        assertSame(house, houseContainer.get(HOUSE_ID));
        verify(sessionRepository).size(HOUSE_ID);
        verifyNoCleanup();
    }

    @Test
    void conditionalDestroyFailsClosedWhenDistributedOccupancyCannotBeRead() {
        House house = temporaryHouse(HOUSE_ID);
        setRooms(house);
        when(sessionRepository.size(HOUSE_ID))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertFalse(houseContainer.destroyIfTemporaryAndEmpty(HOUSE_ID));

        assertSame(house, houseContainer.get(HOUSE_ID));
        verify(sessionRepository).size(HOUSE_ID);
        verifyNoCleanup();
    }

    @Test
    void conditionalDestroyRejectsDefaultAndPermanentRooms() {
        House defaultHouse = permanentHouse(JusicProperties.HOUSE_DEFAULT_ID);
        House permanentHouse = permanentHouse("permanent-room");
        setRooms(defaultHouse, permanentHouse);

        assertFalse(houseContainer.destroyIfTemporaryAndEmpty(defaultHouse.getId()));
        assertFalse(houseContainer.destroyIfTemporaryAndEmpty(permanentHouse.getId()));

        assertSame(defaultHouse, houseContainer.get(defaultHouse.getId()));
        assertSame(permanentHouse, houseContainer.get(permanentHouse.getId()));
        verify(sessionRepository, never()).size(
                org.mockito.ArgumentMatchers.anyString());
        verifyNoCleanup();
    }

    @Test
    void failedCleanupKeepsRoomDiscoverableAndCanBeRetried() {
        House house = temporaryHouse(HOUSE_ID);
        setRooms(house);
        when(sessionRepository.size(HOUSE_ID)).thenReturn(0L);
        when(sessionRepository.destroy(HOUSE_ID))
                .thenThrow(new IllegalStateException("redis unavailable"))
                .thenReturn(true);

        assertFalse(houseContainer.destroyIfTemporaryAndEmpty(HOUSE_ID));
        assertSame(house, houseContainer.get(HOUSE_ID));

        assertTrue(houseContainer.destroyIfTemporaryAndEmpty(HOUSE_ID));
        assertNull(houseContainer.get(HOUSE_ID));
        verify(sessionRepository, times(2)).size(HOUSE_ID);
        verify(sessionRepository, times(2)).destroy(HOUSE_ID);
        verify(configRepository).destroy(HOUSE_ID);
        verify(musicPlayingRepository).destroy(HOUSE_ID);
        verify(musicPickRepository).destroy(HOUSE_ID);
        verify(musicVoteRepository).destroy(HOUSE_ID);
        verify(musicBlackRepository).destroy(HOUSE_ID);
        verify(sessionBlackRepository).destroy(HOUSE_ID);
        verify(musicDefaultRepository).destroy(HOUSE_ID);
        verify(sessionRepository).untrackPresenceHouse(HOUSE_ID);
    }

    @Test
    void explicitDestroyRejectsAnOccupiedPermanentRoomWithoutDroppingItsIndexes() {
        House house = permanentHouse(HOUSE_ID);
        setRooms(house);
        WebSocketSession session = org.mockito.Mockito.mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        properties.getSession().register(session, HOUSE_ID, "ip-a");

        assertFalse(houseContainer.destroy(HOUSE_ID));

        assertSame(house, houseContainer.get(HOUSE_ID));
        assertSame(session, properties.getSessionIfPresent(HOUSE_ID, "session-1"));
        assertEquals(1, properties.getSession().getNodeCount());
        verifyNoCleanup();
    }

    @Test
    void unknownRoomCleanupWaitsForPendingRawSessionFields() {
        setRooms();
        when(sessionRepository.storedSize(HOUSE_ID)).thenReturn(1L);

        assertFalse(houseContainer.destroyRedisIfUnknownAndEmpty(HOUSE_ID));

        verify(sessionRepository).storedSize(HOUSE_ID);
        verifyNoCleanup();
    }

    @Test
    void unknownRoomCleanupDeletesOnlyAfterRawSessionHashIsEmpty() {
        setRooms();
        when(sessionRepository.storedSize(HOUSE_ID)).thenReturn(0L);

        assertTrue(houseContainer.destroyRedisIfUnknownAndEmpty(HOUSE_ID));

        verify(sessionRepository).storedSize(HOUSE_ID);
        verifyCompleteCleanup(1);
    }

    @Test
    void repeatedAddForTheSameHouseIsIdempotent() {
        setRooms();
        House first = temporaryHouse(HOUSE_ID);
        House duplicate = temporaryHouse(HOUSE_ID);
        duplicate.setName("duplicate");

        houseContainer.add(first);
        houseContainer.add(duplicate);

        assertEquals(1, houseContainer.size());
        assertSame(first, houseContainer.get(HOUSE_ID));
        org.mockito.InOrder creationOrder = inOrder(
                sessionRepository, configRepository);
        creationOrder.verify(sessionRepository).trackPresenceHouse(HOUSE_ID);
        creationOrder.verify(configRepository).initialize(HOUSE_ID);
    }

    @Test
    void replacingTheRoomSnapshotRebuildsTheIdIndex() {
        House oldRoom = temporaryHouse("old-room");
        House newRoom = permanentHouse("new-room");
        setRooms(oldRoom);
        assertSame(oldRoom, houseContainer.get(oldRoom.getId()));

        setRooms(newRoom);

        assertNull(houseContainer.get(oldRoom.getId()));
        assertSame(newRoom, houseContainer.get(newRoom.getId()));
        assertTrue(houseContainer.contains(newRoom.getId()));
        assertFalse(houseContainer.contains(oldRoom.getId()));
    }

    @Test
    void legacyShutdownCleanupCanRemoveTemporaryRoomsFromCopyOnWriteRegistry() {
        House temporary = temporaryHouse(HOUSE_ID);
        House permanent = permanentHouse("permanent-room");
        setRooms(temporary, permanent);

        assertDoesNotThrow(() -> houseContainer.destroy());

        assertNull(houseContainer.get(HOUSE_ID));
        assertSame(permanent, houseContainer.get(permanent.getId()));
        verify(housesRespository).destroy(houseContainer.getHouses());
    }

    private void setRooms(House... houses) {
        houseContainer.setHouses(new CopyOnWriteArrayList<>(Arrays.asList(houses)));
    }

    private House temporaryHouse(String id) {
        House house = new House();
        house.setId(id);
        house.setEnableStatus(false);
        return house;
    }

    private House permanentHouse(String id) {
        House house = new House();
        house.setId(id);
        house.setEnableStatus(true);
        return house;
    }

    private void verifyCompleteCleanup(int times) {
        verify(sessionRepository, times(times)).destroy(HOUSE_ID);
        verify(configRepository, times(times)).destroy(HOUSE_ID);
        verify(musicPlayingRepository, times(times)).destroy(HOUSE_ID);
        verify(musicPickRepository, times(times)).destroy(HOUSE_ID);
        verify(musicVoteRepository, times(times)).destroy(HOUSE_ID);
        verify(musicBlackRepository, times(times)).destroy(HOUSE_ID);
        verify(sessionBlackRepository, times(times)).destroy(HOUSE_ID);
        verify(musicDefaultRepository, times(times)).destroy(HOUSE_ID);
        verify(sessionRepository, times(times)).untrackPresenceHouse(HOUSE_ID);
    }

    private void verifyNoCleanup() {
        verify(sessionRepository, never()).destroy(org.mockito.ArgumentMatchers.anyString());
        verify(sessionRepository, never()).untrackPresenceHouse(
                org.mockito.ArgumentMatchers.anyString());
        verify(configRepository, never()).destroy(org.mockito.ArgumentMatchers.anyString());
        verify(musicPlayingRepository, never()).destroy(org.mockito.ArgumentMatchers.anyString());
        verify(musicPickRepository, never()).destroy(org.mockito.ArgumentMatchers.anyString());
        verify(musicVoteRepository, never()).destroy(org.mockito.ArgumentMatchers.anyString());
        verify(musicBlackRepository, never()).destroy(org.mockito.ArgumentMatchers.anyString());
        verify(sessionBlackRepository, never()).destroy(org.mockito.ArgumentMatchers.anyString());
        verify(musicDefaultRepository, never()).destroy(org.mockito.ArgumentMatchers.anyString());
        verifyNoInteractions(housesRespository, retainKeyRepository);
    }
}
