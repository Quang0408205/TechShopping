package com.example.Tech.service.combo;

import com.example.Tech.entity.combo.ComboDiscountType;
import com.example.Tech.service.combo.ComboCalculator.Combo;
import com.example.Tech.service.combo.ComboCalculator.Line;
import com.example.Tech.service.combo.ComboCalculator.Result;
import com.example.Tech.service.combo.ComboCalculator.Rule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComboCalculatorTest {

    private static final long PHONE = 1;
    private static final long COVER = 2;
    private static final long CHARGER = 3;

    private static Rule percent(int value) {
        return new Rule(ComboDiscountType.PERCENT, BigDecimal.valueOf(value));
    }

    private static Rule amount(int value) {
        return new Rule(ComboDiscountType.AMOUNT, BigDecimal.valueOf(value));
    }

    private static Line line(long key, long product, int price, int quantity) {
        return new Line(key, product, BigDecimal.valueOf(price), quantity);
    }

    private static Combo combo(long id, long main, Map<Long, Rule> addOns) {
        return new Combo(id, "Combo " + id, main, addOns);
    }

    @Test
    void noMainProductInTheCart_noDiscount() {
        Result result = ComboCalculator.evaluate(List.of(combo(1, PHONE, Map.of(COVER, percent(50)))),
                List.of(line(10, COVER, 200_000, 2)));
        assertThat(result.total()).isEqualByComparingTo("0");
        assertThat(result.applied()).isEmpty();
    }

    @Test
    void oneAddOnPerMainUnit() {
        List<Combo> combos = List.of(combo(1, PHONE, Map.of(COVER, percent(50))));
        // 1 phone + 2 covers → one cover discounted
        Result one = ComboCalculator.evaluate(combos, List.of(line(10, PHONE, 15_000_000, 1), line(11, COVER, 200_000, 2)));
        assertThat(one.total()).isEqualByComparingTo("100000");
        assertThat(one.discountOf(11L)).isEqualByComparingTo("100000");
        assertThat(one.applied()).singleElement().satisfies(a -> assertThat(a.units()).isEqualTo(1));
        // 2 phones + 2 covers → both
        Result two = ComboCalculator.evaluate(combos, List.of(line(10, PHONE, 15_000_000, 2), line(11, COVER, 200_000, 2)));
        assertThat(two.total()).isEqualByComparingTo("200000");
        assertThat(two.lineCombos().get(11L)).containsExactly("Combo 1");
    }

    @Test
    void amountDiscountNeverGoesBelowZero_percentRoundsDown() {
        List<Combo> combos = List.of(combo(1, PHONE, Map.of(COVER, amount(300_000), CHARGER, percent(33))));
        Result result = ComboCalculator.evaluate(combos, List.of(line(10, PHONE, 15_000_000, 1),
                line(11, COVER, 200_000, 1), line(12, CHARGER, 99_999, 1)));
        assertThat(result.discountOf(11L)).isEqualByComparingTo("200000");
        assertThat(result.discountOf(12L)).isEqualByComparingTo("32999");
    }

    @Test
    void anAddOnInTwoCombos_takesTheLargestDiscount() {
        List<Combo> combos = List.of(
                combo(1, PHONE, Map.of(COVER, percent(10))),
                combo(2, PHONE, Map.of(COVER, amount(150_000))));
        Result result = ComboCalculator.evaluate(combos, List.of(line(10, PHONE, 15_000_000, 1), line(11, COVER, 200_000, 1)));
        assertThat(result.total()).isEqualByComparingTo("150000");
        assertThat(result.applied()).singleElement().satisfies(a -> assertThat(a.comboId()).isEqualTo(2L));
    }

    @Test
    void severalCombosOfOnePhone_doNotGiveTwoDiscountsToTheSameAddOnUnit() {
        List<Combo> combos = List.of(
                combo(1, PHONE, Map.of(COVER, percent(50))),
                combo(2, PHONE, Map.of(COVER, percent(20), CHARGER, percent(10))));
        Result result = ComboCalculator.evaluate(combos, List.of(line(10, PHONE, 15_000_000, 1),
                line(11, COVER, 200_000, 2), line(12, CHARGER, 500_000, 1)));
        // 1 phone backs one cover and one charger only
        assertThat(result.discountOf(11L)).isEqualByComparingTo("100000");
        assertThat(result.discountOf(12L)).isEqualByComparingTo("50000");
    }

    @Test
    void aUnitPlaysOneRole() {
        // a tablet is the main product of the cover combo and an add-on of the phone combo
        long tablet = 4;
        List<Combo> combos = List.of(
                combo(1, PHONE, Map.of(tablet, percent(10))),
                combo(2, tablet, Map.of(COVER, percent(50))));
        Result result = ComboCalculator.evaluate(combos, List.of(line(10, PHONE, 15_000_000, 1),
                line(11, tablet, 10_000_000, 1), line(12, COVER, 200_000, 1)));
        // the one tablet cannot be both the add-on of the phone and the main product backing the cover
        assertThat(result.total()).isEqualByComparingTo("1000000");
        assertThat(result.discountOf(12L)).isEqualByComparingTo("0");
    }

    @Test
    void theDearestUnitsAreDiscountedFirst() {
        List<Combo> combos = List.of(combo(1, PHONE, Map.of(COVER, percent(50))));
        Result result = ComboCalculator.evaluate(combos, List.of(line(10, PHONE, 15_000_000, 1),
                line(11, COVER, 100_000, 1), line(12, COVER, 300_000, 1)));
        assertThat(result.discountOf(12L)).isEqualByComparingTo("150000");
        assertThat(result.discountOf(11L)).isEqualByComparingTo("0");
    }
}
