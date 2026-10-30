package com.example.Tech.controller.payment;

import org.springframework.web.bind.annotation.RequestParam;
import com.example.Tech.service.payment.installment.InstallmentPartnerPolicy;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.payment.CardOptionsResponse;
import com.example.Tech.dto.response.payment.InstallmentOptionsResponse;
import com.example.Tech.entity.payment.InstallmentBank;
import com.example.Tech.service.payment.CardPaymentService;
import com.example.Tech.service.payment.InstallmentPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;

/** Payment rules for the checkout page. Requires a valid access token (the /api/v1/** rule of SecurityConfig). */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment options (payments are simulated)")
public class PaymentController {

    private final CardPaymentService cardPaymentService;
    private final InstallmentPartnerPolicy installmentPartnerPolicy;

    @GetMapping("/installment-options")
    @Operation(summary = "Installment partners the customer can choose, with their terms, order range and the card banks",
            description = "With total, only the partners that accept that order total; the partner fees are never returned")
    @ApiResponse(responseCode = "200", description = "Options returned")
    public ResponseEntity<ApiResult<InstallmentOptionsResponse>> getInstallmentOptions(
            @RequestParam(required = false) BigDecimal total) {
        return ResponseEntity.ok(ApiResult.ok(installmentPartnerPolicy.options(total)));
    }

    @GetMapping("/card-options")
    @Operation(summary = "Whether the card method (Stripe, test mode) is offered, and its largest order total")
    @ApiResponse(responseCode = "200", description = "Options returned")
    public ResponseEntity<ApiResult<CardOptionsResponse>> getCardOptions() {
        return ResponseEntity.ok(ApiResult.ok(cardPaymentService.options()));
    }
}
