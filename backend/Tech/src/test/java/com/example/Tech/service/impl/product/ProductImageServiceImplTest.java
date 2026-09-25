package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.ProductImageCreateRequest;
import com.example.Tech.dto.request.product.ProductImageUpdateRequest;
import com.example.Tech.dto.response.product.ProductImageResponse;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.product.ProductImageMapper;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceImplTest {

    @Mock
    private ProductImageRepository imageRepository;

    @Mock
    private ProductRepository productRepository;

    private ProductImageServiceImpl imageService;

    @BeforeEach
    void setUp() {
        imageService = new ProductImageServiceImpl(imageRepository, productRepository, new ProductImageMapper());
    }

    @Test
    void create_nonPrimary_doesNotTouchOtherImages() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product()));
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> {
            ProductImage image = invocation.getArgument(0);
            image.setId(20L);
            return image;
        });

        ProductImageResponse response = imageService.create(
                new ProductImageCreateRequest(1L, " https://cdn/img.jpg ", "alt", 1, null));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.imageUrl()).isEqualTo("https://cdn/img.jpg");
        assertThat(response.isPrimary()).isFalse();
        verify(imageRepository, never()).findAllByProductIdAndPrimaryTrue(anyLong());
    }

    @Test
    void create_primary_unsetsExistingPrimary() {
        ProductImage oldPrimary = image(10L, true);
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product()));
        when(imageRepository.findAllByProductIdAndPrimaryTrue(1L)).thenReturn(List.of(oldPrimary));
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductImageResponse response = imageService.create(
                new ProductImageCreateRequest(1L, "https://cdn/new.jpg", null, 0, true));

        assertThat(response.isPrimary()).isTrue();
        assertThat(oldPrimary.getPrimary()).isFalse();
    }

    @Test
    void create_productNotFound_throws() {
        when(productRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imageService.create(new ProductImageCreateRequest(99L, "u", null, null, null)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void update_setPrimary_unsetsOthersButKeepsItself() {
        ProductImage target = image(11L, false);
        ProductImage oldPrimary = image(10L, true);
        when(imageRepository.findByIdAndProductDeletedAtIsNull(11L)).thenReturn(Optional.of(target));
        when(imageRepository.findAllByProductIdAndPrimaryTrue(1L)).thenReturn(List.of(oldPrimary));
        when(imageRepository.saveAndFlush(target)).thenReturn(target);

        ProductImageResponse response = imageService.update(11L,
                new ProductImageUpdateRequest("https://cdn/b.jpg", null, 2, true));

        assertThat(response.isPrimary()).isTrue();
        assertThat(oldPrimary.getPrimary()).isFalse();
    }

    @Test
    void update_alreadyPrimary_staysPrimary() {
        ProductImage target = image(10L, true);
        when(imageRepository.findByIdAndProductDeletedAtIsNull(10L)).thenReturn(Optional.of(target));
        when(imageRepository.findAllByProductIdAndPrimaryTrue(1L)).thenReturn(List.of(target));
        when(imageRepository.saveAndFlush(target)).thenReturn(target);

        assertThat(imageService.update(10L, new ProductImageUpdateRequest("u", null, null, true)).isPrimary())
                .isTrue();
    }

    @Test
    void update_primaryOmitted_keepsFlag() {
        ProductImage target = image(10L, true);
        when(imageRepository.findByIdAndProductDeletedAtIsNull(10L)).thenReturn(Optional.of(target));
        when(imageRepository.saveAndFlush(target)).thenReturn(target);

        ProductImageResponse response = imageService.update(10L, new ProductImageUpdateRequest("u2", "alt", 3, null));

        assertThat(response.isPrimary()).isTrue();
        assertThat(response.displayOrder()).isEqualTo(3);
        verify(imageRepository, never()).findAllByProductIdAndPrimaryTrue(anyLong());
    }

    @Test
    void update_unsetPrimary() {
        ProductImage target = image(10L, true);
        when(imageRepository.findByIdAndProductDeletedAtIsNull(10L)).thenReturn(Optional.of(target));
        when(imageRepository.saveAndFlush(target)).thenReturn(target);

        assertThat(imageService.update(10L, new ProductImageUpdateRequest("u", null, null, false)).isPrimary())
                .isFalse();
    }

    @Test
    void getById_notFound_throws() {
        when(imageRepository.findByIdAndProductDeletedAtIsNull(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imageService.getById(5L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_IMAGE_NOT_FOUND);
    }

    @Test
    void getByProductId_returnsOrderedImages() {
        when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product()));
        when(imageRepository.findAllByProductIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(image(10L, true), image(11L, false)));

        assertThat(imageService.getByProductId(1L)).extracting(ProductImageResponse::id).containsExactly(10L, 11L);
    }

    @Test
    void delete_success() {
        ProductImage target = image(10L, false);
        when(imageRepository.findByIdAndProductDeletedAtIsNull(10L)).thenReturn(Optional.of(target));

        imageService.delete(10L);

        verify(imageRepository).delete(target);
    }

    private static Product product() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Galaxy S26");
        return product;
    }

    private static ProductImage image(Long id, boolean primary) {
        ProductImage image = new ProductImage();
        image.setId(id);
        image.setProduct(product());
        image.setImageUrl("https://cdn/" + id + ".jpg");
        image.setPrimary(primary);
        return image;
    }
}
