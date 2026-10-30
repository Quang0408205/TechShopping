package com.example.Tech.dto.request.combo;

import com.example.Tech.entity.combo.ComboDiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Create / replace a combo (ADMIN): one main product and 1–10 add-ons, each with its own discount. */
public record ComboRequest(

        @NotBlank(message = "Vui lòng nhập tên combo")
        @Size(max = 150, message = "Tên combo tối đa 150 ký tự")
        String name,

        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String description,

        @NotNull(message = "Vui lòng chọn sản phẩm chính")
        Long mainProductId,

        @NotNull(message = "Vui lòng chọn thời gian bắt đầu")
        LocalDateTime startsAt,

        @NotNull(message = "Vui lòng chọn thời gian kết thúc")
        LocalDateTime endsAt,

        Boolean active,

        @NotEmpty(message = "Chọn ít nhất 1 món mua kèm")
        @Size(max = 10, message = "Mỗi combo tối đa 10 món mua kèm")
        @Valid
        List<Item> items
) {

    public record Item(
            @NotNull(message = "Thiếu sản phẩm mua kèm")
            Long productId,

            @NotNull(message = "Vui lòng chọn kiểu giảm (PERCENT hoặc AMOUNT)")
            ComboDiscountType discountType,

            @NotNull(message = "Vui lòng nhập mức giảm")
            @DecimalMin(value = "0.01", message = "Mức giảm phải lớn hơn 0")
            BigDecimal discountValue
    ) {
    }
}
