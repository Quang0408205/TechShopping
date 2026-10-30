package com.example.Tech.service.payment.installment;

import com.example.Tech.entity.payment.InstallmentOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulated bank that turns the purchase into an installment on the customer's credit card. It decides by the last
 * digit of the citizen id so a demo can show both answers: 0 or 9 is turned down, anything else is approved.
 */
@Slf4j
@Component
public class CreditCardInstallmentProvider implements InstallmentProvider {

    public static final String CODE = "CREDIT_CARD";

    @Override
    public boolean supports(String partnerCode) {
        return CODE.equals(partnerCode);
    }

    @Override
    public String submit(InstallmentOrder application) {
        return "CC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    @Override
    public void cancel(InstallmentOrder application) {
        log.info("Credit card partner: application {} cancelled", application.getPartnerRef());
    }

    @Override
    public Decision review(InstallmentOrder application) {
        char last = lastDigit(application);
        if (last == '0' || last == '9') {
            return Decision.reject("Ngân hàng phát hành thẻ không chấp thuận trả góp cho thẻ này");
        }
        return Decision.approve();
    }

    static char lastDigit(InstallmentOrder application) {
        String id = application.getCitizenId();
        return id == null || id.isEmpty() ? '0' : id.charAt(id.length() - 1);
    }
}
