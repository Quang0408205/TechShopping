package com.example.Tech.dto.request.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContentPageRequest(

        @NotBlank(message = "Vui lòng nhập tiêu đề trang")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        @NotBlank(message = "Vui lòng nhập nội dung trang")
        @Size(max = 100000, message = "Nội dung tối đa 100.000 ký tự")
        String body,

        Boolean published
) {
}
