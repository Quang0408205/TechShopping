package com.example.Tech.service.impl.aftersales;

import com.example.Tech.dto.request.aftersales.MaintenanceRequestCreateRequest;
import com.example.Tech.dto.request.aftersales.ReturnRequestCreateRequest;
import com.example.Tech.dto.request.aftersales.ServiceRequestSearchRequest;
import com.example.Tech.dto.request.aftersales.WarrantyRequestCreateRequest;
import com.example.Tech.dto.response.aftersales.AfterSalesOrderResponse;
import com.example.Tech.dto.response.aftersales.ServiceRequestResponse;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.entity.aftersales.MaintenanceRequest;
import com.example.Tech.entity.aftersales.ReturnItem;
import com.example.Tech.entity.aftersales.ReturnRequest;
import com.example.Tech.entity.aftersales.ReturnStatus;
import com.example.Tech.entity.aftersales.ServiceRequestStatus;
import com.example.Tech.entity.aftersales.ServiceRequestType;
import com.example.Tech.entity.aftersales.ServiceRequestView;
import com.example.Tech.entity.aftersales.Warranty;
import com.example.Tech.entity.aftersales.WarrantyRequest;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.aftersales.MaintenanceRequestRepository;
import com.example.Tech.repository.aftersales.ReturnItemRepository;
import com.example.Tech.repository.aftersales.ReturnRequestRepository;
import com.example.Tech.repository.aftersales.ServiceRequestViewRepository;
import com.example.Tech.repository.aftersales.WarrantyRepository;
import com.example.Tech.repository.aftersales.WarrantyRequestRepository;
import com.example.Tech.repository.order.OrderItemRepository;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.service.aftersales.AfterSalesService;
import com.example.Tech.service.upload.ImageStorageService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.Tech.repository.aftersales.ServiceRequestFilterSpecifications.ofType;
import static com.example.Tech.repository.aftersales.ServiceRequestFilterSpecifications.ofUser;
import static com.example.Tech.repository.aftersales.ServiceRequestFilterSpecifications.withStatus;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AfterSalesServiceImpl implements AfterSalesService {

    static final int RETURN_WINDOW_DAYS = 7;
    static final int MAX_IMAGES = 5;
    static final int TEXT_MIN_LENGTH = 10;
    static final int TEXT_MAX_LENGTH = 2000;
    static final List<ServiceRequestStatus> OPEN =
            List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.RECEIVED, ServiceRequestStatus.PROCESSING);

    private final CurrentUserLoader currentUserLoader;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WarrantyRepository warrantyRepository;
    private final WarrantyRequestRepository warrantyRequestRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ServiceRequestViewRepository viewRepository;
    private final ServiceRequestLoader loader;
    private final ProductImageRepository productImageRepository;
    private final ImageStorageService imageStorageService;
    private final Clock clock;

    @Override
    public AfterSalesOrderResponse orderAfterSales(Long userId, Long orderId) {
        currentUserLoader.load(userId);
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        requireDelivered(order);

        List<OrderItem> lines = orderItemRepository.findAllWithProductByOrderIdIn(List.of(orderId));
        List<Long> lineIds = lines.stream().map(OrderItem::getId).toList();
        Map<Long, Warranty> warrantyByLine = warrantyRepository.findAllByOrderItem_IdIn(lineIds).stream()
                .collect(Collectors.toMap(w -> w.getOrderItem().getId(), Function.identity()));
        Map<Long, Long> lineByWarranty = warrantyByLine.values().stream()
                .collect(Collectors.toMap(Warranty::getId, w -> w.getOrderItem().getId()));
        Map<Long, Long> openWarrantyByLine = new HashMap<>();
        if (!lineByWarranty.isEmpty()) {
            warrantyRequestRepository.findAllByWarranty_IdInAndStatusIn(lineByWarranty.keySet(), OPEN)
                    .forEach(r -> openWarrantyByLine.put(lineByWarranty.get(r.getWarranty().getId()), r.getId()));
        }
        Map<Long, Long> openMaintenanceByLine = new HashMap<>();
        maintenanceRequestRepository.findAllByOrderItem_IdInAndStatusIn(lineIds, OPEN)
                .forEach(r -> openMaintenanceByLine.put(r.getOrderItem().getId(), r.getId()));
        Map<Long, Long> held = heldQuantities(lineIds);
        Map<Long, String> images = productImages(lines);

        LocalDate today = LocalDate.now(clock);
        String returnUnavailable = returnUnavailableReason(order, LocalDateTime.now(clock));
        List<AfterSalesOrderResponse.Item> items = lines.stream().map(line -> {
            Warranty warranty = warrantyByLine.get(line.getId());
            int returnable = returnUnavailable != null ? 0
                    : (int) Math.max(0, line.getQuantity() - held.getOrDefault(line.getId(), 0L));
            Long productId = line.getVariant().getProduct().getId();
            return new AfterSalesOrderResponse.Item(line.getId(), productId, line.getVariant().getProduct().getName(),
                    line.getVariant().getVariantName(), images.get(productId), line.getQuantity(), line.getUnitPrice(),
                    warranty != null ? warranty.getStartDate() : null, warranty != null ? warranty.getEndDate() : null,
                    warranty != null && warranty.isValidOn(today), openWarrantyByLine.get(line.getId()),
                    openMaintenanceByLine.get(line.getId()), returnable);
        }).toList();

        return new AfterSalesOrderResponse(order.getId(), OrderMapper.code(order.getId()), order.getDeliveredAt(),
                order.getDeliveredAt() != null ? order.getDeliveredAt().plusDays(RETURN_WINDOW_DAYS) : null,
                returnUnavailable == null, returnUnavailable, items);
    }

    @Override
    @Transactional
    public ServiceRequestResponse createWarrantyRequest(Long userId, WarrantyRequestCreateRequest request) {
        String description = checkedText("description", request.description());
        List<String> imageUrls = checkedImageUrls(request.imageUrls());
        User user = currentUserLoader.load(userId);
        OrderItem line = ownedLine(request.orderItemId(), userId);
        lockDelivered(line.getOrder().getId());

        Warranty warranty = warrantyRepository.findByOrderItem_Id(line.getId())
                .filter(w -> w.isValidOn(LocalDate.now(clock)))
                .orElseThrow(() -> new BusinessException(ErrorCode.WARRANTY_NOT_VALID));
        if (warrantyRequestRepository.existsByWarranty_IdAndStatusIn(warranty.getId(), OPEN)) {
            throw new BusinessException(ErrorCode.SERVICE_REQUEST_ALREADY_OPEN);
        }
        WarrantyRequest saved = new WarrantyRequest();
        saved.setWarranty(warranty);
        saved.setUser(user);
        saved.setIssueDescription(description);
        imageUrls.forEach(saved::addImage);
        saved = warrantyRequestRepository.saveAndFlush(saved);
        log.info("User id={} opened warranty request id={} for order line id={}", userId, saved.getId(), line.getId());
        return loader.load(ServiceRequestType.WARRANTY, saved.getId());
    }

    @Override
    @Transactional
    public ServiceRequestResponse createMaintenanceRequest(Long userId, MaintenanceRequestCreateRequest request) {
        String description = checkedText("description", request.description());
        List<String> imageUrls = checkedImageUrls(request.imageUrls());
        User user = currentUserLoader.load(userId);
        OrderItem line = ownedLine(request.orderItemId(), userId);
        lockDelivered(line.getOrder().getId());

        if (maintenanceRequestRepository.existsByOrderItem_IdAndStatusIn(line.getId(), OPEN)) {
            throw new BusinessException(ErrorCode.SERVICE_REQUEST_ALREADY_OPEN);
        }
        MaintenanceRequest saved = new MaintenanceRequest();
        saved.setUser(user);
        saved.setOrderItem(line);
        saved.setMaintenanceType(request.maintenanceType());
        saved.setDescription(description);
        imageUrls.forEach(saved::addImage);
        saved = maintenanceRequestRepository.saveAndFlush(saved);
        log.info("User id={} opened maintenance request id={} for order line id={}", userId, saved.getId(), line.getId());
        return loader.load(ServiceRequestType.MAINTENANCE, saved.getId());
    }

    @Override
    @Transactional
    public ServiceRequestResponse createReturnRequest(Long userId, ReturnRequestCreateRequest request) {
        String reason = checkedText("reason", request.reason());
        List<String> imageUrls = checkedImageUrls(request.imageUrls());
        Set<Long> requestedLines = new HashSet<>();
        for (ReturnRequestCreateRequest.Item item : request.items()) {
            if (!requestedLines.add(item.orderItemId())) {
                throw BusinessException.invalidField("items", "Mỗi sản phẩm chỉ chọn một lần");
            }
        }
        User user = currentUserLoader.load(userId);
        Order order = orderRepository.findByIdAndUserId(request.orderId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        // the order row lock serialises two return requests of the same order (no over-returning)
        lockDelivered(order.getId());
        String unavailable = returnUnavailableReason(order, LocalDateTime.now(clock));
        if (unavailable != null) {
            throw new BusinessException(ErrorCode.RETURN_NOT_AVAILABLE, unavailable);
        }

        Map<Long, OrderItem> linesById = orderItemRepository.findAllWithProductByOrderIdIn(List.of(order.getId()))
                .stream().collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        Map<Long, Long> held = heldQuantities(linesById.keySet());
        ReturnRequest saved = new ReturnRequest();
        saved.setOrder(order);
        saved.setUser(user);
        saved.setReasonType(request.reasonType());
        saved.setReason(reason);
        BigDecimal total = BigDecimal.ZERO;
        for (ReturnRequestCreateRequest.Item item : request.items()) {
            OrderItem line = linesById.get(item.orderItemId());
            if (line == null) {
                throw BusinessException.invalidField("items", "Sản phẩm không thuộc đơn hàng này");
            }
            long left = line.getQuantity() - held.getOrDefault(line.getId(), 0L);
            if (item.quantity() > left) {
                throw BusinessException.invalidField("items", "Chỉ còn %d sản phẩm \"%s\" có thể trả"
                        .formatted(Math.max(left, 0), line.getVariant().getProduct().getName()));
            }
            // unit_price is what the customer actually paid per unit (promotions included)
            BigDecimal refund = line.getUnitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            saved.addItem(new ReturnItem(line, item.quantity(), refund));
            total = total.add(refund);
        }
        saved.setRefundAmount(total);
        imageUrls.forEach(saved::addImage);
        saved = returnRequestRepository.saveAndFlush(saved);
        log.info("User id={} opened return request id={} for order id={} ({} lines, refund {})",
                userId, saved.getId(), order.getId(), request.items().size(), total);
        return loader.load(ServiceRequestType.RETURN, saved.getId());
    }

    @Override
    public PageResponse<ServiceRequestResponse> mine(Long userId, ServiceRequestSearchRequest filter,
                                                     Pageable pageable) {
        currentUserLoader.load(userId);
        Page<ServiceRequestView> page = viewRepository.findAll(
                ofUser(userId).and(ofType(filter.type())).and(withStatus(filter.status())), pageable);
        return PageResponse.from(new PageImpl<>(loader.loadRows(page.getContent()), page.getPageable(),
                page.getTotalElements()));
    }

    @Override
    public ServiceRequestResponse getMine(Long userId, ServiceRequestType type, Long id) {
        currentUserLoader.load(userId);
        viewRepository.findById(ServiceRequestView.viewId(type, id))
                .filter(row -> row.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_REQUEST_NOT_FOUND));
        return loader.load(type, id);
    }

    @Override
    @Transactional
    public ServiceRequestResponse cancel(Long userId, ServiceRequestType type, Long id) {
        currentUserLoader.load(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        switch (type) {
            case WARRANTY -> {
                WarrantyRequest r = warrantyRequestRepository.findByIdForUpdate(id)
                        .filter(found -> found.getUser().getId().equals(userId)).orElseThrow(this::notFound);
                requireCancellable(r.getStatus().canBeCancelledByCustomer(), "Chỉ huỷ được yêu cầu đang chờ tiếp nhận");
                r.setStatus(ServiceRequestStatus.CANCELLED);
                r.setCancelledAt(now);
            }
            case MAINTENANCE -> {
                MaintenanceRequest r = maintenanceRequestRepository.findByIdForUpdate(id)
                        .filter(found -> found.getUser().getId().equals(userId)).orElseThrow(this::notFound);
                requireCancellable(r.getStatus().canBeCancelledByCustomer(), "Chỉ huỷ được yêu cầu đang chờ tiếp nhận");
                r.setStatus(ServiceRequestStatus.CANCELLED);
                r.setCancelledAt(now);
            }
            case RETURN -> {
                ReturnRequest r = returnRequestRepository.findByIdForUpdate(id)
                        .filter(found -> found.getUser().getId().equals(userId)).orElseThrow(this::notFound);
                requireCancellable(r.getStatus().canBeCancelledByCustomer(), "Chỉ huỷ được yêu cầu đang chờ duyệt");
                r.setStatus(ReturnStatus.CANCELLED);
                r.setCancelledAt(now);
            }
        }
        log.info("User id={} cancelled {} request id={}", userId, type, id);
        return loader.load(type, id);
    }

    /** Null when the order can still be returned on the web, else the reason shown to the customer. */
    static String returnUnavailableReason(Order order, LocalDateTime now) {
        if (order.getPaymentMethod() == PaymentMethod.INSTALLMENT) {
            return "Đơn trả góp không trả hàng qua website; vui lòng liên hệ cửa hàng";
        }
        if (order.getDeliveredAt() == null || now.isAfter(order.getDeliveredAt().plusDays(RETURN_WINDOW_DAYS))) {
            return "Đã quá %d ngày kể từ ngày nhận hàng nên không trả hàng được".formatted(RETURN_WINDOW_DAYS);
        }
        return null;
    }

    private OrderItem ownedLine(Long orderItemId, Long userId) {
        return orderItemRepository.findById(orderItemId)
                .filter(line -> line.getOrder().getUser().getId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Không tìm thấy sản phẩm trong đơn hàng của bạn"));
    }

    private void lockDelivered(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        requireDelivered(order);
    }

    private static void requireDelivered(Order order) {
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.AFTER_SALES_NOT_AVAILABLE);
        }
    }

    private static void requireCancellable(boolean cancellable, String message) {
        if (!cancellable) {
            throw new BusinessException(ErrorCode.INVALID_SERVICE_REQUEST_STATUS, message);
        }
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.SERVICE_REQUEST_NOT_FOUND);
    }

    private Map<Long, Long> heldQuantities(java.util.Collection<Long> lineIds) {
        if (lineIds.isEmpty()) {
            return Map.of();
        }
        return returnItemRepository.sumHeldQuantities(lineIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
    }

    private Map<Long, String> productImages(List<OrderItem> lines) {
        List<Long> productIds = lines.stream().map(l -> l.getVariant().getProduct().getId()).distinct().toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> byProduct = new HashMap<>();
        for (ProductImage image : productImageRepository.findAllByProductIdInBestFirst(productIds)) {
            byProduct.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
        }
        return byProduct;
    }

    private static String checkedText(String field, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() < TEXT_MIN_LENGTH || trimmed.length() > TEXT_MAX_LENGTH) {
            throw BusinessException.invalidField(field, "Mô tả từ %d đến %d ký tự"
                    .formatted(TEXT_MIN_LENGTH, TEXT_MAX_LENGTH));
        }
        return trimmed;
    }

    /** Only photos this server stored for after-sales requests, no duplicates, at most 5. */
    private List<String> checkedImageUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        if (new LinkedHashSet<>(urls).size() != urls.size()) {
            throw BusinessException.invalidField("imageUrls", "Một ảnh chỉ được gửi một lần");
        }
        if (urls.size() > MAX_IMAGES) {
            throw BusinessException.invalidField("imageUrls", "Tối đa %d ảnh cho một yêu cầu".formatted(MAX_IMAGES));
        }
        if (!urls.stream().allMatch(imageStorageService::isStoredServiceImage)) {
            throw BusinessException.invalidField("imageUrls", "Chỉ dùng ảnh tải lên từ trang gửi yêu cầu (JPG, PNG, WebP)");
        }
        return new ArrayList<>(urls);
    }
}
