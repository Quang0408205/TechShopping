package com.example.Tech.dto.response.payment;

/** What the reconciliation page needs to know about the setup (the full account number is never sent). */
public record BankReconciliationInfoResponse(
        boolean webhookConfigured,
        boolean simulatorEnabled,
        boolean demoAccount,
        String bankName,
        String accountNumberMasked,
        long needsAttention
) {
}
