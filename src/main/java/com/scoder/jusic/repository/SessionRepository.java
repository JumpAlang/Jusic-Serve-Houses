package com.scoder.jusic.repository;

import com.scoder.jusic.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** Return the unfiltered legacy Redis HASH size without advancing GC. */
    Long storedSize(String houseId);

    /**
     * remove session.
     *
     * @param sessionId session id
     * @return -
     */
    Long removeSession(String sessionId,String houseId);

    List<User> getSession(String houseId);

    /** Renew all node-local WebSocket leases in one bounded Redis pipeline. */
    Map<String, List<String>> refreshPresence(
            Map<String, ? extends Collection<String>> sessionsByHouse);

    /**
     * Advance two-stage GC for users whose lease no longer exists.
     * Node-local open sessions are protected even when their heartbeat was
     * delayed long enough for the Redis lease to expire.
     */
    long pruneExpiredPresence(String houseId,
                              Collection<String> protectedSessionIds);

    /** Backward-compatible cleanup entry point for callers without protection. */
    default long pruneExpiredPresence(String houseId) {
        return pruneExpiredPresence(houseId,
                java.util.Collections.emptyList());
    }

    /** Discover legacy session HASH rooms, including crashed temporary rooms. */
    Set<String> findPresenceHouseIds();

    /** Add a room to the durable cleanup retry index. */
    void trackPresenceHouse(String houseId);

    /** Remove a room from the durable cleanup retry index after full cleanup. */
    void untrackPresenceHouse(String houseId);

}
