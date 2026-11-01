package com.example.Tech.dto.request.stock;

import jakarta.validation.constraints.NotNull;

public record StockSubscribeRequest(

        @NotNull(message = "Vui lòng chọn phiên bản sản phẩm")
        Long variantId
) {
}
