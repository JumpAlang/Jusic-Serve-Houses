package com.scoder.jusic.service;

import com.scoder.jusic.common.page.HulkPage;
import com.scoder.jusic.model.Music;

import java.util.List;

/**
 * Service interface for caching third-party music metadata, play URLs, and search results.
 */
public interface MusicCacheService {

    /**
     * Get cached music metadata.
     */
    Music getSong(String source, String id);

    /**
     * Cache music metadata (long-lived).
     */
    void putSong(String source, String id, Music music);

    /**
     * Get cached play URL.
     */
    String getPlayUrl(String source, String id, String quality);

    /**
     * Cache play URL (short-lived, 15 minutes).
     */
    void putPlayUrl(String source, String id, String quality, String url);

    /**
     * Get cached search result.
     */
    HulkPage<List<Music>> getSearch(String source, String keyword, int page, int size);

    /**
     * Cache search result (10 minutes).
     */
    void putSearch(String source, String keyword, int page, int size, HulkPage<List<Music>> result);
}
