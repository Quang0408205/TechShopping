package com.example.Tech.repository.review;

import com.example.Tech.dto.request.review.AdminReviewSearchRequest;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.review.Review;
import com.example.Tech.entity.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JPA criteria for the admin review list.
 */
public final class ReviewFilterSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private ReviewFilterSpecifications() {
    }

    public static Specification<Review> matching(AdminReviewSearchRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = contains(filter.keyword().trim().toLowerCase(Locale.ROOT));
                Join<Review, Product> product = root.join("product");
                Join<Review, User> user = root.join("user");
                predicates.add(cb.or(
                        cb.like(cb.lower(product.get("name")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(cb.coalesce(user.get("fullname"), "")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(user.get("username")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(user.get("email")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("comment")), pattern, LIKE_ESCAPE)));
            }
            if (filter.rating() != null) {
                predicates.add(cb.equal(root.get("rating"), filter.rating().shortValue()));
            }
            if (filter.hidden() != null) {
                predicates.add(cb.equal(root.get("hidden"), filter.hidden()));
            }
            if (filter.productId() != null) {
                predicates.add(cb.equal(root.get("product").get("id"), filter.productId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
