package com.scoder.jusic.repository;

import com.scoder.jusic.model.User;

import java.util.Set;

/**
 * @author H
 */
public interface SessionBlackRepository {

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
    User getSession(String sessionId,String ip,String houseId);

    /**
     * set session
     *
     * @param user User {@link User}
     */
    void setSession(User user,String houseId);

    /**
     * remove session.
     *
     * @param sessionId session id
     * @return -
     */
    Long removeSession(String sessionId,String houseId);

    Set showBlackList(String houseId);
}
