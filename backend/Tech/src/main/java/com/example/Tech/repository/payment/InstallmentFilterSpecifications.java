package com.example.Tech.repository.payment;

import com.example.Tech.dto.request.payment.AdminInstallmentSearchRequest;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.payment.InstallmentOrder;
import com.example.Tech.entity.payment.InstallmentPayment;
import com.example.Tech.entity.payment.InstallmentPaymentStatus;
import com.example.Tech.entity.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** JPA criteria for the staff installment list. */
public final class InstallmentFilterSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    /** "DH00000042", "dh42" or "42": also matches the order id. */
    private static final Pattern ORDER_CODE = Pattern.compile("^(?i:DH)?0*(\\d{1,18})$");

    private InstallmentFilterSpecifications() {
    }

    /** {@code today}: a period is overdue when it is unpaid and its due date is before today. */
    public static Specification<InstallmentOrder> matching(AdminInstallmentSearchRequest filter, LocalDate today) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String keyword = filter.keyword().trim();
                String pattern = contains(keyword.toLowerCase(Locale.ROOT));
                Join<InstallmentOrder, Order> order = root.join("order");
                Join<Order, User> user = order.join("user");
                List<Predicate> anyOf = new ArrayList<>(List.of(
                        cb.like(cb.lower(order.get("recipientName")), pattern, LIKE_ESCAPE),
                        cb.like(order.get("recipientPhone"), pattern, LIKE_ESCAPE),
                        cb.like(user.get("email"), pattern, LIKE_ESCAPE),
                        cb.like(user.get("username"), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(user.get("fullname")), pattern, LIKE_ESCAPE),
                        cb.like(root.get("citizenId"), pattern, LIKE_ESCAPE)));
                Matcher code = ORDER_CODE.matcher(keyword);
                if (code.matches()) {
                    anyOf.add(cb.equal(order.get("id"), Long.valueOf(code.group(1))));
                }
                predicates.add(cb.or(anyOf.toArray(Predicate[]::new)));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (Boolean.TRUE.equals(filter.overdue())) {
                Subquery<Long> overdue = query.subquery(Long.class);
                Root<InstallmentPayment> period = overdue.from(InstallmentPayment.class);
                overdue.select(period.get("id")).where(
                        cb.equal(period.get("installment"), root),
                        cb.equal(period.get("status"), InstallmentPaymentStatus.PENDING),
                        cb.lessThan(period.get("dueDate"), today));
                predicates.add(cb.exists(overdue));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
