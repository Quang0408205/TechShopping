package com.example.Tech.service.impl.order;

import com.example.Tech.config.BankTransferProperties;
import com.example.Tech.dto.request.order.OrderCreateRequest;
import com.example.Tech.entity.combo.OrderCombo;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.order.OrderCountsResponse;
import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.entity.cart.Cart;
import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.payment.PaymentStatus;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.cart.CartMapper;
import com.example.Tech.repository.cart.CartItemRepository;
import com.example.Tech.repository.cart.CartRepository;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.order.OrderFilterSpecifications;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.payment.PaymentRepository;
import com.example.Tech.service.cart.CartService;
import com.example.Tech.service.combo.ComboCalculator;
import com.example.Tech.service.combo.ComboPricingService;
import com.example.Tech.service.loyalty.LoyaltyService;
import com.example.Tech.service.impl.payment.OrderPaymentLifecycle;
import com.example.Tech.service.order.OrderService;
import com.example.Tech.service.order.ShippingPolicy;
import com.example.Tech.service.recommendation.InteractionRecorder;
import com.example.Tech.service.user.CurrentUserLoader;
import com.example.Tech.service.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    /** Paid on a page of the shop right after ordering: these orders can be "Huỷ thanh toán" while unpaid. */
    private static final EnumSet<PaymentMethod> PAID_ONLINE = EnumSet.of(PaymentMethod.BANK_TRANSFER, PaymentMethod.CARD);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final CurrentUserLoader currentUserLoader;
    private final OrderViewLoader orderViewLoader;
    private final OrderPaymentLifecycle orderPaymentLifecycle;
    private final OrderStockLifecycle orderStockLifecycle;
    private final OrderStatusRecorder statusRecorder;
    private final InteractionRecorder interactionRecorder;
    private final VoucherService voucherService;
    private final ComboPricingService comboPricingService;
    private final LoyaltyService loyaltyService;
    private final PaymentRepository paymentRepository;
    private final BankTransferProperties bankTransfer;
    private final Clock clock;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderCreateRequest request) {
        User user = currentUserLoader.load(userId);
        orderPaymentLifecycle.validateSelection(request.paymentMethod(), request.installment());
        DeliveryType deliveryType = request.deliveryTypeOrDefault();
        orderStockLifecycle.validateDeliverySelection(deliveryType, request.pickupStoreId(), request.shippingAddress());

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
        order.setShippingAddress(trimToNull(request.shippingAddress()));
        if (deliveryType == DeliveryType.HOME_DELIVERY) {
            order.setShippingProvince(trimToNull(request.shippingProvince()));
            order.setShippingWard(trimToNull(request.shippingWard()));
        }
        order.setNotes(trimToNull(request.note()));
        order.setPaymentMethod(request.paymentMethod());
        order.setStatus(OrderStatus.PENDING);
        // after the address is set: home delivery picks the nearest store holding the whole cart, pickup replaces it
        Map<Long, Integer> needed = new LinkedHashMap<>();
        Map<Long, String> labels = new HashMap<>();
        for (CartItem line : lines) {
            ProductVariant variant = line.getVariant();
            needed.merge(variant.getId(), line.getQuantity(), Integer::sum);
            labels.put(variant.getId(), variant.getProduct().getName() + " - " + variant.getVariantName());
        }
        orderStockLifecycle.assignBranch(order, deliveryType, request.pickupStoreId(), needed, labels);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem line : lines) {
            OrderItem item = toOrderItem(line);
            order.addItem(item);
            subtotal = subtotal.add(item.getSubtotal());
        }
        // plan v5 GĐ10: combo first (add-ons bought with their main product), then voucher, then points;
        // the free-shipping threshold uses the goods before all of them
        BigDecimal comboDiscount = applyCombos(order);
        // plan v5: the voucher takes its % off the goods (shipping excluded)
        String voucherCode = trimToNull(request.voucherCode());
        BigDecimal voucherDiscount = voucherCode == null ? BigDecimal.ZERO
                : voucherService.applyToOrder(order, user, voucherCode);
        // plan v5 GĐ6: points after the voucher, at most 10% of what is left of the goods
        BigDecimal pointsDiscount = loyaltyService.applyToOrder(order, user, request.pointsToUse());
        BigDecimal shippingFee = ShippingPolicy.feeFor(subtotal, deliveryType);
        order.setShippingCost(shippingFee);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(subtotal.subtract(comboDiscount).subtract(voucherDiscount).subtract(pointsDiscount).add(shippingFee));
        orderPaymentLifecycle.checkEligible(request.paymentMethod(), order.getTotalAmount());

        Order saved = orderRepository.save(order);
        voucherService.recordUse(saved, user);
        loyaltyService.recordRedeem(saved, user);
        statusRecorder.record(saved, null, OrderStatus.PENDING, user);
        orderPaymentLifecycle.onOrderPlaced(saved, request.installment());
        interactionRecorder.recordPurchases(userId, lines.stream()
                .map(line -> line.getVariant().getProduct().getId())
                .toList());
        int removed = cartItemRepository.deleteAllByCartId(cart.getId());
        cartRepository.touch(cart.getId());
        log.info("User id={} placed order id={} ({} line(s), total {}); {} cart line(s) removed",
                userId, saved.getId(), lines.size(), saved.getTotalAmount(), removed);
        return orderViewLoader.toResponse(saved);
    }

    /** Writes the per-line shares + the applied combos on the order; returns the total taken off. */
    private BigDecimal applyCombos(Order order) {
        ComboCalculator.Result result = comboPricingService.priceOrderItems(order.getItems());
        if (result.total().signum() == 0) {
            return BigDecimal.ZERO;
        }
        for (int i = 0; i < order.getItems().size(); i++) {
            order.getItems().get(i).setComboDiscount(result.discountOf((long) i));
        }
        for (ComboCalculator.Applied applied : result.applied()) {
            OrderCombo combo = new OrderCombo();
            combo.setComboId(applied.comboId());
            combo.setComboName(applied.name());
            combo.setUnits(applied.units());
            combo.setDiscountAmount(applied.discount());
            order.addCombo(combo);
        }
        order.setComboDiscount(result.total());
        return result.total();
    }

    @Override
    public PageResponse<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        currentUserLoader.load(userId);
        return PageResponse.from(orderViewLoader.toResponsePage(orderRepository.findAllByUserId(userId, pageable)));
    }

    @Override
    public PageResponse<OrderResponse> getMyOrders(Long userId, List<OrderStatus> statuses, String keyword, Pageable pageable) {
        currentUserLoader.load(userId);
        if (keyword != null && keyword.length() > 100) {
            throw BusinessException.invalidField("keyword", "Từ khoá tối đa 100 ký tự");
        }
        return PageResponse.from(orderViewLoader.toResponsePage(
                orderRepository.findAll(OrderFilterSpecifications.history(userId, statuses, keyword), pageable)));
    }

    @Override
    public OrderCountsResponse countMyOrders(Long userId) {
        currentUserLoader.load(userId);
        Map<OrderStatus, Long> byStatus = new HashMap<>();
        for (Object[] row : orderRepository.countByStatusForUser(userId)) {
            byStatus.put((OrderStatus) row[0], (Long) row[1]);
        }
        long all = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new OrderCountsResponse(all, byStatus.getOrDefault(OrderStatus.PENDING, 0L),
                byStatus.getOrDefault(OrderStatus.CONFIRMED, 0L) + byStatus.getOrDefault(OrderStatus.SHIPPING, 0L),
                byStatus.getOrDefault(OrderStatus.DELIVERED, 0L), byStatus.getOrDefault(OrderStatus.CANCELLED, 0L));
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
        User user = currentUserLoader.load(userId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .filter(found -> found.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));
        Order saved = cancel(order, user);
        log.info("User id={} cancelled order id={}", userId, orderId);
        return orderViewLoader.toResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse cancelPayment(Long userId, Long orderId) {
        User user = currentUserLoader.load(userId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .filter(found -> found.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));
        boolean awaited = PAID_ONLINE.contains(order.getPaymentMethod())
                && paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(orderId)
                .map(Payment::getStatus)
                .filter(status -> status == PaymentStatus.PENDING)
                .isPresent();
        if (!order.getStatus().canBeCancelledByCustomer() || !awaited) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS,
                    "Đơn %s không còn chờ thanh toán nên không huỷ thanh toán được".formatted(OrderMapper.code(orderId)));
        }
        cancel(order, user);
        int restored = restoreToCart(userId, order);
        log.info("User id={} cancelled the payment of order id={}; {} line(s) back in the cart", userId, orderId, restored);
        // the cart upserts cleared the persistence context: read the order again
        return orderViewLoader.toResponse(orderRepository.findById(orderId).orElseThrow());
    }

    @Override
    public OrderResponse findAwaitingPayment(Long userId) {
        currentUserLoader.load(userId);
        LocalDateTime since = LocalDateTime.now(clock).minusHours(bankTransfer.deadlineHours());
        return paymentRepository.findAwaitedOrderIds(userId, PAID_ONLINE, PaymentStatus.PENDING, OrderStatus.PENDING,
                        since, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .flatMap(orderRepository::findById)
                .map(orderViewLoader::toResponse)
                .orElse(null);
    }

    /** PENDING → CANCELLED by the customer; the caller has locked the order row. */
    private Order cancel(Order order, User user) {
        if (!order.getStatus().canBeCancelledByCustomer()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "Only a pending order can be cancelled (order %d is %s)".formatted(order.getId(), order.getStatus()));
        }
        statusRecorder.record(order, order.getStatus(), OrderStatus.CANCELLED, user);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now(clock));
        orderPaymentLifecycle.onOrderCancelled(order);
        voucherService.releaseFor(order);
        loyaltyService.releaseFor(order);
        return orderRepository.saveAndFlush(order);
    }

    /**
     * Puts the order's lines back into the cart (added to what is there, capped like the cart). Lines of products
     * deleted since are left out; the cart page shows the others even when they are sold out now.
     */
    private int restoreToCart(Long userId, Order order) {
        Map<Long, Integer> lines = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getVariant();
            if (variant != null && variant.getProduct().getDeletedAt() == null) {
                lines.merge(variant.getId(), item.getQuantity(), Integer::sum);
            }
        }
        cartRepository.insertIfMissing(userId);
        Long cartId = cartRepository.findByUserId(userId).orElseThrow().getId();
        lines.forEach((variantId, quantity) ->
                cartItemRepository.upsertQuantity(cartId, variantId, quantity, CartService.MAX_LINE_QUANTITY));
        cartRepository.touch(cartId);
        return lines.size();
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
