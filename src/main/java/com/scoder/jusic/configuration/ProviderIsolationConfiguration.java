package com.scoder.jusic.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ProviderIsolationConfiguration {

    @Bean(name = "pickProviderExecutor")
    public ThreadPoolTaskExecutor pickProviderExecutor(ProviderIsolationProperties properties) {
        return executor("jusic-provider-pick-", properties.getPick());
    }

    @Bean(name = "searchProviderExecutor")
    public ThreadPoolTaskExecutor searchProviderExecutor(ProviderIsolationProperties properties) {
        return executor("jusic-provider-search-", properties.getSearch());
    }

    @Bean(name = "backgroundProviderExecutor")
    public ThreadPoolTaskExecutor backgroundProviderExecutor(ProviderIsolationProperties properties) {
        return executor("jusic-provider-background-", properties.getBackground());
    }

    @Bean(name = "playbackProviderExecutor")
    public ThreadPoolTaskExecutor playbackProviderExecutor(ProviderIsolationProperties properties) {
        return executor("jusic-provider-playback-", properties.getPlayback());
    }

    private ThreadPoolTaskExecutor executor(String prefix,
                                            ProviderIsolationProperties.Pool pool) {
        int maximum = Math.max(1, pool.getMaxSize());
        int core = Math.max(1, Math.min(pool.getCoreSize(), maximum));
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(maximum);
        executor.setQueueCapacity(Math.max(0, pool.getQueueCapacity()));
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix(prefix);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
