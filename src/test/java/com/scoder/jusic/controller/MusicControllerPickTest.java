package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.model.PickMusicResult;
import com.scoder.jusic.security.InboundPayloadValidator;
import com.scoder.jusic.security.ProviderCallExecutor;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import com.scoder.jusic.service.RoomCommandService;
import com.scoder.jusic.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MusicControllerPickTest {

    private static final String SESSION_ID = "session-1";
    private static final String HOUSE_ID = "house-1";

    @Mock
    private HouseContainer houseContainer;
    @Mock
    private JusicProperties jusicProperties;
    @Mock
    private MusicService musicService;
    @Mock
    private SessionService sessionService;
    @Mock
    private ConfigService configService;
    @Mock
    private RoomCommandService roomCommandService;
    @Mock
    private InboundPayloadValidator inboundPayloadValidator;
    @Mock
    private ProviderCallExecutor providerCallExecutor;

    @InjectMocks
    private MusicController controller;

    @BeforeEach
    void configureDependencies() {
        lenient().when(configService.getEnableSearch(HOUSE_ID)).thenReturn(true);
        lenient().when(providerCallExecutor.pick(any())).thenAnswer(invocation ->
                ((Callable<?>) invocation.getArgument(0)).call());
    }

    @Test
    void invalidRequestDoesNotReadRoomOrCallProviderOrBroadcast() {
        Music request = request();
        when(inboundPayloadValidator.validatePick(request)).thenReturn("点歌参数不能为空");

        controller.pick(request, accessor());

        verify(sessionService).send(eq(SESSION_ID), eq(MessageType.NOTICE), any(), eq(HOUSE_ID));
        verify(sessionService, never()).send(any(MessageType.class), any(), eq(HOUSE_ID));
        verifyNoInteractions(
                houseContainer, musicService, configService, jusicProperties, roomCommandService);
    }

    @Test
    void queueAtLimitRejectsBeforeProviderAndDoesNotBroadcastChat() {
        Music request = request();
        when(houseContainer.get(HOUSE_ID)).thenReturn(house());
        when(jusicProperties.getPlaylistSize()).thenReturn(3);
        when(musicService.noDefaultPlaylistSize(HOUSE_ID)).thenReturn(3L);

        controller.pick(request, accessor());

        verify(musicService, never()).getQQMusicById(any(), any());
        verifyNoInteractions(roomCommandService);
        verify(sessionService, never()).send(eq(MessageType.CHAT), any(), eq(HOUSE_ID));
        verify(sessionService).send(eq(SESSION_ID), eq(MessageType.NOTICE), any(), eq(HOUSE_ID));
    }

    @Test
    void queueWithOneSlotLeftCommitsThenKeepsLegacyBroadcastPayloads() {
        Music request = request();
        Music resolved = resolvedMusic();
        LinkedList<Music> pickList = new LinkedList<>();
        pickList.add(resolved);
        when(houseContainer.get(HOUSE_ID)).thenReturn(house());
        when(jusicProperties.getPlaylistSize()).thenReturn(3);
        when(musicService.noDefaultPlaylistSize(HOUSE_ID)).thenReturn(2L);
        when(musicService.getQQMusicById("song-1", "320k")).thenReturn(resolved);
        when(roomCommandService.commitPick(SESSION_ID, HOUSE_ID, "qq", resolved))
                .thenReturn(PickMusicResult.success(resolved, pickList));
        when(sessionService.getNickName(SESSION_ID, HOUSE_ID)).thenReturn("Alice");

        controller.pick(request, accessor());

        verify(roomCommandService).commitPick(SESSION_ID, HOUSE_ID, "qq", resolved);
        ArgumentCaptor<MessageType> types = ArgumentCaptor.forClass(MessageType.class);
        ArgumentCaptor<Object> payloads = ArgumentCaptor.forClass(Object.class);
        verify(sessionService, times(3)).send(types.capture(), payloads.capture(), eq(HOUSE_ID));
        assertEquals(Arrays.asList(MessageType.CHAT, MessageType.NOTICE, MessageType.PICK),
                types.getAllValues());
        Response<?> pickResponse = (Response<?>) payloads.getAllValues().get(2);
        assertSame(pickList, pickResponse.getData());
        assertEquals("20000", pickResponse.getCode());
        for (Object payload : payloads.getAllValues()) {
            assertFalse(payload instanceof PickMusicResult,
                    "internal command result must never be sent to the legacy frontend");
        }
    }

    @Test
    void duplicateResolvedSongDoesNotEnqueueOrBroadcast() {
        Music request = request();
        Music resolved = resolvedMusic();
        when(houseContainer.get(HOUSE_ID)).thenReturn(house());
        when(jusicProperties.getPlaylistSize()).thenReturn(3);
        when(musicService.noDefaultPlaylistSize(HOUSE_ID)).thenReturn(0L);
        when(musicService.getQQMusicById("song-1", "320k")).thenReturn(resolved);
        when(roomCommandService.commitPick(SESSION_ID, HOUSE_ID, "qq", resolved))
                .thenReturn(PickMusicResult.failure("点歌失败, 已在播放列表"));

        controller.pick(request, accessor());

        verify(roomCommandService).commitPick(SESSION_ID, HOUSE_ID, "qq", resolved);
        verify(sessionService, never()).send(any(MessageType.class), any(), eq(HOUSE_ID));
        verify(sessionService).send(eq(SESSION_ID), eq(MessageType.NOTICE), any(), eq(HOUSE_ID));
    }

    @Test
    void providerFailureIsRequesterOnlyAndDoesNotLeakAsChat() {
        Music request = request();
        when(houseContainer.get(HOUSE_ID)).thenReturn(house());
        when(jusicProperties.getPlaylistSize()).thenReturn(3);
        when(musicService.noDefaultPlaylistSize(HOUSE_ID)).thenReturn(0L);
        when(musicService.getQQMusicById("song-1", "320k"))
                .thenThrow(new IllegalStateException("provider token"));

        controller.pick(request, accessor());

        verify(sessionService).send(eq(SESSION_ID), eq(MessageType.NOTICE), any(), eq(HOUSE_ID));
        verify(sessionService, never()).send(any(MessageType.class), any(), eq(HOUSE_ID));
        verifyNoInteractions(roomCommandService);
    }

    private Music request() {
        Music music = new Music();
        music.setId("song-1");
        music.setName("Song One");
        music.setSource("qq");
        music.setQuality("320k");
        return music;
    }

    private Music resolvedMusic() {
        Music music = request();
        music.setUrl("https://music.invalid/song-1.mp3");
        music.setDuration(180_000L);
        return music;
    }

    private House house() {
        House house = new House();
        house.setId(HOUSE_ID);
        house.setEnableStatus(false);
        return house;
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
