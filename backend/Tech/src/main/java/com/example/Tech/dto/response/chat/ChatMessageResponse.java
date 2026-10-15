package com.example.Tech.dto.response.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ChatMessageResponse(

        @Schema(description = "USER or BOT", example = "USER")
        String sender,

        String content,

        LocalDateTime sentAt
) {
}
