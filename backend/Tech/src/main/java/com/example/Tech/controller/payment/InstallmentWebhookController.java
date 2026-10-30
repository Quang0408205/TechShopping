package com.example.Tech.controller.payment;

import com.example.Tech.config.PaymentWebhookProperties;
import com.example.Tech.dto.request.payment.InstallmentWebhookRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.service.payment.InstallmentDecisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Called by an installment partner, not by a browser: no access token, the shared key is the credential (sent as
 * {@code Authorization: Apikey <key>} or {@code X-Webhook-Secret: <key>}, like the bank webhook).
 */
@RestController
@RequestMapping("/api/v1/payments/installment/webhook")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Installment partner webhook (called by the partner, authenticated by a shared key)")
public class InstallmentWebhookController {

    private static final String APIKEY_PREFIX = "Apikey ";

    private final InstallmentDecisionService decisions;
    private final PaymentWebhookProperties webhook;

    @PostMapping
    @Operation(summary = "The partner approved or rejected an installment application",
            description = "Approved: the order counts as paid and the customer is mailed. Rejected: the order stays pending "
                    + "and the customer may choose another partner. The same eventId twice is applied once")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applied (applied = false when the event was already applied)"),
            @ApiResponse(responseCode = "401", description = "WEBHOOK_UNAUTHORIZED"),
            @ApiResponse(responseCode = "404", description = "INSTALLMENT_NOT_FOUND (unknown partnerRef)"),
            @ApiResponse(responseCode = "409", description = "INVALID_INSTALLMENT_STATUS (not waiting for the partner)"),
            @ApiResponse(responseCode = "503", description = "WEBHOOK_NOT_CONFIGURED")
    })
    public ResponseEntity<ApiResult<Map<String, Boolean>>> receive(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(hidden = true) @RequestHeader(value = "X-Webhook-Secret", required = false) String secretHeader,
            @Valid @RequestBody InstallmentWebhookRequest request) {
        if (!webhook.configured()) {
            throw new BusinessException(ErrorCode.WEBHOOK_NOT_CONFIGURED);
        }
        String sent = secretHeader != null ? secretHeader
                : authorization != null && authorization.startsWith(APIKEY_PREFIX)
                ? authorization.substring(APIKEY_PREFIX.length()) : null;
        if (!webhook.accepts(sent)) {
            throw new BusinessException(ErrorCode.WEBHOOK_UNAUTHORIZED);
        }
        boolean applied = decisions.applyByPartnerRef(request.partnerRef().trim(), "APPROVED".equals(request.decision()),
                request.reason(), request.eventId().trim());
        return ResponseEntity.ok(ApiResult.ok(Map.of("applied", applied)));
    }
}
