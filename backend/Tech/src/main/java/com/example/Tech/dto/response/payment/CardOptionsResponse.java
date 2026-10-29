package com.example.Tech.dto.response.payment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Whether the checkout offers "Thẻ quốc tế" (Stripe). */
public record CardOptionsResponse(
        boolean enabled,
        @Schema(description = "Test keys: no real money, test cards such as 4242 4242 4242 4242")
        boolean testMode,
        @Schema(description = "Largest order total Stripe accepts in VND")
        BigDecimal maxAmount
) {
}
