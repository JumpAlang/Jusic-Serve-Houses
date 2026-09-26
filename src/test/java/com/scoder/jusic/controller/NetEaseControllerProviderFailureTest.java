package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.security.ProviderCallException;
import com.scoder.jusic.service.MusicService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NetEaseControllerProviderFailureTest {

    @Test
    void providerFailuresKeepTheLegacyResponseEnvelope() {
        MusicService musicService = mock(MusicService.class);
        ProviderCallException timeout = new ProviderCallException("provider timed out");
        when(musicService.netEaseLoginByPhone(any(), any(), any(), any()))
                .thenThrow(timeout);
        when(musicService.netEaseLoginByEmail(any(), any(), any()))
                .thenThrow(timeout);
        doThrow(timeout).when(musicService).netEaseLoginRefresh();

        NetEaseController controller = new NetEaseController();
        ReflectionTestUtils.setField(controller, "musicService", musicService);

        assertFailureEnvelope(controller.loginByPhone("phone", null, null, null));
        assertFailureEnvelope(controller.loginByEmail("mail", null, null));
        assertFailureEnvelope(controller.loginRefresh());
    }

    private void assertFailureEnvelope(Response response) {
        assertEquals("40000", response.getCode());
        assertEquals("音乐服务暂不可用，请稍后重试", response.getMessage());
        assertNull(response.getData());
    }
}

