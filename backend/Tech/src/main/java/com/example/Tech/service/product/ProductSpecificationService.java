package com.example.Tech.service.product;

import com.example.Tech.dto.request.product.ProductSpecificationCreateRequest;
import com.example.Tech.dto.request.product.ProductSpecificationUpdateRequest;
import com.example.Tech.dto.response.product.ProductSpecificationResponse;

import java.util.List;

public interface ProductSpecificationService {

    List<ProductSpecificationResponse> getByProductId(Long productId);

    ProductSpecificationResponse getById(Long id);

    ProductSpecificationResponse create(ProductSpecificationCreateRequest request);

    ProductSpecificationResponse update(Long id, ProductSpecificationUpdateRequest request);

    void delete(Long id);
}
