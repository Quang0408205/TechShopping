package com.example.Tech.service.payment;

import com.example.Tech.dto.request.payment.AdminInstallmentSearchRequest;
import com.example.Tech.dto.request.payment.PaymentConfirmRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.payment.AdminInstallmentResponse;
import org.springframework.data.domain.Pageable;

/**
 * Installment applications for staff (STAFF or ADMIN, re-checked in the database on every call). The partner's
 * answer reaches the order through the webhook or the partner console (AdminOrderService).
 */
public interface AdminInstallmentService {

    PageResponse<AdminInstallmentResponse> search(Long staffId, AdminInstallmentSearchRequest filter, Pageable pageable);

    /** 404 INSTALLMENT_NOT_FOUND. */
    AdminInstallmentResponse getById(Long staffId, Long installmentId);

    /**
     * Records that the partner paid the shop the net amount of an APPROVED application whose order was delivered.
     * 409 INVALID_INSTALLMENT_STATUS otherwise.
     */
    AdminInstallmentResponse settle(Long staffId, Long installmentId);
}
