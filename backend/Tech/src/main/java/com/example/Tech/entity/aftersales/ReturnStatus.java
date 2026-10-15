package com.example.Tech.entity.aftersales;

/** Status of a return (refund) request (DB CHECK chk_return_requests_status). */
public enum ReturnStatus {
    PENDING,
    APPROVED,
    RECEIVED,
    REFUNDED,
    REJECTED,
    CANCELLED;

    /** Staff transitions; CANCELLED is the customer's own action (see {@link #canBeCancelledByCustomer}). */
    public boolean canMoveTo(ReturnStatus next) {
        return switch (this) {
            case PENDING -> next == APPROVED || next == REJECTED;
            case APPROVED -> next == RECEIVED || next == REJECTED;
            case RECEIVED -> next == REFUNDED;
            case REFUNDED, REJECTED, CANCELLED -> false;
        };
    }

    /** Quantities of an open or refunded return can no longer be returned again. */
    public boolean holdsQuantity() {
        return this != REJECTED && this != CANCELLED;
    }

    public boolean canBeCancelledByCustomer() {
        return this == PENDING;
    }
}
