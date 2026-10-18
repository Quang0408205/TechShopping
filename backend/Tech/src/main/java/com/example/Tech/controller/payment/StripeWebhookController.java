package com.example.Tech.controller.payment;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.service.payment.CardPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by Stripe, not by a browser: no access token. The body is read as the raw text Stripe signed (parsing it
 * first would change the bytes and break the signature).
 */
@RestController
@RequestMapping("/api/v1/payments/stripe/webhook")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Stripe webhook (signed with the endpoint's whsec_ secret)")
public class StripeWebhookController {

    private final CardPaymentService cardPaymentService;

    @PostMapping
    @Operation(summary = "Stripe event (checkout.session.completed / async_payment_succeeded mark the card order paid)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Handled or ignored"),
            @ApiResponse(responseCode = "400", description = "STRIPE_SIGNATURE_INVALID"),
            @ApiResponse(responseCode = "503", description = "WEBHOOK_NOT_CONFIGURED (STRIPE_WEBHOOK_SECRET empty)")
    })
    public ResponseEntity<ApiResult<Void>> receive(
            @Parameter(hidden = true) @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody String payload) {
        cardPaymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok(ApiResult.ok(null));
    }
}
