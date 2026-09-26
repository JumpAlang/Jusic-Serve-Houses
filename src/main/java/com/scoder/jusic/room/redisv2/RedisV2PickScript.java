package com.scoder.jusic.room.redisv2;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Java-side contract for {@code redis/room-command-v2-commit-pick.lua}. */
final class RedisV2PickScript {

    static final String RESOURCE_PATH = "redis/room-command-v2-commit-pick.lua";
    static final int MAX_QUEUE_SIZE = 100_000;
    static final int MAX_IDEMPOTENCY_RECORDS = 10_000;
    static final int MAX_REQUEST_ID_BYTES = 128;
    static final int MAX_SONG_ID_BYTES = 512;
    static final int MAX_MUSIC_JSON_BYTES = 1_048_576;

    private final DefaultRedisScript<List> script;

    RedisV2PickScript() {
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource(RESOURCE_PATH));
        this.script.setResultType(List.class);
    }

    DefaultRedisScript<List> definition() {
        return script;
    }

    List<String> keys(String roomId) {
        return RedisV2RoomKeys.forRoom(roomId).atomicPickKeys();
    }

    Object[] arguments(String requestId,
                       MusicJson music,
                       long maxQueueSize) {
        validateOpaqueIdentifier("requestId", requestId, MAX_REQUEST_ID_BYTES, true);
        validateOpaqueIdentifier("music.id", music.songId, MAX_SONG_ID_BYTES, false);
        if (maxQueueSize <= 0 || maxQueueSize > MAX_QUEUE_SIZE) {
            throw new IllegalArgumentException("maxQueueSize is outside the supported range");
        }
        int jsonBytes = music.json.getBytes(StandardCharsets.UTF_8).length;
        if (jsonBytes == 0 || jsonBytes > MAX_MUSIC_JSON_BYTES) {
            throw new IllegalArgumentException("music json is outside the supported range");
        }
        return new Object[]{
                Long.toString(maxQueueSize),
                requestId,
                music.songId,
                music.json,
                Integer.toString(MAX_IDEMPOTENCY_RECORDS)
        };
    }

    RedisV2PickCommitResult parse(List<?> rawResult) {
        if (rawResult == null || rawResult.size() != 5) {
            throw new IllegalStateException("malformed Redis v2 pick result");
        }
        long statusCode = parseNonNegativeInteger(rawResult.get(0), "status");
        long originalStatusCode = parseNonNegativeInteger(rawResult.get(1), "originalStatus");
        long queueSize = parseNonNegativeInteger(rawResult.get(2), "queueSize");
        long roomVersion = parseNonNegativeInteger(rawResult.get(3), "roomVersion");
        String eventId = parseString(rawResult.get(4), "eventId");

        RedisV2PickStatus status = RedisV2PickStatus.fromCode(statusCode);
        RedisV2PickStatus originalStatus = RedisV2PickStatus.fromCode(originalStatusCode);
        validateStatusPair(status, originalStatus, queueSize, roomVersion, eventId);
        return new RedisV2PickCommitResult(
                status, originalStatus, queueSize, roomVersion, eventId);
    }

    private void validateStatusPair(RedisV2PickStatus status,
                                    RedisV2PickStatus originalStatus,
                                    long queueSize,
                                    long roomVersion,
                                    String eventId) {
        if (status == RedisV2PickStatus.IDEMPOTENT_REPLAY) {
            if (originalStatus != RedisV2PickStatus.ACCEPTED
                    && originalStatus != RedisV2PickStatus.CAPACITY_REACHED
                    && originalStatus != RedisV2PickStatus.BLACKLISTED
                    && originalStatus != RedisV2PickStatus.DUPLICATE) {
                throw new IllegalStateException("invalid idempotent replay result");
            }
        } else if (status != originalStatus) {
            throw new IllegalStateException("Redis v2 result codes disagree");
        }

        boolean accepted = status == RedisV2PickStatus.ACCEPTED
                || (status == RedisV2PickStatus.IDEMPOTENT_REPLAY
                && originalStatus == RedisV2PickStatus.ACCEPTED);
        if (accepted && (queueSize == 0 || roomVersion == 0 || eventId.isEmpty())) {
            throw new IllegalStateException("accepted Redis v2 result lacks commit metadata");
        }
        if (!accepted && !eventId.isEmpty()) {
            throw new IllegalStateException("rejected Redis v2 result has an event id");
        }
    }

    private void validateOpaqueIdentifier(String name,
                                          String value,
                                          int maxBytes,
                                          boolean asciiOnly) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            throw new IllegalArgumentException(name + " is too long");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '{' || character == '}' || Character.isISOControl(character)
                    || (asciiOnly && character > 0x7f)) {
                throw new IllegalArgumentException(name + " contains a forbidden character");
            }
        }
    }

    private long parseNonNegativeInteger(Object value, String field) {
        String text;
        if (value instanceof Number) {
            if (value instanceof Float || value instanceof Double) {
                throw new IllegalStateException(field + " is not an integer");
            }
            text = value.toString();
        } else {
            text = parseString(value, field);
        }
        if (!text.matches("0|[1-9][0-9]*")) {
            throw new IllegalStateException(field + " is not a non-negative integer");
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(field + " exceeds the supported range", ex);
        }
    }

    private String parseString(Object value, String field) {
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof byte[]) {
            return new String((byte[]) value, StandardCharsets.UTF_8);
        }
        throw new IllegalStateException(field + " is not a string");
    }

    static final class MusicJson {
        private final String songId;
        private final String json;

        MusicJson(String songId, String json) {
            this.songId = songId;
            this.json = json;
        }
    }
}
