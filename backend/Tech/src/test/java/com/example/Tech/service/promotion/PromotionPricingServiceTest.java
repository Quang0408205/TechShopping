package com.example.Tech.service.promotion;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.promotion.DiscountType;
import com.example.Tech.entity.promotion.Promotion;
import com.example.Tech.entity.promotion.PromotionProduct;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.promotion.PromotionProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionPricingServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 12, 0);

    @Mock
    private PromotionProductRepository promotionProductRepository;

    private PromotionPricingService pricingService;

    private Product product;

    @BeforeEach
    void setUp() {
        pricingService = new PromotionPricingService(promotionProductRepository);
        product = product(1L);
    }

    @Test
    void resolve_noPromotionNoManualDiscount_returnsThePriceUnchanged() {
        when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of());
        ProductVariant variant = variant(product, "1000000", null);

        EffectivePrice result = pricingService.resolve(variant, NOW);

        assertThat(result.unitPrice()).isEqualByComparingTo("1000000");
        assertThat(result.hasDiscount()).isFalse();
        assertThat(result.activePromotionName()).isNull();
    }

    @Test
    void resolve_noPromotion_manualDiscountLower_keepsOldBehavior() {
        when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of());
        ProductVariant variant = variant(product, "1000000", "900000");

        EffectivePrice result = pricingService.resolve(variant, NOW);

        assertThat(result.unitPrice()).isEqualByComparingTo("900000");
        assertThat(result.activePromotionName()).isNull();
    }

    @Test
    void resolve_percentagePromotion_noManualDiscount_appliesPercentageOff() {
        Promotion promotion = promotion("Sale 10%", DiscountType.PERCENTAGE, "10", null);
        PromotionProduct pp = new PromotionProduct(promotion, product, null, null);
        when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of(pp));
        ProductVariant variant = variant(product, "1000000", null);

        EffectivePrice result = pricingService.resolve(variant, NOW);

        assertThat(result.unitPrice()).isEqualByComparingTo("900000");
        assertThat(result.activePromotionName()).isEqualTo("Sale 10%");
    }

    @Test
    void resolve_percentagePromotion_overMaxDiscountAmount_isCappedAtTheCeiling() {
        Promotion promotion = promotion("Sale 50% capped", DiscountType.PERCENTAGE, "50", "1000000");
        PromotionProduct pp = new PromotionProduct(promotion, product, null, null);
        when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of(pp));
        ProductVariant variant = variant(product, "10000000", null);

        EffectivePrice result = pricingService.resolve(variant, NOW);

        // 50% of 10,000,000 would be 5,000,000 off; capped at 1,000,000 off
        assertThat(result.unitPrice()).isEqualByComparingTo("9000000");
        assertThat(result.activePromotionName()).isEqualTo("Sale 50% capped");
    }

    @Test
    void resolve_fixedAmountPromotion_subtractsTheFlatAmount() {
        Promotion promotion = promotion("Giảm 200k", DiscountType.FIXED_AMOUNT, "200000", null);
        PromotionProduct pp = new PromotionProduct(promotion, product, null, null);
        when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of(pp));
        ProductVariant variant = variant(product, "1000000", null);

        EffectivePrice result = pricingService.resolve(variant, NOW);

        assertThat(result.unitPrice()).isEqualByComparingTo("800000");
    }

    @Test
    void resolve_perProductOverride_beatsTheCampaignDefault() {
        Promotion promotion = promotion("Sale mặc định 10%", DiscountType.PERCENTAGE, "10", null);
        PromotionProduct pp = new PromotionProduct(promotion, product, DiscountType.PERCENTAGE, new BigDecimal("30"));
        when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of(pp));
        ProductVariant variant = variant(product, "1000000", null);

        EffectivePrice result = pricingService.resolve(variant, NOW);

        // override 30%, not the campaign's default 10%
        assertThat(result.unitPrice()).isEqualByComparingTo("700000");
    }

    @Test
    void resolve_bothManualDiscountAndPromotionActive_takesTheLowerPrice() {
        Promotion promotion = promotion("Sale 10%", DiscountType.PERCENTAGE, "10", null);
        PromotionProduct pp = new PromotionProduct(promotion, product, null, null);
        when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of(pp));
        // manual discount price (850000) beats the promotion price (900000)
        ProductVariant variant = variant(product, "1000000", "850000");

        EffectivePrice result = pricingService.resolve(variant, NOW);

        assertThat(result.unitPrice()).isEqualByComparingTo("850000");
        assertThat(result.activePromotionName()).isNull();
    }

    @Test
    void resolve_promotionBeatsManualDiscount_activePromotionNameIsSet() {
        Promotion promotion = promotion("Sale 50%", DiscountType.PERCENTAGE, "50", null);
        PromotionProduct pp = new PromotionProduct(promotion, product, null, null);
        when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of(pp));
        // promotion price (500000) beats the manual discount price (900000)
        ProductVariant variant = variant(product, "1000000", "900000");

        EffectivePrice result = pricingService.resolve(variant, NOW);

        assertThat(result.unitPrice()).isEqualByComparingTo("500000");
        assertThat(result.activePromotionName()).isEqualTo("Sale 50%");
    }

    @Test
    void resolve_fixedAmountAtOrAboveThePrice_isNotApplied_variantKeepsItsPrice() {
        Promotion promotion = promotion("Giảm 500k", DiscountType.FIXED_AMOUNT, "500000", null);
        PromotionProduct pp = new PromotionProduct(promotion, product, null, null);
        when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of(pp));
        ProductVariant cheap = variant(product, "300000", "280000");

        EffectivePrice result = pricingService.resolve(cheap, NOW);

        assertThat(result.unitPrice()).isEqualByComparingTo("280000");
        assertThat(result.activePromotionName()).isNull();
    }

    @Test
    void resolve_hundredPercent_isNotApplied() {
        Promotion promotion = promotion("Miễn phí", DiscountType.PERCENTAGE, "100", null);
        PromotionProduct pp = new PromotionProduct(promotion, product, null, null);
        when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of(pp));

        EffectivePrice result = pricingService.resolve(variant(product, "1000000", null), NOW);

        assertThat(result.unitPrice()).isEqualByComparingTo("1000000");
    }

    @Test
    void resolveProducts_aPageOfProducts_oneQuery_promotionOnlyOnItsProduct() {
        Product other = product(2L);
        other.setBasePrice(new BigDecimal("2000000"));
        other.setDiscountPrice(new BigDecimal("1800000"));
        product.setBasePrice(new BigDecimal("1000000"));
        PromotionProduct pp = new PromotionProduct(promotion("Sale 20%", DiscountType.PERCENTAGE, "20", null), product, null, null);
        when(promotionProductRepository.findActiveForProducts(List.of(1L, 2L), NOW)).thenReturn(List.of(pp));

        Map<Long, EffectivePrice> prices = pricingService.resolveProducts(List.of(product, other), NOW);

        assertThat(prices.get(1L).unitPrice()).isEqualByComparingTo("800000");
        assertThat(prices.get(1L).activePromotionName()).isEqualTo("Sale 20%");
        assertThat(prices.get(2L).unitPrice()).isEqualByComparingTo("1800000");
        assertThat(prices.get(2L).activePromotionName()).isNull();
        verify(promotionProductRepository, times(1)).findActiveForProducts(any(), any());
    }

    @Test
    void resolveVariants_variantsOfOneProduct_eachDiscountedOnItsOwnPrice() {
        PromotionProduct pp = new PromotionProduct(promotion("Sale 10%", DiscountType.PERCENTAGE, "10", null), product, null, null);
        when(promotionProductRepository.findActiveForProducts(List.of(1L), NOW)).thenReturn(List.of(pp));
        ProductVariant small = variant(product, "1000000", null);
        ProductVariant large = variant(product, "3000000", null);
        large.setId(2L);

        Map<Long, EffectivePrice> prices = pricingService.resolveVariants(List.of(small, large), NOW);

        assertThat(prices.get(1L).unitPrice()).isEqualByComparingTo("900000");
        assertThat(prices.get(2L).unitPrice()).isEqualByComparingTo("2700000");
    }

    private static Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        Category category = new Category();
        category.setId(1);
        product.setCategory(category);
        product.setName("Sản phẩm test khuyến mãi");
        product.setSlug("test-promo-pricing-" + id);
        product.setBasePrice(BigDecimal.TEN);
        return product;
    }

    private static ProductVariant variant(Product product, String price, String discountPrice) {
        ProductVariant variant = new ProductVariant();
        variant.setId(1L);
        variant.setProduct(product);
        variant.setVariantName("Mặc định");
        variant.setPrice(new BigDecimal(price));
        variant.setDiscountPrice(discountPrice == null ? null : new BigDecimal(discountPrice));
        return variant;
    }

    private static Promotion promotion(String name, DiscountType type, String value, String maxDiscountAmount) {
        Promotion promotion = new Promotion();
        promotion.setId(1L);
        promotion.setName(name);
        promotion.setDiscountType(type);
        promotion.setDiscountValue(new BigDecimal(value));
        promotion.setMaxDiscountAmount(maxDiscountAmount == null ? null : new BigDecimal(maxDiscountAmount));
        promotion.setStartDate(NOW.minusDays(1));
        promotion.setEndDate(NOW.plusDays(1));
        promotion.setActive(true);
        User admin = new User();
        admin.setId(1L);
        promotion.setCreatedBy(admin);
        return promotion;
    }
}
