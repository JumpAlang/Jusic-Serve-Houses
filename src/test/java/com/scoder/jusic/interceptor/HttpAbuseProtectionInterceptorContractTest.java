package com.scoder.jusic.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scoder.jusic.security.AbuseAction;
import com.scoder.jusic.security.AbuseProtectionService;
import com.scoder.jusic.security.ClientIpResolver;
import com.scoder.jusic.security.RateLimitDecision;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpAbuseProtectionInterceptorContractTest {

    private final AbuseProtectionService abuseProtectionService = mock(AbuseProtectionService.class);
    private final ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpAbuseProtectionInterceptor interceptor =
            new HttpAbuseProtectionInterceptor(abuseProtectionService, clientIpResolver, objectMapper);

    @Test
    void denialKeepsHttp200AndLegacyJsonEnvelopeIncludingNullData() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/house/add");
        request.setRemoteAddr("192.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(clientIpResolver.resolve(request)).thenReturn("192.0.2.10");
        when(abuseProtectionService.check(
                AbuseAction.ROOM_CREATE, null, "192.0.2.10", null))
                .thenReturn(RateLimitDecision.denied(1_500L));

        assertFalse(interceptor.preHandle(request, response, new Object()));

        assertEquals(200, response.getStatus());
        assertEquals("2", response.getHeader("Retry-After"));
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals("40000", body.get("code").asText());
        assertEquals("请求过于频繁，请在2秒后重试", body.get("message").asText());
        assertTrue(body.has("data"));
        assertTrue(body.get("data").isNull());
    }

    @Test
    void contextPathIsRemovedBeforeActionResolution() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jusic/house/enter");
        request.setContextPath("/jusic");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(clientIpResolver.resolve(request)).thenReturn("192.0.2.20");
        when(abuseProtectionService.check(
                AbuseAction.ROOM_ENTER, null, "192.0.2.20", null))
                .thenReturn(RateLimitDecision.allowed());

        assertTrue(interceptor.preHandle(request, response, new Object()));

        verify(abuseProtectionService).check(
                AbuseAction.ROOM_ENTER, null, "192.0.2.20", null);
    }

    @Test
    void routedTrailingSlashAndMatrixParametersCannotBypassActionResolution() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/house/add;source=attacker/");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/house/add/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(clientIpResolver.resolve(request)).thenReturn("192.0.2.30");
        when(abuseProtectionService.check(
                AbuseAction.ROOM_CREATE, null, "192.0.2.30", null))
                .thenReturn(RateLimitDecision.allowed());

        assertTrue(interceptor.preHandle(request, response, new Object()));

        verify(abuseProtectionService).check(
                AbuseAction.ROOM_CREATE, null, "192.0.2.30", null);
    }
}
