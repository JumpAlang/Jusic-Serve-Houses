package com.scoder.jusic.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketInboundExecutorConfigurationTest {

    @Test
    void defaultsCreateABoundedAbortPolicyExecutor() {
        WebSocketInboundProperties properties = new WebSocketInboundProperties();
        ThreadPoolTaskExecutor executor = new WebSocketInboundExecutorConfiguration()
                .clientInboundExecutor(properties);
        executor.initialize();
        try {
            assertEquals(16, executor.getCorePoolSize());
            assertEquals(64, executor.getMaxPoolSize());
            assertEquals(512, executor.getThreadPoolExecutor().getQueue().remainingCapacity());
            assertTrue(executor.getThreadPoolExecutor().getRejectedExecutionHandler()
                    instanceof ThreadPoolExecutor.AbortPolicy);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void invalidPoolShapeFailsFast() {
        WebSocketInboundProperties properties = new WebSocketInboundProperties();
        properties.setCorePoolSize(5);
        properties.setMaxPoolSize(4);

        assertThrows(IllegalArgumentException.class,
                () -> new WebSocketInboundExecutorConfiguration()
                        .clientInboundExecutor(properties));
    }
}
