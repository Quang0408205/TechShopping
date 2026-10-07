package com.example.Tech.dto.response.store;

/**
 * An open store as shown to customers (the "nhận tại chi nhánh" picker at checkout).
 */
public record PublicStoreResponse(
        Integer id,
        String name,
        String address,
        String district,
        String city,
        String phone
) {
}
