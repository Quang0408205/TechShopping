package com.example.Tech.service.payment;

import java.time.Instant;

/** The few Stripe Checkout calls the shop needs; failures throw BusinessException(PAYMENT_GATEWAY_ERROR). */
public interface StripeGateway {

    StripeCheckoutSession createSession(NewSession request);

    StripeCheckoutSession getSession(String sessionId);

    /** Closes an open session so it can no longer be paid; a session that is not open is left as it is. */
    void expireSession(String sessionId);

    record NewSession(Long orderId, String orderCode, long amountVnd, String customerEmail,
                      String successUrl, String cancelUrl, Instant expiresAt) {
    }
}
