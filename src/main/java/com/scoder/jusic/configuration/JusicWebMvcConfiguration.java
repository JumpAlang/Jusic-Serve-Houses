package com.scoder.jusic.configuration;

import com.scoder.jusic.interceptor.HttpAbuseProtectionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class JusicWebMvcConfiguration implements WebMvcConfigurer {

    private final HttpAbuseProtectionInterceptor httpAbuseProtectionInterceptor;

    public JusicWebMvcConfiguration(HttpAbuseProtectionInterceptor httpAbuseProtectionInterceptor) {
        this.httpAbuseProtectionInterceptor = httpAbuseProtectionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpAbuseProtectionInterceptor)
                .addPathPatterns(
                        "/house/add",
                        "/house/enter",
                        "/house/search",
                        "/house/get",
                        "/house/getMiniCode",
                        "/house/edit",
                        "/house/setSize/**",
                        "/house/addRetainKey/**",
                        "/house/showRetainKey",
                        "/house/removeRetainKey/**",
                        "/user/get",
                        "/bili/**",
                        "/netease/**",
                        "/qqmusic/**"
                );
    }
}
