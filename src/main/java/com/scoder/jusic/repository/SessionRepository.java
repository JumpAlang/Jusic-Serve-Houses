package com.scoder.jusic.repository;

import com.scoder.jusic.model.User;

import java.util.List;

/**
 * @author H
 */
public interface SessionRepository {

    /**
     * destroy
     *
     * @return long
     */
    Boolean destroy(String houseId);

    /**
     * get session
     *
     * @param sessionId session id
     * @return User {@link User}
     */
    User getSession(String sessionId,String houseId);

    /**
     * set session
     *
     * @param user User {@link User}
     */
    void setSession(User user,String houseId);

    /**
     * size
     *
     * @return size
     */
    Long size(String houseId);

    /**
     * remove session.
     *
     * @param sessionId session id
     * @return -
     */
    Long removeSession(String sessionId,String houseId);

    List getSession(String houseId);

    }
