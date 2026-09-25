package com.example.Tech.service.product;

import com.example.Tech.dto.request.product.ProductCreateRequest;
import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.dto.request.product.ProductUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    /**
     * Paginated product list; soft-deleted products are always excluded.
     */
    PageResponse<ProductResponse> search(ProductSearchRequest filter, Pageable pageable);

    ProductResponse getById(Long id);

    ProductResponse create(ProductCreateRequest request);

    ProductResponse update(Long id, ProductUpdateRequest request);

    /**
     * Soft delete: sets deleted_at and deactivates the product.
     */
    void delete(Long id);
}
