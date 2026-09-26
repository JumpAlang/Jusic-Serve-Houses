package com.scoder.jusic.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class WebSocketInboundExecutorConfiguration {

    @Bean(name = "jusicClientInboundExecutor")
    public ThreadPoolTaskExecutor clientInboundExecutor(WebSocketInboundProperties properties) {
        validate(properties);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("jusic-ws-inbound-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    private void validate(WebSocketInboundProperties properties) {
        if (properties.getCorePoolSize() <= 0
                || properties.getMaxPoolSize() <= 0
                || properties.getQueueCapacity() <= 0
                || properties.getKeepAliveSeconds() <= 0) {
            throw new IllegalArgumentException(
                    "jusic.websocket.inbound executor settings must be positive");
        }
        if (properties.getCorePoolSize() > properties.getMaxPoolSize()) {
            throw new IllegalArgumentException(
                    "jusic.websocket.inbound.core-pool-size must not exceed max-pool-size");
        }
    }
}
