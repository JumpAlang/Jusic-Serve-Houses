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
    Long destroy();

    /**
     * initialize
     *
     * @return -
     */
    Long initialize();

    /**
     * size
     *
     * @return -
     */
    Long size();

    /**
     * get random member
     *
     * @return random member
     */
    String randomMember();

    /**
     * add value
     *
     * @param value ...value
     * @return long
     */
    Long add(String[] value);

}
