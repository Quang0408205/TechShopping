package com.example.Tech.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Redis read cache (Phase 10.5). Switched off in the test profile so tests never see each other's cached data. */
@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(Boolean enabled, Duration catalogTtl, Duration productTtl, Duration reportTtl) {

    public CacheProperties {
        enabled = enabled == null || enabled;
        catalogTtl = positive(catalogTtl, Duration.ofHours(1));
        productTtl = positive(productTtl, Duration.ofMinutes(10));
        reportTtl = positive(reportTtl, Duration.ofMinutes(5));
    }

    private static Duration positive(Duration value, Duration fallback) {
        return value != null && !value.isNegative() && !value.isZero() ? value : fallback;
    }

    @Configuration
    @EnableConfigurationProperties(CacheProperties.class)
    static class Config {
    }
}
