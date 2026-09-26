package com.scoder.jusic.repository.impl;

import com.scoder.jusic.configuration.JusicEnvironment;
import com.scoder.jusic.configuration.PresenceProperties;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TTL markers for fields kept in the legacy room-session Redis hash.
 */
@Component
public class SessionPresenceStore {

    private static final String MARKER_VALUE = "1";
    private static final byte[] MARKER_BYTES =
            MARKER_VALUE.getBytes(StandardCharsets.UTF_8);
    private static final byte[] STORE_SESSION_SCRIPT = (
            "redis.call('SADD', KEYS[3], ARGV[5]); "
                    + "redis.call('PSETEX', KEYS[1], ARGV[1], ARGV[2]); "
                    + "redis.call('HSET', KEYS[2], ARGV[3], ARGV[4]); "
                    + "redis.call('DEL', KEYS[4]); "
                    + "return 1")
            .getBytes(StandardCharsets.UTF_8);
    private static final byte[] PRUNE_EXPIRED_SCRIPT = (
            "local clock = redis.call('TIME'); "
                    + "local now = (clock[1] * 1000) + math.floor(clock[2] / 1000); "
                    + "local grace = tonumber(ARGV[1]); "
                    + "local removed = 0; "
                    + "for i = 2, #KEYS, 2 do "
                    + "local candidate = KEYS[i + 1]; "
                    + "local field = ARGV[2 + (i / 2)]; "
                    + "if redis.call('EXISTS', KEYS[i]) == 1 then "
                    + "redis.call('DEL', candidate); "
                    + "elseif redis.call('HEXISTS', KEYS[1], field) == 0 then "
                    + "redis.call('DEL', candidate); "
                    + "else local firstSeen = tonumber(redis.call('GET', candidate)); "
                    + "if firstSeen == nil then "
                    + "redis.call('PSETEX', candidate, ARGV[2], tostring(now)); "
                    + "elseif now - firstSeen >= grace then "
                    + "removed = removed + redis.call('HDEL', KEYS[1], field); "
                    + "redis.call('DEL', candidate); "
                    + "end; end; end; return removed")
            .getBytes(StandardCharsets.UTF_8);
    private static final byte[] REFRESH_EXISTING_SCRIPT = (
            "local result = {}; "
                    + "for i = 1, #KEYS, 3 do "
                    + "local field = ARGV[2 + ((i + 2) / 3)]; "
                    + "redis.call('DEL', KEYS[i + 2]); "
                    + "if redis.call('HEXISTS', KEYS[i + 1], field) == 1 then "
                    + "redis.call('PSETEX', KEYS[i], ARGV[1], ARGV[2]); "
                    + "result[#result + 1] = 1; "
                    + "else redis.call('DEL', KEYS[i]); "
                    + "result[#result + 1] = 0; end; end; return result")
            .getBytes(StandardCharsets.UTF_8);
    private static final byte[] REMOVE_SESSION_SCRIPT = (
            "local removed = redis.call('HDEL', KEYS[1], ARGV[1]); "
                    + "redis.call('DEL', KEYS[2]); "
                    + "redis.call('DEL', KEYS[3]); "
                    + "return removed")
            .getBytes(StandardCharsets.UTF_8);
    private static final byte[] LEASED_SCRIPT = (
            "local result = {}; "
                    + "for i = 1, #KEYS do "
                    + "if redis.call('EXISTS', KEYS[i]) == 1 then "
                    + "result[#result + 1] = 1; "
                    + "else result[#result + 1] = 0; end; end; return result")
            .getBytes(StandardCharsets.UTF_8);

    private final RedisTemplate redisTemplate;
    private final PresenceProperties properties;
    private final JusicEnvironment environment;

    public SessionPresenceStore(RedisTemplate redisTemplate,
                                PresenceProperties properties,
                                JusicEnvironment environment) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.environment = environment;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * Atomically publishes a lease and its legacy HASH value. The explicit
     * serializers preserve the byte layout already used by RedisTemplate hash
     * operations; changing hash serializers would break existing data.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void storeSession(String houseId,
                             String sessionId,
                             String hashKey,
                             Object value) {
        if (!isEnabled()) {
            redisTemplate.opsForHash().put(hashKey, sessionId, value);
            return;
        }
        RedisSerializer keySerializer = redisTemplate.getKeySerializer();
        RedisSerializer hashKeySerializer = redisTemplate.getHashKeySerializer();
        RedisSerializer hashValueSerializer = redisTemplate.getHashValueSerializer();
        byte[] ttl = Long.toString(properties.requiredTtl())
                .getBytes(StandardCharsets.US_ASCII);
        redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.eval(
                        STORE_SESSION_SCRIPT,
                        ReturnType.INTEGER,
                        4,
                        keySerializer.serialize(markerKey(houseId, sessionId)),
                        keySerializer.serialize(hashKey),
                        keySerializer.serialize(roomIndexKey()),
                        keySerializer.serialize(candidateKey(houseId, sessionId)),
                        ttl,
                        MARKER_BYTES,
                        hashKeySerializer.serialize(sessionId),
                        hashValueSerializer.serialize(value),
                        houseId.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Renews every open node-local socket in bounded Redis-script batches. A
     * lease is refreshed only while the corresponding legacy HASH field still
     * exists, so heartbeat and cleanup cannot create marker/hash split-brain
     * state.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, List<String>> refresh(String sessionHashPrefix,
            Map<String, ? extends Collection<String>> sessionsByHouse) {
        if (!isEnabled() || sessionsByHouse == null || sessionsByHouse.isEmpty()) {
            return new java.util.LinkedHashMap<>();
        }
        int batchSize = properties.requiredRefreshBatchSize();
        RedisSerializer keySerializer = redisTemplate.getKeySerializer();
        RedisSerializer hashKeySerializer = redisTemplate.getHashKeySerializer();
        List<SessionReference> references = new ArrayList<>();
        for (Map.Entry<String, ? extends Collection<String>> entry
                : sessionsByHouse.entrySet()) {
            if (entry.getValue() != null) {
                for (String sessionId : entry.getValue()) {
                    references.add(new SessionReference(
                            entry.getKey(), sessionId));
                }
            }
        }
        if (references.isEmpty()) {
            return new java.util.LinkedHashMap<>();
        }

        byte[] ttl = Long.toString(properties.requiredTtl())
                .getBytes(StandardCharsets.US_ASCII);
        Map<String, List<String>> missing = new java.util.LinkedHashMap<>();
        for (int start = 0; start < references.size();) {
            int end = start + Math.min(
                    batchSize, references.size() - start);
            refreshBatch(sessionHashPrefix,
                    references.subList(start, end),
                    keySerializer,
                    hashKeySerializer,
                    ttl,
                    missing);
            start = end;
        }
        return missing;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void refreshBatch(String sessionHashPrefix,
                              List<SessionReference> references,
                              RedisSerializer keySerializer,
                              RedisSerializer hashKeySerializer,
                              byte[] ttl,
                              Map<String, List<String>> missing) {
        int sessionCount = references.size();
        int keyCount = sessionCount * 3;
        byte[][] keysAndArguments = new byte[keyCount + 2 + sessionCount][];
        for (int index = 0; index < sessionCount; index++) {
            SessionReference reference = references.get(index);
            keysAndArguments[index * 3] = keySerializer.serialize(
                    markerKey(reference.houseId, reference.sessionId));
            keysAndArguments[index * 3 + 1] = keySerializer.serialize(
                    sessionHashPrefix + reference.houseId);
            keysAndArguments[index * 3 + 2] = keySerializer.serialize(
                    candidateKey(reference.houseId, reference.sessionId));
            keysAndArguments[keyCount + 2 + index] =
                    hashKeySerializer.serialize(reference.sessionId);
        }
        keysAndArguments[keyCount] = ttl;
        keysAndArguments[keyCount + 1] = MARKER_BYTES;
        Object rawResult = redisTemplate.execute(
                (RedisCallback<Object>) connection -> connection.eval(
                        REFRESH_EXISTING_SCRIPT,
                        ReturnType.MULTI,
                        keyCount,
                        keysAndArguments));
        if (!(rawResult instanceof List)
                || ((List) rawResult).size() != sessionCount) {
            throw new IllegalStateException(
                    "invalid Redis presence heartbeat result: expected "
                            + sessionCount + " entries but got "
                            + (rawResult instanceof List
                            ? ((List) rawResult).size()
                            : String.valueOf(rawResult)));
        }

        List results = (List) rawResult;
        for (int index = 0; index < sessionCount; index++) {
            if (!isTruthy(results.get(index))) {
                SessionReference reference = references.get(index);
                missing.computeIfAbsent(
                        reference.houseId, ignored -> new ArrayList<>())
                        .add(reference.sessionId);
            }
        }
    }

    public boolean isAlive(String houseId, String sessionId) {
        List<Boolean> result = alive(
                houseId, java.util.Collections.singletonList(sessionId));
        return result.isEmpty() || Boolean.TRUE.equals(result.get(0));
    }

    public List<Boolean> alive(String houseId, Collection<String> sessionIds) {
        List<Boolean> result = new ArrayList<>();
        if (sessionIds == null || sessionIds.isEmpty()) {
            return result;
        }
        if (!isEnabled()) {
            for (String ignored : sessionIds) {
                result.add(Boolean.TRUE);
            }
            return result;
        }

        @SuppressWarnings("rawtypes")
        RedisSerializer keySerializer = redisTemplate.getKeySerializer();
        byte[][] keys = new byte[sessionIds.size()][];
        int index = 0;
        for (String sessionId : sessionIds) {
            keys[index++] = keySerializer.serialize(
                    markerKey(houseId, sessionId));
        }
        Object rawResult = redisTemplate.execute(
                (RedisCallback<Object>) connection -> connection.eval(
                        LEASED_SCRIPT,
                        ReturnType.MULTI,
                        keys.length,
                        keys));
        if (!(rawResult instanceof List)
                || ((List) rawResult).size() != sessionIds.size()) {
            throw new IllegalStateException(
                    "invalid Redis presence visibility result");
        }
        for (Object value : (List) rawResult) {
            result.add(Boolean.TRUE.equals(value)
                    || (value instanceof Number
                    && ((Number) value).longValue() > 0L));
        }
        return result;
    }

    public void remove(String houseId, String sessionId) {
        if (isEnabled()) {
            redisTemplate.delete(java.util.Arrays.asList(
                    markerKey(houseId, sessionId),
                    candidateKey(houseId, sessionId)));
        }
    }

    /**
     * Atomically removes the legacy HASH field, live marker, and GC candidate.
     * A concurrent store therefore wins wholly before or wholly after remove.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public long removeSession(String houseId,
                              String sessionId,
                              String hashKey) {
        if (!isEnabled()) {
            Long removed = redisTemplate.opsForHash()
                    .delete(hashKey, sessionId);
            return removed == null ? 0L : removed;
        }
        RedisSerializer keySerializer = redisTemplate.getKeySerializer();
        RedisSerializer hashKeySerializer = redisTemplate.getHashKeySerializer();
        Object result = redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.eval(
                        REMOVE_SESSION_SCRIPT,
                        ReturnType.INTEGER,
                        3,
                        keySerializer.serialize(hashKey),
                        keySerializer.serialize(markerKey(houseId, sessionId)),
                        keySerializer.serialize(candidateKey(houseId, sessionId)),
                        hashKeySerializer.serialize(sessionId)));
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    public void remove(String houseId, Collection<String> sessionIds) {
        if (!isEnabled() || sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>();
        for (String sessionId : sessionIds) {
            keys.add(markerKey(houseId, sessionId));
            keys.add(candidateKey(houseId, sessionId));
        }
        redisTemplate.delete(keys);
    }

    @SuppressWarnings("unchecked")
    public Set<String> trackedHouseIds() {
        if (!isEnabled()) {
            return new LinkedHashSet<>();
        }
        RedisSerializer<String> keySerializer =
                (RedisSerializer<String>) redisTemplate.getKeySerializer();
        Set<byte[]> raw = (Set<byte[]>) redisTemplate.execute(
                (RedisCallback<Set<byte[]>>) connection ->
                        connection.sMembers(keySerializer.serialize(
                                roomIndexKey())));
        Set<String> result = new LinkedHashSet<>();
        if (raw != null) {
            for (byte[] member : raw) {
                if (member != null) {
                    result.add(new String(member, StandardCharsets.UTF_8));
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public void trackHouses(Collection<String> houseIds) {
        if (!isEnabled() || houseIds == null || houseIds.isEmpty()) {
            return;
        }
        RedisSerializer<String> keySerializer =
                (RedisSerializer<String>) redisTemplate.getKeySerializer();
        byte[][] members = new byte[houseIds.size()][];
        int index = 0;
        for (String houseId : houseIds) {
            members[index++] = houseId.getBytes(StandardCharsets.UTF_8);
        }
        redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.sAdd(
                        keySerializer.serialize(roomIndexKey()), members));
    }

    @SuppressWarnings("unchecked")
    public void untrackHouse(String houseId) {
        if (!isEnabled() || houseId == null) {
            return;
        }
        RedisSerializer<String> keySerializer =
                (RedisSerializer<String>) redisTemplate.getKeySerializer();
        redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.sRem(
                        keySerializer.serialize(roomIndexKey()),
                        houseId.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Advances two-stage garbage collection. The first missing-marker check
     * records Redis server time in a short-lived candidate key. A later check
     * removes the legacy HASH field only after the configured grace elapsed
     * and only if the marker is still absent in the same atomic script.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public long pruneHashFieldsIfExpired(String houseId,
                                         String hashKey,
                                         Collection<String> sessionIds) {
        if (!isEnabled() || sessionIds == null || sessionIds.isEmpty()) {
            return 0L;
        }
        RedisSerializer keySerializer = redisTemplate.getKeySerializer();
        RedisSerializer hashKeySerializer = redisTemplate.getHashKeySerializer();
        int count = sessionIds.size();
        int keyCount = 1 + (count * 2);
        byte[][] keysAndArguments = new byte[keyCount + 2 + count][];
        keysAndArguments[0] = keySerializer.serialize(hashKey);
        int index = 0;
        for (String sessionId : sessionIds) {
            keysAndArguments[1 + (index * 2)] = keySerializer.serialize(
                    markerKey(houseId, sessionId));
            keysAndArguments[2 + (index * 2)] = keySerializer.serialize(
                    candidateKey(houseId, sessionId));
            keysAndArguments[keyCount + 2 + index] =
                    hashKeySerializer.serialize(sessionId);
            index++;
        }
        keysAndArguments[keyCount] = Long.toString(
                properties.requiredCandidateGrace())
                .getBytes(StandardCharsets.US_ASCII);
        keysAndArguments[keyCount + 1] = Long.toString(
                properties.requiredCandidateTtl())
                .getBytes(StandardCharsets.US_ASCII);
        Object result = redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.eval(
                        PRUNE_EXPIRED_SCRIPT,
                        ReturnType.INTEGER,
                        keyCount,
                        keysAndArguments));
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    String markerKey(String houseId, String sessionId) {
        return properties.getNamespace()
                + ":" + environment.getServerPort()
                + ":" + encode(houseId)
                + ":" + encode(sessionId);
    }

    String candidateKey(String houseId, String sessionId) {
        return properties.getNamespace()
                + ":" + environment.getServerPort()
                + ":gc"
                + ":" + encode(houseId)
                + ":" + encode(sessionId);
    }

    String roomIndexKey() {
        return properties.getNamespace()
                + ":" + environment.getServerPort()
                + ":rooms";
    }

    private boolean isTruthy(Object value) {
        if (Boolean.TRUE.equals(value)) {
            return true;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue() > 0L;
        }
        if (value instanceof byte[]) {
            return "1".equals(new String(
                    (byte[]) value, StandardCharsets.US_ASCII));
        }
        return value != null && "1".equals(value.toString());
    }

    private String encode(String value) {
        if (value == null) {
            throw new IllegalArgumentException("presence key part must not be null");
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class SessionReference {
        private final String houseId;
        private final String sessionId;

        private SessionReference(String houseId, String sessionId) {
            this.houseId = houseId;
            this.sessionId = sessionId;
        }
    }
}
