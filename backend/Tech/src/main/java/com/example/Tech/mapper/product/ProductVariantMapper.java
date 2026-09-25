package com.example.Tech.mapper.product;

import com.example.Tech.dto.request.product.ProductVariantCreateRequest;
import com.example.Tech.dto.request.product.ProductVariantUpdateRequest;
import com.example.Tech.dto.response.product.ProductVariantResponse;
import com.example.Tech.entity.product.AttributeValue;
import com.example.Tech.entity.product.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps simple fields only; product, SKU and attribute values are resolved by the service.
 */
@Component
@RequiredArgsConstructor
public class ProductVariantMapper {

    private final AttributeValueMapper attributeValueMapper;

    public ProductVariant toEntity(ProductVariantCreateRequest request) {
        ProductVariant variant = new ProductVariant();
        variant.setVariantName(request.variantName());
        variant.setPrice(request.price());
        variant.setDiscountPrice(request.discountPrice());
        variant.setColor(request.color());
        variant.setStorage(request.storage());
        variant.setRam(request.ram());
        if (request.stockQuantity() != null) {
            variant.setStockQuantity(request.stockQuantity());
        }
        return variant;
    }

    public void updateEntity(ProductVariant variant, ProductVariantUpdateRequest request) {
        variant.setVariantName(request.variantName());
        variant.setPrice(request.price());
        variant.setDiscountPrice(request.discountPrice());
        variant.setColor(request.color());
        variant.setStorage(request.storage());
        variant.setRam(request.ram());
        if (request.stockQuantity() != null) {
            variant.setStockQuantity(request.stockQuantity());
        }
    }

    public ProductVariantResponse toResponse(ProductVariant variant, List<AttributeValue> attributeValues) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getProduct().getName(),
                variant.getVariantName(),
                variant.getSkuVariant(),
                variant.getPrice(),
                variant.getDiscountPrice(),
                variant.getStockQuantity(),
                variant.getColor(),
                variant.getStorage(),
                variant.getRam(),
                attributeValues.stream().map(attributeValueMapper::toResponse).toList(),
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }
}
