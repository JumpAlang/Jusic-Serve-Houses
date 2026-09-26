package com.scoder.jusic.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Legacy cleanup deletes room-wide Redis state and is therefore unsafe when
 * another node may be serving the same room. It remains opt-in only for an
 * explicit one-node maintenance operation.
 */
@Component
@ConfigurationProperties(prefix = "jusic.lifecycle")
@Data
public class LifecycleProperties {

    private boolean cleanupLegacyStateOnStartup = false;
    private boolean cleanupLegacyStateOnShutdown = false;
}
