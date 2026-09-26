package com.scoder.jusic.interceptor;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.handler.JusicWebSocketHandlerAsync;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Online;
import com.scoder.jusic.model.User;
import com.scoder.jusic.security.AbuseAction;
import com.scoder.jusic.security.AbuseProtectionService;
import com.scoder.jusic.security.RateLimitDecision;
import com.scoder.jusic.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

/**
 * @author H
 */
@Component
public class JusicWebSocketInterceptor implements ChannelInterceptor {

    private static final long RATE_LIMIT_NOTICE_INTERVAL_MS = 1_000L;
    private static final String LAST_RATE_LIMIT_NOTICE_ATTRIBUTE =
            JusicWebSocketInterceptor.class.getName() + ".lastRateLimitNotice";
    private static final String SUBSCRIPTIONS_ATTRIBUTE =
            JusicWebSocketInterceptor.class.getName() + ".subscriptions";
    private static final int MAX_SUBSCRIPTIONS_PER_SESSION = 8;

    @Autowired
    private SessionService sessionService;
    @Autowired
    private HouseContainer houseContainer;
    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private AbuseProtectionService abuseProtectionService;
    @Autowired
    private JusicWebSocketHandlerAsync jusicWebSocketHandlerAsync;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();
        if (StompCommand.SUBSCRIBE.equals(command)) {
            return allowSubscription(accessor, sessionId) ? message : null;
        }
        if (StompCommand.UNSUBSCRIBE.equals(command)) {
            removeSubscription(accessor);
            return message;
        }
        if (!StompCommand.SEND.equals(command)) {
            return message;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        String houseId = attribute(sessionAttributes, "houseId");
        String ip = attribute(sessionAttributes, "remoteAddress");

        if (sessionId == null || houseId == null || houseContainer.get(houseId) == null) {
            return null;
        }
        String destination = accessor.getDestination();
        boolean knownDestination = AbuseAction.isKnownDestination(destination);
        AbuseAction action = knownDestination
                ? AbuseAction.fromDestination(destination)
                : AbuseAction.MESSAGE;

        // Charge a coarse frame budget for every SEND, including unknown
        // destinations. Action-only limits can otherwise be bypassed by
        // rotating destinations, while unknown frames remain unmetered.
        RateLimitDecision decision = abuseProtectionService.check(
                AbuseAction.MESSAGE, sessionId, ip, houseId);
        if (!decision.isAllowed()) {
            if (knownDestination && shouldSendRateLimitNotice(sessionAttributes)) {
                sendRateLimitNotice(sessionId, houseId, action, decision);
            }
            return null;
        }

        if (!knownDestination) {
            return null;
        }

        if (!AbuseAction.MESSAGE.equals(action)) {
            decision = abuseProtectionService.check(action, sessionId, ip, houseId);
        }
        if (!decision.isAllowed()) {
            if (shouldSendRateLimitNotice(sessionAttributes)) {
                sendRateLimitNotice(sessionId, houseId, action, decision);
            }
            return null;
        }

        User black = sessionService.getBlack(sessionId, ip, houseId);
        if (black != null) {
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.failure((Object) null, "你已被管理员拉黑"),houseId);
            return null;
        }
//        System.out.println("presend"+message);
        return message;
    }

    private boolean allowSubscription(StompHeaderAccessor accessor,
                                      String sessionId) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        String houseId = attribute(attributes, "houseId");
        String ip = attribute(attributes, "remoteAddress");
        if (sessionId == null || houseId == null || houseContainer.get(houseId) == null) {
            return false;
        }

        // Subscription churn consumes the same coarse frame budget as SEND,
        // but is dropped silently to avoid response amplification.
        RateLimitDecision decision = abuseProtectionService.check(
                AbuseAction.MESSAGE, sessionId, ip, houseId);
        if (!decision.isAllowed()) {
            return false;
        }

        String destination = accessor.getDestination();
        String subscriptionId = accessor.getSubscriptionId();
        if (!isKnownSubscription(destination)
                || subscriptionId == null
                || subscriptionId.trim().isEmpty()) {
            return false;
        }

        boolean firstSubscription;
        synchronized (attributes) {
            Map<String, String> subscriptions = subscriptions(attributes, true);
            String existing = subscriptions.get(subscriptionId);
            if (existing != null) {
                return existing.equals(destination);
            }
            if (subscriptions.size() >= MAX_SUBSCRIPTIONS_PER_SESSION) {
                return false;
            }
            firstSubscription = subscriptions.isEmpty();
            subscriptions.put(subscriptionId, destination);
        }
        if (firstSubscription) {
            // The application sends legacy direct frames in addition to STOMP
            // messages. Initial room admission can finish before the browser's
            // STOMP CONNECTED callback installs its direct-frame handler, so
            // those initial room-state frames are occasionally missed. A legal
            // first subscription proves the handler is ready; send one
            // requester snapshot to close either ordering of that race.
            Online online = new Online();
            online.setCount(jusicProperties.getSessionCount(houseId));
            sessionService.send(sessionId, MessageType.ONLINE,
                    Response.success(online), houseId);
            jusicWebSocketHandlerAsync.clientMessageHandlerReady(
                    sessionId, houseId, attributes);
        }
        return true;
    }

    private void removeSubscription(StompHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        String subscriptionId = accessor.getSubscriptionId();
        if (attributes == null || subscriptionId == null) {
            return;
        }
        synchronized (attributes) {
            Map<String, String> subscriptions = subscriptions(attributes, false);
            if (subscriptions != null) {
                subscriptions.remove(subscriptionId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> subscriptions(Map<String, Object> attributes,
                                               boolean create) {
        Object current = attributes.get(SUBSCRIPTIONS_ATTRIBUTE);
        if (current instanceof Map) {
            return (Map<String, String>) current;
        }
        if (!create) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        attributes.put(SUBSCRIPTIONS_ATTRIBUTE, result);
        return result;
    }

    private boolean isKnownSubscription(String destination) {
        return "/topic/chat".equals(destination)
                || "/topic/music/order".equals(destination);
    }

    private void sendRateLimitNotice(String sessionId,
                                     String houseId,
                                     AbuseAction action,
                                     RateLimitDecision decision) {
        long retrySeconds = Math.max(1L, (decision.getRetryAfterMs() + 999L) / 1_000L);
        sessionService.send(sessionId, rejectionMessageType(action),
                Response.failure((Object) null, "操作过于频繁，请在" + retrySeconds + "秒后重试"),
                houseId);
    }

    private MessageType rejectionMessageType(AbuseAction action) {
        if (AbuseAction.ROOM_CREATE.equals(action)) {
            return MessageType.ADD_HOUSE;
        }
        if (AbuseAction.ROOM_ENTER.equals(action)) {
            return MessageType.ENTER_HOUSE;
        }
        return MessageType.NOTICE;
    }

    private boolean shouldSendRateLimitNotice(Map<String, Object> sessionAttributes) {
        long now = System.currentTimeMillis();
        synchronized (sessionAttributes) {
            Object value = sessionAttributes.get(LAST_RATE_LIMIT_NOTICE_ATTRIBUTE);
            long previous = value instanceof Number ? ((Number) value).longValue() : 0L;
            if (now - previous >= RATE_LIMIT_NOTICE_INTERVAL_MS) {
                sessionAttributes.put(LAST_RATE_LIMIT_NOTICE_ATTRIBUTE, now);
                return true;
            }
            return false;
        }
    }

    private String attribute(Map<String, Object> attributes, String name) {
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(name);
        return value == null ? null : String.valueOf(value);
    }

//    @Override
//    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
//        System.out.println("postsend"+message);
//    }
//
//    @Override
//    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
//        System.out.println("afterSendCompletion"+message);
//    }
//
//    @Override
//    public boolean preReceive(MessageChannel channel) {
//        System.out.println("preReceive"+channel);
//        return false;
//    }
//
//    @Override
//    public Message<?> postReceive(Message<?> message, MessageChannel channel) {
//        System.out.println("postReceive"+message);
//        return null;
//    }
//
//    @Override
//    public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
//        System.out.println("afterReceiveCompletion"+message);
//    }
}
