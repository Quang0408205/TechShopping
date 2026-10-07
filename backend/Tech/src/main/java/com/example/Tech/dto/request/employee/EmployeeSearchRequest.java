package com.example.Tech.dto.request.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Optional filters for the admin employee list; all conditions are combined with AND.
 */
public record EmployeeSearchRequest(

        @Schema(description = "Part of the full name, email, username, phone or employee code", example = "nguyễn")
        @Size(max = 255, message = "Từ khóa tối đa 255 ký tự")
        String keyword,

        @Schema(description = "Store of the current assignment", example = "1")
        Integer storeId,

        @Schema(description = "true = đang làm, false = đã nghỉ", example = "true")
        Boolean active
) {
}
