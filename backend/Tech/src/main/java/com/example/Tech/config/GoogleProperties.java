package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Google sign-in: an empty client id switches it off (the button is hidden and the endpoint answers 503). */
@ConfigurationProperties(prefix = "app.google")
public record GoogleProperties(String clientId) {

    public GoogleProperties {
        clientId = StringUtils.hasText(clientId) ? clientId.trim() : null;
    }

    public boolean enabled() {
        return clientId != null;
    }
}
