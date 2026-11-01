package com.example.Tech.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerQuestionRequest(

        @NotBlank(message = "Vui lòng nhập câu trả lời")
        @Size(max = 1000, message = "Câu trả lời tối đa 1000 ký tự")
        String answer
) {
}
