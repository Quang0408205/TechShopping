package com.example.Tech.service.impl.aftersales;

import com.example.Tech.dto.request.aftersales.AdminServiceRequestSearchRequest;
import com.example.Tech.dto.request.aftersales.ServiceRequestUpdateRequest;
import com.example.Tech.dto.response.aftersales.ServiceRequestResponse;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.entity.aftersales.MaintenanceRequest;
import com.example.Tech.entity.aftersales.ReturnItem;
import com.example.Tech.entity.aftersales.ReturnRequest;
import com.example.Tech.entity.aftersales.ReturnStatus;
import com.example.Tech.entity.aftersales.ServiceRequestStatus;
import com.example.Tech.entity.aftersales.ServiceRequestType;
import com.example.Tech.entity.aftersales.ServiceRequestView;
import com.example.Tech.entity.aftersales.WarrantyRequest;
import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.aftersales.MaintenanceRequestRepository;
import com.example.Tech.repository.aftersales.ReturnItemRepository;
import com.example.Tech.repository.aftersales.ReturnRequestRepository;
import com.example.Tech.repository.aftersales.ServiceRequestViewRepository;
import com.example.Tech.repository.aftersales.WarrantyRequestRepository;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.inventory.StockMovementRepository;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.service.aftersales.AdminServiceRequestService;
import com.example.Tech.service.store.StoreAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.example.Tech.repository.aftersales.ServiceRequestFilterSpecifications.createdBetween;
import static com.example.Tech.repository.aftersales.ServiceRequestFilterSpecifications.matchingKeyword;
import static com.example.Tech.repository.aftersales.ServiceRequestFilterSpecifications.ofStore;
import static com.example.Tech.repository.aftersales.ServiceRequestFilterSpecifications.ofType;
import static com.example.Tech.repository.aftersales.ServiceRequestFilterSpecifications.withStatus;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceRequestServiceImpl implements AdminServiceRequestService {

    private final StoreAccessGuard storeAccessGuard;
    private final ServiceRequestViewRepository viewRepository;
    private final ServiceRequestLoader loader;
    private final WarrantyRequestRepository warrantyRequestRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnItemRepository returnItemRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final Clock clock;

    @Override
    public PageResponse<ServiceRequestResponse> search(Long staffId, AdminServiceRequestSearchRequest filter,
                                                       Pageable pageable) {
        StoreAccessGuard.OrderScope scope = storeAccessGuard.orderScope(staffId);
        if (filter.fromDate() != null && filter.toDate() != null && filter.fromDate().isAfter(filter.toDate())) {
            throw BusinessException.invalidField("fromDate", "Ngày bắt đầu phải trước ngày kết thúc");
        }
        Integer storeId = scope.admin() ? filter.storeId() : scope.storeId();
        Page<ServiceRequestView> page = viewRepository.findAll(ofType(filter.type())
                .and(withStatus(filter.status()))
                .and(ofStore(storeId))
                .and(createdBetween(filter.fromDate(), filter.toDate()))
                .and(matchingKeyword(filter.keyword())), pageable);
        return PageResponse.from(new PageImpl<>(loader.loadRows(page.getContent()), page.getPageable(),
                page.getTotalElements()));
    }

    @Override
    public ServiceRequestResponse get(Long staffId, ServiceRequestType type, Long id) {
        StoreAccessGuard.OrderScope scope = storeAccessGuard.orderScope(staffId);
        ServiceRequestView row = viewRepository.findById(ServiceRequestView.viewId(type, id))
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_REQUEST_NOT_FOUND));
        if (!scope.admin() && !scope.storeId().equals(row.getStoreId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Yêu cầu không thuộc chi nhánh của bạn");
        }
        return loader.load(type, id);
    }

    @Override
    @Transactional
    public ServiceRequestResponse update(Long staffId, ServiceRequestType type, Long id,
                                         ServiceRequestUpdateRequest request) {
        StoreAccessGuard.OrderScope scope = storeAccessGuard.orderScope(staffId);
        User staff = scope.user();
        LocalDateTime now = LocalDateTime.now(clock);
        switch (type) {
            case WARRANTY -> {
                WarrantyRequest r = warrantyRequestRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
                scope.check(r.getWarranty().getOrderItem().getOrder());
                refuseFields(request.estimatedCost() != null, "estimatedCost", "Chỉ yêu cầu bảo trì có chi phí");
                refuseFields(request.actualCost() != null, "actualCost", "Chỉ yêu cầu bảo trì có chi phí");
                refuseRestock(request);
                ServiceRequestStatus next = parse(request.status(), ServiceRequestStatus.class);
                checkRepairFlow(r.getStatus(), next);
                String reason = next == ServiceRequestStatus.REJECTED ? requiredReason(request) : null;
                applyRepairFields(request, r::setNotes, r::setEstimatedCompletionDate);
                if (next != null) {
                    r.setStatus(next);
                    if (r.getAssignedTo() == null) {
                        r.setAssignedTo(staff);
                    }
                    switch (next) {
                        case RECEIVED -> r.setReceivedAt(now);
                        case COMPLETED -> r.setCompletedAt(now);
                        case REJECTED -> r.setRejectionReason(reason);
                        default -> { }
                    }
                }
            }
            case MAINTENANCE -> {
                MaintenanceRequest r = maintenanceRequestRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
                scope.check(r.getOrderItem().getOrder());
                refuseRestock(request);
                ServiceRequestStatus next = parse(request.status(), ServiceRequestStatus.class);
                checkRepairFlow(r.getStatus(), next);
                String reason = next == ServiceRequestStatus.REJECTED ? requiredReason(request) : null;
                applyRepairFields(request, r::setNotes, r::setEstimatedCompletionDate);
                if (request.estimatedCost() != null) {
                    r.setEstimatedCost(request.estimatedCost());
                }
                if (request.actualCost() != null) {
                    r.setActualCost(request.actualCost());
                }
                if (next != null) {
                    r.setStatus(next);
                    if (r.getAssignedTo() == null) {
                        r.setAssignedTo(staff);
                    }
                    switch (next) {
                        case RECEIVED -> r.setReceivedAt(now);
                        case COMPLETED -> r.setCompletionDate(now.toLocalDate());
                        case REJECTED -> r.setRejectionReason(reason);
                        default -> { }
                    }
                }
            }
            case RETURN -> {
                ReturnRequest r = returnRequestRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
                Order order = r.getOrder();
                scope.check(order);
                refuseFields(request.notes() != null, "notes", "Yêu cầu trả hàng không có ghi chú xử lý");
                refuseFields(request.estimatedCompletionDate() != null, "estimatedCompletionDate",
                        "Yêu cầu trả hàng không có ngày dự kiến");
                refuseFields(request.estimatedCost() != null || request.actualCost() != null, "estimatedCost",
                        "Chỉ yêu cầu bảo trì có chi phí");
                ReturnStatus next = parse(request.status(), ReturnStatus.class);
                if (next == null) {
                    throw BusinessException.invalidField("status", "Vui lòng chọn trạng thái mới");
                }
                if (!r.getStatus().canMoveTo(next)) {
                    throw wrongFlow(r.getStatus().name(), next.name());
                }
                if (next != ReturnStatus.RECEIVED) {
                    refuseRestock(request);
                }
                // every check before the first change
                String reason = next == ReturnStatus.REJECTED ? requiredReason(request) : null;
                List<ReturnItem> items = returnItemRepository.findAllByReturnRequest_IdIn(List.of(r.getId()));
                Set<Long> chosen = next == ReturnStatus.RECEIVED
                        ? checkedRestock(items, order, request.restockOrderItemIds()) : Set.of();
                r.setStatus(next);
                if (r.getAssignedTo() == null) {
                    r.setAssignedTo(staff);
                }
                switch (next) {
                    case APPROVED -> r.setApprovedAt(now);
                    case REJECTED -> r.setRejectionReason(reason);
                    case RECEIVED -> {
                        r.setReceivedAt(now);
                        restock(r, order, items, chosen, staff);
                    }
                    case REFUNDED -> r.setCompletedAt(now);
                    default -> { }
                }
                if (next == ReturnStatus.REFUNDED) {
                    returnRequestRepository.flush();
                    // the refunded amount no longer counts as the customer's spending (clears the persistence context)
                    customerProfileRepository.addToTotalSpent(r.getUser().getId(), r.getRefundAmount().negate());
                }
            }
        }
        log.info("Staff id={} updated {} request id={} (status {})", staffId, type, id, request.status());
        return loader.load(type, id);
    }

    private static Set<Long> checkedRestock(List<ReturnItem> items, Order order, List<Long> restockIds) {
        Set<Long> chosen = restockIds == null ? Set.of() : Set.copyOf(restockIds);
        Set<Long> lineIds = items.stream().map(item -> item.getOrderItem().getId()).collect(Collectors.toSet());
        if (!lineIds.containsAll(chosen)) {
            throw BusinessException.invalidField("restockOrderItemIds", "Sản phẩm không thuộc yêu cầu trả hàng này");
        }
        if (!chosen.isEmpty() && order.getStore() == null) {
            throw BusinessException.invalidField("restockOrderItemIds", "Đơn không có chi nhánh nên không cộng lại kho được");
        }
        return chosen;
    }

    /** Puts the chosen returned lines back into the order's store stock (RETURN movements); the others stay out. */
    private void restock(ReturnRequest r, Order order, List<ReturnItem> items, Set<Long> chosen, User staff) {
        Store store = order.getStore();
        Map<Long, Integer> quantityByVariant = new TreeMap<>();
        Map<Long, ReturnItem> itemByVariant = new HashMap<>();
        for (ReturnItem item : items) {
            boolean back = chosen.contains(item.getOrderItem().getId());
            item.setRestocked(back);
            if (back) {
                Long variantId = item.getOrderItem().getVariant().getId();
                quantityByVariant.merge(variantId, item.getQuantity(), Integer::sum);
                itemByVariant.put(variantId, item);
            }
        }
        if (quantityByVariant.isEmpty()) {
            return;
        }
        quantityByVariant.keySet().forEach(variantId -> inventoryRepository.insertIfMissing(store.getId(), variantId));
        // same lock order as stock-in / order confirm (variant id ascending)
        Map<Long, Inventory> rows = inventoryRepository.findAllForUpdate(store.getId(), List.copyOf(quantityByVariant.keySet()))
                .stream().collect(Collectors.toMap(row -> row.getId().getVariantId(), row -> row));
        String note = "Trả hàng " + ServiceRequestType.RETURN.code(r.getId());
        quantityByVariant.forEach((variantId, quantity) -> {
            Inventory row = rows.get(variantId);
            row.setQuantity(row.getQuantity() + quantity);
            StockMovement movement = new StockMovement();
            movement.setStore(store);
            movement.setVariant(itemByVariant.get(variantId).getOrderItem().getVariant());
            movement.setMovementType(MovementType.RETURN);
            movement.setQuantityChange(quantity);
            movement.setOrder(order);
            movement.setNote(note);
            movement.setCreatedBy(staff);
            stockMovementRepository.save(movement);
        });
    }

    private static void checkRepairFlow(ServiceRequestStatus current, ServiceRequestStatus next) {
        if (next == null) {
            if (!current.isOpen()) {
                throw new BusinessException(ErrorCode.INVALID_SERVICE_REQUEST_STATUS, "Yêu cầu đã kết thúc, không sửa được");
            }
            return;
        }
        if (!current.canMoveTo(next)) {
            throw wrongFlow(current.name(), next.name());
        }
    }

    private static void applyRepairFields(ServiceRequestUpdateRequest request,
                                          java.util.function.Consumer<String> notes,
                                          java.util.function.Consumer<LocalDate> estimatedDate) {
        if (request.notes() != null) {
            notes.accept(request.notes().isBlank() ? null : request.notes().trim());
        }
        if (request.estimatedCompletionDate() != null) {
            estimatedDate.accept(request.estimatedCompletionDate());
        }
    }

    private static String requiredReason(ServiceRequestUpdateRequest request) {
        if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
            throw BusinessException.invalidField("rejectionReason", "Vui lòng nhập lý do từ chối (khách sẽ thấy lý do này)");
        }
        return request.rejectionReason().trim();
    }

    private static void refuseRestock(ServiceRequestUpdateRequest request) {
        refuseFields(request.restockOrderItemIds() != null && !request.restockOrderItemIds().isEmpty(),
                "restockOrderItemIds", "Chỉ chọn hàng cộng lại kho khi nhận hàng trả về");
    }

    private static void refuseFields(boolean present, String field, String message) {
        if (present) {
            throw BusinessException.invalidField(field, message);
        }
    }

    private static <E extends Enum<E>> E parse(String status, Class<E> type) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalidField("status", "Trạng thái không hợp lệ");
        }
    }

    private static BusinessException wrongFlow(String from, String to) {
        return new BusinessException(ErrorCode.INVALID_SERVICE_REQUEST_STATUS,
                "Không chuyển được yêu cầu từ %s sang %s".formatted(from, to));
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.SERVICE_REQUEST_NOT_FOUND);
    }
}
