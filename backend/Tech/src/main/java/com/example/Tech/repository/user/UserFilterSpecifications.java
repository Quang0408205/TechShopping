package com.example.Tech.repository.user;

import com.example.Tech.dto.request.user.UserSearchRequest;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JPA criteria for the admin user list. Soft-deleted users are excluded unless includeDeleted is true.
 */
public final class UserFilterSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private UserFilterSpecifications() {
    }

    public static Specification<User> matching(UserSearchRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!Boolean.TRUE.equals(filter.includeDeleted())) {
                predicates.add(cb.isNull(root.get("deletedAt")));
            }
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = contains(filter.keyword().trim().toLowerCase(Locale.ROOT));
                predicates.add(cb.or(
                        cb.like(root.get("email"), pattern, LIKE_ESCAPE),
                        cb.like(root.get("username"), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("fullname")), pattern, LIKE_ESCAPE)));
            }
            if (filter.isActive() != null) {
                // NULL is_active counts as active, like User.isEnabled()
                predicates.add(filter.isActive()
                        ? cb.or(cb.isNull(root.get("active")), cb.isTrue(root.get("active")))
                        : cb.isFalse(root.get("active")));
            }
            if (filter.role() != null && !filter.role().isBlank()) {
                Subquery<Long> withRole = query.subquery(Long.class);
                Root<UserRole> userRole = withRole.from(UserRole.class);
                withRole.select(userRole.get("id").get("userId")).where(
                        cb.equal(userRole.get("id").get("userId"), root.get("id")),
                        cb.equal(userRole.get("role").get("name"), filter.role().trim().toUpperCase(Locale.ROOT)));
                predicates.add(cb.exists(withRole));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
