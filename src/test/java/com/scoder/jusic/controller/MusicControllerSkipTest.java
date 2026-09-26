package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.job.MusicJob;
import com.scoder.jusic.model.Chat;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MusicControllerSkipTest {

    private static final String SESSION_ID = "session-1";
    private static final String HOUSE_ID = "house-1";

    @Mock
    private JusicProperties jusicProperties;
    @Mock
    private MusicService musicService;
    @Mock
    private SessionService sessionService;
    @Mock
    private ConfigService configService;
    @Mock
    private MusicJob musicJob;

    @InjectMocks
    private MusicController controller;

    @Test
    void disabledSwitchRejectsOrdinaryUserWithoutChatOrVoteMutation() {
        when(sessionService.getRole(SESSION_ID, HOUSE_ID)).thenReturn("default");
        when(configService.getEnableSwitch(HOUSE_ID)).thenReturn(false);

        controller.skip(accessor());

        assertRequesterNotice("40000", "禁止切歌");
        verify(sessionService, never()).send(eq(MessageType.CHAT), any(), eq(HOUSE_ID));
        verify(sessionService, never()).getNickName(SESSION_ID, HOUSE_ID);
        verify(configService, never()).setPushSwitch(anyBoolean(), eq(HOUSE_ID));
        verify(configService, never()).getVoteRate(HOUSE_ID);
        verifyNoInteractions(musicService, jusicProperties);
    }

    @Test
    void duplicateVoteNotifiesRequesterButDoesNotPublishChat() {
        when(sessionService.getRole(SESSION_ID, HOUSE_ID)).thenReturn("default");
        when(configService.getEnableSwitch(HOUSE_ID)).thenReturn(true);
        when(jusicProperties.getSessionCount(HOUSE_ID)).thenReturn(4);
        when(musicService.vote(SESSION_ID, HOUSE_ID)).thenReturn(0L);
        when(musicService.getVoteCount(HOUSE_ID)).thenReturn(1L);

        controller.skip(accessor());

        assertRequesterNotice("40000", "你已经投过票了,当前状态：1/4");
        verify(sessionService, never()).send(eq(MessageType.CHAT), any(), eq(HOUSE_ID));
        verify(sessionService, never()).getNickName(SESSION_ID, HOUSE_ID);
        verify(configService, never()).setPushSwitch(anyBoolean(), eq(HOUSE_ID));
        verify(configService, never()).getVoteRate(HOUSE_ID);
    }

    @Test
    void acceptedOrdinaryVoteKeepsLegacyChatAndNoticePayloads() {
        when(sessionService.getRole(SESSION_ID, HOUSE_ID)).thenReturn("default");
        when(sessionService.getNickName(SESSION_ID, HOUSE_ID)).thenReturn("Alice");
        when(configService.getEnableSwitch(HOUSE_ID)).thenReturn(true);
        when(jusicProperties.getSessionCount(HOUSE_ID)).thenReturn(4);
        when(musicService.vote(SESSION_ID, HOUSE_ID)).thenReturn(1L);
        when(musicService.getVoteCount(HOUSE_ID)).thenReturn(1L);
        when(configService.getVoteRate(HOUSE_ID)).thenReturn(0.5F);

        controller.skip(accessor());

        ArgumentCaptor<Object> chatPayload = ArgumentCaptor.forClass(Object.class);
        InOrder order = inOrder(musicService, sessionService);
        order.verify(musicService).vote(SESSION_ID, HOUSE_ID);
        order.verify(sessionService).send(eq(MessageType.CHAT), chatPayload.capture(), eq(HOUSE_ID));
        order.verify(sessionService).send(eq(MessageType.NOTICE), argThat(payload -> {
            Response<?> response = (Response<?>) payload;
            return "20000".equals(response.getCode())
                    && "1/4 投票成功".equals(response.getMessage())
                    && response.getData() == null;
        }), eq(HOUSE_ID));

        Response<?> chatResponse = (Response<?>) chatPayload.getValue();
        assertEquals("20000", chatResponse.getCode());
        Chat chat = (Chat) chatResponse.getData();
        assertEquals(SESSION_ID, chat.getSessionId());
        assertEquals("Alice", chat.getNickName());
        assertEquals("投票切歌", chat.getContent());
        assertTrue(chat.getSendTime() > 0);

        verify(sessionService).send(eq(MessageType.NOTICE), argThat(payload -> {
            Response<?> response = (Response<?>) payload;
            return "20000".equals(response.getCode())
                    && response.getMessage() != null
                    && response.getMessage().contains("有人希望切歌")
                    && response.getData() == null;
        }), eq(HOUSE_ID));
    }

    @Test
    void administratorCanSkipWhenVotingIsDisabledAndKeepsSuccessMessage() {
        when(sessionService.getRole(SESSION_ID, HOUSE_ID)).thenReturn("admin");
        when(sessionService.getNickName(SESSION_ID, HOUSE_ID)).thenReturn("Admin");
        when(configService.getEnableSwitch(HOUSE_ID)).thenReturn(false);

        controller.skip(accessor());

        InOrder order = inOrder(configService, sessionService);
        order.verify(configService).setPushSwitch(true, HOUSE_ID);
        verify(musicJob).wakeHouse(HOUSE_ID);
        order.verify(sessionService).send(eq(MessageType.CHAT), any(), eq(HOUSE_ID));
        ArgumentCaptor<Object> noticePayload = ArgumentCaptor.forClass(Object.class);
        order.verify(sessionService).send(eq(MessageType.NOTICE), noticePayload.capture(), eq(HOUSE_ID));

        Response<?> response = (Response<?>) noticePayload.getValue();
        assertEquals("20000", response.getCode());
        assertEquals("已接受切歌请求", response.getMessage());
        assertNull(response.getData());
        verifyNoInteractions(musicService);
    }

    @Test
    void voteReachingThresholdWakesPlaybackDespiteOlderBackoff() {
        when(sessionService.getRole(SESSION_ID, HOUSE_ID)).thenReturn("default");
        when(sessionService.getNickName(SESSION_ID, HOUSE_ID)).thenReturn("Alice");
        when(configService.getEnableSwitch(HOUSE_ID)).thenReturn(true);
        when(jusicProperties.getSessionCount(HOUSE_ID)).thenReturn(2);
        when(musicService.vote(SESSION_ID, HOUSE_ID)).thenReturn(1L);
        when(musicService.getVoteCount(HOUSE_ID)).thenReturn(1L);
        when(configService.getVoteRate(HOUSE_ID)).thenReturn(0.5F);

        controller.skip(accessor());

        verify(configService).setPushSwitch(true, HOUSE_ID);
        verify(musicJob).wakeHouse(HOUSE_ID);
    }

    private void assertRequesterNotice(String code, String message) {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(sessionService).send(
                eq(SESSION_ID), eq(MessageType.NOTICE), payload.capture(), eq(HOUSE_ID));
        Response<?> response = (Response<?>) payload.getValue();
        assertEquals(code, response.getCode());
        assertEquals(message, response.getMessage());
        assertNull(response.getData());
    }

    private StompHeaderAccessor accessor() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId(SESSION_ID);
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("houseId", HOUSE_ID);
        accessor.setSessionAttributes(attributes);
        return accessor;
    }
}
