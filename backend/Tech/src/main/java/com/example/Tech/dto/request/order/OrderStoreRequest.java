package com.example.Tech.dto.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** ADMIN moves a pending order to another store. */
public record OrderStoreRequest(

        @Schema(description = "Open store that will handle the order", example = "2")
        @NotNull(message = "Vui lòng chọn chi nhánh")
        Integer storeId
) {
}
