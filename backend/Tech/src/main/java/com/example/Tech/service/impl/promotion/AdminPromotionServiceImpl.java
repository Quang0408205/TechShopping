package com.example.Tech.service.impl.promotion;

import com.example.Tech.dto.request.promotion.PromotionProductSelection;
import com.example.Tech.dto.request.promotion.PromotionRequest;
import com.example.Tech.dto.request.promotion.PromotionSearchRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.promotion.PromotionProductResponse;
import com.example.Tech.dto.response.promotion.PromotionResponse;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.promotion.DiscountType;
import com.example.Tech.entity.promotion.Promotion;
import com.example.Tech.entity.promotion.PromotionProduct;
import com.example.Tech.entity.promotion.PromotionStatus;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.promotion.PromotionFilterSpecifications;
import com.example.Tech.repository.promotion.PromotionProductRepository;
import com.example.Tech.repository.promotion.PromotionRepository;
import com.example.Tech.service.promotion.AdminPromotionService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPromotionServiceImpl implements AdminPromotionService {

    private static final BigDecimal MAX_PERCENT = BigDecimal.valueOf(100);

    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final CurrentUserLoader currentUserLoader;
    private final Clock clock;

    @Override
    public PageResponse<PromotionResponse> search(Long adminId, PromotionSearchRequest filter, Pageable pageable) {
        ensureAdmin(adminId);
        if (filter.fromDate() != null && filter.toDate() != null && filter.fromDate().isAfter(filter.toDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Từ ngày không được sau đến ngày");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Page<Promotion> page = promotionRepository.findAll(PromotionFilterSpecifications.matching(filter, now), pageable);
        Map<Long, List<PromotionProductResponse>> products =
                productsByPromotion(page.getContent().stream().map(Promotion::getId).toList());
        return PageResponse.from(page.map(promotion ->
                toResponse(promotion, products.getOrDefault(promotion.getId(), List.of()), now)));
    }

    @Override
    public PromotionResponse getById(Long adminId, Long promotionId) {
        ensureAdmin(adminId);
        return toResponse(findWithCreatedBy(promotionId));
    }

    @Override
    @Transactional
    public PromotionResponse create(Long adminId, PromotionRequest request) {
        User admin = ensureAdmin(adminId);
        validate(request);
        Map<Long, Product> products = lockProducts(request.products());
        checkNoOverlap(request, products, -1L);

        Promotion promotion = new Promotion();
        promotion.setCreatedBy(admin);
        apply(promotion, request);
        Promotion saved = promotionRepository.save(promotion);
        for (PromotionProductSelection selection : request.products()) {
            promotionProductRepository.save(new PromotionProduct(saved, products.get(selection.productId()),
                    selection.discountType(), selection.discountValue()));
        }
        log.info("Admin id={} created promotion id={} with {} product(s)", adminId, saved.getId(), products.size());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PromotionResponse update(Long adminId, Long promotionId, PromotionRequest request) {
        ensureAdmin(adminId);
        Promotion promotion = findWithCreatedBy(promotionId);
        validate(request);
        Map<Long, Product> products = lockProducts(request.products());
        checkNoOverlap(request, products, promotionId);

        apply(promotion, request);

        Map<Long, PromotionProduct> current = promotionProductRepository
                .findAllWithProductByPromotionIdIn(List.of(promotionId)).stream()
                .collect(Collectors.toMap(row -> row.getId().getProductId(), Function.identity()));
        for (PromotionProductSelection selection : request.products()) {
            PromotionProduct row = current.remove(selection.productId());
            if (row == null) {
                promotionProductRepository.save(new PromotionProduct(promotion, products.get(selection.productId()),
                        selection.discountType(), selection.discountValue()));
            } else {
                row.setDiscountType(selection.discountType());
                row.setDiscountValue(selection.discountValue());
            }
        }
        promotionProductRepository.deleteAll(current.values());
        log.info("Admin id={} updated promotion id={} ({} product(s), {} removed)",
                adminId, promotionId, products.size(), current.size());
        return toResponse(promotion);
    }

    @Override
    @Transactional
    public void delete(Long adminId, Long promotionId) {
        ensureAdmin(adminId);
        Promotion promotion = promotionRepository.findById(promotionId).orElseThrow(() -> notFound(promotionId));
        // promotion_products rows go with it (ON DELETE CASCADE); placed orders keep their prices
        promotionRepository.delete(promotion);
        log.info("Admin id={} deleted promotion id={}", adminId, promotionId);
    }

    private User ensureAdmin(Long adminId) {
        return currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
    }

    private Promotion findWithCreatedBy(Long promotionId) {
        return promotionRepository.findWithCreatedById(promotionId).orElseThrow(() -> notFound(promotionId));
    }

    private static BusinessException notFound(Long promotionId) {
        return new BusinessException(ErrorCode.PROMOTION_NOT_FOUND,
                "Không tìm thấy chương trình khuyến mãi id %d".formatted(promotionId));
    }

    /** Rules that bean validation cannot express (cross-field and per-type limits). */
    private static void validate(PromotionRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new BusinessException(ErrorCode.INVALID_PROMOTION_DATE_RANGE);
        }
        checkDiscount(request.discountType(), request.discountValue());
        Set<Long> seen = new TreeSet<>();
        for (PromotionProductSelection selection : request.products()) {
            if (!seen.add(selection.productId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Sản phẩm id %d bị chọn trùng".formatted(selection.productId()));
            }
            if ((selection.discountType() == null) != (selection.discountValue() == null)) {
                throw new BusinessException(ErrorCode.INVALID_PROMOTION_DISCOUNT,
                        "Sản phẩm id %d: nhập cả kiểu giảm và mức giảm riêng, hoặc để trống cả hai"
                                .formatted(selection.productId()));
            }
            if (selection.discountType() != null) {
                checkDiscount(selection.discountType(), selection.discountValue());
            }
        }
    }

    private static void checkDiscount(DiscountType type, BigDecimal value) {
        if (type == DiscountType.PERCENTAGE && value.compareTo(MAX_PERCENT) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PROMOTION_DISCOUNT, "Giảm theo phần trăm tối đa 100%");
        }
    }

    /** Locks the selected products (see ProductRepository.findAllByIdInForUpdate); deleted ones are refused. */
    private Map<Long, Product> lockProducts(List<PromotionProductSelection> selections) {
        List<Long> ids = new ArrayList<>(new TreeSet<>(selections.stream().map(PromotionProductSelection::productId).toList()));
        Map<Long, Product> products = productRepository.findAllByIdInForUpdate(ids).stream()
                .filter(product -> product.getDeletedAt() == null)
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<Long> missing = ids.stream().filter(id -> !products.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm id %s".formatted(missing));
        }
        return products;
    }

    private static void apply(Promotion promotion, PromotionRequest request) {
        promotion.setName(request.name().trim());
        promotion.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        promotion.setDiscountType(request.discountType());
        promotion.setDiscountValue(request.discountValue());
        promotion.setMaxDiscountAmount(request.maxDiscountAmount());
        promotion.setStartDate(request.startDate());
        promotion.setEndDate(request.endDate());
        promotion.setActive(!Boolean.FALSE.equals(request.active()));
    }

    /** A paused promotion may overlap others; turning it on later runs this check again. */
    private void checkNoOverlap(PromotionRequest request, Map<Long, Product> products, Long excludePromotionId) {
        if (Boolean.FALSE.equals(request.active())) {
            return;
        }
        List<Long> conflicts = promotionProductRepository.findOverlappingProductIds(
                List.copyOf(products.keySet()), request.startDate(), request.endDate(), excludePromotionId);
        if (!conflicts.isEmpty()) {
            String names = conflicts.stream().sorted().map(id -> products.get(id).getName())
                    .collect(Collectors.joining(", "));
            throw new BusinessException(ErrorCode.PROMOTION_PRODUCT_OVERLAP,
                    "Đã thuộc chương trình khuyến mãi khác đang bật trùng thời gian: " + names);
        }
    }

    private PromotionResponse toResponse(Promotion promotion) {
        return toResponse(promotion, productsByPromotion(List.of(promotion.getId())).getOrDefault(promotion.getId(), List.of()),
                LocalDateTime.now(clock));
    }

    private static PromotionResponse toResponse(Promotion promotion, List<PromotionProductResponse> products,
                                                LocalDateTime now) {
        User creator = promotion.getCreatedBy();
        return new PromotionResponse(
                promotion.getId(),
                promotion.getName(),
                promotion.getDescription(),
                promotion.getDiscountType(),
                promotion.getDiscountValue(),
                promotion.getMaxDiscountAmount(),
                promotion.getStartDate(),
                promotion.getEndDate(),
                Boolean.TRUE.equals(promotion.getActive()),
                PromotionStatus.of(promotion, now),
                creator.getId(),
                creator.getFullname(),
                products,
                promotion.getCreatedAt(),
                promotion.getUpdatedAt());
    }

    /** Two queries for any number of promotions: their product rows, then one image per product. */
    private Map<Long, List<PromotionProductResponse>> productsByPromotion(List<Long> promotionIds) {
        if (promotionIds.isEmpty()) {
            return Map.of();
        }
        List<PromotionProduct> rows = promotionProductRepository.findAllWithProductByPromotionIdIn(promotionIds);
        Map<Long, String> images = new HashMap<>();
        if (!rows.isEmpty()) {
            Set<Long> productIds = rows.stream().map(row -> row.getId().getProductId()).collect(Collectors.toSet());
            for (ProductImage image : imageRepository.findAllByProductIdInBestFirst(productIds)) {
                images.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
            }
        }
        Map<Long, List<PromotionProductResponse>> result = new LinkedHashMap<>();
        for (PromotionProduct row : rows) {
            result.computeIfAbsent(row.getId().getPromotionId(), id -> new ArrayList<>())
                    .add(toProductResponse(row, images.get(row.getId().getProductId())));
        }
        return result;
    }

    private static PromotionProductResponse toProductResponse(PromotionProduct row, String imageUrl) {
        Product product = row.getProduct();
        Promotion promotion = row.getPromotion();
        boolean override = row.hasOverride();
        return new PromotionProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getBasePrice(),
                imageUrl,
                product.getDeletedAt() == null && !Boolean.FALSE.equals(product.getActive()),
                override ? row.getDiscountType() : promotion.getDiscountType(),
                override ? row.getDiscountValue() : promotion.getDiscountValue(),
                override);
    }
}
