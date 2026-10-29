package com.example.Tech.service.payment;

import com.example.Tech.dto.request.payment.BankSimulateRequest;
import com.example.Tech.dto.request.payment.BankTransactionAssignRequest;
import com.example.Tech.dto.request.payment.BankTransactionIgnoreRequest;
import com.example.Tech.dto.request.payment.BankWebhookRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.payment.BankReconciliationInfoResponse;
import com.example.Tech.dto.response.payment.BankTransactionResponse;
import com.example.Tech.dto.response.payment.BankWebhookResponse;
import com.example.Tech.entity.payment.BankTransactionStatus;
import org.springframework.data.domain.Pageable;

/** Bank transfers reported by the webhook: automatic matching to orders and the ADMIN reconciliation. */
public interface BankTransactionService {

    /** The webhook entry point (also used by the simulator). The caller has already checked the shared key. */
    BankWebhookResponse receive(BankWebhookRequest request, String provider);

    PageResponse<BankTransactionResponse> search(BankTransactionStatus status, boolean attentionOnly, Pageable pageable);

    BankReconciliationInfoResponse info();

    /** ADMIN applies an UNMATCHED / REVIEW transfer to an order by hand. */
    BankTransactionResponse assign(Long adminId, Long id, BankTransactionAssignRequest request);

    /** ADMIN marks an UNMATCHED / REVIEW transfer as unrelated to the shop. */
    BankTransactionResponse ignore(Long adminId, Long id, BankTransactionIgnoreRequest request);

    /** Demo only (PAYMENT_SIMULATOR_ENABLED): fakes a bank notice and runs it through {@link #receive}. */
    BankWebhookResponse simulate(BankSimulateRequest request);

    /**
     * Demo only (PAYMENT_SIMULATOR_ENABLED): the customer's payment page showed the QR for a minute, so the transfer
     * is taken as received. Own BANK_TRANSFER orders only; a paid / cancelled order is left as it is.
     */
    BankWebhookResponse demoConfirm(Long userId, Long orderId);
}
