package com.example.Tech.service.impl.inventory;

import com.example.Tech.dto.request.inventory.InventorySearchRequest;
import com.example.Tech.dto.request.inventory.StockInRequest;
import com.example.Tech.dto.request.inventory.StockInStatsRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.InventoryItemResponse;
import com.example.Tech.dto.response.inventory.InventoryOverviewResponse;
import com.example.Tech.dto.response.inventory.StockInStatsResponse;
import com.example.Tech.dto.response.inventory.StockMovementResponse;
import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.inventory.InventoryFilterSpecifications;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.inventory.StockInStoreTotals;
import com.example.Tech.repository.inventory.StockInTotals;
import com.example.Tech.repository.inventory.StockMovementRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.service.inventory.InventoryService;
import com.example.Tech.service.store.StoreAccessGuard;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    /** Bounds used when the stats filter leaves a side of the period open. */
    private static final LocalDateTime OPEN_START = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime OPEN_END = LocalDateTime.of(3000, 1, 1, 0, 0);

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final StoreAccessGuard storeAccessGuard;
    private final CurrentUserLoader currentUserLoader;
    private final StoreRepository storeRepository;

    @Override
    public PageResponse<InventoryItemResponse> list(Long userId, Integer storeId, InventorySearchRequest filter,
                                                    Pageable pageable) {
        storeAccessGuard.require(userId, storeId);
        Page<Inventory> page = inventoryRepository.findAll(
                InventoryFilterSpecifications.matching(storeId, filter.keyword(), filter.outOfStock()), pageable);
        Map<Long, String> images = imagesByProduct(page.getContent().stream()
                .map(row -> row.getVariant().getProduct().getId()).collect(Collectors.toSet()));
        return PageResponse.from(page.map(row -> toItem(row, images.get(row.getVariant().getProduct().getId()))));
    }

    @Override
    @Transactional
    public InventoryItemResponse stockIn(Long userId, Integer storeId, StockInRequest request) {
        StoreAccessGuard.Access access = storeAccessGuard.require(userId, storeId);
        ProductVariant variant = variantRepository.findByIdAndProductDeletedAtIsNull(request.variantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND,
                        "Không tìm thấy phiên bản sản phẩm id %d".formatted(request.variantId())));

        inventoryRepository.insertIfMissing(storeId, variant.getId());
        Inventory row = inventoryRepository.findAllForUpdate(storeId, List.of(variant.getId())).getFirst();
        row.setQuantity(row.getQuantity() + request.quantity());
        inventoryRepository.saveAndFlush(row);

        StockMovement movement = new StockMovement();
        movement.setStore(access.store());
        movement.setVariant(variant);
        movement.setMovementType(MovementType.IN);
        movement.setQuantityChange(request.quantity());
        movement.setSupplierName(trimToNull(request.supplierName()));
        movement.setNote(trimToNull(request.note()));
        movement.setCreatedBy(access.user());
        stockMovementRepository.save(movement);

        log.info("User id={} stocked in {} of variant id={} at store id={} (now {})",
                userId, request.quantity(), variant.getId(), storeId, row.getQuantity());
        Long productId = variant.getProduct().getId();
        return toItem(row, imagesByProduct(Set.of(productId)).get(productId));
    }

    @Override
    public PageResponse<StockMovementResponse> movements(Long userId, Integer storeId, Long variantId, Pageable pageable) {
        storeAccessGuard.require(userId, storeId);
        return PageResponse.from(stockMovementRepository
                .findByStoreIdAndVariantIdOrderByCreatedAtDescIdDesc(storeId, variantId, pageable)
                .map(InventoryServiceImpl::toMovement));
    }

    @Override
    public PageResponse<InventoryOverviewResponse> overview(Long userId, InventorySearchRequest filter,
                                                            Pageable pageable) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
        Page<ProductVariant> page = variantRepository.findAll(
                InventoryFilterSpecifications.stockedVariants(filter.keyword(), filter.outOfStock()), pageable);
        List<ProductVariant> variants = page.getContent();
        Map<Long, List<Inventory>> rowsByVariant = variants.isEmpty() ? Map.of()
                : inventoryRepository.findAllByIdVariantIdIn(variants.stream().map(ProductVariant::getId).toList())
                .stream().collect(Collectors.groupingBy(row -> row.getId().getVariantId()));
        Map<Long, String> images = imagesByProduct(variants.stream()
                .map(variant -> variant.getProduct().getId()).collect(Collectors.toSet()));
        return PageResponse.from(page.map(variant -> toOverview(variant,
                rowsByVariant.getOrDefault(variant.getId(), List.of()), images.get(variant.getProduct().getId()))));
    }

    @Override
    public StockInStatsResponse stockInStats(Long userId, StockInStatsRequest filter) {
        currentUserLoader.loadWithAnyRole(userId, RoleName.ADMIN);
        LocalDate fromDate = filter.fromDate();
        LocalDate toDate = filter.toDate();
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Ngày bắt đầu phải trước hoặc trùng ngày kết thúc");
        }
        Integer storeId = filter.storeId();
        List<Store> stores = storeId == null
                ? storeRepository.findAll(Sort.by("name", "id"))
                : List.of(storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(
                        ErrorCode.STORE_NOT_FOUND, "Không tìm thấy chi nhánh id %d".formatted(storeId))));

        LocalDateTime from = fromDate == null ? OPEN_START : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? OPEN_END : toDate.plusDays(1).atStartOfDay();
        StockInTotals totals = stockMovementRepository.stockInTotals(MovementType.IN, storeId, from, to);
        Map<Integer, StockInStoreTotals> byStore = stockMovementRepository
                .stockInTotalsByStore(MovementType.IN, storeId, from, to).stream()
                .collect(Collectors.toMap(StockInStoreTotals::storeId, row -> row));

        List<StockInStatsResponse.StoreStockIn> rows = stores.stream()
                .filter(store -> storeId != null || !Boolean.FALSE.equals(store.getActive())
                        || byStore.containsKey(store.getId()))
                .map(store -> toStoreStockIn(store, byStore.get(store.getId())))
                .sorted(Comparator.comparingLong(StockInStatsResponse.StoreStockIn::quantity).reversed())
                .toList();
        return new StockInStatsResponse(storeId, fromDate, toDate, orZero(totals.quantity()),
                orZero(totals.stockInCount()), orZero(totals.variantCount()), orZero(totals.supplierCount()), rows);
    }

    private static StockInStatsResponse.StoreStockIn toStoreStockIn(Store store, StockInStoreTotals totals) {
        return new StockInStatsResponse.StoreStockIn(store.getId(), store.getName(),
                !Boolean.FALSE.equals(store.getActive()),
                totals == null ? 0 : orZero(totals.quantity()),
                totals == null ? 0 : orZero(totals.stockInCount()),
                totals == null ? 0 : orZero(totals.variantCount()),
                totals == null ? null : totals.lastStockInAt());
    }

    private static long orZero(Long value) {
        return value == null ? 0 : value;
    }

    /** One image per product (primary first), in one query. */
    private Map<Long, String> imagesByProduct(Set<Long> productIds) {
        Map<Long, String> images = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (ProductImage image : imageRepository.findAllByProductIdInBestFirst(productIds)) {
                images.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
            }
        }
        return images;
    }

    private static InventoryItemResponse toItem(Inventory row, String imageUrl) {
        ProductVariant variant = row.getVariant();
        return new InventoryItemResponse(
                row.getId().getStoreId(),
                variant.getId(),
                variant.getVariantName(),
                variant.getSkuVariant(),
                variant.getProduct().getId(),
                variant.getProduct().getName(),
                imageUrl,
                row.getQuantity(),
                row.getUpdatedAt());
    }

    private static InventoryOverviewResponse toOverview(ProductVariant variant, List<Inventory> rows, String imageUrl) {
        List<InventoryOverviewResponse.StoreStock> stores = rows.stream()
                .map(row -> {
                    Store store = row.getStore();
                    return new InventoryOverviewResponse.StoreStock(store.getId(), store.getName(),
                            !Boolean.FALSE.equals(store.getActive()), row.getQuantity(), row.getUpdatedAt());
                })
                .sorted(Comparator.comparing(InventoryOverviewResponse.StoreStock::storeName)
                        .thenComparing(InventoryOverviewResponse.StoreStock::storeId))
                .toList();
        return new InventoryOverviewResponse(
                variant.getId(),
                variant.getVariantName(),
                variant.getSkuVariant(),
                variant.getProduct().getId(),
                variant.getProduct().getName(),
                imageUrl,
                stores.stream().mapToInt(InventoryOverviewResponse.StoreStock::quantity).sum(),
                stores);
    }

    private static StockMovementResponse toMovement(StockMovement movement) {
        Order order = movement.getOrder();
        User by = movement.getCreatedBy();
        return new StockMovementResponse(
                movement.getId(),
                movement.getMovementType(),
                movement.getQuantityChange(),
                movement.getSupplierName(),
                movement.getNote(),
                order == null ? null : order.getId(),
                order == null ? null : OrderMapper.code(order.getId()),
                by == null ? null : by.getId(),
                by == null ? null : by.getFullname(),
                movement.getCreatedAt());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
