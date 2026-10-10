package com.example.Tech.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Overview cards: one store for STAFF (storeId set), every store for an ADMIN (storeId null). Revenues are net
 * (delivered − refunded, see SalesReportResponse). previousMonthSamePeriod = the 1st of last month up to the same
 * day of the month (capped at its last day), so it compares with thisMonth fairly. openStores / activeEmployees are
 * ADMIN only (null for STAFF).
 */
public record DashboardSummaryResponse(
        Integer storeId,
        String storeName,
        LocalDate today,
        BigDecimal revenueToday,
        long deliveredToday,
        BigDecimal revenueThisMonth,
        long deliveredThisMonth,
        BigDecimal revenuePreviousMonthSamePeriod,
        long pendingOrders,
        long confirmedOrders,
        long shippingOrders,
        long openServiceRequests,
        long lowStockVariants,
        long outOfStockVariants,
        Long openStores,
        Long activeEmployees
) {
}
