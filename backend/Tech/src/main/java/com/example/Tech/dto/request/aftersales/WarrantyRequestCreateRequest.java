package com.example.Tech.dto.request.aftersales;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WarrantyRequestCreateRequest(
        @NotNull(message = "Vui lòng chọn sản phẩm")
        @Schema(description = "Line of a delivered order of mine", example = "1")
        Long orderItemId,

        @NotBlank(message = "Vui lòng mô tả lỗi của sản phẩm")
        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        @Schema(example = "Máy tự tắt nguồn khi đang sạc pin.")
        String description,

        @Size(max = 5, message = "Tối đa 5 ảnh cho một yêu cầu")
        @Schema(description = "URLs from POST /uploads/service-images, at most 5")
        List<String> imageUrls
) {
}
