package com.example.Tech.repository.order;

import com.example.Tech.dto.request.order.AdminOrderSearchRequest;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JPA criteria for the staff order list.
 */
public final class OrderFilterSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    /** "DH00000042", "dh42" or "42": also matches the order id. */
    private static final Pattern ORDER_CODE = Pattern.compile("^(?i:DH)?0*(\\d{1,18})$");

    private OrderFilterSpecifications() {
    }

    public static Specification<Order> matching(AdminOrderSearchRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String keyword = filter.keyword().trim();
                String pattern = contains(keyword.toLowerCase(Locale.ROOT));
                Join<Order, User> user = root.join("user");
                List<Predicate> anyOf = new ArrayList<>(List.of(
                        cb.like(cb.lower(root.get("recipientName")), pattern, LIKE_ESCAPE),
                        cb.like(root.get("recipientPhone"), pattern, LIKE_ESCAPE),
                        cb.like(user.get("email"), pattern, LIKE_ESCAPE),
                        cb.like(user.get("username"), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(user.get("fullname")), pattern, LIKE_ESCAPE)));
                Matcher code = ORDER_CODE.matcher(keyword);
                if (code.matches()) {
                    anyOf.add(cb.equal(root.get("id"), Long.valueOf(code.group(1))));
                }
                predicates.add(cb.or(anyOf.toArray(Predicate[]::new)));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), filter.fromDate().atStartOfDay()));
            }
            if (filter.toDate() != null) {
                predicates.add(cb.lessThan(root.get("orderDate"), filter.toDate().plusDays(1).atStartOfDay()));
            }
            if (filter.storeId() != null) {
                predicates.add(cb.equal(root.get("store").get("id"), filter.storeId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Purchase history: the customer's own orders, optionally limited to some statuses and to a keyword
     * (order code / id, or a product name in the order).
     */
    public static Specification<Order> history(Long userId, Collection<OrderStatus> statuses, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (keyword != null && !keyword.isBlank()) {
                String text = keyword.trim();
                Subquery<Long> withProduct = query.subquery(Long.class);
                Root<OrderItem> item = withProduct.from(OrderItem.class);
                withProduct.select(item.get("id")).where(
                        cb.equal(item.get("order"), root),
                        cb.like(cb.lower(item.get("variant").get("product").get("name")),
                                contains(text.toLowerCase(Locale.ROOT)), LIKE_ESCAPE));
                List<Predicate> anyOf = new ArrayList<>(List.of(cb.exists(withProduct)));
                Matcher code = ORDER_CODE.matcher(text);
                if (code.matches()) {
                    anyOf.add(cb.equal(root.get("id"), Long.valueOf(code.group(1))));
                }
                predicates.add(cb.or(anyOf.toArray(Predicate[]::new)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
