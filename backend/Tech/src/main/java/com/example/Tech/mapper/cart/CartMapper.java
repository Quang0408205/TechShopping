package com.example.Tech.mapper.cart;

import com.example.Tech.dto.response.cart.CartItemResponse;
import com.example.Tech.dto.response.cart.CartResponse;
import com.example.Tech.entity.cart.Cart;
import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.service.combo.ComboCalculator;
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
 * column); a line is available only while its product is not deleted, not hidden, has a price and the open
 * stores hold enough units (plan v3 GD E).
 * The shipping fee is the one checkout charges (ShippingPolicy). Checkout (OrderServiceImpl) uses the same
 * unitPrice / isPurchasable rules.
 */
@Component
@RequiredArgsConstructor
public class CartMapper {

    private final PromotionPricingService promotionPricingService;

    private final Clock clock;

    public CartResponse toResponse(Cart cart, List<CartItemResponse> items) {
        return toResponse(cart, items, null);
    }

    /** {@code combos}: the combo discounts of the purchasable lines (null = none); keys are cart line ids. */
    public CartResponse toResponse(Cart cart, List<CartItemResponse> items, ComboCalculator.Result combos) {
        int totalQuantity = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal subtotal = items.stream()
                .filter(CartItemResponse::available)
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasUnavailableItems = items.stream().anyMatch(item -> !item.available());
        BigDecimal shippingFee = ShippingPolicy.feeFor(subtotal);
        BigDecimal comboDiscount = combos == null ? BigDecimal.ZERO : combos.total();
        List<CartResponse.ComboApplied> applied = combos == null ? List.of() : combos.applied().stream()
                .map(a -> new CartResponse.ComboApplied(a.comboId(), a.name(), a.units(), a.discount())).toList();
        return new CartResponse(items, totalQuantity, subtotal, comboDiscount, applied, shippingFee,
                subtotal.subtract(comboDiscount).add(shippingFee), hasUnavailableItems, cart.getUpdatedAt());
    }

    /** availableUnits: the variant's units in all open stores (StockAvailability). */
    public CartItemResponse toItemResponse(CartItem item, String imageUrl, int availableUnits) {
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
                isPurchasable(product, unitPrice) && availableUnits >= item.getQuantity(),
                availableUnits,
                item.getAddedAt(),
                BigDecimal.ZERO,
                List.of());
    }

    /** The lower of the manual discount price and any currently active promotion for the variant's product. */
    public BigDecimal unitPrice(ProductVariant variant) {
        return promotionPricingService.resolve(variant, LocalDateTime.now(clock)).unitPrice();
    }

    /** Not soft-deleted, not hidden, its category not hidden (is_active NULL counts as active) and a price above 0. */
    public boolean isPurchasable(ProductVariant variant) {
        return isPurchasable(variant.getProduct(), unitPrice(variant));
    }

    private boolean isPurchasable(Product product, BigDecimal unitPrice) {
        return product.getDeletedAt() == null
                && !Boolean.FALSE.equals(product.getActive())
                && (product.getCategory() == null || !Boolean.FALSE.equals(product.getCategory().getActive()))
                && unitPrice.signum() > 0;
    }
}
