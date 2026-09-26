package com.scoder.jusic.configuration;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Component
public class HouseOperationLocks {

    private static final int STRIPE_COUNT = 256;
    private final ReentrantLock[] locks = new ReentrantLock[STRIPE_COUNT];

    public HouseOperationLocks() {
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new ReentrantLock();
        }
    }

    public ReentrantLock getLock(String houseId) {
        return locks[stripeIndex(houseId)];
    }

    /**
     * Acquire two room stripes in one deterministic global order. This is used
     * by room moves so opposite-direction switches cannot deadlock and neither
     * source nor target can be reclaimed until the move finishes.
     */
    public LockScope lockBoth(String firstHouseId, String secondHouseId) {
        int firstIndex = stripeIndex(firstHouseId);
        int secondIndex = stripeIndex(secondHouseId);
        ReentrantLock firstLock = locks[Math.min(firstIndex, secondIndex)];
        ReentrantLock secondLock = locks[Math.max(firstIndex, secondIndex)];
        firstLock.lock();
        if (secondLock != firstLock) {
            secondLock.lock();
        }
        return new LockScope(firstLock, secondLock);
    }

    private int stripeIndex(String houseId) {
        int hash = houseId == null ? 0 : houseId.hashCode();
        hash ^= (hash >>> 16);
        return hash & (STRIPE_COUNT - 1);
    }

    /** Striped locks are bounded and live for the process lifetime. */
    public void remove(String houseId) {
        // no-op by design; removing a lock can create two concurrent locks for one room
    }

    public static final class LockScope implements AutoCloseable {
        private final ReentrantLock firstLock;
        private final ReentrantLock secondLock;
        private boolean closed;

        private LockScope(ReentrantLock firstLock, ReentrantLock secondLock) {
            this.firstLock = firstLock;
            this.secondLock = secondLock;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (secondLock != firstLock) {
                secondLock.unlock();
            }
            firstLock.unlock();
        }
    }
}
