package com.example.Tech.dto.response.promotion;

import com.example.Tech.entity.promotion.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record PromotionProductResponse(
        Long productId,
        String productName,
        String productSlug,
        BigDecimal basePrice,
        String primaryImageUrl,

        @Schema(description = "false when the product was deleted or hidden after it was selected")
        boolean productAvailable,

        @Schema(description = "The discount applied to this product: its own when override is true, "
                + "else the promotion's default")
        DiscountType discountType,
        BigDecimal discountValue,
        boolean override
) {
}
