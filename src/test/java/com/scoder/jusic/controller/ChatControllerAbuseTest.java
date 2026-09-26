package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.Chat;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import com.scoder.jusic.security.InboundPayloadValidator;
import com.scoder.jusic.security.ProviderCallExecutor;
import com.scoder.jusic.service.ChatService;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerAbuseTest {

    private static final String SESSION_ID = "session-1";
    private static final String HOUSE_ID = "house-1";

    @Mock
    private SessionService sessionService;
    @Mock
    private ChatService chatService;
    @Mock
    private InboundPayloadValidator inboundPayloadValidator;
    @Mock
    private ProviderCallExecutor providerCallExecutor;

    @InjectMocks
    private ChatController controller;

    @Test
    void invalidChatIsRequesterOnlyFailureWithoutSideEffects() {
        Chat chat = new Chat();
        chat.setContent(" ");
        when(inboundPayloadValidator.validateChat(chat)).thenReturn("消息内容不能为空");

        controller.chat(chat, accessor());

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(sessionService).send(eq(SESSION_ID), eq(MessageType.NOTICE),
                payload.capture(), eq(HOUSE_ID));
        Response<?> response = (Response<?>) payload.getValue();
        assertEquals("40000", response.getCode());
        assertEquals("消息内容不能为空", response.getMessage());
        verify(sessionService, never()).send(eq(MessageType.CHAT), any(), eq(HOUSE_ID));
        verify(sessionService, never()).setLastMessageTime(any(), any(), any());
        verifyNoInteractions(chatService);
    }

    @Test
    void missingUserDoesNotThrowOrBroadcast() {
        Chat chat = new Chat();
        chat.setContent("hello");
        when(sessionService.getUser(SESSION_ID, HOUSE_ID)).thenReturn(null);

        controller.chat(chat, accessor());

        verify(sessionService).send(eq(SESSION_ID), eq(MessageType.NOTICE),
                any(), eq(HOUSE_ID));
        verify(sessionService, never()).send(eq(MessageType.CHAT), any(), eq(HOUSE_ID));
    }

    @Test
    void acceptedChatUsesServerIdentityAndTimestampAndKeepsLegacyPayload() {
        User user = User.builder()
                .sessionId(SESSION_ID)
                .name("Alice")
                .remoteAddress("192.0.2.10")
                .lastMessageTime(null)
                .build();
        when(sessionService.getUser(SESSION_ID, HOUSE_ID)).thenReturn(user);

        Chat chat = new Chat();
        chat.setContent("一起听歌");
        chat.setSessionId("forged-session");
        chat.setNickName("forged-name");
        chat.setSendTime(1L);
        long before = System.currentTimeMillis();

        controller.chat(chat, accessor());

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(sessionService).send(eq(MessageType.CHAT), payload.capture(), eq(HOUSE_ID));
        Response<?> response = (Response<?>) payload.getValue();
        assertEquals("20000", response.getCode());
        assertSame(chat, response.getData());
        assertEquals(SESSION_ID, chat.getSessionId());
        assertEquals(user.getNickName(), chat.getNickName());
        assertNotEquals(1L, chat.getSendTime());
        verify(sessionService).setLastMessageTime(eq(user), eq(chat.getSendTime()), eq(HOUSE_ID));
        org.junit.jupiter.api.Assertions.assertTrue(chat.getSendTime() >= before);
    }

    @Test
    void mentioningYourselfDeliversOneChatFrameInsteadOfTwo() {
        String selfSessionId = "session01";
        User user = User.builder()
                .sessionId(selfSessionId)
                .name("Alice")
                .remoteAddress("192.0.2.10")
                .lastMessageTime(null)
                .build();
        when(sessionService.getUser(selfSessionId, HOUSE_ID)).thenReturn(user);
        Chat chat = new Chat();
        chat.setContent("@" + selfSessionId + " hello");
        StompHeaderAccessor accessor = accessor();
        accessor.setSessionId(selfSessionId);

        controller.chat(chat, accessor);

        verify(sessionService, times(1)).send(eq(selfSessionId),
                eq(MessageType.CHAT), any(), eq(HOUSE_ID));
        verify(sessionService).setLastMessageTime(user, chat.getSendTime(), HOUSE_ID);
    }

    private StompHeaderAccessor accessor() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId(SESSION_ID);
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("houseId", HOUSE_ID);
        attributes.put("remoteAddress", "192.0.2.10");
        accessor.setSessionAttributes(attributes);
        return accessor;
    }
}
