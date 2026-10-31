package com.example.Tech.service.inventory;

import com.example.Tech.dto.request.inventory.StockTransferRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.StockTransferResponse;
import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import com.example.Tech.entity.inventory.StockTransfer;
import com.example.Tech.entity.inventory.StockTransferItem;
import com.example.Tech.entity.inventory.StockTransferStatus;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.inventory.StockMovementRepository;
import com.example.Tech.repository.inventory.StockTransferItemRepository;
import com.example.Tech.repository.inventory.StockTransferRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.service.store.StoreAccessGuard;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transfers of goods between stores. Drafting, sending and cancelling belong to the sending store; receiving belongs to
 * the receiving store; an ADMIN may do all of it (StoreAccessGuard). Sending takes the goods out of the sending store
 * at once, so the quantity on the road is never counted as stock anywhere.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTransferService {

    public enum Direction {
        OUTGOING,
        INCOMING
    }

    private final StockTransferRepository transferRepository;
    private final StockTransferItemRepository itemRepository;
    private final StoreRepository storeRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StoreAccessGuard storeAccessGuard;
    private final Clock clock;

    /**
     * A staff member sees the transfers that leave from or arrive at their own store; an ADMIN sees every transfer
     * (optionally only those of {@code storeId}). {@code direction} narrows it to one side.
     */
    public PageResponse<StockTransferResponse> list(Long userId, Direction direction, StockTransferStatus status,
                                                    Integer storeId, Pageable pageable) {
        StoreAccessGuard.OrderScope scope = storeAccessGuard.orderScope(userId);
        Integer ownStore = scope.admin() ? storeId : scope.storeId();
        Specification<StockTransfer> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (ownStore != null) {
                Predicate from = cb.equal(root.get("fromStore").get("id"), ownStore);
                // a draft is the sending store's private work: the receiving store sees a transfer once it is sent
                Predicate to = cb.and(cb.equal(root.get("toStore").get("id"), ownStore),
                        cb.notEqual(root.get("status"), StockTransferStatus.DRAFT));
                parts.add(direction == Direction.OUTGOING ? from : direction == Direction.INCOMING ? to : cb.or(from, to));
            }
            if (status != null) {
                parts.add(cb.equal(root.get("status"), status));
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
        var page = transferRepository.findAll(spec, pageable);
        Map<Long, List<StockTransferItem>> items = itemsOf(page.getContent());
        return PageResponse.from(page.map(t -> toResponse(t, items.getOrDefault(t.getId(), List.of()), false)));
    }

    public StockTransferResponse get(Long userId, Long transferId) {
        StoreAccessGuard.OrderScope scope = storeAccessGuard.orderScope(userId);
        StockTransfer transfer = transferRepository.findWithDetailsById(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_TRANSFER_NOT_FOUND));
        if (!scope.admin()) {
            boolean sender = scope.storeId().equals(transfer.getFromStore().getId());
            boolean receiver = scope.storeId().equals(transfer.getToStore().getId())
                    && transfer.getStatus() != StockTransferStatus.DRAFT;
            if (!sender && !receiver) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, "Phiếu chuyển này không liên quan tới chi nhánh của bạn");
            }
        }
        return toResponse(transfer, itemRepository.findByTransferIdOrderByIdAsc(transferId), true);
    }

    @Transactional
    public StockTransferResponse create(Long userId, StockTransferRequest request) {
        StoreAccessGuard.Access access = storeAccessGuard.require(userId, request.fromStoreId());
        StockTransfer transfer = new StockTransfer();
        transfer.setFromStore(access.store());
        transfer.setToStore(receivingStore(request.fromStoreId(), request.toStoreId()));
        transfer.setNote(trimToNull(request.note()));
        transfer.setCreatedBy(access.user());
        StockTransfer saved = transferRepository.saveAndFlush(transfer);
        saveLines(saved, request);
        log.info("User id={} created stock transfer id={} from store id={} to store id={}", userId, saved.getId(),
                request.fromStoreId(), request.toStoreId());
        return get(userId, saved.getId());
    }

    @Transactional
    public StockTransferResponse update(Long userId, Long transferId, StockTransferRequest request) {
        StockTransfer transfer = lock(transferId);
        storeAccessGuard.require(userId, transfer.getFromStore().getId());
        requireStatus(transfer, StockTransferStatus.DRAFT);
        transfer.setToStore(receivingStore(transfer.getFromStore().getId(), request.toStoreId()));
        transfer.setNote(trimToNull(request.note()));
        transferRepository.saveAndFlush(transfer);
        itemRepository.deleteByTransferId(transferId);
        saveLines(transfer, request);
        return get(userId, transferId);
    }

    /** DRAFT → IN_TRANSIT: takes every line out of the sending store; 409 when it holds less than a line needs. */
    @Transactional
    public StockTransferResponse send(Long userId, Long transferId) {
        StockTransfer transfer = lock(transferId);
        StoreAccessGuard.Access access = storeAccessGuard.require(userId, transfer.getFromStore().getId());
        requireStatus(transfer, StockTransferStatus.DRAFT);
        List<StockTransferItem> items = sorted(itemRepository.findByTransferIdOrderByIdAsc(transferId));
        Map<Long, Inventory> rows = lockedRows(transfer.getFromStore().getId(), items);

        List<String> missing = new ArrayList<>();
        for (StockTransferItem item : items) {
            Inventory row = rows.get(item.getVariant().getId());
            int available = row == null ? 0 : row.getQuantity();
            if (available < item.getQuantity()) {
                missing.add("%s - %s (cần %d, còn %d)".formatted(item.getVariant().getProduct().getName(),
                        item.getVariant().getVariantName(), item.getQuantity(), available));
            }
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.STOCK_TRANSFER_INSUFFICIENT_STOCK,
                    "Chi nhánh %s không đủ hàng để chuyển: %s".formatted(transfer.getFromStore().getName(),
                            String.join("; ", missing)));
        }
        for (StockTransferItem item : items) {
            Inventory row = rows.get(item.getVariant().getId());
            row.setQuantity(row.getQuantity() - item.getQuantity());
            record(transfer, transfer.getFromStore(), item, MovementType.TRANSFER_OUT, -item.getQuantity(),
                    "Chuyển tới " + transfer.getToStore().getName(), access.user());
        }
        transfer.setStatus(StockTransferStatus.IN_TRANSIT);
        transfer.setSentBy(access.user());
        transfer.setSentAt(LocalDateTime.now(clock));
        transferRepository.saveAndFlush(transfer);
        log.info("User id={} sent stock transfer id={}", userId, transferId);
        return get(userId, transferId);
    }

    /** IN_TRANSIT → RECEIVED: adds every line to the receiving store. */
    @Transactional
    public StockTransferResponse receive(Long userId, Long transferId) {
        StockTransfer transfer = lock(transferId);
        StoreAccessGuard.Access access = storeAccessGuard.require(userId, transfer.getToStore().getId());
        requireStatus(transfer, StockTransferStatus.IN_TRANSIT);
        List<StockTransferItem> items = sorted(itemRepository.findByTransferIdOrderByIdAsc(transferId));
        Map<Long, Inventory> rows = lockedRows(transfer.getToStore().getId(), items);
        for (StockTransferItem item : items) {
            Inventory row = rows.get(item.getVariant().getId());
            row.setQuantity(row.getQuantity() + item.getQuantity());
            record(transfer, transfer.getToStore(), item, MovementType.TRANSFER_IN, item.getQuantity(),
                    "Nhận từ " + transfer.getFromStore().getName(), access.user());
        }
        transfer.setStatus(StockTransferStatus.RECEIVED);
        transfer.setReceivedBy(access.user());
        transfer.setReceivedAt(LocalDateTime.now(clock));
        transferRepository.saveAndFlush(transfer);
        log.info("User id={} received stock transfer id={}", userId, transferId);
        return get(userId, transferId);
    }

    /** DRAFT or IN_TRANSIT → CANCELLED; what already left the sending store goes back to it. */
    @Transactional
    public StockTransferResponse cancel(Long userId, Long transferId) {
        StockTransfer transfer = lock(transferId);
        StoreAccessGuard.Access access = storeAccessGuard.require(userId, transfer.getFromStore().getId());
        if (transfer.getStatus() != StockTransferStatus.DRAFT && transfer.getStatus() != StockTransferStatus.IN_TRANSIT) {
            throw new BusinessException(ErrorCode.STOCK_TRANSFER_INVALID_STATUS);
        }
        if (transfer.getStatus() == StockTransferStatus.IN_TRANSIT) {
            List<StockTransferItem> items = sorted(itemRepository.findByTransferIdOrderByIdAsc(transferId));
            Map<Long, Inventory> rows = lockedRows(transfer.getFromStore().getId(), items);
            for (StockTransferItem item : items) {
                Inventory row = rows.get(item.getVariant().getId());
                row.setQuantity(row.getQuantity() + item.getQuantity());
                record(transfer, transfer.getFromStore(), item, MovementType.TRANSFER_IN, item.getQuantity(),
                        "Huỷ phiếu chuyển " + code(transferId), access.user());
            }
        }
        transfer.setStatus(StockTransferStatus.CANCELLED);
        transfer.setCancelledBy(access.user());
        transfer.setCancelledAt(LocalDateTime.now(clock));
        transferRepository.saveAndFlush(transfer);
        log.info("User id={} cancelled stock transfer id={}", userId, transferId);
        return get(userId, transferId);
    }

    public static String code(Long transferId) {
        return "CK%06d".formatted(transferId);
    }

    private StockTransfer lock(Long transferId) {
        return transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_TRANSFER_NOT_FOUND));
    }

    private static void requireStatus(StockTransfer transfer, StockTransferStatus expected) {
        if (transfer.getStatus() != expected) {
            throw new BusinessException(ErrorCode.STOCK_TRANSFER_INVALID_STATUS);
        }
    }

    private Store receivingStore(Integer fromStoreId, Integer toStoreId) {
        if (fromStoreId.equals(toStoreId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chi nhánh nhận phải khác chi nhánh gửi");
        }
        Store store = storeRepository.findById(toStoreId).orElseThrow(() -> new BusinessException(
                ErrorCode.STORE_NOT_FOUND, "Không tìm thấy chi nhánh id %d".formatted(toStoreId)));
        if (Boolean.FALSE.equals(store.getActive())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chi nhánh nhận đang tạm đóng");
        }
        return store;
    }

    private void saveLines(StockTransfer transfer, StockTransferRequest request) {
        Set<Long> seen = new HashSet<>();
        List<StockTransferItem> items = new ArrayList<>();
        for (StockTransferRequest.Line line : request.items()) {
            if (!seen.add(line.variantId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Mỗi phiên bản chỉ xuất hiện một dòng trong phiếu (id %d bị lặp)".formatted(line.variantId()));
            }
            ProductVariant variant = variantRepository.findByIdAndProductDeletedAtIsNull(line.variantId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND,
                            "Không tìm thấy phiên bản sản phẩm id %d".formatted(line.variantId())));
            StockTransferItem item = new StockTransferItem();
            item.setTransfer(transfer);
            item.setVariant(variant);
            item.setQuantity(line.quantity());
            items.add(item);
        }
        itemRepository.saveAll(items);
        itemRepository.flush();
    }

    private static List<StockTransferItem> sorted(List<StockTransferItem> items) {
        List<StockTransferItem> copy = new ArrayList<>(items);
        copy.sort(Comparator.comparing(item -> item.getVariant().getId()));
        return copy;
    }

    /** Locks the store's rows of the lines in variant id order (like order confirmation); rows that do not exist yet are created. */
    private Map<Long, Inventory> lockedRows(Integer storeId, List<StockTransferItem> items) {
        List<Long> variantIds = items.stream().map(item -> item.getVariant().getId()).toList();
        variantIds.forEach(variantId -> inventoryRepository.insertIfMissing(storeId, variantId));
        return inventoryRepository.findAllForUpdate(storeId, variantIds).stream()
                .collect(Collectors.toMap(row -> row.getId().getVariantId(), Function.identity(), (a, b) -> a, TreeMap::new));
    }

    private void record(StockTransfer transfer, Store store, StockTransferItem item, MovementType type, int change,
                        String note, User by) {
        StockMovement movement = new StockMovement();
        movement.setStore(store);
        movement.setVariant(item.getVariant());
        movement.setMovementType(type);
        movement.setQuantityChange(change);
        movement.setNote(note);
        movement.setCreatedBy(by);
        movement.setTransferId(transfer.getId());
        stockMovementRepository.save(movement);
    }

    private Map<Long, List<StockTransferItem>> itemsOf(List<StockTransfer> transfers) {
        if (transfers.isEmpty()) {
            return Map.of();
        }
        return itemRepository.findByTransferIdInOrderByIdAsc(transfers.stream().map(StockTransfer::getId).toList())
                .stream().collect(Collectors.groupingBy(item -> item.getTransfer().getId()));
    }

    private static StockTransferResponse toResponse(StockTransfer t, List<StockTransferItem> items, boolean withItems) {
        long quantity = items.stream().mapToLong(StockTransferItem::getQuantity).sum();
        List<StockTransferResponse.Item> lines = withItems ? items.stream().map(item -> {
            ProductVariant variant = item.getVariant();
            return new StockTransferResponse.Item(variant.getId(), variant.getVariantName(), variant.getSkuVariant(),
                    variant.getProduct().getId(), variant.getProduct().getName(), item.getQuantity());
        }).toList() : List.of();
        return new StockTransferResponse(t.getId(), code(t.getId()), t.getFromStore().getId(), t.getFromStore().getName(),
                t.getToStore().getId(), t.getToStore().getName(), t.getStatus(), t.getNote(), items.size(), quantity,
                name(t.getCreatedBy()), t.getCreatedAt(), name(t.getSentBy()), t.getSentAt(), name(t.getReceivedBy()),
                t.getReceivedAt(), name(t.getCancelledBy()), t.getCancelledAt(), lines);
    }

    private static String name(User user) {
        return user == null ? null : user.getFullname();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
