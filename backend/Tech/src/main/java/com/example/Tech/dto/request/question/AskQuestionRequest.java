package com.example.Tech.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AskQuestionRequest(

        @NotNull(message = "Vui lòng chọn sản phẩm")
        Long productId,

        @NotBlank(message = "Vui lòng nhập câu hỏi")
        @Size(min = 10, max = 500, message = "Câu hỏi dài từ 10 đến 500 ký tự")
        String question
) {
}
