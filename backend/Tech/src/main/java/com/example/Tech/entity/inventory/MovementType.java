package com.example.Tech.entity.inventory;

/**
 * Kind of change recorded in {@code stock_movements}. IN = stock-in (nhập kho, supplier name free
 * text); OUT = deducted when an order is CONFIRMED; RETURN = restored when a CONFIRMED order is
 * cancelled; ADJUSTMENT = reserved for a future manual correction (not produced by this phase).
 */
public enum MovementType {
    IN,
    OUT,
    ADJUSTMENT,
    RETURN
}
