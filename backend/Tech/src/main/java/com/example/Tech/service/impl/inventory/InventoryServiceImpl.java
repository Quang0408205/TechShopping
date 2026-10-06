package com.example.Tech.service.impl.inventory;

import com.example.Tech.dto.request.inventory.InventorySearchRequest;
import com.example.Tech.dto.request.inventory.StockInRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.InventoryItemResponse;
import com.example.Tech.dto.response.inventory.StockMovementResponse;
import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.inventory.InventoryFilterSpecifications;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.inventory.StockMovementRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.service.inventory.InventoryService;
import com.example.Tech.service.store.StoreAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final StoreAccessGuard storeAccessGuard;

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
