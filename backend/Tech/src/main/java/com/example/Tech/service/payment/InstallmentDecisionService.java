package com.example.Tech.service.payment;

/**
 * Where an installment partner's answer is applied to the order: the webhook, the simulated partner's own timer and
 * the partner console of STAFF / ADMIN all end here.
 */
public interface InstallmentDecisionService {

    /**
     * Applies the partner's answer to the application found by the reference the partner was given.
     * A repeated event id changes nothing and returns false.
     */
    boolean applyByPartnerRef(String partnerRef, boolean approved, String reason, String eventId);

    /** The simulated partner reviews the application of the order by its own rules and answers. */
    void decideAsPartner(Long orderId);
}
