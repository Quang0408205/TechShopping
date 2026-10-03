package com.example.Tech.service.impl.order;

import com.example.Tech.dto.request.order.OrderCreateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.entity.cart.Cart;
import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.cart.CartMapper;
import com.example.Tech.repository.cart.CartItemRepository;
import com.example.Tech.repository.cart.CartRepository;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.service.impl.payment.OrderPaymentLifecycle;
import com.example.Tech.service.order.OrderService;
import com.example.Tech.service.order.ShippingPolicy;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final CurrentUserLoader currentUserLoader;
    private final OrderViewLoader orderViewLoader;
    private final OrderPaymentLifecycle orderPaymentLifecycle;
    private final Clock clock;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderCreateRequest request) {
        User user = currentUserLoader.load(userId);
        orderPaymentLifecycle.validateSelection(request.paymentMethod(), request.installment());

        // SELECT … FOR UPDATE: a concurrent second submit waits here, then finds the emptied cart
        Cart cart = cartRepository.findByUserIdForUpdate(userId).orElseThrow(OrderServiceImpl::cartEmpty);
        List<CartItem> lines = cartItemRepository.findAllWithProductByCartId(cart.getId());
        if (lines.isEmpty()) {
            throw cartEmpty();
        }
        List<Long> unavailable = lines.stream()
                .map(CartItem::getVariant)
                .filter(variant -> !cartMapper.isPurchasable(variant))
                .map(ProductVariant::getId)
                .toList();
        if (!unavailable.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE,
                    "Remove the unavailable items from the cart first (variant ids %s)".formatted(unavailable));
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now(clock));
        order.setRecipientName(request.recipientName().trim());
        order.setRecipientPhone(request.recipientPhone().trim());
        order.setShippingAddress(request.shippingAddress().trim());
        order.setNotes(trimToNull(request.note()));
        order.setPaymentMethod(request.paymentMethod());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem line : lines) {
            OrderItem item = toOrderItem(line);
            order.addItem(item);
            subtotal = subtotal.add(item.getSubtotal());
        }
        BigDecimal shippingFee = ShippingPolicy.feeFor(subtotal);
        order.setShippingCost(shippingFee);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(subtotal.add(shippingFee));
        orderPaymentLifecycle.checkEligible(request.paymentMethod(), order.getTotalAmount());

        Order saved = orderRepository.save(order);
        orderPaymentLifecycle.onOrderPlaced(saved, request.installment());
        int removed = cartItemRepository.deleteAllByCartId(cart.getId());
        cartRepository.touch(cart.getId());
        log.info("User id={} placed order id={} ({} line(s), total {}); {} cart line(s) removed",
                userId, saved.getId(), lines.size(), saved.getTotalAmount(), removed);
        return orderViewLoader.toResponse(saved);
    }

    @Override
    public PageResponse<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        currentUserLoader.load(userId);
        return PageResponse.from(orderViewLoader.toResponsePage(orderRepository.findAllByUserId(userId, pageable)));
    }

    @Override
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        currentUserLoader.load(userId);
        return orderViewLoader.toResponse(orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId)));
    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(Long userId, Long orderId) {
        currentUserLoader.load(userId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .filter(found -> found.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));
        if (!order.getStatus().canBeCancelledByCustomer()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "Only a pending order can be cancelled (order %d is %s)".formatted(orderId, order.getStatus()));
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now(clock));
        orderPaymentLifecycle.onOrderCancelled(order);
        Order saved = orderRepository.saveAndFlush(order);
        log.info("User id={} cancelled order id={}", userId, orderId);
        return orderViewLoader.toResponse(saved);
    }

    /** Price fixed now: the variant's discount price when lower (same rule as the cart). */
    private OrderItem toOrderItem(CartItem line) {
        ProductVariant variant = line.getVariant();
        BigDecimal unitPrice = cartMapper.unitPrice(variant);
        BigDecimal quantity = BigDecimal.valueOf(line.getQuantity());
        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(line.getQuantity());
        item.setUnitPrice(unitPrice);
        item.setDiscountAmount(variant.getPrice().subtract(unitPrice).multiply(quantity));
        item.setSubtotal(unitPrice.multiply(quantity));
        return item;
    }

    private static BusinessException cartEmpty() {
        return new BusinessException(ErrorCode.CART_EMPTY, ErrorCode.CART_EMPTY.getDefaultMessage());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
