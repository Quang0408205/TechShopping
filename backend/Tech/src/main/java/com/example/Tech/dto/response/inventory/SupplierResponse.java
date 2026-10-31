package com.example.Tech.dto.response.inventory;

import java.time.LocalDateTime;

public record SupplierResponse(Integer id, String name, String phone, String email, String address, String taxCode,
                               String note, boolean active, LocalDateTime createdAt) {
}
