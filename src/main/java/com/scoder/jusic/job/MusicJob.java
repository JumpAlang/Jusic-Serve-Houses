package com.scoder.jusic.job;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.RoomMusicLocks;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.repository.SessionRepository;
import com.scoder.jusic.security.ProviderCallException;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author H
 */
@Component
@Slf4j
public class MusicJob {

    private static final long INITIAL_RETRY_DELAY_MILLIS = 1_000L;
    private static final long MAX_RETRY_DELAY_MILLIS = 60_000L;
    private static final int MAX_RETRY_FAILURES = 7;

    private final ExecutorService executorService;
    private final ExecutorService prefetchExecutor;
    private final ConcurrentHashMap<String, AtomicBoolean> houseTaskMap;
    private final ConcurrentHashMap<String, RetryState> houseRetryMap;
    private final ConcurrentHashMap<String, String> lastPrefetchedSongMap;
    private final JusicProperties jusicProperties;
    private final LongSupplier currentTimeMillis;
    private final LongUnaryOperator retryJitterMillis;

    @Autowired
    public MusicJob(JusicProperties jusicProperties) {
        this(jusicProperties, createExecutor(jusicProperties));
    }

    MusicJob(JusicProperties jusicProperties, ExecutorService executorService) {
        this(jusicProperties, executorService,
                createPrefetchExecutor(jusicProperties),
                System::currentTimeMillis, MusicJob::randomRetryJitterMillis);
    }

    MusicJob(JusicProperties jusicProperties,
             ExecutorService executorService,
             LongSupplier currentTimeMillis,
             LongUnaryOperator retryJitterMillis) {
        this(jusicProperties, executorService,
                createPrefetchExecutor(jusicProperties),
                currentTimeMillis, retryJitterMillis);
    }

    private MusicJob(JusicProperties jusicProperties,
                     ExecutorService executorService,
                     ExecutorService prefetchExecutor,
                     LongSupplier currentTimeMillis,
                     LongUnaryOperator retryJitterMillis) {
        this.executorService = executorService;
        this.prefetchExecutor = prefetchExecutor;
        this.jusicProperties = jusicProperties;
        this.houseTaskMap = new ConcurrentHashMap<>();
        this.houseRetryMap = new ConcurrentHashMap<>();
        this.lastPrefetchedSongMap = new ConcurrentHashMap<>();
        this.currentTimeMillis = currentTimeMillis;
        this.retryJitterMillis = retryJitterMillis;
    }

    private static long randomRetryJitterMillis(long baseDelayMillis) {
        long available = MAX_RETRY_DELAY_MILLIS - baseDelayMillis;
        if (available <= 0L) {
            return 0L;
        }
        long upperBound = Math.min(available, Math.max(1L,
                baseDelayMillis / 4L));
        return ThreadLocalRandom.current().nextLong(upperBound + 1L);
    }

    private static ExecutorService createExecutor(JusicProperties properties) {
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        int corePoolSize = Math.max(2, Math.min(8, processors));
        int maximumPoolSize = Math.max(corePoolSize, Math.min(32, processors * 2));
        int configuredHouseSize = properties.getHouseSize() == null
                ? 10
                : properties.getHouseSize();
        int queueCapacity = Math.max(1, Math.min(512, configuredHouseSize));
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task,
                    "jusic-music-job-" + threadNumber.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    private static ExecutorService createPrefetchExecutor(
            JusicProperties properties) {
        int configuredHouseSize = properties.getHouseSize() == null
                ? 10 : properties.getHouseSize();
        int queueCapacity = Math.max(1, Math.min(64, configuredHouseSize));
        int poolSize = Math.max(1, Math.min(4,
                Runtime.getRuntime().availableProcessors()));
        AtomicInteger threadNumber = new AtomicInteger();
        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task,
                    "jusic-music-prefetch-" + threadNumber.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private MusicVoteRepository musicVoteRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private MusicService musicService;
    @Autowired
    private HouseContainer houseContainer;
    @Autowired
    private HouseOperationLocks houseOperationLocks;
    @Autowired
    private RoomMusicLocks roomMusicLocks;

    /**
     * 广播条件：第一次启动时 playing 为空、音乐播放完毕、投票切歌
     */
    @Scheduled(fixedRate = 500)
    private void sendIfSufficient() {
        CopyOnWriteArrayList<House> houses = houseContainer.getHouses();
        removeRetryStateForMissingRooms(houses);
        for(House house : houses){
            if(jusicProperties.hasSessions(house.getId())){//没人就不推送
//                log.info("houseName:{},houseId:{}",house.getName(),house.getId());
                if (!isRetryDue(house.getId())) {
                    continue;
                }
                if (isHouseTaskRunning(house.getId())) {
                    continue;
                }
                try{
                    String triggerReason = null;
                    if (this.isPlayingNull(house.getId())) {
                        configRepository.setPushSwitch(true,house.getId());
                        triggerReason = "首次启动" + house.getName();
                    } else if (this.isPlayingOver(house.getId())) {
                        configRepository.setPushSwitch(true,house.getId());
                        triggerReason = "上一首播放完毕";
                    } else if (this.isPlayingSkip(house.getId())) {
                        configRepository.setPushSwitch(true,house.getId());
                        triggerReason = "投票通过";
                    }

                    if (this.isPushSwitchOpen(house.getId())) {
                        if (submitHouseTask(house)
                                && triggerReason != null) {
                            log.info("推送开关开启, 原因: {}", triggerReason);
                        }
                    } else {
                        tryPreFetchNextSong(house.getId());
                    }
                }catch(Exception e){
                    recordRetryFailure(house, retryGeneration(house.getId()));
                    handleException(e,house);
                }
            }else if(house != null
                    && house.getCreateTime() != null
                    && (System.currentTimeMillis()-house.getCreateTime() > 20000)){
                    houseContainer.destroyIfTemporaryAndEmpty(house.getId());
            }
//

        }

    }

    private boolean isHouseTaskRunning(String houseId) {
        AtomicBoolean marker = houseTaskMap.get(houseId);
        return marker != null && marker.get();
    }

    private void tryPreFetchNextSong(String houseId) {
        Long lastMusicDuration = configRepository.getLastMusicDuration(houseId);
        Long lastMusicPushTime = configRepository.getLastMusicPushTime(houseId);
        if (lastMusicDuration == null || lastMusicPushTime == null) {
            return;
        }
        long remainingTimeMs = (lastMusicPushTime + lastMusicDuration) - System.currentTimeMillis();
        // If remaining time is less than 20 seconds, pre-fetch next song
        if (remainingTimeMs > 0 && remainingTimeMs <= 20000) {
            LinkedList<Music> pickList = musicService.getPickList(houseId);
            // getPickList exposes the current playing item at index 0 and the
            // next queued item at index 1. Prefetching index 0 merely refreshes
            // the song that is already playing and gives no switch benefit.
            if (pickList != null && pickList.size() > 1) {
                Music nextSong = pickList.get(1);
                if (nextSong != null && nextSong.getId() != null) {
                    String trackKey = nextSong.getSource() + ":" + nextSong.getId();
                    String lastTrack = lastPrefetchedSongMap.get(houseId);
                    if (!trackKey.equals(lastTrack)) {
                        lastPrefetchedSongMap.put(houseId, trackKey);
                        try {
                            prefetchExecutor.execute(() -> {
                                try {
                                    log.info("Asynchronously pre-fetching next song: {} for room: {}", nextSong.getName(), houseId);
                                    musicService.updateMusicUrl(nextSong);
                                } catch (Exception e) {
                                    log.warn("Failed to pre-fetch next song for room: {}, err: {}", houseId, e.getMessage());
                                }
                            });
                        } catch (RejectedExecutionException ex) {
                            // A prefetch is optional. Let a later scheduler
                            // tick retry it without competing with playback
                            // transition workers.
                            lastPrefetchedSongMap.remove(houseId, trackKey);
                            log.debug("music prefetch queue is full, houseId: {}",
                                    houseId);
                        }
                    }
                }
            }
        }
    }

    boolean submitHouseTask(House house) {
        String houseId = house.getId();
        if (!isRetryDue(houseId)) {
            return false;
        }
        AtomicBoolean marker = houseTaskMap.computeIfAbsent(
                houseId, id -> new AtomicBoolean(false));
        if (!marker.compareAndSet(false, true)) {
            return false;
        }
        try {
            executorService.execute(() -> {
                try {
                    processHouse(house, house);
                } catch (Exception e) {
                    handleException(e, house);
                } finally {
                    houseTaskMap.remove(houseId, marker);
                }
            });
            return true;
        } catch (RejectedExecutionException ex) {
            // Keep the push switch open so a later scheduler tick can retry,
            // but release the local marker. AbortPolicy ensures processHouse
            // never runs on the 500 ms scheduler thread.
            houseTaskMap.remove(houseId, marker);
            recordRetryFailure(house, retryGeneration(houseId));
            log.warn("music task queue is full, houseId: {}", houseId);
            return false;
        }
    }

    private void handleException(Exception e, House house) {
        if (house != null) {
            try {
                closePushSwitchIfRoomPresent(house);
            } catch (Exception resetException) {
                log.error("定时任务关闭推送开关异常[{}]", resetException.getMessage());
            }
            if (e instanceof ProviderCallException) {
                RetryState retryState = houseRetryMap.get(house.getId());
                if (retryState != null
                        && retryState.consecutiveFailures == 1) {
                    try {
                        sessionService.send(
                                MessageType.NOTICE,
                                Response.failure((Object) null,
                                        "音乐源响应超时，切歌将在稍后自动重试"),
                                house.getId());
                    } catch (RuntimeException noticeFailure) {
                        log.debug("Unable to publish provider timeout notice for house {}",
                                house.getId(), noticeFailure);
                    }
                }
                log.warn("Playback provider unavailable, houseId: {}, retry scheduled: {}",
                        house.getId(), e.getMessage());
                log.debug("Playback provider failure for house " + house.getId(), e);
            } else {
                log.error("houseName:{},houseId:{},message:[{}]",
                        house.getName(), house.getId(), e.getMessage(), e);
            }
        } else {
            log.error("music scheduler failed: {}", e.getMessage(), e);
        }
    }
    @PreDestroy
    public void shutdown() {
        shutdownExecutor(executorService);
        shutdownExecutor(prefetchExecutor);
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    private void processHouse(House house) {
        processHouse(house, houseContainer.get(house.getId()));
    }

    private void processHouse(House house, House expectedHouse){
        log.info("检测到推送开关已开启");
        String houseId = house.getId();
        try {
            if (!isRoomActive(expectedHouse)) {
                return;
            }
            long attemptGeneration = retryGeneration(houseId);
            Music music;
            LinkedList<Music> result;
            try {
                music = musicService.musicSwitch(houseId);
                if (music == null) {
                    recordRetryFailure(expectedHouse, attemptGeneration);
                    log.warn("推送开关已开启但播放列表为空, houseId: {}", houseId);
                    return;
                }
                long pushTime = System.currentTimeMillis();
                if(music.getDuration() == null){
                    music.setDuration(300000L);
                }
                music.setPushTime(pushTime);
                result = commitPlaybackState(
                        expectedHouse, music, pushTime);
            } catch (RuntimeException ex) {
                recordRetryFailure(expectedHouse, attemptGeneration);
                throw ex;
            }
            if (result == null) {
                log.info("Skip playback commit because room is gone or empty, houseId: {}",
                        houseId);
                return;
            }
            log.info("已保存推送时间和音乐时长"+house.getName());
            sessionService.send(MessageType.MUSIC,
                    Response.success(music, "正在播放"),houseId);
            log.info("已重置投票");
            sessionService.send(MessageType.PICK,
                    Response.success(result, "播放列表"),houseId);
        } finally {
            closePushSwitchIfRoomPresent(expectedHouse);
            log.info("已关闭音乐推送开关"+house.getName());
        }
    }

    /**
     * Commit the reconnect snapshot only while room destruction is excluded.
     * Slow provider work is completed by musicSwitch before these locks are
     * acquired. The final room/session recheck prevents a task that outlived
     * its last listener from recreating destroyed legacy Redis keys.
     */
    private LinkedList<Music> commitPlaybackState(House expectedHouse,
                                                  Music music,
                                                  long pushTime) {
        String houseId = expectedHouse.getId();
        ReentrantLock houseLock = houseOperationLocks.getLock(houseId);
        ReentrantLock musicLock = roomMusicLocks.getLock(houseId);
        houseLock.lock();
        musicLock.lock();
        try {
            House currentHouse = houseContainer.get(houseId);
            if (currentHouse != expectedHouse
                    || !jusicProperties.hasSessions(houseId)) {
                clearRetryStateIfOwned(houseId, currentHouse, expectedHouse);
                return null;
            }
            configRepository.setLastMusicPushTimeAndDuration(
                    pushTime, music.getDuration(), houseId);
            // Persist the reconnect snapshot before publishing the event.
            musicPlayingRepository.leftPush(music, houseId);
            musicPlayingRepository.keepTheOne(houseId);
            musicVoteRepository.reset(houseId);
            LinkedList<Music> pickList = musicService.getPickList(houseId);
            clearRetryState(houseId);
            return pickList == null ? new LinkedList<>() : pickList;
        } finally {
            musicLock.unlock();
            houseLock.unlock();
        }
    }

    private boolean isRoomActive(House expectedHouse) {
        if (expectedHouse == null || expectedHouse.getId() == null) {
            return false;
        }
        String houseId = expectedHouse.getId();
        ReentrantLock houseLock = houseOperationLocks.getLock(houseId);
        houseLock.lock();
        try {
            House currentHouse = houseContainer.get(houseId);
            boolean active = currentHouse == expectedHouse
                    && jusicProperties.hasSessions(houseId);
            if (!active) {
                clearRetryStateIfOwned(houseId, currentHouse, expectedHouse);
            }
            return active;
        } finally {
            houseLock.unlock();
        }
    }

    private void closePushSwitchIfRoomPresent(House expectedHouse) {
        if (expectedHouse == null || expectedHouse.getId() == null) {
            return;
        }
        String houseId = expectedHouse.getId();
        ReentrantLock houseLock = houseOperationLocks.getLock(houseId);
        ReentrantLock musicLock = roomMusicLocks.getLock(houseId);
        houseLock.lock();
        musicLock.lock();
        try {
            if (houseContainer.get(houseId) == expectedHouse) {
                configRepository.setPushSwitch(false, houseId);
            }
        } finally {
            musicLock.unlock();
            houseLock.unlock();
        }
    }

    private boolean isRetryDue(String houseId) {
        RetryState state = houseRetryMap.get(houseId);
        return state == null
                || currentTimeMillis.getAsLong() >= state.nextRetryAtMillis;
    }

    private long retryGeneration(String houseId) {
        RetryState state = houseRetryMap.get(houseId);
        return state == null ? 0L : state.generation;
    }

    private void recordRetryFailure(House expectedHouse,
                                    long attemptGeneration) {
        if (expectedHouse == null || expectedHouse.getId() == null) {
            return;
        }
        String houseId = expectedHouse.getId();
        ReentrantLock houseLock = houseOperationLocks.getLock(houseId);
        houseLock.lock();
        try {
            House currentHouse = houseContainer.get(houseId);
            if (currentHouse != expectedHouse) {
                clearRetryStateIfOwned(houseId, currentHouse, expectedHouse);
                return;
            }
            final long now = currentTimeMillis.getAsLong();
            houseRetryMap.compute(houseId, (id, previous) -> {
                long currentGeneration = previous == null
                        ? 0L : previous.generation;
                if (currentGeneration != attemptGeneration) {
                    return previous;
                }
                int failures = previous == null
                        ? 1
                        : Math.min(MAX_RETRY_FAILURES,
                        previous.consecutiveFailures + 1);
                long baseDelay = retryDelayMillis(failures);
                long jitter = retryJitterMillis.applyAsLong(baseDelay);
                long maxJitter = MAX_RETRY_DELAY_MILLIS - baseDelay;
                jitter = Math.max(0L, Math.min(maxJitter, jitter));
                return new RetryState(failures,
                        now + baseDelay + jitter, currentGeneration);
            });
        } finally {
            houseLock.unlock();
        }
    }

    private long retryDelayMillis(int failures) {
        long delay = INITIAL_RETRY_DELAY_MILLIS;
        for (int failure = 1;
             failure < failures && delay < MAX_RETRY_DELAY_MILLIS;
             failure++) {
            delay = Math.min(MAX_RETRY_DELAY_MILLIS, delay * 2L);
        }
        return delay;
    }

    private void clearRetryState(String houseId) {
        houseRetryMap.remove(houseId);
    }

    private void clearRetryStateIfOwned(String houseId,
                                        House currentHouse,
                                        House expectedHouse) {
        if (currentHouse == null || currentHouse == expectedHouse) {
            clearRetryState(houseId);
        }
    }

    /** A newly committed song must not wait behind an earlier provider backoff. */
    public void wakeHouse(String houseId) {
        if (houseId == null) {
            return;
        }
        ReentrantLock houseLock = houseOperationLocks.getLock(houseId);
        houseLock.lock();
        try {
            if (houseContainer.get(houseId) == null) {
                clearRetryState(houseId);
                return;
            }
            final long now = currentTimeMillis.getAsLong();
            houseRetryMap.compute(houseId, (id, previous) -> {
                int failures = previous == null
                        ? 0 : previous.consecutiveFailures;
                long generation = previous == null
                        ? 1L : previous.generation + 1L;
                return new RetryState(failures, now, generation);
            });
        } finally {
            houseLock.unlock();
        }
    }

    private void removeRetryStateForMissingRooms(
            CopyOnWriteArrayList<House> houses) {
        Set<String> activeHouseIds = new HashSet<>();
        for (House house : houses) {
            if (house != null && house.getId() != null) {
                activeHouseIds.add(house.getId());
            }
        }
        if (!houseRetryMap.isEmpty()) {
            houseRetryMap.keySet().retainAll(activeHouseIds);
        }
        lastPrefetchedSongMap.keySet().retainAll(activeHouseIds);
    }

    private static final class RetryState {
        private final int consecutiveFailures;
        private final long nextRetryAtMillis;
        private final long generation;

        private RetryState(int consecutiveFailures,
                           long nextRetryAtMillis,
                           long generation) {
            this.consecutiveFailures = consecutiveFailures;
            this.nextRetryAtMillis = nextRetryAtMillis;
            this.generation = generation;
        }
    }

    private boolean isPushSwitchOpen(String houseId) {
        Boolean pushSwitch = configRepository.getPushSwitch(houseId);
        return pushSwitch == null ? false : pushSwitch;
    }

    private boolean isGoodModel(String houseId){
        Boolean goodModel = configRepository.getGoodModel(houseId);
        return goodModel == null ? false:goodModel;
    }

    private boolean isPlayingSkip(String houseId) {
        Long voteSize = musicVoteRepository.size(houseId);
        Integer sessionSize = jusicProperties.getSessionCount(houseId);
        Float voteRate = configRepository.getVoteRate(houseId);
        if (voteSize != null && voteSize != 0 && sessionSize != null && voteRate != null) {
            return voteSize >= sessionSize * voteRate;
        }
        return false;
    }

    public void triggerHouse(String houseId) {
        if (houseId == null) {
            return;
        }
        wakeHouse(houseId);
        House house = houseContainer.get(houseId);
        if (house != null && jusicProperties.hasSessions(houseId)) {
            submitHouseTask(house);
        }
    }

    public boolean isPlayingOver(String houseId) {
        Long lastMusicDuration = configRepository.getLastMusicDuration(houseId);
        Long lastMusicPushTime = configRepository.getLastMusicPushTime(houseId);
        if(null == lastMusicDuration || null == lastMusicPushTime){
            return true;
        }
        return (lastMusicPushTime + lastMusicDuration) - System.currentTimeMillis() <= 0;
    }

    public boolean isPlayingNull(String houseId) {
        return null == musicPlayingRepository.getPlaying(houseId);
    }
}
