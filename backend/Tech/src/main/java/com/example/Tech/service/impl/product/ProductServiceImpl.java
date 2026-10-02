package com.example.Tech.service.impl.product;

import com.example.Tech.dto.request.product.ProductCreateRequest;
import com.example.Tech.dto.request.product.ProductImageInput;
import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.dto.request.product.ProductUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.mapper.product.ProductImageMapper;
import com.example.Tech.mapper.product.ProductMapper;
import com.example.Tech.repository.product.BrandRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductFilterSpecifications;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.service.product.ProductService;
import com.example.Tech.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductImageRepository imageRepository;
    private final ProductMapper productMapper;
    private final ProductImageMapper imageMapper;

    @Override
    public PageResponse<ProductResponse> search(ProductSearchRequest filter, Pageable pageable) {
        if (filter.minPrice() != null && filter.maxPrice() != null
                && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "minPrice must not exceed maxPrice");
        }
        Page<Product> page = productRepository.findAll(ProductFilterSpecifications.matching(filter), pageable);
        Map<Long, String> imageUrls = primaryImageUrls(page.getContent().stream().map(Product::getId).toList());
        return PageResponse.from(page.map(product -> productMapper.toResponse(product, imageUrls.get(product.getId()))));
    }

    @Override
    public ProductResponse getById(Long id) {
        return toResponse(findProduct(id));
    }

    /**
     * The product and its images are saved in one transaction: exactly one primary image and at least one
     * secondary image (the count is checked by the request validation, the primary rule here).
     */
    @Override
    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        List<ProductImageInput> images = request.images() != null ? request.images() : List.of();
        long primaryCount = images.stream().filter(image -> Boolean.TRUE.equals(image.isPrimary())).count();
        if (primaryCount != 1) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_DATA, "Exactly one image must be primary");
        }
        validatePrices(request.basePrice(), request.discountPrice());
        String slug = resolveSlug(request.slug(), request.name());
        String sku = normalizeSku(request.sku());
        if (productRepository.existsBySlug(slug)) {
            throw duplicate("slug", slug);
        }
        if (sku != null && productRepository.existsBySku(sku)) {
            throw duplicate("sku", sku);
        }

        Product product = productMapper.toEntity(request);
        product.setSlug(slug);
        product.setSku(sku);
        product.setCategory(findCategory(request.categoryId()));
        product.setBrand(findBrand(request.brandId()));

        Product saved = productRepository.save(product);

        List<ProductImage> imageEntities = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            ProductImage image = imageMapper.toEntity(images.get(i), i);
            image.setProduct(saved);
            imageEntities.add(image);
        }
        imageRepository.saveAll(imageEntities);
        String primaryImageUrl = imageEntities.stream()
                .filter(ProductImage::getPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);

        log.info("Created product id={} with {} image(s)", saved.getId(), imageEntities.size());
        return productMapper.toResponse(saved, primaryImageUrl);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = findProduct(id);

        validatePrices(request.basePrice(), request.discountPrice());
        String slug = resolveSlug(request.slug(), request.name());
        String sku = normalizeSku(request.sku());
        if (productRepository.existsBySlugAndIdNot(slug, id)) {
            throw duplicate("slug", slug);
        }
        if (sku != null && productRepository.existsBySkuAndIdNot(sku, id)) {
            throw duplicate("sku", sku);
        }

        Category category = findCategory(request.categoryId());
        Brand brand = findBrand(request.brandId());

        productMapper.updateEntity(product, request);
        product.setSlug(slug);
        product.setSku(sku);
        product.setCategory(category);
        product.setBrand(brand);

        Product saved = productRepository.saveAndFlush(product);
        log.info("Updated product id={}", id);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        product.setDeletedAt(LocalDateTime.now());
        product.setActive(false);
        log.info("Soft-deleted product id={}", id);
    }

    private ProductResponse toResponse(Product product) {
        return productMapper.toResponse(product, primaryImageUrls(List.of(product.getId())).get(product.getId()));
    }

    /** One query for all products: the best image of each (primary, else the first by display order). */
    private Map<Long, String> primaryImageUrls(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> urls = new HashMap<>();
        for (ProductImage image : imageRepository.findAllByProductIdInBestFirst(productIds)) {
            urls.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
        }
        return urls;
    }

    private Product findProduct(Long id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND, id));
    }

    private Category findCategory(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND, categoryId));
    }

    private Brand findBrand(Integer brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BRAND_NOT_FOUND, brandId));
    }

    private void validatePrices(BigDecimal basePrice, BigDecimal discountPrice) {
        if (discountPrice != null && discountPrice.compareTo(basePrice) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_DATA,
                    "Discount price must not exceed base price");
        }
    }

    private String resolveSlug(String slug, String name) {
        String resolved = SlugUtil.resolve(slug, name);
        if (resolved.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_DATA, "Cannot generate slug from product name");
        }
        return resolved;
    }

    /** Blank SKUs are stored as NULL so they do not collide on the unique constraint. */
    private String normalizeSku(String sku) {
        return sku == null || sku.isBlank() ? null : sku.trim();
    }

    private BusinessException duplicate(String field, String value) {
        return new BusinessException(ErrorCode.DUPLICATE_PRODUCT,
                "Product with %s '%s' already exists".formatted(field, value));
    }
}
