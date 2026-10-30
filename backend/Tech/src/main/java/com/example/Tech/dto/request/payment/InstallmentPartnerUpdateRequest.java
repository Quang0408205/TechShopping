package com.example.Tech.dto.request.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** ADMIN saves the conditions and the fee table of one installment partner. */
public record InstallmentPartnerUpdateRequest(

        @NotNull(message = "Vui lòng chọn trạng thái hoạt động")
        Boolean active,

        @NotNull(message = "Vui lòng nhập giá trị đơn tối thiểu")
        @DecimalMin(value = "0", message = "Giá trị đơn tối thiểu không được âm")
        @DecimalMax(value = "999999999999", message = "Giá trị đơn tối thiểu quá lớn")
        @Digits(integer = 13, fraction = 0, message = "Giá trị đơn là số đồng nguyên")
        BigDecimal minOrderTotal,

        @DecimalMin(value = "0", message = "Giá trị đơn tối đa không được âm")
        @DecimalMax(value = "999999999999", message = "Giá trị đơn tối đa quá lớn")
        @Digits(integer = 13, fraction = 0, message = "Giá trị đơn là số đồng nguyên")
        BigDecimal maxOrderTotal,

        @NotEmpty(message = "Đối tác cần ít nhất một kỳ hạn")
        @Size(max = 12, message = "Tối đa 12 kỳ hạn")
        List<@Valid Rate> rates
) {

    public record Rate(
            @NotNull(message = "Vui lòng nhập kỳ hạn")
            @Min(value = 1, message = "Kỳ hạn từ 1 đến 36 tháng")
            @Max(value = 36, message = "Kỳ hạn từ 1 đến 36 tháng")
            Integer numMonths,
            @NotNull(message = "Vui lòng nhập phí")
            @DecimalMin(value = "0", message = "Phí từ 0 đến 100%")
            @DecimalMax(value = "100", message = "Phí từ 0 đến 100%")
            @Digits(integer = 3, fraction = 2, message = "Phí tối đa 2 chữ số thập phân")
            BigDecimal feePercent
    ) {
    }
}
