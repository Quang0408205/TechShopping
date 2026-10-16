package com.example.Tech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Shop bank account shown to customers who pay by bank transfer; the QR is built from it. The transfer is confirmed
 * by the bank webhook (or by staff by hand).
 * {@code bankBin} is the VietQR bank id used to build the QR image URL (970436 = Vietcombank).
 */
@ConfigurationProperties(prefix = "app.payment.bank-transfer")
public record BankTransferProperties(String bankName, String bankBin, String accountNumber, String accountName,
                                     Integer deadlineHours) {

    public BankTransferProperties {
        bankName = StringUtils.hasText(bankName) ? bankName.trim() : "Vietcombank";
        bankBin = StringUtils.hasText(bankBin) ? bankBin.trim() : "970436";
        accountNumber = StringUtils.hasText(accountNumber) ? accountNumber.trim() : DEMO_ACCOUNT_NUMBER;
        accountName = StringUtils.hasText(accountName) ? accountName.trim() : "CONG TY POY";
        deadlineHours = deadlineHours != null && deadlineHours > 0 ? deadlineHours : 24;
    }

    public static final String DEMO_ACCOUNT_NUMBER = "0123456789";

    public boolean isDemoAccount() {
        return DEMO_ACCOUNT_NUMBER.equals(accountNumber);
    }
}
