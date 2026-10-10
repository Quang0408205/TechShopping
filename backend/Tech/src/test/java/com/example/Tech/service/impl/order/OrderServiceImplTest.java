package com.example.Tech.service.impl.order;

import com.example.Tech.dto.request.order.OrderCreateRequest;
import com.example.Tech.dto.request.payment.InstallmentRequest;
import com.example.Tech.entity.payment.InstallmentBank;
import com.example.Tech.service.impl.payment.OrderPaymentLifecycle;
import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.entity.cart.Cart;
import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.promotion.DiscountType;
import com.example.Tech.entity.promotion.Promotion;
import com.example.Tech.entity.promotion.PromotionProduct;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.cart.CartMapper;
import com.example.Tech.repository.cart.CartItemRepository;
import com.example.Tech.repository.cart.CartRepository;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.promotion.PromotionProductRepository;
import com.example.Tech.service.promotion.PromotionPricingService;
import com.example.Tech.service.user.CurrentUserLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T03:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CurrentUserLoader currentUserLoader;

    @Mock
    private OrderViewLoader orderViewLoader;

    @Mock
    private PromotionProductRepository promotionProductRepository;

    @Mock
    private OrderPaymentLifecycle orderPaymentLifecycle;

    @Mock
    private OrderStockLifecycle orderStockLifecycle;

    @Mock
    private OrderStatusRecorder statusRecorder;

    private OrderServiceImpl orderService;

    private User user;
    private Cart cart;
    private ProductVariant black;
    private ProductVariant white;

    @BeforeEach
    void setUp() {
        lenient().when(promotionProductRepository.findActiveForProducts(any(), any())).thenReturn(List.of());
        CartMapper cartMapper = new CartMapper(new PromotionPricingService(promotionProductRepository), CLOCK);
        orderService = new OrderServiceImpl(orderRepository, cartRepository, cartItemRepository, cartMapper,
                currentUserLoader, orderViewLoader, orderPaymentLifecycle, orderStockLifecycle, statusRecorder, CLOCK);
        user = new User();
        user.setId(USER_ID);
        cart = new Cart(user);
        cart.setId(30L);
        Product phone = product(1L);
        black = variant(11L, phone, "15990000", "14990000");
        white = variant(12L, phone, "2000000", null);
    }

    @Test
    void placeOrder_buildsTheOrderFromTheCart_freeShippingFrom10Million_emptiesTheCart() {
        cartWith(line(black, 2), line(white, 1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });
        OrderResponse expected = mock(OrderResponse.class);
        when(orderViewLoader.toResponse(any(Order.class))).thenReturn(expected);

        OrderResponse response = orderService.placeOrder(USER_ID, new OrderCreateRequest(
                "  Nguyễn Văn An ", " 0901 234 567 ", " 12 Nguyễn Trãi, Quận 5 ", "   ", PaymentMethod.BANK_TRANSFER));

        assertThat(response).isSameAs(expected);
        Order order = savedOrder();
        verify(statusRecorder).record(order, null, OrderStatus.PENDING, user);
        assertThat(order.getUser()).isSameAs(user);
        assertThat(order.getRecipientName()).isEqualTo("Nguyễn Văn An");
        assertThat(order.getRecipientPhone()).isEqualTo("0901 234 567");
        assertThat(order.getShippingAddress()).isEqualTo("12 Nguyễn Trãi, Quận 5");
        assertThat(order.getNotes()).isNull();
        assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getOrderDate()).isEqualTo(NOW);
        assertThat(order.getItems())
                .extracting(item -> item.getVariant().getId(), OrderItem::getQuantity,
                        item -> item.getUnitPrice().toPlainString(), item -> item.getDiscountAmount().toPlainString(),
                        item -> item.getSubtotal().toPlainString())
                .containsExactly(
                        tuple(11L, 2, "14990000", "2000000", "29980000"),
                        tuple(12L, 1, "2000000", "0", "2000000"));
        assertThat(order.getItems()).allSatisfy(item -> assertThat(item.getOrder()).isSameAs(order));
        assertThat(order.getShippingCost()).isEqualByComparingTo("0");
        assertThat(order.getTaxAmount()).isEqualByComparingTo("0");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("31980000");
        verify(cartItemRepository).deleteAllByCartId(30L);
        verify(cartRepository).touch(30L);
        verify(orderPaymentLifecycle).validateSelection(PaymentMethod.BANK_TRANSFER, null);
        verify(orderPaymentLifecycle).checkEligible(PaymentMethod.BANK_TRANSFER, order.getTotalAmount());
        verify(orderPaymentLifecycle).onOrderPlaced(order, null);
    }

    @Test
    void placeOrder_homeDeliveryByDefault_assignsTheBranchAfterTheAddressIsSet() {
        cartWith(line(white, 1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            assertThat(order.getShippingAddress()).isEqualTo("12 Nguyễn Trãi");
            return null;
        }).when(orderStockLifecycle).assignBranch(any(Order.class), eq(DeliveryType.HOME_DELIVERY), isNull());

        orderService.placeOrder(USER_ID, request(null, PaymentMethod.COD));

        Order order = savedOrder();
        verify(orderStockLifecycle).validateDeliverySelection(DeliveryType.HOME_DELIVERY, null, "12 Nguyễn Trãi");
        verify(orderStockLifecycle).assignBranch(order, DeliveryType.HOME_DELIVERY, null);
        assertThat(order.getShippingCost()).isEqualByComparingTo("30000");
    }

    @Test
    void placeOrder_pickup_hasNoShippingFee_andNeedsNoAddress() {
        cartWith(line(white, 1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.placeOrder(USER_ID, new OrderCreateRequest("Nguyễn Văn An", "0901234567", null, null,
                PaymentMethod.COD, null, DeliveryType.PICKUP, 5));

        Order order = savedOrder();
        verify(orderStockLifecycle).validateDeliverySelection(DeliveryType.PICKUP, 5, null);
        verify(orderStockLifecycle).assignBranch(order, DeliveryType.PICKUP, 5);
        assertThat(order.getShippingCost()).isEqualByComparingTo("0");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("2000000");
    }

    @Test
    void placeOrder_invalidDeliverySelection_readsNoCart() {
        doThrow(BusinessException.invalidField("pickupStoreId", "Vui lòng chọn chi nhánh nhận hàng"))
                .when(orderStockLifecycle).validateDeliverySelection(DeliveryType.PICKUP, null, null);

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, new OrderCreateRequest("Nguyễn Văn An",
                "0901234567", null, null, PaymentMethod.COD, null, DeliveryType.PICKUP, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(cartRepository, never()).findByUserIdForUpdate(any());
    }

    @Test
    void placeOrder_passesTheInstallmentApplicationToThePaymentLifecycle() {
        cartWith(line(black, 1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        InstallmentRequest installment = new InstallmentRequest(6, "0123456789", InstallmentBank.TCB);

        orderService.placeOrder(USER_ID, new OrderCreateRequest("Nguyễn Văn An", "0901234567", "12 Nguyễn Trãi", null,
                PaymentMethod.INSTALLMENT, installment));

        Order order = savedOrder();
        verify(orderPaymentLifecycle).validateSelection(PaymentMethod.INSTALLMENT, installment);
        verify(orderPaymentLifecycle).checkEligible(PaymentMethod.INSTALLMENT, order.getTotalAmount());
        verify(orderPaymentLifecycle).onOrderPlaced(order, installment);
    }

    @Test
    void placeOrder_whenThePaymentChoiceIsRefused_savesNothingAndKeepsTheCart() {
        cartWith(line(white, 1));
        doThrow(new BusinessException(ErrorCode.INSTALLMENT_NOT_ELIGIBLE))
                .when(orderPaymentLifecycle).checkEligible(any(), any());

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, request(null, PaymentMethod.INSTALLMENT)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSTALLMENT_NOT_ELIGIBLE);
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAllByCartId(anyLong());
        verify(orderPaymentLifecycle, never()).onOrderPlaced(any(), any());
    }

    @Test
    void placeOrder_belowTheThreshold_chargesTheFlatShippingFee_keepsTheNote() {
        cartWith(line(white, 2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.placeOrder(USER_ID, request(" Giao giờ hành chính ", PaymentMethod.COD));

        Order order = savedOrder();
        assertThat(order.getShippingCost()).isEqualByComparingTo("30000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("4030000");
        assertThat(order.getNotes()).isEqualTo("Giao giờ hành chính");
    }

    @Test
    void placeOrder_whenAPromotionIsActiveForTheVariantsProduct_chargesThePromotionPrice() {
        ProductVariant plain = variant(20L, product(3L), "1000000", null);
        cartWith(line(plain, 1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Promotion promotion = new Promotion();
        promotion.setId(1L);
        promotion.setName("Sale 30%");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("30"));
        promotion.setStartDate(NOW.minusDays(1));
        promotion.setEndDate(NOW.plusDays(1));
        promotion.setActive(true);
        PromotionProduct promotionProduct = new PromotionProduct(promotion, plain.getProduct(), null, null);
        when(promotionProductRepository.findActiveForProducts(List.of(plain.getProduct().getId()), NOW))
                .thenReturn(List.of(promotionProduct));

        orderService.placeOrder(USER_ID, request(null, PaymentMethod.COD));

        OrderItem item = savedOrder().getItems().getFirst();
        assertThat(item.getUnitPrice()).isEqualByComparingTo("700000");
        assertThat(item.getDiscountAmount()).isEqualByComparingTo("300000");
    }

    @Test
    void placeOrder_withoutCart_throwsCartEmpty() {
        when(currentUserLoader.load(USER_ID)).thenReturn(user);
        when(cartRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, request(null, PaymentMethod.COD)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CART_EMPTY);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_withEmptyCart_throwsCartEmpty() {
        cartWith();

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, request(null, PaymentMethod.COD)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CART_EMPTY);
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAllByCartId(anyLong());
    }

    @Test
    void placeOrder_withAnUnavailableLine_throwsAndKeepsTheCart() {
        white.getProduct().setActive(false);
        cartWith(line(black, 1), line(white, 1));

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, request(null, PaymentMethod.COD)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("12")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_AVAILABLE);
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAllByCartId(anyLong());
    }

    @Test
    void placeOrder_withAZeroPriceLine_throwsProductNotAvailable() {
        ProductVariant free = variant(13L, product(2L), "0", null);
        cartWith(line(free, 1));

        assertThatThrownBy(() -> orderService.placeOrder(USER_ID, request(null, PaymentMethod.COD)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_AVAILABLE);
    }

    @Test
    void getMyOrder_ofAnotherUser_throwsOrderNotFound() {
        when(orderRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrder(USER_ID, 5L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        verify(currentUserLoader).load(USER_ID);
    }

    @Test
    void cancelMyOrder_pending_becomesCancelledWithATimestamp() {
        Order order = order(5L, user, OrderStatus.PENDING);
        when(currentUserLoader.load(USER_ID)).thenReturn(user);
        when(orderRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(order));
        when(orderRepository.saveAndFlush(order)).thenReturn(order);

        orderService.cancelMyOrder(USER_ID, 5L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isEqualTo(NOW);
        verify(orderPaymentLifecycle).onOrderCancelled(order);
        verify(statusRecorder).record(order, OrderStatus.PENDING, OrderStatus.CANCELLED, user);
        verify(orderViewLoader).toResponse(order);
    }

    @Test
    void cancelMyOrder_confirmed_throwsInvalidOrderStatus() {
        Order order = order(5L, user, OrderStatus.CONFIRMED);
        when(orderRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(USER_ID, 5L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository, never()).saveAndFlush(any());
        verify(orderPaymentLifecycle, never()).onOrderCancelled(any());
    }

    @Test
    void cancelMyOrder_ofAnotherUser_throwsOrderNotFound() {
        User other = new User();
        other.setId(8L);
        when(orderRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(order(5L, other, OrderStatus.PENDING)));

        assertThatThrownBy(() -> orderService.cancelMyOrder(USER_ID, 5L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    private void cartWith(CartItem... lines) {
        when(currentUserLoader.load(USER_ID)).thenReturn(user);
        when(cartRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllWithProductByCartId(30L)).thenReturn(List.of(lines));
    }

    private Order savedOrder() {
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        return captor.getValue();
    }

    private CartItem line(ProductVariant variant, int quantity) {
        return new CartItem(cart, variant, quantity);
    }

    private static OrderCreateRequest request(String note, PaymentMethod method) {
        return new OrderCreateRequest("Nguyễn Văn An", "0901234567", "12 Nguyễn Trãi", note, method);
    }

    private static Order order(Long id, User owner, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUser(owner);
        order.setStatus(status);
        return order;
    }

    private static Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("Điện thoại " + id);
        return product;
    }

    private static ProductVariant variant(Long id, Product product, String price, String discountPrice) {
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProduct(product);
        variant.setPrice(new BigDecimal(price));
        variant.setDiscountPrice(discountPrice != null ? new BigDecimal(discountPrice) : null);
        return variant;
    }
}
