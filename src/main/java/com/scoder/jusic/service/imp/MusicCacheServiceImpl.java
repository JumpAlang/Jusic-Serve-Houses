package com.scoder.jusic.service.imp;

import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.model.Music;
import com.scoder.jusic.security.ProviderCallExecutor;
import com.scoder.jusic.service.MusicCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of MusicCacheService using RedisTemplate with Jackson serializer.
 */
@Service
@Slf4j
public class MusicCacheServiceImpl implements MusicCacheService {

    private static final String SONG_KEY_PREFIX = "jusic:cache:song:";
    private static final String URL_KEY_PREFIX = "jusic:cache:url:";
    private static final String SEARCH_KEY_PREFIX = "jusic:cache:search:";
    private static final long FAILURE_LOG_INTERVAL_MS = 30_000L;

    @Autowired
    private RedisTemplate redisTemplate;
    private final AtomicLong lastFailureLog = new AtomicLong();

    @Override
    public Music getSong(String source, String id) {
        if (source == null || id == null || shouldSkipCacheCall()) {
            return null;
        }
        try {
            String key = SONG_KEY_PREFIX + source + ":" + id;
            return (Music) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logCacheFailure("get song", e);
            return null;
        }
    }

    @Override
    public void putSong(String source, String id, Music music) {
        if (source == null || id == null || music == null
                || shouldSkipCacheCall()) {
            return;
        }
        try {
            String key = SONG_KEY_PREFIX + source + ":" + id;
            // Cache metadata for 24 hours
            redisTemplate.opsForValue().set(key, music, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            logCacheFailure("put song", e);
        }
    }

    @Override
    public String getPlayUrl(String source, String id, String quality) {
        if (source == null || id == null || shouldSkipCacheCall()) {
            return null;
        }
        try {
            String q = quality == null ? "default" : quality;
            String key = URL_KEY_PREFIX + source + ":" + id + ":" + q;
            return (String) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logCacheFailure("get play URL", e);
            return null;
        }
    }

    @Override
    public void putPlayUrl(String source, String id, String quality, String url) {
        if (source == null || id == null || url == null
                || shouldSkipCacheCall()) {
            return;
        }
        try {
            String q = quality == null ? "default" : quality;
            String key = URL_KEY_PREFIX + source + ":" + id + ":" + q;
            // Cache play URL for 15 minutes
            redisTemplate.opsForValue().set(key, url, 15, TimeUnit.MINUTES);
        } catch (Exception e) {
            logCacheFailure("put play URL", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public HulkPage<List<Music>> getSearch(String source, String keyword, int page, int size) {
        if (source == null || keyword == null || shouldSkipCacheCall()) {
            return null;
        }
        try {
            String hash = sha256(keyword);
            String key = SEARCH_KEY_PREFIX + source + ":" + hash + ":" + page + ":" + size;
            return (HulkPage<List<Music>>) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logCacheFailure("get search", e);
            return null;
        }
    }

    @Override
    public void putSearch(String source, String keyword, int page, int size, HulkPage<List<Music>> result) {
        if (source == null || keyword == null || result == null
                || shouldSkipCacheCall()) {
            return;
        }
        try {
            String hash = sha256(keyword);
            String key = SEARCH_KEY_PREFIX + source + ":" + hash + ":" + page + ":" + size;
            // Cache search result for 10 minutes
            redisTemplate.opsForValue().set(key, result, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            logCacheFailure("put search", e);
        }
    }

    private void logCacheFailure(String operation, Exception exception) {
        if (isInterruption(exception)) {
            // Future.cancel(true) may arrive after the pre-call check and
            // while Lettuce is waiting for SETEX/GET. Restore the flag that
            // CompletableFuture.get clears and let the provider worker stop;
            // cache I/O is best-effort and this is not a Redis outage.
            Thread.currentThread().interrupt();
            return;
        }
        long now = System.currentTimeMillis();
        long previous = lastFailureLog.get();
        if (now - previous >= FAILURE_LOG_INTERVAL_MS
                && lastFailureLog.compareAndSet(previous, now)) {
            log.warn("Music cache unavailable during {}: {}",
                    operation, exception.getMessage());
            log.debug("Music cache failure", exception);
        }
    }

    private boolean shouldSkipCacheCall() {
        return ProviderCallExecutor.shouldAbortCurrentCall();
    }

    private boolean isInterruption(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof InterruptedException
                    || "RedisCommandInterruptedException".equals(
                    current.getClass().getSimpleName())) {
                return true;
            }
            current = current.getCause();
        }
        return Thread.currentThread().isInterrupted();
    }

    private String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            return String.valueOf(base.hashCode());
        }
    }
}
