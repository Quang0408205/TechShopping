package com.example.Tech.service.impl.review;

import com.example.Tech.dto.request.review.AdminReviewSearchRequest;
import com.example.Tech.dto.request.review.ReviewVisibilityRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.review.AdminReviewResponse;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.review.Review;
import com.example.Tech.entity.review.ReviewImage;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.order.OrderItemRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.review.ReviewFilterSpecifications;
import com.example.Tech.repository.review.ReviewImageRepository;
import com.example.Tech.repository.review.ReviewRepository;
import com.example.Tech.service.review.AdminReviewService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReviewServiceImpl implements AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final CurrentUserLoader currentUserLoader;
    private final ProductReviewStats productReviewStats;

    @Override
    public PageResponse<AdminReviewResponse> search(Long adminId, AdminReviewSearchRequest filter, Pageable pageable) {
        currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
        Page<Review> page = reviewRepository.findAll(ReviewFilterSpecifications.matching(filter), pageable);
        Map<Long, List<String>> images = imageUrlsByReview(page.getContent());
        Set<String> buyers = buyerPairs(page.getContent());
        return PageResponse.from(page.map(review -> toResponse(review,
                images.getOrDefault(review.getId(), List.of()), buyers.contains(pairKey(review)))));
    }

    @Override
    @Transactional
    public AdminReviewResponse setVisibility(Long adminId, Long reviewId, ReviewVisibilityRequest request) {
        User admin = currentUserLoader.loadWithAnyRole(adminId, RoleName.ADMIN);
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (request.hidden() && reason.isEmpty()) {
            String message = "Vui lòng nhập lý do ẩn đánh giá (người viết sẽ thấy lý do này)";
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message, Map.of("reason", message));
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        Product product = review.getProduct();

        // Same lock as the customer writes: the rating recompute never interleaves with another one
        productRepository.findAllByIdInForUpdate(List.of(product.getId()));
        if (request.hidden()) {
            review.setHidden(true);
            review.setHiddenReason(reason);
            review.setHiddenBy(admin);
            review.setHiddenAt(LocalDateTime.now());
        } else {
            review.setHidden(false);
            review.setHiddenReason(null);
            review.setHiddenBy(null);
            review.setHiddenAt(null);
        }
        reviewRepository.flush();
        productReviewStats.refresh(product);

        log.info("Admin id={} {} review id={} of product id={}", adminId, request.hidden() ? "hid" : "showed",
                reviewId, product.getId());
        List<Review> one = List.of(review);
        return toResponse(review, imageUrlsByReview(one).getOrDefault(review.getId(), List.of()),
                buyerPairs(one).contains(pairKey(review)));
    }

    private Map<Long, List<String>> imageUrlsByReview(Collection<Review> reviews) {
        if (reviews.isEmpty()) {
            return Map.of();
        }
        return reviewImageRepository.findAllByReview_IdInOrderByReview_IdAscDisplayOrderAsc(
                        reviews.stream().map(Review::getId).toList()).stream()
                .collect(Collectors.groupingBy(image -> image.getReview().getId(),
                        Collectors.mapping(ReviewImage::getImageUrl, Collectors.toList())));
    }

    /** "userId:productId" of the authors who received the reviewed product (one query for the page). */
    private Set<String> buyerPairs(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return Set.of();
        }
        Set<Long> userIds = reviews.stream().map(review -> review.getUser().getId()).collect(Collectors.toSet());
        Set<Long> productIds = reviews.stream().map(review -> review.getProduct().getId()).collect(Collectors.toSet());
        Set<String> pairs = new HashSet<>();
        for (Object[] row : orderItemRepository.findBuyerProductPairs(userIds, productIds, OrderStatus.DELIVERED)) {
            pairs.add(row[0] + ":" + row[1]);
        }
        return pairs;
    }

    private static String pairKey(Review review) {
        return review.getUser().getId() + ":" + review.getProduct().getId();
    }

    private static AdminReviewResponse toResponse(Review review, List<String> imageUrls, boolean verifiedPurchase) {
        User author = review.getUser();
        User hiddenBy = review.getHiddenBy();
        String authorName = author.getFullname() == null || author.getFullname().isBlank()
                ? author.getUsername() : author.getFullname();
        return new AdminReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getProduct().getName(),
                author.getId(),
                authorName,
                author.getUsername(),
                author.getEmail(),
                review.getRating(),
                review.getComment(),
                imageUrls,
                verifiedPurchase,
                review.getUpdatedAt() != null,
                review.getCreatedAt(),
                review.getUpdatedAt(),
                Boolean.TRUE.equals(review.getHidden()),
                review.getHiddenReason(),
                hiddenBy == null ? null : hiddenBy.getFullname(),
                review.getHiddenAt());
    }
}
