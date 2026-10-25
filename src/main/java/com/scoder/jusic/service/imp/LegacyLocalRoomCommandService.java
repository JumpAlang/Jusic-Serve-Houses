package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.RoomMusicLocks;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.PickMusicResult;
import com.scoder.jusic.job.MusicJob;
import com.scoder.jusic.security.RoomRolePolicy;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.RoomCommandService;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Compatibility adapter for the existing port-suffixed Redis v1 schema.
 *
 * <p>This closes the check-then-enqueue race within one JVM only. It is the
 * sole supported {@link RoomCommandService} mode until the full room read and
 * write path, playback job and event fan-out have migrated to Redis v2.</p>
 */
@Service
@ConditionalOnProperty(
        prefix = "jusic.room-command",
        name = "mode",
        havingValue = "legacy-local",
        matchIfMissing = true)
@Slf4j
public class LegacyLocalRoomCommandService implements RoomCommandService {

    @Autowired
    private HouseOperationLocks houseOperationLocks;
    @Autowired
    private RoomMusicLocks roomMusicLocks;
    @Autowired
    private HouseContainer houseContainer;
    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private MusicService musicService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private SessionService sessionService;
    @Autowired(required = false)
    private MusicJob musicJob;

    @Override
    public PickMusicResult commitPick(String sessionId,
                                      String houseId,
                                      String source,
                                      Music music) {
        ReentrantLock houseLock = houseOperationLocks.getLock(houseId);
        ReentrantLock musicLock = roomMusicLocks.getLock(houseId);
        // Lock order is lifecycle -> music everywhere that needs both.
        houseLock.lock();
        musicLock.lock();
        try {
            House house = houseContainer.get(houseId);
            if (house == null) {
                return PickMusicResult.failure("房间不存在");
            }
            String pickDenialMessage = pickDenialMessage(sessionId, houseId);
            if (pickDenialMessage != null) {
                return PickMusicResult.failure(pickDenialMessage);
            }

            long limit = playlistLimit(house);
            Long currentSize = musicService.noDefaultPlaylistSize(houseId);
            if (currentSize != null && currentSize >= limit) {
                return PickMusicResult.failure("最多能点播" + limit + "首歌");
            }
            if (music == null) {
                return PickMusicResult.failure("点歌失败,请稍后重试");
            }
            if (hasText(music.getId()) && musicService.isBlack(music.getId(), houseId)) {
                return PickMusicResult.failure("点歌失败, 音乐已被拉黑");
            }
            if (hasText(music.getId()) && musicService.isPicked(music.getId(), houseId)) {
                return PickMusicResult.failure("点歌失败, 已在播放列表");
            }

            Music committed = musicService.toPick(sessionId, music, houseId, source);
            if (committed == null) {
                return PickMusicResult.failure("点歌失败,请稍后重试");
            }
            if (musicJob != null) {
                try {
                    musicJob.wakeHouse(houseId);
                } catch (RuntimeException wakeFailure) {
                    // The queue write already committed. A best-effort wake
                    // must never turn that successful write into a false
                    // failure response that encourages a duplicate retry.
                    log.warn("Unable to wake music job after committed pick, houseId: {}, reason: {}",
                            houseId, wakeFailure.getMessage());
                    log.debug("Music job wake failure", wakeFailure);
                }
            }
            LinkedList<Music> pickList;
            try {
                pickList = musicService.getPickList(houseId);
            } catch (RuntimeException snapshotFailure) {
                // The song is already in Redis. Preserve the successful
                // command result; the regular PICK broadcast/reconnect
                // snapshot will repair this best-effort response snapshot.
                log.warn("Unable to load pick snapshot after committed pick, houseId: {}, reason: {}",
                        houseId, snapshotFailure.getMessage());
                log.debug("Committed pick snapshot failure", snapshotFailure);
                pickList = new LinkedList<>();
            }
            if (pickList == null) {
                pickList = new LinkedList<>();
            }
            return PickMusicResult.success(committed, pickList);
        } catch (RuntimeException ex) {
            log.warn("点歌提交失败，reason: {}", ex.getMessage());
            return PickMusicResult.failure("点歌失败,请稍后重试");
        } finally {
            musicLock.unlock();
            houseLock.unlock();
        }
    }

    private String pickDenialMessage(String sessionId, String houseId) {
        // A provider call may finish after this connection has switched rooms.
        // Recheck the node-local authoritative membership while holding the
        // same lifecycle lock as the move, so a delayed old-room request can
        // never enqueue after switch cleanup has completed.
        if (jusicProperties.getSessionIfPresent(houseId, sessionId) == null) {
            return "当前禁止点歌";
        }
        String role = sessionService.getRole(sessionId, houseId);
        if (RoomRolePolicy.isPrivileged(role)) {
            return null;
        }
        if (Boolean.FALSE.equals(configService.getEnableSearch(houseId))
                && !RoomRolePolicy.hasCapability(role, "picker")) {
            return "当前禁止点歌";
        }
        Integer configuredMinutes = configService.getPickWaitMinutes(houseId);
        int waitMinutes = configuredMinutes == null ? 0 : Math.max(0, configuredMinutes);
        if (waitMinutes == 0) {
            return null;
        }
        com.scoder.jusic.model.User user = sessionService.getUser(sessionId, houseId);
        if (user == null || user.getJoinedAt() == null) {
            return null;
        }
        long remainingMillis = waitMinutes * 60_000L
                - (System.currentTimeMillis() - user.getJoinedAt());
        if (remainingMillis <= 0) {
            return null;
        }
        long remainingMinutes = (remainingMillis + 59_999L) / 60_000L;
        return "进入房间满" + waitMinutes + "分钟后才能点歌，还需等待约"
                + remainingMinutes + "分钟";
    }

    private long playlistLimit(House house) {
        return Boolean.TRUE.equals(house.getEnableStatus())
                ? jusicProperties.getForeverlistSize()
                : jusicProperties.getPlaylistSize();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
