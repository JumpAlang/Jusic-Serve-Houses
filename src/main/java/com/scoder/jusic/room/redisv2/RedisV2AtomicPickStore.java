package com.scoder.jusic.room.redisv2;

import com.scoder.jusic.model.Music;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Migration-only Redis v2 atomic command prototype.
 *
 * <p><strong>This is not a {@code RoomCommandService} adapter.</strong> Setting
 * {@code jusic.room-command.redis-v2.migration-store-enabled=true} only
 * registers this otherwise-unused bean; it does not route Controller traffic
 * to Redis v2. Calling it is unsafe until room state, every queue reader and
 * writer, the playback job, event consumers and recovery tooling all use the
 * v2 namespace.</p>
 *
 * <p>The store deliberately uses {@link StringRedisTemplate}: keys, hash/set
 * members, Lua arguments and Stream fields are UTF-8 strings, while queued
 * music is plain JSON from {@link RedisV2MusicCodec}. It never writes the old
 * port-suffixed keys or their Jackson-default-typing list values.</p>
 */
@Component
@ConditionalOnProperty(
        prefix = "jusic.room-command.redis-v2",
        name = "migration-store-enabled",
        havingValue = "true")
public class RedisV2AtomicPickStore {

    private final StringRedisTemplate redisTemplate;
    private final RedisV2MusicCodec musicCodec;
    private final RedisV2PickScript pickScript;

    public RedisV2AtomicPickStore(StringRedisTemplate redisTemplate,
                                  RedisV2MusicCodec musicCodec) {
        this.redisTemplate = redisTemplate;
        this.musicCodec = musicCodec;
        this.pickScript = new RedisV2PickScript();
    }

    /**
     * Atomically validates and enqueues one resolved song. {@code requestId}
     * defines a bounded per-room idempotency window (the latest 10,000 command
     * results). Replays never enqueue or emit a second event.
     */
    public RedisV2PickCommitResult commitPick(String requestId,
                                              String roomId,
                                              Music music,
                                              long maxQueueSize) {
        if (music == null) {
            throw new IllegalArgumentException("music must not be null");
        }
        String json = musicCodec.encode(music);
        RedisV2PickScript.MusicJson musicJson =
                new RedisV2PickScript.MusicJson(music.getId(), json);
        List<String> keys = pickScript.keys(roomId);
        Object[] arguments = pickScript.arguments(requestId, musicJson, maxQueueSize);
        List<?> rawResult = redisTemplate.execute(
                pickScript.definition(), keys, arguments);
        return pickScript.parse(rawResult);
    }
}
