package com.scoder.jusic.configuration;

/**
 * Signals that a local WebSocket session cannot be admitted without violating
 * a connection-container invariant.
 */
public class ConnectionAdmissionException extends IllegalStateException {

    public enum Reason {
        NODE_LIMIT,
        ROOM_LIMIT,
        IP_LIMIT,
        SESSION_ID_CONFLICT
    }

    private final Reason reason;
    private final int limit;
    private final int current;

    private ConnectionAdmissionException(Reason reason, String message, int limit, int current) {
        super(message);
        this.reason = reason;
        this.limit = limit;
        this.current = current;
    }

    static ConnectionAdmissionException capacity(Reason reason, int limit, int current) {
        return new ConnectionAdmissionException(
                reason,
                "WebSocket connection rejected: " + reason + " (current=" + current + ", limit=" + limit + ")",
                limit,
                current
        );
    }

    static ConnectionAdmissionException sessionIdConflict(String sessionId) {
        return new ConnectionAdmissionException(
                Reason.SESSION_ID_CONFLICT,
                "WebSocket connection rejected: session id is already owned by another session: " + sessionId,
                1,
                1
        );
    }

    public Reason getReason() {
        return reason;
    }

    public int getLimit() {
        return limit;
    }

    public int getCurrent() {
        return current;
    }
}
