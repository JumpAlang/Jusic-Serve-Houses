package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.RedisConfiguration;
import com.scoder.jusic.model.Music;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Opt-in proof that the production Jackson Redis serializer can round-trip a
 * Music value and use that value in an exact LREM. Database 15 and random keys
 * keep the test isolated from normal room data.
 */
@EnabledIfSystemProperty(named = "jusic.redis.integration", matches = "true")
class MusicPickRepositoryImplRedisIntegrationTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void exactLremPreservesOtherUserOrderAndSeparatePlayingKey() {
        RedisStandaloneConfiguration standalone =
                new RedisStandaloneConfiguration("127.0.0.1", 6379);
        standalone.setDatabase(15);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone);
        factory.afterPropertiesSet();
        RedisTemplate template = new RedisConfiguration().redisTemplate(factory);
        template.afterPropertiesSet();

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String pickPrefix = "codex:test:switch-pick:" + suffix + ":";
        String houseId = "house";
        String pickKey = pickPrefix + houseId;
        String playingKey = "codex:test:switch-playing:" + suffix;
        String ownerSession = "owner-session";
        Music firstOwned = music("same-song", ownerSession, 1L);
        Music otherUser = music("same-song", "other-session", 2L);
        Music secondOwned = music("second-song", ownerSession, 3L);
        Music playing = music("currently-playing", ownerSession, 4L);

        JusicProperties.RedisKeys redisKeys = mock(JusicProperties.RedisKeys.class);
        when(redisKeys.getPickList()).thenReturn(pickPrefix);
        MusicPickRepositoryImpl repository = new MusicPickRepositoryImpl();
        ReflectionTestUtils.setField(repository, "redisTemplate", template);
        ReflectionTestUtils.setField(repository, "redisKeys", redisKeys);

        try {
            template.opsForList().rightPushAll(
                    pickKey, Arrays.asList(firstOwned, otherUser, secondOwned));
            template.opsForValue().set(playingKey, playing);

            assertEquals(2L, repository.removeQueuedMusicBySession(
                    ownerSession, houseId));

            List<Music> remaining = template.opsForList().range(pickKey, 0, -1);
            assertNotNull(remaining);
            assertEquals(1, remaining.size());
            assertEquals("other-session", remaining.get(0).getSessionId());
            assertEquals("same-song", remaining.get(0).getId());
            Music storedPlaying = (Music) template.opsForValue().get(playingKey);
            assertNotNull(storedPlaying);
            assertEquals("currently-playing", storedPlaying.getId());

            Music laterPick = music("later-song", "later-session", 5L);
            template.opsForList().rightPush(pickKey, laterPick);
            assertEquals(1L, repository.moveExactToTail(
                    remaining.get(0), 123L, houseId));
            List<Music> reordered = template.opsForList().range(pickKey, 0, -1);
            assertNotNull(reordered);
            assertEquals(2, reordered.size());
            assertEquals("later-song", reordered.get(0).getId());
            assertEquals("same-song", reordered.get(1).getId());
            assertEquals(Long.valueOf(123L), reordered.get(1).getTopTime());
        } finally {
            try {
                template.delete(Arrays.asList(pickKey, playingKey));
            } finally {
                factory.destroy();
            }
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void concurrentLeftPushesSurviveAtomicTop() throws Exception {
        RedisStandaloneConfiguration standalone =
                new RedisStandaloneConfiguration("127.0.0.1", 6379);
        standalone.setDatabase(15);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone);
        factory.afterPropertiesSet();
        RedisTemplate template = new RedisConfiguration().redisTemplate(factory);
        template.afterPropertiesSet();

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String pickPrefix = "codex:test:concurrent-top:" + suffix + ":";
        String houseId = "house";
        String pickKey = pickPrefix + houseId;
        int concurrentPicks = 100;
        Music target = music("target", "owner", 1L);
        Music existing = music("existing", "other", 2L);

        JusicProperties.RedisKeys redisKeys = mock(JusicProperties.RedisKeys.class);
        when(redisKeys.getPickList()).thenReturn(pickPrefix);
        MusicPickRepositoryImpl repository = new MusicPickRepositoryImpl();
        ReflectionTestUtils.setField(repository, "redisTemplate", template);
        ReflectionTestUtils.setField(repository, "redisKeys", redisKeys);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            template.opsForList().rightPushAll(
                    pickKey, Arrays.asList(target, existing));
            Music storedTarget = (Music) template.opsForList().index(pickKey, 0);
            assertNotNull(storedTarget);

            CountDownLatch start = new CountDownLatch(1);
            Future<Long> top = executor.submit(() -> {
                start.await();
                return repository.moveExactToTail(
                        storedTarget, 123L, houseId);
            });
            Future<?> picks = executor.submit(() -> {
                start.await();
                for (int index = 0; index < concurrentPicks; index++) {
                    repository.leftPush(
                            music("new-" + index, "picker-" + index, 10L + index),
                            houseId);
                }
                return null;
            });

            start.countDown();
            assertEquals(Long.valueOf(1L), top.get(10, TimeUnit.SECONDS));
            picks.get(10, TimeUnit.SECONDS);

            List<Music> finalQueue =
                    template.opsForList().range(pickKey, 0, -1);
            assertNotNull(finalQueue);
            assertEquals(concurrentPicks + 2, finalQueue.size());
            assertEquals("target",
                    finalQueue.get(finalQueue.size() - 1).getId());
            assertEquals(Long.valueOf(123L),
                    finalQueue.get(finalQueue.size() - 1).getTopTime());

            Set<String> ids = new HashSet<>();
            for (Music music : finalQueue) {
                ids.add(music.getId());
            }
            assertEquals(concurrentPicks + 2, ids.size());
            assertEquals(true, ids.contains("existing"));
            for (int index = 0; index < concurrentPicks; index++) {
                assertEquals(true, ids.contains("new-" + index));
            }
        } finally {
            executor.shutdownNow();
            try {
                template.delete(pickKey);
            } finally {
                factory.destroy();
            }
        }
    }

    private Music music(String id, String sessionId, long pickTime) {
        Music music = new Music();
        music.setId(id);
        music.setName(id);
        music.setSessionId(sessionId);
        music.setPickTime(pickTime);
        return music;
    }
}
