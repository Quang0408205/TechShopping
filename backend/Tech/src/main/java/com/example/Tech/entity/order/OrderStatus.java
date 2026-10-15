package com.example.Tech.entity.order;

/**
 * Order status, stored as upper-case text in orders.status (the DB default 'pending' is never used: the
 * application always sets the status).
 * Flow: PENDING → CONFIRMED → SHIPPING → DELIVERED; CANCELLED from PENDING or CONFIRMED.
 * Returns are Phase 6.
 */
public enum OrderStatus {

    PENDING,
    CONFIRMED,
    SHIPPING,
    DELIVERED,
    CANCELLED;

    /** Transitions staff may make. A customer may only cancel a PENDING order ({@link #canBeCancelledByCustomer}). */
    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == SHIPPING || next == CANCELLED;
            case SHIPPING -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    public boolean canBeCancelledByCustomer() {
        return this == PENDING;
    }
}
