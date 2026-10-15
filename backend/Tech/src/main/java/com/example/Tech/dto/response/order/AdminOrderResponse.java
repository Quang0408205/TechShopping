package com.example.Tech.dto.response.order;

/**
 * An order as staff see it: the customer's view of the order plus who placed it.
 */
public record AdminOrderResponse(
        OrderResponse order,
        Customer customer
) {

    /** The account that placed the order (the recipient may be someone else). */
    public record Customer(
            Long id,
            String username,
            String fullname,
            String email,
            String phone,
            boolean deleted
    ) {
    }
}
