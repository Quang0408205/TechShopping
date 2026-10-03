package com.example.Tech.mapper.cart;

import com.example.Tech.dto.response.cart.CartItemResponse;
import com.example.Tech.dto.response.cart.CartResponse;
import com.example.Tech.entity.cart.Cart;
import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.service.order.ShippingPolicy;
import com.example.Tech.service.promotion.PromotionPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds the cart response. Prices always come from the variant at read time (cart_items has no price
 * column); a line is available only while its product is not deleted, not hidden and has a price.
 * The shipping fee is the one checkout charges (ShippingPolicy). Checkout (OrderServiceImpl) uses the same
 * unitPrice / isPurchasable rules.
 */
@Component
@RequiredArgsConstructor
public class CartMapper {

    private final PromotionPricingService promotionPricingService;

    private final Clock clock;

    public CartResponse toResponse(Cart cart, List<CartItemResponse> items) {
        int totalQuantity = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal subtotal = items.stream()
                .filter(CartItemResponse::available)
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasUnavailableItems = items.stream().anyMatch(item -> !item.available());
        BigDecimal shippingFee = ShippingPolicy.feeFor(subtotal);
        return new CartResponse(items, totalQuantity, subtotal, shippingFee, subtotal.add(shippingFee),
                hasUnavailableItems, cart.getUpdatedAt());
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
                isPurchasable(product, unitPrice),
                item.getAddedAt());
    }

    /** The lower of the manual discount price and any currently active promotion for the variant's product. */
    public BigDecimal unitPrice(ProductVariant variant) {
        return promotionPricingService.resolve(variant, LocalDateTime.now(clock)).unitPrice();
    }

    /** Not soft-deleted, not hidden (is_active NULL counts as active) and a price above 0. */
    public boolean isPurchasable(ProductVariant variant) {
        return isPurchasable(variant.getProduct(), unitPrice(variant));
    }

    private boolean isPurchasable(Product product, BigDecimal unitPrice) {
        return product.getDeletedAt() == null
                && !Boolean.FALSE.equals(product.getActive())
                && unitPrice.signum() > 0;
    }
}
