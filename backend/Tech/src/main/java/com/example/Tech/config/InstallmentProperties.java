package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Installment partners are simulated. {@code autoDecideSeconds}: seconds after an application is sent until the
 * simulated partner answers on its own; a negative value switches the automatic answer off (an ADMIN or STAFF member
 * then answers from the partner console, which is how the tests drive it).
 */
@ConfigurationProperties(prefix = "app.payment.installment")
public record InstallmentProperties(int autoDecideSeconds) {

    public boolean autoDecide() {
        return autoDecideSeconds >= 0;
    }
}
