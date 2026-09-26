package com.scoder.jusic.configuration;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Bounded node-local serialization for commands issued by one WebSocket
 * session. A fixed stripe array avoids retaining a lock object per disconnected
 * user while preventing two room transitions from interleaving their replies.
 */
@Component
public class SessionCommandLocks {

    private static final int STRIPE_COUNT = 1024;
    private final ReentrantLock[] locks = new ReentrantLock[STRIPE_COUNT];

    public SessionCommandLocks() {
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new ReentrantLock();
        }
    }

    public ReentrantLock getLock(String sessionId) {
        int hash = sessionId == null ? 0 : sessionId.hashCode();
        hash ^= (hash >>> 16);
        return locks[hash & (STRIPE_COUNT - 1)];
    }
}
