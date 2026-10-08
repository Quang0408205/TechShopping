package com.example.Tech.dto.response.aftersales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** What the customer may still ask for on a delivered order (order detail page). */
public record AfterSalesOrderResponse(
        Long orderId,
        String orderCode,
        LocalDateTime deliveredAt,
        LocalDateTime returnDeadline,
        boolean returnAvailable,
        String returnUnavailableReason,
        List<Item> items
) {

    public record Item(
            Long orderItemId,
            Long productId,
            String productName,
            String variantName,
            String imageUrl,
            Integer quantity,
            BigDecimal unitPrice,
            LocalDate warrantyStartDate,
            LocalDate warrantyEndDate,
            boolean warrantyValid,
            Long openWarrantyRequestId,
            Long openMaintenanceRequestId,
            int returnableQuantity
    ) {
    }
}
