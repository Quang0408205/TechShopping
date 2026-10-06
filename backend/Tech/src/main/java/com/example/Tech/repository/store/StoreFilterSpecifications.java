package com.example.Tech.repository.store;

import com.example.Tech.dto.request.store.StoreSearchRequest;
import com.example.Tech.entity.store.Store;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JPA criteria for the admin store list.
 */
public final class StoreFilterSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private StoreFilterSpecifications() {
    }

    public static Specification<Store> matching(StoreSearchRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = contains(filter.keyword().trim().toLowerCase(Locale.ROOT));
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("address")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(cb.coalesce(root.get("district"), "")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(cb.coalesce(root.get("city"), "")), pattern, LIKE_ESCAPE),
                        cb.like(cb.coalesce(root.get("phone"), ""), pattern, LIKE_ESCAPE)));
            }
            if (filter.active() != null) {
                // is_active null counts as open, like the rest of the application
                predicates.add(filter.active()
                        ? cb.or(cb.isNull(root.get("active")), cb.isTrue(root.get("active")))
                        : cb.isFalse(root.get("active")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
