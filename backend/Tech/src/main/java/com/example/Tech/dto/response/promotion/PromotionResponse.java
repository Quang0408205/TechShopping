package com.example.Tech.dto.response.promotion;

import com.example.Tech.entity.promotion.DiscountType;
import com.example.Tech.entity.promotion.PromotionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PromotionResponse(
        Long id,
        String name,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean active,

        @Schema(description = "Derived from active and the dates at the time of the request")
        PromotionStatus status,

        Long createdById,
        String createdByName,
        List<PromotionProductResponse> products,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
