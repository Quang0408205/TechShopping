package com.example.Tech.mapper.product;

import com.example.Tech.dto.request.product.CategoryCreateRequest;
import com.example.Tech.dto.request.product.CategoryUpdateRequest;
import com.example.Tech.dto.response.product.CategoryResponse;
import com.example.Tech.entity.product.Category;
import org.springframework.stereotype.Component;

/**
 * Maps simple fields only; slug and parent are resolved by the service.
 */
@Component
public class CategoryMapper {

    public Category toEntity(CategoryCreateRequest request) {
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setIconUrl(request.iconUrl());
        category.setDisplayOrder(request.displayOrder());
        if (request.isActive() != null) {
            category.setActive(request.isActive());
        }
        return category;
    }

    public void updateEntity(Category category, CategoryUpdateRequest request) {
        category.setName(request.name());
        category.setDescription(request.description());
        category.setIconUrl(request.iconUrl());
        category.setDisplayOrder(request.displayOrder());
        if (request.isActive() != null) {
            category.setActive(request.isActive());
        }
    }

    public CategoryResponse toResponse(Category category) {
        Category parent = category.getParent();
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                parent != null ? parent.getId() : null,
                parent != null ? parent.getName() : null,
                category.getIconUrl(),
                category.getDisplayOrder(),
                category.getActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
