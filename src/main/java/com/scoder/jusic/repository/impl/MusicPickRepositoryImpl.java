package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.repository.MusicPickRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author H
 */
@Repository
public class MusicPickRepositoryImpl implements MusicPickRepository {

    /**
     * One-key script, so it is also valid when Redis Cluster is introduced.
     * ARGV values are the exact bytes produced by the legacy value serializer.
     */
    private static final byte[] MOVE_EXACT_TO_TAIL_SCRIPT = (
            "local removed = redis.call('LREM', KEYS[1], 1, ARGV[1]) "
                    + "if removed == 1 then "
                    + "redis.call('RPUSH', KEYS[1], ARGV[2]) "
                    + "end "
                    + "return removed")
            .getBytes(StandardCharsets.UTF_8);

    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Boolean destroy(String houseId) {
        return redisTemplate.delete(redisKeys.getPickList()+houseId);
//        redisTemplate.opsForList()
//                .trim(redisKeys.getPickList()+houseId, 1, 0);
    }

    @Override
    public Long leftPush(Music pick,String houseId) {
        return redisTemplate.opsForList()
                .leftPush(redisKeys.getPickList()+houseId, pick);
    }

    @Override
    public Long leftPushAll(String houseId,Object... value) {
        return redisTemplate.opsForList()
                .leftPushAll(redisKeys.getPickList()+houseId, value);
    }

    @Override
    public Long rightPushAll(String houseId,Object... value) {
        return redisTemplate.opsForList()
                .rightPushAll(redisKeys.getPickList()+houseId, value);
    }

    @Override
    public Long size(String houseId) {
        return redisTemplate.opsForList()
                .size(redisKeys.getPickList()+houseId);
    }

    /**
     * clear the pick list.
     */
    @Override
    public void reset(String houseId) {
        redisTemplate.opsForList()
                .trim(redisKeys.getPickList()+houseId, 1, 0);
    }

    /**
     * get all pick music.
     *
     * @return LinkedList
     */
    @Override
    public List<Music> getPickMusicList(String houseId) {
        return (List<Music>) redisTemplate.opsForList()
                .range(redisKeys.getPickList()+houseId, 0, -1);
    }

    @Override
    public long removeOne(Music music, String houseId) {
        if (music == null || !hasText(music.getId()) || !hasText(houseId)) {
            return 0L;
        }
        Long removed = redisTemplate.opsForList().remove(
                redisKeys.getPickList() + houseId, 1, music);
        return removed == null ? 0L : removed;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public long moveExactToTail(Music music, long topTime, String houseId) {
        if (music == null || !hasText(music.getId()) || !hasText(houseId)) {
            return 0L;
        }

        RedisSerializer keySerializer = redisTemplate.getKeySerializer();
        RedisSerializer valueSerializer = redisTemplate.getValueSerializer();
        byte[] key = keySerializer.serialize(redisKeys.getPickList() + houseId);
        // Serialize the original value first: LREM must match the bytes already
        // stored in Redis, including the previous topTime value.
        byte[] oldValue = valueSerializer.serialize(music);
        Long previousTopTime = music.getTopTime();
        Object result;
        try {
            music.setTopTime(topTime);
            byte[] movedValue = valueSerializer.serialize(music);
            result = redisTemplate.execute((RedisCallback<Object>) connection ->
                    connection.eval(
                            MOVE_EXACT_TO_TAIL_SCRIPT,
                            ReturnType.INTEGER,
                            1,
                            key,
                            oldValue,
                            movedValue));
        } catch (RuntimeException ex) {
            music.setTopTime(previousTopTime);
            throw ex;
        }
        long moved = result instanceof Number ? ((Number) result).longValue() : 0L;
        if (moved == 0L) {
            music.setTopTime(previousTopTime);
        }
        return moved;
    }

    @Override
    public long removeQueuedMusicBySession(String sessionId, String houseId) {
        if (!hasText(sessionId) || !hasText(houseId)) {
            return 0L;
        }

        String key = redisKeys.getPickList() + houseId;
        List<Music> queued = (List<Music>) redisTemplate.opsForList()
                .range(key, 0, -1);
        if (queued == null || queued.isEmpty()) {
            return 0L;
        }

        long removed = 0L;
        for (Music music : queued) {
            if (music == null || !sessionId.equals(music.getSessionId())) {
                continue;
            }
            // LREM one exact serialized entry at a time. This never rewrites
            // the list and cannot overwrite unrelated concurrent additions.
            Long current = redisTemplate.opsForList().remove(key, 1, music);
            if (current != null && current > 0L) {
                removed += current;
            }
        }
        return removed;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
