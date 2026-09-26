package com.scoder.jusic.room.redisv2;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisV2RoomKeysTest {

    @Test
    void allAtomicKeysUseOneOpaqueClusterHashTag() {
        RedisV2RoomKeys keys = RedisV2RoomKeys.forRoom("house-1");
        List<String> atomicKeys = keys.atomicPickKeys();
        Set<String> tags = new HashSet<>();

        for (String key : atomicKeys) {
            tags.add(key.substring(key.indexOf('{') + 1, key.indexOf('}')));
            assertTrue(key.startsWith("jusic:v2:{"));
            assertFalse(key.contains("house-1"));
            assertFalse(key.contains("server.port"));
            assertFalse(key.endsWith("8888"));
        }

        assertEquals(7, atomicKeys.size());
        assertEquals(1, tags.size());
        assertEquals(43, tags.iterator().next().length(), "SHA-256 base64url without padding");
        assertNotEquals(
                RedisV2RoomKeys.forRoom("house-2").state(),
                keys.state());
    }

    @Test
    void roomIdCannotInjectAHashTagOrControlCharacter() {
        assertThrows(IllegalArgumentException.class,
                () -> RedisV2RoomKeys.forRoom("house{other}"));
        assertThrows(IllegalArgumentException.class,
                () -> RedisV2RoomKeys.forRoom("house\r\nother"));
        assertThrows(IllegalArgumentException.class,
                () -> RedisV2RoomKeys.forRoom("   "));
    }
}
