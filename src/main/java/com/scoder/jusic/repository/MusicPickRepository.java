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

}
