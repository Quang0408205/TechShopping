package com.example.Tech.service.impl.product;

import com.example.Tech.cache.ReadCache;
import com.example.Tech.dto.request.product.ProductCreateRequest;
import com.example.Tech.dto.request.product.ProductImageInput;
import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.dto.request.product.ProductUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.dto.response.product.ProductSuggestResult;
import com.example.Tech.dto.response.product.ProductSuggestionResponse;
import com.example.Tech.dto.response.product.ProductVariantOption;
import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
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
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.service.product.ProductFilterService;
import com.example.Tech.service.product.ProductService;
import com.example.Tech.service.promotion.EffectivePrice;
import com.example.Tech.service.promotion.PromotionPricingService;
import com.example.Tech.util.SlugUtil;
import com.example.Tech.service.inventory.StockAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductImageRepository imageRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductMapper productMapper;
    private final ProductImageMapper imageMapper;
    private final PromotionPricingService pricingService;
    private final StockAvailability stockAvailability;
    private final Clock clock;
    private final ReadCache cache;
    private final ProductFilterService filterService;

    @Override
    public PageResponse<ProductResponse> search(ProductSearchRequest filter, Pageable pageable) {
        if (filter.minPrice() != null && filter.maxPrice() != null
                && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "minPrice must not exceed maxPrice");
        }
        Page<Product> page = productRepository.findAll(ProductFilterSpecifications.matching(filter, LocalDateTime.now(clock)), pageable);
        Map<Long, String> imageUrls = primaryImageUrls(page.getContent().stream().map(Product::getId).toList());
        Map<Long, EffectivePrice> prices = pricingService.resolveProducts(page.getContent(), LocalDateTime.now(clock));
        Map<Long, Integer> available = stockAvailability.byProduct(page.getContent().stream().map(Product::getId).toList());
        Map<Long, List<ProductVariantOption>> variants = variantOptions(page.getContent());
        return PageResponse.from(page.map(product -> productMapper.toResponse(product, imageUrls.get(product.getId()),
                prices.get(product.getId()), available.getOrDefault(product.getId(), 0))
                .withVariants(variants.getOrDefault(product.getId(), List.of()))));
    }

    /** Card choices for a page of products: three queries in all (variants, promotions, stock), cheapest first. */
    private Map<Long, List<ProductVariantOption>> variantOptions(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        List<ProductVariant> variants = variantRepository.findAllByProductIdInOrderByIdAsc(
                products.stream().map(Product::getId).toList());
        Map<Long, EffectivePrice> prices = pricingService.resolveVariants(variants, LocalDateTime.now(clock));
        Map<Long, Integer> stock = stockAvailability.byVariant(variants.stream().map(ProductVariant::getId).toList());
        Map<Long, List<ProductVariantOption>> options = new HashMap<>();
        for (ProductVariant variant : variants) {
            EffectivePrice price = prices.get(variant.getId());
            options.computeIfAbsent(variant.getProduct().getId(), id -> new ArrayList<>())
                    .add(new ProductVariantOption(variant.getId(), optionLabel(variant), variant.getColor(),
                            price.unitPrice(), variant.getPrice(), stock.getOrDefault(variant.getId(), 0)));
        }
        options.values().forEach(list -> list.sort(Comparator.comparing(ProductVariantOption::effectivePrice)));
        return options;
    }

    private static String optionLabel(ProductVariant variant) {
        for (String text : new String[] {variant.getStorage(), variant.getRam(), variant.getVariantName()}) {
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }
        return "";
    }

    /** Matches looked at before ordering (stock / name), so a few in-stock items are not hidden behind old ids. */
    private static final int SUGGEST_POOL = 60;

    @Override
    public ProductSuggestResult suggest(String query, int size) {
        String text = query == null ? "" : query.trim();
        if (text.isEmpty()) {
            return new ProductSuggestResult(List.of(), 0);
        }
        Page<Product> page = productRepository.findAll(
                ProductFilterSpecifications.matching(new ProductSearchRequest(text, null, null, true, null, null)),
                PageRequest.of(0, SUGGEST_POOL, Sort.by("id")));
        List<Product> pool = page.getContent();
        List<Long> ids = pool.stream().map(Product::getId).toList();
        Map<Long, String> imageUrls = primaryImageUrls(ids);
        Map<Long, EffectivePrice> prices = pricingService.resolveProducts(pool, LocalDateTime.now(clock));
        Map<Long, Integer> available = stockAvailability.byProduct(ids);
        String firstWord = SlugUtil.toSlug(text.split("\\s+")[0]);
        Comparator<Product> order = Comparator
                .<Product>comparingInt(product -> available.getOrDefault(product.getId(), 0) > 0 ? 0 : 1)
                .thenComparingInt(product -> product.getSlug() != null && !firstWord.isEmpty()
                        && product.getSlug().startsWith(firstWord) ? 0 : 1)
                .thenComparingInt(product -> product.getName().length())
                .thenComparing(Product::getId);
        List<ProductSuggestionResponse> items = pool.stream()
                .sorted(order)
                .limit(size)
                .map(product -> {
                    EffectivePrice price = prices.get(product.getId());
                    return new ProductSuggestionResponse(product.getId(), product.getName(),
                            product.getBrand() == null ? null : product.getBrand().getName(),
                            product.getCategory() == null ? null : product.getCategory().getName(),
                            imageUrls.get(product.getId()), price.unitPrice(),
                            price.hasDiscount() ? price.originalPrice() : null,
                            available.getOrDefault(product.getId(), 0) > 0);
                })
                .toList();
        return new ProductSuggestResult(items, page.getTotalElements());
    }

    @Override
    public ProductResponse getById(Long id) {
        // the product is cached for 10 minutes; the stock figure is always read fresh
        ProductResponse product = cache.get(ReadCache.PRODUCT, id, new TypeReference<>() { },
                () -> toResponse(findProduct(id)));
        return product.withAvailableQuantity(stockAvailability.byProduct(List.of(id)).getOrDefault(id, 0));
    }

    @Override
    public List<ProductResponse> getActiveByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> byId = new HashMap<>();
        for (Product product : productRepository.findAllById(ids)) {
            if (product.getDeletedAt() == null && !Boolean.FALSE.equals(product.getActive())) {
                byId.put(product.getId(), product);
            }
        }
        List<Product> ordered = ids.stream().distinct().map(byId::get).filter(Objects::nonNull).toList();
        Map<Long, String> imageUrls = primaryImageUrls(ordered.stream().map(Product::getId).toList());
        Map<Long, EffectivePrice> prices = pricingService.resolveProducts(ordered, LocalDateTime.now(clock));
        Map<Long, Integer> available = stockAvailability.byProduct(ordered.stream().map(Product::getId).toList());
        return ordered.stream()
                .map(product -> productMapper.toResponse(product, imageUrls.get(product.getId()),
                        prices.get(product.getId()), available.getOrDefault(product.getId(), 0)))
                .toList();
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
        cache.evictAll(ReadCache.CATEGORY_BRANDS);

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
        return productMapper.toResponse(saved, primaryImageUrl, priceOf(saved), 0);
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
        evictProduct(id);
        filterService.reindex(id);
        log.info("Updated product id={}", id);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        product.setDeletedAt(LocalDateTime.now());
        product.setActive(false);
        evictProduct(id);
        cache.evict(ReadCache.VARIANTS, id);
        cache.evict(ReadCache.IMAGES, id);
        cache.evict(ReadCache.SPECS, id);
        log.info("Soft-deleted product id={}", id);
    }

    /** The product page data and the brand lists (category, brand or visibility may have changed). */
    private void evictProduct(Long id) {
        cache.evict(ReadCache.PRODUCT, id);
        cache.evictAll(ReadCache.CATEGORY_BRANDS);
    }

    private ProductResponse toResponse(Product product) {
        return productMapper.toResponse(product, primaryImageUrls(List.of(product.getId())).get(product.getId()),
                priceOf(product), stockAvailability.byProduct(List.of(product.getId())).getOrDefault(product.getId(), 0));
    }

    private EffectivePrice priceOf(Product product) {
        return pricingService.resolveProducts(List.of(product), LocalDateTime.now(clock)).get(product.getId());
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
