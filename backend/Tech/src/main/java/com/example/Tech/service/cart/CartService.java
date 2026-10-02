package com.example.Tech.service.cart;

import com.example.Tech.dto.request.cart.CartItemAddRequest;
import com.example.Tech.dto.request.cart.CartItemUpdateRequest;
import com.example.Tech.dto.response.cart.CartResponse;

/**
 * The logged-in user's cart (the id comes from the access token). Every operation re-checks the account
 * and returns the whole cart, with prices read from the variants at that moment.
 */
public interface CartService {

    /** Maximum quantity of one line. */
    int MAX_LINE_QUANTITY = 10;

    /** Maximum number of different variants in one cart. */
    int MAX_LINES = 50;

    /** An empty cart when the user has none yet (nothing is created). */
    CartResponse getCart(Long userId);

    /** Creates the cart if needed; adds to an existing line, capped at MAX_LINE_QUANTITY. */
    CartResponse addItem(Long userId, CartItemAddRequest request);

    CartResponse updateItem(Long userId, Long variantId, CartItemUpdateRequest request);

    CartResponse removeItem(Long userId, Long variantId);

    CartResponse clear(Long userId);
}
