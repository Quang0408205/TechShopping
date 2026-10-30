package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Bank webhook (POST /api/v1/payments/webhook). {@code secret} is the key the bank / provider sends; empty = the
 * webhook is switched off (503). {@code simulatorEnabled} lets an ADMIN fake a transfer for demos (off by default).
 */
@ConfigurationProperties(prefix = "app.payment.webhook")
public record PaymentWebhookProperties(String secret, boolean simulatorEnabled) {

    public PaymentWebhookProperties {
        secret = StringUtils.hasText(secret) ? secret.trim() : "";
    }

    public boolean configured() {
        return !secret.isEmpty();
    }

    /** True when the key a caller sent (header value, trimmed) is the shared secret; compared in constant time. */
    public boolean accepts(String sent) {
        return sent != null && java.security.MessageDigest.isEqual(
                sent.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
