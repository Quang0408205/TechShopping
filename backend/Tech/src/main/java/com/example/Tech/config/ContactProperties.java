package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Contact form send limits, counted in Redis per client IP and per email over one window.
 * The IP is the TCP peer (X-Forwarded-For is not trusted: there is no proxy in front of the API).
 */
@ConfigurationProperties(prefix = "app.contact.rate-limit")
public record ContactProperties(Integer perIp, Integer perEmail, Duration window) {

    public ContactProperties {
        perIp = perIp != null && perIp > 0 ? perIp : 10;
        perEmail = perEmail != null && perEmail > 0 ? perEmail : 3;
        window = window != null && !window.isNegative() && !window.isZero() ? window : Duration.ofHours(1);
    }
}
