package com.scoder.jusic.configuration;

import org.springframework.boot.autoconfigure.session.DefaultCookieSerializerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP session cookie compatibility for Spring Session Redis.
 *
 * <p>Secure is deliberately left unset. DefaultCookieSerializer then follows
 * the request security state: local HTTP remains usable while a direct HTTPS
 * request receives a Secure cookie. TLS-terminating proxies must forward the
 * original scheme correctly or explicitly enable the production cookie flag.</p>
 */
@Configuration
public class HttpSessionRedisConfiguration {

    @Bean
    public DefaultCookieSerializerCustomizer jusicHttpSessionCookieCustomizer() {
        return cookieSerializer -> {
            cookieSerializer.setCookieName("JSESSIONID");
            cookieSerializer.setCookiePath("/");
            cookieSerializer.setUseHttpOnlyCookie(true);
            cookieSerializer.setSameSite("Lax");
        };
    }
}
