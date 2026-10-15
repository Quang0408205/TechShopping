package com.example.Tech.repository.promotion;

import com.example.Tech.dto.request.promotion.PromotionSearchRequest;
import com.example.Tech.entity.promotion.Promotion;
import com.example.Tech.entity.promotion.PromotionStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JPA criteria for the admin promotion list. The status filter uses the same rules as PromotionStatus.of.
 */
public final class PromotionFilterSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private PromotionFilterSpecifications() {
    }

    public static Specification<Promotion> matching(PromotionSearchRequest filter, LocalDateTime now) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = contains(filter.keyword().trim().toLowerCase(Locale.ROOT));
                predicates.add(cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE));
            }
            if (filter.status() != null) {
                predicates.add(status(root, cb, filter.status(), now));
            }
            if (filter.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), filter.fromDate().atStartOfDay()));
            }
            if (filter.toDate() != null) {
                predicates.add(cb.lessThan(root.get("startDate"), filter.toDate().plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate status(Root<Promotion> root, CriteriaBuilder cb, PromotionStatus status,
                                    LocalDateTime now) {
        Path<LocalDateTime> start = root.get("startDate");
        Path<LocalDateTime> end = root.get("endDate");
        Path<Boolean> active = root.get("active");
        Predicate notEnded = cb.greaterThan(end, now);
        return switch (status) {
            case ENDED -> cb.lessThanOrEqualTo(end, now);
            case PAUSED -> cb.and(notEnded, cb.isFalse(active));
            case UPCOMING -> cb.and(cb.isTrue(active), cb.greaterThan(start, now));
            case RUNNING -> cb.and(notEnded, cb.isTrue(active), cb.lessThanOrEqualTo(start, now));
        };
    }

    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
