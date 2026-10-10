package com.example.Tech.dto.request.aftersales;

import com.example.Tech.entity.aftersales.MaintenanceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MaintenanceRequestCreateRequest(
        @NotNull(message = "Vui lòng chọn sản phẩm")
        @Schema(example = "1")
        Long orderItemId,

        @NotNull(message = "Vui lòng chọn loại bảo trì")
        @Schema(example = "CLEANING")
        MaintenanceType maintenanceType,

        @NotBlank(message = "Vui lòng mô tả yêu cầu bảo trì")
        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        @Schema(example = "Vệ sinh loa và cổng sạc.")
        String description,

        @Size(max = 5, message = "Tối đa 5 ảnh cho một yêu cầu")
        List<String> imageUrls
) {
}
