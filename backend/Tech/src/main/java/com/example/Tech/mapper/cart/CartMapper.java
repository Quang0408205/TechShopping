package com.example.Tech.mapper.cart;

import com.example.Tech.dto.response.cart.CartItemResponse;
import com.example.Tech.dto.response.cart.CartResponse;
import com.example.Tech.entity.cart.Cart;
import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Builds the cart response. Prices always come from the variant at read time (cart_items has no price
 * column); a line is available only while its product is not deleted, not hidden and has a price.
 */
@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart, List<CartItemResponse> items) {
        int totalQuantity = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal subtotal = items.stream()
                .filter(CartItemResponse::available)
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasUnavailableItems = items.stream().anyMatch(item -> !item.available());
        return new CartResponse(items, totalQuantity, subtotal, hasUnavailableItems, cart.getUpdatedAt());
    }

    public CartItemResponse toItemResponse(CartItem item, String imageUrl) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();
        BigDecimal unitPrice = unitPrice(variant);
        BigDecimal originalPrice = unitPrice.compareTo(variant.getPrice()) < 0 ? variant.getPrice() : null;
        return new CartItemResponse(
                item.getId(),
                variant.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                variant.getVariantName(),
                imageUrl,
                unitPrice,
                originalPrice,
                item.getQuantity(),
                unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())),
                isPurchasable(variant),
                item.getAddedAt());
    }

    /** The discount price when it is set and lower than the price, else the price. */
    public BigDecimal unitPrice(ProductVariant variant) {
        BigDecimal discount = variant.getDiscountPrice();
        return discount != null && discount.compareTo(variant.getPrice()) < 0 ? discount : variant.getPrice();
    }

    /** Not soft-deleted, not hidden (is_active NULL counts as active) and a price above 0. */
    public boolean isPurchasable(ProductVariant variant) {
        Product product = variant.getProduct();
        return product.getDeletedAt() == null
                && !Boolean.FALSE.equals(product.getActive())
                && unitPrice(variant).signum() > 0;
    }
}
