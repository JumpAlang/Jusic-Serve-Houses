package com.scoder.jusic.repository;

import com.scoder.jusic.model.User;

/**
 * @author H
 */
public interface SessionRepository {

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
     * size
     *
     * @return size
     */
    Long size();

    /**
     * remove session.
     *
     * @param sessionId session id
     * @return -
     */
    Long removeSession(String sessionId);

}
