package com.example.Tech.entity.inventory;

/**
 * Kind of change recorded in {@code stock_movements}. IN = stock-in (nhập kho, supplier name free
 * text); OUT = deducted when an order is CONFIRMED; RETURN = restored when a CONFIRMED order is
 * cancelled; ADJUSTMENT = reserved for a future manual correction (not produced yet).
 */
public enum MovementType {
    IN,
    OUT,
    ADJUSTMENT,
    RETURN,
    /** A stock transfer takes the goods out of the sending store; a transfer cancelled in transit puts them back as TRANSFER_IN. */
    TRANSFER_OUT,
    TRANSFER_IN
}
