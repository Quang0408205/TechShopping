package com.example.Tech.entity.order;

/**
 * How the customer receives the order (orders.delivery_type). HOME_DELIVERY: shipped from the store nearest to
 * the shipping address; PICKUP: collected at the store the customer chose, no shipping fee.
 */
public enum DeliveryType {
    HOME_DELIVERY,
    PICKUP
}
