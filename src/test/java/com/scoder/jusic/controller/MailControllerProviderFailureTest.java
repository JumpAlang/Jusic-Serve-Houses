package com.scoder.jusic.controller;

import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.model.Chat;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import com.scoder.jusic.security.ProviderCallException;
import com.scoder.jusic.service.MailService;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailControllerProviderFailureTest {

    @Test
    void notificationProviderFailuresStillSendTheLegacyNoticeEnvelope() {
        MailService mailService = mock(MailService.class);
        SessionService sessionService = mock(SessionService.class);
        HouseContainer houseContainer = mock(HouseContainer.class);
        User user = User.builder()
                .sessionId("session-1")
                .houseId("house-1")
                .remoteAddress("127.0.0.1")
                .nickName("tester")
                .build();
        when(sessionService.getUser("session-1", "house-1")).thenReturn(user);
        doThrow(new ProviderCallException("busy"))
                .when(mailService).sendServerJ(any(), any());
        doThrow(new ProviderCallException("timed out"))
                .when(mailService).sendSimpleMail(any(), any());

        MailController controller = new MailController();
        ReflectionTestUtils.setField(controller, "mailService", mailService);
        ReflectionTestUtils.setField(controller, "sessionService", sessionService);
        ReflectionTestUtils.setField(controller, "houseContainer", houseContainer);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId("session-1");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("houseId", "house-1");
        accessor.setSessionAttributes(attributes);

        assertDoesNotThrow(() -> controller.send(new Chat(), accessor));
        verify(sessionService).send(eq("session-1"), eq(MessageType.NOTICE),
                any(), eq("house-1"));
    }
}

