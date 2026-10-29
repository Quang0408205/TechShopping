package com.example.Tech.dto.request.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CardCheckoutRequest(
        @Schema(description = "The shop's payment page Stripe sends the customer back to; its origin must be an allowed "
                + "CORS origin", example = "http://localhost:5510/customer/payment.html")
        @NotBlank @Size(max = 500)
        String returnUrl
) {
}
