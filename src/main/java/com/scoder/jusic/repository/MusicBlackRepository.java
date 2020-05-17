package com.scoder.jusic.repository;

import java.util.Set;

/**
 * @author H
 */
public interface MusicBlackRepository {

    /**
     * is member ?
     *
     * @param id music id
     * @return boolean
     */
    boolean isMember(String id);

    /**
     * add value
     *
     * @param value value
     * @return long
     */
    Long add(String value);

    /**
     * remove
     *
     * @param id music id
     * @return -
     */
    Long remove(String id);

    Set showBlackList();
}
