package com.scoder.jusic.configuration;

import kong.unirest.Unirest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.time.Duration;

/**
 * Shared hard transport limits for every legacy Unirest call and for the
 * NetEase proxy RestTemplate. End-to-end budgets remain in
 * {@link com.scoder.jusic.security.ProviderCallExecutor}.
 */
@Configuration
public class ProviderHttpClientConfiguration {

    @PreDestroy
    public void shutdownUnirest() {
        Unirest.shutDown();
    }

    @Bean
    @Qualifier("providerRestTemplate")
    public RestTemplate providerRestTemplate(RestTemplateBuilder builder,
                                             ProviderIsolationProperties properties) {
        ProviderIsolationProperties.Http http = properties.getHttp();
        int connectTimeout = positive(http.getConnectTimeoutMs());
        int readTimeout = positive(http.getReadTimeoutMs());
        int maxConnections = positive(http.getMaxConnections());
        int maxPerRoute = Math.min(maxConnections,
                positive(http.getMaxConnectionsPerRoute()));

        // Disable the client's hidden retries. Legacy provider adapters already
        // have explicit retry loops and those loops must remain inside the one
        // workload-level deadline.
        Unirest.config()
                .connectTimeout(connectTimeout)
                .socketTimeout(readTimeout)
                .concurrency(maxConnections, maxPerRoute)
                .automaticRetries(false);

        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

    private int positive(int value) {
        return Math.max(1, value);
    }
}
