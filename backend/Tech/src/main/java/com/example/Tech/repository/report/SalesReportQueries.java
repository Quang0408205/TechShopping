package com.example.Tech.repository.report;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregations for the sales report and the dashboard, grouped in the database. Every query takes a half-open
 * period [from, to) and an optional store (null = every store, orders without a store included).
 * Revenue = DELIVERED orders by delivered_at; refunds = REFUNDED return requests by completed_at.
 */
@Repository
@RequiredArgsConstructor
public class SalesReportQueries {

    private static final String DELIVERED = " o.status = 'DELIVERED' and o.delivered_at >= :from and o.delivered_at < :to ";
    private static final String REFUNDED = " rr.status = 'REFUNDED' and rr.completed_at >= :from and rr.completed_at < :to ";

    private final NamedParameterJdbcTemplate jdbc;

    public record Totals(long orders, BigDecimal revenue, BigDecimal shipping) {
    }

    public record Refunds(long count, BigDecimal amount) {
    }

    public record PeriodRow(LocalDate start, long orders, BigDecimal amount) {
    }

    public record CategoryRow(Integer categoryId, String categoryName, long units, BigDecimal revenue) {
    }

    public record PaymentRow(String paymentMethod, long orders, BigDecimal revenue) {
    }

    public record StoreTotals(Integer storeId, long orders, BigDecimal amount) {
    }

    public record EmployeeTotals(Long employeeId, String employeeCode, String fullname, Integer storeId,
                                 String storeName, long orders, BigDecimal sales, BigDecimal commission) {
    }

    /** employeeId / storeId null = refunds of orders without a sales record. */
    public record EmployeeRefund(Long employeeId, Integer storeId, BigDecimal amount) {
    }

    public record ProductTotals(Long productId, String productName, String categoryName, long units,
                                BigDecimal revenue) {
    }

    /** One delivered order for the export; refunded = every refunded return of the order (any date). */
    public record DeliveredOrder(Long orderId, LocalDateTime deliveredAt, String storeName, String customer,
                                 String confirmedBy, String paymentMethod, BigDecimal total, BigDecimal refunded) {
    }

    public Totals delivered(LocalDateTime from, LocalDateTime to, Integer storeId) {
        return jdbc.queryForObject("select count(*) n, coalesce(sum(o.total_amount), 0) revenue, "
                        + "coalesce(sum(o.shipping_cost), 0) shipping from orders o where" + DELIVERED + store(storeId),
                params(from, to, storeId),
                (rs, i) -> new Totals(rs.getLong("n"), rs.getBigDecimal("revenue"), rs.getBigDecimal("shipping")));
    }

    public long unitsSold(LocalDateTime from, LocalDateTime to, Integer storeId) {
        Long units = jdbc.queryForObject("select coalesce(sum(oi.quantity), 0) from order_items oi "
                + "join orders o on o.order_id = oi.order_id where" + DELIVERED + store(storeId),
                params(from, to, storeId), Long.class);
        return units == null ? 0 : units;
    }

    public Refunds refunds(LocalDateTime from, LocalDateTime to, Integer storeId) {
        return jdbc.queryForObject("select count(*) n, coalesce(sum(rr.refund_amount), 0) amount from return_requests rr "
                        + "join orders o on o.order_id = rr.order_id where" + REFUNDED + store(storeId),
                params(from, to, storeId), (rs, i) -> new Refunds(rs.getLong("n"), rs.getBigDecimal("amount")));
    }

    public BigDecimal commission(LocalDateTime from, LocalDateTime to, Integer storeId) {
        return jdbc.queryForObject("select coalesce(sum(sr.commission), 0) from sales_records sr "
                + "join orders o on o.order_id = sr.order_id where" + DELIVERED + store(storeId),
                params(from, to, storeId), BigDecimal.class);
    }

    /** {@code monthly} false = per day. */
    public List<PeriodRow> deliveredByPeriod(LocalDateTime from, LocalDateTime to, Integer storeId, boolean monthly) {
        return jdbc.query("select date_trunc('" + unit(monthly) + "', o.delivered_at) p, count(*) n, "
                        + "sum(o.total_amount) amount from orders o where" + DELIVERED + store(storeId)
                        + " group by 1 order by 1",
                params(from, to, storeId), (rs, i) -> new PeriodRow(
                        rs.getObject("p", LocalDateTime.class).toLocalDate(), rs.getLong("n"), rs.getBigDecimal("amount")));
    }

    public List<PeriodRow> refundsByPeriod(LocalDateTime from, LocalDateTime to, Integer storeId, boolean monthly) {
        return jdbc.query("select date_trunc('" + unit(monthly) + "', rr.completed_at) p, count(*) n, "
                        + "sum(rr.refund_amount) amount from return_requests rr join orders o on o.order_id = rr.order_id "
                        + "where" + REFUNDED + store(storeId) + " group by 1 order by 1",
                params(from, to, storeId), (rs, i) -> new PeriodRow(
                        rs.getObject("p", LocalDateTime.class).toLocalDate(), rs.getLong("n"), rs.getBigDecimal("amount")));
    }

    public List<CategoryRow> byCategory(LocalDateTime from, LocalDateTime to, Integer storeId) {
        return jdbc.query("select c.category_id, c.name, sum(oi.quantity) units, sum(oi.subtotal) revenue "
                        + "from order_items oi join orders o on o.order_id = oi.order_id "
                        + "join product_variants v on v.variant_id = oi.variant_id "
                        + "join products p on p.product_id = v.product_id "
                        + "left join categories c on c.category_id = p.category_id "
                        + "where" + DELIVERED + store(storeId)
                        + " group by c.category_id, c.name order by revenue desc, c.name",
                params(from, to, storeId), (rs, i) -> new CategoryRow((Integer) rs.getObject("category_id"),
                        rs.getString("name"), rs.getLong("units"), rs.getBigDecimal("revenue")));
    }

    public List<PaymentRow> byPaymentMethod(LocalDateTime from, LocalDateTime to, Integer storeId) {
        return jdbc.query("select o.payment_method, count(*) n, sum(o.total_amount) revenue from orders o where"
                        + DELIVERED + store(storeId) + " group by o.payment_method order by revenue desc",
                params(from, to, storeId), (rs, i) -> new PaymentRow(rs.getString("payment_method"), rs.getLong("n"),
                        rs.getBigDecimal("revenue")));
    }

    public Map<Integer, StoreTotals> deliveredByStore(LocalDateTime from, LocalDateTime to) {
        return jdbc.query("select o.store_id, count(*) n, sum(o.total_amount) amount from orders o where" + DELIVERED
                        + "and o.store_id is not null group by o.store_id", params(from, to, null),
                (rs, i) -> new StoreTotals(rs.getInt("store_id"), rs.getLong("n"), rs.getBigDecimal("amount")))
                .stream().collect(Collectors.toMap(StoreTotals::storeId, row -> row));
    }

    public Map<Integer, StoreTotals> refundsByStore(LocalDateTime from, LocalDateTime to) {
        return jdbc.query("select o.store_id, count(*) n, sum(rr.refund_amount) amount from return_requests rr "
                        + "join orders o on o.order_id = rr.order_id where" + REFUNDED
                        + "and o.store_id is not null group by o.store_id", params(from, to, null),
                (rs, i) -> new StoreTotals(rs.getInt("store_id"), rs.getLong("n"), rs.getBigDecimal("amount")))
                .stream().collect(Collectors.toMap(StoreTotals::storeId, row -> row));
    }

    public List<EmployeeTotals> byEmployee(LocalDateTime from, LocalDateTime to, Integer storeId) {
        return jdbc.query("select e.employee_id, e.employee_code, u.fullname, s.store_id, s.name store_name, "
                        + "count(*) n, sum(sr.sales_amount) sales, sum(sr.commission) commission "
                        + "from orders o join sales_records sr on sr.order_id = o.order_id "
                        + "join employees e on e.employee_id = sr.employee_id join users u on u.user_id = e.user_id "
                        + "join stores s on s.store_id = sr.store_id where" + DELIVERED + store(storeId)
                        + " group by e.employee_id, e.employee_code, u.fullname, s.store_id, s.name "
                        + "order by sales desc, u.fullname",
                params(from, to, storeId), (rs, i) -> new EmployeeTotals(rs.getLong("employee_id"),
                        rs.getString("employee_code"), rs.getString("fullname"), rs.getInt("store_id"),
                        rs.getString("store_name"), rs.getLong("n"), rs.getBigDecimal("sales"),
                        rs.getBigDecimal("commission")));
    }

    /** Delivered orders of the period without a sales record. */
    public Totals deliveredWithoutSalesRecord(LocalDateTime from, LocalDateTime to, Integer storeId) {
        return jdbc.queryForObject("select count(*) n, coalesce(sum(o.total_amount), 0) revenue, 0 shipping "
                        + "from orders o where" + DELIVERED + store(storeId)
                        + " and not exists (select 1 from sales_records sr where sr.order_id = o.order_id)",
                params(from, to, storeId),
                (rs, i) -> new Totals(rs.getLong("n"), rs.getBigDecimal("revenue"), BigDecimal.ZERO));
    }

    public List<EmployeeRefund> refundsByEmployee(LocalDateTime from, LocalDateTime to, Integer storeId) {
        return jdbc.query("select sr.employee_id, sr.store_id, sum(rr.refund_amount) amount from return_requests rr "
                        + "join orders o on o.order_id = rr.order_id left join sales_records sr on sr.order_id = o.order_id "
                        + "where" + REFUNDED + store(storeId) + " group by sr.employee_id, sr.store_id",
                params(from, to, storeId), (rs, i) -> new EmployeeRefund((Long) rs.getObject("employee_id"),
                        (Integer) rs.getObject("store_id"), rs.getBigDecimal("amount")));
    }

    public List<ProductTotals> topProducts(LocalDateTime from, LocalDateTime to, Integer storeId, int limit) {
        MapSqlParameterSource params = params(from, to, storeId).addValue("limit", limit);
        return jdbc.query("select p.product_id, p.name, c.name category_name, sum(oi.quantity) units, "
                        + "sum(oi.subtotal) revenue from order_items oi join orders o on o.order_id = oi.order_id "
                        + "join product_variants v on v.variant_id = oi.variant_id "
                        + "join products p on p.product_id = v.product_id "
                        + "left join categories c on c.category_id = p.category_id where" + DELIVERED + store(storeId)
                        + " group by p.product_id, p.name, c.name order by revenue desc, units desc, p.product_id "
                        + "limit :limit",
                params, (rs, i) -> new ProductTotals(rs.getLong("product_id"), rs.getString("name"),
                        rs.getString("category_name"), rs.getLong("units"), rs.getBigDecimal("revenue")));
    }

    /** Delivered orders of the period, oldest first, at most {@code limit} rows. */
    public List<DeliveredOrder> deliveredOrders(LocalDateTime from, LocalDateTime to, Integer storeId, int limit) {
        MapSqlParameterSource params = params(from, to, storeId).addValue("limit", limit);
        return jdbc.query("select o.order_id, o.delivered_at, s.name store_name, coalesce(u.fullname, o.recipient_name) customer, "
                        + "eu.fullname confirmed_by, o.payment_method, o.total_amount, "
                        + "coalesce((select sum(rr.refund_amount) from return_requests rr "
                        + "where rr.order_id = o.order_id and rr.status = 'REFUNDED'), 0) refunded "
                        + "from orders o left join stores s on s.store_id = o.store_id left join users u on u.user_id = o.user_id "
                        + "left join sales_records sr on sr.order_id = o.order_id "
                        + "left join employees e on e.employee_id = sr.employee_id left join users eu on eu.user_id = e.user_id "
                        + "where" + DELIVERED + store(storeId) + " order by o.delivered_at, o.order_id limit :limit",
                params, (rs, i) -> new DeliveredOrder(rs.getLong("order_id"), rs.getObject("delivered_at", LocalDateTime.class),
                        rs.getString("store_name"), rs.getString("customer"), rs.getString("confirmed_by"),
                        rs.getString("payment_method"), rs.getBigDecimal("total_amount"), rs.getBigDecimal("refunded")));
    }

    /** Orders per status, for the given statuses (store null = every order). */
    public Map<String, Long> orderCountsByStatus(List<String> statuses, Integer storeId) {
        MapSqlParameterSource params = new MapSqlParameterSource("statuses", statuses).addValue("storeId", storeId);
        return jdbc.query("select o.status, count(*) n from orders o where o.status in (:statuses)" + store(storeId)
                        + " group by o.status", params, (rs, i) -> Map.entry(rs.getString("status"), rs.getLong("n")))
                .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** Requests of the three after-sales kinds still waiting for the store (not finished, rejected or cancelled). */
    public long openServiceRequests(Integer storeId) {
        Long n = jdbc.queryForObject("select count(*) from service_requests_view o "
                        + "where o.status not in ('COMPLETED', 'REFUNDED', 'REJECTED', 'CANCELLED')" + store(storeId),
                new MapSqlParameterSource("storeId", storeId), Long.class);
        return n == null ? 0 : n;
    }

    /** Stocked variants of products still on sale with 1..lowStockMax units, and with none. */
    public long[] lowAndOutOfStock(Integer storeId, int lowStockMax) {
        MapSqlParameterSource params = new MapSqlParameterSource("storeId", storeId).addValue("low", lowStockMax);
        return jdbc.queryForObject("select count(*) filter (where i.quantity between 1 and :low) low, "
                        + "count(*) filter (where i.quantity <= 0) out_of_stock from inventory i "
                        + "join product_variants v on v.variant_id = i.variant_id "
                        + "join products p on p.product_id = v.product_id "
                        + "where p.deleted_at is null and p.is_active" + (storeId == null ? "" : " and i.store_id = :storeId"),
                params, (rs, i) -> new long[]{rs.getLong("low"), rs.getLong("out_of_stock")});
    }

    public long openStores() {
        Long n = jdbc.queryForObject("select count(*) from stores where is_active", Map.of(), Long.class);
        return n == null ? 0 : n;
    }

    public long activeEmployees() {
        Long n = jdbc.queryForObject("select count(*) from employees where is_active", Map.of(), Long.class);
        return n == null ? 0 : n;
    }

    private static String unit(boolean monthly) {
        return monthly ? "month" : "day";
    }

    /** Alias {@code o} must be the table with store_id (orders or the view). */
    private static String store(Integer storeId) {
        return storeId == null ? "" : " and o.store_id = :storeId";
    }

    private static MapSqlParameterSource params(LocalDateTime from, LocalDateTime to, Integer storeId) {
        return new MapSqlParameterSource("from", from).addValue("to", to).addValue("storeId", storeId);
    }
}
