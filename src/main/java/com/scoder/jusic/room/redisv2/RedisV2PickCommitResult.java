package com.scoder.jusic.room.redisv2;

/** Parsed result of one Redis v2 atomic pick command. */
public final class RedisV2PickCommitResult {

    private final RedisV2PickStatus status;
    private final RedisV2PickStatus originalStatus;
    private final long queueSize;
    private final long roomVersion;
    private final String eventId;

    RedisV2PickCommitResult(RedisV2PickStatus status,
                            RedisV2PickStatus originalStatus,
                            long queueSize,
                            long roomVersion,
                            String eventId) {
        this.status = status;
        this.originalStatus = originalStatus;
        this.queueSize = queueSize;
        this.roomVersion = roomVersion;
        this.eventId = eventId;
    }

    public RedisV2PickStatus getStatus() {
        return status;
    }

    public RedisV2PickStatus getOriginalStatus() {
        return originalStatus;
    }

    public long getQueueSize() {
        return queueSize;
    }

    public long getRoomVersion() {
        return roomVersion;
    }

    public String getEventId() {
        return eventId;
    }

    public boolean isAccepted() {
        return status == RedisV2PickStatus.ACCEPTED
                || (status == RedisV2PickStatus.IDEMPOTENT_REPLAY
                && originalStatus == RedisV2PickStatus.ACCEPTED);
    }
}
