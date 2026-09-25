package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.CategoryCreateRequest;
import com.example.Tech.dto.request.product.CategoryUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.CategoryResponse;
import com.example.Tech.entity.product.Category;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.CategoryMapper;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.service.product.CategoryService;
import com.example.Tech.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public PageResponse<CategoryResponse> getAll(Pageable pageable) {
        return PageResponse.from(categoryRepository.findAll(pageable).map(categoryMapper::toResponse));
    }

    @Override
    public CategoryResponse getById(Integer id) {
        return categoryMapper.toResponse(findCategory(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        String slug = resolveSlug(request.slug(), request.name());
        if (categoryRepository.existsBySlug(slug)) {
            throw duplicateSlug(slug);
        }

        Category category = categoryMapper.toEntity(request);
        category.setSlug(slug);
        category.setParent(request.parentId() != null ? findCategory(request.parentId()) : null);

        Category saved = categoryRepository.save(category);
        log.info("Created category id={}", saved.getId());
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse update(Integer id, CategoryUpdateRequest request) {
        Category category = findCategory(id);

        String slug = resolveSlug(request.slug(), request.name());
        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw duplicateSlug(slug);
        }

        Category parent = request.parentId() != null ? findCategory(request.parentId()) : null;
        ensureNotDescendant(category, parent);

        categoryMapper.updateEntity(category, request);
        category.setSlug(slug);
        category.setParent(parent);

        Category saved = categoryRepository.saveAndFlush(category);
        log.info("Updated category id={}", id);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Category category = findCategory(id);
        if (categoryRepository.existsByParentId(id)) {
            throw new BusinessException(ErrorCode.RESOURCE_IN_USE,
                    "Category %d still has child categories".formatted(id));
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException(ErrorCode.RESOURCE_IN_USE,
                    "Category %d is still used by products".formatted(id));
        }
        categoryRepository.delete(category);
        categoryRepository.flush();
        log.info("Deleted category id={}", id);
    }

    private Category findCategory(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND, id));
    }

    private String resolveSlug(String slug, String name) {
        String resolved = SlugUtil.resolve(slug, name);
        if (resolved.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Cannot generate slug from category name");
        }
        return resolved;
    }

    private BusinessException duplicateSlug(String slug) {
        return new BusinessException(ErrorCode.DUPLICATE_CATEGORY,
                "Category with slug '%s' already exists".formatted(slug));
    }

    /**
     * A category cannot become its own parent or a child of one of its descendants.
     */
    private void ensureNotDescendant(Category category, Category newParent) {
        for (Category current = newParent; current != null; current = current.getParent()) {
            if (current.getId().equals(category.getId())) {
                throw new BusinessException(ErrorCode.INVALID_CATEGORY_PARENT,
                        "Category %d cannot be placed under itself or its descendant".formatted(category.getId()));
            }
        }
    }
}
