package com.scoder.jusic.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
* Bounded provider pools keep slow music APIs from consuming every WebSocket
 * inbound worker. Pick, search, playback and maintenance are isolated so one
 * provider workload cannot starve the others.
 */
@Component
@ConfigurationProperties(prefix = "jusic.provider")
@Data
public class ProviderIsolationProperties {

    private Pool pick = new Pool(4, 16, 32, 8_000L);
    private Pool search = new Pool(8, 24, 64, 10_000L);
    private Pool playback = new Pool(2, 8, 16, 12_000L);
    private Pool background = new Pool(2, 8, 16, 12_000L);
    private Http http = new Http();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pool {
        private int coreSize;
        private int maxSize;
        private int queueCapacity;
        private long timeoutMs;
    }

    /**
     * Hard transport bounds. The workload timeout above is the end-to-end
     * caller budget; these values make sure a cancelled task cannot leave an
     * HTTP client thread blocked forever.
     */
    @Data
    public static class Http {
        private int connectTimeoutMs = 3_000;
        private int readTimeoutMs = 5_000;
        private int maxConnections = 64;
        private int maxConnectionsPerRoute = 16;
    }
}
