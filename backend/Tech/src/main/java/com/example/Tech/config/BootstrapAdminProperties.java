package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * First ADMIN account, read from app.bootstrap-admin.* (environment variables ADMIN_EMAIL,
 * ADMIN_USERNAME, ADMIN_PASSWORD). Values are never stored in source code.
 */
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(
        String email,
        String username,
        String password
) {

    public boolean isComplete() {
        return StringUtils.hasText(email) && StringUtils.hasText(username) && StringUtils.hasText(password);
    }
}
