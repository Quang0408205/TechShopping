package com.example.Tech.controller.payment;

import com.example.Tech.dto.request.payment.BankSimulateRequest;
import com.example.Tech.dto.request.payment.BankTransactionAssignRequest;
import com.example.Tech.dto.request.payment.BankTransactionIgnoreRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.payment.BankReconciliationInfoResponse;
import com.example.Tech.dto.response.payment.BankTransactionResponse;
import com.example.Tech.dto.response.payment.BankWebhookResponse;
import com.example.Tech.entity.payment.BankTransactionStatus;
import com.example.Tech.service.payment.BankTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reconciliation of the money that arrived on the shop account. ADMIN only (the account belongs to the company). */
@RestController
@RequestMapping("/api/v1/admin/bank-transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Bank transactions", description = "Bank transfers reported by the webhook (ADMIN)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED")
})
public class AdminBankTransactionController {

    private final BankTransactionService bankTransactionService;

    @GetMapping
    @Operation(summary = "Bank transactions, newest first",
            description = "status filters one status; attention=true lists UNMATCHED + REVIEW (what still needs a person)")
    @ApiResponse(responseCode = "200", description = "Page of transactions")
    public ResponseEntity<ApiResult<PageResponse<BankTransactionResponse>>> search(
            @RequestParam(required = false) BankTransactionStatus status,
            @RequestParam(defaultValue = "false") boolean attention,
            @ParameterObject @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(bankTransactionService.search(status, attention, pageable)));
    }

    @GetMapping("/info")
    @Operation(summary = "Webhook / simulator set-up and the number of transactions that need attention")
    @ApiResponse(responseCode = "200", description = "Set-up returned")
    public ResponseEntity<ApiResult<BankReconciliationInfoResponse>> info() {
        return ResponseEntity.ok(ApiResult.ok(bankTransactionService.info()));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Apply an UNMATCHED / REVIEW transfer to an order (confirms its bank transfer)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The transaction, now RESOLVED"),
            @ApiResponse(responseCode = "404", description = "BANK_TRANSACTION_NOT_FOUND, ORDER_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_BANK_TRANSACTION (already handled, order not payable)")
    })
    public ResponseEntity<ApiResult<BankTransactionResponse>> assign(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody BankTransactionAssignRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bankTransactionService.assign(Long.valueOf(jwt.getSubject()), id, request)));
    }

    @PostMapping("/{id}/ignore")
    @Operation(summary = "Mark an UNMATCHED / REVIEW transfer as unrelated to the shop (a reason is required)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The transaction, now IGNORED"),
            @ApiResponse(responseCode = "404", description = "BANK_TRANSACTION_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_BANK_TRANSACTION")
    })
    public ResponseEntity<ApiResult<BankTransactionResponse>> ignore(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody BankTransactionIgnoreRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bankTransactionService.ignore(Long.valueOf(jwt.getSubject()), id, request)));
    }

    @PostMapping("/simulate")
    @Operation(summary = "Demo: fake a bank notice \"money received\"",
            description = "Only when PAYMENT_SIMULATOR_ENABLED=true (404 SIMULATOR_DISABLED otherwise). "
                    + "Goes through the same matching as the real webhook")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "How the fake transfer was matched"),
            @ApiResponse(responseCode = "404", description = "SIMULATOR_DISABLED")
    })
    public ResponseEntity<ApiResult<BankWebhookResponse>> simulate(@Valid @RequestBody BankSimulateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bankTransactionService.simulate(request)));
    }
}
