package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.model.House;
import com.scoder.jusic.security.ProviderCallException;
import com.scoder.jusic.security.ProviderCallExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class HomeControllerProviderFailureTest {

    @Test
    void proxyTimeoutsKeepTheLegacyResponseEnvelope() throws Exception {
        ProviderCallExecutor providerCallExecutor = mock(ProviderCallExecutor.class);
        when(providerCallExecutor.search(any(Callable.class)))
                .thenThrow(new ProviderCallException("provider timed out"));
        HouseContainer houseContainer = mock(HouseContainer.class);
        House house = new House();
        house.setId("house-a");
        when(houseContainer.get("house-a")).thenReturn(house);
        HomeController controller = new HomeController();
        ReflectionTestUtils.setField(controller, "providerCallExecutor", providerCallExecutor);
        ReflectionTestUtils.setField(controller, "houseContainer", houseContainer);

        assertFailure(controller.getMiniCode(house, null));
        assertFailure(controller.room_init("1", null));
        assertFailure(controller.getConf("1", null));
        assertFailure(controller.getDanmuInfo("1", null));
    }

    private void assertFailure(Response response) {
        assertEquals("40000", response.getCode());
        assertEquals("第三方服务暂不可用，请稍后重试", response.getMessage());
        assertNull(response.getData());
    }
}
