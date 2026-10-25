package com.example.Tech.repository.product;

import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Product;
import com.example.Tech.util.SlugUtil;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
        return matching(filter, java.time.LocalDateTime.now());
    }

    public static Specification<Product> matching(ProductSearchRequest filter, java.time.LocalDateTime now) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                // plan v5 GĐ3: the brand name counts too ("samsung 256" finds a Galaxy whose name has no "Samsung")
                Join<Product, Brand> brand = root.join("brand", JoinType.LEFT);
                // Every word must match, in any order: "iphone 256gb" finds "iPhone 18 Pro 256GB"
                for (String word : WHITESPACE.split(filter.keyword().trim().toLowerCase(Locale.ROOT))) {
                    Predicate byName = cb.or(cb.like(cb.lower(root.get("name")), contains(word), LIKE_ESCAPE),
                            cb.like(cb.lower(brand.get("name")), contains(word), LIKE_ESCAPE));
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
            // plan v3: "on sale" (what the shop pages ask for) also needs a visible category; NULL counts as visible
            if (Boolean.TRUE.equals(filter.isActive())) {
                var categoryActive = root.get("category").<Boolean>get("active");
                predicates.add(cb.or(cb.isNull(categoryActive), cb.isTrue(categoryActive)));
            }
            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), filter.maxPrice()));
            }
            if (Boolean.TRUE.equals(filter.onSale())) {
                predicates.add(onSale(root, query, cb, now));
            }
            addSpecFilters(filter, root, query, cb, predicates);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Manual discount below the price (product or any variant) or a promotion running now that lists the product. */
    private static Predicate onSale(jakarta.persistence.criteria.Root<Product> root,
                                    jakarta.persistence.criteria.CriteriaQuery<?> query,
                                    jakarta.persistence.criteria.CriteriaBuilder cb, java.time.LocalDateTime now) {
        var promo = query.subquery(Long.class);
        var link = promo.from(com.example.Tech.entity.promotion.PromotionProduct.class);
        var campaign = link.join("promotion");
        promo.select(cb.literal(1L)).where(
                cb.equal(link.get("product").get("id"), root.get("id")),
                cb.isTrue(campaign.get("active")),
                cb.lessThanOrEqualTo(campaign.<java.time.LocalDateTime>get("startDate"), now),
                cb.greaterThan(campaign.<java.time.LocalDateTime>get("endDate"), now));

        var variant = query.subquery(Long.class);
        var v = variant.from(com.example.Tech.entity.product.ProductVariant.class);
        variant.select(cb.literal(1L)).where(
                cb.equal(v.get("product").get("id"), root.get("id")),
                cb.isNotNull(v.get("discountPrice")),
                cb.lessThan(v.<java.math.BigDecimal>get("discountPrice"), v.<java.math.BigDecimal>get("price")));

        return cb.or(
                cb.and(cb.isNotNull(root.get("discountPrice")),
                        cb.lessThan(root.<java.math.BigDecimal>get("discountPrice"), root.<java.math.BigDecimal>get("basePrice"))),
                cb.exists(variant),
                cb.exists(promo));
    }

    /** filter=group:value (same group = OR, groups = AND) and need=slug (any), each as an EXISTS on the index tables. */
    private static void addSpecFilters(ProductSearchRequest filter, jakarta.persistence.criteria.Root<Product> root,
                                       jakarta.persistence.criteria.CriteriaQuery<?> query,
                                       jakarta.persistence.criteria.CriteriaBuilder cb, List<Predicate> predicates) {
        java.util.Map<String, List<String>> groups = new java.util.LinkedHashMap<>();
        if (filter.filter() != null) {
            for (String item : filter.filter()) {
                int colon = item == null ? -1 : item.indexOf(':');
                if (colon > 0 && colon < item.length() - 1) {
                    groups.computeIfAbsent(item.substring(0, colon).trim(), key -> new ArrayList<>())
                            .add(item.substring(colon + 1).trim());
                }
            }
        }
        for (var entry : groups.entrySet()) {
            var sub = query.subquery(Long.class);
            var value = sub.from(com.example.Tech.entity.product.ProductFilterValue.class);
            sub.select(value.<Long>get("id")).where(
                    cb.equal(value.get("productId"), root.get("id")),
                    cb.equal(value.get("groupKey"), entry.getKey()),
                    value.get("label").in(entry.getValue()));
            predicates.add(cb.exists(sub));
        }
        if (filter.need() != null && !filter.need().isEmpty()) {
            var sub = query.subquery(Long.class);
            var link = sub.from(com.example.Tech.entity.product.ProductNeed.class);
            sub.select(link.<Long>get("id")).where(
                    cb.equal(link.get("productId"), root.get("id")),
                    link.get("need").get("slug").in(filter.need()));
            predicates.add(cb.exists(sub));
        }
    }

    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
