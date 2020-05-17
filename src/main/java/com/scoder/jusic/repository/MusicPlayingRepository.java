package com.scoder.jusic.repository;

import com.scoder.jusic.model.Music;

/**
 * @author H
 */
public interface MusicPlayingRepository {

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
     * from pick list to playing list.
     *
     * @return {@link Music}
     */
    Music pickToPlaying();

    /**
     * 清理播放列表，除了 index = 0
     */
    void keepTheOne();

    /**
     * get playing.
     *
     * @return {@link Music}
     */
    Music getPlaying();

}
