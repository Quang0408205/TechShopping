package com.example.Tech.service.product.sales;

import com.example.Tech.dto.response.product.ProductSalesResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plan v8: "Đã bán N" and the "Bán chạy" label on the product cards, from real orders (status not CANCELLED).
 * The top-seller list is computed for the whole shop and kept for a few minutes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSalesService {

    public static final int MAX_IDS = 60;
    static final int TOP_SELLERS = 20;
    static final long MIN_SOLD_FOR_BEST_SELLER = 2;
    private static final Duration TOP_TTL = Duration.ofMinutes(5);

    private final EntityManager entityManager;
    private final Clock clock;

    private volatile Set<Long> topSellers;
    private volatile Instant topSellersAt = Instant.EPOCH;

    public List<ProductSalesResponse> sales(List<Long> ids) {
        List<Long> wanted = ids.stream().distinct().limit(MAX_IDS).toList();
        if (wanted.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> sold = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select v.product_id, sum(oi.quantity)
                from order_items oi
                join orders o on o.order_id = oi.order_id
                join product_variants v on v.variant_id = oi.variant_id
                where upper(o.status) <> 'CANCELLED' and v.product_id in (:ids)
                group by v.product_id""")
                .setParameter("ids", wanted)
                .getResultList();
        for (Object[] row : rows) {
            sold.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        Set<Long> top = topSellers();
        return wanted.stream()
                .map(id -> new ProductSalesResponse(id, sold.getOrDefault(id, 0L), top.contains(id)))
                .toList();
    }

    private Set<Long> topSellers() {
        Instant now = clock.instant();
        if (topSellers == null || now.isAfter(topSellersAt.plus(TOP_TTL))) {
            @SuppressWarnings("unchecked")
            List<Object> rows = entityManager.createNativeQuery("""
                    select v.product_id
                    from order_items oi
                    join orders o on o.order_id = oi.order_id
                    join product_variants v on v.variant_id = oi.variant_id
                    where upper(o.status) <> 'CANCELLED'
                    group by v.product_id
                    having sum(oi.quantity) >= :min
                    order by sum(oi.quantity) desc, v.product_id
                    limit :top""")
                    .setParameter("min", MIN_SOLD_FOR_BEST_SELLER)
                    .setParameter("top", TOP_SELLERS)
                    .getResultList();
            Set<Long> ids = new HashSet<>();
            rows.forEach(id -> ids.add(((Number) id).longValue()));
            topSellers = ids;
            topSellersAt = now;
        }
        return topSellers;
    }
}
