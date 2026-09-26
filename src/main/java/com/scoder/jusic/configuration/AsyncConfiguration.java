package com.scoder.jusic.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Slf4j
public class AsyncConfiguration implements AsyncConfigurer {

    private final AsyncTaskProperties properties;

    public AsyncConfiguration(AsyncTaskProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "applicationTaskExecutor")
    public ThreadPoolTaskExecutor applicationTaskExecutor() {
        validate(properties);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setThreadNamePrefix("jusic-async-");
        // Never run slow lifecycle/provider work on a WebSocket I/O thread.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("async task failed, method: {}, message: {}", method.getName(), ex.getMessage(), ex);
    }

    private void validate(AsyncTaskProperties value) {
        if (value.getCorePoolSize() <= 0
                || value.getMaxPoolSize() <= 0
                || value.getQueueCapacity() <= 0
                || value.getKeepAliveSeconds() <= 0
                || value.getAwaitTerminationSeconds() <= 0) {
            throw new IllegalArgumentException("jusic.async executor settings must be positive");
        }
        if (value.getCorePoolSize() > value.getMaxPoolSize()) {
            throw new IllegalArgumentException(
                    "jusic.async.core-pool-size must not exceed max-pool-size");
        }
    }
}
