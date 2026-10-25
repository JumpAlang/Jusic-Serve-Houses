package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.repository.MusicDefaultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private String orderKey(String houseId) { return redisKeys.getDefaultSet() + "_order_" + houseId; }
    private String cycleKey(String houseId) { return redisKeys.getDefaultSet() + "_cycle_" + houseId; }
    private String cycleModeKey(String houseId) { return redisKeys.getDefaultSet() + "_cycle_mode_" + houseId; }

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
    public synchronized List<DefaultPlaylistSong> page(String houseId, long offset, int limit) {
        List<String> ordered = orderedMembers(houseId);
        int from = (int) Math.min(Math.max(0L, offset), ordered.size());
        int to = Math.min(ordered.size(), from + Math.max(0, limit));
        List<String> members = ordered.subList(from, to);
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
        Long deleted = redisTemplate.delete(Arrays.asList(redisKeys.getDefaultSet()+"_"+houseId,
                metadataKey(houseId), orderKey(houseId), cycleKey(houseId), cycleModeKey(houseId)));
        return deleted != null && deleted > 0;
//        Set members = redisTemplate.opsForSet()
//                .members(redisKeys.getDefaultSet());
//        return members != null && members.size() > 0 ? redisTemplate.opsForSet()
//                .remove(redisKeys.getDefaultSet(), members.toArray()) : 0;
    }

    @Override
    public Long initialize(String houseId) {
        return add(JusicProperties.getDefaultListForRepository().toArray(new String[0]), houseId);
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
    public synchronized String nextMember(String playlistHouseId, String playbackHouseId,
                                          boolean random) {
        String signature = playlistHouseId + "|" + (random ? "random" : "ordered");
        Object previousSignature = redisTemplate.opsForValue().get(cycleModeKey(playbackHouseId));
        if (!signature.equals(previousSignature)) {
            redisTemplate.delete(cycleKey(playbackHouseId));
            redisTemplate.opsForValue().set(cycleModeKey(playbackHouseId), signature);
        }

        Object next = redisTemplate.opsForList().leftPop(cycleKey(playbackHouseId));
        if (next != null) {
            return next.toString();
        }

        List<String> cycle = orderedMembers(playlistHouseId);
        if (cycle.isEmpty()) {
            return "25718007";
        }
        if (random) {
            Collections.shuffle(cycle);
        }
        redisTemplate.opsForList().rightPushAll(cycleKey(playbackHouseId), cycle);
        Object first = redisTemplate.opsForList().leftPop(cycleKey(playbackHouseId));
        return first == null ? "25718007" : first.toString();
    }

    @Override
    public void remove(String id, String houseId) {
        stringRedisTemplate.opsForHash().delete(metadataKey(houseId), id);
        redisTemplate.opsForSet()
                .remove(redisKeys.getDefaultSet()+"_"+houseId,id);
        redisTemplate.opsForList().remove(orderKey(houseId), 0, id);
        redisTemplate.opsForList().remove(cycleKey(houseId), 0, id);
    }

    @Override
    public synchronized Long add(String[] value,String houseId) {
        long added = 0L;
        if (value == null) return added;
        for (String member : value) {
            if (member == null || member.trim().isEmpty()) continue;
            Long count = redisTemplate.opsForSet()
                    .add(redisKeys.getDefaultSet()+"_"+houseId, member);
            if (count != null && count > 0L) {
                redisTemplate.opsForList().rightPush(orderKey(houseId), member);
                added += count;
            }
        }
        return added;
    }

    @SuppressWarnings("unchecked")
    private List<String> orderedMembers(String houseId) {
        Set<Object> rawMembers = redisTemplate.opsForSet()
                .members(redisKeys.getDefaultSet()+"_"+houseId);
        if (rawMembers == null || rawMembers.isEmpty()) {
            redisTemplate.delete(orderKey(houseId));
            return new ArrayList<>();
        }

        LinkedHashSet<String> valid = rawMembers.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<Object> stored = redisTemplate.opsForList().range(orderKey(houseId), 0, -1);
        LinkedHashSet<String> repaired = new LinkedHashSet<>();
        if (stored != null) {
            for (Object member : stored) {
                if (member != null && valid.contains(member.toString())) {
                    repaired.add(member.toString());
                }
            }
        }
        List<String> missing = new ArrayList<>();
        for (String member : valid) {
            if (!repaired.contains(member)) missing.add(member);
        }
        // A Redis Set did not retain the order of legacy data. Keep migration
        // deterministic; all newly imported songs retain their source order.
        Collections.sort(missing);
        repaired.addAll(missing);
        List<String> result = new ArrayList<>(repaired);
        if (stored == null || stored.size() != result.size()
                || !result.equals(stored.stream().map(Object::toString)
                .collect(java.util.stream.Collectors.toList()))) {
            redisTemplate.delete(orderKey(houseId));
            if (!result.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(orderKey(houseId), result);
            }
        }
        return result;
    }
}
