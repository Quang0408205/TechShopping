package com.example.Tech.dto.request.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Hide (reason required, shown to the author) or show again (the reason is cleared) a review.
 */
public record ReviewVisibilityRequest(

        @NotNull(message = "Vui lòng chọn ẩn hay hiện đánh giá")
        @Schema(example = "true")
        Boolean hidden,

        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        @Schema(description = "Required when hiding", example = "Nội dung quảng cáo, không liên quan sản phẩm")
        String reason
) {
}
