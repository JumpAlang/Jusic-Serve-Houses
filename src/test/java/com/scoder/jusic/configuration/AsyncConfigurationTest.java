package com.scoder.jusic.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConfigurationTest {

    @Test
    void safeDefaultsAreBoundedAndRejectInsteadOfRunningOnTheCaller() {
        AsyncTaskProperties properties = new AsyncTaskProperties();
        ThreadPoolTaskExecutor executor =
                new AsyncConfiguration(properties).applicationTaskExecutor();
        assertEquals(8, executor.getCorePoolSize());
        assertEquals(32, executor.getMaxPoolSize());

        executor.initialize();
        try {
            assertEquals(512, executor.getThreadPoolExecutor()
                    .getQueue().remainingCapacity());
            assertTrue(executor.getThreadPoolExecutor().getRejectedExecutionHandler()
                    instanceof ThreadPoolExecutor.AbortPolicy);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void invalidPoolRelationshipsFailAtConfigurationTime() {
        AsyncTaskProperties properties = new AsyncTaskProperties();
        properties.setCorePoolSize(33);
        properties.setMaxPoolSize(32);

        assertThrows(IllegalArgumentException.class,
                () -> new AsyncConfiguration(properties).applicationTaskExecutor());
    }

    @Test
    void nonPositiveResourceLimitsFailAtConfigurationTime() {
        AsyncTaskProperties properties = new AsyncTaskProperties();
        properties.setQueueCapacity(0);

        assertThrows(IllegalArgumentException.class,
                () -> new AsyncConfiguration(properties).applicationTaskExecutor());
    }
}
