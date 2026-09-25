package com.example.Tech.service.product;

import com.example.Tech.dto.request.product.ProductImageCreateRequest;
import com.example.Tech.dto.request.product.ProductImageUpdateRequest;
import com.example.Tech.dto.response.product.ProductImageResponse;

import java.util.List;

public interface ProductImageService {

    List<ProductImageResponse> getByProductId(Long productId);

    ProductImageResponse getById(Long id);

    ProductImageResponse create(ProductImageCreateRequest request);

    ProductImageResponse update(Long id, ProductImageUpdateRequest request);

    void delete(Long id);
}
