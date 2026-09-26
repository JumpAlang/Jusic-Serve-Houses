package com.scoder.jusic.job;

import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.RoomMusicLocks;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MusicJobBackoffTest {

    @Test
    void repeatedProviderFailuresAreExponentiallyLimitedOverTenSeconds() {
        TestFixture fixture = new TestFixture("failing-room");
        when(fixture.musicService.musicSwitch("failing-room"))
                .thenThrow(new IllegalStateException("provider unavailable"));

        for (int tick = 0; tick <= 20; tick++) {
            fixture.now.set(tick * 500L);
            ReflectionTestUtils.invokeMethod(
                    fixture.job, "sendIfSufficient");
        }

        verify(fixture.musicService, times(4))
                .musicSwitch("failing-room");
        assertEquals(1, retryStates(fixture.job).size());
        fixture.job.shutdown();
    }

    @Test
    void successfulPlaybackClearsBackoffAndAllowsAnImmediateNextAttempt() {
        TestFixture fixture = new TestFixture("recovering-room");
        Music recovered = new Music();
        recovered.setDuration(1_000L);
        Music next = new Music();
        next.setDuration(1_000L);
        when(fixture.musicService.musicSwitch("recovering-room"))
                .thenThrow(new IllegalStateException("first call fails"))
                .thenReturn(recovered)
                .thenReturn(next);
        when(fixture.musicService.getPickList("recovering-room"))
                .thenReturn(new LinkedList<>());

        fixture.now.set(0L);
        assertTrue(fixture.job.submitHouseTask(fixture.house));
        assertFalse(fixture.job.submitHouseTask(fixture.house));

        fixture.now.set(1_000L);
        assertTrue(fixture.job.submitHouseTask(fixture.house));
        assertTrue(retryStates(fixture.job).isEmpty());
        assertTrue(fixture.job.submitHouseTask(fixture.house));

        verify(fixture.musicService, times(3))
                .musicSwitch("recovering-room");
        fixture.job.shutdown();
    }

    @Test
    void playbackStateCommitFailureAlsoEntersBackoff() {
        TestFixture fixture = new TestFixture("commit-failure-room");
        Music music = new Music();
        music.setDuration(1_000L);
        when(fixture.musicService.musicSwitch("commit-failure-room"))
                .thenReturn(music);
        doThrow(new IllegalStateException("redis write failed"))
                .when(fixture.configRepository)
                .setLastMusicPushTimeAndDuration(
                        any(Long.class), eq(1_000L),
                        eq("commit-failure-room"));

        fixture.now.set(0L);
        assertTrue(fixture.job.submitHouseTask(fixture.house));
        fixture.now.set(500L);
        assertFalse(fixture.job.submitHouseTask(fixture.house));
        fixture.now.set(1_000L);
        assertTrue(fixture.job.submitHouseTask(fixture.house));

        verify(fixture.musicService, times(2))
                .musicSwitch("commit-failure-room");
        fixture.job.shutdown();
    }

    @Test
    void retryStateIsRemovedAfterRoomDisappears() {
        TestFixture fixture = new TestFixture("removed-room");
        when(fixture.musicService.musicSwitch("removed-room"))
                .thenThrow(new IllegalStateException("provider unavailable"));

        fixture.now.set(0L);
        assertTrue(fixture.job.submitHouseTask(fixture.house));
        assertEquals(1, retryStates(fixture.job).size());

        fixture.houses.clear();
        ReflectionTestUtils.invokeMethod(fixture.job, "sendIfSufficient");

        assertTrue(retryStates(fixture.job).isEmpty());
        fixture.job.shutdown();
    }

    @Test
    void aNewlyQueuedSongWakesImmediatelyButPreservesFailureCount() {
        TestFixture fixture = new TestFixture("new-song-room");
        when(fixture.musicService.musicSwitch("new-song-room"))
                .thenThrow(new IllegalStateException("provider unavailable"));
        fixture.now.set(0L);
        assertTrue(fixture.job.submitHouseTask(fixture.house));
        fixture.now.set(100L);
        assertFalse(fixture.job.submitHouseTask(fixture.house));

        fixture.job.wakeHouse("new-song-room");

        assertTrue(fixture.job.submitHouseTask(fixture.house));
        fixture.now.set(1_100L);
        assertFalse(fixture.job.submitHouseTask(fixture.house));
        fixture.now.set(2_100L);
        assertTrue(fixture.job.submitHouseTask(fixture.house));
        verify(fixture.musicService, times(3)).musicSwitch("new-song-room");
        fixture.job.shutdown();
    }

    @Test
    void inFlightOldFailureCannotOverwriteANewerWake() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        TestFixture fixture = new TestFixture("wake-race-room", worker);
        CountDownLatch providerStarted = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        when(fixture.musicService.musicSwitch("wake-race-room"))
                .thenAnswer(invocation -> {
                    if (calls.incrementAndGet() == 1) {
                        providerStarted.countDown();
                        if (!releaseProvider.await(2L, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(
                                    "provider test latch timed out");
                        }
                    }
                    throw new IllegalStateException("provider unavailable");
                });

        try {
            fixture.now.set(0L);
            assertTrue(fixture.job.submitHouseTask(fixture.house));
            assertTrue(providerStarted.await(2L, TimeUnit.SECONDS));

            fixture.now.set(100L);
            fixture.job.wakeHouse("wake-race-room");
            releaseProvider.countDown();
            assertTrue(waitForEmptyHouseTasks(fixture.job, 2_000L));

            assertTrue(fixture.job.submitHouseTask(fixture.house));
            assertTrue(waitForEmptyHouseTasks(fixture.job, 2_000L));
            assertEquals(2, calls.get());
        } finally {
            releaseProvider.countDown();
            fixture.job.shutdown();
        }
    }

    @Test
    void roomDeletedWhileProviderIsInFlightCannotRecreateRetryState()
            throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        TestFixture fixture = new TestFixture("deleted-in-flight-room", worker);
        CountDownLatch providerStarted = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        when(fixture.musicService.musicSwitch("deleted-in-flight-room"))
                .thenAnswer(invocation -> {
                    providerStarted.countDown();
                    if (!releaseProvider.await(2L, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "provider test latch timed out");
                    }
                    throw new IllegalStateException("provider unavailable");
                });

        try {
            assertTrue(fixture.job.submitHouseTask(fixture.house));
            assertTrue(providerStarted.await(2L, TimeUnit.SECONDS));
            fixture.houses.clear();
            releaseProvider.countDown();
            assertTrue(waitForEmptyHouseTasks(fixture.job, 2_000L));

            assertTrue(retryStates(fixture.job).isEmpty());
        } finally {
            releaseProvider.countDown();
            fixture.job.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> retryStates(MusicJob job) {
        return (Map<String, ?>) ReflectionTestUtils.getField(
                job, "houseRetryMap");
    }

    private static boolean waitForEmptyHouseTasks(MusicJob job,
                                                   long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            Map<?, ?> tasks = (Map<?, ?>) ReflectionTestUtils.getField(
                    job, "houseTaskMap");
            if (tasks != null && tasks.isEmpty()) {
                return true;
            }
            Thread.sleep(10L);
        }
        return false;
    }

    private static final class TestFixture {
        private final AtomicLong now = new AtomicLong();
        private final CopyOnWriteArrayList<House> houses =
                new CopyOnWriteArrayList<>();
        private final House house;
        private final MusicService musicService = mock(MusicService.class);
        private final ConfigRepository configRepository =
                mock(ConfigRepository.class);
        private final MusicJob job;

        private TestFixture(String houseId) {
            this(houseId, new DirectExecutorService());
        }

        private TestFixture(String houseId, ExecutorService executorService) {
            house = new House();
            house.setId(houseId);
            house.setName(houseId);
            houses.add(house);

            JusicProperties properties = mock(JusicProperties.class);
            when(properties.hasSessions(anyString())).thenReturn(true);
            job = new MusicJob(properties, executorService,
                    now::get, ignored -> 0L);

            HouseContainer houseContainer = mock(HouseContainer.class);
            when(houseContainer.getHouses()).thenReturn(houses);
            when(houseContainer.get(anyString())).thenAnswer(invocation -> {
                String requestedId = invocation.getArgument(0);
                for (House candidate : houses) {
                    if (candidate.getId().equals(requestedId)) {
                        return candidate;
                    }
                }
                return null;
            });
            when(configRepository.getPushSwitch(anyString())).thenReturn(true);

            ReflectionTestUtils.setField(job, "houseContainer", houseContainer);
            ReflectionTestUtils.setField(job, "houseOperationLocks",
                    new HouseOperationLocks());
            ReflectionTestUtils.setField(job, "roomMusicLocks",
                    new RoomMusicLocks());
            ReflectionTestUtils.setField(job, "configRepository",
                    configRepository);
            ReflectionTestUtils.setField(job, "musicPlayingRepository",
                    mock(MusicPlayingRepository.class));
            ReflectionTestUtils.setField(job, "musicVoteRepository",
                    mock(MusicVoteRepository.class));
            ReflectionTestUtils.setField(job, "sessionService",
                    mock(SessionService.class));
            ReflectionTestUtils.setField(job, "musicService", musicService);
        }
    }

    private static final class DirectExecutorService
            extends AbstractExecutorService {
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
