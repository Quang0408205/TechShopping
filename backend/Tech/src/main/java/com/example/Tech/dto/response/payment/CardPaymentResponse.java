package com.example.Tech.dto.response.payment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** A card payment still awaited: the payment page offers the Stripe button. */
public record CardPaymentResponse(
        @Schema(description = "Stripe test mode: pay with test cards only")
        boolean testMode,
        @Schema(description = "Order date + the payment deadline (24 h); informative only")
        LocalDateTime payBefore
) {
}
