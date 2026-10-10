package com.example.Tech.repository.employee;

import com.example.Tech.dto.request.employee.EmployeeSearchRequest;
import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JPA criteria for the admin employee list. The store filter matches the employee's current (open, active)
 * assignment only.
 */
public final class EmployeeFilterSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private EmployeeFilterSpecifications() {
    }

    public static Specification<Employee> matching(EmployeeSearchRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = contains(filter.keyword().trim().toLowerCase(Locale.ROOT));
                Join<Employee, User> user = root.join("user");
                predicates.add(cb.or(
                        cb.like(cb.lower(user.get("fullname")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(user.get("email")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(user.get("username")), pattern, LIKE_ESCAPE),
                        cb.like(cb.coalesce(user.get("phone"), ""), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(cb.coalesce(root.get("employeeCode"), "")), pattern, LIKE_ESCAPE)));
            }
            if (filter.storeId() != null) {
                Subquery<Long> current = query.subquery(Long.class);
                Root<EmployeeAssignment> assignment = current.from(EmployeeAssignment.class);
                current.select(assignment.get("id")).where(
                        cb.equal(assignment.get("employee"), root),
                        cb.equal(assignment.get("store").get("id"), filter.storeId()),
                        cb.isTrue(assignment.get("active")),
                        cb.isNull(assignment.get("endDate")));
                predicates.add(cb.exists(current));
            }
            if (filter.active() != null) {
                predicates.add(filter.active()
                        ? cb.or(cb.isNull(root.get("active")), cb.isTrue(root.get("active")))
                        : cb.isFalse(root.get("active")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Employees whose most recent assignment is at the store: still working there (open assignment) or gone
     * (profile marked as left). Someone moved to another store, or unassigned but still active, is not included.
     */
    public static Specification<Employee> latestAssignmentAt(Integer storeId) {
        return (root, query, cb) -> {
            Subquery<Long> latest = query.subquery(Long.class);
            Root<EmployeeAssignment> other = latest.from(EmployeeAssignment.class);
            latest.select(cb.max(other.<Long>get("id"))).where(cb.equal(other.get("employee"), root));

            Subquery<Long> found = query.subquery(Long.class);
            Root<EmployeeAssignment> assignment = found.from(EmployeeAssignment.class);
            found.select(assignment.<Long>get("id")).where(
                    cb.equal(assignment.get("employee"), root),
                    cb.equal(assignment.get("store").get("id"), storeId),
                    cb.equal(assignment.<Long>get("id"), latest),
                    cb.or(cb.and(cb.isTrue(assignment.get("active")), cb.isNull(assignment.get("endDate"))),
                            cb.isFalse(root.get("active"))));
            return cb.exists(found);
        };
    }

    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
