package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.ConnectionAdmissionResult;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.OutboundMailboxProperties;
import com.scoder.jusic.configuration.RoomMusicLocks;
import com.scoder.jusic.configuration.SessionCommandLocks;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.MusicVoteRepository;
import com.scoder.jusic.repository.SessionBlackRepository;
import com.scoder.jusic.repository.SessionRepository;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.imp.SessionOutboundMailboxes;
import com.scoder.jusic.service.imp.SessionServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HouseControllerSessionLifecycleTest {

    private static final String SESSION_ID = "session-enter-failure";
    private static final String SOURCE_HOUSE_ID = "source-house";
    private static final String TARGET_HOUSE_ID = "target-house";
    private static final String CLIENT_IP = "192.0.2.30";

    @Test
    void failedEnterRollsBackToSourceWithoutClearAndKeepsEnterFailureShape() throws Exception {
        JusicProperties properties = new JusicProperties();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("houseId", SOURCE_HOUSE_ID);
        attributes.put("remoteAddress", CLIENT_IP);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(SESSION_ID);
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(attributes);

        ConnectionAdmissionResult registration = properties.getSession()
                .register(session, SOURCE_HOUSE_ID, CLIENT_IP);
        assertEquals(ConnectionAdmissionResult.Outcome.REGISTERED,
                registration.getOutcome());

        SessionRepository sessionRepository = mock(SessionRepository.class);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(sessionRepository)
                .setSession(any(User.class), eq(TARGET_HOUSE_ID));
        SessionOutboundMailboxes mailboxes = new SessionOutboundMailboxes(
                Runnable::run, new OutboundMailboxProperties());
        SessionServiceImpl sessionService = spy(new SessionServiceImpl(
                properties,
                sessionRepository,
                mock(SessionBlackRepository.class),
                mock(MusicVoteRepository.class),
                mailboxes,
                mock(com.scoder.jusic.configuration.ClientConnectionRegistry.class)
        ));

        House targetHouse = new House();
        targetHouse.setId(TARGET_HOUSE_ID);
        targetHouse.setNeedPwd(false);
        HouseContainer houseContainer = mock(HouseContainer.class);
        when(houseContainer.get(TARGET_HOUSE_ID)).thenReturn(targetHouse);

        HouseController controller = new HouseController();
        MusicService musicService = mock(MusicService.class);
        ReflectionTestUtils.setField(controller, "sessionService", sessionService);
        ReflectionTestUtils.setField(controller, "houseContainer", houseContainer);
        ReflectionTestUtils.setField(controller, "jusicProperties", properties);
        ReflectionTestUtils.setField(controller, "houseOperationLocks", new HouseOperationLocks());
        ReflectionTestUtils.setField(controller, "roomMusicLocks", new RoomMusicLocks());
        ReflectionTestUtils.setField(controller, "sessionCommandLocks", new SessionCommandLocks());
        ReflectionTestUtils.setField(controller, "musicService", musicService);

        controller.enterHouse(targetHouse, accessor(attributes));

        assertSame(session, properties.getSessions(SOURCE_HOUSE_ID).get(SESSION_ID));
        assertFalse(properties.getSessions(TARGET_HOUSE_ID).containsKey(SESSION_ID));
        assertEquals(SOURCE_HOUSE_ID, attributes.get("houseId"));

        verify(sessionService).putSession(session, TARGET_HOUSE_ID);
        verify(sessionService, never()).clearSession(
                any(WebSocketSession.class), anyString());
        verify(sessionService, never()).clearSession(anyString(), anyString());
        verify(musicService, never()).removeQueuedMusicBySession(
                anyString(), anyString());
        verify(sessionRepository, never()).removeSession(
                SESSION_ID, SOURCE_HOUSE_ID);
        verify(session, never()).close(any(CloseStatus.class));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(sessionService).send(
                eq(session), eq(MessageType.ENTER_HOUSE), payload.capture());
        Response<?> response = (Response<?>) payload.getValue();
        assertEquals("40000", response.getCode());
        assertNull(response.getData());
    }

    private static StompHeaderAccessor accessor(Map<String, Object> attributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId(SESSION_ID);
        accessor.setSessionAttributes(attributes);
        return accessor;
    }
}
