package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.service.MusicCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MusicServiceImplUrlCacheTest {

    @Test
    void prefetchedUrlIsReusedWhenQueuedSongHasNoPickTime() {
        MusicCacheService cache = mock(MusicCacheService.class);
        when(cache.getPlayUrl("qq", "song-1", "320k"))
                .thenReturn("https://cdn.example/song-1.mp3");
        JusicProperties properties = new JusicProperties();
        properties.setMusicExpireTime(1_200_000L);
        MusicServiceImpl service = new MusicServiceImpl(mock(RestTemplate.class));
        ReflectionTestUtils.setField(service, "jusicProperties", properties);
        ReflectionTestUtils.setField(service, "musicCacheService", cache);

        Music music = new Music();
        music.setSource("qq");
        music.setId("song-1");
        music.setQuality("320k");

        service.updateMusicUrl(music);

        assertEquals("https://cdn.example/song-1.mp3", music.getUrl());
        assertNotNull(music.getPickTime());
        verify(cache).getPlayUrl("qq", "song-1", "320k");
    }
}
