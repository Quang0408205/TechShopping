package com.example.Tech.dto.response.audit;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id, Long userId, String username, String fullName, String role, String method,
                               String path, String actionLabel, String entityType, String entityLabel, String entityId,
                               int status, String ip, String requestData, LocalDateTime createdAt) {
}
