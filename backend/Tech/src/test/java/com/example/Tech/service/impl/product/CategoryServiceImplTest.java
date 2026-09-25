package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.CategoryCreateRequest;
import com.example.Tech.dto.request.product.CategoryUpdateRequest;
import com.example.Tech.dto.response.product.CategoryResponse;
import com.example.Tech.entity.product.Category;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.CategoryMapper;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, productRepository, new CategoryMapper());
    }

    @Test
    void create_success_generatesSlugFromName() {
        CategoryCreateRequest request = new CategoryCreateRequest("Điện thoại", null, null, null, null, 1, null);
        when(categoryRepository.existsBySlug("dien-thoai")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(1);
            return category;
        });

        CategoryResponse response = categoryService.create(request);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.slug()).isEqualTo("dien-thoai");
        assertThat(response.isActive()).isTrue();
        assertThat(response.parentId()).isNull();
    }

    @Test
    void create_withParent_setsParent() {
        Category parent = category(10, "Thiết bị", null);
        CategoryCreateRequest request = new CategoryCreateRequest("Laptop", "laptop", null, 10, null, null, false);
        when(categoryRepository.existsBySlug("laptop")).thenReturn(false);
        when(categoryRepository.findById(10)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.create(request);

        assertThat(response.parentId()).isEqualTo(10);
        assertThat(response.parentName()).isEqualTo("Thiết bị");
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void create_duplicateSlug_throwsConflict() {
        CategoryCreateRequest request = new CategoryCreateRequest("Laptop", "laptop", null, null, null, null, null);
        when(categoryRepository.existsBySlug("laptop")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_CATEGORY);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_parentNotFound_throwsNotFound() {
        CategoryCreateRequest request = new CategoryCreateRequest("Laptop", null, null, 99, null, null, null);
        when(categoryRepository.existsBySlug("laptop")).thenReturn(false);
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void getById_success() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1, "Laptop", null)));

        assertThat(categoryService.getById(1).name()).isEqualTo("Laptop");
    }

    @Test
    void getById_notFound_throws() {
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_success() {
        Category existing = category(1, "Laptop", null);
        CategoryUpdateRequest request = new CategoryUpdateRequest("Laptop Gaming", null, "desc", null, null, 2, null);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsBySlugAndIdNot("laptop-gaming", 1)).thenReturn(false);
        when(categoryRepository.saveAndFlush(existing)).thenReturn(existing);

        CategoryResponse response = categoryService.update(1, request);

        assertThat(response.name()).isEqualTo("Laptop Gaming");
        assertThat(response.slug()).isEqualTo("laptop-gaming");
        assertThat(response.displayOrder()).isEqualTo(2);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void update_parentIsSelf_throwsInvalidParent() {
        Category existing = category(1, "Laptop", null);
        CategoryUpdateRequest request = new CategoryUpdateRequest("Laptop", null, null, 1, null, null, null);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsBySlugAndIdNot("laptop", 1)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.update(1, request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CATEGORY_PARENT);
    }

    @Test
    void update_parentIsDescendant_throwsInvalidParent() {
        Category root = category(1, "Root", null);
        Category child = category(2, "Child", root);
        Category grandChild = category(3, "Grand child", child);
        CategoryUpdateRequest request = new CategoryUpdateRequest("Root", null, null, 3, null, null, null);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(root));
        when(categoryRepository.findById(3)).thenReturn(Optional.of(grandChild));
        when(categoryRepository.existsBySlugAndIdNot("root", 1)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.update(1, request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CATEGORY_PARENT);
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void delete_success() {
        Category existing = category(1, "Laptop", null);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByParentId(1)).thenReturn(false);
        when(productRepository.existsByCategoryId(1)).thenReturn(false);

        categoryService.delete(1);

        verify(categoryRepository).delete(existing);
    }

    @Test
    void delete_usedByProducts_throwsResourceInUse() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1, "Laptop", null)));
        when(categoryRepository.existsByParentId(1)).thenReturn(false);
        when(productRepository.existsByCategoryId(1)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_IN_USE);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_withChildren_throwsResourceInUse() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1, "Laptop", null)));
        when(categoryRepository.existsByParentId(1)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_IN_USE);
        verify(categoryRepository, never()).delete(any());
    }

    private static Category category(Integer id, String name, Category parent) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(name.toLowerCase().replace(' ', '-'));
        category.setParent(parent);
        return category;
    }
}
