package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Login lock and per-IP limits on the auth endpoints (counters live in Redis).
 * The IP is the TCP peer (X-Forwarded-For is not trusted: there is no proxy in front of the API).
 */
@ConfigurationProperties(prefix = "app.auth.rate-limit")
public record AuthRateLimitProperties(Integer maxLoginFailures, Duration failureWindow, Duration lockDuration,
                                      Integer registerPerIp, Duration registerWindow,
                                      Integer refreshPerIp, Duration refreshWindow) {

    public AuthRateLimitProperties {
        maxLoginFailures = positive(maxLoginFailures, 5);
        failureWindow = positive(failureWindow, Duration.ofMinutes(15));
        lockDuration = positive(lockDuration, Duration.ofMinutes(15));
        registerPerIp = positive(registerPerIp, 10);
        registerWindow = positive(registerWindow, Duration.ofHours(1));
        refreshPerIp = positive(refreshPerIp, 60);
        refreshWindow = positive(refreshWindow, Duration.ofMinutes(1));
    }

    private static Integer positive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private static Duration positive(Duration value, Duration fallback) {
        return value != null && !value.isNegative() && !value.isZero() ? value : fallback;
    }
}
