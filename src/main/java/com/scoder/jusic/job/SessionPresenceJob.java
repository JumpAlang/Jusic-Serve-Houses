package com.scoder.jusic.job;

import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.PresenceProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.repository.SessionRepository;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renews live presence leases and removes Redis-only ghost users.
 */
@Component
@Slf4j
public class SessionPresenceJob {

    private final JusicProperties jusicProperties;
    private final HouseContainer houseContainer;
    private final SessionRepository sessionRepository;
    private final SessionService sessionService;
    private final PresenceProperties properties;
    private final ConcurrentHashMap<String, Boolean> missingHashOnce =
            new ConcurrentHashMap<>();

    public SessionPresenceJob(JusicProperties jusicProperties,
                              HouseContainer houseContainer,
                              SessionRepository sessionRepository,
                              SessionService sessionService,
                              PresenceProperties properties) {
        this.jusicProperties = jusicProperties;
        this.houseContainer = houseContainer;
        this.sessionRepository = sessionRepository;
        this.sessionService = sessionService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${jusic.presence.heartbeat-delay:15000}")
    public void refreshLocalPresence() {
        if (!properties.isEnabled()) {
            return;
        }
        Map<String, Collection<String>> openSessions = new LinkedHashMap<>();
        Set<String> openSessionKeys = new LinkedHashSet<>();
        for (Map.Entry<String, ConcurrentHashMap<String, WebSocketSession>> houseEntry
                : jusicProperties.getSession().get().entrySet()) {
            String houseId = houseEntry.getKey();
            List<String> sessionIds = new ArrayList<>();
            for (WebSocketSession session : houseEntry.getValue().values()) {
                try {
                    if (!session.isOpen()) {
                        sessionService.clearSession(session, houseId);
                    } else {
                        sessionIds.add(session.getId());
                        openSessionKeys.add(localKey(houseId, session.getId()));
                    }
                } catch (RuntimeException ex) {
                    log.warn("检查本地在线连接失败，保留连接 {}: {}",
                            session.getId(), ex.getMessage());
                    log.debug("检查本地在线连接异常", ex);
                }
            }
            if (!sessionIds.isEmpty()) {
                openSessions.put(houseId, sessionIds);
            }
        }
        missingHashOnce.keySet().retainAll(openSessionKeys);
        if (openSessions.isEmpty()) {
            return;
        }
        try {
            Map<String, List<String>> missing =
                    sessionRepository.refreshPresence(openSessions);
            handleMissingHashes(openSessions,
                    missing == null ? java.util.Collections.emptyMap() : missing);
        } catch (RuntimeException ex) {
            // Redis/network failure is not proof that the lease expired.
            log.warn("批量刷新在线租约失败，保留本地连接: {}", ex.getMessage());
            log.debug("批量刷新在线租约异常", ex);
        }
    }

    private void handleMissingHashes(
            Map<String, Collection<String>> openSessions,
            Map<String, List<String>> missing) {
        int newlyMissing = 0;
        int closed = 0;
        int closeFailures = 0;
        for (Map.Entry<String, Collection<String>> houseEntry
                : openSessions.entrySet()) {
            String houseId = houseEntry.getKey();
            Collection<String> missingInHouse = missing.get(houseId);
            if (missingInHouse != null && !(missingInHouse instanceof Set)) {
                // The repository currently returns lists. Convert once so a
                // large missing batch remains O(n) instead of O(n squared).
                missingInHouse = new HashSet<>(missingInHouse);
            }
            for (String sessionId : houseEntry.getValue()) {
                String localKey = localKey(houseId, sessionId);
                if (missingInHouse == null
                        || !missingInHouse.contains(sessionId)) {
                    missingHashOnce.remove(localKey);
                    continue;
                }
                if (missingHashOnce.putIfAbsent(
                        localKey, Boolean.TRUE) == null) {
                    newlyMissing++;
                    continue;
                }

                WebSocketSession session = jusicProperties.getSession()
                        .getSessionIfPresent(houseId, sessionId);
                missingHashOnce.remove(localKey);
                if (session == null) {
                    continue;
                }
                try {
                    // Two consecutive atomic checks found no User HASH. Keep
                    // Redis and local membership consistent and let SockJS
                    // reconnect instead of renewing a marker-only ghost.
                    sessionService.clearSession(session, houseId);
                    if (session.isOpen()) {
                        session.close(CloseStatus.SESSION_NOT_RELIABLE);
                    }
                    closed++;
                } catch (Exception ex) {
                    closeFailures++;
                    log.debug("关闭缺失 Redis 用户快照的连接 {} 异常",
                            sessionId, ex);
                }
            }
        }
        if (newlyMissing > 0) {
            log.warn("检测到 {} 个在线连接的 Redis 用户快照缺失，等待下一轮复核",
                    newlyMissing);
        }
        if (closed > 0) {
            log.info("已关闭 {} 个连续缺失 Redis 用户快照的连接", closed);
        }
        if (closeFailures > 0) {
            log.warn("关闭缺失 Redis 用户快照的连接失败: {} 个",
                    closeFailures);
        }
    }

    private String localKey(String houseId, String sessionId) {
        return houseId + '\u0000' + sessionId;
    }

    @Scheduled(fixedDelayString = "${jusic.presence.cleanup-delay:30000}")
    public void pruneRedisGhosts() {
        if (!properties.isEnabled()) {
            return;
        }
        Set<String> houseIds = new LinkedHashSet<>();
        for (House house : houseContainer.getHouses()) {
            if (house != null && house.getId() != null) {
                houseIds.add(house.getId());
            }
        }
        try {
            // SCAN also discovers session HASHes left by temporary rooms that
            // were not restored into HouseContainer after a process crash.
            houseIds.addAll(sessionRepository.findPresenceHouseIds());
        } catch (RuntimeException ex) {
            // Known rooms can still be cleaned when the discovery scan fails.
            log.warn("扫描 Redis 在线房间失败，继续清理已知房间: {}", ex.getMessage());
            log.debug("扫描 Redis 在线房间异常", ex);
        }

        long removed = 0L;
        for (String houseId : houseIds) {
            try {
                removed += sessionRepository.pruneExpiredPresence(
                        houseId, openLocalSessionIds(houseId));
                cleanupUnknownEmptyRoom(houseId);
            } catch (RuntimeException ex) {
                log.warn("清理房间 {} 的过期在线用户失败: {}",
                        houseId, ex.getMessage());
                log.debug("清理过期在线用户异常", ex);
            }
        }
        if (removed > 0L) {
            log.info("已清理 Redis 过期在线用户: {}", removed);
        }
    }

    private Collection<String> openLocalSessionIds(String houseId) {
        Map<String, WebSocketSession> localSessions = jusicProperties
                .getSession().getHouseSessionIfPresent(houseId);
        if (localSessions == null || localSessions.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<String> open = new ArrayList<>();
        for (Map.Entry<String, WebSocketSession> entry
                : localSessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            try {
                if (session != null && session.isOpen()) {
                    open.add(entry.getKey());
                }
            } catch (RuntimeException ex) {
                // Failure to inspect a local socket is not proof that it is a
                // ghost. Protect it until the connection reaper can decide.
                open.add(entry.getKey());
            }
        }
        return open;
    }

    private void cleanupUnknownEmptyRoom(String houseId) {
        // The durable presence room index remains until destroyRedis deletes
        // every room key successfully. A partial failure is retried next run.
        if (houseContainer.destroyRedisIfUnknownAndEmpty(houseId)) {
            log.info("已清理崩溃遗留的临时房间: {}", houseId);
        }
    }
}
