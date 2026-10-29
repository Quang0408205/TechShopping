package com.example.Tech.service.impl.product;

import com.example.Tech.cache.ReadCache;
import com.example.Tech.dto.request.product.ProductVariantCreateRequest;
import com.example.Tech.dto.request.product.ProductVariantUpdateRequest;
import com.example.Tech.dto.response.product.ProductVariantResponse;
import com.example.Tech.entity.product.AttributeValue;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.product.VariantAttributeValue;
import com.example.Tech.entity.product.VariantAttributeValueId;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.ProductVariantMapper;
import com.example.Tech.repository.cart.CartItemRepository;
import com.example.Tech.repository.order.OrderItemRepository;
import com.example.Tech.repository.product.AttributeValueRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.product.VariantAttributeValueRepository;
import com.example.Tech.service.product.ProductFilterService;
import com.example.Tech.service.product.ProductVariantService;
import com.example.Tech.service.promotion.EffectivePrice;
import com.example.Tech.service.promotion.PromotionPricingService;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.inventory.StockMovementRepository;
import com.example.Tech.service.inventory.StockAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductVariantServiceImpl implements ProductVariantService {

    private static final Comparator<AttributeValue> ATTRIBUTE_ORDER =
            Comparator.comparing((AttributeValue value) -> value.getAttribute().getId())
                    .thenComparing(AttributeValue::getValue);

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final VariantAttributeValueRepository variantAttributeValueRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductVariantMapper variantMapper;
    private final PromotionPricingService pricingService;
    private final StockAvailability stockAvailability;
    private final Clock clock;
    private final ReadCache cache;
    private final ProductFilterService filterService;

    @Override
    public List<ProductVariantResponse> getByProductId(Long productId) {
        // variants and prices are cached for 10 minutes; the stock figures are always read fresh
        List<ProductVariantResponse> variants = cache.get(ReadCache.VARIANTS, productId, new TypeReference<>() { },
                () -> loadByProductId(productId));
        Map<Long, Integer> available = stockAvailability.byVariant(variants.stream().map(ProductVariantResponse::id).toList());
        return variants.stream()
                .map(variant -> variant.withAvailableQuantity(available.getOrDefault(variant.id(), 0)))
                .toList();
    }

    private List<ProductVariantResponse> loadByProductId(Long productId) {
        findProduct(productId);
        List<ProductVariant> variants = variantRepository.findAllByProductIdOrderByIdAsc(productId);
        Map<Long, List<AttributeValue>> valuesByVariant = loadAttributeValues(
                variants.stream().map(ProductVariant::getId).toList());
        Map<Long, EffectivePrice> prices = pricingService.resolveVariants(variants, LocalDateTime.now(clock));
        Map<Long, Integer> available = stockAvailability.byVariant(variants.stream().map(ProductVariant::getId).toList());
        return variants.stream()
                .map(variant -> variantMapper.toResponse(variant, valuesByVariant.getOrDefault(variant.getId(), List.of()),
                        prices.get(variant.getId()), available.getOrDefault(variant.getId(), 0)))
                .toList();
    }

    @Override
    public ProductVariantResponse getById(Long id) {
        return toResponse(findVariant(id));
    }

    @Override
    @Transactional
    public ProductVariantResponse create(ProductVariantCreateRequest request) {
        validatePrices(request.price(), request.discountPrice());
        String sku = normalizeSku(request.skuVariant());
        if (sku != null && variantRepository.existsBySkuVariant(sku)) {
            throw duplicateSku(sku);
        }

        ProductVariant variant = variantMapper.toEntity(request);
        variant.setSkuVariant(sku);
        variant.setProduct(findProduct(request.productId()));

        ProductVariant saved = variantRepository.save(variant);
        cache.evict(ReadCache.VARIANTS, request.productId());
        filterService.reindex(request.productId());
        log.info("Created product variant id={} for product id={}", saved.getId(), request.productId());
        return variantMapper.toResponse(saved, List.of(), pricingService.resolve(saved, LocalDateTime.now(clock)), 0);
    }

    @Override
    @Transactional
    public ProductVariantResponse update(Long id, ProductVariantUpdateRequest request) {
        ProductVariant variant = findVariant(id);

        validatePrices(request.price(), request.discountPrice());
        String sku = normalizeSku(request.skuVariant());
        if (sku != null && variantRepository.existsBySkuVariantAndIdNot(sku, id)) {
            throw duplicateSku(sku);
        }

        variantMapper.updateEntity(variant, request);
        variant.setSkuVariant(sku);

        ProductVariant saved = variantRepository.saveAndFlush(variant);
        cache.evict(ReadCache.VARIANTS, variant.getProduct().getId());
        filterService.reindex(variant.getProduct().getId());
        log.info("Updated product variant id={}", id);
        return toResponse(saved);
    }

    /**
     * Links to attribute values are removed by the database (ON DELETE CASCADE). cart_items has no cascade,
     * so the variant is first removed from every cart (carts are temporary data). A variant that appears in
     * an order is never deleted (order history; order_items has no cascade) → 409 RESOURCE_IN_USE.
     */
    @Override
    @Transactional
    public void delete(Long id) {
        ProductVariant variant = findVariant(id);
        if (orderItemRepository.existsByVariantId(id)) {
            throw new BusinessException(ErrorCode.RESOURCE_IN_USE,
                    "Product variant %d appears in orders and cannot be deleted; hide the product instead".formatted(id));
        }
        // the stock book keeps history: a variant with units in a store or with movements stays (hide the product)
        if (inventoryRepository.existsByIdVariantIdAndQuantityGreaterThan(id, 0)
                || stockMovementRepository.existsByVariantId(id)) {
            throw new BusinessException(ErrorCode.RESOURCE_IN_USE,
                    "Phiên bản còn tồn kho hoặc đã có lịch sử nhập / xuất kho, không xoá được; hãy ẩn sản phẩm");
        }
        inventoryRepository.deleteAllByVariantId(id);
        int cartLines = cartItemRepository.deleteAllByVariantId(id);
        if (cartLines > 0) {
            log.info("Removed product variant id={} from {} cart line(s)", id, cartLines);
        }
        variantRepository.delete(variant);
        variantRepository.flush();
        cache.evict(ReadCache.VARIANTS, variant.getProduct().getId());
        cache.evict(ReadCache.SPECS, variant.getProduct().getId());
        filterService.reindex(variant.getProduct().getId());
        log.info("Deleted product variant id={}", id);
    }

    @Override
    @Transactional
    public ProductVariantResponse addAttributeValue(Long variantId, Integer valueId) {
        ProductVariant variant = findVariant(variantId);
        AttributeValue attributeValue = attributeValueRepository.findWithAttributeById(valueId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND, valueId));

        if (variantAttributeValueRepository.existsById(new VariantAttributeValueId(variantId, valueId))) {
            throw new BusinessException(ErrorCode.DUPLICATE_VARIANT_ATTRIBUTE_VALUE,
                    "Attribute value %d is already assigned to variant %d".formatted(valueId, variantId));
        }
        // A variant has at most one value per attribute (e.g. a single color)
        if (variantAttributeValueRepository.existsByVariantIdAndAttributeValueAttributeId(
                variantId, attributeValue.getAttribute().getId())) {
            throw new BusinessException(ErrorCode.VARIANT_ATTRIBUTE_CONFLICT,
                    "Variant %d already has a value for attribute '%s'"
                            .formatted(variantId, attributeValue.getAttribute().getName()));
        }

        variantAttributeValueRepository.save(new VariantAttributeValue(variant, attributeValue));
        cache.evict(ReadCache.VARIANTS, variant.getProduct().getId());
        log.info("Assigned attribute value id={} to variant id={}", valueId, variantId);
        return toResponse(variant);
    }

    @Override
    @Transactional
    public void removeAttributeValue(Long variantId, Integer valueId) {
        ProductVariant variant = findVariant(variantId);
        VariantAttributeValueId linkId = new VariantAttributeValueId(variantId, valueId);
        if (!variantAttributeValueRepository.existsById(linkId)) {
            throw new BusinessException(ErrorCode.VARIANT_ATTRIBUTE_VALUE_NOT_FOUND,
                    "Attribute value %d is not assigned to variant %d".formatted(valueId, variantId));
        }
        variantAttributeValueRepository.deleteById(linkId);
        cache.evict(ReadCache.VARIANTS, variant.getProduct().getId());
        log.info("Removed attribute value id={} from variant id={}", valueId, variantId);
    }

    private ProductVariantResponse toResponse(ProductVariant variant) {
        List<AttributeValue> values = loadAttributeValues(List.of(variant.getId()))
                .getOrDefault(variant.getId(), List.of());
        return variantMapper.toResponse(variant, values, pricingService.resolve(variant, LocalDateTime.now(clock)),
                stockAvailability.byVariant(List.of(variant.getId())).getOrDefault(variant.getId(), 0));
    }

    /** Loads attribute values of several variants in one query, grouped by variant id. */
    private Map<Long, List<AttributeValue>> loadAttributeValues(List<Long> variantIds) {
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        return variantAttributeValueRepository.findAllByVariantIdIn(variantIds).stream()
                .collect(Collectors.groupingBy(
                        link -> link.getId().getVariantId(),
                        Collectors.collectingAndThen(
                                Collectors.mapping(VariantAttributeValue::getAttributeValue, Collectors.toList()),
                                values -> values.stream().sorted(ATTRIBUTE_ORDER).toList())));
    }

    private ProductVariant findVariant(Long id) {
        return variantRepository.findByIdAndProductDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, id));
    }

    private Product findProduct(Long productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId));
    }

    private void validatePrices(BigDecimal price, BigDecimal discountPrice) {
        if (discountPrice != null && discountPrice.compareTo(price) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_VARIANT_DATA,
                    "Discount price must not exceed price");
        }
    }

    /** Blank SKUs are stored as NULL so they do not collide on the unique constraint. */
    private String normalizeSku(String sku) {
        return sku == null || sku.isBlank() ? null : sku.trim();
    }

    private BusinessException duplicateSku(String sku) {
        return new BusinessException(ErrorCode.DUPLICATE_PRODUCT_VARIANT,
                "Product variant with sku '%s' already exists".formatted(sku));
    }
}
