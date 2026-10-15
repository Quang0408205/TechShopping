package com.example.Tech.dto.request.aftersales;

import com.example.Tech.entity.aftersales.ReturnReasonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReturnRequestCreateRequest(
        @NotNull(message = "Vui lòng chọn đơn hàng")
        @Schema(example = "1")
        Long orderId,

        @NotNull(message = "Vui lòng chọn lý do trả hàng")
        @Schema(example = "DEFECTIVE")
        ReturnReasonType reasonType,

        @NotBlank(message = "Vui lòng mô tả lý do trả hàng")
        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        @Schema(example = "Màn hình có điểm chết ngay khi mở hộp.")
        String reason,

        @NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm để trả")
        @Valid
        List<Item> items,

        @Size(max = 5, message = "Tối đa 5 ảnh cho một yêu cầu")
        List<String> imageUrls
) {

    public record Item(
            @NotNull(message = "Vui lòng chọn sản phẩm")
            Long orderItemId,

            @NotNull(message = "Vui lòng nhập số lượng")
            @Min(value = 1, message = "Số lượng trả ít nhất là 1")
            Integer quantity
    ) {
    }
}
