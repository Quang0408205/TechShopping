package com.example.Tech.dto.request.promotion;

import com.example.Tech.entity.promotion.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Create or update (full replace, including the product list) a promotion.
 */
public record PromotionRequest(

        @NotBlank(message = "Vui lòng nhập tên chương trình")
        @Size(max = 255, message = "Tên chương trình tối đa 255 ký tự")
        @Schema(example = "Flash Sale tháng 10")
        String name,

        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String description,

        @NotNull(message = "Vui lòng chọn kiểu giảm giá")
        @Schema(example = "PERCENTAGE")
        DiscountType discountType,

        @NotNull(message = "Vui lòng nhập mức giảm")
        @Positive(message = "Mức giảm phải lớn hơn 0")
        @Schema(description = "Percent (1–100) or an amount in VND, depending on discountType", example = "10")
        BigDecimal discountValue,

        @Positive(message = "Mức giảm tối đa phải lớn hơn 0")
        @Schema(description = "Cap on the amount taken off by any percentage discount of this promotion "
                + "(default or a product's own); null = no cap", example = "2000000")
        BigDecimal maxDiscountAmount,

        @NotNull(message = "Vui lòng chọn thời gian bắt đầu")
        @Schema(example = "2026-10-10T00:00:00")
        LocalDateTime startDate,

        @NotNull(message = "Vui lòng chọn thời gian kết thúc")
        @Schema(example = "2026-10-20T23:59:59")
        LocalDateTime endDate,

        @Schema(description = "false = paused; null counts as true", example = "true")
        Boolean active,

        @NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm")
        @Size(max = 200, message = "Một chương trình tối đa 200 sản phẩm")
        List<@NotNull(message = "Sản phẩm không hợp lệ") @Valid PromotionProductSelection> products
) {
}
