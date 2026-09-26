package com.scoder.jusic.room.redisv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/** Builds the isolated Redis v2 keyspace for one room. */
public final class RedisV2RoomKeys {

    private static final int MAX_ROOM_ID_BYTES = 512;
    private static final String PREFIX = "jusic:v2:";

    private final String state;
    private final String pick;
    private final String black;
    private final String songIndex;
    private final String commandResults;
    private final String commandOrder;
    private final String events;

    private RedisV2RoomKeys(String roomDigest) {
        String roomPrefix = PREFIX + "{" + roomDigest + "}:";
        this.state = roomPrefix + "state";
        this.pick = roomPrefix + "pick";
        this.black = roomPrefix + "black";
        this.songIndex = roomPrefix + "song-index";
        this.commandResults = roomPrefix + "command-results";
        this.commandOrder = roomPrefix + "command-order";
        this.events = roomPrefix + "events";
    }

    public static RedisV2RoomKeys forRoom(String roomId) {
        byte[] roomBytes = validateRoomId(roomId);
        return new RedisV2RoomKeys(digest(roomBytes));
    }

    public String state() {
        return state;
    }

    public String pick() {
        return pick;
    }

    public String black() {
        return black;
    }

    public String songIndex() {
        return songIndex;
    }

    public String commandResults() {
        return commandResults;
    }

    public String commandOrder() {
        return commandOrder;
    }

    public String events() {
        return events;
    }

    /**
     * Key order is part of the Lua contract. Every key contains the same
     * opaque hash tag, so Redis Cluster executes the command in one slot.
     */
    public List<String> atomicPickKeys() {
        return Collections.unmodifiableList(Arrays.asList(
                state,
                pick,
                black,
                songIndex,
                commandResults,
                commandOrder,
                events));
    }

    private static byte[] validateRoomId(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException("roomId must not be blank");
        }
        for (int index = 0; index < roomId.length(); index++) {
            char character = roomId.charAt(index);
            if (character == '{' || character == '}' || Character.isISOControl(character)) {
                throw new IllegalArgumentException("roomId contains a forbidden character");
            }
        }
        byte[] bytes = roomId.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_ROOM_ID_BYTES) {
            throw new IllegalArgumentException("roomId is too long");
        }
        return bytes;
    }

    private static String digest(byte[] roomBytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(roomBytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
