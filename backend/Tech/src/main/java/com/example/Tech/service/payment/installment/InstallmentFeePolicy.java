package com.example.Tech.service.payment.installment;

import com.example.Tech.repository.payment.InstallmentFeeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * The fee a partner keeps from the shop. The customer pays 0% interest; the partner pays the shop the order total
 * minus this fee (percent of the total, rounded to the whole đồng).
 */
@Component
@RequiredArgsConstructor
public class InstallmentFeePolicy {

    private final InstallmentFeeRateRepository feeRates;

    public Optional<BigDecimal> percent(String partnerCode, int months) {
        return feeRates.findOne(partnerCode, months).map(rate -> rate.getFeePercent());
    }

    public Fee fee(String partnerCode, int months, BigDecimal orderTotal) {
        BigDecimal percent = percent(partnerCode, months)
                .orElseThrow(() -> new IllegalStateException("No fee rate for %s / %d months".formatted(partnerCode, months)));
        return fee(percent, orderTotal);
    }

    public static Fee fee(BigDecimal percent, BigDecimal orderTotal) {
        BigDecimal amount = orderTotal.multiply(percent).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return new Fee(percent, amount, orderTotal.subtract(amount));
    }

    /** {@code net} is what the partner pays the shop. */
    public record Fee(BigDecimal percent, BigDecimal amount, BigDecimal net) {
    }
}
