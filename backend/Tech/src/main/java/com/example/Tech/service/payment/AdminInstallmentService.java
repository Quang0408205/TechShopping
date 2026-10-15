package com.example.Tech.service.payment;

import com.example.Tech.dto.request.payment.AdminInstallmentSearchRequest;
import com.example.Tech.dto.request.payment.PaymentConfirmRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.payment.AdminInstallmentResponse;
import org.springframework.data.domain.Pageable;

/**
 * Installment plans for staff (STAFF or ADMIN, re-checked in the database on every call). Approving and
 * rejecting a plan are order actions (AdminOrderService).
 */
public interface AdminInstallmentService {

    PageResponse<AdminInstallmentResponse> search(Long staffId, AdminInstallmentSearchRequest filter, Pageable pageable);

    /** 404 INSTALLMENT_NOT_FOUND. */
    AdminInstallmentResponse getById(Long staffId, Long installmentId);

    /**
     * Records period {@code number} as paid in full. 409 INVALID_INSTALLMENT_STATUS (plan not ACTIVE),
     * 409 INSTALLMENT_PERIOD_OUT_OF_ORDER (not the earliest unpaid period). The last period completes the plan.
     */
    AdminInstallmentResponse payPeriod(Long staffId, Long installmentId, int number, PaymentConfirmRequest request);
}
