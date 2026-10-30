package com.example.Tech.service.payment.installment;

import com.example.Tech.entity.payment.InstallmentOrder;

/**
 * The shop's gateway to one installment partner. The shop sends the application ({@link #submit}), the partner answers
 * later through the installment webhook, and the shop tells it when the order is cancelled ({@link #cancel}).
 * Both providers here simulate the partner; a real partner replaces them by implementing this interface.
 */
public interface InstallmentProvider {

    boolean supports(String partnerCode);

    /** Sends the application to the partner and returns the reference it gave to it. */
    String submit(InstallmentOrder application);

    /** Tells the partner to drop the application (order cancelled): it stops collecting from the customer. */
    void cancel(InstallmentOrder application);

    /** What the simulated partner decides about the application. */
    Decision review(InstallmentOrder application);

    record Decision(boolean approved, String reason) {

        public static Decision approve() {
            return new Decision(true, null);
        }

        public static Decision reject(String reason) {
            return new Decision(false, reason);
        }
    }
}
