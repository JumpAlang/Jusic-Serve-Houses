package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Music;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class MusicPickRepositoryImplSessionCleanupTest {

    private static final String PICK_PREFIX = "jusic_pick_";
    private static final String HOUSE_ID = "room-a";
    private static final String KEY = PICK_PREFIX + HOUSE_ID;
    private static final String SESSION_ID = "session-owner";

    private RedisTemplate redisTemplate;
    private ListOperations listOperations;
    private JusicProperties.RedisKeys redisKeys;
    private MusicPickRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        listOperations = mock(ListOperations.class);
        redisKeys = mock(JusicProperties.RedisKeys.class);
        repository = new MusicPickRepositoryImpl();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisKeys.getPickList()).thenReturn(PICK_PREFIX);
        ReflectionTestUtils.setField(repository, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(repository, "redisKeys", redisKeys);
    }

    @Test
    void removesEveryMatchingEntryWithIndividualExactLremCalls() {
        Music firstOwned = music("first", SESSION_ID, 1L);
        Music otherUsers = music("other", "session-other", 2L);
        Music secondOwned = music("second", SESSION_ID, 3L);
        when(listOperations.range(KEY, 0, -1))
                .thenReturn(Arrays.asList(firstOwned, otherUsers, secondOwned));
        when(listOperations.remove(KEY, 1, firstOwned)).thenReturn(1L);
        when(listOperations.remove(KEY, 1, secondOwned)).thenReturn(1L);

        long removed = repository.removeQueuedMusicBySession(SESSION_ID, HOUSE_ID);

        assertEquals(2L, removed);
        verify(listOperations).remove(KEY, 1, firstOwned);
        verify(listOperations).remove(KEY, 1, secondOwned);
        verify(listOperations, never()).remove(KEY, 1, otherUsers);
        verify(listOperations, never()).trim(any(), anyLong(), anyLong());
        verify(listOperations, never()).rightPushAll(any(), any(Object[].class));
    }

    @Test
    void readsTheWholeQueueWithOneRedisRoundTrip() {
        Music queued = music("queued", SESSION_ID, 1L);
        when(listOperations.range(KEY, 0, -1))
                .thenReturn(Collections.singletonList(queued));

        assertEquals(Collections.singletonList(queued),
                repository.getPickMusicList(HOUSE_ID));

        verify(listOperations).range(KEY, 0, -1);
        verify(listOperations, never()).size(any());
    }

    @Test
    void exactSingleDeleteUsesLremWithoutAQueueRewrite() {
        Music queued = music("queued", SESSION_ID, 1L);
        when(listOperations.remove(KEY, 1, queued)).thenReturn(1L);

        assertEquals(1L, repository.removeOne(queued, HOUSE_ID));

        verify(listOperations).remove(KEY, 1, queued);
        verify(listOperations, never()).trim(any(), anyLong(), anyLong());
        verify(listOperations, never()).rightPushAll(any(), any(Object[].class));
    }

    @Test
    void atomicTopSerializesTheOldValueBeforePublishingTheUpdatedValue() {
        Music queued = music("queued", SESSION_ID, 1L);
        RedisSerializer keySerializer = mock(RedisSerializer.class);
        RedisSerializer valueSerializer = mock(RedisSerializer.class);
        when(redisTemplate.getKeySerializer()).thenReturn(keySerializer);
        when(redisTemplate.getValueSerializer()).thenReturn(valueSerializer);
        when(keySerializer.serialize(KEY)).thenReturn(new byte[]{1});
        when(valueSerializer.serialize(queued))
                .thenReturn(new byte[]{2}, new byte[]{3});
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(1L);

        assertEquals(1L, repository.moveExactToTail(queued, 123L, HOUSE_ID));

        assertEquals(Long.valueOf(123L), queued.getTopTime());
        verify(valueSerializer, times(2)).serialize(queued);
        verify(redisTemplate).execute(any(RedisCallback.class));
        verify(listOperations, never()).trim(any(), anyLong(), anyLong());
    }

    @Test
    void atomicTopRestoresPreviousTopTimeWhenLuaDoesNotMoveTheEntry() {
        Music queued = music("queued", SESSION_ID, 1L);
        queued.setTopTime(77L);
        configureTopSerializers(queued);
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(0L);

        assertEquals(0L, repository.moveExactToTail(queued, 123L, HOUSE_ID));

        assertEquals(Long.valueOf(77L), queued.getTopTime());
    }

    @Test
    void atomicTopRestoresPreviousTopTimeWhenLuaFails() {
        Music queued = music("queued", SESSION_ID, 1L);
        queued.setTopTime(77L);
        configureTopSerializers(queued);
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertThrows(IllegalStateException.class,
                () -> repository.moveExactToTail(queued, 123L, HOUSE_ID));

        assertEquals(Long.valueOf(77L), queued.getTopTime());
    }

    @Test
    void exactDeleteAndTopRejectBlankHouseOrMusicIdentifiersBeforeRedisAccess() {
        Music missingId = music(null, SESSION_ID, 1L);
        Music blankId = music(" ", SESSION_ID, 2L);
        Music valid = music("valid", SESSION_ID, 3L);

        assertEquals(0L, repository.removeOne(null, HOUSE_ID));
        assertEquals(0L, repository.removeOne(missingId, HOUSE_ID));
        assertEquals(0L, repository.removeOne(blankId, HOUSE_ID));
        assertEquals(0L, repository.removeOne(valid, null));
        assertEquals(0L, repository.removeOne(valid, " "));
        assertEquals(0L, repository.moveExactToTail(null, 10L, HOUSE_ID));
        assertEquals(0L, repository.moveExactToTail(missingId, 10L, HOUSE_ID));
        assertEquals(0L, repository.moveExactToTail(blankId, 10L, HOUSE_ID));
        assertEquals(0L, repository.moveExactToTail(valid, 10L, null));
        assertEquals(0L, repository.moveExactToTail(valid, 10L, " "));

        verifyNoInteractions(redisTemplate, redisKeys);
    }

    @Test
    void returnsOnlyTheNumberRedisActuallyRemoved() {
        Music removedEntry = music("removed", SESSION_ID, 1L);
        Music racedWithPlayback = music("already-popped", SESSION_ID, 2L);
        when(listOperations.range(KEY, 0, -1))
                .thenReturn(Arrays.asList(removedEntry, racedWithPlayback));
        when(listOperations.remove(KEY, 1, removedEntry)).thenReturn(1L);
        when(listOperations.remove(KEY, 1, racedWithPlayback)).thenReturn(0L);

        assertEquals(1L,
                repository.removeQueuedMusicBySession(SESSION_ID, HOUSE_ID));
    }

    @Test
    void emptyQueueDoesNotIssueAnyRemoveOrRewrite() {
        when(listOperations.range(KEY, 0, -1))
                .thenReturn(Collections.emptyList());

        assertEquals(0L,
                repository.removeQueuedMusicBySession(SESSION_ID, HOUSE_ID));

        verify(listOperations, never()).remove(any(), anyLong(), any());
        verify(listOperations, never()).trim(any(), anyLong(), anyLong());
    }

    @Test
    void blankIdentifiersAreRejectedBeforeRedisAccess() {
        assertEquals(0L, repository.removeQueuedMusicBySession(null, HOUSE_ID));
        assertEquals(0L, repository.removeQueuedMusicBySession(" ", HOUSE_ID));
        assertEquals(0L, repository.removeQueuedMusicBySession(SESSION_ID, null));
        assertEquals(0L, repository.removeQueuedMusicBySession(SESSION_ID, " "));

        verifyNoInteractions(redisTemplate, redisKeys);
    }

    private Music music(String id, String sessionId, long pickTime) {
        Music music = new Music();
        music.setId(id);
        music.setName(id);
        music.setSessionId(sessionId);
        music.setPickTime(pickTime);
        return music;
    }

    private void configureTopSerializers(Music queued) {
        RedisSerializer keySerializer = mock(RedisSerializer.class);
        RedisSerializer valueSerializer = mock(RedisSerializer.class);
        when(redisTemplate.getKeySerializer()).thenReturn(keySerializer);
        when(redisTemplate.getValueSerializer()).thenReturn(valueSerializer);
        when(keySerializer.serialize(KEY)).thenReturn(new byte[]{1});
        when(valueSerializer.serialize(queued))
                .thenReturn(new byte[]{2}, new byte[]{3});
    }
}
