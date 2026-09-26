package com.scoder.jusic.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Bounded executor settings for non-critical application lifecycle work. */
@Component
@ConfigurationProperties(prefix = "jusic.async")
@Data
public class AsyncTaskProperties {

    private int corePoolSize = 8;
    private int maxPoolSize = 32;
    private int queueCapacity = 512;
    private int keepAliveSeconds = 60;
    private int awaitTerminationSeconds = 5;
}
