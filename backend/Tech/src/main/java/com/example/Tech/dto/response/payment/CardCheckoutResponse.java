package com.example.Tech.dto.response.payment;

import io.swagger.v3.oas.annotations.media.Schema;

/** Where to send the customer to pay; {@code paid} = Stripe already has the money, no page to open. */
public record CardCheckoutResponse(
        @Schema(description = "Stripe Checkout page (null when already paid)")
        String checkoutUrl,
        boolean paid
) {
}
