package com.example.Tech.service.payment.installment;

import com.example.Tech.entity.payment.InstallmentOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Simulated finance companies (A and B): they lend by the citizen id alone. Company A turns down a citizen id ending
 * in 5 or 9, company B one ending in 9, so one customer can be refused by A and accepted by B.
 */
@Slf4j
@Component
public class FinanceInstallmentProvider implements InstallmentProvider {

    public static final String COMPANY_A = "FINANCE_A";
    public static final String COMPANY_B = "FINANCE_B";

    private static final Set<String> CODES = Set.of(COMPANY_A, COMPANY_B);

    @Override
    public boolean supports(String partnerCode) {
        return CODES.contains(partnerCode);
    }

    @Override
    public String submit(InstallmentOrder application) {
        String prefix = COMPANY_A.equals(application.getPartner().getCode()) ? "FA-" : "FB-";
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    @Override
    public void cancel(InstallmentOrder application) {
        log.info("Finance partner {}: application {} cancelled", application.getPartner().getCode(),
                application.getPartnerRef());
    }

    @Override
    public Decision review(InstallmentOrder application) {
        char last = CreditCardInstallmentProvider.lastDigit(application);
        boolean refused = last == '9' || (last == '5' && COMPANY_A.equals(application.getPartner().getCode()));
        return refused ? Decision.reject("Công ty tài chính không duyệt hồ sơ vay trả góp này") : Decision.approve();
    }
}
