package com.example.Tech.dto.request.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Creates a draft transfer, or replaces the content of one ({@code fromStoreId} is ignored on update). */
public record StockTransferRequest(

        @NotNull(message = "Vui lòng chọn chi nhánh gửi")
        Integer fromStoreId,

        @NotNull(message = "Vui lòng chọn chi nhánh nhận")
        Integer toStoreId,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String note,

        @NotEmpty(message = "Phiếu chuyển cần ít nhất một sản phẩm")
        @Size(max = 200, message = "Một phiếu chuyển tối đa 200 dòng")
        List<@Valid Line> items
) {

    public record Line(

            @NotNull(message = "Vui lòng chọn phiên bản sản phẩm")
            Long variantId,

            @NotNull(message = "Vui lòng nhập số lượng")
            @Positive(message = "Số lượng chuyển phải lớn hơn 0")
            @Max(value = 100000, message = "Mỗi dòng tối đa 100.000 sản phẩm")
            Integer quantity
    ) {
    }
}
