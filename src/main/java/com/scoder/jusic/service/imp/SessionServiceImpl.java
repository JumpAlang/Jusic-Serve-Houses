package com.scoder.jusic.service.imp;

import com.alibaba.fastjson.JSON;
import com.scoder.jusic.configuration.ClientConnectionRegistry;
import com.scoder.jusic.configuration.ConnectionAdmissionResult;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.RoomMusicLocks;
import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.handler.JusicWebSocketHandlerAsync;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.repository.SessionBlackRepository;
import com.scoder.jusic.repository.SessionRepository;
import com.scoder.jusic.service.SessionService;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.security.RoomRolePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author H
 */
@Service
@Slf4j
public class SessionServiceImpl implements SessionService {

    private static final long CLEANUP_FAILURE_LOG_INTERVAL_MS = 10_000L;
    public static final String ROOM_ROLE_ATTRIBUTE =
            SessionServiceImpl.class.getName() + ".roomRole";

    private final JusicProperties jusicProperties;
    private final SessionRepository sessionRepository;
    private final SessionBlackRepository sessionBlackRepository;
    private final MusicVoteRepository musicVoteRepository;
    private final SessionOutboundMailboxes outboundMailboxes;
    private final ClientConnectionRegistry clientConnectionRegistry;
    private final AtomicLong lastCleanupFailureLog = new AtomicLong();
    @Autowired(required = false)
    private ConfigService configService;
    @Autowired(required = false)
    @Lazy
    private MusicService musicService;
    @Autowired(required = false)
    private RoomMusicLocks roomMusicLocks;

    @Autowired
    public SessionServiceImpl(JusicProperties jusicProperties,
                              SessionRepository sessionRepository,
                              SessionBlackRepository sessionBlackRepository,
                              MusicVoteRepository musicVoteRepository,
                              SessionOutboundMailboxes outboundMailboxes,
                              ClientConnectionRegistry clientConnectionRegistry) {
        this.jusicProperties = jusicProperties;
        this.sessionRepository = sessionRepository;
        this.sessionBlackRepository = sessionBlackRepository;
        this.musicVoteRepository = musicVoteRepository;
        this.outboundMailboxes = outboundMailboxes;
        this.clientConnectionRegistry = clientConnectionRegistry;
    }

    @Override
    public void settingName(String sessionId, String name,String houseId) {
        if (name == null) {
            return;
        }
        log.info("setting name: {}", name);
        User user = sessionRepository.getSession(sessionId,houseId);
        if (user == null) {
            log.debug("Ignoring nickname update for missing session {} in house {}",
                    sessionId, houseId);
            return;
        }
        user.setName(name);
        user.setNickName(user.getNickName());
        sessionRepository.setSession(user,houseId);
    }

    @Override
    public String getRole(String sessionId,String houseId) {
        User session = sessionRepository.getSession(sessionId,houseId);
        if(session != null){
            return session.getRole();
        }else{
            return "";
        }
    }

    @Override
    public User  putSession(WebSocketSession session,String houseId) {
        if (session == null || !session.isOpen()
                || Boolean.TRUE.equals(session.getAttributes().get(
                JusicWebSocketHandlerAsync.CONNECTION_CLOSED_ATTRIBUTE))) {
            throw new IllegalStateException("cannot register a closed WebSocket session");
        }
        Object remoteAddress = session.getAttributes().get("remoteAddress");
        String clientIp = remoteAddress == null ? "unknown" : remoteAddress.toString();
        ConnectionAdmissionResult admission = jusicProperties.getSession()
                .move(session, houseId, clientIp);
        String previousHouseId = admission.getPreviousHouseId();
        boolean moved = admission.getOutcome() == ConnectionAdmissionResult.Outcome.MOVED;
        if (moved) {
            // Frames already queued for the old room must never arrive after
            // the membership switch.
            outboundMailboxes.discard(session);
        }
        session.getAttributes().put("houseId", houseId);
        String initialRole = session.getId().equals(houseId)?"admin":"default";
        session.getAttributes().put(ROOM_ROLE_ATTRIBUTE, initialRole);
        User user = User.builder()
                .sessionId(session.getId())
                .name("")
                .nickName("")
                .remoteAddress(clientIp)
                .joinedAt(System.currentTimeMillis())
                .role(initialRole).houseId(houseId)
                .build();
        try {
            sessionRepository.setSession(user,houseId);
        } catch (RuntimeException ex) {
            // Never leave a half-initialized local connection when Redis
            // registration fails.
            try {
                rollbackAdmission(session, admission, clientIp);
            } catch (RuntimeException rollbackFailure) {
                // A rollback can itself be rejected when the old room filled
                // concurrently. In that case, remove the target registration
                // so the failed Redis write never leaves a local ghost.
                try {
                    jusicProperties.getSession().remove(session);
                } catch (RuntimeException removalFailure) {
                    rollbackFailure.addSuppressed(removalFailure);
                }
                failClosedAfterRollback(session, admission, rollbackFailure);
                ex.addSuppressed(rollbackFailure);
                logCleanupFailure("local-rollback", session.getId(), houseId,
                        rollbackFailure);
            }
            outboundMailboxes.discard(session);
            throw ex;
        }
        if (moved && previousHouseId != null && !previousHouseId.equals(houseId)) {
            cleanupRedisMembership(session.getId(), previousHouseId);
        }
        return user;
    }

    private void rollbackAdmission(WebSocketSession session,
                                   ConnectionAdmissionResult admission,
                                   String clientIp) {
        if (admission.getOutcome() == ConnectionAdmissionResult.Outcome.REGISTERED) {
            jusicProperties.getSession().remove(session);
            return;
        }
        if (admission.getOutcome() == ConnectionAdmissionResult.Outcome.MOVED
                && admission.getPreviousHouseId() != null) {
            jusicProperties.getSession().move(
                    session, admission.getPreviousHouseId(), clientIp);
            session.getAttributes().put("houseId", admission.getPreviousHouseId());
        }
    }

    private void failClosedAfterRollback(WebSocketSession session,
                                         ConnectionAdmissionResult admission,
                                         RuntimeException rollbackFailure) {
        String previousHouseId = admission.getPreviousHouseId();
        if (previousHouseId != null) {
            session.getAttributes().put("houseId", previousHouseId);
        }
        session.getAttributes().put(
                JusicWebSocketHandlerAsync.CONNECTION_CLOSED_ATTRIBUTE, Boolean.TRUE);
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception closeFailure) {
            rollbackFailure.addSuppressed(closeFailure);
            log.warn("Failed to close session {} after membership rollback failed: {}",
                    session.getId(), closeFailure.getMessage());
            log.debug("WebSocket close failure after membership rollback", closeFailure);
        }
    }

    private void cleanupRedisMembership(String sessionId, String houseId) {
        try {
            sessionRepository.removeSession(sessionId, houseId);
        } catch (RuntimeException ex) {
            logCleanupFailure("redis-session", sessionId, houseId, ex);
        }
        try {
            musicVoteRepository.remove(sessionId, houseId);
        } catch (RuntimeException ex) {
            logCleanupFailure("redis-vote", sessionId, houseId, ex);
        }
    }

    @Override
    public void clearSession(WebSocketSession session,String houseId) {
        if (session == null) {
            return;
        }
        // All production registration/move paths use the same session monitor.
        // Cleanup therefore cannot interleave between local admission and its
        // Redis write, which would otherwise recreate stale Redis presence.
        synchronized (session) {
            clearSessionResources(session.getId(), houseId, session);
        }
    }

    @Override
    public WebSocketSession clearSession(String sessionId,String houseId) {
        return clearSessionResources(sessionId, houseId, null);
    }

    private WebSocketSession clearSessionResources(String sessionId,
                                                   String houseId,
                                                   WebSocketSession sessionHint) {
        if (sessionId == null || houseId == null) {
            if (sessionHint != null) {
                outboundMailboxes.discard(sessionHint);
            } else if (sessionId != null) {
                outboundMailboxes.discard(sessionId);
            }
            return sessionHint;
        }

        User departingUser = readDepartingUserForQueueCleanup(
                sessionId, houseId, sessionHint);
        WebSocketSession removed = null;
        ConnectionAdmissionResult removal = null;
        try {
            removal = sessionHint == null
                    ? jusicProperties.getSession().remove(sessionId, houseId)
                    : jusicProperties.getSession().remove(sessionHint);
            removed = removal.getSession();
        } catch (RuntimeException ex) {
            logCleanupFailure("local-session", sessionId, houseId, ex);
        }

        WebSocketSession indexedSession = removed != null ? removed : sessionHint;
        if (indexedSession != null) {
            try {
                clientConnectionRegistry.unbind(indexedSession);
            } catch (RuntimeException ex) {
                logCleanupFailure("client-connection-index", sessionId, houseId, ex);
            }
        }

        // A delayed id/house cleanup may refer to the room a live session has
        // already left. Never discard the current session's mailbox or remove
        // its new-room Redis presence in that case; only clean the stale room.
        if (removal != null
                && removal.getOutcome() == ConnectionAdmissionResult.Outcome.STALE_SESSION) {
            if (sessionHint != null) {
                try {
                    outboundMailboxes.discard(sessionHint);
                } catch (RuntimeException ex) {
                    logCleanupFailure("outbound-mailbox", sessionId, houseId, ex);
                }
                return sessionHint;
            }
            cleanupRedisMembership(sessionId, houseId);
            return null;
        }

        WebSocketSession mailboxSession = removed != null ? removed : sessionHint;
        try {
            if (mailboxSession == null) {
                outboundMailboxes.discard(sessionId);
            } else {
                outboundMailboxes.discard(mailboxSession);
            }
        } catch (RuntimeException ex) {
            logCleanupFailure("outbound-mailbox", sessionId, houseId, ex);
        }

        // Each cleanup step is independent and idempotent. A Redis outage in
        // one repository must not retain the local socket or skip vote cleanup.
        cleanupRedisMembership(sessionId, houseId);
        if (removal != null
                && removal.getOutcome() == ConnectionAdmissionResult.Outcome.REMOVED) {
            cleanupDepartedUserQueue(departingUser, houseId);
        }
        return removed != null ? removed : sessionHint;
    }

    private User readDepartingUserForQueueCleanup(String sessionId,
                                                   String houseId,
                                                   WebSocketSession sessionHint) {
        if (configService == null || musicService == null || roomMusicLocks == null) {
            return null;
        }
        try {
            if (Boolean.FALSE.equals(configService.getClearQueuedOnLeave(houseId))) {
                return null;
            }
            User user = sessionRepository.getSession(sessionId, houseId);
            if (user == null) {
                WebSocketSession roleSource = sessionHint != null
                        ? sessionHint
                        : jusicProperties.getSessionIfPresent(houseId, sessionId);
                Object cachedRole = roleSource == null ? null
                        : roleSource.getAttributes().get(ROOM_ROLE_ATTRIBUTE);
                if (cachedRole != null) {
                    user = User.builder().sessionId(sessionId).houseId(houseId)
                            .role(cachedRole.toString()).build();
                }
            }
            return user != null && !RoomRolePolicy.isPrivileged(user.getRole())
                    ? user : null;
        } catch (RuntimeException ex) {
            logCleanupFailure("departure-queue-policy", sessionId, houseId, ex);
            return null;
        }
    }

    private void cleanupDepartedUserQueue(User departingUser, String houseId) {
        if (departingUser == null) {
            return;
        }
        ReentrantLock musicLock = roomMusicLocks.getLock(houseId);
        musicLock.lock();
        try {
            long removed = musicService.removeQueuedMusicBySession(
                    departingUser.getSessionId(), houseId);
            if (removed > 0L) {
                LinkedList<Music> pickList = musicService.getPickList(houseId);
                send(MessageType.PICK,
                        Response.success(pickList, "点歌列表"), houseId);
            }
        } catch (RuntimeException ex) {
            logCleanupFailure("departure-queue", departingUser.getSessionId(),
                    houseId, ex);
        } finally {
            musicLock.unlock();
        }
    }

    private void logCleanupFailure(String resource,
                                   String sessionId,
                                   String houseId,
                                   RuntimeException ex) {
        long now = System.currentTimeMillis();
        long previous = lastCleanupFailureLog.get();
        if (now - previous >= CLEANUP_FAILURE_LOG_INTERVAL_MS
                && lastCleanupFailureLog.compareAndSet(previous, now)) {
            log.warn("WebSocket cleanup failed for resource {}, session {}, house {}: {}",
                    resource, sessionId, houseId, ex.getMessage());
        }
        log.debug("WebSocket cleanup failure for resource " + resource, ex);
    }

    @Override
    public void send(Object payload,String houseId) {
        this.send(MessageType.NOTICE, payload,houseId);
    }

    @Override
    public void send(MessageType messageType, Object payload,String houseId) {
        Map<String, WebSocketSession> sessions = jusicProperties.getSession()
                .getHouseSessionIfPresent(houseId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        SessionOutboundMailboxes.PreparedMessage textMessage =
                createTextMessage(messageType, payload);
        sessions.forEach((key, session) -> {
            if (session.isOpen()) {
                dispatch(session, textMessage);
            } else {
                clearSession(session, houseId);
            }
        });
    }

    @Override
    public void send(MessageType messageType, Object payload) {
        JusicProperties.SessionContainer sessions = jusicProperties.getSessions();
        Map<String,ConcurrentHashMap<String,WebSocketSession>> housesSession = sessions.get();
        if (housesSession.isEmpty()) {
            return;
        }
        SessionOutboundMailboxes.PreparedMessage textMessage =
                createTextMessage(messageType, payload);
        Set<String> houseIdSet = housesSession.keySet();
        Iterator<String> iterator = houseIdSet.iterator();
        while(iterator.hasNext()){
            String houseId = iterator.next();
            ConcurrentHashMap<String,WebSocketSession> houseSession = housesSession.get(houseId);
            if (houseSession == null) {
                continue;
            }
            houseSession.forEach((key, session) -> {
                if (session.isOpen()) {
                    dispatch(session, textMessage);
                } else {
                    clearSession(session, houseId);
                }
            });
        }
    }

    @Override
    public void send(String sessionId, Object payload,String houseId) {
        this.send(sessionId, MessageType.NOTICE, payload,houseId);
    }

    @Override
    public void send(String sessionId, MessageType messageType, Object payload,String houseId) {
        Map<String, WebSocketSession> sessions = jusicProperties.getSession()
                .getHouseSessionIfPresent(houseId);
        if (sessions == null) {
            return;
        }
        WebSocketSession session = sessions.get(sessionId);
        if(session != null){
            if (session.isOpen()) {
                this.send(session, messageType, payload);
            } else {
                clearSession(session, houseId);
            }
        }
    }

    @Override
    public void send(WebSocketSession session, Object payload) {
        this.send(session, MessageType.NOTICE, payload);
    }

    @Override
    public void send(WebSocketSession session, MessageType messageType, Object payload) {
        dispatch(session, createTextMessage(messageType, payload));
    }

    private SessionOutboundMailboxes.PreparedMessage createTextMessage(
            MessageType messageType, Object payload) {
        return SessionOutboundMailboxes.PreparedMessage.from(
                new TextMessage(this.getPayload(messageType.type(), payload)));
    }

    private void dispatch(WebSocketSession session,
                          SessionOutboundMailboxes.PreparedMessage textMessage) {
        SessionOutboundMailboxes.DispatchResult result = outboundMailboxes.dispatch(session, textMessage);
        if (result.requiresPresenceCleanup()) {
            Object houseId = session.getAttributes().get("houseId");
            if (houseId == null) {
                outboundMailboxes.discard(session);
                log.warn("Discarded outbound session {} without a house after {}",
                        session.getId(), result);
            } else {
                clearSession(session, houseId.toString());
            }
        }
    }

    @Override
    public String getNickName(String sessionId,String houseId) {
        User session = sessionRepository.getSession(sessionId,houseId);
        if(session != null){
            return session.getNickName();
        }else{
            return "";
        }
    }

    @Override
    public void setLastMessageTime(User user, Long time,String houseId) {
        user.setLastMessageTime(time);
        sessionRepository.setSession(user,houseId);
    }

    @Override
    public User getUser(String sessionId,String houseId) {
        return sessionRepository.getSession(sessionId,houseId);
    }

    @Override
    public void black(User user,String houseId) {
        sessionBlackRepository.setSession(user,houseId);
    }

    @Override
    public String showBlackUser(String houseId) {
        Set blackList = sessionBlackRepository.showBlackList(houseId);
        if(blackList != null && blackList.size() > 0) {
            return String.join(",", blackList);
        }
        return null;
    }



    @Override
    public User getBlack(String sessionId,String ip,String houseId) {
        return sessionBlackRepository.getSession(sessionId,ip,houseId);
    }

    @Override
    public void unblack(String sessionId,String houseId) {
        sessionBlackRepository.removeSession(sessionId,houseId);
    }

    @Override
    public Long size(String houseId) {
        return sessionRepository.size(houseId);
    }

    @Override
    public List getSession(String houseId) {
        return sessionRepository.getSession(houseId);
    }

    /**
     * 自定义消息格式，模仿 stomp 的格式
     * </p>
     * 详见：http://stomp.github.io/stomp-specification-1.2.html#MESSAGE
     *
     * @param content payload 要发送的消息内容
     * @return 格式化后的字符串，可以直接作为消息发送出去
     */
    private String getPayload(String messageType, Object content) {
        StringBuilder payload = new StringBuilder();
        String jsonString = JSON.toJSONString(content);
        payload.append(messageType)
                .append("\n")
                .append("content-type:application/json");
        payload.append("\n")
                .append("content-length:")
                .append(jsonString.getBytes(StandardCharsets.UTF_8).length);
        payload.append("\n")
                .append("\n")
                .append(jsonString);
        return payload.toString();
    }

}
