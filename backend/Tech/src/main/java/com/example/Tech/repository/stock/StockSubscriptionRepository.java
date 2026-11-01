package com.example.Tech.repository.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * "Tell me when it is back" subscriptions. A variant is in stock when the stores that are open hold at least one unit
 * of it (the same rule the shop applies when it sells).
 */
@Repository
@RequiredArgsConstructor
public class StockSubscriptionRepository {

    private static final String VARIANT_INFO = "select v.variant_id, v.variant_name, p.product_id, p.name product_name, "
            + "(p.deleted_at is null and p.is_active) on_sale, "
            + "coalesce((select sum(i.quantity) from inventory i join stores s on s.store_id = i.store_id "
            + "where i.variant_id = v.variant_id and s.is_active), 0) stock "
            + "from product_variants v join products p on p.product_id = v.product_id ";

    private final NamedParameterJdbcTemplate jdbc;

    public record VariantInfo(Long variantId, String variantName, Long productId, String productName, boolean onSale, long stock) {
    }

    public record Subscription(Long id, Long userId, VariantInfo variant) {
    }

    public VariantInfo findVariant(Long variantId) {
        List<VariantInfo> rows = jdbc.query(VARIANT_INFO + "where v.variant_id = :id", new MapSqlParameterSource("id", variantId),
                (rs, i) -> new VariantInfo(rs.getLong("variant_id"), rs.getString("variant_name"), rs.getLong("product_id"),
                        rs.getString("product_name"), rs.getBoolean("on_sale"), rs.getLong("stock")));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<Long> variantIdsOf(Long userId) {
        return jdbc.queryForList("select variant_id from stock_subscriptions where user_id = :userId order by created_at desc",
                new MapSqlParameterSource("userId", userId), Long.class);
    }

    public int countOf(Long userId) {
        Integer total = jdbc.queryForObject("select count(*) from stock_subscriptions where user_id = :userId",
                new MapSqlParameterSource("userId", userId), Integer.class);
        return total == null ? 0 : total;
    }

    public void insert(Long userId, Long variantId) {
        jdbc.update("insert into stock_subscriptions (user_id, variant_id) values (:userId, :variantId) on conflict do nothing",
                new MapSqlParameterSource("userId", userId).addValue("variantId", variantId));
    }

    public void delete(Long userId, Long variantId) {
        jdbc.update("delete from stock_subscriptions where user_id = :userId and variant_id = :variantId",
                new MapSqlParameterSource("userId", userId).addValue("variantId", variantId));
    }

    public void deleteAll(List<Long> ids) {
        if (!ids.isEmpty()) {
            jdbc.update("delete from stock_subscriptions where subscription_id in (:ids)", new MapSqlParameterSource("ids", ids));
        }
    }

    public List<Subscription> findAll() {
        return jdbc.query("select sub.subscription_id, sub.user_id, v.variant_id, v.variant_name, p.product_id, p.name product_name, "
                        + "(p.deleted_at is null and p.is_active) on_sale, "
                        + "coalesce((select sum(i.quantity) from inventory i join stores s on s.store_id = i.store_id "
                        + "where i.variant_id = v.variant_id and s.is_active), 0) stock "
                        + "from stock_subscriptions sub join product_variants v on v.variant_id = sub.variant_id "
                        + "join products p on p.product_id = v.product_id order by sub.subscription_id",
                (rs, i) -> new Subscription(rs.getLong("subscription_id"), rs.getLong("user_id"),
                        new VariantInfo(rs.getLong("variant_id"), rs.getString("variant_name"), rs.getLong("product_id"),
                                rs.getString("product_name"), rs.getBoolean("on_sale"), rs.getLong("stock"))));
    }

    /** Only one run at a time (the hourly job and an ADMIN's manual run must not mail the same person twice). */
    public boolean tryLockRun() {
        Boolean locked = jdbc.getJdbcTemplate().queryForObject("select pg_try_advisory_xact_lock(7101)", Boolean.class);
        return Boolean.TRUE.equals(locked);
    }
}
