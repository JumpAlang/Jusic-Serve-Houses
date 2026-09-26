package com.scoder.jusic.room.redisv2;

/** Stable result codes returned by the Redis v2 atomic pick command. */
public enum RedisV2PickStatus {
    ACCEPTED(0),
    IDEMPOTENT_REPLAY(1),
    CAPACITY_REACHED(2),
    BLACKLISTED(3),
    DUPLICATE(4),
    ROOM_NOT_READY(5),
    CORRUPT_STATE(6),
    INVALID_ARGUMENTS(7);

    private final int code;

    RedisV2PickStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    static RedisV2PickStatus fromCode(long code) {
        for (RedisV2PickStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalStateException("unknown Redis v2 pick result code: " + code);
    }
}
