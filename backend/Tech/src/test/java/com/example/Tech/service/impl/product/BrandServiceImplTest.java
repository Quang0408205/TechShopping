package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.BrandCreateRequest;
import com.example.Tech.dto.request.product.BrandUpdateRequest;
import com.example.Tech.dto.response.product.BrandResponse;
import com.example.Tech.entity.product.Brand;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.BrandMapper;
import com.example.Tech.repository.product.BrandRepository;
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
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private ProductRepository productRepository;

    private BrandServiceImpl brandService;

    @BeforeEach
    void setUp() {
        brandService = new BrandServiceImpl(brandRepository, productRepository, new BrandMapper());
    }

    @Test
    void create_success() {
        BrandCreateRequest request = new BrandCreateRequest("Apple", null, null, "US brand", null, null);
        when(brandRepository.existsByName("Apple")).thenReturn(false);
        when(brandRepository.existsBySlug("apple")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> {
            Brand brand = invocation.getArgument(0);
            brand.setId(1);
            return brand;
        });

        BrandResponse response = brandService.create(request);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.slug()).isEqualTo("apple");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void create_duplicateName_throwsConflict() {
        BrandCreateRequest request = new BrandCreateRequest("Apple", null, null, null, null, null);
        when(brandRepository.existsByName("Apple")).thenReturn(true);

        assertThatThrownBy(() -> brandService.create(request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BRAND);
        verify(brandRepository, never()).save(any());
    }

    @Test
    void create_duplicateSlug_throwsConflict() {
        BrandCreateRequest request = new BrandCreateRequest("Apple Inc", "apple", null, null, null, null);
        when(brandRepository.existsByName("Apple Inc")).thenReturn(false);
        when(brandRepository.existsBySlug("apple")).thenReturn(true);

        assertThatThrownBy(() -> brandService.create(request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BRAND);
    }

    @Test
    void getById_success() {
        when(brandRepository.findById(1)).thenReturn(Optional.of(brand(1, "Samsung")));

        assertThat(brandService.getById(1).name()).isEqualTo("Samsung");
    }

    @Test
    void getById_notFound_throws() {
        when(brandRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.getById(1))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BRAND_NOT_FOUND);
    }

    @Test
    void update_success() {
        Brand existing = brand(1, "Samsung");
        BrandUpdateRequest request = new BrandUpdateRequest("Samsung Electronics", "samsung", null, null,
                "https://samsung.com", false);
        when(brandRepository.findById(1)).thenReturn(Optional.of(existing));
        when(brandRepository.existsByNameAndIdNot("Samsung Electronics", 1)).thenReturn(false);
        when(brandRepository.existsBySlugAndIdNot("samsung", 1)).thenReturn(false);
        when(brandRepository.saveAndFlush(existing)).thenReturn(existing);

        BrandResponse response = brandService.update(1, request);

        assertThat(response.name()).isEqualTo("Samsung Electronics");
        assertThat(response.websiteUrl()).isEqualTo("https://samsung.com");
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void update_duplicateName_throwsConflict() {
        when(brandRepository.findById(1)).thenReturn(Optional.of(brand(1, "Samsung")));
        when(brandRepository.existsByNameAndIdNot("Apple", 1)).thenReturn(true);
        BrandUpdateRequest request = new BrandUpdateRequest("Apple", null, null, null, null, null);

        assertThatThrownBy(() -> brandService.update(1, request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_BRAND);
    }

    @Test
    void delete_success() {
        Brand existing = brand(1, "Samsung");
        when(brandRepository.findById(1)).thenReturn(Optional.of(existing));
        when(productRepository.existsByBrandId(1)).thenReturn(false);

        brandService.delete(1);

        verify(brandRepository).delete(existing);
    }

    @Test
    void delete_usedByProducts_throwsResourceInUse() {
        when(brandRepository.findById(1)).thenReturn(Optional.of(brand(1, "Samsung")));
        when(productRepository.existsByBrandId(1)).thenReturn(true);

        assertThatThrownBy(() -> brandService.delete(1))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_IN_USE);
        verify(brandRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_throws() {
        when(brandRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.delete(1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Brand brand(Integer id, String name) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setName(name);
        brand.setSlug(name.toLowerCase());
        return brand;
    }
}
