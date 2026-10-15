package com.example.Tech.service.impl.aftersales;

import com.example.Tech.dto.response.aftersales.ServiceRequestResponse;
import com.example.Tech.entity.aftersales.MaintenanceRequest;
import com.example.Tech.entity.aftersales.ReturnItem;
import com.example.Tech.entity.aftersales.ReturnRequest;
import com.example.Tech.entity.aftersales.ServiceRequestImage;
import com.example.Tech.entity.aftersales.ServiceRequestType;
import com.example.Tech.entity.aftersales.ServiceRequestView;
import com.example.Tech.entity.aftersales.WarrantyRequest;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.aftersales.MaintenanceRequestRepository;
import com.example.Tech.repository.aftersales.ReturnItemRepository;
import com.example.Tech.repository.aftersales.ReturnRequestRepository;
import com.example.Tech.repository.aftersales.ServiceRequestImageRepository;
import com.example.Tech.repository.aftersales.WarrantyRequestRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Maps requests of any type to {@link ServiceRequestResponse}, loading each type in a few batch queries. */
@Component
@RequiredArgsConstructor
public class ServiceRequestLoader {

    public record Key(ServiceRequestType type, Long id) {
    }

    private final WarrantyRequestRepository warrantyRequestRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ServiceRequestImageRepository imageRepository;
    private final ProductImageRepository productImageRepository;

    public ServiceRequestResponse load(ServiceRequestType type, Long id) {
        List<ServiceRequestResponse> loaded = load(List.of(new Key(type, id)));
        if (loaded.isEmpty()) {
            throw new BusinessException(ErrorCode.SERVICE_REQUEST_NOT_FOUND);
        }
        return loaded.getFirst();
    }

    public List<ServiceRequestResponse> loadRows(List<ServiceRequestView> rows) {
        return load(rows.stream().map(row -> new Key(row.getRequestType(), row.getRequestId())).toList());
    }

    /** Same order as the keys; unknown ids are skipped. */
    public List<ServiceRequestResponse> load(List<Key> keys) {
        Map<ServiceRequestType, List<Long>> idsByType = keys.stream().collect(Collectors.groupingBy(Key::type,
                Collectors.mapping(Key::id, Collectors.toList())));

        List<WarrantyRequest> warranties = idsByType.containsKey(ServiceRequestType.WARRANTY)
                ? warrantyRequestRepository.findAllByIdIn(idsByType.get(ServiceRequestType.WARRANTY)) : List.of();
        List<MaintenanceRequest> maintenances = idsByType.containsKey(ServiceRequestType.MAINTENANCE)
                ? maintenanceRequestRepository.findAllByIdIn(idsByType.get(ServiceRequestType.MAINTENANCE)) : List.of();
        List<ReturnRequest> returns = idsByType.containsKey(ServiceRequestType.RETURN)
                ? returnRequestRepository.findAllByIdIn(idsByType.get(ServiceRequestType.RETURN)) : List.of();
        Map<Long, List<ReturnItem>> returnItems = returns.isEmpty() ? Map.of()
                : returnItemRepository.findAllByReturnRequest_IdIn(ids(returns, ReturnRequest::getId)).stream()
                .collect(Collectors.groupingBy(item -> item.getReturnRequest().getId()));

        Map<Long, String> productImages = productImages(Stream.of(
                warranties.stream().map(r -> r.getWarranty().getOrderItem()),
                maintenances.stream().map(MaintenanceRequest::getOrderItem),
                returnItems.values().stream().flatMap(List::stream).map(ReturnItem::getOrderItem)).flatMap(s -> s));

        Map<Key, ServiceRequestResponse> byKey = new HashMap<>();
        Map<Long, List<String>> warrantyPhotos = photos(warranties.isEmpty() ? List.of()
                : imageRepository.findAllByWarrantyRequest_IdInOrderByDisplayOrderAsc(ids(warranties, WarrantyRequest::getId)),
                image -> image.getWarrantyRequest().getId());
        warranties.forEach(r -> byKey.put(new Key(ServiceRequestType.WARRANTY, r.getId()),
                warranty(r, warrantyPhotos.getOrDefault(r.getId(), List.of()), productImages)));
        Map<Long, List<String>> maintenancePhotos = photos(maintenances.isEmpty() ? List.of()
                : imageRepository.findAllByMaintenanceRequest_IdInOrderByDisplayOrderAsc(ids(maintenances, MaintenanceRequest::getId)),
                image -> image.getMaintenanceRequest().getId());
        maintenances.forEach(r -> byKey.put(new Key(ServiceRequestType.MAINTENANCE, r.getId()),
                maintenance(r, maintenancePhotos.getOrDefault(r.getId(), List.of()), productImages)));
        Map<Long, List<String>> returnPhotos = photos(returns.isEmpty() ? List.of()
                : imageRepository.findAllByReturnRequest_IdInOrderByDisplayOrderAsc(ids(returns, ReturnRequest::getId)),
                image -> image.getReturnRequest().getId());
        returns.forEach(r -> byKey.put(new Key(ServiceRequestType.RETURN, r.getId()),
                returnResponse(r, returnItems.getOrDefault(r.getId(), List.of()),
                        returnPhotos.getOrDefault(r.getId(), List.of()), productImages)));

        return keys.stream().map(byKey::get).filter(response -> response != null).toList();
    }

    private ServiceRequestResponse warranty(WarrantyRequest r, List<String> photos, Map<Long, String> productImages) {
        OrderItem line = r.getWarranty().getOrderItem();
        Order order = line.getOrder();
        return new ServiceRequestResponse(ServiceRequestType.WARRANTY, r.getId(),
                ServiceRequestType.WARRANTY.code(r.getId()), r.getStatus().name(),
                r.getStatus().canBeCancelledByCustomer(), order.getId(), OrderMapper.code(order.getId()),
                storeName(order), r.getUser().getId(), displayName(r.getUser()), r.getUser().getEmail(),
                order.getRecipientPhone(),
                List.of(item(line, line.getQuantity(), null, null, productImages)), r.getIssueDescription(), null,
                null, photos, r.getWarranty().getEndDate(), r.getEstimatedCompletionDate(), null, null, null,
                r.getRejectionReason(), r.getNotes(), handler(r.getAssignedTo()), r.getCreatedAt(), null,
                r.getReceivedAt(), r.getCompletedAt(), r.getCancelledAt(), r.getUpdatedAt());
    }

    private ServiceRequestResponse maintenance(MaintenanceRequest r, List<String> photos,
                                               Map<Long, String> productImages) {
        OrderItem line = r.getOrderItem();
        Order order = line.getOrder();
        // completion_date is a legacy DATE column
        LocalDateTime completedAt = r.getCompletionDate() != null ? r.getCompletionDate().atStartOfDay() : null;
        return new ServiceRequestResponse(ServiceRequestType.MAINTENANCE, r.getId(),
                ServiceRequestType.MAINTENANCE.code(r.getId()), r.getStatus().name(),
                r.getStatus().canBeCancelledByCustomer(), order.getId(), OrderMapper.code(order.getId()),
                storeName(order), r.getUser().getId(), displayName(r.getUser()), r.getUser().getEmail(),
                order.getRecipientPhone(),
                List.of(item(line, line.getQuantity(), null, null, productImages)), r.getDescription(),
                r.getMaintenanceType(), null, photos, null, r.getEstimatedCompletionDate(), r.getEstimatedCost(),
                r.getActualCost(), null, r.getRejectionReason(), r.getNotes(), handler(r.getAssignedTo()),
                r.getCreatedAt(), null, r.getReceivedAt(), completedAt, r.getCancelledAt(), r.getUpdatedAt());
    }

    private ServiceRequestResponse returnResponse(ReturnRequest r, List<ReturnItem> items, List<String> photos,
                                                  Map<Long, String> productImages) {
        Order order = r.getOrder();
        List<ServiceRequestResponse.Item> lines = items.stream()
                .sorted((a, b) -> a.getOrderItem().getId().compareTo(b.getOrderItem().getId()))
                .map(i -> item(i.getOrderItem(), i.getQuantity(), i.getRefundAmount(), i.getRestocked(), productImages))
                .toList();
        return new ServiceRequestResponse(ServiceRequestType.RETURN, r.getId(),
                ServiceRequestType.RETURN.code(r.getId()), r.getStatus().name(),
                r.getStatus().canBeCancelledByCustomer(), order.getId(), OrderMapper.code(order.getId()),
                storeName(order), r.getUser().getId(), displayName(r.getUser()), r.getUser().getEmail(),
                order.getRecipientPhone(), lines,
                r.getReason(), null, r.getReasonType(), photos, null, null, null, null, r.getRefundAmount(),
                r.getRejectionReason(), null, handler(r.getAssignedTo()), r.getCreatedAt(), r.getApprovedAt(),
                r.getReceivedAt(), r.getCompletedAt(), r.getCancelledAt(), r.getUpdatedAt());
    }

    private static ServiceRequestResponse.Item item(OrderItem line, Integer quantity, java.math.BigDecimal refund,
                                                    Boolean restocked, Map<Long, String> productImages) {
        Long productId = line.getVariant().getProduct().getId();
        return new ServiceRequestResponse.Item(line.getId(), productId, line.getVariant().getProduct().getName(),
                line.getVariant().getVariantName(), productImages.get(productId), quantity, refund, restocked);
    }

    private Map<Long, String> productImages(Stream<OrderItem> lines) {
        List<Long> productIds = lines.map(line -> line.getVariant().getProduct().getId()).distinct().toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> byProduct = new HashMap<>();
        for (ProductImage image : productImageRepository.findAllByProductIdInBestFirst(productIds)) {
            byProduct.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
        }
        return byProduct;
    }

    private static Map<Long, List<String>> photos(List<ServiceRequestImage> images,
                                                  Function<ServiceRequestImage, Long> owner) {
        Map<Long, List<String>> byOwner = new HashMap<>();
        images.forEach(image -> byOwner.computeIfAbsent(owner.apply(image), id -> new ArrayList<>())
                .add(image.getImageUrl()));
        return byOwner;
    }

    private static <T> Collection<Long> ids(List<T> rows, Function<T, Long> id) {
        return rows.stream().map(id).toList();
    }

    private static String storeName(Order order) {
        return order.getStore() != null ? order.getStore().getName() : null;
    }

    static String displayName(User user) {
        return user.getFullname() != null && !user.getFullname().isBlank() ? user.getFullname() : user.getUsername();
    }

    private static String handler(User user) {
        return user != null ? displayName(user) : null;
    }
}
