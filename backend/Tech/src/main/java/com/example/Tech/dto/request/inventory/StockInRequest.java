package com.example.Tech.dto.request.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Nhập kho: adds a quantity of one variant to a store at once (no draft / multi-line receipt).
 */
public record StockInRequest(

        @NotNull(message = "Vui lòng chọn phiên bản sản phẩm")
        @Schema(example = "120")
        Long variantId,

        @NotNull(message = "Vui lòng nhập số lượng")
        @Positive(message = "Số lượng nhập phải lớn hơn 0")
        @Max(value = 100000, message = "Mỗi lần nhập tối đa 100.000 sản phẩm")
        @Schema(example = "20")
        Integer quantity,

        @Size(max = 150, message = "Tên nhà cung cấp tối đa 150 ký tự")
        @Schema(description = "Free text, optional", example = "Công ty Phân phối ABC")
        String supplierName,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        @Schema(example = "Lô hàng tháng 10")
        String note
) {
}
