package com.scoder.jusic.interceptor;

import com.scoder.jusic.configuration.HouseContainer;
import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.security.AbuseAction;
import com.scoder.jusic.security.AbuseProtectionService;
import com.scoder.jusic.security.ClientIpResolver;
import com.scoder.jusic.security.RateLimitDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JusicWebSocketHandshakeInterceptorTest {

    private static final String IP = "192.0.2.10";
    private static final String CLIENT_ID =
            "0123456789abcdef0123456789abcdef";

    @Mock
    private HouseContainer houseContainer;
    @Mock
    private AbuseProtectionService abuseProtectionService;
    @Mock
    private ClientIpResolver clientIpResolver;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private WebSocketHandler webSocketHandler;

    @InjectMocks
    private JusicWebSocketHandshakeInterceptor interceptor;

    @Test
    void defaultRoomKeepsLegacyHandshakeAttributesAndUsesGlobalIpQuota() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE, CLIENT_ID);
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        Map<String, Object> attributes = new HashMap<>();
        when(clientIpResolver.resolve(request)).thenReturn(IP);
        when(abuseProtectionService.check(AbuseAction.HANDSHAKE, null, IP, null))
                .thenReturn(RateLimitDecision.allowed());
        when(abuseProtectionService.check(
                AbuseAction.HANDSHAKE, null, null, JusicProperties.HOUSE_DEFAULT_ID))
                .thenReturn(RateLimitDecision.allowed());

        assertTrue(interceptor.beforeHandshake(
                request, response, webSocketHandler, attributes));

        assertEquals(IP, attributes.get("remoteAddress"));
        assertEquals(JusicProperties.HOUSE_DEFAULT_ID, attributes.get("houseId"));
        assertEquals(CLIENT_ID, attributes.get(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE));
        verify(houseContainer, never()).get(org.mockito.ArgumentMatchers.any());
        verify(abuseProtectionService).check(
                AbuseAction.HANDSHAKE, null, null, JusicProperties.HOUSE_DEFAULT_ID);
    }

    @Test
    void unknownPrivateRoomFailsClosed() throws Exception {
        MockHttpServletRequest servletRequest = request("missing", null, null);
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        when(clientIpResolver.resolve(request)).thenReturn(IP);
        when(abuseProtectionService.check(AbuseAction.HANDSHAKE, null, IP, null))
                .thenReturn(RateLimitDecision.allowed());

        assertFalse(interceptor.beforeHandshake(
                request, response, webSocketHandler, new HashMap<>()));
    }

    @Test
    void passwordRoomChecksRateLimitBeforeComparingPassword() throws Exception {
        MockHttpServletRequest servletRequest = request("private", "secret", "reconnect");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        House house = new House();
        house.setNeedPwd(true);
        house.setPassword("secret");
        when(clientIpResolver.resolve(request)).thenReturn(IP);
        when(abuseProtectionService.check(AbuseAction.HANDSHAKE, null, IP, null))
                .thenReturn(RateLimitDecision.allowed());
        when(abuseProtectionService.check(AbuseAction.HANDSHAKE, null, null, "private"))
                .thenReturn(RateLimitDecision.allowed());
        when(abuseProtectionService.check(AbuseAction.ROOM_ENTER, null, IP, "private"))
                .thenReturn(RateLimitDecision.allowed());
        when(houseContainer.get("private")).thenReturn(house);
        Map<String, Object> attributes = new HashMap<>();

        assertTrue(interceptor.beforeHandshake(
                request, response, webSocketHandler, attributes));

        assertEquals("private", attributes.get("houseId"));
        assertEquals("reconnect", attributes.get("connectType"));
        verify(abuseProtectionService).check(AbuseAction.ROOM_ENTER, null, IP, "private");
    }

    @Test
    void nullStoredPasswordDoesNotThrowOrAllowPasswordRoom() throws Exception {
        MockHttpServletRequest servletRequest = request("private", null, null);
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        House house = new House();
        house.setNeedPwd(true);
        house.setPassword(null);
        when(clientIpResolver.resolve(request)).thenReturn(IP);
        when(abuseProtectionService.check(AbuseAction.HANDSHAKE, null, IP, null))
                .thenReturn(RateLimitDecision.allowed());
        when(abuseProtectionService.check(AbuseAction.HANDSHAKE, null, null, "private"))
                .thenReturn(RateLimitDecision.allowed());
        when(abuseProtectionService.check(AbuseAction.ROOM_ENTER, null, IP, "private"))
                .thenReturn(RateLimitDecision.allowed());
        when(houseContainer.get("private")).thenReturn(house);

        assertFalse(interceptor.beforeHandshake(
                request, response, webSocketHandler, new HashMap<>()));
    }

    @Test
    void handshakeDenialDoesNotReadRoomState() throws Exception {
        MockHttpServletRequest servletRequest = request("private", "guess", null);
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        when(clientIpResolver.resolve(request)).thenReturn(IP);
        when(abuseProtectionService.check(AbuseAction.HANDSHAKE, null, IP, null))
                .thenReturn(RateLimitDecision.denied(1_000L));

        assertFalse(interceptor.beforeHandshake(
                request, response, webSocketHandler, new HashMap<>()));

        verify(houseContainer, never()).get(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void knownPublicRoomConsumesRoomQuotaWithoutChargingIpTwice() throws Exception {
        MockHttpServletRequest servletRequest = request("public", null, null);
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        House house = new House();
        house.setNeedPwd(false);
        when(clientIpResolver.resolve(request)).thenReturn(IP);
        when(abuseProtectionService.check(AbuseAction.HANDSHAKE, null, IP, null))
                .thenReturn(RateLimitDecision.allowed());
        when(abuseProtectionService.check(AbuseAction.HANDSHAKE, null, null, "public"))
                .thenReturn(RateLimitDecision.denied(1_000L));
        when(houseContainer.get("public")).thenReturn(house);

        assertFalse(interceptor.beforeHandshake(
                request, response, webSocketHandler, new HashMap<>()));

        verify(abuseProtectionService).check(
                AbuseAction.HANDSHAKE, null, null, "public");
        verify(abuseProtectionService, never()).check(
                AbuseAction.ROOM_ENTER, null, IP, "public");
    }

    private MockHttpServletRequest request(String houseId, String password, String connectType) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("houseId", houseId);
        if (password != null) {
            request.setParameter("housePwd", password);
        }
        if (connectType != null) {
            request.setParameter("connectType", connectType);
        }
        return request;
    }
}
