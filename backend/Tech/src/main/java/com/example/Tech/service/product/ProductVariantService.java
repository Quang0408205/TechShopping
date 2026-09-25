package com.example.Tech.service.product;

import com.example.Tech.dto.request.product.ProductVariantCreateRequest;
import com.example.Tech.dto.request.product.ProductVariantUpdateRequest;
import com.example.Tech.dto.response.product.ProductVariantResponse;

import java.util.List;

public interface ProductVariantService {

    List<ProductVariantResponse> getByProductId(Long productId);

    ProductVariantResponse getById(Long id);

    ProductVariantResponse create(ProductVariantCreateRequest request);

    ProductVariantResponse update(Long id, ProductVariantUpdateRequest request);

    void delete(Long id);

    ProductVariantResponse addAttributeValue(Long variantId, Integer valueId);

    void removeAttributeValue(Long variantId, Integer valueId);
}
