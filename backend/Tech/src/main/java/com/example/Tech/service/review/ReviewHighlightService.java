package com.example.Tech.service.review;

import com.example.Tech.dto.response.review.ReviewHighlightResponse;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.review.Review;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.order.OrderItemRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Plan v6 B5, public: real customer reviews for the home page ("Khách hàng nói gì"). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewHighlightService {

    static final short MIN_RATING = 4;

    private final ReviewRepository reviewRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderItemRepository orderItemRepository;

    /** The newest visible reviews of 4-5 stars of products on sale; empty list when there is none. */
    public List<ReviewHighlightResponse> latest(int size) {
        List<Review> reviews = reviewRepository.findHighlights(MIN_RATING, PageRequest.of(0, size));
        if (reviews.isEmpty()) {
            return List.of();
        }
        Map<Long, String> images = new HashMap<>();
        for (ProductImage image : productImageRepository.findAllByProductIdInBestFirst(
                reviews.stream().map(review -> review.getProduct().getId()).distinct().toList())) {
            images.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
        }
        return reviews.stream().map(review -> {
            User author = review.getUser();
            Long productId = review.getProduct().getId();
            boolean buyer = !orderItemRepository.findBuyerIds(productId, List.of(author.getId()), OrderStatus.DELIVERED).isEmpty();
            String name = author.getFullname() == null || author.getFullname().isBlank() ? author.getUsername() : author.getFullname();
            return new ReviewHighlightResponse(review.getId(), productId, review.getProduct().getName(), images.get(productId),
                    name, review.getRating(), review.getComment(), review.getVariantLabel(), buyer, review.getCreatedAt());
        }).toList();
    }
}
