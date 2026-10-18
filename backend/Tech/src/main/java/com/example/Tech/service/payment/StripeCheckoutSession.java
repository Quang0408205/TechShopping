package com.example.Tech.service.payment;

/**
 * What the shop reads from a Stripe Checkout session. {@code status}: open / complete / expired;
 * {@code paymentStatus}: paid / unpaid / no_payment_required. {@code orderId} comes from client_reference_id.
 */
public record StripeCheckoutSession(String id, String url, String status, String paymentStatus, Long amountTotal,
                                    String currency, String paymentIntent, Long orderId) {

    public boolean paid() {
        return "paid".equals(paymentStatus);
    }

    public boolean open() {
        return "open".equals(status);
    }
}
