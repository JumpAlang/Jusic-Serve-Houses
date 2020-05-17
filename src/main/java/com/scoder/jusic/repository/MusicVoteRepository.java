package com.scoder.jusic.repository;

import java.util.Set;

/**
 * @author H
 */
public interface MusicVoteRepository {

    /**
     * destroy
     *
     * @return -
     */
    Long destroy();

    /**
     * add to set
     *
     * @param value value
     * @return -
     */
    Long add(Object... value);

    /**
     * size
     *
     * @return -
     */
    Long size();

    /**
     * 清空 set
     */
    void reset();

    /**
     * members
     *
     * @return -
     */
    Set members();
}
