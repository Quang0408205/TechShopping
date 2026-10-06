package com.example.Tech.repository.inventory;

import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JPA criteria for one store's inventory list.
 */
public final class InventoryFilterSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private InventoryFilterSpecifications() {
    }

    /** keyword: part of the product name, variant name or variant SKU; outOfStock: only rows at 0. */
    public static Specification<Inventory> matching(Integer storeId, String keyword, Boolean outOfStock) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("id").get("storeId"), storeId));
            if (keyword != null && !keyword.isBlank()) {
                String pattern = contains(keyword.trim().toLowerCase(Locale.ROOT));
                Join<Inventory, ProductVariant> variant = root.join("variant");
                Join<ProductVariant, Product> product = variant.join("product");
                predicates.add(cb.or(
                        cb.like(cb.lower(product.get("name")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(variant.get("variantName")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(cb.coalesce(variant.get("skuVariant"), "")), pattern, LIKE_ESCAPE)));
            }
            if (Boolean.TRUE.equals(outOfStock)) {
                predicates.add(cb.equal(root.get("quantity"), 0));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
