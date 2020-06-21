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
    Boolean destroy(String houseId);

    /**
     * add to set
     *
     * @param value value
     * @return -
     */
    Long add(String houseId,Object... value);

    /**
     * size
     *
     * @return -
     */
    Long size(String houseId);

    /**
     * 清空 set
     */
    void reset(String houseId);

    /**
     * members
     *
     * @return -
     */
    Set members(String houseId);

    Long remove(String sessionId,String houseId);

    }
