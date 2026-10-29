package com.example.Tech.service.order;

import com.example.Tech.dto.response.order.OrderTrackingResponse;
import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatusHistory;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.order.OrderStatusHistoryRepository;
import com.example.Tech.repository.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Public lookup of an order by the random token in its email link (plan v7). A wrong or malformed token is a 404. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderTrackingService {

    private static final Pattern TOKEN = Pattern.compile("[0-9a-f]{64}");

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;

    public OrderTrackingResponse track(String token) {
        Order order = token == null || !TOKEN.matcher(token).matches() ? null
                : orderRepository.findByTrackingToken(token).orElse(null);
        if (order == null) {
            throw new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "tracking link");
        }
        List<OrderTrackingResponse.Item> items = order.getItems().stream()
                .map(item -> new OrderTrackingResponse.Item(name(item.getVariant().getProduct().getName(),
                        item.getVariant().getVariantName()), item.getQuantity(), item.getSubtotal()))
                .toList();
        List<OrderTrackingResponse.Step> steps = historyRepository.findAllByOrder_IdOrderByChangedAtAscIdAsc(order.getId())
                .stream()
                .map((OrderStatusHistory h) -> new OrderTrackingResponse.Step(h.getNewStatus(), h.getChangedAt()))
                .toList();
        String paymentStatus = paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId())
                .map(Payment::getStatus).map(Enum::name).orElse(null);
        return new OrderTrackingResponse(OrderMapper.code(order.getId()), order.getStatus(), order.getPaymentMethod(),
                paymentStatus, order.getDeliveryType(), receiving(order), order.getTrackingNumber(), items,
                order.getTotalAmount(), order.getOrderDate(), steps);
    }

    private static String name(String product, String variant) {
        return variant == null || variant.isBlank() ? product : product + " - " + variant;
    }

    private static String receiving(Order order) {
        if (order.getDeliveryType() == DeliveryType.PICKUP) {
            return order.getStore() != null ? order.getStore().getName() : "Cửa hàng";
        }
        return String.join(", ", Stream.of(order.getShippingWard(), order.getShippingProvince())
                .filter(part -> part != null && !part.isBlank()).toList());
    }
}
