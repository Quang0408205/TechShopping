package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Where the AI service runs and how long the backend waits for it (app.ai in application.yml). */
@ConfigurationProperties(prefix = "app.ai")
public record AiServiceProperties(String baseUrl, String internalKey, Duration timeout, Duration chatTimeout) {

    public AiServiceProperties {
        baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl : "http://localhost:8000";
        internalKey = internalKey != null ? internalKey : "";
        timeout = timeout != null && timeout.isPositive() ? timeout : Duration.ofSeconds(3);
        chatTimeout = chatTimeout != null && chatTimeout.isPositive() ? chatTimeout : Duration.ofSeconds(90);
    }
}
