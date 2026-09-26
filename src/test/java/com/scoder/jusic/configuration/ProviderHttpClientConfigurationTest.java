package com.scoder.jusic.configuration;

import kong.unirest.Unirest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderHttpClientConfigurationTest {

    @AfterEach
    void resetGlobalUnirestConfiguration() {
        Unirest.config().reset();
    }

    @Test
    void configuresHardUnirestAndRestTemplateTimeouts() {
        ProviderIsolationProperties properties = new ProviderIsolationProperties();
        properties.getHttp().setConnectTimeoutMs(1234);
        properties.getHttp().setReadTimeoutMs(4321);
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(builder.setConnectTimeout(Duration.ofMillis(1234)))
                .thenReturn(builder);
        when(builder.setReadTimeout(Duration.ofMillis(4321)))
                .thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);

        RestTemplate result = new ProviderHttpClientConfiguration()
                .providerRestTemplate(builder, properties);

        assertSame(restTemplate, result);
        assertEquals(1234, Unirest.config().getConnectionTimeout());
        assertEquals(4321, Unirest.config().getSocketTimeout());
        assertFalse(Unirest.config().isAutomaticRetries());
        verify(builder).setConnectTimeout(Duration.ofMillis(1234));
        verify(builder).setReadTimeout(Duration.ofMillis(4321));
    }
}
