package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.configuration.RedisConfiguration;
import com.scoder.jusic.model.DefaultPlaylistSong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named="jusic.redis.integration",matches="true")
class DefaultPlaylistRedisIntegrationTest {
    @Test void mixedLegacyAndMetadataPaginateAndCleanupWithProductionSerializer() {
        RedisStandaloneConfiguration config=new RedisStandaloneConfiguration("127.0.0.1",6379);config.setDatabase(15);
        LettuceConnectionFactory factory=new LettuceConnectionFactory(config);factory.afterPropertiesSet();
        RedisTemplate template=new RedisConfiguration().redisTemplate(factory);template.afterPropertiesSet();
        StringRedisTemplate strings=new StringRedisTemplate(factory);
        String prefix="codex:test:default:"+UUID.randomUUID();
        JusicProperties.RedisKeys keys=mock(JusicProperties.RedisKeys.class);when(keys.getDefaultSet()).thenReturn(prefix);
        MusicDefaultRepositoryImpl repository=new MusicDefaultRepositoryImpl();
        ReflectionTestUtils.setField(repository,"redisTemplate",template);ReflectionTestUtils.setField(repository,"stringRedisTemplate",strings);ReflectionTestUtils.setField(repository,"redisKeys",keys);
        try {
            repository.add(new String[]{"123","same___qq"},"room");
            DefaultPlaylistSong modern=DefaultPlaylistSong.legacy("same");modern.setName("新歌");modern.setAlbum("专辑");modern.setDuration(120000L);
            assertEquals(1L,repository.addSongs(Collections.singletonList(modern),"room"));
            assertEquals(0L,repository.addSongs(Collections.singletonList(DefaultPlaylistSong.legacy("same")),"room"));
            List<DefaultPlaylistSong> all=new ArrayList<>();
            all.addAll(repository.page("room",0,2));all.addAll(repository.page("room",2,2));
            assertEquals(3,all.size());
            assertEquals("新歌",all.stream().filter(s->s.member().equals("same")).findFirst().get().getName());
            assertNull(all.stream().filter(s->s.member().equals("same___qq")).findFirst().get().getName());
            repository.remove("same","room");assertFalse(strings.opsForHash().hasKey(prefix+"_metadata_room","same"));
            repository.destroy("room");assertEquals(0L,repository.size("room"));assertFalse(strings.hasKey(prefix+"_metadata_room"));
        } finally {template.delete(Arrays.asList(prefix+"_room",prefix+"_metadata_room"));factory.destroy();}
    }
}
