package com.example.Tech.entity.aftersales;

/** Status of a warranty or maintenance request (DB CHECK on both tables). */
public enum ServiceRequestStatus {
    PENDING,
    RECEIVED,
    PROCESSING,
    COMPLETED,
    REJECTED,
    CANCELLED;

    /** Staff transitions; CANCELLED is the customer's own action (see {@link #canBeCancelledByCustomer}). */
    public boolean canMoveTo(ServiceRequestStatus next) {
        return switch (this) {
            case PENDING -> next == RECEIVED || next == REJECTED;
            case RECEIVED -> next == PROCESSING || next == REJECTED;
            case PROCESSING -> next == COMPLETED || next == REJECTED;
            case COMPLETED, REJECTED, CANCELLED -> false;
        };
    }

    public boolean isOpen() {
        return this == PENDING || this == RECEIVED || this == PROCESSING;
    }

    public boolean canBeCancelledByCustomer() {
        return this == PENDING;
    }
}
