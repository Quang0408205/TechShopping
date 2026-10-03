package com.example.Tech.service.impl.cart;

import com.example.Tech.dto.request.cart.CartItemAddRequest;
import com.example.Tech.dto.request.cart.CartItemUpdateRequest;
import com.example.Tech.dto.response.cart.CartItemResponse;
import com.example.Tech.dto.response.cart.CartResponse;
import com.example.Tech.entity.cart.Cart;
import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.cart.CartMapper;
import com.example.Tech.repository.cart.CartItemRepository;
import com.example.Tech.repository.cart.CartRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.promotion.PromotionProductRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.service.cart.CartService;
import com.example.Tech.service.promotion.PromotionPricingService;
import com.example.Tech.service.user.CurrentUserLoader;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final Long CART_ID = 3L;
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 9, 28, 10, 0);

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductImageRepository imageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PromotionProductRepository promotionProductRepository;

    private CartServiceImpl cartService;

    private User user;
    private Cart cart;

    @BeforeEach
    void setUp() {
        lenient().when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of());
        CartMapper cartMapper = new CartMapper(new PromotionPricingService(promotionProductRepository), Clock.systemDefaultZone());
        cartService = new CartServiceImpl(cartRepository, cartItemRepository, variantRepository, imageRepository,
                new CurrentUserLoader(userRepository, userRoleRepository), cartMapper);
        user = new User();
        user.setId(USER_ID);
        cart = new Cart(user);
        cart.setId(CART_ID);
        cart.setUpdatedAt(UPDATED_AT);
    }

    private void stubUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    private static void assertError(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(code);
    }

    @Test
    void getCart_withoutCart_returnsEmptyCartAndCreatesNothing() {
        stubUser();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalQuantity()).isZero();
        assertThat(response.subtotal()).isEqualByComparingTo("0");
        assertThat(response.updatedAt()).isNull();
        verify(cartRepository, never()).insertIfMissing(anyLong());
    }

    @Test
    void getCart_pricesComeFromTheVariants_andUnavailableLinesAreNotCounted() {
        stubUser();
        Product phone = product(1L, "Điện thoại A");
        ProductVariant discounted = variant(10L, phone, "20000000", "18000000");
        Product hidden = product(2L, "Điện thoại B");
        hidden.setActive(false);
        ProductVariant hiddenVariant = variant(20L, hidden, "5000000", null);
        Product free = product(3L, "Sạc C");
        ProductVariant noPrice = variant(30L, free, "0", null);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllWithProductByCartId(CART_ID)).thenReturn(List.of(
                item(100L, discounted, 2), item(200L, hiddenVariant, 1), item(300L, noPrice, 4)));
        when(imageRepository.findAllByProductIdInBestFirst(List.of(1L, 2L, 3L))).thenReturn(List.of(
                image(phone, "https://cdn.example/a-primary.jpg"), image(phone, "https://cdn.example/a-2.jpg"),
                image(hidden, "https://cdn.example/b.jpg")));

        CartResponse response = cartService.getCart(USER_ID);

        CartItemResponse first = response.items().getFirst();
        assertThat(first.variantId()).isEqualTo(10L);
        assertThat(first.productName()).isEqualTo("Điện thoại A");
        assertThat(first.unitPrice()).isEqualByComparingTo("18000000");
        assertThat(first.originalPrice()).isEqualByComparingTo("20000000");
        assertThat(first.lineTotal()).isEqualByComparingTo("36000000");
        assertThat(first.imageUrl()).isEqualTo("https://cdn.example/a-primary.jpg");
        assertThat(first.available()).isTrue();

        assertThat(response.items().get(1).available()).isFalse();
        assertThat(response.items().get(1).originalPrice()).isNull();
        assertThat(response.items().get(2).available()).isFalse();
        assertThat(response.items().get(2).imageUrl()).isNull();

        assertThat(response.totalQuantity()).isEqualTo(7);
        assertThat(response.subtotal()).isEqualByComparingTo("36000000");
        assertThat(response.hasUnavailableItems()).isTrue();
        assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void addItem_createsTheCartIfNeeded_andCapsTheLine() {
        stubUser();
        ProductVariant variant = variant(10L, product(1L, "Điện thoại A"), "15990000", null);
        when(variantRepository.findByIdAndProductDeletedAtIsNull(10L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.existsByCartIdAndVariantId(CART_ID, 10L)).thenReturn(false);
        when(cartItemRepository.countByCartId(CART_ID)).thenReturn(0L);
        when(cartItemRepository.findAllWithProductByCartId(CART_ID)).thenReturn(List.of(item(100L, variant, 3)));

        CartResponse response = cartService.addItem(USER_ID, new CartItemAddRequest(10L, 3));

        verify(cartRepository).insertIfMissing(USER_ID);
        verify(cartItemRepository).upsertQuantity(CART_ID, 10L, 3, CartService.MAX_LINE_QUANTITY);
        verify(cartRepository).touch(CART_ID);
        assertThat(response.totalQuantity()).isEqualTo(3);
        assertThat(response.subtotal()).isEqualByComparingTo("47970000");
    }

    @Test
    void addItem_unknownOrDeletedVariant_throwsNotFound() {
        stubUser();
        when(variantRepository.findByIdAndProductDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertError(() -> cartService.addItem(USER_ID, new CartItemAddRequest(99L, 1)),
                ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        verify(cartRepository, never()).insertIfMissing(anyLong());
    }

    @Test
    void addItem_hiddenProduct_isNotAvailable() {
        stubUser();
        Product hidden = product(1L, "Điện thoại A");
        hidden.setActive(false);
        when(variantRepository.findByIdAndProductDeletedAtIsNull(10L))
                .thenReturn(Optional.of(variant(10L, hidden, "15990000", null)));

        assertError(() -> cartService.addItem(USER_ID, new CartItemAddRequest(10L, 1)),
                ErrorCode.PRODUCT_NOT_AVAILABLE);
        verify(cartRepository, never()).insertIfMissing(anyLong());
    }

    @Test
    void addItem_priceZero_isNotAvailable() {
        stubUser();
        when(variantRepository.findByIdAndProductDeletedAtIsNull(10L))
                .thenReturn(Optional.of(variant(10L, product(1L, "Sạc"), "0", null)));

        assertError(() -> cartService.addItem(USER_ID, new CartItemAddRequest(10L, 1)),
                ErrorCode.PRODUCT_NOT_AVAILABLE);
    }

    @Test
    void addItem_newLineInAFullCart_isRejected_butAnExistingLineCanGrow() {
        stubUser();
        ProductVariant variant = variant(10L, product(1L, "Điện thoại A"), "15990000", null);
        when(variantRepository.findByIdAndProductDeletedAtIsNull(10L)).thenReturn(Optional.of(variant));
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.existsByCartIdAndVariantId(CART_ID, 10L)).thenReturn(false);
        when(cartItemRepository.countByCartId(CART_ID)).thenReturn((long) CartService.MAX_LINES);

        assertError(() -> cartService.addItem(USER_ID, new CartItemAddRequest(10L, 1)),
                ErrorCode.CART_LIMIT_EXCEEDED);
        verify(cartItemRepository, never()).upsertQuantity(anyLong(), anyLong(), anyInt(), anyInt());

        when(cartItemRepository.existsByCartIdAndVariantId(CART_ID, 10L)).thenReturn(true);
        when(cartItemRepository.findAllWithProductByCartId(CART_ID)).thenReturn(List.of(item(100L, variant, 2)));

        cartService.addItem(USER_ID, new CartItemAddRequest(10L, 1));

        verify(cartItemRepository).upsertQuantity(CART_ID, 10L, 1, CartService.MAX_LINE_QUANTITY);
    }

    @Test
    void updateItem_setsTheQuantity() {
        stubUser();
        ProductVariant variant = variant(10L, product(1L, "Điện thoại A"), "1000000", null);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.existsByCartIdAndVariantId(CART_ID, 10L)).thenReturn(true);
        when(cartItemRepository.findAllWithProductByCartId(CART_ID)).thenReturn(List.of(item(100L, variant, 5)));

        CartResponse response = cartService.updateItem(USER_ID, 10L, new CartItemUpdateRequest(5));

        verify(cartItemRepository).updateQuantity(CART_ID, 10L, 5);
        verify(cartRepository).touch(CART_ID);
        assertThat(response.subtotal()).isEqualByComparingTo("5000000");
    }

    @Test
    void updateItem_variantNotInCart_orNoCart_throwsCartItemNotFound() {
        stubUser();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.existsByCartIdAndVariantId(CART_ID, 10L)).thenReturn(false);

        assertError(() -> cartService.updateItem(USER_ID, 10L, new CartItemUpdateRequest(2)),
                ErrorCode.CART_ITEM_NOT_FOUND);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertError(() -> cartService.updateItem(USER_ID, 10L, new CartItemUpdateRequest(2)),
                ErrorCode.CART_ITEM_NOT_FOUND);
        verify(cartItemRepository, never()).updateQuantity(anyLong(), anyLong(), anyInt());
    }

    @Test
    void removeItem_deletesTheLine_orThrowsWhenItIsMissing() {
        stubUser();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.existsByCartIdAndVariantId(CART_ID, 10L)).thenReturn(true);
        when(cartItemRepository.findAllWithProductByCartId(CART_ID)).thenReturn(List.of());

        CartResponse response = cartService.removeItem(USER_ID, 10L);

        verify(cartItemRepository).deleteByCartIdAndVariantId(CART_ID, 10L);
        assertThat(response.items()).isEmpty();

        when(cartItemRepository.existsByCartIdAndVariantId(CART_ID, 10L)).thenReturn(false);
        assertError(() -> cartService.removeItem(USER_ID, 10L), ErrorCode.CART_ITEM_NOT_FOUND);
    }

    @Test
    void clear_emptiesAnExistingCart_andIsANoOpWithoutCart() {
        stubUser();
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllWithProductByCartId(CART_ID)).thenReturn(List.of());

        assertThat(cartService.clear(USER_ID).items()).isEmpty();
        verify(cartItemRepository).deleteAllByCartId(CART_ID);
        verify(cartRepository).touch(CART_ID);

        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        assertThat(cartService.clear(USER_ID).updatedAt()).isNull();
    }

    @Test
    void disabledOrMissingAccount_isRejectedBeforeTouchingTheCart() {
        user.setActive(false);
        stubUser();

        assertError(() -> cartService.getCart(USER_ID), ErrorCode.ACCOUNT_DISABLED);
        assertError(() -> cartService.addItem(USER_ID, new CartItemAddRequest(10L, 1)),
                ErrorCode.ACCOUNT_DISABLED);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        assertError(() -> cartService.clear(USER_ID), ErrorCode.INVALID_TOKEN);

        verify(cartRepository, never()).findByUserId(anyLong());
        verify(variantRepository, never()).findByIdAndProductDeletedAtIsNull(anyLong());
    }

    private static Product product(Long id, String name) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSlug("slug-" + id);
        return product;
    }

    private static ProductVariant variant(Long id, Product product, String price, String discountPrice) {
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProduct(product);
        variant.setVariantName("Phiên bản " + id);
        variant.setPrice(new BigDecimal(price));
        variant.setDiscountPrice(discountPrice == null ? null : new BigDecimal(discountPrice));
        return variant;
    }

    private CartItem item(Long id, ProductVariant variant, int quantity) {
        CartItem item = new CartItem(cart, variant, quantity);
        item.setId(id);
        return item;
    }

    private static ProductImage image(Product product, String url) {
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(url);
        return image;
    }
}
