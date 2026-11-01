package com.example.Tech.dto.request.question;

import jakarta.validation.constraints.NotNull;

public record HideQuestionRequest(

        @NotNull(message = "Thiếu trạng thái ẩn")
        Boolean hidden
) {
}
