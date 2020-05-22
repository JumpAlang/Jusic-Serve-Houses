package com.scoder.jusic.repository;

import java.util.Set;

/**
 * @author H
 */
public interface MusicBlackRepository {

    Boolean destroy(String houseId);
    /**
     * is member ?
     *
     * @param id music id
     * @return boolean
     */
    boolean isMember(String id,String houseId);

    /**
     * add value
     *
     * @param value value
     * @return long
     */
    Long add(String value,String houseId);

    /**
     * remove
     *
     * @param id music id
     * @return -
     */
    Long remove(String id,String houseId);

    Set showBlackList(String houseId);
}
