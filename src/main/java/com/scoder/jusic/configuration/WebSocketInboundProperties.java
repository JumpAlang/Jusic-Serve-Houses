package com.scoder.jusic.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bounds work accepted from the WebSocket client-inbound channel. Keeping the
 * queue finite prevents a chat/search flood from turning into unbounded heap
 * growth while still allowing short bursts to drain.
 */
@Component
@ConfigurationProperties(prefix = "jusic.websocket.inbound")
@Data
public class WebSocketInboundProperties {

    private int corePoolSize = 16;
    private int maxPoolSize = 64;
    private int queueCapacity = 512;
    private int keepAliveSeconds = 60;
}
