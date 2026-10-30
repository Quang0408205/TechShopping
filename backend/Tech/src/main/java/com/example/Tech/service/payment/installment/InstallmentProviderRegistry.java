package com.example.Tech.service.payment.installment;

import org.springframework.stereotype.Component;

import java.util.List;

/** Finds the gateway of a partner by its code. */
@Component
public class InstallmentProviderRegistry {

    private final List<InstallmentProvider> providers;

    public InstallmentProviderRegistry(List<InstallmentProvider> providers) {
        this.providers = providers;
    }

    public InstallmentProvider forPartner(String partnerCode) {
        return providers.stream()
                .filter(provider -> provider.supports(partnerCode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No installment provider for partner " + partnerCode));
    }
}
