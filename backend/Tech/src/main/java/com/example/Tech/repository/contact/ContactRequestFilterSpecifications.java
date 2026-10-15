package com.example.Tech.repository.contact;

import com.example.Tech.dto.request.contact.ContactRequestSearchRequest;
import com.example.Tech.entity.contact.ContactRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JPA criteria for the staff contact-request list.
 */
public final class ContactRequestFilterSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private ContactRequestFilterSpecifications() {
    }

    public static Specification<ContactRequest> matching(ContactRequestSearchRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = contains(filter.keyword().trim().toLowerCase(Locale.ROOT));
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("message")), pattern, LIKE_ESCAPE)));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.topic() != null) {
                predicates.add(cb.equal(root.get("topic"), filter.topic()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
