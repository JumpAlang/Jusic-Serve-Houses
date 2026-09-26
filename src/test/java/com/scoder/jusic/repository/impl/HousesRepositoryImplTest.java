package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class HousesRepositoryImplTest {

    private RedisTemplate redisTemplate;
    private ListOperations listOperations;
    private JusicProperties.RedisKeys redisKeys;
    private HousesRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        listOperations = mock(ListOperations.class);
        redisKeys = mock(JusicProperties.RedisKeys.class);
        repository = new HousesRepositoryImpl();
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(redisKeys.getHouses()).thenReturn("jusic_houses");
        ReflectionTestUtils.setField(repository, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(repository, "redisKeys", redisKeys);
    }

    @Test
    void initializeLoadsTheRoomRegistryWithOneListRead() {
        when(listOperations.range("jusic_houses", 0, -1))
                .thenReturn(Collections.emptyList());

        CopyOnWriteArrayList<House> houses = repository.initialize();

        assertEquals(1, houses.size());
        assertEquals(JusicProperties.HOUSE_DEFAULT_ID, houses.get(0).getId());
        verify(listOperations).range("jusic_houses", 0, -1);
        verify(listOperations, never()).size(any());
    }

    @Test
    void keyScanUsesTemplateManagedConnectionAndAlwaysClosesTheCursor()
            throws Exception {
        RedisConnection connection = mock(RedisConnection.class);
        Cursor<byte[]> cursor = mock(Cursor.class);
        when(connection.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn("jusic_key".getBytes(StandardCharsets.UTF_8));
        when(redisTemplate.getKeySerializer()).thenReturn(new StringRedisSerializer());
        when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation ->
                ((RedisCallback) invocation.getArgument(0)).doInRedis(connection));

        Set<String> keys = repository.allKeys();

        assertTrue(keys.contains("jusic_key"));
        verify(cursor).close();
        verify(redisTemplate).execute(any(RedisCallback.class));
    }
}
