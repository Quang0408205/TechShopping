package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.ProductSpecificationCreateRequest;
import com.example.Tech.dto.request.product.ProductSpecificationUpdateRequest;
import com.example.Tech.dto.response.product.ProductSpecificationResponse;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductSpecification;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.product.ProductSpecificationMapper;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductSpecificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSpecificationServiceImplTest {

    @Mock
    private ProductSpecificationRepository specificationRepository;

    @Mock
    private ProductRepository productRepository;

    private ProductSpecificationServiceImpl specificationService;

    @BeforeEach
    void setUp() {
        specificationService = new ProductSpecificationServiceImpl(specificationRepository, productRepository,
                new ProductSpecificationMapper());
    }

    @Test
    void create_success_trimsText() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product()));
        when(specificationRepository.save(any(ProductSpecification.class))).thenAnswer(invocation -> {
            ProductSpecification specification = invocation.getArgument(0);
            specification.setId(7L);
            return specification;
        });

        ProductSpecificationResponse response = specificationService.create(
                new ProductSpecificationCreateRequest(1L, " Màn hình ", " 6.2 inch ", 1));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.specName()).isEqualTo("Màn hình");
        assertThat(response.specValue()).isEqualTo("6.2 inch");
    }

    @Test
    void create_productNotFound_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specificationService.create(
                new ProductSpecificationCreateRequest(99L, "Chip", "Exynos", null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        verify(specificationRepository, never()).save(any());
    }

    @Test
    void getByProductId_returnsOrdered() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product()));
        when(specificationRepository.findAllByProductIdOrderBySpecOrderAscIdAsc(1L))
                .thenReturn(List.of(specification(1L, "Màn hình", 1), specification(2L, "Chip", 2)));

        assertThat(specificationService.getByProductId(1L))
                .extracting(ProductSpecificationResponse::specName)
                .containsExactly("Màn hình", "Chip");
    }

    @Test
    void getByProductId_productNotFound_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specificationService.getByProductId(1L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void getById_notFound_throws() {
        when(specificationRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specificationService.getById(5L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_SPECIFICATION_NOT_FOUND);
    }

    @Test
    void update_success() {
        ProductSpecification existing = specification(5L, "Chip", 2);
        when(specificationRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(existing));
        when(specificationRepository.saveAndFlush(existing)).thenReturn(existing);

        ProductSpecificationResponse response = specificationService.update(5L,
                new ProductSpecificationUpdateRequest("Vi xử lý", "Snapdragon 8 Elite", 3));

        assertThat(response.specName()).isEqualTo("Vi xử lý");
        assertThat(response.specValue()).isEqualTo("Snapdragon 8 Elite");
        assertThat(response.specOrder()).isEqualTo(3);
    }

    @Test
    void update_notFound_throws() {
        when(specificationRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> specificationService.update(5L,
                new ProductSpecificationUpdateRequest("a", "b", null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_SPECIFICATION_NOT_FOUND);
    }

    @Test
    void delete_success() {
        ProductSpecification existing = specification(5L, "Chip", 2);
        when(specificationRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.of(existing));

        specificationService.delete(5L);

        verify(specificationRepository).delete(existing);
    }

    private static Product product() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Galaxy S26");
        return product;
    }

    private static ProductSpecification specification(Long id, String name, Integer order) {
        ProductSpecification specification = new ProductSpecification();
        specification.setId(id);
        specification.setProduct(product());
        specification.setSpecName(name);
        specification.setSpecValue("value");
        specification.setSpecOrder(order);
        return specification;
    }
}
