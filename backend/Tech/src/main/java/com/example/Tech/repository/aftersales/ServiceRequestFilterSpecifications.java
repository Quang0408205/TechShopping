package com.example.Tech.repository.aftersales;

import com.example.Tech.entity.aftersales.ServiceRequestType;
import com.example.Tech.entity.aftersales.ServiceRequestView;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.user.User;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServiceRequestFilterSpecifications {

    private static final char LIKE_ESCAPE = '\\';
    private static final Pattern REQUEST_CODE = Pattern.compile("^(BH|BT|DT)0*(\\d{1,18})$");
    private static final Pattern ORDER_CODE = Pattern.compile("^DH0*(\\d{1,18})$");
    private static final Pattern DIGITS = Pattern.compile("^\\d{1,18}$");

    private ServiceRequestFilterSpecifications() {
    }

    public static Specification<ServiceRequestView> ofUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<ServiceRequestView> ofType(ServiceRequestType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("requestType"), type);
    }

    public static Specification<ServiceRequestView> withStatus(String status) {
        return (root, query, cb) -> status == null || status.isBlank() ? null
                : cb.equal(root.get("status"), status.trim().toUpperCase(Locale.ROOT));
    }

    public static Specification<ServiceRequestView> ofStore(Integer storeId) {
        return (root, query, cb) -> storeId == null ? null : cb.equal(root.get("storeId"), storeId);
    }

    public static Specification<ServiceRequestView> createdBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from == null) {
                return cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
            }
            if (to == null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
            }
            return cb.and(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()),
                    cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
        };
    }

    /** BH / BT / DT request code, DH order code, else customer name / email / username / phone (digits: order id too). */
    public static Specification<ServiceRequestView> matchingKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String text = keyword.trim().toUpperCase(Locale.ROOT);
            Matcher requestCode = REQUEST_CODE.matcher(text);
            if (requestCode.matches()) {
                ServiceRequestType type = switch (requestCode.group(1)) {
                    case "BH" -> ServiceRequestType.WARRANTY;
                    case "BT" -> ServiceRequestType.MAINTENANCE;
                    default -> ServiceRequestType.RETURN;
                };
                return cb.and(cb.equal(root.get("requestType"), type),
                        cb.equal(root.get("requestId"), Long.parseLong(requestCode.group(2))));
            }
            Matcher orderCode = ORDER_CODE.matcher(text);
            if (orderCode.matches()) {
                return cb.equal(root.get("orderId"), Long.parseLong(orderCode.group(1)));
            }

            String pattern = contains(keyword.trim().toLowerCase(Locale.ROOT));
            Subquery<Long> users = query.subquery(Long.class);
            Root<User> user = users.from(User.class);
            users.select(user.get("id")).where(cb.or(
                    cb.like(cb.lower(cb.coalesce(user.get("fullname"), "")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(user.get("email")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(user.get("username")), pattern, LIKE_ESCAPE),
                    cb.like(cb.coalesce(user.get("phone"), ""), pattern, LIKE_ESCAPE)));
            Subquery<Long> orders = query.subquery(Long.class);
            Root<Order> order = orders.from(Order.class);
            orders.select(order.get("id")).where(cb.or(
                    cb.like(cb.lower(order.get("recipientName")), pattern, LIKE_ESCAPE),
                    cb.like(order.get("recipientPhone"), pattern, LIKE_ESCAPE)));
            if (DIGITS.matcher(text).matches()) {
                return cb.or(root.get("userId").in(users), root.get("orderId").in(orders),
                        cb.equal(root.get("orderId"), Long.parseLong(text)));
            }
            return cb.or(root.get("userId").in(users), root.get("orderId").in(orders));
        };
    }

    private static String contains(String text) {
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
