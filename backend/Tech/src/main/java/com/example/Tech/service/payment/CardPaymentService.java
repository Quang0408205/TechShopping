package com.example.Tech.service.payment;

import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.dto.response.payment.CardCheckoutResponse;
import com.example.Tech.dto.response.payment.CardOptionsResponse;

/** "Thẻ quốc tế": Stripe Checkout in test mode (plan v5 GĐ5). */
public interface CardPaymentService {

    CardOptionsResponse options();

    /** Opens (or reuses the still open) Stripe page for my awaited card payment. */
    CardCheckoutResponse startCheckout(Long userId, Long orderId, String returnUrl);

    /** Asks Stripe whether the session of my order is paid (the customer came back) and records it; works without webhook. */
    OrderResponse refresh(Long userId, Long orderId);

    /** Stripe webhook: the caller passes the raw body and the Stripe-Signature header. */
    void handleWebhook(String payload, String signatureHeader);
}
