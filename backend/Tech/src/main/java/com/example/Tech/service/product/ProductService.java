package com.example.Tech.service.product;

import com.example.Tech.dto.request.product.ProductCreateRequest;
import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.dto.request.product.ProductUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.dto.response.product.ProductSuggestResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    /**
     * Paginated product list; soft-deleted products are always excluded.
     */
    PageResponse<ProductResponse> search(ProductSearchRequest filter, Pageable pageable);

    /**
     * Search box suggestions: products on sale whose name / slug / brand contain every word of the
     * query, in stock first, then names starting with the first word, then the shortest names; at most {@code size}.
     */
    ProductSuggestResult suggest(String query, int size);

    ProductResponse getById(Long id);

    /**
     * Products that can be shown to customers (not deleted, active), in the order of the ids; other ids are
     * skipped. Used for lists whose order is decided elsewhere (recommendations).
     */
    List<ProductResponse> getActiveByIds(List<Long> ids);

    ProductResponse create(ProductCreateRequest request);

    ProductResponse update(Long id, ProductUpdateRequest request);

    /**
     * Soft delete: sets deleted_at and deactivates the product.
     */
    void delete(Long id);
}
