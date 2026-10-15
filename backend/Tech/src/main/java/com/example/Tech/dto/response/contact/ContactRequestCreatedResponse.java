package com.example.Tech.dto.response.contact;

import java.time.LocalDateTime;

/** What the sender gets back: only the reference number and time, nothing about other requests. */
public record ContactRequestCreatedResponse(Long id, LocalDateTime createdAt) {
}
