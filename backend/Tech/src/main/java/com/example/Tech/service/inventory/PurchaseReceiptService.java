package com.example.Tech.service.inventory;

import com.example.Tech.dto.request.inventory.PurchaseReceiptRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.PurchaseReceiptResponse;
import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.PurchaseReceipt;
import com.example.Tech.entity.inventory.PurchaseReceiptItem;
import com.example.Tech.entity.inventory.PurchaseReceiptStatus;
import com.example.Tech.entity.inventory.StockMovement;
import com.example.Tech.entity.inventory.Supplier;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.inventory.PurchaseReceiptItemRepository;
import com.example.Tech.repository.inventory.PurchaseReceiptRepository;
import com.example.Tech.repository.inventory.StockMovementRepository;
import com.example.Tech.repository.inventory.SupplierRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.service.store.StoreAccessGuard;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Goods receipts of a store. Every method is store-scoped through StoreAccessGuard (ADMIN: any store; staff: the store
 * of their assignment). A draft can be replaced or cancelled; completing it adds the quantities to the store, writes
 * one IN movement per line and moves the average cost of each variant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseReceiptService {

    private final PurchaseReceiptRepository receiptRepository;
    private final PurchaseReceiptItemRepository itemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StoreAccessGuard storeAccessGuard;
    private final EntityManager entityManager;
    private final Clock clock;

    public PageResponse<PurchaseReceiptResponse> list(Long userId, Integer storeId, PurchaseReceiptStatus status,
                                                      String keyword, Pageable pageable) {
        storeAccessGuard.require(userId, storeId);
        Specification<PurchaseReceipt> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            parts.add(cb.equal(root.get("store").get("id"), storeId));
            if (status != null) {
                parts.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                parts.add(cb.like(cb.lower(root.get("supplier").get("name")),
                        "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
        var page = receiptRepository.findAll(spec, pageable);
        Map<Long, List<PurchaseReceiptItem>> items = itemsOf(page.getContent());
        return PageResponse.from(page.map(receipt -> toResponse(receipt, items.getOrDefault(receipt.getId(), List.of()), false)));
    }

    public PurchaseReceiptResponse get(Long userId, Integer storeId, Long receiptId) {
        storeAccessGuard.require(userId, storeId);
        PurchaseReceipt receipt = find(storeId, receiptId);
        return toResponse(receipt, itemRepository.findByReceiptIdOrderByIdAsc(receiptId), true);
    }

    @Transactional
    public PurchaseReceiptResponse create(Long userId, Integer storeId, PurchaseReceiptRequest request) {
        StoreAccessGuard.Access access = storeAccessGuard.require(userId, storeId);
        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setStore(access.store());
        receipt.setCreatedBy(access.user());
        receipt.setSupplier(activeSupplier(request.supplierId()));
        receipt.setNote(trimToNull(request.note()));
        PurchaseReceipt saved = receiptRepository.saveAndFlush(receipt);
        saveLines(saved, request);
        log.info("User id={} created purchase receipt id={} at store id={}", userId, saved.getId(), storeId);
        return get(userId, storeId, saved.getId());
    }

    @Transactional
    public PurchaseReceiptResponse update(Long userId, Integer storeId, Long receiptId, PurchaseReceiptRequest request) {
        storeAccessGuard.require(userId, storeId);
        PurchaseReceipt receipt = lockDraft(storeId, receiptId);
        receipt.setSupplier(activeSupplier(request.supplierId()));
        receipt.setNote(trimToNull(request.note()));
        receiptRepository.saveAndFlush(receipt);
        itemRepository.deleteByReceiptId(receiptId);
        saveLines(receipt, request);
        return get(userId, storeId, receiptId);
    }

    @Transactional
    public PurchaseReceiptResponse cancel(Long userId, Integer storeId, Long receiptId) {
        storeAccessGuard.require(userId, storeId);
        PurchaseReceipt receipt = lockDraft(storeId, receiptId);
        receipt.setStatus(PurchaseReceiptStatus.CANCELLED);
        receipt.setCancelledAt(LocalDateTime.now(clock));
        receiptRepository.saveAndFlush(receipt);
        log.info("User id={} cancelled purchase receipt id={}", userId, receiptId);
        return get(userId, storeId, receiptId);
    }

    /**
     * Adds the lines to the store. Variants are handled in id order (the same order order-confirmation locks stock),
     * each one's total stock before the receipt weights its new average cost.
     */
    @Transactional
    public PurchaseReceiptResponse complete(Long userId, Integer storeId, Long receiptId) {
        StoreAccessGuard.Access access = storeAccessGuard.require(userId, storeId);
        PurchaseReceipt receipt = lockDraft(storeId, receiptId);
        List<PurchaseReceiptItem> items = new ArrayList<>(itemRepository.findByReceiptIdOrderByIdAsc(receiptId));
        items.sort((a, b) -> a.getVariant().getId().compareTo(b.getVariant().getId()));
        for (PurchaseReceiptItem item : items) {
            ProductVariant variant = item.getVariant();
            entityManager.refresh(variant, LockModeType.PESSIMISTIC_WRITE);
            long stockBefore = inventoryRepository.sumQuantityByVariantId(variant.getId());
            inventoryRepository.insertIfMissing(storeId, variant.getId());
            Inventory row = inventoryRepository.findAllForUpdate(storeId, List.of(variant.getId())).getFirst();
            row.setQuantity(row.getQuantity() + item.getQuantity());
            inventoryRepository.saveAndFlush(row);
            variant.setAverageCost(CostPolicy.weightedAverage(stockBefore, variant.getAverageCost(),
                    item.getQuantity(), item.getUnitCost()));

            StockMovement movement = new StockMovement();
            movement.setStore(access.store());
            movement.setVariant(variant);
            movement.setMovementType(MovementType.IN);
            movement.setQuantityChange(item.getQuantity());
            movement.setSupplierName(receipt.getSupplier().getName());
            movement.setNote("Phiếu nhập " + code(receiptId));
            movement.setCreatedBy(access.user());
            movement.setReceiptId(receiptId);
            stockMovementRepository.save(movement);
        }
        receipt.setStatus(PurchaseReceiptStatus.COMPLETED);
        receipt.setCompletedBy(access.user());
        receipt.setCompletedAt(LocalDateTime.now(clock));
        receiptRepository.saveAndFlush(receipt);
        log.info("User id={} completed purchase receipt id={} ({} lines)", userId, receiptId, items.size());
        return get(userId, storeId, receiptId);
    }

    public static String code(Long receiptId) {
        return "PN%06d".formatted(receiptId);
    }

    private PurchaseReceipt find(Integer storeId, Long receiptId) {
        return receiptRepository.findWithDetailsById(receiptId)
                .filter(receipt -> receipt.getStore().getId().equals(storeId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PURCHASE_RECEIPT_NOT_FOUND));
    }

    private PurchaseReceipt lockDraft(Integer storeId, Long receiptId) {
        PurchaseReceipt receipt = receiptRepository.findByIdForUpdate(receiptId)
                .filter(found -> found.getStore().getId().equals(storeId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PURCHASE_RECEIPT_NOT_FOUND));
        if (receipt.getStatus() != PurchaseReceiptStatus.DRAFT) {
            throw new BusinessException(ErrorCode.PURCHASE_RECEIPT_NOT_DRAFT);
        }
        return receipt;
    }

    private Supplier activeSupplier(Integer supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND));
        if (!supplier.isActive()) {
            throw new BusinessException(ErrorCode.SUPPLIER_INACTIVE);
        }
        return supplier;
    }

    private void saveLines(PurchaseReceipt receipt, PurchaseReceiptRequest request) {
        Set<Long> seen = new HashSet<>();
        List<PurchaseReceiptItem> items = new ArrayList<>();
        for (PurchaseReceiptRequest.Line line : request.items()) {
            if (!seen.add(line.variantId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Mỗi phiên bản chỉ xuất hiện một dòng trong phiếu (id %d bị lặp)".formatted(line.variantId()));
            }
            ProductVariant variant = variantRepository.findByIdAndProductDeletedAtIsNull(line.variantId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND,
                            "Không tìm thấy phiên bản sản phẩm id %d".formatted(line.variantId())));
            PurchaseReceiptItem item = new PurchaseReceiptItem();
            item.setReceipt(receipt);
            item.setVariant(variant);
            item.setQuantity(line.quantity());
            item.setUnitCost(line.unitCost());
            items.add(item);
        }
        itemRepository.saveAll(items);
        itemRepository.flush();
    }

    private Map<Long, List<PurchaseReceiptItem>> itemsOf(List<PurchaseReceipt> receipts) {
        if (receipts.isEmpty()) {
            return Map.of();
        }
        return itemRepository.findByReceiptIdInOrderByIdAsc(receipts.stream().map(PurchaseReceipt::getId).toList())
                .stream().collect(Collectors.groupingBy(item -> item.getReceipt().getId(), TreeMap::new, Collectors.toList()));
    }

    private static PurchaseReceiptResponse toResponse(PurchaseReceipt receipt, List<PurchaseReceiptItem> items,
                                                      boolean withItems) {
        long quantity = items.stream().mapToLong(PurchaseReceiptItem::getQuantity).sum();
        BigDecimal total = items.stream()
                .map(item -> item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        User createdBy = receipt.getCreatedBy();
        User completedBy = receipt.getCompletedBy();
        List<PurchaseReceiptResponse.Item> lines = withItems ? items.stream().map(item -> {
            ProductVariant variant = item.getVariant();
            return new PurchaseReceiptResponse.Item(variant.getId(), variant.getVariantName(), variant.getSkuVariant(),
                    variant.getProduct().getId(), variant.getProduct().getName(), item.getQuantity(), item.getUnitCost(),
                    item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantity())), variant.getAverageCost());
        }).toList() : List.of();
        return new PurchaseReceiptResponse(receipt.getId(), code(receipt.getId()), receipt.getStore().getId(),
                receipt.getStore().getName(), receipt.getSupplier().getId(), receipt.getSupplier().getName(),
                receipt.getStatus(), receipt.getNote(), items.size(), quantity, total,
                createdBy == null ? null : createdBy.getFullname(), receipt.getCreatedAt(),
                completedBy == null ? null : completedBy.getFullname(), receipt.getCompletedAt(), lines);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
