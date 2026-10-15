package com.example.Tech.dto.request.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Writing (POST, productId required) or editing (PUT, productId ignored) a review. The comment is trimmed by the
 * service, which then checks 10–2000 characters; imageUrls must be review photos this server stored
 * (POST /uploads/review-images), in display order.
 */
public record ReviewRequest(

        @Schema(description = "Product to review (POST only)", example = "1")
        Long productId,

        @NotNull(message = "Vui lòng chọn số sao")
        @Min(value = 1, message = "Số sao từ 1 đến 5")
        @Max(value = 5, message = "Số sao từ 1 đến 5")
        @Schema(example = "5")
        Integer rating,

        @NotBlank(message = "Vui lòng nhập nội dung đánh giá")
        @Size(max = 2000, message = "Nội dung đánh giá tối đa 2000 ký tự")
        @Schema(example = "Máy chạy mượt, pin dùng cả ngày, giao hàng nhanh.")
        String comment,

        @Size(max = 5, message = "Tối đa 5 ảnh cho một đánh giá")
        @Schema(description = "URLs from POST /uploads/review-images, at most 5")
        List<String> imageUrls
) {
}
