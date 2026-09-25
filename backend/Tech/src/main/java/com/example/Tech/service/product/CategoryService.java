package com.example.Tech.service.product;

import com.example.Tech.dto.request.product.CategoryCreateRequest;
import com.example.Tech.dto.request.product.CategoryUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.CategoryResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    PageResponse<CategoryResponse> getAll(Pageable pageable);

    CategoryResponse getById(Integer id);

    CategoryResponse create(CategoryCreateRequest request);

    CategoryResponse update(Integer id, CategoryUpdateRequest request);

    void delete(Integer id);
}
