package com.scoder.jusic.room.redisv2;

import com.scoder.jusic.model.Music;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in only. Normal unit-test runs skip this class and never open Redis.
 * Set JUSIC_TEST_REDIS_V2=true explicitly against an expendable Redis instance.
 */
@EnabledIfEnvironmentVariable(named = "JUSIC_TEST_REDIS_V2", matches = "true")
class RedisV2AtomicPickStoreOptionalRedisTest {

    @Test
    void roomVersionZeroCommitsOnceAndTheSameRequestReplays() {
        String host = environment("JUSIC_TEST_REDIS_HOST", "127.0.0.1");
        int port = Integer.parseInt(environment("JUSIC_TEST_REDIS_PORT", "6379"));
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(host, port);
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        String roomId = "optional-test-" + UUID.randomUUID();
        RedisV2RoomKeys keys = RedisV2RoomKeys.forRoom(roomId);
        try {
            Map<String, String> state = new HashMap<>();
            state.put("schemaVersion", "2");
            state.put("status", "ACTIVE");
            state.put("roomVersion", "0");
            redisTemplate.opsForHash().putAll(keys.state(), state);

            Music music = new Music();
            music.setId("song-1");
            music.setName("Song One");
            music.setUrl("https://music.invalid/song-1.mp3");
            RedisV2AtomicPickStore store = new RedisV2AtomicPickStore(
                    redisTemplate, new RedisV2MusicCodec());

            RedisV2PickCommitResult first = store.commitPick(
                    "request-1", roomId, music, 10);
            RedisV2PickCommitResult replay = store.commitPick(
                    "request-1", roomId, music, 10);
            RedisV2PickCommitResult duplicate = store.commitPick(
                    "request-2", roomId, music, 10);

            Music secondMusic = new Music();
            secondMusic.setId("song-2");
            secondMusic.setName("Song Two");
            secondMusic.setUrl("https://music.invalid/song-2.mp3");
            RedisV2PickCommitResult capacityRejected = store.commitPick(
                    "request-3", roomId, secondMusic, 1);

            assertEquals(RedisV2PickStatus.ACCEPTED, first.getStatus());
            assertEquals(RedisV2PickStatus.IDEMPOTENT_REPLAY, replay.getStatus());
            assertEquals(RedisV2PickStatus.ACCEPTED, replay.getOriginalStatus());
            assertEquals(first.getEventId(), replay.getEventId());
            assertEquals(RedisV2PickStatus.DUPLICATE, duplicate.getStatus());
            assertEquals(RedisV2PickStatus.CAPACITY_REACHED, capacityRejected.getStatus());
            assertEquals("1", redisTemplate.opsForHash().get(keys.state(), "roomVersion"));
            assertEquals(1L, redisTemplate.opsForList().size(keys.pick()));
            assertEquals(1L, redisTemplate.opsForStream().size(keys.events()).longValue());
            assertTrue(Boolean.TRUE.equals(redisTemplate.opsForSet()
                    .isMember(keys.songIndex(), "song-1")));
        } finally {
            redisTemplate.delete(keys.atomicPickKeys());
            connectionFactory.destroy();
        }
    }

    private String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
