package com.example.Tech.dto.request.aftersales;

import com.example.Tech.entity.aftersales.ServiceRequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AdminServiceRequestSearchRequest(
        ServiceRequestType type,

        @Schema(description = "Status text, e.g. PENDING, RECEIVED, REFUNDED", example = "PENDING")
        String status,

        @Schema(description = "ADMIN only; a STAFF member always sees their own store", example = "1")
        Integer storeId,

        @Size(max = 255, message = "Từ khóa tối đa 255 ký tự")
        @Schema(description = "Request code (BH000012), order code (DH00000042), customer name / email / username / phone",
                example = "DH00000042")
        String keyword,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate
) {
}
