package com.example.Tech.dto.request.order;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ShippingFeeRuleRequest(

        @NotBlank(message = "Vui lòng nhập tên tỉnh / thành")
        @Size(max = 100, message = "Tên tỉnh / thành tối đa 100 ký tự")
        String provinceName,

        @NotNull(message = "Vui lòng nhập phí giao hàng")
        @DecimalMin(value = "0", message = "Phí giao hàng không được âm")
        @DecimalMax(value = "10000000", message = "Phí giao hàng tối đa 10.000.000đ")
        BigDecimal fee
) {
}
