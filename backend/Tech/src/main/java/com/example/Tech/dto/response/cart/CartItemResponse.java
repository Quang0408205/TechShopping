package com.example.Tech.dto.response.cart;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CartItemResponse(
        Long cartItemId,
        Long variantId,
        Long productId,
        String productName,
        String productSlug,
        String variantName,
        @Schema(description = "Primary image of the product, else its first image; null when it has none")
        String imageUrl,
        @Schema(description = "Current price of the variant: the discount price when it is lower, else the price")
        BigDecimal unitPrice,
        @Schema(description = "The price before discount; null when there is no discount")
        BigDecimal originalPrice,
        Integer quantity,
        @Schema(description = "unitPrice × quantity")
        BigDecimal lineTotal,
        @Schema(description = "false when the product was deleted, hidden or has no price, or when the open stores "
                + "hold fewer units than quantity (plan v3); such lines are not counted in the subtotal")
        boolean available,
        @Schema(description = "Units of this variant in all open stores (0 = out of stock)")
        Integer availableQuantity,
        LocalDateTime addedAt,
        @Schema(description = "Plan v5 GĐ10: combo discount on this line (0 when none)")
        BigDecimal comboDiscount,
        @Schema(description = "Names of the combos that discount this line")
        List<String> comboNames
) {

    public CartItemResponse withCombo(BigDecimal discount, List<String> names) {
        return new CartItemResponse(cartItemId, variantId, productId, productName, productSlug, variantName, imageUrl,
                unitPrice, originalPrice, quantity, lineTotal, available, availableQuantity, addedAt, discount, names);
    }
}
