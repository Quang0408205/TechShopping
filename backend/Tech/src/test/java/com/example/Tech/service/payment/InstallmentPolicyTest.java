package com.example.Tech.service.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InstallmentPolicyTest {

    @Test
    void monthlyPayment_roundsDownToTheDong_theLastPeriodTakesTheRemainder() {
        BigDecimal total = new BigDecimal("10000000.50");

        assertThat(InstallmentPolicy.monthlyPayment(total, 3)).isEqualByComparingTo("3333333");
        assertThat(InstallmentPolicy.lastPayment(total, 3)).isEqualByComparingTo("3333334.50");
    }

    @Test
    void maskCitizenId_keepsTheLastFourDigits() {
        assertThat(InstallmentPolicy.maskCitizenId("0123456789")).isEqualTo("******6789");
        assertThat(InstallmentPolicy.maskCitizenId("1234")).isEqualTo("1234");
        assertThat(InstallmentPolicy.maskCitizenId(null)).isNull();
    }
}
