package com.scoder.jusic.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated bounded pool for socket writes. In particular, this pool must not
 * use CallerRunsPolicy because a rejected write must never block a controller,
 * scheduler, or broadcast caller.
 */
@Configuration
public class OutboundMailboxConfiguration {

    @Bean(name = "webSocketOutboundExecutor")
    public ThreadPoolTaskExecutor webSocketOutboundExecutor(OutboundMailboxProperties properties) {
        int maximum = Math.max(1, properties.getMaxPoolSize());
        int core = Math.max(1, Math.min(properties.getCorePoolSize(), maximum));

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(maximum);
        executor.setQueueCapacity(Math.max(1, properties.getExecutorQueueCapacity()));
        executor.setKeepAliveSeconds(Math.max(1, properties.getKeepAliveSeconds()));
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("jusic-ws-outbound-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
