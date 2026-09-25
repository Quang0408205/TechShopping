package com.example.Tech.dto.response.user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerProfileResponse(
        Long customerId,
        LocalDate dateOfBirth,
        String gender,
        String address,
        String city,
        String district,
        String ward,
        String postalCode,
        String defaultShippingAddress,
        Integer loyaltyPoints,
        BigDecimal totalSpent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
