package com.scoder.jusic.job;

import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.ProviderIsolationProperties;
import com.scoder.jusic.configuration.RoomMusicLocks;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.security.ProviderCallExecutor;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MusicJobConcurrencyTest {

    @Test
    void schedulerDoesNotReevaluateVoteTriggerWhileHouseTaskIsRunning()
            throws Exception {
        JusicProperties properties = mock(JusicProperties.class);
        when(properties.hasSessions("vote-room")).thenReturn(true);
        when(properties.getSessionCount("vote-room")).thenReturn(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        MusicJob job = new MusicJob(properties, executor);
        House house = house("vote-room");
        HouseContainer houseContainer = mock(HouseContainer.class);
        when(houseContainer.getHouses()).thenReturn(
                new CopyOnWriteArrayList<>(
                        java.util.Collections.singletonList(house)));
        when(houseContainer.get("vote-room")).thenReturn(house);
        ConfigRepository configRepository = mock(ConfigRepository.class);
        when(configRepository.getLastMusicPushTime("vote-room"))
                .thenReturn(System.currentTimeMillis());
        when(configRepository.getLastMusicDuration("vote-room"))
                .thenReturn(60_000L);
        when(configRepository.getVoteRate("vote-room")).thenReturn(0.5F);
        when(configRepository.getPushSwitch("vote-room")).thenReturn(true);
        MusicPlayingRepository playingRepository =
                mock(MusicPlayingRepository.class);
        when(playingRepository.getPlaying("vote-room"))
                .thenReturn(new Music());
        MusicVoteRepository voteRepository = mock(MusicVoteRepository.class);
        when(voteRepository.size("vote-room")).thenReturn(1L);
        MusicService musicService = mock(MusicService.class);
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch releaseTask = new CountDownLatch(1);
        doAnswer(invocation -> {
            taskStarted.countDown();
            assertTrue(releaseTask.await(2, TimeUnit.SECONDS));
            return null;
        }).when(musicService).musicSwitch("vote-room");
        ReflectionTestUtils.setField(job, "houseContainer", houseContainer);
        ReflectionTestUtils.setField(job, "configRepository", configRepository);
        ReflectionTestUtils.setField(job, "musicPlayingRepository",
                playingRepository);
        ReflectionTestUtils.setField(job, "musicVoteRepository",
                voteRepository);
        ReflectionTestUtils.setField(job, "musicService", musicService);
        ReflectionTestUtils.setField(job, "houseOperationLocks",
                new HouseOperationLocks());
        ReflectionTestUtils.setField(job, "roomMusicLocks",
                new RoomMusicLocks());

        try {
            ReflectionTestUtils.invokeMethod(job, "sendIfSufficient");
            assertTrue(taskStarted.await(2, TimeUnit.SECONDS));
            ReflectionTestUtils.invokeMethod(job, "sendIfSufficient");

            verify(configRepository).setPushSwitch(true, "vote-room");
        } finally {
            releaseTask.countDown();
            job.shutdown();
        }
    }

    @Test
    void tinyHouseLimitStillCreatesABoundedAbortPolicyExecutor() {
        JusicProperties properties = new JusicProperties();
        properties.setHouseSize(1);
        MusicJob job = new MusicJob(properties);
        try {
            ThreadPoolExecutor executor = (ThreadPoolExecutor)
                    ReflectionTestUtils.getField(job, "executorService");
            assertTrue(executor.getQueue().remainingCapacity() >= 1);
            assertTrue(executor.getRejectedExecutionHandler()
                    instanceof ThreadPoolExecutor.AbortPolicy);
            assertTrue(executor.getMaximumPoolSize() <= 32);
            ThreadPoolExecutor prefetchExecutor = (ThreadPoolExecutor)
                    ReflectionTestUtils.getField(job, "prefetchExecutor");
            assertNotSame(executor, prefetchExecutor);
            assertTrue(prefetchExecutor.getQueue().remainingCapacity() >= 1);
            assertTrue(prefetchExecutor.getRejectedExecutionHandler()
                    instanceof ThreadPoolExecutor.AbortPolicy);
        } finally {
            job.shutdown();
        }
    }

    @Test
    void rejectionNeverRunsMusicWorkOnTheSchedulerCallerAndReleasesMarker() {
        RejectingExecutor executor = new RejectingExecutor();
        MusicJob job = new MusicJob(new JusicProperties(), executor);
        House house = new House();
        house.setId("full-room");
        HouseContainer houseContainer = mock(HouseContainer.class);
        when(houseContainer.get("full-room")).thenReturn(house);
        ReflectionTestUtils.setField(job, "houseContainer", houseContainer);
        ReflectionTestUtils.setField(job, "houseOperationLocks",
                new HouseOperationLocks());

        assertFalse(job.submitHouseTask(house));
        assertFalse(job.submitHouseTask(house),
                "queue saturation should be backed off instead of retried every 500 ms");

        Map<?, ?> markers = (Map<?, ?>) ReflectionTestUtils.getField(job, "houseTaskMap");
        assertTrue(markers.isEmpty());
        Map<?, ?> retryStates = (Map<?, ?>) ReflectionTestUtils.getField(
                job, "houseRetryMap");
        assertTrue(retryStates.containsKey("full-room"));
        job.shutdown();
    }

    @Test
    void prefetchTargetsNextQueuedSongInsteadOfCurrentPlayingSong()
            throws Exception {
        MusicJob job = new MusicJob(new JusicProperties());
        ConfigRepository configRepository = mock(ConfigRepository.class);
        MusicService musicService = mock(MusicService.class);
        ReflectionTestUtils.setField(job, "configRepository", configRepository);
        ReflectionTestUtils.setField(job, "musicService", musicService);
        long now = System.currentTimeMillis();
        when(configRepository.getLastMusicPushTime("prefetch-room"))
                .thenReturn(now);
        when(configRepository.getLastMusicDuration("prefetch-room"))
                .thenReturn(10_000L);
        Music playing = new Music();
        playing.setId("playing");
        playing.setSource("wy");
        Music next = new Music();
        next.setId("next");
        next.setSource("wy");
        LinkedList<Music> snapshot = new LinkedList<>();
        snapshot.add(playing);
        snapshot.add(next);
        when(musicService.getPickList("prefetch-room"))
                .thenReturn(snapshot);
        CountDownLatch prefetched = new CountDownLatch(1);
        doAnswer(invocation -> {
            prefetched.countDown();
            return null;
        }).when(musicService).updateMusicUrl(next);

        try {
            ReflectionTestUtils.invokeMethod(
                    job, "tryPreFetchNextSong", "prefetch-room");

            assertTrue(prefetched.await(2L, TimeUnit.SECONDS));
            verify(musicService).updateMusicUrl(next);
            verify(musicService, never()).updateMusicUrl(playing);
        } finally {
            job.shutdown();
        }
    }

    @Test
    void playingSnapshotIsPersistedBeforeMusicBroadcast() {
        MusicJob job = jobWithDependencies();
        MusicService musicService = field(job, "musicService");
        ConfigRepository configRepository = field(job, "configRepository");
        MusicPlayingRepository playingRepository = field(job, "musicPlayingRepository");
        MusicVoteRepository voteRepository = field(job, "musicVoteRepository");
        SessionService sessionService = field(job, "sessionService");
        House house = house("room-1");
        Music music = new Music();
        music.setDuration(1_000L);
        LinkedList<Music> picks = new LinkedList<>();
        when(musicService.musicSwitch("room-1")).thenReturn(music);
        when(musicService.getPickList("room-1")).thenReturn(picks);

        ReflectionTestUtils.invokeMethod(job, "processHouse", house);

        InOrder order = inOrder(configRepository, playingRepository,
                sessionService, voteRepository);
        order.verify(configRepository).setLastMusicPushTimeAndDuration(
                any(Long.class), eq(1_000L), eq("room-1"));
        order.verify(playingRepository).leftPush(music, "room-1");
        order.verify(playingRepository).keepTheOne("room-1");
        order.verify(voteRepository).reset("room-1");
        order.verify(sessionService).send(eq(MessageType.MUSIC), any(), eq("room-1"));
        order.verify(sessionService).send(eq(MessageType.PICK), any(), eq("room-1"));
        order.verify(configRepository).setPushSwitch(false, "room-1");
        job.shutdown();
    }

    @Test
    void emptyQueueClosesSwitchWithoutDestroyingRoomState() {
        MusicJob job = jobWithDependencies();
        MusicService musicService = field(job, "musicService");
        ConfigRepository configRepository = field(job, "configRepository");
        MusicPlayingRepository playingRepository = field(job, "musicPlayingRepository");
        when(musicService.musicSwitch("room-empty")).thenReturn(null);

        ReflectionTestUtils.invokeMethod(job, "processHouse", house("room-empty"));

        verify(configRepository).setPushSwitch(false, "room-empty");
        verify(configRepository, never()).destroy("room-empty");
        verify(playingRepository, never()).destroy("room-empty");
        job.shutdown();
    }

    @Test
    void providerTimeoutReleasesPerHouseMarker() throws Exception {
        ProviderIsolationProperties providerProperties =
                new ProviderIsolationProperties();
        providerProperties.getPlayback().setTimeoutMs(40L);
        ThreadPoolTaskExecutor pick = providerExecutor("test-provider-pick-");
        ThreadPoolTaskExecutor search = providerExecutor("test-provider-search-");
        ThreadPoolTaskExecutor playback =
                providerExecutor("test-provider-playback-");
        ThreadPoolTaskExecutor background =
                providerExecutor("test-provider-background-");
        ProviderCallExecutor providerCalls = new ProviderCallExecutor(
                pick, search, playback, background, providerProperties);
        ExecutorService jobExecutor = Executors.newSingleThreadExecutor();
        JusicProperties jusicProperties = mock(JusicProperties.class);
        when(jusicProperties.hasSessions("slow-room")).thenReturn(true);
        MusicJob job = new MusicJob(jusicProperties, jobExecutor);
        MusicService musicService = mock(MusicService.class);
        SessionService sessionService = mock(SessionService.class);
        ConfigRepository configRepository = mock(ConfigRepository.class);
        HouseContainer houseContainer = mock(HouseContainer.class);
        House house = house("slow-room");
        CountDownLatch providerStarted = new CountDownLatch(1);
        when(houseContainer.get("slow-room")).thenReturn(house);
        when(musicService.musicSwitch("slow-room")).thenAnswer(invocation ->
                providerCalls.playback(() -> {
                    providerStarted.countDown();
                    new CountDownLatch(1).await();
                    return null;
                }));
        ReflectionTestUtils.setField(job, "musicService", musicService);
        ReflectionTestUtils.setField(job, "sessionService", sessionService);
        ReflectionTestUtils.setField(job, "configRepository", configRepository);
        ReflectionTestUtils.setField(job, "houseContainer", houseContainer);
        ReflectionTestUtils.setField(job, "houseOperationLocks",
                new HouseOperationLocks());
        ReflectionTestUtils.setField(job, "roomMusicLocks",
                new RoomMusicLocks());

        try {
            assertTrue(job.submitHouseTask(house));
            assertTrue(providerStarted.await(1L, TimeUnit.SECONDS));
            assertTrue(waitForEmptyMarkers(job, 2_000L));
            verify(configRepository, atLeastOnce())
                    .setPushSwitch(false, "slow-room");
            verify(sessionService).send(eq(MessageType.NOTICE),
                    any(), eq("slow-room"));
        } finally {
            job.shutdown();
            pick.shutdown();
            search.shutdown();
            playback.shutdown();
            background.shutdown();
        }
    }

    @Test
    void providerPhaseIsUnlockedAndPlaybackCommitExcludesConcurrentRoomMutation()
            throws Exception {
        MusicJob job = jobWithDependencies();
        MusicService musicService = field(job, "musicService");
        ConfigRepository configRepository = field(job, "configRepository");
        HouseOperationLocks houseLocks = field(job, "houseOperationLocks");
        RoomMusicLocks musicLocks = field(job, "roomMusicLocks");
        ReentrantLock houseLock = houseLocks.getLock("room-lock-scope");
        ReentrantLock musicLock = musicLocks.getLock("room-lock-scope");
        Music music = new Music();
        music.setDuration(1_000L);
        CountDownLatch commitStarted = new CountDownLatch(1);
        CountDownLatch releaseCommit = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        doAnswer(invocation -> {
            assertFalse(houseLock.isHeldByCurrentThread(),
                    "provider/musicSwitch call must not be wrapped by the job's room lock");
            assertFalse(musicLock.isHeldByCurrentThread(),
                    "provider/musicSwitch call must not be wrapped by the job's music lock");
            return music;
        }).when(musicService).musicSwitch("room-lock-scope");
        when(musicService.getPickList("room-lock-scope"))
                .thenReturn(new LinkedList<>());
        doAnswer(invocation -> {
            assertTrue(houseLock.isHeldByCurrentThread());
            assertTrue(musicLock.isHeldByCurrentThread());
            commitStarted.countDown();
            assertTrue(releaseCommit.await(2, TimeUnit.SECONDS));
            return null;
        }).when(configRepository).setLastMusicPushTimeAndDuration(
                any(Long.class), eq(1_000L), eq("room-lock-scope"));

        Thread worker = new Thread(() -> {
            try {
                ReflectionTestUtils.invokeMethod(
                        job, "processHouse", house("room-lock-scope"));
            } catch (Throwable ex) {
                failure.set(ex);
            }
        });
        worker.start();
        assertTrue(commitStarted.await(2, TimeUnit.SECONDS));

        boolean acquiredHouse = houseLock.tryLock();
        boolean acquiredMusic = musicLock.tryLock();
        try {
            assertFalse(acquiredHouse);
            assertFalse(acquiredMusic);
        } finally {
            if (acquiredMusic) {
                musicLock.unlock();
            }
            if (acquiredHouse) {
                houseLock.unlock();
            }
            releaseCommit.countDown();
        }
        worker.join(3_000L);

        assertFalse(worker.isAlive());
        assertNull(failure.get());
        job.shutdown();
    }

    @Test
    void roomDestroyedDuringProviderResolutionDoesNotRecreatePlaybackKeys()
            throws Exception {
        MusicJob job = jobWithDependencies();
        MusicService musicService = field(job, "musicService");
        ConfigRepository configRepository = field(job, "configRepository");
        MusicPlayingRepository playingRepository = field(job, "musicPlayingRepository");
        HouseContainer houseContainer = field(job, "houseContainer");
        AtomicReference<House> currentHouse = new AtomicReference<>(
                house("room-destroyed"));
        CountDownLatch providerStarted = new CountDownLatch(1);
        CountDownLatch releaseProvider = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Music music = new Music();
        music.setDuration(1_000L);
        when(houseContainer.get("room-destroyed"))
                .thenAnswer(invocation -> currentHouse.get());
        doAnswer(invocation -> {
            providerStarted.countDown();
            assertTrue(releaseProvider.await(2, TimeUnit.SECONDS));
            return music;
        }).when(musicService).musicSwitch("room-destroyed");

        Thread worker = new Thread(() -> {
            try {
                ReflectionTestUtils.invokeMethod(
                        job, "processHouse", house("room-destroyed"));
            } catch (Throwable ex) {
                failure.set(ex);
            }
        });
        worker.start();
        assertTrue(providerStarted.await(2, TimeUnit.SECONDS));
        currentHouse.set(null);
        releaseProvider.countDown();
        worker.join(3_000L);

        assertFalse(worker.isAlive());
        assertNull(failure.get());
        verify(configRepository, never()).setLastMusicPushTimeAndDuration(
                any(Long.class), any(Long.class), eq("room-destroyed"));
        verify(configRepository, never()).setPushSwitch(false, "room-destroyed");
        verify(playingRepository, never()).leftPush(any(Music.class), eq("room-destroyed"));
        verify(playingRepository, never()).keepTheOne("room-destroyed");
        job.shutdown();
    }

    private MusicJob jobWithDependencies() {
        JusicProperties properties = mock(JusicProperties.class);
        when(properties.hasSessions(any())).thenReturn(true);
        MusicJob job = new MusicJob(properties, new RejectingExecutor());
        ReflectionTestUtils.setField(job, "musicService", mock(MusicService.class));
        ReflectionTestUtils.setField(job, "configRepository", mock(ConfigRepository.class));
        ReflectionTestUtils.setField(job, "musicPlayingRepository", mock(MusicPlayingRepository.class));
        ReflectionTestUtils.setField(job, "musicVoteRepository", mock(MusicVoteRepository.class));
        ReflectionTestUtils.setField(job, "sessionService", mock(SessionService.class));
        HouseContainer houseContainer = mock(HouseContainer.class);
        Map<String, House> houses = new ConcurrentHashMap<>();
        when(houseContainer.get(any())).thenAnswer(invocation ->
                houses.computeIfAbsent(
                        invocation.getArgument(0), this::house));
        ReflectionTestUtils.setField(job, "houseContainer", houseContainer);
        ReflectionTestUtils.setField(job, "houseOperationLocks", new HouseOperationLocks());
        ReflectionTestUtils.setField(job, "roomMusicLocks", new RoomMusicLocks());
        return job;
    }

    @SuppressWarnings("unchecked")
    private <T> T field(MusicJob job, String name) {
        return (T) ReflectionTestUtils.getField(job, name);
    }

    private House house(String id) {
        House house = new House();
        house.setId(id);
        house.setName(id);
        return house;
    }

    private ThreadPoolTaskExecutor providerExecutor(String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix(prefix);
        executor.initialize();
        return executor;
    }

    private boolean waitForEmptyMarkers(MusicJob job, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Map<?, ?> markers = (Map<?, ?>)
                    ReflectionTestUtils.getField(job, "houseTaskMap");
            if (markers != null && markers.isEmpty()) {
                return true;
            }
            Thread.sleep(10L);
        }
        return false;
    }

    private static final class RejectingExecutor extends AbstractExecutorService {
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            shutdown = true;
            return java.util.Collections.emptyList();
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
            throw new RejectedExecutionException("full");
        }
    }
}
