package com.scoder.jusic.configuration;

import com.scoder.jusic.interceptor.ClientIdentityFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Node-local index that makes a browser connection single-flight.
 *
 * <p>The first active socket owns the stable browser id. A later socket must
 * be rejected by the lifecycle handler instead of replacing this binding.
 * Different browsers/devices still have different ids and remain independent
 * users.</p>
 */
@Component
public class ClientConnectionRegistry {

    private static final int LOCK_STRIPES = 256;

    private final ConcurrentHashMap<String, WebSocketSession> connections =
            new ConcurrentHashMap<>();
    private final ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];

    public ClientConnectionRegistry() {
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new ReentrantLock();
        }
    }

    public ReentrantLock lockFor(String clientId) {
        String checked = requireClientId(clientId);
        int index = (checked.hashCode() & Integer.MAX_VALUE) % locks.length;
        return locks[index];
    }

    public WebSocketSession current(String clientId) {
        return connections.get(requireClientId(clientId));
    }

    /**
     * Atomically reserve a stable browser id for the first connection.
     * Repeating the call for the same session is idempotent; a different
     * session can never replace the current owner.
     */
    public boolean tryReserve(String clientId, WebSocketSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        WebSocketSession existing = connections.putIfAbsent(
                requireClientId(clientId), session);
        return existing == null || existing == session;
    }

    public void bind(String clientId, WebSocketSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        connections.put(requireClientId(clientId), session);
    }

    public boolean unbind(WebSocketSession session) {
        if (session == null) {
            return false;
        }
        Object clientId = session.getAttributes().get(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE);
        return clientId != null
                && connections.remove(clientId.toString(), session);
    }

    public int size() {
        return connections.size();
    }

    private String requireClientId(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        return clientId;
    }
}
