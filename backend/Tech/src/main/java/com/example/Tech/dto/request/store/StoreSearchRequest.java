package com.example.Tech.dto.request.store;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Optional filters for the admin store list; all conditions are combined with AND.
 */
public record StoreSearchRequest(

        @Schema(description = "Part of the name, address, district, city or phone", example = "quận 1")
        @Size(max = 255, message = "Từ khóa tối đa 255 ký tự")
        String keyword,

        @Schema(description = "true = đang mở, false = tạm đóng", example = "true")
        Boolean active
) {
}
