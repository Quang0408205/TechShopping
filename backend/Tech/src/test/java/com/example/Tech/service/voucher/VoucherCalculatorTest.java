package com.example.Tech.service.voucher;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.voucher.Voucher;
import com.example.Tech.service.voucher.VoucherCalculator.Line;
import com.example.Tech.service.voucher.VoucherCalculator.Problem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VoucherCalculatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 17, 12, 0);

    @Test
    void percentOfTheGoods_cappedByMaxDiscount_splitOverTheLines() {
        Voucher voucher = voucher("10", "500000", "5000000");
        VoucherCalculator.Result result = VoucherCalculator.evaluate(voucher,
                List.of(line(1, 1, "3333333"), line(2, 1, "1666667"), line(3, 2, "3000000")), 0, NOW);

        assertThat(result.applies()).isTrue();
        assertThat(result.eligibleSubtotal()).isEqualByComparingTo("8000000");
        // 10% = 800.000 → capped at 500.000
        assertThat(result.discount()).isEqualByComparingTo("500000");
        assertThat(result.shares().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("500000");
        // 208.333 + 104.166 + 187.500 = 499.999: the rounding remainder (1đ) goes to the largest line
        assertThat(result.shares().get(1L)).isEqualByComparingTo("208334");
        assertThat(result.shares().get(2L)).isEqualByComparingTo("104166");
        assertThat(result.shares().get(3L)).isEqualByComparingTo("187500");
    }

    @Test
    void noCap_roundsDownToWholeDong() {
        Voucher voucher = voucher("7.5", null, "0");
        VoucherCalculator.Result result = VoucherCalculator.evaluate(voucher, List.of(line(1, 1, "1999999")), 0, NOW);

        assertThat(result.discount()).isEqualByComparingTo("149999");
    }

    @Test
    void categoryScope_onlyThoseLinesCount_forTheMinimumAndTheDiscount() {
        Voucher voucher = voucher("10", null, "2000000");
        voucher.setAllCategories(false);
        voucher.getCategories().add(category(2));

        VoucherCalculator.Result below = VoucherCalculator.evaluate(voucher,
                List.of(line(1, 1, "9000000"), line(2, 2, "1500000")), 0, NOW);
        assertThat(below.problem()).isEqualTo(Problem.BELOW_MINIMUM);
        assertThat(below.shortfall()).isEqualByComparingTo("500000");
        assertThat(below.message()).contains("500.000");

        VoucherCalculator.Result ok = VoucherCalculator.evaluate(voucher,
                List.of(line(1, 1, "9000000"), line(2, 2, "2500000")), 0, NOW);
        assertThat(ok.discount()).isEqualByComparingTo("250000");
        assertThat(ok.shares()).containsOnlyKeys(2L);

        assertThat(VoucherCalculator.evaluate(voucher, List.of(line(1, 1, "9000000")), 0, NOW).problem())
                .isEqualTo(Problem.NO_ELIGIBLE_ITEMS);
    }

    @Test
    void windowSwitchAndLimits_areChecked() {
        Voucher voucher = voucher("10", null, "0");
        List<Line> lines = List.of(line(1, 1, "1000000"));

        voucher.setStartsAt(NOW.plusMinutes(1));
        assertThat(VoucherCalculator.evaluate(voucher, lines, 0, NOW).problem()).isEqualTo(Problem.NOT_STARTED);
        voucher.setStartsAt(NOW.minusDays(1));
        voucher.setEndsAt(NOW);
        assertThat(VoucherCalculator.evaluate(voucher, lines, 0, NOW).problem()).isEqualTo(Problem.EXPIRED);
        voucher.setEndsAt(NOW.plusDays(1));
        voucher.setActive(false);
        assertThat(VoucherCalculator.evaluate(voucher, lines, 0, NOW).problem()).isEqualTo(Problem.INACTIVE);
        voucher.setActive(true);
        voucher.setUsageLimit(3);
        voucher.setUsedCount(3);
        assertThat(VoucherCalculator.evaluate(voucher, lines, 0, NOW).problem()).isEqualTo(Problem.USED_UP);
        voucher.setUsedCount(2);
        assertThat(VoucherCalculator.evaluate(voucher, lines, 1, NOW).problem()).isEqualTo(Problem.CUSTOMER_LIMIT);
        voucher.setPerCustomerLimit(2);
        assertThat(VoucherCalculator.evaluate(voucher, lines, 1, NOW).applies()).isTrue();
    }

    @Test
    void hundredPercent_neverMoreThanTheGoods() {
        VoucherCalculator.Result result = VoucherCalculator.evaluate(voucher("100", null, "0"),
                List.of(line(1, 1, "450000")), 0, NOW);

        assertThat(result.discount()).isEqualByComparingTo("450000");
    }

    @Test
    void returnedUnits_payBackTheirShareOfTheVoucher() {
        com.example.Tech.entity.order.OrderItem item = new com.example.Tech.entity.order.OrderItem();
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("1000000"));
        item.setVoucherDiscount(new BigDecimal("100000"));

        assertThat(item.paidFor(1)).isEqualByComparingTo("966667");
        assertThat(item.paidFor(3)).isEqualByComparingTo("2900000");
        item.setVoucherDiscount(BigDecimal.ZERO);
        assertThat(item.paidFor(2)).isEqualByComparingTo("2000000");
    }

    private static Voucher voucher(String percent, String max, String min) {
        Voucher voucher = new Voucher();
        voucher.setCode("TEST");
        voucher.setDiscountPercent(new BigDecimal(percent));
        voucher.setMaxDiscount(max == null ? null : new BigDecimal(max));
        voucher.setMinOrderAmount(new BigDecimal(min));
        voucher.setStartsAt(NOW.minusDays(1));
        voucher.setEndsAt(NOW.plusDays(1));
        return voucher;
    }

    private static Category category(int id) {
        Category category = new Category();
        category.setId(id);
        return category;
    }

    private static Line line(long key, int categoryId, String subtotal) {
        return new Line(key, categoryId, new BigDecimal(subtotal));
    }
}
