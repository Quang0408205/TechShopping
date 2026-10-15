package com.example.Tech.controller.payment;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.payment.InstallmentOptionsResponse;
import com.example.Tech.entity.payment.InstallmentBank;
import com.example.Tech.service.payment.InstallmentPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;

/** Payment rules for the checkout page. Requires a valid access token (the /api/v1/** rule of SecurityConfig). */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment options (payments are simulated)")
public class PaymentController {

    private static final InstallmentOptionsResponse INSTALLMENT_OPTIONS = new InstallmentOptionsResponse(
            InstallmentPolicy.TERMS_IN_MONTHS,
            InstallmentPolicy.MIN_ORDER_TOTAL,
            BigDecimal.ZERO,
            InstallmentPolicy.CITIZEN_ID_LENGTH,
            Arrays.stream(InstallmentBank.values())
                    .map(bank -> new InstallmentOptionsResponse.Bank(bank.name(), bank.getDisplayName()))
                    .toList());

    @GetMapping("/installment-options")
    @Operation(summary = "Installment terms, minimum order total, citizen id length and card banks")
    @ApiResponse(responseCode = "200", description = "Options returned")
    public ResponseEntity<ApiResult<InstallmentOptionsResponse>> getInstallmentOptions() {
        return ResponseEntity.ok(ApiResult.ok(INSTALLMENT_OPTIONS));
    }
}
