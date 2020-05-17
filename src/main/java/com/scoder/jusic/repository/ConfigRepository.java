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
    Long destroy();

    /**
     * initialize
     */
    void initialize();

    /**
     * get
     *
     * @param hashKey hash key
     * @return -
     */
    Object get(Object hashKey);

    /**
     * put
     *
     * @param hashKey hash key
     * @param value   value
     */
    void put(Object hashKey, Object value);

    /**
     * put all
     *
     * @param map map, k->v
     */
    void putAll(Map<String, Object> map);

    /**
     * get password
     *
     * @param role role: root | admin
     * @return password
     */
    String getPassword(String role);

    /**
     * set password
     *
     * @param role     role: root | admin
     * @param password password
     */
    void setPassword(String role, String password);

    /**
     * init root's password
     */
    void initRootPassword();

    /**
     * init admin's password
     */
    void initAdminPassword();

    /**
     * get root password
     *
     * @return password
     */
    String getRootPassword();

    /**
     * get admin password
     *
     * @return password
     */
    String getAdminPassword();

    /**
     * get last music duration
     *
     * @return millisecond
     */
    Long getLastMusicDuration();

    /**
     * set last music duration
     *
     * @param duration duration (millisecond)
     */
    void setLastMusicDuration(Long duration);

    /**
     * get last music push time.
     *
     * @return push time (millisecond)
     */
    Long getLastMusicPushTime();

    /**
     * set last music push time.
     *
     * @param pushTime push time (millisecond)
     */
    void setLastMusicPushTime(Long pushTime);

    /**
     * set last music push time and duration.
     *
     * @param pushTime last music push time （millisecond）
     * @param duration last music duration （millisecond）
     */
    void setLastMusicPushTimeAndDuration(Long pushTime, Long duration);

    /**
     * get push switch.
     *
     * @return boolean
     */
    Boolean getPushSwitch();

    /**
     * set push switch
     *
     * @param pushSwitch push switch
     */
    void setPushSwitch(boolean pushSwitch);

    /**
     * get vote rate
     *
     * @return -
     */
    Float getVoteRate();

    Boolean getEnableSwitch();
    void setEnableSwitch(boolean enableSwitch);

    Boolean getEnableSearch();
    void setEnableSearch(boolean enableSearch);

    Boolean getGoodModel();
    void setGoodModel(boolean goodModel);
}
