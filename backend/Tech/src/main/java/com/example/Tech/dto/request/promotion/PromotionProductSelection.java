package com.example.Tech.dto.request.promotion;

import com.example.Tech.entity.promotion.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * A product selected into a promotion. discountType and discountValue are both set (own discount for this
 * product) or both null (the promotion's default discount).
 */
public record PromotionProductSelection(

        @NotNull(message = "Vui lòng chọn sản phẩm")
        @Schema(example = "12")
        Long productId,

        @Schema(description = "Own discount type; null = the promotion's default", example = "PERCENTAGE")
        DiscountType discountType,

        @Positive(message = "Mức giảm riêng phải lớn hơn 0")
        @Schema(description = "Own discount value; null = the promotion's default", example = "15")
        BigDecimal discountValue
) {
}
