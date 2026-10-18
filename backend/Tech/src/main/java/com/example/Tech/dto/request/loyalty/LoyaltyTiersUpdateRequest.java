package com.example.Tech.dto.request.loyalty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** ADMIN saves all 4 tiers at once (thresholds must rise with the tier). */
public record LoyaltyTiersUpdateRequest(
        @NotEmpty(message = "Vui lòng gửi đủ các hạng")
        @Size(max = 10)
        List<@Valid Tier> tiers
) {

    public record Tier(
            @NotBlank(message = "Thiếu mã hạng")
            String code,
            @NotNull(message = "Vui lòng nhập ngưỡng chi tiêu")
            @DecimalMin(value = "0", message = "Ngưỡng chi tiêu không được âm")
            @DecimalMax(value = "999999999999", message = "Ngưỡng chi tiêu quá lớn")
            @Digits(integer = 13, fraction = 0, message = "Ngưỡng chi tiêu là số đồng nguyên")
            BigDecimal minSpent,
            @NotNull(message = "Vui lòng nhập hệ số nhân điểm")
            @DecimalMin(value = "1", message = "Hệ số nhân từ 1 đến 5")
            @DecimalMax(value = "5", message = "Hệ số nhân từ 1 đến 5")
            @Digits(integer = 1, fraction = 2, message = "Hệ số nhân tối đa 2 chữ số thập phân")
            BigDecimal pointMultiplier
    ) {
    }
}
