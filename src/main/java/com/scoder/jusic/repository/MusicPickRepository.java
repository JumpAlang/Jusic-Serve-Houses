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
    void destroy();

    /**
     * left push
     *
     * @param pick music
     * @return 0 or 1
     */
    Long leftPush(Music pick);

    /**
     * left push all.
     *
     * @param value value
     * @return -
     */
    Long leftPushAll(Object... value);

    /**
     * right push all
     *
     * @param value value
     * @return -
     */
    Long rightPushAll(Object... value);

    /**
     * get size
     *
     * @return -
     */
    Long size();

    /**
     * clear the pick list.
     */
    void reset();

    /**
     * get all pick music.
     *
     * @return LinkedList
     */
    List<Music> getPickMusicList();

}
