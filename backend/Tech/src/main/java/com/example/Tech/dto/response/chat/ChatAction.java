package com.example.Tech.dto.response.chat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An action the assistant proposes (shown as a "Xác nhận" button). It is never executed by the chatbot: the
 * browser runs it with the customer's normal API call only when the customer presses the button.
 */
public record ChatAction(

        @Schema(description = "ADD_TO_CART (POST /cart/items) or CANCEL_ORDER (POST /orders/{orderId}/cancel)")
        String type,

        @Schema(example = "Thêm iPhone 17e 256GB (256 GB - Hồng nhạt) × 1 vào giỏ")
        String label,

        Long variantId,

        Integer quantity,

        Long orderId
) {
}
