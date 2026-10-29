package com.example.Tech.dto.response.order;

import com.example.Tech.dto.response.payment.InstallmentResponse;
import com.example.Tech.dto.response.payment.PaymentResponse;
import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        @Schema(description = "Code shown to people: DH + the id on 8 digits", example = "DH00000042")
        String code,
        OrderStatus status,
        PaymentMethod paymentMethod,
        String recipientName,
        String recipientPhone,
        String shippingAddress,
        @Schema(description = "Province of a home delivery placed with the 2-level address; null otherwise")
        String shippingProvince,
        String shippingWard,
        String note,
        List<OrderItemResponse> items,
        @Schema(description = "Sum of the item quantities")
        int totalQuantity,
        @Schema(description = "Sum of the item subtotals")
        BigDecimal subtotal,
        @Schema(description = "Plan v5 GĐ10: total combo discount, taken off before the voucher")
        BigDecimal comboDiscount,
        @Schema(description = "Combos applied to the order (name copied at checkout)")
        List<ComboApplied> combos,
        @Schema(description = "Plan v5: voucher used (null when none) and the amount it took off the goods")
        String voucherCode,
        BigDecimal voucherDiscount,
        @Schema(description = "Plan v5 GĐ6: loyalty points spent and the amount they took off (1 point = 1.000đ)")
        int pointsUsed,
        BigDecimal pointsDiscount,
        @Schema(description = "Points credited when the order was delivered (0 before)")
        int pointsEarned,
        BigDecimal shippingFee,
        BigDecimal taxAmount,
        @Schema(description = "subtotal − comboDiscount − voucherDiscount − pointsDiscount + shippingFee + taxAmount")
        BigDecimal total,
        String trackingNumber,
        LocalDateTime orderDate,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt,
        LocalDateTime updatedAt,
        @Schema(description = "true while the customer may still cancel (PENDING)")
        boolean cancellable,
        @Schema(description = "Main payment of a COD / BANK_TRANSFER / CARD order; null for INSTALLMENT")
        PaymentResponse payment,
        @Schema(description = "Installment plan of an INSTALLMENT order; null otherwise")
        InstallmentResponse installment,
        @Schema(description = "HOME_DELIVERY or PICKUP")
        DeliveryType deliveryType,
        @Schema(description = "Store that handles the order (nearest to the address, or the pickup store); "
                + "null for orders placed before stores existed")
        Integer storeId,
        String storeName
) {

    public record ComboApplied(String name, int units, BigDecimal discount) {
    }
}
