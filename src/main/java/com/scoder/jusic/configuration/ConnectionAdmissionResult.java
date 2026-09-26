package com.scoder.jusic.configuration;

import org.springframework.web.socket.WebSocketSession;

/**
 * Result of an atomic local session-container mutation.
 */
public final class ConnectionAdmissionResult {

    public enum Outcome {
        REGISTERED,
        MOVED,
        ALREADY_REGISTERED,
        REMOVED,
        ALREADY_REMOVED,
        STALE_SESSION
    }

    private final Outcome outcome;
    private final WebSocketSession session;
    private final String previousHouseId;
    private final String houseId;

    ConnectionAdmissionResult(Outcome outcome,
                              WebSocketSession session,
                              String previousHouseId,
                              String houseId) {
        this.outcome = outcome;
        this.session = session;
        this.previousHouseId = previousHouseId;
        this.houseId = houseId;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public String getPreviousHouseId() {
        return previousHouseId;
    }

    public String getHouseId() {
        return houseId;
    }

    /**
     * Whether this call changed membership or counters.
     */
    public boolean isChanged() {
        return outcome == Outcome.REGISTERED
                || outcome == Outcome.MOVED
                || outcome == Outcome.REMOVED;
    }
}
