package com.scoder.jusic.repository;

/**
 * @author H
 */
public interface MusicDefaultRepository {

    /**
     * destroy
     *
     * @return -
     */
    Boolean destroy(String houseId);

    /**
     * initialize
     *
     * @return -
     */
    Long initialize(String houseId);

    /**
     * size
     *
     * @return -
     */
    Long size(String houseId);

    /**
     * get random member
     *
     * @return random member
     */
    String randomMember(String houseId);

    void remove(String id,String houseId);

    /**
     * add value
     *
     * @param value ...value
     * @return long
     */
    Long add(String[] value,String houseId);

}
