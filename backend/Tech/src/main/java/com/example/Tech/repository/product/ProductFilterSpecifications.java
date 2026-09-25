package com.example.Tech.repository.product;

import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.entity.product.Product;
import com.example.Tech.util.SlugUtil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * JPA criteria for the product list filters. Soft-deleted products are always excluded.
 */
public final class ProductFilterSpecifications {

    private static final char LIKE_ESCAPE = '\\';
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private ProductFilterSpecifications() {
    }

    public static Specification<Product> matching(ProductSearchRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                // Every word must match, in any order: "iphone 256gb" finds "iPhone 18 Pro 256GB"
                for (String word : WHITESPACE.split(filter.keyword().trim().toLowerCase(Locale.ROOT))) {
                    Predicate byName = cb.like(cb.lower(root.get("name")), contains(word), LIKE_ESCAPE);
                    // Slugs have no diacritics, so "dien thoai" also finds "Điện thoại"
                    String slugWord = SlugUtil.toSlug(word);
                    predicates.add(slugWord.isEmpty()
                            ? byName
                            : cb.or(byName, cb.like(root.get("slug"), contains(slugWord), LIKE_ESCAPE)));
                }
            }
            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (filter.brandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), filter.brandId()));
            }
            if (filter.isActive() != null) {
                predicates.add(cb.equal(root.get("active"), filter.isActive()));
            }
            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), filter.maxPrice()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
