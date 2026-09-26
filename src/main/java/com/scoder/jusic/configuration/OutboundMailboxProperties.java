package com.scoder.jusic.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bounds for asynchronous WebSocket delivery.
 *
 * <p>The per-session limits include the message currently being written. This
 * prevents one slow connection from retaining an unbounded number of frames.
 * The executor queue is also bounded so slow sockets cannot consume all server
 * memory.</p>
 */
@Component
@ConfigurationProperties(prefix = "jusic.websocket.outbound")
@Data
public class OutboundMailboxProperties {

    private int maxPendingMessages = 64;
    private long maxPendingBytes = 256L * 1024L;

    private int corePoolSize = 4;
    private int maxPoolSize = 16;
    /**
     * Node-level hard bound for mailboxes waiting to drain. A room broadcast
     * can submit one first drain task per connected session, so this value must
     * be sized at or above the node's target concurrent connection count.
     */
    private int executorQueueCapacity = 32_768;
    private int keepAliveSeconds = 60;
}
