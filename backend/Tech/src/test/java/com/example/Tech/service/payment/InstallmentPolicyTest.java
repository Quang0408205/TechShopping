package com.example.Tech.service.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InstallmentPolicyTest {

    @Test
    void terms_onlyThreeSixNineTwelveMonths() {
        assertThat(InstallmentPolicy.isAllowedTerm(3)).isTrue();
        assertThat(InstallmentPolicy.isAllowedTerm(12)).isTrue();
        assertThat(InstallmentPolicy.isAllowedTerm(4)).isFalse();
        assertThat(InstallmentPolicy.isAllowedTerm(24)).isFalse();
        assertThat(InstallmentPolicy.isAllowedTerm(null)).isFalse();
    }

    @Test
    void eligible_fromThreeMillionIncluded() {
        assertThat(InstallmentPolicy.isEligible(new BigDecimal("3000000"))).isTrue();
        assertThat(InstallmentPolicy.isEligible(new BigDecimal("2999999.99"))).isFalse();
        assertThat(InstallmentPolicy.isEligible(null)).isFalse();
    }

    @Test
    void periodAmounts_roundDownToTheDong_lastPeriodTakesTheRemainder_andAddUpToTheTotal() {
        BigDecimal total = new BigDecimal("10000000.50");

        assertThat(InstallmentPolicy.monthlyPayment(total, 3)).isEqualByComparingTo("3333333");
        assertThat(InstallmentPolicy.lastPayment(total, 3)).isEqualByComparingTo("3333334.50");
        assertThat(InstallmentPolicy.periodAmounts(total, 3))
                .extracting(BigDecimal::toPlainString)
                .containsExactly("3333333", "3333333", "3333334.50");
        assertThat(InstallmentPolicy.periodAmounts(total, 3).stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(total);
        assertThat(InstallmentPolicy.periodAmounts(new BigDecimal("12000000"), 12)).hasSize(12)
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("1000000"));
    }

    @Test
    void dueDate_isDeliveryPlusNMonths_clampedToTheEndOfShortMonths() {
        LocalDate delivered = LocalDate.of(2027, 1, 31);

        assertThat(InstallmentPolicy.dueDate(delivered, 1)).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(InstallmentPolicy.dueDate(delivered, 2)).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(InstallmentPolicy.dueDate(LocalDate.of(2026, 10, 3), 12)).isEqualTo(LocalDate.of(2027, 10, 3));
    }

    @Test
    void maskCitizenId_keepsTheLastFourDigits() {
        assertThat(InstallmentPolicy.maskCitizenId("0123456789")).isEqualTo("******6789");
        assertThat(InstallmentPolicy.maskCitizenId("1234")).isEqualTo("1234");
        assertThat(InstallmentPolicy.maskCitizenId(null)).isNull();
    }
}
