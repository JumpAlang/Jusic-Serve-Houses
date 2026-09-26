package com.scoder.jusic.interceptor;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.handler.JusicWebSocketHandlerAsync;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Online;
import com.scoder.jusic.model.User;
import com.scoder.jusic.security.AbuseAction;
import com.scoder.jusic.security.AbuseProtectionService;
import com.scoder.jusic.security.RateLimitDecision;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JusicWebSocketInterceptorContractTest {

    private static final String HOUSE_ID = "house-1";
    private static final String IP = "192.0.2.10";

    @Mock
    private SessionService sessionService;
    @Mock
    private HouseContainer houseContainer;
    @Mock
    private JusicProperties jusicProperties;
    @Mock
    private AbuseProtectionService abuseProtectionService;
    @Mock
    private JusicWebSocketHandlerAsync jusicWebSocketHandlerAsync;
    @Mock
    private MessageChannel channel;

    @InjectMocks
    private JusicWebSocketInterceptor interceptor;

    @ParameterizedTest
    @CsvSource({
            "/chat,CHAT,NOTICE",
            "/music/pick,PICK_MUSIC,NOTICE",
            "/music/search,SEARCH,NOTICE",
            "/house/add,ROOM_CREATE,ADD_HOUSE",
            "/house/enter,ROOM_ENTER,ENTER_HOUSE"
    })
    void denialUsesFrontendCompatibleRequesterOnlyMessageType(String destination,
                                                               AbuseAction action,
                                                               MessageType expectedType) {
        String sessionId = "session-" + action.name();
        Message<byte[]> message = message(StompCommand.SEND, destination, sessionId, true);
        when(houseContainer.get(HOUSE_ID)).thenReturn(new House());
        when(abuseProtectionService.check(AbuseAction.MESSAGE, sessionId, IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.allowed());
        when(abuseProtectionService.check(action, sessionId, IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.denied(2_500L));

        assertNull(interceptor.preSend(message, channel));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(sessionService).send(eq(sessionId), eq(expectedType), payload.capture(), eq(HOUSE_ID));
        Response<?> response = (Response<?>) payload.getValue();
        assertEquals("40000", response.getCode());
        assertEquals("操作过于频繁，请在3秒后重试", response.getMessage());
        verify(sessionService, never()).send(any(MessageType.class), any(), eq(HOUSE_ID));
    }

    @Test
    void lifecycleFramesAreNotChargedOrRejected() {
        Message<byte[]> connect = message(StompCommand.CONNECT, null, "session-connect", false);

        assertSame(connect, interceptor.preSend(connect, channel));

        verifyNoInteractions(houseContainer, sessionService, abuseProtectionService);
    }

    @Test
    void missingRoomDropsMalformedSendBeforeRedisLookups() {
        Message<byte[]> message = message(StompCommand.SEND, "/chat", "session-missing", false);

        assertNull(interceptor.preSend(message, channel));

        verify(houseContainer, never()).get(any());
        verifyNoInteractions(sessionService, abuseProtectionService);
    }

    @Test
    void unknownDestinationIsDroppedButStillConsumesTheGenericFrameQuota() {
        Message<byte[]> message = message(
                StompCommand.SEND, "/unknown/expensive", "session-unknown", true);
        when(houseContainer.get(HOUSE_ID)).thenReturn(new House());
        when(abuseProtectionService.check(
                AbuseAction.MESSAGE, "session-unknown", IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.allowed());

        assertNull(interceptor.preSend(message, channel));

        verify(abuseProtectionService).check(
                AbuseAction.MESSAGE, "session-unknown", IP, HOUSE_ID);
        verifyNoInteractions(sessionService);
    }

    @Test
    void blacklistedSenderKeepsLegacyNoticeAndDoesNotConsumeActionQuota() {
        Message<byte[]> message = message(StompCommand.SEND, "/chat", "session-black", true);
        when(houseContainer.get(HOUSE_ID)).thenReturn(new House());
        when(abuseProtectionService.check(AbuseAction.MESSAGE, "session-black", IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.allowed());
        when(abuseProtectionService.check(AbuseAction.CHAT, "session-black", IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.allowed());
        when(sessionService.getBlack("session-black", IP, HOUSE_ID)).thenReturn(new User());

        assertNull(interceptor.preSend(message, channel));

        verify(sessionService).send(eq("session-black"), eq(MessageType.NOTICE),
                any(), eq(HOUSE_ID));
        verify(abuseProtectionService).check(AbuseAction.CHAT, "session-black", IP, HOUSE_ID);
    }

    @Test
    void repeatedDeniedFramesDoNotAmplifyIntoOneResponsePerRequest() {
        Message<byte[]> message = message(StompCommand.SEND, "/chat", "session-flood", true);
        when(houseContainer.get(HOUSE_ID)).thenReturn(new House());
        when(abuseProtectionService.check(AbuseAction.MESSAGE, "session-flood", IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.allowed());
        when(abuseProtectionService.check(AbuseAction.CHAT, "session-flood", IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.denied(1_000L));

        assertNull(interceptor.preSend(message, channel));
        assertNull(interceptor.preSend(message, channel));

        verify(abuseProtectionService, times(2))
                .check(AbuseAction.CHAT, "session-flood", IP, HOUSE_ID);
        verify(sessionService, times(1)).send(eq("session-flood"), eq(MessageType.NOTICE),
                any(), eq(HOUSE_ID));
    }

    @Test
    void genericFrameDenialStopsBeforeTheActionSpecificBudget() {
        Message<byte[]> message = message(StompCommand.SEND, "/music/pick", "session-wide", true);
        when(houseContainer.get(HOUSE_ID)).thenReturn(new House());
        when(abuseProtectionService.check(AbuseAction.MESSAGE, "session-wide", IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.denied(1_000L));

        assertNull(interceptor.preSend(message, channel));

        verify(abuseProtectionService).check(
                AbuseAction.MESSAGE, "session-wide", IP, HOUSE_ID);
        verify(abuseProtectionService, never()).check(
                eq(AbuseAction.PICK_MUSIC), any(), any(), any());
        verify(sessionService).send(eq("session-wide"), eq(MessageType.NOTICE),
                any(), eq(HOUSE_ID));
    }

    @Test
    void knownLegacySubscriptionIsAllowedAndConsumesGenericBudget() {
        HashMap<String, Object> attributes = sessionAttributes();
        Message<byte[]> message = subscriptionMessage(
                StompCommand.SUBSCRIBE, "/topic/chat", "sub-1", attributes);
        when(houseContainer.get(HOUSE_ID)).thenReturn(new House());
        when(jusicProperties.getSessionCount(HOUSE_ID)).thenReturn(2);
        when(abuseProtectionService.check(
                AbuseAction.MESSAGE, "session-subscribe", IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.allowed());

        assertSame(message, interceptor.preSend(message, channel));

        verify(abuseProtectionService).check(
                AbuseAction.MESSAGE, "session-subscribe", IP, HOUSE_ID);
        ArgumentCaptor<Object> snapshot = ArgumentCaptor.forClass(Object.class);
        verify(sessionService).send(eq("session-subscribe"),
                eq(MessageType.ONLINE), snapshot.capture(), eq(HOUSE_ID));
        Response<?> response = (Response<?>) snapshot.getValue();
        assertEquals(2, ((Online) response.getData()).getCount());
        verify(jusicWebSocketHandlerAsync).clientMessageHandlerReady(
                "session-subscribe", HOUSE_ID, attributes);
    }

    @Test
    void onlyFirstSubscriptionSendsOnlineSnapshot() {
        HashMap<String, Object> attributes = sessionAttributes();
        when(houseContainer.get(HOUSE_ID)).thenReturn(new House());
        when(jusicProperties.getSessionCount(HOUSE_ID)).thenReturn(3);
        when(abuseProtectionService.check(
                AbuseAction.MESSAGE, "session-subscribe", IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.allowed());

        Message<byte[]> first = subscriptionMessage(
                StompCommand.SUBSCRIBE, "/topic/chat", "sub-1", attributes);
        assertSame(first, interceptor.preSend(first, channel));
        Message<byte[]> second = subscriptionMessage(
                StompCommand.SUBSCRIBE, "/topic/music/order", "sub-2", attributes);
        assertSame(second, interceptor.preSend(second, channel));

        verify(sessionService, times(1)).send(eq("session-subscribe"),
                eq(MessageType.ONLINE), any(), eq(HOUSE_ID));
        verify(jusicWebSocketHandlerAsync, times(1)).clientMessageHandlerReady(
                "session-subscribe", HOUSE_ID, attributes);
    }

    @Test
    void unknownSubscriptionIsChargedThenDroppedSilently() {
        Message<byte[]> message = subscriptionMessage(
                StompCommand.SUBSCRIBE, "/topic/attacker", "sub-x",
                sessionAttributes());
        when(houseContainer.get(HOUSE_ID)).thenReturn(new House());
        when(abuseProtectionService.check(
                AbuseAction.MESSAGE, "session-subscribe", IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.allowed());

        assertNull(interceptor.preSend(message, channel));

        verifyNoInteractions(sessionService);
    }

    @Test
    void ninthConcurrentSubscriptionIsRejectedAndUnsubscribeReleasesCapacity() {
        HashMap<String, Object> attributes = sessionAttributes();
        when(houseContainer.get(HOUSE_ID)).thenReturn(new House());
        when(abuseProtectionService.check(
                AbuseAction.MESSAGE, "session-subscribe", IP, HOUSE_ID))
                .thenReturn(RateLimitDecision.allowed());

        for (int index = 0; index < 8; index++) {
            Message<byte[]> allowed = subscriptionMessage(
                    StompCommand.SUBSCRIBE, "/topic/chat", "sub-" + index, attributes);
            assertSame(allowed, interceptor.preSend(allowed, channel));
        }
        Message<byte[]> denied = subscriptionMessage(
                StompCommand.SUBSCRIBE, "/topic/chat", "sub-8", attributes);
        assertNull(interceptor.preSend(denied, channel));

        Message<byte[]> unsubscribe = subscriptionMessage(
                StompCommand.UNSUBSCRIBE, null, "sub-0", attributes);
        assertSame(unsubscribe, interceptor.preSend(unsubscribe, channel));
        Message<byte[]> replacement = subscriptionMessage(
                StompCommand.SUBSCRIBE, "/topic/music/order", "sub-8", attributes);
        assertSame(replacement, interceptor.preSend(replacement, channel));
    }

    private Message<byte[]> message(StompCommand command,
                                    String destination,
                                    String sessionId,
                                    boolean includeHouse) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(sessionId);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        HashMap<String, Object> attributes = new HashMap<>();
        if (includeHouse) {
            attributes.put("houseId", HOUSE_ID);
            attributes.put("remoteAddress", IP);
        }
        accessor.setSessionAttributes(attributes);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private HashMap<String, Object> sessionAttributes() {
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("houseId", HOUSE_ID);
        attributes.put("remoteAddress", IP);
        return attributes;
    }

    private Message<byte[]> subscriptionMessage(StompCommand command,
                                                String destination,
                                                String subscriptionId,
                                                Map<String, Object> attributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-subscribe");
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setSubscriptionId(subscriptionId);
        accessor.setSessionAttributes(attributes);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
