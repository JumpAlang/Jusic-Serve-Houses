package com.scoder.jusic.handler;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.ConnectionAdmissionException;
import com.scoder.jusic.configuration.ClientConnectionRegistry;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.Online;
import com.scoder.jusic.repository.MusicPlayingRepository;
import com.scoder.jusic.interceptor.ClientIdentityFilter;
import com.scoder.jusic.job.MusicJob;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author H
 */
@Component
@Slf4j
public class JusicWebSocketHandlerAsync {

    public static final String CONNECTION_CLOSED_ATTRIBUTE =
            JusicWebSocketHandlerAsync.class.getName() + ".connectionClosed";
    public static final int DUPLICATE_CLIENT_CLOSE_CODE = 4001;
    public static final String DUPLICATE_CLIENT_CLOSE_REASON =
            "browser connection already active";
    private static final String CONNECTION_CLEANED_ATTRIBUTE =
            JusicWebSocketHandlerAsync.class.getName() + ".connectionCleaned";
    private static final String DUPLICATE_CLIENT_REJECTED_ATTRIBUTE =
            JusicWebSocketHandlerAsync.class.getName()
                    + ".duplicateClientRejected";
    private static final String CLIENT_MESSAGE_HANDLER_READY_ATTRIBUTE =
            JusicWebSocketHandlerAsync.class.getName()
                    + ".clientMessageHandlerReady";
    private static final String INITIAL_ROOM_SNAPSHOT_ATTRIBUTE =
            JusicWebSocketHandlerAsync.class.getName()
                    + ".initialRoomSnapshot";
    private static final String INITIAL_ROOM_SNAPSHOT_DELIVERED_ATTRIBUTE =
            JusicWebSocketHandlerAsync.class.getName()
                    + ".initialRoomSnapshotDelivered";

    @Autowired
    private JusicProperties musicBar;
    @Autowired
    private MusicPlayingRepository musicPlayingRepository;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private MusicService musicService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private HouseContainer houseContainer;
    @Autowired
    private HouseOperationLocks houseOperationLocks;
    @Autowired
    private ClientConnectionRegistry clientConnectionRegistry;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired(required = false)
    private MusicJob musicJob;

    /**
     * Reserve the stable browser id before the STOMP delegate observes the
     * transport. This method is intentionally synchronous and first-wins.
     */
    public boolean tryReserveConnection(WebSocketSession session)
            throws Exception {
        String clientId = clientId(session);
        ReentrantLock lock = clientConnectionRegistry.lockFor(clientId);
        lock.lock();
        try {
            WebSocketSession existing = clientConnectionRegistry.current(clientId);
            if (existing != null && existing != session && existing.isOpen()) {
                log.info("Superseding older browser session {} with new session {}",
                        existing.getId(), session.getId());
                try {
                    existing.close(new CloseStatus(
                            4002, "superseded by new browser connection"));
                } catch (Exception ex) {
                    log.warn("Failed to close superseded session {}", existing.getId(), ex);
                }
            }
            clientConnectionRegistry.bind(clientId, session);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean releaseConnectionReservation(WebSocketSession session) {
        return clientConnectionRegistry.unbind(session);
    }

    public boolean isDuplicateClientRejected(WebSocketSession session) {
        return Boolean.TRUE.equals(session.getAttributes().get(
                DUPLICATE_CLIENT_REJECTED_ATTRIBUTE));
    }

    @Async("applicationTaskExecutor")
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!isConnectionActive(session)) {
            releaseConnectionReservation(session);
            return;
        }
        String clientId = clientId(session);
        if (clientConnectionRegistry.current(clientId) != session) {
            // Production callers reserve synchronously in the decorator. Do
            // not let an unreserved async task enter local or Redis presence.
            markConnectionClosed(session);
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
            return;
        }
        String houseId = (String)session.getAttributes().get("houseId");
        House house = houseContainer.get(houseId);
        if(house == null){
            house = houseContainer.get(JusicProperties.HOUSE_DEFAULT_ID);
            if (house == null) {
                House[] available = houseContainer.getHouses().toArray(new House[0]);
                house = available.length == 0 ? null : available[0];
            }
            if (house == null) {
                releaseConnectionReservation(session);
                markConnectionClosed(session);
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            houseId = house.getId();
        }

        ReentrantLock houseLock = houseOperationLocks.getLock(houseId);
        houseLock.lock();
        try {
            // Browser ownership was reserved synchronously before the STOMP
            // delegate was invoked. This async stage only performs room
            // admission and initialization.
            House currentHouse = houseContainer.get(houseId);
            if (currentHouse == null) {
                releaseConnectionReservation(session);
                markConnectionClosed(session);
                if (session.isOpen()) {
                    session.close(CloseStatus.POLICY_VIOLATION);
                }
                return;
            }
            house = currentHouse;

            // The active check and insertion are serialized with close
            // marking. If close wins, insertion is skipped; if insertion
            // wins, close waits and then removes it.
            synchronized (session) {
                if (!isConnectionActiveLocked(session)) {
                    releaseConnectionReservation(session);
                    return;
                }
                try {
                    session.getAttributes().put("houseId",houseId);
                    sessionService.putSession(session,houseId);
                } catch (ConnectionAdmissionException ex) {
                    log.warn("WebSocket connection rejected for session {}: {}",
                            session.getId(), ex.getReason());
                    releaseConnectionReservation(session);
                    markConnectionClosed(session);
                    if (session.isOpen()) {
                        session.close(new CloseStatus(1013,
                                "connection capacity reached"));
                    }
                    return;
                } catch (RuntimeException ex) {
                    log.error("WebSocket session registration failed for {}",
                            session.getId(), ex);
                    releaseConnectionReservation(session);
                    markConnectionClosed(session);
                    if (session.isOpen()) {
                        session.close(CloseStatus.SERVER_ERROR);
                    }
                    return;
                }
            }
        } finally {
            houseLock.unlock();
        }
        try {
            initializeRegisteredConnection(session, houseId, house);
        } catch (RuntimeException ex) {
            // Registration already succeeded. Do not leave a half-initialized
            // socket in local/Redis presence when a snapshot read fails.
            log.warn("WebSocket initialization failed for session {}: {}",
                    session.getId(), ex.getMessage());
            log.debug("WebSocket initialization failure", ex);
            markConnectionClosed(session);
            try {
                sessionService.clearSession(session, houseId);
            } catch (RuntimeException cleanupFailure) {
                log.warn("WebSocket initialization cleanup failed for session {}: {}",
                        session.getId(), cleanupFailure.getMessage());
                log.debug("WebSocket initialization cleanup failure", cleanupFailure);
            } finally {
                releaseConnectionReservation(session);
            }
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.SERVER_ERROR);
                }
            } catch (Exception closeFailure) {
                log.debug("Unable to close failed WebSocket initialization {}",
                        session.getId(), closeFailure);
            }
        }
    }

    private void initializeRegisteredConnection(WebSocketSession session,
                                                String houseId,
                                                House house) {
        int registeredSize = musicBar.getSessionCount(houseId);
        // Do not persist a full client IP in routine connection logs. The
        // trusted address remains in session attributes for abuse controls.
        log.info("Connection established: {}, and now online: {}",
                session.getId(), registeredSize);
        synchronized (session) {
            if (!isConnectionActiveLocked(session)) {
                return;
            }
            sessionService.send(session, MessageType.NOTICE,
                    Response.success((Object) null, "连接到服务器成功！"));

            // 1. send online
            Online online = new Online();
            // Read again at broadcast time so a concurrent join/close is not
            // represented by the count captured for the connection log.
            online.setCount(musicBar.getSessionCount(houseId));
            sessionService.send(
                    MessageType.ONLINE, Response.success(online), houseId);
        }

        Object connectType = session.getAttributes().get("connectType");

        if (JusicProperties.HOUSE_DEFAULT_ID.equals(houseId)
                || (connectType != null
                && !connectType.toString().isEmpty())) {
            synchronized (session) {
                if (!isConnectionActiveLocked(session)) {
                    return;
                }
                if (house != null && house.getAnnounce() != null
                        && house.getAnnounce().getContent() != null
                        && !house.getAnnounce().getContent().trim().isEmpty()) {
                    sessionService.send(session, MessageType.ANNOUNCEMENT,
                            Response.success(
                                    house.getAnnounce(), "房间公告"));
                } else {
                    sessionService.send(session, MessageType.ANNOUNCEMENT,
                            Response.success("", "房间公告"));
                }
            }
        }

        // 2. send playing
        Music playing = musicPlayingRepository.getPlaying(houseId);
        boolean isOver = playing == null || (musicJob != null && musicJob.isPlayingOver(houseId));
        Object playingSnapshot = null;
        if (playing != null && !isOver) {
            try {
                musicService.updateMusicUrl(playing);
            } catch (RuntimeException ex) {
                // Presence is already registered. Keep connection
                // initialization progressing with the last known snapshot.
                log.warn("连接 {} 初始化时播放链接刷新失败: {}",
                        session.getId(), ex.getMessage());
                log.debug("WebSocket 初始化播放链接刷新异常", ex);
            }
            synchronized (session) {
                if (!isConnectionActiveLocked(session)) {
                    return;
                }
                playing.setIps(null);
                playingSnapshot = Response.success(playing, "正在播放");
                sessionService.send(session, MessageType.MUSIC, playingSnapshot);
            }
        } else {
            // 如果房间初始无正在播放歌曲或上一首已播放完毕，开启推送开关并立即触发选歌推送
            if (configRepository != null) {
                configRepository.setPushSwitch(true, houseId);
            }
            if (musicJob != null) {
                musicJob.triggerHouse(houseId);
            }
        }

        // 3. send pick list
        LinkedList<Music> pickList = musicService.getPickList(houseId);
        synchronized (session) {
            if (!isConnectionActiveLocked(session)) {
                return;
            }
            Object pickSnapshot = Boolean.TRUE.equals(configService.getGoodModel(houseId))
                    ? Response.success(pickList, "goodlist")
                    : Response.success(pickList, "播放列表");
            sessionService.send(session, MessageType.PICK, pickSnapshot);
            publishInitialRoomSnapshot(
                    session, houseId, playingSnapshot, pickSnapshot);
        }
        if (playing != null && pickList != null) {
            log.info("发现有客户端连接, 已向该客户端: {} 发送正在播放的音乐: {}, 以及播放列表, 共 {} 首",
                    session.getId(), playing.getName(), pickList.size());
        }
    }

    /**
     * The browser installs its legacy direct-frame handler only from the STOMP
     * CONNECTED callback. A first legal subscription acknowledges that the
     * handler is ready, so replay the initial room state after that point.
     */
    public void clientMessageHandlerReady(String sessionId,
                                          String houseId,
                                          Map<String, Object> attributes) {
        InitialRoomSnapshot snapshot;
        synchronized (attributes) {
            attributes.put(CLIENT_MESSAGE_HANDLER_READY_ATTRIBUTE, Boolean.TRUE);
            snapshot = claimInitialRoomSnapshot(attributes);
        }
        replayInitialRoomSnapshot(sessionId, houseId, snapshot);
    }

    private void publishInitialRoomSnapshot(WebSocketSession session,
                                            String houseId,
                                            Object playingSnapshot,
                                            Object pickSnapshot) {
        Map<String, Object> attributes = session.getAttributes();
        InitialRoomSnapshot snapshot;
        synchronized (attributes) {
            attributes.put(INITIAL_ROOM_SNAPSHOT_ATTRIBUTE,
                    new InitialRoomSnapshot(playingSnapshot, pickSnapshot));
            snapshot = claimInitialRoomSnapshot(attributes);
        }
        replayInitialRoomSnapshot(session.getId(), houseId, snapshot);
    }

    private InitialRoomSnapshot claimInitialRoomSnapshot(
            Map<String, Object> attributes) {
        if (!Boolean.TRUE.equals(attributes.get(
                CLIENT_MESSAGE_HANDLER_READY_ATTRIBUTE))
                || Boolean.TRUE.equals(attributes.get(
                INITIAL_ROOM_SNAPSHOT_DELIVERED_ATTRIBUTE))) {
            return null;
        }
        Object value = attributes.get(INITIAL_ROOM_SNAPSHOT_ATTRIBUTE);
        if (!(value instanceof InitialRoomSnapshot)) {
            return null;
        }
        attributes.put(INITIAL_ROOM_SNAPSHOT_DELIVERED_ATTRIBUTE, Boolean.TRUE);
        return (InitialRoomSnapshot) value;
    }

    private void replayInitialRoomSnapshot(String sessionId,
                                           String houseId,
                                           InitialRoomSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (snapshot.playing != null) {
            sessionService.send(sessionId, MessageType.MUSIC,
                    snapshot.playing, houseId);
        }
        sessionService.send(sessionId, MessageType.PICK,
                snapshot.pickList, houseId);
        // Read current settings when the ready client receives its snapshot.
        sessionService.send(sessionId, MessageType.ROOM_STATE,
                Response.success(com.scoder.jusic.model.RoomState.read(configService, houseId)), houseId);
    }

    private static final class InitialRoomSnapshot {
        private final Object playing;
        private final Object pickList;

        private InitialRoomSnapshot(Object playing, Object pickList) {
            this.playing = playing;
            this.pickList = pickList;
        }
    }

    /**
     * Cleanup intentionally stays synchronous. Dropping it when an async queue
     * is full would leak local and Redis session state. The close marker is
     * cheap and is set by the decorator before delegating the close event.
     */
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        markConnectionClosed(session);
        if (!beginConnectionCleanup(session)) {
            return;
        }
        if (isDuplicateClientRejected(session)) {
            // This transport was rejected before local or Redis admission.
            // Its close callback must not touch the healthy first connection
            // that owns the same stable browser id.
            return;
        }
        // A transport can close after the browser id was reserved but before
        // room admission completed. Always release this node-local ownership;
        // unbind is identity-safe and is therefore also harmless when the
        // normal clearSession path releases it again.
        releaseConnectionReservation(session);
        String houseId = (String)session.getAttributes().get("houseId");
        if (houseId == null) {
            log.warn("Connection closed without house id: {}", session.getId());
            return;
        }
        sessionService.clearSession(session,houseId);
        int size = musicBar.getSessionCount(houseId);
        log.info("Connection closed: {}, and now online: {}", session.getId(), size);
        Online online = new Online();
        online.setCount(size);
        if(size != 0){
            sessionService.send(MessageType.ONLINE, Response.success(online, null),houseId);
        }else{
            houseContainer.destroyIfTemporaryAndEmpty(houseId);
        }
    }

    public void markConnectionClosed(WebSocketSession session) {
        synchronized (session) {
            session.getAttributes().put(CONNECTION_CLOSED_ATTRIBUTE, Boolean.TRUE);
        }
    }

    public boolean isConnectionActive(WebSocketSession session) {
        synchronized (session) {
            return isConnectionActiveLocked(session);
        }
    }

    private boolean beginConnectionCleanup(WebSocketSession session) {
        synchronized (session) {
            if (Boolean.TRUE.equals(
                    session.getAttributes().get(CONNECTION_CLEANED_ATTRIBUTE))) {
                return false;
            }
            session.getAttributes().put(CONNECTION_CLEANED_ATTRIBUTE, Boolean.TRUE);
            return true;
        }
    }

    private boolean isConnectionActiveLocked(WebSocketSession session) {
        return session.isOpen()
                && !Boolean.TRUE.equals(
                session.getAttributes().get(CONNECTION_CLOSED_ATTRIBUTE));
    }

    private String clientId(WebSocketSession session) {
        Object clientId = session.getAttributes().get(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE);
        if (clientId == null || clientId.toString().trim().isEmpty()) {
            // Compatibility for non-browser clients and older tests.
            clientId = session.getId();
            session.getAttributes().put(
                    ClientIdentityFilter.CLIENT_ID_ATTRIBUTE, clientId);
        }
        return clientId.toString();
    }

    private void rejectDuplicateClientConnection(
            WebSocketSession newcomer,
            WebSocketSession existing) throws Exception {
        synchronized (newcomer) {
            newcomer.getAttributes().put(
                    DUPLICATE_CLIENT_REJECTED_ATTRIBUTE, Boolean.TRUE);
            newcomer.getAttributes().put(
                    CONNECTION_CLOSED_ATTRIBUTE, Boolean.TRUE);
        }
        log.info("Rejected duplicate browser connection {}; active session remains {}",
                newcomer.getId(), existing == null ? "unknown" : existing.getId());
        if (newcomer.isOpen()) {
            newcomer.close(new CloseStatus(
                    DUPLICATE_CLIENT_CLOSE_CODE,
                    DUPLICATE_CLIENT_CLOSE_REASON));
        }
    }

}
