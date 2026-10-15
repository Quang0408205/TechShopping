package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.ProductImageCreateRequest;
import com.example.Tech.dto.request.product.ProductImageUpdateRequest;
import com.example.Tech.dto.response.product.ProductImageResponse;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.ProductImageMapper;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.service.product.ProductImageService;
import com.example.Tech.service.upload.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductImageServiceImpl implements ProductImageService {

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final ProductImageMapper imageMapper;
    private final ImageStorageService imageStorageService;

    @Override
    public List<ProductImageResponse> getByProductId(Long productId) {
        Product product = findProduct(productId);
        return imageRepository.findAllByProductIdOrderByDisplayOrderAscIdAsc(product.getId()).stream()
                .map(imageMapper::toResponse)
                .toList();
    }

    @Override
    public ProductImageResponse getById(Long id) {
        return imageMapper.toResponse(findImage(id));
    }

    /**
     * At most {@link #MAX_IMAGES_PER_PRODUCT} images per product. The first image of a product that has no
     * primary image (older crawled products) becomes primary even when isPrimary is omitted.
     */
    @Override
    @Transactional
    public ProductImageResponse create(ProductImageCreateRequest request) {
        Product product = findProduct(request.productId());
        if (imageRepository.countByProductId(product.getId()) >= MAX_IMAGES_PER_PRODUCT) {
            throw new BusinessException(ErrorCode.PRODUCT_IMAGE_LIMIT_EXCEEDED,
                    "A product can have at most %d images".formatted(MAX_IMAGES_PER_PRODUCT));
        }

        ProductImage image = imageMapper.toEntity(request);
        image.setProduct(product);
        if (Boolean.TRUE.equals(request.isPrimary())) {
            unsetOtherPrimaryImages(product.getId(), null);
            image.setPrimary(true);
        } else if (!imageRepository.existsByProductIdAndPrimaryTrue(product.getId())) {
            image.setPrimary(true);
        }

        ProductImage saved = imageRepository.save(image);
        log.info("Created product image id={} for product id={}", saved.getId(), product.getId());
        return imageMapper.toResponse(saved);
    }

    /**
     * isPrimary=false on the primary image is refused: the product would lose its primary image.
     * When the URL changes, the old file is deleted if it was one of our uploads.
     */
    @Override
    @Transactional
    public ProductImageResponse update(Long id, ProductImageUpdateRequest request) {
        ProductImage image = findImage(id);
        if (Boolean.FALSE.equals(request.isPrimary()) && Boolean.TRUE.equals(image.getPrimary())) {
            throw new BusinessException(ErrorCode.PRIMARY_IMAGE_REQUIRED,
                    ErrorCode.PRIMARY_IMAGE_REQUIRED.getDefaultMessage());
        }
        String previousUrl = image.getImageUrl();

        imageMapper.updateEntity(image, request);
        if (Boolean.TRUE.equals(request.isPrimary())) {
            unsetOtherPrimaryImages(image.getProduct().getId(), id);
            image.setPrimary(true);
        }

        ProductImage saved = imageRepository.saveAndFlush(image);
        if (!saved.getImageUrl().equals(previousUrl)) {
            imageStorageService.deleteAfterCommit(previousUrl);
        }
        log.info("Updated product image id={}", id);
        return imageMapper.toResponse(saved);
    }

    /**
     * The last image of a product cannot be deleted. Deleting the primary image promotes the next image
     * (display order, then id). An uploaded file is deleted after commit.
     */
    @Override
    @Transactional
    public void delete(Long id) {
        ProductImage image = findImage(id);
        List<ProductImage> images = imageRepository.findAllByProductIdOrderByDisplayOrderAscIdAsc(
                image.getProduct().getId());
        if (images.size() <= 1) {
            throw new BusinessException(ErrorCode.LAST_PRODUCT_IMAGE, ErrorCode.LAST_PRODUCT_IMAGE.getDefaultMessage());
        }

        imageRepository.delete(image);
        if (Boolean.TRUE.equals(image.getPrimary())) {
            images.stream()
                    .filter(other -> !other.getId().equals(id))
                    .findFirst()
                    .ifPresent(next -> {
                        next.setPrimary(true);
                        log.info("Image id={} is now the primary image of product id={}",
                                next.getId(), image.getProduct().getId());
                    });
        }
        imageStorageService.deleteAfterCommit(image.getImageUrl());
        log.info("Deleted product image id={}", id);
    }

    /**
     * A product has at most one primary image: clears the flag on the others (same transaction).
     */
    private void unsetOtherPrimaryImages(Long productId, Long keepImageId) {
        for (ProductImage other : imageRepository.findAllByProductIdAndPrimaryTrue(productId)) {
            if (!other.getId().equals(keepImageId)) {
                other.setPrimary(false);
            }
        }
    }

    private ProductImage findImage(Long id) {
        return imageRepository.findByIdAndProductDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND, id));
    }

    private Product findProduct(Long productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, productId));
    }
}
