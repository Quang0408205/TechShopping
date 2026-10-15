package com.example.Tech.dto.request.aftersales;

import com.example.Tech.entity.aftersales.ServiceRequestType;
import io.swagger.v3.oas.annotations.media.Schema;

public record ServiceRequestSearchRequest(
        ServiceRequestType type,

        @Schema(description = "Status text, e.g. PENDING, PROCESSING, REFUNDED", example = "PENDING")
        String status
) {
}
