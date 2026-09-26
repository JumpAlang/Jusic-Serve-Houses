package com.scoder.jusic.configuration;

import com.scoder.jusic.model.House;
import com.scoder.jusic.security.ProviderCallException;
import com.scoder.jusic.service.ConfigService;
import com.scoder.jusic.service.MusicService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JusicInitializingTest {

    @Test
    void startupLoadsLocalSnapshotAndClosesItsStream() throws Exception {
        JusicProperties jusicProperties = mock(JusicProperties.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource resource = mock(Resource.class);
        HouseContainer houseContainer = mock(HouseContainer.class);
        LifecycleProperties lifecycleProperties = new LifecycleProperties();
        MusicService musicService = mock(MusicService.class);
        AtomicBoolean closed = new AtomicBoolean();
        ByteArrayInputStream stream = new ByteArrayInputStream(
                "local-a\n\n local-b \n".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };

        when(houseContainer.loadSurvive()).thenReturn(new CopyOnWriteArrayList<>());
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(stream);

        JusicInitializing initializing = initializing(jusicProperties, resourceLoader,
                houseContainer, lifecycleProperties, musicService);

        initializing.afterPropertiesSet();

        assertEquals(Arrays.asList("local-a", "local-b"),
                JusicProperties.getDefaultListForRepository());
        assertTrue(closed.get());
    }

    @Test
    void providerRefreshFailureDoesNotPreventNodeStartup() throws Exception {
        JusicProperties jusicProperties = mock(JusicProperties.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource resource = mock(Resource.class);
        HouseContainer houseContainer = mock(HouseContainer.class);
        LifecycleProperties lifecycleProperties = new LifecycleProperties();
        MusicService musicService = mock(MusicService.class);
        CopyOnWriteArrayList<House> houses = new CopyOnWriteArrayList<>();

        when(houseContainer.loadSurvive()).thenReturn(houses);
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(
                new ByteArrayInputStream("music-id\n".getBytes(StandardCharsets.UTF_8)));
        doThrow(new ProviderCallException("provider timed out"))
                .when(musicService).neteaseRefreshCookie();

        JusicInitializing initializing = initializing(jusicProperties, resourceLoader,
                houseContainer, lifecycleProperties, musicService);

        assertDoesNotThrow(initializing::afterPropertiesSet);
        verify(houseContainer).initialize(houses);
        verify(musicService).neteaseRefreshCookie();
    }

    private JusicInitializing initializing(JusicProperties jusicProperties,
                                            ResourceLoader resourceLoader,
                                            HouseContainer houseContainer,
                                            LifecycleProperties lifecycleProperties,
                                            MusicService musicService) {
        JusicInitializing initializing = new JusicInitializing(
                jusicProperties, resourceLoader, houseContainer, lifecycleProperties);
        ReflectionTestUtils.setField(initializing, "musicService", musicService);
        ReflectionTestUtils.setField(initializing, "configService", mock(ConfigService.class));
        return initializing;
    }
}
