package com.example.Tech.service.product.sales;

import com.example.Tech.dto.response.product.TopBrandResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The brands with the most products on sale, for the home page strip. Some crawled products got a product
 * type as their "brand" (Túi, Đèn, Ốp…); those words are left out so the strip only shows real brands.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopBrandService {

    static final Set<String> NOT_BRANDS = Set.of("adapter", "bàn", "balo", "bút", "cáp", "camera", "chuột", "dây", "đèn",
            "đế", "giá", "gậy", "hub", "kính", "loa", "máy", "miếng", "ốp", "pin", "quạt", "sạc", "tai", "thẻ", "túi",
            "usb", "vỏ", "bao", "bộ", "phụ kiện");

    private final EntityManager entityManager;

    public List<TopBrandResponse> top(int size) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select b.brand_id, b.name, count(*)
                from products p
                join brands b on b.brand_id = p.brand_id
                where coalesce(p.is_active, true) and p.deleted_at is null and coalesce(b.is_active, true)
                group by b.brand_id, b.name
                order by count(*) desc, b.name""")
                .setMaxResults(size + NOT_BRANDS.size())
                .getResultList();
        return rows.stream()
                .map(row -> new TopBrandResponse(((Number) row[0]).intValue(), (String) row[1], ((Number) row[2]).longValue()))
                .filter(brand -> !NOT_BRANDS.contains(brand.name().trim().toLowerCase(Locale.ROOT)))
                .limit(size)
                .toList();
    }
}
