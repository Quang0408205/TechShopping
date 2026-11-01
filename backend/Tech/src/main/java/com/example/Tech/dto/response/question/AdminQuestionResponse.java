package com.example.Tech.dto.response.question;

import java.time.LocalDateTime;

public record AdminQuestionResponse(
        Long id,
        Long productId,
        String productName,
        String question,
        String answer,
        String askerName,
        String askerEmail,
        String answeredByName,
        boolean hidden,
        LocalDateTime createdAt,
        LocalDateTime answeredAt
) {
}
