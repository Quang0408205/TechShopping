package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Per-IP limit on the search box suggestions (Redis counter). Behind the deploy proxy every visitor has
 * the proxy's IP, so the default is generous; the page also waits for a pause in typing before asking.
 */
@ConfigurationProperties(prefix = "app.search")
public record SearchProperties(Integer suggestPerIp, Duration suggestWindow) {

    public SearchProperties {
        suggestPerIp = suggestPerIp != null && suggestPerIp > 0 ? suggestPerIp : 600;
        suggestWindow = suggestWindow != null && !suggestWindow.isNegative() && !suggestWindow.isZero()
                ? suggestWindow : Duration.ofMinutes(1);
    }

    @Configuration
    @EnableConfigurationProperties(SearchProperties.class)
    static class Registration {
    }
}
