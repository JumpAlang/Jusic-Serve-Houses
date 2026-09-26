package com.scoder.jusic.configuration;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Bounded node-local serialization for mutations of one room's legacy music
 * state.
 *
 * <p>This lock is deliberately separate from {@link HouseOperationLocks} so a
 * future migration can narrow room lifecycle critical sections. The current
 * compatibility slice covers point-song commits, playback selection, the
 * playback job's state commit, exact delete/top/clear mutations and old-room
 * queue cleanup after a room switch. Legacy order/good whole-list rewrites are
 * not yet versioned and must not be treated as safe against concurrent queue
 * changes.</p>
 */
@Component
public class RoomMusicLocks {

    private static final int STRIPE_COUNT = 1024;
    private final ReentrantLock[] locks = new ReentrantLock[STRIPE_COUNT];

    public RoomMusicLocks() {
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new ReentrantLock();
        }
    }

    public ReentrantLock getLock(String houseId) {
        int hash = houseId == null ? 0 : houseId.hashCode();
        hash ^= (hash >>> 16);
        return locks[hash & (STRIPE_COUNT - 1)];
    }
}
