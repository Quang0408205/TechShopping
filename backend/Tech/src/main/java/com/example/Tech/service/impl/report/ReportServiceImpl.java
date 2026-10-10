package com.example.Tech.service.impl.report;

import com.example.Tech.dto.request.report.ReportGroupBy;
import com.example.Tech.dto.request.report.SalesReportRequest;
import com.example.Tech.dto.response.report.DashboardSummaryResponse;
import com.example.Tech.dto.response.report.SalesReportResponse;
import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.store.Store;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.employee.EmployeeRepository;
import com.example.Tech.repository.report.SalesReportQueries;
import com.example.Tech.repository.report.SalesReportQueries.EmployeeRefund;
import com.example.Tech.repository.report.SalesReportQueries.EmployeeTotals;
import com.example.Tech.repository.report.SalesReportQueries.PeriodRow;
import com.example.Tech.repository.report.SalesReportQueries.StoreTotals;
import com.example.Tech.repository.report.SalesReportQueries.Totals;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.service.report.ReportService;
import com.example.Tech.service.store.StoreAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    static final int MAX_DAYS = 366;
    static final int MAX_DAYS_BY_DAY = 62;
    static final int DEFAULT_DAYS = 30;
    static final int TOP_PRODUCTS = 10;
    static final int LOW_STOCK_MAX = 5;

    private final StoreAccessGuard storeAccessGuard;
    private final SalesReportQueries queries;
    private final StoreRepository storeRepository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;

    @Override
    public SalesReportResponse sales(Long userId, SalesReportRequest filter) {
        StoreAccessGuard.OrderScope scope = storeAccessGuard.reportScope(userId);
        Integer storeId = filter.storeId();
        if (!scope.admin()) {
            if (storeId != null && !storeId.equals(scope.storeId())) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, "Bạn chỉ xem được báo cáo của chi nhánh mình");
            }
            storeId = scope.storeId();
        }
        Store store = storeId == null ? null : findStore(storeId);

        LocalDate toDate = filter.toDate() != null ? filter.toDate() : LocalDate.now(clock);
        LocalDate fromDate = filter.fromDate() != null ? filter.fromDate() : toDate.minusDays(DEFAULT_DAYS - 1);
        if (fromDate.isAfter(toDate)) {
            throw BusinessException.invalidField("fromDate", "Ngày bắt đầu phải trước hoặc trùng ngày kết thúc");
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > MAX_DAYS) {
            throw BusinessException.invalidField("toDate", "Chỉ xem được tối đa %d ngày mỗi lần".formatted(MAX_DAYS));
        }
        ReportGroupBy groupBy = filter.groupBy() != null ? filter.groupBy()
                : days <= MAX_DAYS_BY_DAY ? ReportGroupBy.DAY : ReportGroupBy.MONTH;
        if (groupBy == ReportGroupBy.DAY && days > MAX_DAYS_BY_DAY) {
            throw BusinessException.invalidField("groupBy",
                    "Xem theo ngày tối đa %d ngày; hãy chọn xem theo tháng".formatted(MAX_DAYS_BY_DAY));
        }

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.plusDays(1).atStartOfDay();
        boolean monthly = groupBy == ReportGroupBy.MONTH;

        Totals delivered = queries.delivered(from, to, storeId);
        SalesReportQueries.Refunds refunds = queries.refunds(from, to, storeId);
        SalesReportResponse.Summary summary = new SalesReportResponse.Summary(
                delivered.revenue(), refunds.amount(), delivered.revenue().subtract(refunds.amount()),
                delivered.orders(), queries.unitsSold(from, to, storeId),
                delivered.orders() == 0 ? BigDecimal.ZERO
                        : delivered.revenue().divide(BigDecimal.valueOf(delivered.orders()), 0, RoundingMode.HALF_UP),
                delivered.shipping(), refunds.count(), queries.commission(from, to, storeId));

        return new SalesReportResponse(storeId, store != null ? store.getName() : null, fromDate, toDate, groupBy,
                summary,
                series(fromDate, toDate, monthly, queries.deliveredByPeriod(from, to, storeId, monthly),
                        queries.refundsByPeriod(from, to, storeId, monthly)),
                queries.byCategory(from, to, storeId).stream()
                        .map(row -> new SalesReportResponse.CategorySlice(row.categoryId(), row.categoryName(),
                                row.units(), row.revenue()))
                        .toList(),
                queries.byPaymentMethod(from, to, storeId).stream()
                        .map(row -> new SalesReportResponse.PaymentSlice(row.paymentMethod(), row.orders(), row.revenue()))
                        .toList(),
                scope.admin() && storeId == null ? byStore(from, to) : List.of(),
                byEmployee(from, to, storeId),
                queries.topProducts(from, to, storeId, TOP_PRODUCTS).stream()
                        .map(row -> new SalesReportResponse.ProductRow(row.productId(), row.productName(),
                                row.categoryName(), row.units(), row.revenue()))
                        .toList());
    }

    @Override
    public DashboardSummaryResponse dashboard(Long userId) {
        StoreAccessGuard.OrderScope scope = storeAccessGuard.orderScope(userId);
        Integer storeId = scope.storeId();
        LocalDate today = LocalDate.now(clock);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate previousStart = monthStart.minusMonths(1);
        // same number of days into last month, capped at its length (31 March vs 1–28 February)
        LocalDate previousEnd = previousStart.withDayOfMonth(Math.min(today.getDayOfMonth(), previousStart.lengthOfMonth()));

        Totals deliveredToday = delivered(today, today, storeId);
        Totals deliveredMonth = delivered(monthStart, today, storeId);
        Map<String, Long> statuses = queries.orderCountsByStatus(List.of("PENDING", "CONFIRMED", "SHIPPING"), storeId);
        long[] stock = queries.lowAndOutOfStock(storeId, LOW_STOCK_MAX);

        return new DashboardSummaryResponse(storeId, storeId == null ? null : findStore(storeId).getName(), today,
                net(deliveredToday, today, today, storeId), deliveredToday.orders(),
                net(deliveredMonth, monthStart, today, storeId), deliveredMonth.orders(),
                net(delivered(previousStart, previousEnd, storeId), previousStart, previousEnd, storeId),
                statuses.getOrDefault("PENDING", 0L), statuses.getOrDefault("CONFIRMED", 0L),
                statuses.getOrDefault("SHIPPING", 0L),
                queries.openServiceRequests(storeId), stock[0], stock[1],
                scope.admin() ? queries.openStores() : null,
                scope.admin() ? queries.activeEmployees() : null);
    }

    /** Over the days [first, last]. */
    private Totals delivered(LocalDate first, LocalDate last, Integer storeId) {
        return queries.delivered(first.atStartOfDay(), last.plusDays(1).atStartOfDay(), storeId);
    }

    /** Delivered − refunded over the days [first, last]. */
    private BigDecimal net(Totals delivered, LocalDate first, LocalDate last, Integer storeId) {
        return delivered.revenue().subtract(
                queries.refunds(first.atStartOfDay(), last.plusDays(1).atStartOfDay(), storeId).amount());
    }

    private static List<SalesReportResponse.Period> series(LocalDate fromDate, LocalDate toDate, boolean monthly,
                                                           List<PeriodRow> delivered, List<PeriodRow> refunded) {
        Map<LocalDate, PeriodRow> sales = delivered.stream().collect(Collectors.toMap(PeriodRow::start, Function.identity()));
        Map<LocalDate, PeriodRow> refunds = refunded.stream().collect(Collectors.toMap(PeriodRow::start, Function.identity()));
        List<SalesReportResponse.Period> periods = new ArrayList<>();
        LocalDate start = monthly ? fromDate.withDayOfMonth(1) : fromDate;
        for (LocalDate day = start; !day.isAfter(toDate); day = monthly ? day.plusMonths(1) : day.plusDays(1)) {
            PeriodRow sale = sales.get(day);
            PeriodRow refund = refunds.get(day);
            BigDecimal gross = sale == null ? BigDecimal.ZERO : sale.amount();
            BigDecimal refundAmount = refund == null ? BigDecimal.ZERO : refund.amount();
            periods.add(new SalesReportResponse.Period(day, sale == null ? 0 : sale.orders(), gross, refundAmount,
                    gross.subtract(refundAmount)));
        }
        return periods;
    }

    private List<SalesReportResponse.StoreRow> byStore(LocalDateTime from, LocalDateTime to) {
        Map<Integer, StoreTotals> sales = queries.deliveredByStore(from, to);
        Map<Integer, StoreTotals> refunds = queries.refundsByStore(from, to);
        return storeRepository.findAll(Sort.by("name", "id")).stream()
                .filter(store -> !Boolean.FALSE.equals(store.getActive())
                        || sales.containsKey(store.getId()) || refunds.containsKey(store.getId()))
                .map(store -> {
                    StoreTotals sale = sales.get(store.getId());
                    StoreTotals refund = refunds.get(store.getId());
                    BigDecimal gross = sale == null ? BigDecimal.ZERO : sale.amount();
                    BigDecimal refunded = refund == null ? BigDecimal.ZERO : refund.amount();
                    return new SalesReportResponse.StoreRow(store.getId(), store.getName(),
                            !Boolean.FALSE.equals(store.getActive()), sale == null ? 0 : sale.orders(), gross, refunded,
                            gross.subtract(refunded));
                })
                .sorted(Comparator.comparing(SalesReportResponse.StoreRow::netRevenue).reversed())
                .toList();
    }

    private List<SalesReportResponse.EmployeeRow> byEmployee(LocalDateTime from, LocalDateTime to, Integer storeId) {
        Map<String, BigDecimal> refunds = new HashMap<>();
        for (EmployeeRefund refund : queries.refundsByEmployee(from, to, storeId)) {
            refunds.merge(key(refund.employeeId(), refund.storeId()), refund.amount(), BigDecimal::add);
        }
        List<SalesReportResponse.EmployeeRow> rows = new ArrayList<>();
        for (EmployeeTotals row : queries.byEmployee(from, to, storeId)) {
            rows.add(new SalesReportResponse.EmployeeRow(row.employeeId(), row.employeeCode(), row.fullname(),
                    row.storeId(), row.storeName(), row.orders(), row.sales(), row.commission(),
                    orZero(refunds.remove(key(row.employeeId(), row.storeId())))));
        }
        BigDecimal unassignedRefunds = orZero(refunds.remove(key(null, null)));
        // refunds in the period of an employee's orders delivered before it: a row without sales
        refunds.forEach((key, amount) -> rows.add(refundOnlyRow(key, amount)));
        Totals unassigned = queries.deliveredWithoutSalesRecord(from, to, storeId);
        if (unassigned.orders() > 0 || unassignedRefunds.signum() > 0) {
            rows.add(new SalesReportResponse.EmployeeRow(null, null, null, null, null, unassigned.orders(),
                    unassigned.revenue(), BigDecimal.ZERO, unassignedRefunds));
        }
        return rows;
    }

    private SalesReportResponse.EmployeeRow refundOnlyRow(String key, BigDecimal amount) {
        String[] parts = key.split(":");
        Long employeeId = Long.valueOf(parts[0]);
        Integer storeId = Integer.valueOf(parts[1]);
        Employee employee = employeeRepository.findWithUserById(employeeId).orElse(null);
        Store store = storeRepository.findById(storeId).orElse(null);
        return new SalesReportResponse.EmployeeRow(employeeId, employee != null ? employee.getEmployeeCode() : null,
                employee != null ? employee.getUser().getFullname() : null, storeId,
                store != null ? store.getName() : null, 0, BigDecimal.ZERO, BigDecimal.ZERO, amount);
    }

    private static String key(Long employeeId, Integer storeId) {
        return Objects.toString(employeeId, "-") + ":" + Objects.toString(storeId, "-");
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Store findStore(Integer storeId) {
        return storeRepository.findById(storeId).orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND,
                "Không tìm thấy chi nhánh id %d".formatted(storeId)));
    }
}
