package com.example.Tech.controller.payment;

import com.example.Tech.config.PaymentWebhookProperties;
import com.example.Tech.dto.request.payment.BankWebhookRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.payment.BankWebhookResponse;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.service.payment.BankTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Called by the bank / payment provider, not by a browser: no access token, the shared key is the credential.
 * It is sent as {@code Authorization: Apikey <key>} (SePay style) or {@code X-Webhook-Secret: <key>}.
 */
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Bank webhook (called by the bank, authenticated by a shared key)")
public class BankWebhookController {

    private static final String APIKEY_PREFIX = "Apikey ";

    private final BankTransactionService bankTransactionService;
    private final PaymentWebhookProperties webhook;

    @PostMapping
    @Operation(summary = "Bank balance change (money in)",
            description = "Reads the order code from the transfer content and confirms the bank transfer when the amount "
                    + "matches; anything else is kept for the ADMIN reconciliation. The same transaction id twice is stored once")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stored (or already stored: duplicate = true)"),
            @ApiResponse(responseCode = "401", description = "WEBHOOK_UNAUTHORIZED"),
            @ApiResponse(responseCode = "503", description = "WEBHOOK_NOT_CONFIGURED")
    })
    public ResponseEntity<ApiResult<BankWebhookResponse>> receive(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(hidden = true) @RequestHeader(value = "X-Webhook-Secret", required = false) String secretHeader,
            @Valid @RequestBody BankWebhookRequest request) {
        if (!webhook.configured()) {
            throw new BusinessException(ErrorCode.WEBHOOK_NOT_CONFIGURED);
        }
        String sent = secretHeader != null ? secretHeader
                : authorization != null && authorization.startsWith(APIKEY_PREFIX)
                ? authorization.substring(APIKEY_PREFIX.length()) : null;
        if (sent == null || !MessageDigest.isEqual(sent.trim().getBytes(StandardCharsets.UTF_8),
                webhook.secret().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.WEBHOOK_UNAUTHORIZED);
        }
        try {
            return ResponseEntity.ok(ApiResult.ok(bankTransactionService.receive(request, "WEBHOOK")));
        } catch (DataIntegrityViolationException duplicate) {
            // two deliveries of the same transaction id raced: the other one stored it
            return ResponseEntity.ok(ApiResult.ok(new BankWebhookResponse(true, null, null)));
        }
    }
}
