package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author H
 */
@Repository
@Slf4j
public class SessionRepositoryImpl implements SessionRepository {

    @Autowired
    private JusicProperties.RedisKeys redisKeys;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SessionPresenceStore presenceStore;

    @Override
    public Boolean destroy(String houseId) {
        String hashKey = redisKeys.getSessionHash() + houseId;
        Set sessionIds = redisTemplate.opsForHash().keys(hashKey);
        Boolean deleted = redisTemplate.delete(hashKey);
        if (sessionIds != null && !sessionIds.isEmpty()) {
            presenceStore.remove(houseId, sessionIds);
        }
        return deleted;
    }

    @Override
    public User getSession(String sessionId,String houseId) {
        String hashKey = redisKeys.getSessionHash() + houseId;
        User user = (User) redisTemplate.opsForHash()
                .get(hashKey, sessionId);
        return user == null || presenceStore.isAlive(houseId, sessionId)
                ? user : null;
    }

    @Override
    public List<User> getSession(String houseId) {
        return activeUsers(houseId);
    }

    @Override
    public void setSession(User user,String houseId) {
        presenceStore.storeSession(
                houseId,
                user.getSessionId(),
                redisKeys.getSessionHash() + houseId,
                user);
    }

    @Override
    public Long size(String houseId) {
        return (long) activeUsers(houseId).size();
    }

    @Override
    public Long storedSize(String houseId) {
        return redisTemplate.opsForHash()
                .size(redisKeys.getSessionHash() + houseId);
    }

    @Override
    public Long removeSession(String sessionId,String houseId) {
        return presenceStore.removeSession(
                houseId,
                sessionId,
                redisKeys.getSessionHash() + houseId);
    }

    @Override
    public Map<String, List<String>> refreshPresence(
            Map<String, ? extends Collection<String>> sessionsByHouse) {
        return presenceStore.refresh(
                redisKeys.getSessionHash(), sessionsByHouse);
    }

    @Override
    @SuppressWarnings("unchecked")
    public long pruneExpiredPresence(
            String houseId, Collection<String> protectedSessionIds) {
        String hashKey = redisKeys.getSessionHash() + houseId;
        Set<String> storedIds = redisTemplate.opsForHash().keys(hashKey);
        if (storedIds == null || storedIds.isEmpty()) {
            return 0L;
        }
        Set<String> protectedIds = protectedSessionIds == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(protectedSessionIds);
        List<String> candidates = new ArrayList<>();
        for (String sessionId : storedIds) {
            if (sessionId != null && !protectedIds.contains(sessionId)) {
                candidates.add(sessionId);
            }
        }
        return presenceStore.pruneHashFieldsIfExpired(
                houseId, hashKey, candidates);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Set<String> findPresenceHouseIds() {
        Set<String> houseIds = presenceStore.trackedHouseIds();
        Set<String> discovered;
        try {
            discovered = scanLegacySessionHouseIds();
        } catch (RuntimeException ex) {
            // The durable index is still useful when a keyspace scan fails.
            log.warn("Failed to scan legacy session hashes; using tracked rooms: {}",
                    ex.getMessage());
            log.debug("Legacy session hash scan failure", ex);
            return houseIds;
        }
        // Persist discoveries before returning them for pruning. If cleanup
        // later removes the last session HASH but full room deletion fails,
        // the next run can still retry through this durable index.
        presenceStore.trackHouses(discovered);
        houseIds.addAll(discovered);
        return houseIds;
    }

    @Override
    public void trackPresenceHouse(String houseId) {
        presenceStore.trackHouses(Collections.singleton(houseId));
    }

    @Override
    public void untrackPresenceHouse(String houseId) {
        presenceStore.untrackHouse(houseId);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Set<String> scanLegacySessionHouseIds() {
        String prefix = redisKeys.getSessionHash();
        ScanOptions options = ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(200L)
                .build();
        return (Set<String>) redisTemplate.execute(
                (RedisCallback<Set<String>>) connection -> {
                    Set<String> houseIds = new LinkedHashSet<>();
                    Cursor<byte[]> cursor = connection.scan(options);
                    try {
                        while (cursor.hasNext()) {
                            Object decoded = redisTemplate.getKeySerializer()
                                    .deserialize(cursor.next());
                            String key = decoded == null
                                    ? null : decoded.toString();
                            if (key != null && key.startsWith(prefix)
                                    && key.length() > prefix.length()) {
                                houseIds.add(key.substring(prefix.length()));
                            }
                        }
                    } finally {
                        try {
                            cursor.close();
                        } catch (IOException ex) {
                            throw new IllegalStateException(
                                    "failed to close Redis presence scan", ex);
                        }
                    }
                    return houseIds;
                });
    }

    @SuppressWarnings("unchecked")
    private List<User> activeUsers(String houseId) {
        List<User> stored = redisTemplate.opsForHash()
                .values(redisKeys.getSessionHash() + houseId);
        if (stored == null || stored.isEmpty() || !presenceStore.isEnabled()) {
            return stored == null ? Collections.emptyList() : stored;
        }

        List<String> sessionIds = new ArrayList<>();
        List<User> candidates = new ArrayList<>();
        for (User user : stored) {
            if (user != null && user.getSessionId() != null) {
                candidates.add(user);
                sessionIds.add(user.getSessionId());
            }
        }

        List<Boolean> alive = presenceStore.alive(houseId, sessionIds);
        List<User> active = new ArrayList<>();
        for (int index = 0; index < candidates.size(); index++) {
            if (index < alive.size() && Boolean.TRUE.equals(alive.get(index))) {
                active.add(candidates.get(index));
            }
        }
        return active;
    }

}
