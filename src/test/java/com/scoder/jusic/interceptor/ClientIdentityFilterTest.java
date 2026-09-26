package com.scoder.jusic.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.Cookie;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientIdentityFilterTest {

    private static final String EXISTING_CLIENT_ID =
            "0123456789abcdef0123456789abcdef";

    private final ClientIdentityFilter filter = new ClientIdentityFilter();

    @Test
    void existingValidCookieIsReusedWithoutCreatingSessionOrAnotherCookie()
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(
                ClientIdentityFilter.COOKIE_NAME, EXISTING_CLIENT_ID));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Object> identitySeenByChain = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                identitySeenByChain.set(servletRequest.getAttribute(
                        ClientIdentityFilter.CLIENT_ID_ATTRIBUTE)));

        assertEquals(EXISTING_CLIENT_ID, identitySeenByChain.get());
        assertEquals(EXISTING_CLIENT_ID, request.getAttribute(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE));
        assertNull(response.getHeader("Set-Cookie"));
        assertNull(request.getSession(false));
    }

    @Test
    void missingCookieCreatesOpaquePersistentCookieAndPublishesSameRequestAttribute()
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        String clientId = (String) request.getAttribute(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE);
        String cookie = response.getHeader("Set-Cookie");
        assertNotNull(clientId);
        assertTrue(clientId.matches("[a-f0-9]{32}"));
        assertNotNull(cookie);
        assertTrue(cookie.startsWith(
                ClientIdentityFilter.COOKIE_NAME + "=" + clientId + ";"));
        assertTrue(cookie.contains("Path=/"));
        assertTrue(cookie.contains("Max-Age=31536000"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertFalse(cookie.contains("; Secure"));
        assertNull(request.getSession(false));
    }

    @Test
    void invalidClientIdCookieIsRejectedAndRotated()
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(
                ClientIdentityFilter.COOKIE_NAME, "attacker-controlled-value"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        String replacement = (String) request.getAttribute(
                ClientIdentityFilter.CLIENT_ID_ATTRIBUTE);
        assertNotNull(replacement);
        assertNotEquals("attacker-controlled-value", replacement);
        assertTrue(replacement.matches("[a-f0-9]{32}"));
        assertTrue(response.getHeader("Set-Cookie").startsWith(
                ClientIdentityFilter.COOKIE_NAME + "=" + replacement + ";"));
    }

    @Test
    void secureRequestMarksNewCookieSecure()
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertTrue(response.getHeader("Set-Cookie").contains("; Secure"));
    }
}
