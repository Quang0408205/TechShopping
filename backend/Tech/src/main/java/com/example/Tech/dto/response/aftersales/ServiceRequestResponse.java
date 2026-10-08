package com.example.Tech.dto.response.aftersales;

import com.example.Tech.entity.aftersales.MaintenanceType;
import com.example.Tech.entity.aftersales.ReturnReasonType;
import com.example.Tech.entity.aftersales.ServiceRequestType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** One shape for the three request types; fields that do not apply to a type are null. */
public record ServiceRequestResponse(
        ServiceRequestType type,
        Long id,
        String code,
        String status,
        boolean cancellable,
        Long orderId,
        String orderCode,
        String storeName,
        Long customerId,
        String customerName,
        String customerEmail,
        String recipientPhone,
        List<Item> items,
        String description,
        MaintenanceType maintenanceType,
        ReturnReasonType reasonType,
        List<String> imageUrls,
        LocalDate warrantyEndDate,
        LocalDate estimatedCompletionDate,
        BigDecimal estimatedCost,
        BigDecimal actualCost,
        BigDecimal refundAmount,
        String rejectionReason,
        String notes,
        String handlerName,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime receivedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        LocalDateTime updatedAt
) {

    public record Item(
            Long orderItemId,
            Long productId,
            String productName,
            String variantName,
            String imageUrl,
            Integer quantity,
            BigDecimal refundAmount,
            Boolean restocked
    ) {
    }
}
