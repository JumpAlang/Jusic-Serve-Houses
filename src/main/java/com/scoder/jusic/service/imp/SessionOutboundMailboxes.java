package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.OutboundMailboxProperties;
import com.scoder.jusic.handler.SerializedWebSocketSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Per-connection ordered outbound mailboxes.
 *
 * <p>Each mailbox schedules at most one drain task, which makes
 * {@link WebSocketSession#sendMessage(org.springframework.web.socket.WebSocketMessage)}
 * single-writer and FIFO for that session. Different sessions drain
 * independently on a bounded executor.</p>
 */
@Component
@Slf4j
public class SessionOutboundMailboxes {

    public enum DispatchResult {
        ACCEPTED,
        OVERFLOW,
        EXECUTOR_REJECTED,
        SESSION_CLOSED;

        public boolean requiresPresenceCleanup() {
            return this != ACCEPTED;
        }
    }

    private final ConcurrentMap<String, Mailbox> mailboxes = new ConcurrentHashMap<>();
    private final Executor executor;
    private final int maxPendingMessages;
    private final long maxPendingBytes;

    public SessionOutboundMailboxes(
            @Qualifier("webSocketOutboundExecutor") Executor executor,
            OutboundMailboxProperties properties) {
        this.executor = executor;
        this.maxPendingMessages = Math.max(1, properties.getMaxPendingMessages());
        this.maxPendingBytes = Math.max(1L, properties.getMaxPendingBytes());
    }

    public DispatchResult dispatch(WebSocketSession session, TextMessage message) {
        return dispatch(session, PreparedMessage.from(message));
    }

    DispatchResult dispatch(WebSocketSession session, PreparedMessage message) {
        if (!session.isOpen()) {
            discard(session);
            return DispatchResult.SESSION_CLOSED;
        }

        Mailbox mailbox = mailboxes.compute(session.getId(), (sessionId, current) -> {
            if (current == null || !current.belongsTo(session)) {
                if (current != null) {
                    current.discard();
                }
                return new Mailbox(
                        session,
                        SerializedWebSocketSession.outboundSession(session));
            }
            return current;
        });
        return mailbox.enqueue(message);
    }

    public void discard(WebSocketSession session) {
        Mailbox mailbox = mailboxes.get(session.getId());
        if (mailbox != null && mailbox.belongsTo(session) && mailboxes.remove(session.getId(), mailbox)) {
            mailbox.discard();
        }
    }

    public void discard(String sessionId) {
        Mailbox mailbox = mailboxes.remove(sessionId);
        if (mailbox != null) {
            mailbox.discard();
        }
    }

    int mailboxCount() {
        return mailboxes.size();
    }

    private final class Mailbox implements Runnable {

        private final WebSocketSession session;
        private final WebSocketSession outboundSession;
        private final Deque<PendingMessage> queue = new ArrayDeque<>();

        private int pendingMessages;
        private long pendingBytes;
        private boolean drainScheduled;
        private boolean discarded;

        private Mailbox(WebSocketSession session,
                        WebSocketSession outboundSession) {
            this.session = session;
            this.outboundSession = outboundSession;
        }

        private boolean belongsTo(WebSocketSession candidate) {
            return this.session == candidate;
        }

        private DispatchResult enqueue(PreparedMessage message) {
            int bytes = message.bytes;
            boolean scheduleDrain = false;
            boolean overflow = false;

            synchronized (this) {
                if (discarded || !session.isOpen()) {
                    return DispatchResult.SESSION_CLOSED;
                }
                if (pendingMessages >= maxPendingMessages
                        || bytes > maxPendingBytes - pendingBytes) {
                    discardLocked();
                    overflow = true;
                } else {
                    queue.addLast(new PendingMessage(message.message, bytes));
                    pendingMessages++;
                    pendingBytes += bytes;
                    if (!drainScheduled) {
                        drainScheduled = true;
                        scheduleDrain = true;
                    }
                }
            }

            if (overflow) {
                mailboxes.remove(session.getId(), this);
                close(session, CloseStatus.SERVICE_OVERLOAD, "outbound mailbox overflow", null);
                return DispatchResult.OVERFLOW;
            }

            if (scheduleDrain) {
                try {
                    executor.execute(this);
                } catch (RejectedExecutionException exception) {
                    discard();
                    mailboxes.remove(session.getId(), this);
                    close(session, CloseStatus.SERVICE_OVERLOAD, "outbound executor rejected", exception);
                    return DispatchResult.EXECUTOR_REJECTED;
                }
            }
            return DispatchResult.ACCEPTED;
        }

        @Override
        public void run() {
            while (true) {
                PendingMessage pending;
                synchronized (this) {
                    if (discarded) {
                        drainScheduled = false;
                        return;
                    }
                    pending = queue.pollFirst();
                    if (pending == null) {
                        drainScheduled = false;
                        return;
                    }
                }

                try {
                    if (!session.isOpen()) {
                        failClosedSession();
                        return;
                    }
                    outboundSession.sendMessage(pending.message);
                } catch (IOException | RuntimeException exception) {
                    // The transport can close after isOpen() and before the
                    // actual write. Treat that ordinary disconnect race like
                    // the pre-write closed-session path instead of reporting
                    // it as an outbound failure with a misleading stack trace.
                    if (!session.isOpen()) {
                        failClosedSession();
                    } else {
                        failSend(exception);
                    }
                    return;
                }

                synchronized (this) {
                    if (!discarded) {
                        pendingMessages--;
                        pendingBytes -= pending.bytes;
                    }
                }
            }
        }

        private void failClosedSession() {
            discard();
            mailboxes.remove(session.getId(), this);
        }

        private void failSend(Exception exception) {
            discard();
            mailboxes.remove(session.getId(), this);
            close(session, CloseStatus.SESSION_NOT_RELIABLE, "outbound send failed", exception);
        }

        private synchronized void discard() {
            discardLocked();
        }

        private void discardLocked() {
            discarded = true;
            queue.clear();
            pendingMessages = 0;
            pendingBytes = 0L;
        }
    }

    private static void close(WebSocketSession session,
                              CloseStatus status,
                              String reason,
                              Exception cause) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException closeException) {
            if (cause == null) {
                log.warn("{} for session {}, and closing the session failed",
                        reason, session.getId(), closeException);
            } else {
                log.warn("{} for session {}, and closing the session failed",
                        reason, session.getId(), closeException);
                log.debug("Original outbound failure", cause);
            }
            return;
        }

        if (cause == null) {
            log.warn("{}; closed session {}", reason, session.getId());
        } else {
            // Overload and send failures can occur in bursts. Keep the WARN
            // line bounded and move stack traces to DEBUG to avoid log-I/O
            // amplification while the node is already under pressure.
            log.warn("{}; closed session {}", reason, session.getId());
            log.debug("Outbound failure for session " + session.getId(), cause);
        }
    }

    /** One immutable frame and its precomputed UTF-8 size, reusable for fan-out. */
    static final class PreparedMessage {
        private final TextMessage message;
        private final int bytes;

        private PreparedMessage(TextMessage message, int bytes) {
            this.message = message;
            this.bytes = bytes;
        }

        static PreparedMessage from(TextMessage message) {
            return new PreparedMessage(message,
                    message.getPayload().getBytes(StandardCharsets.UTF_8).length);
        }
    }

    private static final class PendingMessage {
        private final TextMessage message;
        private final int bytes;

        private PendingMessage(TextMessage message, int bytes) {
            this.message = message;
            this.bytes = bytes;
        }
    }
}
