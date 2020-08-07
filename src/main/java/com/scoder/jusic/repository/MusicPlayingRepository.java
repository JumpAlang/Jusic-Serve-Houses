package com.scoder.jusic.repository;

import com.scoder.jusic.model.Music;

/**
 * @author H
 */
public interface MusicPlayingRepository {

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
     * from pick list to playing list.
     *
     * @return {@link Music}
     */
    Music pickToPlaying(String houseId);

    Music randomToPlaying(String houseId);

    /**
     * 清理播放列表，除了 index = 0
     */
    void keepTheOne(String houseId);

    /**
     * get playing.
     *
     * @return {@link Music}
     */
    Music getPlaying(String houseId);

}
