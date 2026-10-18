package com.example.Tech.dto.request.voucher;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Create / replace a voucher (ADMIN, plan v5 GĐ4). */
public record VoucherRequest(

        @Schema(description = "3–30 letters / digits / _ -, stored upper case", example = "POY10")
        @NotBlank(message = "Vui lòng nhập mã voucher")
        @Size(max = 30, message = "Mã voucher tối đa 30 ký tự")
        String code,

        @NotBlank(message = "Vui lòng nhập tên voucher")
        @Size(max = 150, message = "Tên voucher tối đa 150 ký tự")
        String name,

        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String description,

        @Schema(example = "10")
        @NotNull(message = "Vui lòng nhập phần trăm giảm")
        @DecimalMin(value = "0.01", message = "Phần trăm giảm phải lớn hơn 0")
        @DecimalMax(value = "100", message = "Phần trăm giảm tối đa 100")
        BigDecimal discountPercent,

        @Schema(description = "Cap on the discount; empty = no cap", example = "500000")
        @DecimalMin(value = "1", message = "Giảm tối đa phải lớn hơn 0")
        BigDecimal maxDiscount,

        @Schema(description = "Goods in scope must reach this amount; empty = 0", example = "5000000")
        @DecimalMin(value = "0", message = "Đơn tối thiểu không được âm")
        BigDecimal minOrderAmount,

        @NotNull(message = "Vui lòng chọn thời gian bắt đầu")
        LocalDateTime startsAt,

        @NotNull(message = "Vui lòng chọn thời gian kết thúc")
        LocalDateTime endsAt,

        @Schema(description = "Total uses; empty = unlimited")
        @Min(value = 1, message = "Tổng lượt dùng phải từ 1 trở lên")
        Integer usageLimit,

        @Schema(description = "Uses per customer; empty = 1")
        @Min(value = 1, message = "Lượt mỗi khách phải từ 1 trở lên")
        Integer perCustomerLimit,

        @Schema(description = "true / empty = every category; false = only categoryIds")
        Boolean allCategories,

        List<Integer> categoryIds,

        @Schema(description = "Empty = true")
        Boolean active
) {
}
