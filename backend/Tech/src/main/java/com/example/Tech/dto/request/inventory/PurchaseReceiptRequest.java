package com.example.Tech.dto.request.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Creates or replaces a draft receipt: the whole content is sent each time. */
public record PurchaseReceiptRequest(

        @NotNull(message = "Vui lòng chọn nhà cung cấp")
        Integer supplierId,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String note,

        @NotEmpty(message = "Phiếu nhập cần ít nhất một sản phẩm")
        @Size(max = 200, message = "Một phiếu nhập tối đa 200 dòng")
        List<@Valid Line> items
) {

    public record Line(

            @NotNull(message = "Vui lòng chọn phiên bản sản phẩm")
            Long variantId,

            @NotNull(message = "Vui lòng nhập số lượng")
            @Positive(message = "Số lượng nhập phải lớn hơn 0")
            @Max(value = 100000, message = "Mỗi dòng tối đa 100.000 sản phẩm")
            Integer quantity,

            @NotNull(message = "Vui lòng nhập giá nhập")
            @DecimalMin(value = "0", message = "Giá nhập không được âm")
            @DecimalMax(value = "1000000000", message = "Giá nhập quá lớn")
            BigDecimal unitCost
    ) {
    }
}
