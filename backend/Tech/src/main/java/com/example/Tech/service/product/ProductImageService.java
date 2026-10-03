package com.example.Tech.service.product;

import com.example.Tech.dto.request.product.ProductImageCreateRequest;
import com.example.Tech.dto.request.product.ProductImageUpdateRequest;
import com.example.Tech.dto.response.product.ProductImageResponse;

import java.util.List;

public interface ProductImageService {

    /** Upper limit of images per product (create and later additions). */
    int MAX_IMAGES_PER_PRODUCT = 10;

    /** A new product needs one primary image and at least one secondary image. */
    int MIN_IMAGES_ON_CREATE = 2;

    List<ProductImageResponse> getByProductId(Long productId);

    ProductImageResponse getById(Long id);

    ProductImageResponse create(ProductImageCreateRequest request);

    ProductImageResponse update(Long id, ProductImageUpdateRequest request);

    void delete(Long id);
}
