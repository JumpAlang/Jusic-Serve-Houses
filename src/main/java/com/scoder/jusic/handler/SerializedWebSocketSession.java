package com.scoder.jusic.handler;

import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Shared final write gate for both Spring STOMP output and legacy direct
 * application output.
 *
 * <p>Spring serializes messages sent through its own outbound channel, but
 * this application also writes custom frames directly to the underlying
 * session. Both paths must pass through the same decorator or Tomcat can see
 * concurrent partial writes and close the transport.</p>
 */
public final class SerializedWebSocketSession extends WebSocketSessionDecorator {

    private static final String ATTRIBUTE =
            SerializedWebSocketSession.class.getName() + ".instance";

    private final ReentrantLock writeLock = new ReentrantLock();

    private SerializedWebSocketSession(WebSocketSession delegate) {
        super(delegate);
    }

    public static WebSocketSession wrap(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        synchronized (attributes) {
            Object current = attributes.get(ATTRIBUTE);
            if (current instanceof SerializedWebSocketSession) {
                return (SerializedWebSocketSession) current;
            }
            WebSocketSession serialized = new SerializedWebSocketSession(session);
            attributes.put(ATTRIBUTE, serialized);
            return serialized;
        }
    }

    public static WebSocketSession outboundSession(WebSocketSession session) {
        Object serialized = session.getAttributes().get(ATTRIBUTE);
        return serialized instanceof SerializedWebSocketSession
                ? (WebSocketSession) serialized
                : session;
    }

    @Override
    public void sendMessage(WebSocketMessage<?> message) throws IOException {
        writeLock.lock();
        try {
            super.sendMessage(message);
        } finally {
            writeLock.unlock();
        }
    }
}
