package com.scoder.jusic.repository;

import com.scoder.jusic.model.Music;

import java.util.List;

/**
 * @author H
 */
public interface MusicPickRepository {

    /**
     * destroy
     */
    Boolean destroy(String houseId);

    /**
     * left push
     *
     * @param pick music
     * @return 0 or 1
     */
    Long leftPush(Music pick,String houseId);

    /**
     * left push all.
     *
     * @param value value
     * @return -
     */
    Long leftPushAll(String houseId,Object... value);

    /**
     * right push all
     *
     * @param value value
     * @return -
     */
    Long rightPushAll(String houseId,Object... value);

    /**
     * get size
     *
     * @return -
     */
    Long size(String houseId);

    /**
     * clear the pick list.
     */
    void reset(String houseId);

    /**
     * get all pick music.
     *
     * @return LinkedList
     */
    List<Music> getPickMusicList(String houseId);

    /**
     * Removes one exact serialized queue entry. Unlike a read/clear/rewrite
     * sequence, an unrelated concurrent LPUSH can never be overwritten.
     *
     * @return number of entries actually removed (zero or one)
     */
    long removeOne(Music music, String houseId);

    /**
     * Atomically replaces one exact serialized queue entry and moves it to
     * the tail (the next-play position in the legacy list layout).
     *
     * <p>The old value is serialized before {@code topTime} is changed, then
     * one single-key Lua command performs LREM + RPUSH. Concurrent point-song
     * LPUSH commands therefore remain in the queue.</p>
     *
     * @return one when the entry was moved, otherwise zero
     */
    long moveExactToTail(Music music, long topTime, String houseId);

    /**
     * Removes every pending queue item attributed to one WebSocket session.
     * The currently playing item is stored separately and is not affected.
     *
     * @return number of list entries actually removed
     */
    long removeQueuedMusicBySession(String sessionId, String houseId);

}
