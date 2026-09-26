package com.scoder.jusic.job;

import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MusicJobTemporaryHouseCleanupTest {

    @Test
    void oldLocallyEmptyRoomUsesConditionalLifecycleCleanup() {
        String houseId = "temporary-room";
        JusicProperties properties = new JusicProperties();
        properties.getSessions(houseId);
        ExecutorService executorService = mock(ExecutorService.class);
        MusicJob musicJob = new MusicJob(properties, executorService);
        HouseContainer houseContainer = mock(HouseContainer.class);
        House house = new House();
        house.setId(houseId);
        house.setEnableStatus(false);
        house.setCreateTime(System.currentTimeMillis() - 21_000L);
        CopyOnWriteArrayList<House> houses = new CopyOnWriteArrayList<>();
        houses.add(house);
        when(houseContainer.getHouses()).thenReturn(houses);
        ReflectionTestUtils.setField(musicJob, "houseContainer", houseContainer);

        ReflectionTestUtils.invokeMethod(musicJob, "sendIfSufficient");

        verify(houseContainer).destroyIfTemporaryAndEmpty(houseId);
        verify(houseContainer, never()).destroy(houseId);
    }
}
