package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.ProductCreateRequest;
import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.dto.request.product.ProductUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.ProductMapper;
import com.example.Tech.repository.product.BrandRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, categoryRepository, brandRepository,
                new ProductMapper());
    }

    @Test
    void create_success_appliesDefaultsAndRelations() {
        ProductCreateRequest request = createRequest("Điện thoại iPhone 18 Pro 256GB", 1, 2,
                "38990000", "35990000", "TGDD-370977");
        when(productRepository.existsBySlug("dien-thoai-iphone-18-pro-256gb")).thenReturn(false);
        when(productRepository.existsBySku("TGDD-370977")).thenReturn(false);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category()));
        when(brandRepository.findById(2)).thenReturn(Optional.of(brand()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(100L);
            return product;
        });

        ProductResponse response = productService.create(request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.slug()).isEqualTo("dien-thoai-iphone-18-pro-256gb");
        assertThat(response.categoryName()).isEqualTo("Điện thoại");
        assertThat(response.brandName()).isEqualTo("Apple");
        assertThat(response.stockQuantity()).isZero();
        assertThat(response.warrantyMonths()).isEqualTo(12);
        assertThat(response.rating()).isEqualByComparingTo("0");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void create_withoutBrand_success() {
        ProductCreateRequest request = createRequest("Ốp lưng", 1, null, "100000", null, " ");
        when(productRepository.existsBySlug("op-lung")).thenReturn(false);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category()));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.create(request);

        assertThat(response.brandId()).isNull();
        assertThat(response.sku()).isNull();
    }

    @Test
    void create_discountAboveBasePrice_throwsInvalidData() {
        ProductCreateRequest request = createRequest("Laptop", 1, null, "100", "200", null);

        assertThatThrownBy(() -> productService.create(request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PRODUCT_DATA);
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_duplicateSku_throwsConflict() {
        ProductCreateRequest request = createRequest("Laptop", 1, null, "100", null, "SKU-1");
        when(productRepository.existsBySlug("laptop")).thenReturn(false);
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_PRODUCT);
    }

    @Test
    void create_categoryNotFound_throws() {
        ProductCreateRequest request = createRequest("Laptop", 99, null, "100", null, null);
        when(productRepository.existsBySlug("laptop")).thenReturn(false);
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void create_brandNotFound_throws() {
        ProductCreateRequest request = createRequest("Laptop", 1, 99, "100", null, null);
        when(productRepository.existsBySlug("laptop")).thenReturn(false);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category()));
        when(brandRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BRAND_NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_returnsPageResponse() {
        PageRequest pageable = PageRequest.of(1, 2);
        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(product()), pageable, 3));

        PageResponse<ProductResponse> page = productService.search(
                new ProductSearchRequest("laptop", 1, null, true, null, null), pageable);

        assertThat(page.content()).extracting(ProductResponse::name).containsExactly("Laptop");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void search_minPriceAboveMaxPrice_throwsValidationError() {
        ProductSearchRequest filter = new ProductSearchRequest(null, null, null, null,
                new BigDecimal("200"), new BigDecimal("100"));

        assertThatThrownBy(() -> productService.search(filter, PageRequest.of(0, 20)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(productRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getById_success() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product()));

        assertThat(productService.getById(1L).name()).isEqualTo("Laptop");
    }

    @Test
    void getById_notFoundOrDeleted_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void update_success_keepsReadOnlyCounters() {
        Product existing = product();
        existing.setViewCount(50);
        ProductUpdateRequest request = new ProductUpdateRequest("Laptop Pro", null, "desc", 1, null,
                new BigDecimal("2000"), new BigDecimal("1500"), 5, "SKU-2", null, 24, null);
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySlugAndIdNot("laptop-pro", 1L)).thenReturn(false);
        when(productRepository.existsBySkuAndIdNot("SKU-2", 1L)).thenReturn(false);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category()));
        when(productRepository.saveAndFlush(existing)).thenReturn(existing);

        ProductResponse response = productService.update(1L, request);

        assertThat(response.name()).isEqualTo("Laptop Pro");
        assertThat(response.discountPrice()).isEqualByComparingTo("1500");
        assertThat(response.stockQuantity()).isEqualTo(5);
        assertThat(response.warrantyMonths()).isEqualTo(24);
        assertThat(response.brandId()).isNull();
        assertThat(response.viewCount()).isEqualTo(50);
    }

    @Test
    void update_duplicateSlug_throwsConflict() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product()));
        when(productRepository.existsBySlugAndIdNot("macbook", 1L)).thenReturn(true);
        ProductUpdateRequest request = new ProductUpdateRequest("Macbook", null, null, 1, null,
                new BigDecimal("100"), null, null, null, null, null, null);

        assertThatThrownBy(() -> productService.update(1L, request))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_PRODUCT);
    }

    @Test
    void delete_softDeletesProduct() {
        Product existing = product();
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(existing));

        productService.delete(1L);

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getActive()).isFalse();
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void delete_notFound_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static ProductCreateRequest createRequest(String name, Integer categoryId, Integer brandId,
                                                      String basePrice, String discountPrice, String sku) {
        return new ProductCreateRequest(name, null, null, categoryId, brandId, new BigDecimal(basePrice),
                discountPrice != null ? new BigDecimal(discountPrice) : null, null, sku, null, null, null);
    }

    private static Category category() {
        Category category = new Category();
        category.setId(1);
        category.setName("Điện thoại");
        category.setSlug("dien-thoai");
        return category;
    }

    private static Brand brand() {
        Brand brand = new Brand();
        brand.setId(2);
        brand.setName("Apple");
        brand.setSlug("apple");
        return brand;
    }

    private static Product product() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setSlug("laptop");
        product.setBasePrice(new BigDecimal("1000"));
        product.setCategory(category());
        return product;
    }
}
