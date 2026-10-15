package com.example.Tech.service.promotion;

import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.promotion.DiscountType;
import com.example.Tech.entity.promotion.Promotion;
import com.example.Tech.entity.promotion.PromotionProduct;
import com.example.Tech.repository.promotion.PromotionProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes the price actually charged: the lower of (a) the manual discount_price column and (b) the discount
 * of the one promotion currently active for the product, if any (a product cannot be in two
 * simultaneously-active promotions, enforced at promotion create/update time). Used by the cart and checkout
 * (variant prices) and by the product / variant responses (effectivePrice), so they always agree.
 */
@Component
@RequiredArgsConstructor
public class PromotionPricingService {

    private final PromotionProductRepository promotionProductRepository;

    public EffectivePrice resolve(ProductVariant variant, LocalDateTime now) {
        Long productId = variant.getProduct().getId();
        return price(variant.getPrice(), variant.getDiscountPrice(),
                activeByProductId(List.of(productId), now).get(productId));
    }

    /** Several variants (e.g. of one product page), one query; keyed by variant id. */
    public Map<Long, EffectivePrice> resolveVariants(List<ProductVariant> variants, LocalDateTime now) {
        Map<Long, PromotionProduct> active = activeByProductId(
                variants.stream().map(variant -> variant.getProduct().getId()).distinct().toList(), now);
        Map<Long, EffectivePrice> prices = new HashMap<>();
        for (ProductVariant variant : variants) {
            prices.put(variant.getId(), price(variant.getPrice(), variant.getDiscountPrice(),
                    active.get(variant.getProduct().getId())));
        }
        return prices;
    }

    /** Product-level prices (basePrice / discountPrice) of a page of products, one query; keyed by product id. */
    public Map<Long, EffectivePrice> resolveProducts(List<Product> products, LocalDateTime now) {
        Map<Long, PromotionProduct> active = activeByProductId(products.stream().map(Product::getId).toList(), now);
        Map<Long, EffectivePrice> prices = new HashMap<>();
        for (Product product : products) {
            prices.put(product.getId(), price(product.getBasePrice(), product.getDiscountPrice(), active.get(product.getId())));
        }
        return prices;
    }

    private Map<Long, PromotionProduct> activeByProductId(List<Long> productIds, LocalDateTime now) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, PromotionProduct> active = new HashMap<>();
        for (PromotionProduct row : promotionProductRepository.findActiveForProducts(productIds, now)) {
            active.putIfAbsent(row.getId().getProductId(), row);
        }
        return active;
    }

    private static EffectivePrice price(BigDecimal listPrice, BigDecimal discountPrice, PromotionProduct promotion) {
        BigDecimal manualPrice = discountPrice != null && discountPrice.compareTo(listPrice) < 0 ? discountPrice : listPrice;
        if (promotion == null) {
            return new EffectivePrice(manualPrice, listPrice, null);
        }
        BigDecimal promoPrice = discountedPrice(listPrice, promotion);
        // a discount reaching the whole price (fixed amount above a cheap variant, or 100%) would make the
        // variant unpurchasable (price 0): the promotion is not applied to that price
        if (promoPrice.signum() > 0 && promoPrice.compareTo(manualPrice) < 0) {
            return new EffectivePrice(promoPrice, listPrice, promotion.getPromotion().getName());
        }
        return new EffectivePrice(manualPrice, listPrice, null);
    }

    private static BigDecimal discountedPrice(BigDecimal listPrice, PromotionProduct promotionProduct) {
        Promotion promotion = promotionProduct.getPromotion();
        DiscountType type = promotionProduct.hasOverride() ? promotionProduct.getDiscountType() : promotion.getDiscountType();
        BigDecimal value = promotionProduct.hasOverride() ? promotionProduct.getDiscountValue() : promotion.getDiscountValue();

        BigDecimal discountAmount = type == DiscountType.PERCENTAGE
                ? capPercentageDiscount(listPrice.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP),
                        promotion.getMaxDiscountAmount())
                : value;
        return listPrice.subtract(discountAmount);
    }

    private static BigDecimal capPercentageDiscount(BigDecimal discountAmount, BigDecimal maxDiscountAmount) {
        return maxDiscountAmount != null && discountAmount.compareTo(maxDiscountAmount) > 0
                ? maxDiscountAmount : discountAmount;
    }
}
