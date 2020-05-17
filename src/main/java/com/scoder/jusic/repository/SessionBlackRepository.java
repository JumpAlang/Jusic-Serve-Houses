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
    Long destroy();

    /**
     * get session
     *
     * @param sessionId session id
     * @return User {@link User}
     */
    User getSession(String sessionId);

    /**
     * set session
     *
     * @param user User {@link User}
     */
    void setSession(User user);

    /**
     * remove session.
     *
     * @param sessionId session id
     * @return -
     */
    Long removeSession(String sessionId);

    Set showBlackList();
}
