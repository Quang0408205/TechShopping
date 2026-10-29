package com.example.Tech.service.voucher;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.voucher.Voucher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Plan v5 GĐ4, no I/O: whether a voucher applies to a set of lines and how much it takes off.
 * In-scope lines = every line, or the lines of the voucher's categories. discount = ⌊in-scope × percent⌋ (whole
 * đồng), capped by maxDiscount and by the in-scope amount. The discount is split over the in-scope lines in
 * proportion to their subtotal (rounded down; the remainder goes to the largest line) so a return pays back
 * exactly what the customer paid for those units.
 */
public final class VoucherCalculator {

    private VoucherCalculator() {
    }

    /** One priced line: key = cart / order line id, subtotal = unit price after promotions × quantity. */
    public record Line(Long key, Integer categoryId, BigDecimal subtotal) {
    }

    public enum Problem { INACTIVE, NOT_STARTED, EXPIRED, USED_UP, CUSTOMER_LIMIT, NO_ELIGIBLE_ITEMS, BELOW_MINIMUM }

    /**
     * problem = null when the voucher applies (discount + shares filled); shortfall = how much more in-scope goods
     * are needed for BELOW_MINIMUM.
     */
    public record Result(Problem problem, String message, BigDecimal eligibleSubtotal, BigDecimal discount,
                         BigDecimal shortfall, Map<Long, BigDecimal> shares) {

        public boolean applies() {
            return problem == null;
        }
    }

    public static Result evaluate(Voucher voucher, List<Line> lines, long customerUses, LocalDateTime now) {
        Set<Integer> scope = Boolean.TRUE.equals(voucher.getAllCategories()) ? null
                : voucher.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
        List<Line> eligible = lines.stream()
                .filter(line -> scope == null || (line.categoryId() != null && scope.contains(line.categoryId())))
                .toList();
        BigDecimal eligibleSubtotal = eligible.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        Problem problem = null;
        String message = null;
        BigDecimal shortfall = BigDecimal.ZERO;
        if (!Boolean.TRUE.equals(voucher.getActive())) {
            problem = Problem.INACTIVE;
            message = "Voucher đang tạm ngưng";
        } else if (now.isBefore(voucher.getStartsAt())) {
            problem = Problem.NOT_STARTED;
            message = "Voucher chưa đến thời gian áp dụng";
        } else if (!now.isBefore(voucher.getEndsAt())) {
            problem = Problem.EXPIRED;
            message = "Voucher đã hết hạn";
        } else if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            problem = Problem.USED_UP;
            message = "Voucher đã hết lượt sử dụng";
        } else if (customerUses >= voucher.getPerCustomerLimit()) {
            problem = Problem.CUSTOMER_LIMIT;
            message = "Bạn đã dùng hết lượt của voucher này";
        } else if (eligibleSubtotal.signum() <= 0) {
            problem = Problem.NO_ELIGIBLE_ITEMS;
            message = "Giỏ hàng không có sản phẩm thuộc danh mục áp dụng voucher";
        } else if (eligibleSubtotal.compareTo(voucher.getMinOrderAmount()) < 0) {
            problem = Problem.BELOW_MINIMUM;
            shortfall = voucher.getMinOrderAmount().subtract(eligibleSubtotal);
            message = "Mua thêm %sđ sản phẩm áp dụng để dùng voucher này".formatted(vnd(shortfall));
        }
        if (problem != null) {
            return new Result(problem, message, eligibleSubtotal, BigDecimal.ZERO, shortfall, Map.of());
        }

        BigDecimal discount = eligibleSubtotal.multiply(voucher.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        if (voucher.getMaxDiscount() != null && discount.compareTo(voucher.getMaxDiscount()) > 0) {
            discount = voucher.getMaxDiscount().setScale(0, RoundingMode.FLOOR);
        }
        discount = discount.min(eligibleSubtotal);
        return new Result(null, null, eligibleSubtotal, discount, BigDecimal.ZERO, split(eligible, eligibleSubtotal, discount));
    }

    private static Map<Long, BigDecimal> split(List<Line> eligible, BigDecimal total, BigDecimal discount) {
        Map<Long, BigDecimal> shares = new LinkedHashMap<>();
        BigDecimal given = BigDecimal.ZERO;
        Line largest = null;
        for (Line line : eligible) {
            BigDecimal share = discount.multiply(line.subtotal()).divide(total, 0, RoundingMode.FLOOR);
            shares.put(line.key(), share);
            given = given.add(share);
            if (largest == null || line.subtotal().compareTo(largest.subtotal()) > 0) {
                largest = line;
            }
        }
        if (largest != null) {
            shares.merge(largest.key(), discount.subtract(given), BigDecimal::add);
        }
        return shares;
    }

    static String vnd(BigDecimal amount) {
        return String.format(java.util.Locale.ROOT, "%,d", amount.setScale(0, RoundingMode.CEILING).longValue())
                .replace(',', '.');
    }
}
