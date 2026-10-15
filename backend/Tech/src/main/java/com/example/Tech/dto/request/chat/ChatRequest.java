package com.example.Tech.dto.request.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(

        @Size(max = 64, message = "Session token is too long")
        @Schema(description = "Token returned by the previous answer; empty to start a new conversation")
        String sessionToken,

        @NotBlank(message = "Message is required")
        @Size(max = 1000, message = "Message must be at most 1000 characters")
        @Schema(example = "Điện thoại chụp ảnh đẹp dưới 10 triệu?")
        String message
) {
}
