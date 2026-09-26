package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.service.MusicService;
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
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MusicControllerQueueMutationTest {

    private static final String SESSION_ID = "session-1";
    private static final String HOUSE_ID = "house-1";

    @Mock
    private MusicService musicService;
    @Mock
    private SessionService sessionService;

    @InjectMocks
    private MusicController controller;

    @Test
    void topRaceIsReportedAsFailureWithoutAStalePickBroadcast() {
        Music music = music("song-a");
        when(sessionService.getRole(SESSION_ID, HOUSE_ID)).thenReturn("admin");
        when(musicService.topPickMusic(music, HOUSE_ID)).thenReturn(false);

        controller.topPickMusic(music, accessor());

        assertRequesterNotice("40000", "歌曲已不在待播列表，请刷新后重试");
        verify(musicService, never()).getPickList(HOUSE_ID);
        verify(sessionService, never())
                .send(eq(MessageType.PICK), any(), eq(HOUSE_ID));
    }

    @Test
    void emptyOrFailedClearIsNotReportedAsSuccess() {
        when(sessionService.getRole(SESSION_ID, HOUSE_ID)).thenReturn("admin");
        when(musicService.clearPlayList(HOUSE_ID)).thenReturn(false);

        controller.clearPlayerList(accessor());

        assertRequesterNotice("40000", "待播列表已为空或清空失败");
        verify(musicService, never()).getPlaying(HOUSE_ID);
        verify(sessionService, never())
                .send(eq(MessageType.PICK), any(), eq(HOUSE_ID));
    }

    @Test
    void blankDeleteRequestIsRejectedBeforeQueueAccess() {
        controller.deletePickMusic(music(" "), accessor());

        assertRequesterNotice("40000", "请输入完整歌曲信息");
        verifyNoInteractions(musicService);
        verify(sessionService, never()).getRole(SESSION_ID, HOUSE_ID);
    }

    @Test
    void successfulTopKeepsTheLegacyNoticeAndPickResponseShapes() {
        Music music = music("song-a");
        LinkedList<Music> snapshot = new LinkedList<>();
        snapshot.add(music);
        when(sessionService.getRole(SESSION_ID, HOUSE_ID)).thenReturn("admin");
        when(musicService.topPickMusic(music, HOUSE_ID)).thenReturn(true);
        when(musicService.getPickList(HOUSE_ID)).thenReturn(snapshot);

        controller.topPickMusic(music, accessor());

        assertRequesterNotice("20000", "置顶成功");
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(sessionService).send(
                eq(MessageType.PICK), payload.capture(), eq(HOUSE_ID));
        Response<?> response = (Response<?>) payload.getValue();
        assertEquals("20000", response.getCode());
        assertEquals("置顶后的播放列表", response.getMessage());
        assertSame(snapshot, response.getData());
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

    private Music music(String id) {
        Music music = new Music();
        music.setId(id);
        music.setName(id);
        return music;
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
