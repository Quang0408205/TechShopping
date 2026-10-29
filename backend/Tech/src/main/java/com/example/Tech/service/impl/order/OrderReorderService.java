package com.example.Tech.service.impl.order;

import com.example.Tech.dto.request.cart.CartItemAddRequest;
import com.example.Tech.dto.response.cart.CartResponse;
import com.example.Tech.dto.response.order.ReorderResponse;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.service.cart.CartService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Plan v5 GĐ6 "Mua lại": puts the lines of a delivered / cancelled order back into the cart at today's prices. Each
 * line goes through the normal "add to cart" in its own transaction, so a line that is no longer sold, out of stock
 * or over the cart limits is skipped with its reason while the others are added.
 */
@Slf4j
@Service
public class OrderReorderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final CurrentUserLoader currentUserLoader;
    private final TransactionTemplate readOnly;

    public OrderReorderService(OrderRepository orderRepository, CartService cartService,
                               CurrentUserLoader currentUserLoader, PlatformTransactionManager transactionManager) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.currentUserLoader = currentUserLoader;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    private record Wanted(Long variantId, String productName, String variantName, int quantity) {
    }

    public ReorderResponse reorder(Long userId, Long orderId) {
        currentUserLoader.load(userId);
        List<Wanted> lines = readOnly.execute(status -> {
            Order order = orderRepository.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));
            if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED) {
                throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                        "Chỉ mua lại được đơn đã giao hoặc đã huỷ (đơn %s đang xử lý)".formatted(OrderMapper.code(orderId)));
            }
            List<Wanted> wanted = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                wanted.add(new Wanted(item.getVariant().getId(), item.getVariant().getProduct().getName(),
                        item.getVariant().getVariantName(), item.getQuantity()));
            }
            return wanted;
        });

        List<ReorderResponse.Line> added = new ArrayList<>();
        List<ReorderResponse.Line> skipped = new ArrayList<>();
        CartResponse cart = null;
        for (Wanted line : lines) {
            int quantity = Math.min(line.quantity(), CartService.MAX_LINE_QUANTITY);
            try {
                cart = cartService.addItem(userId, new CartItemAddRequest(line.variantId(), quantity));
                added.add(new ReorderResponse.Line(line.variantId(), line.productName(), line.variantName(), quantity, null));
            } catch (BusinessException e) {
                skipped.add(new ReorderResponse.Line(line.variantId(), line.productName(), line.variantName(), quantity,
                        reason(e)));
            }
        }
        if (cart == null) {
            cart = cartService.getCart(userId);
        }
        log.info("User id={} reordered order id={}: {} line(s) added, {} skipped", userId, orderId, added.size(), skipped.size());
        return new ReorderResponse(added, skipped, cart.totalQuantity());
    }

    private static String reason(BusinessException e) {
        return switch (e.getErrorCode()) {
            case PRODUCT_NOT_AVAILABLE, PRODUCT_VARIANT_NOT_FOUND -> "Sản phẩm đã ngừng bán";
            case OUT_OF_STOCK -> e.getMessage();
            case CART_LIMIT_EXCEEDED -> "Giỏ hàng đã đủ số loại sản phẩm tối đa";
            default -> e.getMessage();
        };
    }
}
