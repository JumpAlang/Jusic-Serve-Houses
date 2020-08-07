package com.scoder.jusic.repository;

import java.util.Map;

/**
 * @author H
 */
public interface ConfigRepository {

    /**
     * destroy
     *
     * @return -
     */
    Boolean destroy(String houseId);


    /**
     * initialize
     */
    void initialize(String houseId);

    /**
     * get
     *
     * @param hashKey hash key
     * @return -
     */
    Object get(Object hashKey,String houseId);

    /**
     * put
     *
     * @param hashKey hash key
     * @param value   value
     */
    void put(Object hashKey, Object value,String houseId);

    /**
     * put all
     *
     * @param map map, k->v
     */
    void putAll(Map<String, Object> map,String houseId);

    /**
     * get password
     *
     * @param role role: root | admin
     * @return password
     */
    String getPassword(String role,String houseId);

    /**
     * set password
     *
     * @param role     role: root | admin
     * @param password password
     */
    void setPassword(String role, String password,String houseId);

    /**
     * init root's password
     */
    void initRootPassword(String houseId);

    /**
     * init admin's password
     */
    void initAdminPassword(String houseId);

    /**
     * get root password
     *
     * @return password
     */
    String getRootPassword(String houseId);

    /**
     * get admin password
     *
     * @return password
     */
    String getAdminPassword(String houseId);

    /**
     * get last music duration
     *
     * @return millisecond
     */
    Long getLastMusicDuration(String houseId);

    /**
     * set last music duration
     *
     * @param duration duration (millisecond)
     */
    void setLastMusicDuration(Long duration,String houseId);

    /**
     * get last music push time.
     *
     * @return push time (millisecond)
     */
    Long getLastMusicPushTime(String houseId);

    /**
     * set last music push time.
     *
     * @param pushTime push time (millisecond)
     */
    void setLastMusicPushTime(Long pushTime,String houseId);

    /**
     * set last music push time and duration.
     *
     * @param pushTime last music push time （millisecond）
     * @param duration last music duration （millisecond）
     */
    void setLastMusicPushTimeAndDuration(Long pushTime, Long duration,String houseId);

    /**
     * get push switch.
     *
     * @return boolean
     */
    Boolean getPushSwitch(String houseId);

    /**
     * set push switch
     *
     * @param pushSwitch push switch
     */
    void setPushSwitch(boolean pushSwitch,String houseId);

    /**
     * get vote rate
     *
     * @return -
     */
    Float getVoteRate(String houseId);
    void setVoteRate(Float voteRate,String houseId);

    Boolean getEnableSwitch(String houseId);
    void setEnableSwitch(boolean enableSwitch,String houseId);

    Boolean getEnableSearch(String houseId);
    void setEnableSearch(boolean enableSearch,String houseId);

    Boolean getGoodModel(String houseId);
    void setGoodModel(boolean goodModel,String houseId);

    Boolean getRandomModel(String houseId);
    void setRandomModel(boolean randomModel,String houseId);

    void setAdminPassword(String password, String houseId);
    void setRootPassword(String password, String houseId);

}
