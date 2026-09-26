package com.scoder.jusic.room.redisv2;

import com.scoder.jusic.model.Music;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisV2MusicCodecTest {

    private final RedisV2MusicCodec codec = new RedisV2MusicCodec();

    @Test
    void codecUsesPlainJsonWithoutJavaTypeMetadata() {
        Music music = new Music();
        music.setId("song-1");
        music.setName("Song One");
        music.setSource("qq");
        music.setSessionId("session-1");

        String json = codec.encode(music);
        Music decoded = codec.decode(json);

        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("\"id\":\"song-1\""));
        assertFalse(json.contains("@class"));
        assertFalse(json.contains("com.scoder.jusic"));
        assertEquals("song-1", decoded.getId());
        assertEquals("session-1", decoded.getSessionId());
        assertEquals("qq", decoded.getSource());
    }
}
