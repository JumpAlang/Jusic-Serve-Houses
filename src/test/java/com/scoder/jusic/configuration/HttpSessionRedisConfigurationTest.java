package com.scoder.jusic.configuration;

import com.scoder.jusic.util.UUIDUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.session.DefaultCookieSerializerCustomizer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.MapSession;
import org.springframework.session.web.http.CookieSerializer.CookieValue;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.boot.env.YamlPropertySourceLoader;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpSessionRedisConfigurationTest {

    @Test
    void applicationConfigurationUsesAnIsolatedThirtyMinuteRedisSessionStore()
            throws IOException {
        PropertySourcesPropertyResolver properties = applicationProperties();

        assertEquals("redis", properties.getProperty("spring.session.store-type"));
        assertEquals("30m", properties.getProperty("spring.session.timeout"));
        assertEquals("30m", properties.getProperty("server.servlet.session.timeout"));
        assertEquals("jusic:http-session:v1",
                properties.getProperty("spring.session.redis.namespace"));
        assertFalse(properties.getProperty("spring.session.redis.namespace")
                .startsWith("jusic_session_"));
        assertEquals("notify-keyspace-events",
                properties.getProperty("spring.session.redis.configure-action"));
        assertEquals("JSESSIONID",
                properties.getProperty("server.servlet.session.cookie.name"));
        assertEquals("true",
                properties.getProperty("server.servlet.session.cookie.http-only"));
    }

    @Test
    void httpCookieKeepsLegacyNameAndDoesNotForceSecureLocally() {
        String setCookie = writeCookie(false);

        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith("JSESSIONID="));
        assertTrue(setCookie.contains("Path=/"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertFalse(setCookie.contains("Secure"));
    }

    @Test
    void secureRequestReceivesSecureCookieWithoutForcingItForHttp() {
        String setCookie = writeCookie(true);

        assertNotNull(setCookie);
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=Lax"));
    }

    @Test
    void springSessionUuidRemainsCompatibleWithLegacyEightCharacterDerivation() {
        String derived = UUIDUtils.getUUID8Len(new MapSession().getId());

        assertEquals(8, derived.length());
        assertTrue(derived.matches("[A-Za-z0-9]{8}"));
    }

    private PropertySourcesPropertyResolver applicationProperties() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> loaded = loader.load(
                "application", new ClassPathResource("application.yml"));
        MutablePropertySources propertySources = new MutablePropertySources();
        for (PropertySource<?> propertySource : loaded) {
            propertySources.addLast(propertySource);
        }
        return new PropertySourcesPropertyResolver(propertySources);
    }

    private String writeCookie(boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        DefaultCookieSerializerCustomizer customizer =
                new HttpSessionRedisConfiguration().jusicHttpSessionCookieCustomizer();
        customizer.customize(serializer);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(secure);
        request.setScheme(secure ? "https" : "http");
        MockHttpServletResponse response = new MockHttpServletResponse();
        serializer.writeCookieValue(new CookieValue(request, response, "session-id"));
        return response.getHeader("Set-Cookie");
    }
}
