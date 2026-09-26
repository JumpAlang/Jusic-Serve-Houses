package com.scoder.jusic.service.imp;

import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.model.Music;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.RedisSystemException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MusicCacheServiceImplTest {

    @Mock
    private RedisTemplate redisTemplate;

    @Mock
    private ValueOperations valueOperations;

    @InjectMocks
    private MusicCacheServiceImpl musicCacheService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetAndPutSong() {
        Music music = new Music();
        music.setId("song-123");
        music.setName("Test Song");
        music.setArtist("Test Artist");

        when(valueOperations.get("jusic:cache:song:qq:song-123")).thenReturn(music);

        Music cached = musicCacheService.getSong("qq", "song-123");
        assertNotNull(cached);
        assertEquals("song-123", cached.getId());
        assertEquals("Test Song", cached.getName());

        musicCacheService.putSong("qq", "song-123", music);
        verify(valueOperations).set(eq("jusic:cache:song:qq:song-123"), eq(music), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetAndPutPlayUrl() {
        String testUrl = "http://music.com/play/123";
        when(valueOperations.get("jusic:cache:url:qq:song-123:320k")).thenReturn(testUrl);

        String cachedUrl = musicCacheService.getPlayUrl("qq", "song-123", "320k");
        assertEquals(testUrl, cachedUrl);

        musicCacheService.putPlayUrl("qq", "song-123", "320k", testUrl);
        verify(valueOperations).set(eq("jusic:cache:url:qq:song-123:320k"), eq(testUrl), eq(15L), eq(TimeUnit.MINUTES));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetAndPutSearch() {
        HulkPage<List<Music>> page = new HulkPage<>();
        List<Music> list = new ArrayList<>();
        Music m = new Music();
        m.setId("1");
        list.add(m);
        page.setData(list);

        String keyword = "hello";
        String expectedKey = "jusic:cache:search:qq:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824:1:10";

        when(valueOperations.get(expectedKey)).thenReturn(page);

        HulkPage<List<Music>> cached = musicCacheService.getSearch("qq", keyword, 1, 10);
        assertNotNull(cached);
        assertEquals(1, cached.getData().size());

        musicCacheService.putSearch("qq", keyword, 1, 10, page);
        verify(valueOperations).set(eq(expectedKey), eq(page), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void interruptedProviderWorkerSkipsCacheWrite() {
        Thread.currentThread().interrupt();
        try {
            musicCacheService.putPlayUrl(
                    "qq", "song-123", "320k", "https://example.test/song.mp3");

            assertTrue(Thread.currentThread().isInterrupted());
            verify(valueOperations, never()).set(
                    any(), any(), anyLong(), any(TimeUnit.class));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void redisInterruptionIsTreatedAsCancellationAndRestoresInterruptFlag() {
        doThrow(new RedisSystemException(
                "Redis command interrupted",
                new RuntimeException(new InterruptedException())))
                .when(valueOperations).set(
                any(), any(), anyLong(), any(TimeUnit.class));

        try {
            assertDoesNotThrow(() -> musicCacheService.putPlayUrl(
                    "qq", "song-123", "320k", "https://example.test/song.mp3"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
