package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SessionBlackRepositoryImplTest {

    private final RedisTemplate redisTemplate = mock(RedisTemplate.class);
    private final HashOperations hashOperations = mock(HashOperations.class);
    private final JusicProperties.RedisKeys redisKeys =
            mock(JusicProperties.RedisKeys.class);
    private final SessionBlackRepositoryImpl repository =
            new SessionBlackRepositoryImpl();

    @BeforeEach
    void setUp() {
        when(redisKeys.getSessionBlackHash()).thenReturn("black:");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        ReflectionTestUtils.setField(repository, "redisKeys", redisKeys);
        ReflectionTestUtils.setField(repository, "redisTemplate", redisTemplate);
    }

    @Test
    void nullOrBlankRemovalDoesNotTouchRedis() {
        assertEquals(1L, repository.removeSession(null, "room"));
        assertEquals(1L, repository.removeSession("   ", "room"));

        verifyNoInteractions(hashOperations);
    }

    @Test
    void removalSkipsEmptyCommaSeparatedEntries() {
        repository.removeSession("session-1, ,", "room");

        verify(hashOperations).get("black:room", "session-1");
        verify(hashOperations).delete("black:room", "session-1");
        verify(hashOperations, never()).get("black:room", "");
    }
}
