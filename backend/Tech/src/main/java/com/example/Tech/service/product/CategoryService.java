package com.example.Tech.service.product;

import com.example.Tech.dto.request.product.CategoryCreateRequest;
import com.example.Tech.dto.request.product.CategoryUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.BrandResponse;
import com.example.Tech.dto.response.product.CategoryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {

    PageResponse<CategoryResponse> getAll(Pageable pageable);

    CategoryResponse getById(Integer id);

    /** Brands that have products on sale in the category, by name. */
    List<BrandResponse> getBrands(Integer id);

    CategoryResponse create(CategoryCreateRequest request);

    CategoryResponse update(Integer id, CategoryUpdateRequest request);

    /**
     * 409 RESOURCE_IN_USE (details.reason CHILDREN / PRODUCTS / HISTORY) unless the category is empty, or
     * withProducts is true and none of its products has orders, stock or reviews: then they are deleted with it.
     */
    void delete(Integer id, boolean withProducts);
}
