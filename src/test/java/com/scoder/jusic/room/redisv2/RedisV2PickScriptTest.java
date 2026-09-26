package com.scoder.jusic.room.redisv2;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisV2PickScriptTest {

    private final RedisV2PickScript script = new RedisV2PickScript();

    @Test
    void invocationUsesStrictKeysAndExplicitIdempotencyArguments() {
        Object[] arguments = script.arguments(
                "request-123",
                new RedisV2PickScript.MusicJson("song-1", "{\"id\":\"song-1\"}"),
                25);

        assertEquals(7, script.keys("house-1").size());
        assertArrayEquals(new Object[]{
                "25",
                "request-123",
                "song-1",
                "{\"id\":\"song-1\"}",
                "10000"
        }, arguments);
        assertThrows(IllegalArgumentException.class, () -> script.arguments(
                "bad\nrequest",
                new RedisV2PickScript.MusicJson("song-1", "{}"),
                25));
        assertThrows(IllegalArgumentException.class, () -> script.arguments(
                "request-123",
                new RedisV2PickScript.MusicJson("song-1", "{}"),
                0));
    }

    @Test
    void resultParserAcceptsZeroRoomVersionForStableRejection() {
        RedisV2PickCommitResult result = script.parse(Arrays.asList(
                2L, 2L, 25L, 0L, ""));

        assertEquals(RedisV2PickStatus.CAPACITY_REACHED, result.getStatus());
        assertEquals(0, result.getRoomVersion());
        assertFalse(result.isAccepted());
    }

    @Test
    void resultParserDistinguishesSuccessFromSuccessfulReplay() {
        RedisV2PickCommitResult accepted = script.parse(Arrays.asList(
                0L, 0L, 1L, 1L, "1710000000000-0"));
        RedisV2PickCommitResult replay = script.parse(Arrays.asList(
                1L, 0L, 1L, 1L, "1710000000000-0"));

        assertEquals(RedisV2PickStatus.ACCEPTED, accepted.getStatus());
        assertEquals(RedisV2PickStatus.IDEMPOTENT_REPLAY, replay.getStatus());
        assertEquals(RedisV2PickStatus.ACCEPTED, replay.getOriginalStatus());
        assertTrue(accepted.isAccepted());
        assertTrue(replay.isAccepted());
        assertEquals(accepted.getEventId(), replay.getEventId());
    }

    @Test
    void malformedOrContradictoryLuaResultsFailClosed() {
        assertThrows(IllegalStateException.class,
                () -> script.parse(Arrays.asList(0L, 0L, 1L, 1L)));
        assertThrows(IllegalStateException.class,
                () -> script.parse(Arrays.asList(99L, 99L, 0L, 0L, "")));
        assertThrows(IllegalStateException.class,
                () -> script.parse(Arrays.asList(1L, 5L, 0L, 0L, "")));
        assertThrows(IllegalStateException.class,
                () -> script.parse(Arrays.asList(0L, 0L, 1L, 1L, "")));
    }

    @Test
    void luaContractContainsIdempotencyVersionAndEventAndNoLegacyKey() {
        String lua = script.definition().getScriptAsString();

        assertTrue(lua.contains("HGET', KEYS[5], ARGV[2]"));
        assertTrue(lua.contains("HINCRBY', KEYS[1], 'roomVersion', 1"));
        assertTrue(lua.contains("'XADD', KEYS[7]"));
        assertTrue(lua.contains("'MAXLEN', '~', 10000"));
        assertTrue(lua.contains("value == '0' or string.match"));
        assertFalse(lua.contains("jusic_pick"));
        assertFalse(lua.contains("jusic_black"));
        assertFalse(lua.contains("server.port"));
    }
}
