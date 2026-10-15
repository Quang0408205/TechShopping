package com.example.Tech.dto.response.report;

import com.example.Tech.dto.request.report.ReportGroupBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Revenue = orders DELIVERED in the period (by delivery day, order total incl. shipping); refunds = return requests
 * REFUNDED in the period (by refund day); net = revenue − refunds. Category / product figures use the order lines
 * (no shipping fee), so they add up to revenue − shipping fees.
 */
public record SalesReportResponse(
        Integer storeId,
        String storeName,
        LocalDate fromDate,
        LocalDate toDate,
        ReportGroupBy groupBy,
        Summary summary,
        List<Period> series,
        List<CategorySlice> byCategory,
        List<PaymentSlice> byPaymentMethod,
        List<StoreRow> byStore,
        List<EmployeeRow> byEmployee,
        List<ProductRow> topProducts
) {

    public record Summary(
            BigDecimal grossRevenue,
            BigDecimal refundAmount,
            BigDecimal netRevenue,
            long deliveredOrders,
            long unitsSold,
            BigDecimal averageOrderValue,
            BigDecimal shippingFees,
            long refundCount,
            BigDecimal totalCommission
    ) {
    }

    /** One day or one month; every period of the range is present, with zeros when nothing happened. */
    public record Period(
            LocalDate start,
            long orders,
            BigDecimal grossRevenue,
            BigDecimal refundAmount,
            BigDecimal netRevenue
    ) {
    }

    public record CategorySlice(Integer categoryId, String categoryName, long units, BigDecimal revenue) {
    }

    /** paymentMethod: COD / BANK_TRANSFER / INSTALLMENT. */
    public record PaymentSlice(String paymentMethod, long orders, BigDecimal revenue) {
    }

    /** ADMIN without a store filter only: open stores always, closed ones when they had activity. */
    public record StoreRow(
            Integer storeId,
            String storeName,
            boolean storeActive,
            long orders,
            BigDecimal grossRevenue,
            BigDecimal refundAmount,
            BigDecimal netRevenue
    ) {
    }

    /**
     * One row per employee and store; employeeId null = delivered orders nobody with an employee profile confirmed
     * (confirmed before Phase 10). refundAmount = refunds in the period of orders credited to that row.
     */
    public record EmployeeRow(
            Long employeeId,
            String employeeCode,
            String fullname,
            Integer storeId,
            String storeName,
            long orders,
            BigDecimal salesAmount,
            BigDecimal commission,
            BigDecimal refundAmount
    ) {
    }

    public record ProductRow(Long productId, String productName, String categoryName, long units, BigDecimal revenue) {
    }
}
