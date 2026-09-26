package com.scoder.jusic.configuration;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JusicDisposableTest {

    @Test
    void normalShutdownNeverDeletesSharedRoomState() throws Exception {
        HouseContainer houseContainer = mock(HouseContainer.class);
        LifecycleProperties properties = new LifecycleProperties();

        new JusicDisposable(houseContainer, properties).destroy();

        verify(houseContainer, never()).destroy();
    }

    @Test
    void legacyCleanupRequiresExplicitMaintenanceOptIn() throws Exception {
        HouseContainer houseContainer = mock(HouseContainer.class);
        LifecycleProperties properties = new LifecycleProperties();
        properties.setCleanupLegacyStateOnShutdown(true);

        new JusicDisposable(houseContainer, properties).destroy();

        verify(houseContainer).destroy();
    }
}
