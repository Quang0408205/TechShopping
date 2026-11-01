package com.example.Tech.dto.response.notification;

import java.time.LocalDateTime;

public record StaffNotificationResponse(
        Long id,
        String kind,
        String title,
        String message,
        String link,
        Integer storeId,
        String storeName,
        boolean read,
        LocalDateTime createdAt
) {
}
