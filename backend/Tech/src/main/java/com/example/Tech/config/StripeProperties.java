package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Card payments through Stripe Checkout (plan v5 GĐ5). Stripe has no merchant accounts in Vietnam, so only test
 * keys ({@code sk_test_…}) are meant to be used: no real money moves. Empty {@code secretKey} = the card method is
 * hidden and refused. {@code webhookSecret} ({@code whsec_…}) checks the signature of Stripe's webhook; without it
 * the payment is still confirmed when the customer comes back from Stripe. {@code apiBase} is only changed by the
 * E2E tests (a local fake Stripe).
 */
@ConfigurationProperties(prefix = "app.payment.stripe")
public record StripeProperties(String secretKey, String webhookSecret, String apiBase, Integer sessionMinutes) {

    /** Stripe takes at most 8 digits for an amount; VND has no minor unit. */
    public static final long MAX_AMOUNT_VND = 99_999_999L;

    public StripeProperties {
        secretKey = StringUtils.hasText(secretKey) ? secretKey.trim() : "";
        webhookSecret = StringUtils.hasText(webhookSecret) ? webhookSecret.trim() : "";
        apiBase = StringUtils.hasText(apiBase) ? apiBase.trim().replaceAll("/+$", "") : "https://api.stripe.com";
        // Stripe accepts 30 minutes to 24 hours
        sessionMinutes = sessionMinutes == null ? 30 : Math.max(30, Math.min(sessionMinutes, 24 * 60));
    }

    public boolean enabled() {
        return !secretKey.isEmpty();
    }

    public boolean webhookConfigured() {
        return !webhookSecret.isEmpty();
    }

    public boolean testMode() {
        return !secretKey.startsWith("sk_live_");
    }
}
