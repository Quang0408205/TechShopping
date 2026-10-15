package com.example.Tech.dto.response.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Overview cards: one store for STAFF / BRANCH_MANAGER (storeId set), every store for an ADMIN (storeId null).
 * The three revenue figures are null for plain STAFF (only ADMIN and BRANCH_MANAGER see revenue). Revenues are net
 * (delivered − refunded, see SalesReportResponse). previousMonthSamePeriod = the 1st of last month up to the same
 * day of the month (capped at its last day), so it compares with thisMonth fairly. openStores / activeEmployees are
 * ADMIN only (null for STAFF). openContactRequests = contact messages not resolved yet (shared by every store).
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
        long openContactRequests,
        long lowStockVariants,
        long outOfStockVariants,
        Long openStores,
        Long activeEmployees
) {
}
