package com.scoder.jusic.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for removing locally retained WebSocket sessions that the
 * transport has already marked as closed.
 *
 * <p>This reaper intentionally has no inactivity threshold. A connected user
 * who only listens and sends no inbound messages is still a valid session.</p>
 */
@Component
@ConfigurationProperties(prefix = "jusic.websocket.reaper")
@Data
public class SessionReaperProperties {

    private boolean enabled = true;

    /**
     * Delay, in milliseconds, between completed scans.
     */
    private long fixedDelay = 15_000L;
}
