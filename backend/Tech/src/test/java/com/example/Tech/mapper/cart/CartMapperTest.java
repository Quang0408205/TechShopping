package com.example.Tech.mapper.cart;

import com.example.Tech.dto.response.cart.CartItemResponse;
import com.example.Tech.entity.cart.Cart;
import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.User;
import com.example.Tech.service.promotion.EffectivePrice;
import com.example.Tech.service.promotion.PromotionPricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CartMapper now delegates its unitPrice rule to PromotionPricingService (mocked here); these tests check
 * the wiring (one resolve() call per line, not two) and the available/original-price fields built from it.
 */
@ExtendWith(MockitoExtension.class)
class CartMapperTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-03T05:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private PromotionPricingService promotionPricingService;

    private CartMapper cartMapper;

    private Product product;
    private ProductVariant variant;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartMapper = new CartMapper(promotionPricingService, CLOCK);

        Category category = new Category();
        category.setId(1);
        product = new Product();
        product.setId(1L);
        product.setCategory(category);
        product.setName("Điện thoại Test Giỏ Hàng");
        product.setSlug("test-cart-mapper");

        variant = new ProductVariant();
        variant.setId(10L);
        variant.setProduct(product);
        variant.setVariantName("Mặc định");
        variant.setPrice(new BigDecimal("1000000"));

        User owner = new User();
        owner.setId(7L);
        cart = new Cart(owner);
        cart.setId(3L);
    }

    @Test
    void toItemResponse_discountedPrice_setsOriginalPriceAndCallsResolveOnce() {
        when(promotionPricingService.resolve(eq(variant), any()))
                .thenReturn(new EffectivePrice(new BigDecimal("800000"), new BigDecimal("1000000"), "Sale 20%"));
        CartItem item = new CartItem(cart, variant, 2);

        CartItemResponse response = cartMapper.toItemResponse(item, "/img.png");

        assertThat(response.unitPrice()).isEqualByComparingTo("800000");
        assertThat(response.originalPrice()).isEqualByComparingTo("1000000");
        assertThat(response.lineTotal()).isEqualByComparingTo("1600000");
        assertThat(response.available()).isTrue();
        verify(promotionPricingService, times(1)).resolve(eq(variant), any());
    }

    @Test
    void toItemResponse_noDiscount_originalPriceIsNull() {
        when(promotionPricingService.resolve(eq(variant), any()))
                .thenReturn(new EffectivePrice(new BigDecimal("1000000"), new BigDecimal("1000000"), null));
        CartItem item = new CartItem(cart, variant, 1);

        CartItemResponse response = cartMapper.toItemResponse(item, null);

        assertThat(response.originalPrice()).isNull();
    }

    @Test
    void toItemResponse_zeroEffectivePrice_isNotPurchasable() {
        when(promotionPricingService.resolve(eq(variant), any()))
                .thenReturn(new EffectivePrice(BigDecimal.ZERO, new BigDecimal("1000000"), "Sale 100%"));
        CartItem item = new CartItem(cart, variant, 1);

        CartItemResponse response = cartMapper.toItemResponse(item, null);

        assertThat(response.available()).isFalse();
        verify(promotionPricingService, times(1)).resolve(eq(variant), any());
    }

    @Test
    void toItemResponse_softDeletedProduct_isNotPurchasableEvenWithAPositivePrice() {
        product.setDeletedAt(java.time.LocalDateTime.now());
        when(promotionPricingService.resolve(eq(variant), any()))
                .thenReturn(new EffectivePrice(new BigDecimal("1000000"), new BigDecimal("1000000"), null));
        CartItem item = new CartItem(cart, variant, 1);

        CartItemResponse response = cartMapper.toItemResponse(item, null);

        assertThat(response.available()).isFalse();
    }

    @Test
    void isPurchasable_delegatesToResolveExactlyOnce() {
        when(promotionPricingService.resolve(eq(variant), any()))
                .thenReturn(new EffectivePrice(new BigDecimal("1000000"), new BigDecimal("1000000"), null));

        boolean purchasable = cartMapper.isPurchasable(variant);

        assertThat(purchasable).isTrue();
        verify(promotionPricingService, times(1)).resolve(eq(variant), any());
    }
}
