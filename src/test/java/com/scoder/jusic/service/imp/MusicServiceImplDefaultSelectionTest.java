package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.HouseOperationLocks;
import com.scoder.jusic.configuration.RoomMusicLocks;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.repository.MusicDefaultRepository;
import com.scoder.jusic.repository.MusicPickRepository;
import com.scoder.jusic.repository.MusicPlayingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MusicServiceImplDefaultSelectionTest {

    private static final String HOUSE_ID = "default-selection-room";
    private static final int MAX_DEFAULT_ATTEMPTS = 8;

    @Test
    void blankProviderUrlsStopAtTheBoundAndUseTheLegacyFallback() {
        Fixture fixture = fixture();
        AtomicInteger providerCalls = new AtomicInteger();
        doAnswer(invocation -> {
            assertProviderRunsOutsideRoomLocks(fixture);
            return music(providerCalls.incrementAndGet() > MAX_DEFAULT_ATTEMPTS
                        ? "https://music.invalid/too-late.mp3"
                        : "",
                    "wy");
        })
                .when(fixture.service)
                .getWYMusicById(eq("bad-track"), isNull());
        Music fallback = music("https://music.invalid/fallback.mp3", "lz");
        doReturn(fallback).when(fixture.service).getLZMusic(251);

        Music selected = fixture.service.musicSwitch(HOUSE_ID);

        assertSame(fallback, selected);
        assertEquals(MAX_DEFAULT_ATTEMPTS, providerCalls.get());
        verify(fixture.defaultRepository, times(MAX_DEFAULT_ATTEMPTS))
                .remove("bad-track", HOUSE_ID);
        verify(fixture.service).getLZMusic(251);
    }

    @Test
    void repeatedNullProviderResultsStopAtTheBoundAndReturnNullWhenFallbackFails() {
        Fixture fixture = fixture();
        doReturn(null).when(fixture.service)
                .getWYMusicById(eq("bad-track"), isNull());
        doReturn(null).when(fixture.service).getLZMusic(251);

        Music selected = fixture.service.musicSwitch(HOUSE_ID);

        assertNull(selected);
        verify(fixture.service, times(MAX_DEFAULT_ATTEMPTS))
                .getWYMusicById(eq("bad-track"), isNull());
        verify(fixture.service).getLZMusic(251);
    }

    @Test
    void nullKeywordsAreBoundedAndNeverReachAProviderParser() {
        Fixture fixture = fixture();
        when(fixture.defaultRepository.nextMember(HOUSE_ID, HOUSE_ID, true)).thenReturn(null);
        Music fallback = music("https://music.invalid/fallback.mp3", "lz");
        doReturn(fallback).when(fixture.service).getLZMusic(251);

        Music selected = fixture.service.musicSwitch(HOUSE_ID);

        assertSame(fallback, selected);
        verify(fixture.defaultRepository, times(MAX_DEFAULT_ATTEMPTS))
                .nextMember(anyString(), eq(HOUSE_ID), eq(true));
        verify(fixture.service, never()).getWYMusicById(anyString(), any());
        verify(fixture.service, never()).getQQMusicById(anyString(), any());
    }

    private static Fixture fixture() {
        MusicServiceImpl service = spy(new MusicServiceImpl(new RestTemplate()));
        MusicPickRepository pickRepository = mock(MusicPickRepository.class);
        MusicPlayingRepository playingRepository = mock(MusicPlayingRepository.class);
        MusicDefaultRepository defaultRepository = mock(MusicDefaultRepository.class);
        ConfigRepository configRepository = mock(ConfigRepository.class);
        HouseOperationLocks houseOperationLocks = new HouseOperationLocks();
        RoomMusicLocks roomMusicLocks = new RoomMusicLocks();

        when(pickRepository.size(HOUSE_ID)).thenReturn(0L);
        when(playingRepository.getPlaying(HOUSE_ID)).thenReturn(null);
        when(defaultRepository.size(HOUSE_ID)).thenReturn(1L);
        when(defaultRepository.nextMember(HOUSE_ID, HOUSE_ID, true)).thenReturn("bad-track");
        when(configRepository.getDefaultPlaylistRandom(HOUSE_ID)).thenReturn(true);

        ReflectionTestUtils.setField(service, "musicPickRepository", pickRepository);
        ReflectionTestUtils.setField(service, "musicPlayingRepository", playingRepository);
        ReflectionTestUtils.setField(service, "musicDefaultRepository", defaultRepository);
        ReflectionTestUtils.setField(service, "configRepository", configRepository);
        ReflectionTestUtils.setField(service, "houseOperationLocks", houseOperationLocks);
        ReflectionTestUtils.setField(service, "roomMusicLocks", roomMusicLocks);
        Fixture fixture = new Fixture(service, defaultRepository,
                houseOperationLocks, roomMusicLocks);
        doAnswer(invocation -> {
            assertProviderRunsOutsideRoomLocks(fixture);
            return null;
        }).when(service).updateMusicUrl(any(Music.class));
        return fixture;
    }

    private static void assertProviderRunsOutsideRoomLocks(Fixture fixture) {
        assertFalse(fixture.houseOperationLocks.getLock(HOUSE_ID)
                .isHeldByCurrentThread());
        assertFalse(fixture.roomMusicLocks.getLock(HOUSE_ID)
                .isHeldByCurrentThread());
    }

    private static Music music(String url, String source) {
        Music music = new Music();
        music.setId("resolved-track");
        music.setUrl(url);
        music.setSource(source);
        return music;
    }

    private static final class Fixture {
        private final MusicServiceImpl service;
        private final MusicDefaultRepository defaultRepository;
        private final HouseOperationLocks houseOperationLocks;
        private final RoomMusicLocks roomMusicLocks;

        private Fixture(MusicServiceImpl service,
                        MusicDefaultRepository defaultRepository,
                        HouseOperationLocks houseOperationLocks,
                        RoomMusicLocks roomMusicLocks) {
            this.service = service;
            this.defaultRepository = defaultRepository;
            this.houseOperationLocks = houseOperationLocks;
            this.roomMusicLocks = roomMusicLocks;
        }
    }
}
