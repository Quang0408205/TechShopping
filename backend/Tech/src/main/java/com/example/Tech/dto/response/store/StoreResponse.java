package com.example.Tech.dto.response.store;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StoreResponse(
        Integer id,
        String name,
        String address,
        String district,
        String city,
        String phone,
        String email,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
