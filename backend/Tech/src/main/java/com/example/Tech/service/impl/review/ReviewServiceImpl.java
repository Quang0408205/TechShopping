package com.example.Tech.service.impl.review;

import com.example.Tech.dto.request.review.ReviewRequest;
import com.example.Tech.dto.request.review.ReviewSearchRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.review.ReviewLikeResponse;
import com.example.Tech.dto.response.review.ReviewResponse;
import com.example.Tech.dto.response.review.ReviewSummaryResponse;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.review.Review;
import com.example.Tech.entity.review.ReviewImage;
import com.example.Tech.entity.review.ReviewLike;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.order.OrderItemRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.review.ReviewImageRepository;
import com.example.Tech.repository.review.ReviewLikeRepository;
import com.example.Tech.repository.review.ReviewRepository;
import com.example.Tech.service.review.ReviewService;
import com.example.Tech.service.upload.ImageStorageService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    static final int COMMENT_MIN_LENGTH = 10;
    static final int COMMENT_MAX_LENGTH = 2000;
    static final int MAX_IMAGES = 5;

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final CurrentUserLoader currentUserLoader;
    private final ImageStorageService imageStorageService;
    private final ProductReviewStats productReviewStats;

    /** Smallest gap between two reviews of one account (0 = off, as in the test profile). */
    @Value("${app.review.min-interval-seconds:60}")
    private long minIntervalSeconds;

    @Override
    public PageResponse<ReviewResponse> listForProduct(Long productId, ReviewSearchRequest filter, Pageable pageable,
                                                       Long viewerId) {
        Product product = findProduct(productId);
        Short rating = filter.rating() == null ? null : filter.rating().shortValue();
        Page<Review> page = reviewRepository.findVisible(product.getId(), rating,
                Boolean.TRUE.equals(filter.withImages()),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
        Map<Long, List<String>> images = imageUrlsByReview(page.getContent());
        Set<Long> buyers = buyerIds(product.getId(), page.getContent());
        LikeState likes = likeState(page.getContent(), viewerId);
        return PageResponse.from(page.map(review -> toResponse(review, product,
                images.getOrDefault(review.getId(), List.of()), buyers.contains(review.getUser().getId()), false,
                likes)));
    }

    @Override
    public ReviewSummaryResponse summary(Long productId) {
        return productReviewStats.summary(findProduct(productId).getId());
    }

    @Override
    public List<ReviewResponse> mine(Long userId, Long productId) {
        currentUserLoader.load(userId);
        List<Review> reviews = reviewRepository.findMine(userId, productId);
        Map<Long, List<String>> images = imageUrlsByReview(reviews);
        LikeState likes = likeState(reviews, userId);
        return reviews.stream()
                .map(review -> toResponse(review, review.getProduct(), images.getOrDefault(review.getId(), List.of()),
                        isBuyer(review.getProduct().getId(), userId), true, likes))
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponse create(Long userId, ReviewRequest request) {
        User user = currentUserLoader.load(userId);
        if (request.productId() == null) {
            throw validation("productId", "Vui lòng chọn sản phẩm cần đánh giá");
        }
        Product product = findProduct(request.productId());
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE,
                    "Sản phẩm này đã ngừng bán nên không nhận đánh giá mới");
        }
        String comment = checkedComment(request.comment());
        List<String> imageUrls = checkedImageUrls(request.imageUrls());

        lock(product);
        // any number of reviews per account, but not two within minIntervalSeconds (anti flood)
        if (minIntervalSeconds > 0 && reviewRepository.existsByUser_IdAndCreatedAtAfter(userId,
                LocalDateTime.now().minusSeconds(minIntervalSeconds))) {
            throw new BusinessException(ErrorCode.REVIEW_TOO_SOON);
        }
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.rating().shortValue());
        review.setComment(comment);
        review.setVariantLabel(boughtVariantLabel(product.getId(), userId));
        reviewRepository.save(review);
        saveImages(review, imageUrls);
        reviewRepository.flush();
        productReviewStats.refresh(product);

        log.info("User id={} reviewed product id={} with {} stars ({} photos)",
                userId, product.getId(), request.rating(), imageUrls.size());
        return toResponse(review, product, imageUrls, isBuyer(product.getId(), userId), true,
                likeState(List.of(review), userId));
    }

    @Override
    @Transactional
    public ReviewResponse update(Long userId, Long reviewId, ReviewRequest request) {
        currentUserLoader.load(userId);
        Review review = findMine(userId, reviewId);
        String comment = checkedComment(request.comment());
        List<String> imageUrls = checkedImageUrls(request.imageUrls());
        Product product = review.getProduct();

        lock(product);
        List<ReviewImage> oldImages = reviewImageRepository.findAllByReview_IdOrderByDisplayOrderAsc(review.getId());
        review.setRating(request.rating().shortValue());
        review.setComment(comment);
        review.setUpdatedAt(LocalDateTime.now());
        review.getImages().clear();
        // Delete the old rows before inserting the new ones: unique(review_id, display_order)
        reviewImageRepository.deleteAll(oldImages);
        reviewImageRepository.flush();
        saveImages(review, imageUrls);
        reviewRepository.flush();
        productReviewStats.refresh(product);

        oldImages.stream().map(ReviewImage::getImageUrl).filter(url -> !imageUrls.contains(url))
                .forEach(imageStorageService::deleteAfterCommit);
        log.info("User id={} edited review id={}", userId, reviewId);
        return toResponse(review, product, imageUrls, isBuyer(product.getId(), userId), true,
                likeState(List.of(review), userId));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long reviewId) {
        currentUserLoader.load(userId);
        Review review = findMine(userId, reviewId);
        Product product = review.getProduct();

        lock(product);
        List<ReviewImage> images = reviewImageRepository.findAllByReview_IdOrderByDisplayOrderAsc(review.getId());
        review.getImages().clear();
        reviewImageRepository.deleteAll(images);
        reviewRepository.delete(review);
        reviewRepository.flush();
        productReviewStats.refresh(product);

        images.forEach(image -> imageStorageService.deleteAfterCommit(image.getImageUrl()));
        log.info("User id={} deleted review id={} of product id={}", userId, reviewId, product.getId());
    }

    @Override
    @Transactional
    public ReviewLikeResponse like(Long userId, Long reviewId) {
        currentUserLoader.load(userId);
        findVisible(reviewId);
        ReviewLike.Key key = new ReviewLike.Key(reviewId, userId);
        if (!reviewLikeRepository.existsById(key)) {
            reviewLikeRepository.saveAndFlush(new ReviewLike(reviewId, userId));
        }
        return new ReviewLikeResponse(reviewLikeRepository.countByReviewId(reviewId), true);
    }

    @Override
    @Transactional
    public ReviewLikeResponse unlike(Long userId, Long reviewId) {
        currentUserLoader.load(userId);
        findVisible(reviewId);
        ReviewLike.Key key = new ReviewLike.Key(reviewId, userId);
        if (reviewLikeRepository.existsById(key)) {
            reviewLikeRepository.deleteById(key);
            reviewLikeRepository.flush();
        }
        return new ReviewLikeResponse(reviewLikeRepository.countByReviewId(reviewId), false);
    }

    /** A hidden or missing review behaves as missing: nobody can like it. */
    private Review findVisible(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .filter(review -> !Boolean.TRUE.equals(review.getHidden()))
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
    }

    /** "Phân loại hàng" = storage, RAM, colour of the variant in the author's latest delivered order, else its name. */
    private String boughtVariantLabel(Long productId, Long userId) {
        List<ProductVariant> bought = orderItemRepository.findVariantsOfProductByUser(productId, userId,
                OrderStatus.DELIVERED, PageRequest.of(0, 1));
        if (bought.isEmpty()) {
            return null;
        }
        ProductVariant variant = bought.get(0);
        String parts = Stream.of(variant.getStorage(), variant.getRam(), variant.getColor())
                .filter(part -> part != null && !part.isBlank()).map(String::trim)
                .collect(Collectors.joining(", "));
        return parts.isEmpty() ? variant.getVariantName() : parts;
    }

    /** Like counts of these reviews and which of them the viewer (nullable) has liked, two queries. */
    private LikeState likeState(Collection<Review> reviews, Long viewerId) {
        if (reviews.isEmpty()) {
            return new LikeState(Map.of(), Set.of());
        }
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : reviewLikeRepository.countByReviewIds(ids)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        Set<Long> liked = viewerId == null ? Set.of() : new HashSet<>(reviewLikeRepository.findLikedReviewIds(viewerId, ids));
        return new LikeState(counts, liked);
    }

    private record LikeState(Map<Long, Long> counts, Set<Long> liked) {
    }

    private Product findProduct(Long productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm id %d".formatted(productId)));
    }

    private Review findMine(Long userId, Long reviewId) {
        return reviewRepository.findByIdAndUser_Id(reviewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
    }

    /** Serialises review writes of one product (rating recompute, one review per account). */
    private void lock(Product product) {
        productRepository.findAllByIdInForUpdate(List.of(product.getId()));
    }

    private static String checkedComment(String comment) {
        String trimmed = comment == null ? "" : comment.trim();
        if (trimmed.length() < COMMENT_MIN_LENGTH || trimmed.length() > COMMENT_MAX_LENGTH) {
            throw validation("comment", "Nội dung đánh giá từ %d đến %d ký tự"
                    .formatted(COMMENT_MIN_LENGTH, COMMENT_MAX_LENGTH));
        }
        return trimmed;
    }

    /** Only photos this server stored for reviews, no duplicates, at most 5 (also checked by @Size). */
    private List<String> checkedImageUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        Set<String> distinct = new LinkedHashSet<>(urls);
        if (distinct.size() != urls.size()) {
            throw validation("imageUrls", "Một ảnh chỉ được gửi một lần");
        }
        if (urls.size() > MAX_IMAGES) {
            throw validation("imageUrls", "Tối đa %d ảnh cho một đánh giá".formatted(MAX_IMAGES));
        }
        if (!urls.stream().allMatch(imageStorageService::isStoredReviewImage)) {
            throw validation("imageUrls", "Chỉ dùng ảnh tải lên từ trang đánh giá (JPG, PNG, WebP)");
        }
        return List.copyOf(urls);
    }

    private void saveImages(Review review, List<String> imageUrls) {
        List<ReviewImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            ReviewImage image = new ReviewImage(review, imageUrls.get(i), i + 1);
            review.getImages().add(image);
            images.add(image);
        }
        reviewImageRepository.saveAll(images);
    }

    private boolean isBuyer(Long productId, Long userId) {
        return !orderItemRepository.findBuyerIds(productId, List.of(userId), OrderStatus.DELIVERED).isEmpty();
    }

    /** Which authors of these reviews have a DELIVERED order containing the product, in one query. */
    private Set<Long> buyerIds(Long productId, List<Review> reviews) {
        if (reviews.isEmpty()) {
            return Set.of();
        }
        Set<Long> userIds = reviews.stream().map(review -> review.getUser().getId()).collect(Collectors.toSet());
        return new HashSet<>(orderItemRepository.findBuyerIds(productId, userIds, OrderStatus.DELIVERED));
    }

    /** reviewId → photo URLs in display order, for several reviews in one query. */
    private Map<Long, List<String>> imageUrlsByReview(Collection<Review> reviews) {
        if (reviews.isEmpty()) {
            return Map.of();
        }
        return reviewImageRepository.findAllByReview_IdInOrderByReview_IdAscDisplayOrderAsc(
                        reviews.stream().map(Review::getId).toList()).stream()
                .collect(Collectors.groupingBy(image -> image.getReview().getId(),
                        Collectors.mapping(ReviewImage::getImageUrl, Collectors.toList())));
    }

    /** withHiddenReason: only for the author (GET /reviews/mine, their own writes); public lists never show it. */
    private static ReviewResponse toResponse(Review review, Product product, List<String> imageUrls,
                                             boolean verifiedPurchase, boolean withHiddenReason, LikeState likes) {
        User author = review.getUser();
        String authorName = author.getFullname() == null || author.getFullname().isBlank()
                ? author.getUsername() : author.getFullname();
        return new ReviewResponse(
                review.getId(),
                product.getId(),
                product.getName(),
                authorName,
                review.getRating(),
                review.getComment(),
                imageUrls,
                verifiedPurchase,
                review.getUpdatedAt() != null,
                review.getCreatedAt(),
                review.getUpdatedAt(),
                Boolean.TRUE.equals(review.getHidden()),
                withHiddenReason ? review.getHiddenReason() : null,
                review.getVariantLabel(),
                likes.counts().getOrDefault(review.getId(), 0L),
                likes.liked().contains(review.getId()));
    }

    private static BusinessException validation(String field, String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message, Map.of(field, message));
    }
}
