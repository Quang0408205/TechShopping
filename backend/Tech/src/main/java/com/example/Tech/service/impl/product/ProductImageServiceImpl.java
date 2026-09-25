package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.ProductImageCreateRequest;
import com.example.Tech.dto.request.product.ProductImageUpdateRequest;
import com.example.Tech.dto.response.product.ProductImageResponse;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.ProductImageMapper;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.service.product.ProductImageService;
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

    @Override
    @Transactional
    public ProductImageResponse create(ProductImageCreateRequest request) {
        Product product = findProduct(request.productId());

        ProductImage image = imageMapper.toEntity(request);
        image.setProduct(product);
        if (Boolean.TRUE.equals(request.isPrimary())) {
            unsetOtherPrimaryImages(product.getId(), null);
            image.setPrimary(true);
        }

        ProductImage saved = imageRepository.save(image);
        log.info("Created product image id={} for product id={}", saved.getId(), product.getId());
        return imageMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductImageResponse update(Long id, ProductImageUpdateRequest request) {
        ProductImage image = findImage(id);

        imageMapper.updateEntity(image, request);
        if (Boolean.TRUE.equals(request.isPrimary())) {
            unsetOtherPrimaryImages(image.getProduct().getId(), id);
            image.setPrimary(true);
        } else if (Boolean.FALSE.equals(request.isPrimary())) {
            image.setPrimary(false);
        }

        ProductImage saved = imageRepository.saveAndFlush(image);
        log.info("Updated product image id={}", id);
        return imageMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProductImage image = findImage(id);
        imageRepository.delete(image);
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
