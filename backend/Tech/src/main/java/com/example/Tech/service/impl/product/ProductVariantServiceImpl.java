package com.example.Tech.service.impl.product;

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
import com.example.Tech.repository.product.AttributeValueRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.product.VariantAttributeValueRepository;
import com.example.Tech.service.product.ProductVariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final ProductVariantMapper variantMapper;

    @Override
    public List<ProductVariantResponse> getByProductId(Long productId) {
        findProduct(productId);
        List<ProductVariant> variants = variantRepository.findAllByProductIdOrderByIdAsc(productId);
        Map<Long, List<AttributeValue>> valuesByVariant = loadAttributeValues(
                variants.stream().map(ProductVariant::getId).toList());
        return variants.stream()
                .map(variant -> variantMapper.toResponse(variant,
                        valuesByVariant.getOrDefault(variant.getId(), List.of())))
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
        log.info("Created product variant id={} for product id={}", saved.getId(), request.productId());
        return variantMapper.toResponse(saved, List.of());
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
        log.info("Updated product variant id={}", id);
        return toResponse(saved);
    }

    /**
     * Links to attribute values are removed by the database (ON DELETE CASCADE).
     */
    @Override
    @Transactional
    public void delete(Long id) {
        ProductVariant variant = findVariant(id);
        variantRepository.delete(variant);
        variantRepository.flush();
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
        log.info("Assigned attribute value id={} to variant id={}", valueId, variantId);
        return toResponse(variant);
    }

    @Override
    @Transactional
    public void removeAttributeValue(Long variantId, Integer valueId) {
        findVariant(variantId);
        VariantAttributeValueId linkId = new VariantAttributeValueId(variantId, valueId);
        if (!variantAttributeValueRepository.existsById(linkId)) {
            throw new BusinessException(ErrorCode.VARIANT_ATTRIBUTE_VALUE_NOT_FOUND,
                    "Attribute value %d is not assigned to variant %d".formatted(valueId, variantId));
        }
        variantAttributeValueRepository.deleteById(linkId);
        log.info("Removed attribute value id={} from variant id={}", valueId, variantId);
    }

    private ProductVariantResponse toResponse(ProductVariant variant) {
        List<AttributeValue> values = loadAttributeValues(List.of(variant.getId()))
                .getOrDefault(variant.getId(), List.of());
        return variantMapper.toResponse(variant, values);
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
