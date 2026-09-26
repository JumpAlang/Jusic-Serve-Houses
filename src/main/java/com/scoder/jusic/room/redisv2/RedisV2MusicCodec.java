package com.scoder.jusic.room.redisv2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scoder.jusic.model.Music;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Dedicated v2 codec. It intentionally has no Jackson default typing and does
 * not share the legacy RedisTemplate serializer.
 */
@Component
public class RedisV2MusicCodec {

    private final ObjectMapper objectMapper;

    public RedisV2MusicCodec() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String encode(Music music) {
        if (music == null) {
            throw new IllegalArgumentException("music must not be null");
        }
        try {
            return objectMapper.writeValueAsString(music);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("music cannot be encoded", ex);
        }
    }

    public Music decode(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("music json must not be blank");
        }
        try {
            return objectMapper.readValue(json, Music.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("music json cannot be decoded", ex);
        }
    }
}
