package com.example.Tech.dto.request.content;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BannerRequest(

        @NotBlank(message = "Vui lòng nhập tiêu đề")
        @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự")
        String title,

        @Size(max = 300, message = "Mô tả tối đa 300 ký tự")
        String subtitle,

        @NotBlank(message = "Vui lòng chọn ảnh banner")
        @Size(max = 500, message = "Địa chỉ ảnh tối đa 500 ký tự")
        String imageUrl,

        @Size(max = 500, message = "Liên kết tối đa 500 ký tự")
        String linkUrl,

        @Min(value = 0, message = "Thứ tự không được âm")
        @Max(value = 9999, message = "Thứ tự tối đa 9999")
        Integer displayOrder,

        Boolean active,

        LocalDateTime startsAt,

        LocalDateTime endsAt
) {
}
