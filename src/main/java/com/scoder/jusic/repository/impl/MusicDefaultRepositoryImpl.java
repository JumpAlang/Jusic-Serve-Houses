package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.repository.MusicDefaultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.query.SortQueryBuilder;
import com.alibaba.fastjson.JSONObject;
import com.scoder.jusic.model.DefaultPlaylistSong;
import java.util.*;

/**
 * @author H
 */
@Repository
public class MusicDefaultRepositoryImpl implements MusicDefaultRepository {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private String metadataKey(String houseId) { return redisKeys.getDefaultSet() + "_metadata_" + houseId; }

    @Override
    public Long addSongs(List<DefaultPlaylistSong> songs, String houseId) {
        Map<String, String> details = new LinkedHashMap<>();
        for (DefaultPlaylistSong song : songs) {
            if (song.getId() == null || song.getId().trim().isEmpty()) continue;
            // A failed metadata refresh must not erase details already saved.
            if (song.getName() != null && !song.getName().isEmpty())
                details.put(song.member(), JSONObject.toJSONString(song));
        }
        if (!details.isEmpty()) stringRedisTemplate.opsForHash().putAll(metadataKey(houseId), details);
        String[] members = songs.stream().filter(s -> s.getId() != null && !s.getId().trim().isEmpty())
                .map(DefaultPlaylistSong::member).distinct().toArray(String[]::new);
        return members.length == 0 ? 0L : add(members, houseId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DefaultPlaylistSong> page(String houseId, long offset, int limit) {
        // Keep the legacy set authoritative; bound the response even for large lists.
        List<String> members = redisTemplate.sort(SortQueryBuilder.sort(redisKeys.getDefaultSet()+"_"+houseId)
                .alphabetical(true).limit(offset, limit).build());
        if (members == null || members.isEmpty()) return Collections.emptyList();
        List<Object> fields = new ArrayList<>(members);
        List<Object> details = stringRedisTemplate.opsForHash().multiGet(metadataKey(houseId), fields);
        List<DefaultPlaylistSong> result = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            DefaultPlaylistSong identity = DefaultPlaylistSong.legacy(members.get(i));
            DefaultPlaylistSong song = null;
            if (details != null && details.get(i) != null) {
                try { song = JSONObject.parseObject(details.get(i).toString(), DefaultPlaylistSong.class); }
                catch (RuntimeException ignored) { /* legacy display also covers malformed old metadata */ }
            }
            if (song == null) song = identity;
            song.setId(identity.getId());
            song.setSource(identity.getSource());
            result.add(song);
        }
        return result;
    }

    @Override
    public Boolean destroy(String houseId) {
        Long deleted = redisTemplate.delete(Arrays.asList(redisKeys.getDefaultSet()+"_"+houseId, metadataKey(houseId)));
        return deleted != null && deleted > 0;
//        Set members = redisTemplate.opsForSet()
//                .members(redisKeys.getDefaultSet());
//        return members != null && members.size() > 0 ? redisTemplate.opsForSet()
//                .remove(redisKeys.getDefaultSet(), members.toArray()) : 0;
    }

    @Override
    public Long initialize(String houseId) {
        return redisTemplate.opsForSet()
                .add(redisKeys.getDefaultSet()+"_"+houseId, JusicProperties.getDefaultListForRepository().toArray());
    }

    /**
     * set
     *
     * @return long
     */
    @Override
    public Long size(String houseId) {
        return redisTemplate.opsForSet()
                .size(redisKeys.getDefaultSet()+"_"+houseId);
    }

    @Override
    public String randomMember(String houseId) {
        Object obj = redisTemplate.opsForSet()
                .randomMember(redisKeys.getDefaultSet()+"_"+houseId);
        if(obj != null){
            return (String) obj;
        }else{
            return "25718007";
        }

    }

    @Override
    public void remove(String id, String houseId) {
        stringRedisTemplate.opsForHash().delete(metadataKey(houseId), id);
        redisTemplate.opsForSet()
                .remove(redisKeys.getDefaultSet()+"_"+houseId,id);
    }

    @Override
    public Long add(String[] value,String houseId) {
        return redisTemplate.opsForSet()
                .add(redisKeys.getDefaultSet()+"_"+houseId, value);
    }
}
