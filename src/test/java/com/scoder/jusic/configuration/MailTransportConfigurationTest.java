package com.scoder.jusic.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailTransportConfigurationTest {

    @Test
    void smtpTransportCannotBlockTheSharedBackgroundPoolForever() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> loaded = loader.load(
                "application", new ClassPathResource("application.yml"));
        MutablePropertySources sources = new MutablePropertySources();
        for (PropertySource<?> source : loaded) {
            sources.addLast(source);
        }
        PropertySourcesPropertyResolver properties =
                new PropertySourcesPropertyResolver(sources);

        assertEquals("3000", properties.getProperty(
                "spring.mail.properties.mail.smtp.connectiontimeout"));
        assertEquals("5000", properties.getProperty(
                "spring.mail.properties.mail.smtp.timeout"));
        assertEquals("5000", properties.getProperty(
                "spring.mail.properties.mail.smtp.writetimeout"));
    }
}

