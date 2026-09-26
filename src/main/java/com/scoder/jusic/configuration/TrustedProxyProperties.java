package com.scoder.jusic.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Forwarded client IP header configuration. The legacy class name and proxy
 * list are retained for configuration compatibility; the list is not consulted.
 */
@Component
@ConfigurationProperties(prefix = "jusic.network")
@Data
public class TrustedProxyProperties {

    private boolean useForwardHeaders = true;
    @Deprecated
    private List<String> trustedProxies = Arrays.asList(
            "127.0.0.1",
            "0:0:0:0:0:0:0:1",
            "::1"
    );
}
