package com.example.Tech.entity.recommendation;

/**
 * Behaviour recorded in user_interactions (DB CHECK chk_user_interactions_type). The weight is w_t of the
 * interaction score r(u,i) = Σ w_t · log(1 + n(u,i,t)) (report section 3.5.3 i, table 3.19); it is stored in
 * interaction_score so the AI service and reports read the same value.
 */
public enum InteractionType {
    VIEW(1),
    CLICK(2),
    LIKE(3),
    ADD_TO_CART(4),
    PURCHASE(5);

    private final int weight;

    InteractionType(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    /** Only these may be sent by the browser; ADD_TO_CART and PURCHASE are recorded by the cart and order services. */
    public boolean clientReportable() {
        return this == VIEW || this == CLICK;
    }
}
