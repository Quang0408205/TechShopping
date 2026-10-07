package com.example.Tech.service.impl.order;

import com.example.Tech.dto.request.order.AdminOrderSearchRequest;
import com.example.Tech.dto.request.order.OrderStatusUpdateRequest;
import com.example.Tech.dto.request.order.OrderStoreRequest;
import com.example.Tech.dto.request.payment.InstallmentRejectRequest;
import com.example.Tech.dto.request.payment.PaymentConfirmRequest;
import com.example.Tech.service.impl.payment.OrderPaymentLifecycle;
import org.mockito.InOrder;
import com.example.Tech.dto.response.order.AdminOrderResponse;
import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.service.store.StoreAccessGuard;
import com.example.Tech.service.user.CurrentUserLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminOrderServiceImplTest {

    private static final Long STAFF_ID = 2L;
    private static final Long ADMIN_ID = 1L;
    private static final Integer STORE_ID = 3;
    private static final Long CUSTOMER_ID = 9L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private CurrentUserLoader currentUserLoader;

    @Mock
    private OrderViewLoader orderViewLoader;

    @Mock
    private OrderPaymentLifecycle orderPaymentLifecycle;

    @Mock
    private OrderStockLifecycle orderStockLifecycle;

    @Mock
    private StoreAccessGuard storeAccessGuard;

    private final User staff = new User();

    private AdminOrderServiceImpl adminOrderService;

    private User customer;

    @BeforeEach
    void setUp() {
        adminOrderService = new AdminOrderServiceImpl(orderRepository, customerProfileRepository, currentUserLoader,
                orderViewLoader, orderPaymentLifecycle, orderStockLifecycle, storeAccessGuard, CLOCK);
        customer = new User();
        customer.setId(CUSTOMER_ID);
        customer.setUsername("khach");
        customer.setFullname("Khách Hàng");
        customer.setEmail("khach@example.com");
        when(orderViewLoader.toStaffResponses(any())).thenReturn(List.of(mock(OrderResponse.class)));
        when(storeAccessGuard.orderScope(STAFF_ID)).thenReturn(new StoreAccessGuard.OrderScope(staff, STORE_ID));
    }

    @Test
    void updateStatus_pendingToConfirmed_savesWithoutTouchingTotalSpent() {
        Order order = stored(OrderStatus.PENDING);

        AdminOrderResponse response = adminOrderService.updateStatus(STAFF_ID, 5L,
                new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getDeliveredAt()).isNull();
        assertThat(order.getCancelledAt()).isNull();
        verify(orderRepository).saveAndFlush(order);
        verify(customerProfileRepository, never()).addToTotalSpent(anyLong(), any());
        verify(storeAccessGuard).orderScope(STAFF_ID);
        // payment rules first, then the stock is taken from the order's store, then the save
        InOrder inOrder = inOrder(orderPaymentLifecycle, orderStockLifecycle, orderRepository);
        inOrder.verify(orderPaymentLifecycle).checkCanConfirm(order);
        inOrder.verify(orderStockLifecycle).onOrderConfirmed(order, staff);
        inOrder.verify(orderRepository).saveAndFlush(order);
        assertThat(response.customer().username()).isEqualTo("khach");
        assertThat(response.customer().deleted()).isFalse();
    }

    @Test
    void updateStatus_confirmedToShipping_withATrackingNumber() {
        Order order = stored(OrderStatus.CONFIRMED);

        adminOrderService.updateStatus(STAFF_ID, 5L, new OrderStatusUpdateRequest(OrderStatus.SHIPPING, " GHN123 "));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
        assertThat(order.getTrackingNumber()).isEqualTo("GHN123");
    }

    @Test
    void updateStatus_shippingToDelivered_setsDeliveredAtAndAddsToTotalSpent() {
        Order order = stored(OrderStatus.SHIPPING);
        when(customerProfileRepository.addToTotalSpent(CUSTOMER_ID, order.getTotalAmount())).thenReturn(1);

        adminOrderService.updateStatus(STAFF_ID, 5L, new OrderStatusUpdateRequest(OrderStatus.DELIVERED, null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isEqualTo(NOW);
        verify(customerProfileRepository).addToTotalSpent(CUSTOMER_ID, new BigDecimal("31980000"));
        // payment changes happen before the flush that precedes addToTotalSpent (it clears the persistence context)
        InOrder inOrder = inOrder(orderPaymentLifecycle, orderRepository, customerProfileRepository);
        inOrder.verify(orderPaymentLifecycle).onOrderDelivered(order, staff, NOW);
        inOrder.verify(orderRepository).saveAndFlush(order);
        inOrder.verify(customerProfileRepository).addToTotalSpent(anyLong(), any());
    }

    @Test
    void updateStatus_confirmedToCancelled_setsCancelledAt_andCancelsThePayment() {
        Order order = stored(OrderStatus.CONFIRMED);

        adminOrderService.updateStatus(STAFF_ID, 5L, new OrderStatusUpdateRequest(OrderStatus.CANCELLED, null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isEqualTo(NOW);
        verify(customerProfileRepository, never()).addToTotalSpent(anyLong(), any());
        verify(orderPaymentLifecycle).onOrderCancelled(order);
        verify(orderStockLifecycle).onOrderCancelled(order, OrderStatus.CONFIRMED, staff);
        verify(orderPaymentLifecycle, never()).onOrderDelivered(any(), any(), any());
    }

    @Test
    void updateStatus_toConfirmed_withoutEnoughStock_savesNothing() {
        Order order = stored(OrderStatus.PENDING);
        doThrow(new BusinessException(ErrorCode.INSUFFICIENT_STOCK)).when(orderStockLifecycle)
                .onOrderConfirmed(order, staff);

        assertThatThrownBy(() -> adminOrderService.updateStatus(STAFF_ID, 5L,
                new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void staff_cannotReachAnOrderOfAnotherStore_orWithoutAStore() {
        Order order = stored(OrderStatus.PENDING);
        order.setStore(store(99));

        assertThatThrownBy(() -> adminOrderService.updateStatus(STAFF_ID, 5L,
                new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThatThrownBy(() -> adminOrderService.getById(STAFF_ID, 5L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThatThrownBy(() -> adminOrderService.confirmPayment(STAFF_ID, 5L, null))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        order.setStore(null);
        assertThatThrownBy(() -> adminOrderService.approveInstallment(STAFF_ID, 5L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(orderPaymentLifecycle, never()).checkCanConfirm(any());
        verify(orderPaymentLifecycle, never()).confirmTransfer(any(), any(), any(), any());
        verify(orderPaymentLifecycle, never()).approveInstallment(any(), any(), any());
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void admin_reachesAnOrderWithoutAStore() {
        User admin = new User();
        when(storeAccessGuard.orderScope(ADMIN_ID)).thenReturn(new StoreAccessGuard.OrderScope(admin, null));
        Order order = stored(OrderStatus.PENDING);
        order.setStore(null);

        adminOrderService.updateStatus(ADMIN_ID, 5L, new OrderStatusUpdateRequest(OrderStatus.CANCELLED, null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderStockLifecycle).onOrderCancelled(order, OrderStatus.PENDING, admin);
    }

    @Test
    void reassignStore_adminOnly_pendingOnly() {
        Order order = stored(OrderStatus.PENDING);

        adminOrderService.reassignStore(ADMIN_ID, 5L, new OrderStoreRequest(7));

        verify(currentUserLoader).loadWithAnyRole(ADMIN_ID, RoleName.ADMIN);
        verify(orderStockLifecycle).reassignStore(order, 7);
        verify(orderRepository).saveAndFlush(order);

        order.setStatus(OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> adminOrderService.reassignStore(ADMIN_ID, 5L, new OrderStoreRequest(7)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        verify(orderStockLifecycle, times(1)).reassignStore(any(), any());

        when(currentUserLoader.loadWithAnyRole(STAFF_ID, RoleName.ADMIN))
                .thenThrow(new BusinessException(ErrorCode.ACCESS_DENIED));
        assertThatThrownBy(() -> adminOrderService.reassignStore(STAFF_ID, 5L, new OrderStoreRequest(7)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void updateStatus_toConfirmed_whenThePaymentRulesRefuse_savesNothingAndTakesNoStock() {
        Order order = stored(OrderStatus.PENDING);
        doThrow(new BusinessException(ErrorCode.PAYMENT_REQUIRED)).when(orderPaymentLifecycle).checkCanConfirm(order);

        assertThatThrownBy(() -> adminOrderService.updateStatus(STAFF_ID, 5L,
                new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, "GHN1")))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_REQUIRED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTrackingNumber()).isNull();
        verify(orderStockLifecycle, never()).onOrderConfirmed(any(), any());
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateStatus_sameStatusConfirmed_doesNotCheckThePaymentAgain() {
        stored(OrderStatus.CONFIRMED);

        adminOrderService.updateStatus(STAFF_ID, 5L, new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, "GHN1"));

        verify(orderPaymentLifecycle, never()).checkCanConfirm(any());
        verify(orderStockLifecycle, never()).onOrderConfirmed(any(), any());
    }

    @Test
    void paymentActions_lockTheOrder_andPassTheStaffMember() {
        Order order = stored(OrderStatus.PENDING);

        adminOrderService.confirmPayment(STAFF_ID, 5L, new PaymentConfirmRequest("FT1"));
        adminOrderService.confirmPayment(STAFF_ID, 5L, null);
        adminOrderService.refundPayment(STAFF_ID, 5L);
        adminOrderService.approveInstallment(STAFF_ID, 5L);

        verify(orderPaymentLifecycle).confirmTransfer(order, staff, "FT1", NOW);
        verify(orderPaymentLifecycle).confirmTransfer(order, staff, null, NOW);
        verify(orderPaymentLifecycle).confirmRefund(order, staff, NOW);
        verify(orderPaymentLifecycle).approveInstallment(order, staff, NOW);
        verify(orderRepository, times(4)).findByIdForUpdate(5L);
    }

    @Test
    void rejectInstallment_cancelsTheOrder() {
        Order order = stored(OrderStatus.PENDING);

        adminOrderService.rejectInstallment(STAFF_ID, 5L, new InstallmentRejectRequest("Sai CCCD"));

        verify(orderPaymentLifecycle).rejectInstallment(order, staff, "Sai CCCD", NOW);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isEqualTo(NOW);
        verify(orderRepository).saveAndFlush(order);
    }

    @ParameterizedTest
    @CsvSource({"PENDING,SHIPPING", "PENDING,DELIVERED", "CONFIRMED,PENDING", "SHIPPING,CANCELLED",
            "DELIVERED,CANCELLED", "CANCELLED,CONFIRMED", "DELIVERED,SHIPPING"})
    void updateStatus_outsideTheFlow_throwsInvalidOrderStatusAndSavesNothing(OrderStatus from, OrderStatus to) {
        Order order = stored(from);

        assertThatThrownBy(() -> adminOrderService.updateStatus(STAFF_ID, 5L, new OrderStatusUpdateRequest(to, "X1")))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
        assertThat(order.getStatus()).isEqualTo(from);
        assertThat(order.getTrackingNumber()).isNull();
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateStatus_sameStatus_onlyChangesTheTrackingNumber() {
        Order order = stored(OrderStatus.SHIPPING);
        order.setTrackingNumber("OLD");

        adminOrderService.updateStatus(STAFF_ID, 5L, new OrderStatusUpdateRequest(OrderStatus.SHIPPING, "NEW-1"));
        assertThat(order.getTrackingNumber()).isEqualTo("NEW-1");
        assertThat(order.getDeliveredAt()).isNull();

        adminOrderService.updateStatus(STAFF_ID, 5L, new OrderStatusUpdateRequest(OrderStatus.SHIPPING, "  "));
        assertThat(order.getTrackingNumber()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    void anyCall_byAUserWithoutStaffRole_isDeniedBeforeReadingOrders() {
        when(storeAccessGuard.orderScope(STAFF_ID)).thenThrow(new BusinessException(ErrorCode.ACCESS_DENIED));

        assertThatThrownBy(() -> adminOrderService.updateStatus(STAFF_ID, 5L,
                new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThatThrownBy(() -> adminOrderService.getById(STAFF_ID, 5L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
        verify(orderRepository, never()).findByIdForUpdate(anyLong());
        verify(orderRepository, never()).findWithUserById(anyLong());
    }

    @Test
    void getById_unknownOrder_throwsOrderNotFound() {
        when(orderRepository.findWithUserById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminOrderService.getById(STAFF_ID, 5L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_fromDateAfterToDate_throwsValidationError() {
        AdminOrderSearchRequest filter = new AdminOrderSearchRequest(null, null,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1), null);

        assertThatThrownBy(() -> adminOrderService.search(STAFF_ID, filter, PageRequest.of(0, 20)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(orderRepository, never()).findAll(any(Specification.class), any(PageRequest.class));
    }

    private Order stored(OrderStatus status) {
        Order order = new Order();
        order.setId(5L);
        order.setUser(customer);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("31980000"));
        order.setStore(store(STORE_ID));
        when(orderRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(order));
        when(orderRepository.findWithUserById(5L)).thenReturn(Optional.of(order));
        return order;
    }

    private static Store store(Integer id) {
        Store store = new Store();
        store.setId(id);
        store.setName("Chi nhánh " + id);
        return store;
    }
}
