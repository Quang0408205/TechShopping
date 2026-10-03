package com.example.Tech.controller.cart;

import com.example.Tech.dto.request.cart.CartItemAddRequest;
import com.example.Tech.dto.request.cart.CartItemUpdateRequest;
import com.example.Tech.dto.response.cart.CartResponse;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.service.cart.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The logged-in user's cart. Requires a valid access token (see SecurityConfig). Lines are addressed by
 * variant id (UNIQUE per cart), and every call returns the whole cart.
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "The logged-in user's shopping cart; prices are read from the variants")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED")
})
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get my cart (empty when I have none yet)")
    @ApiResponse(responseCode = "200", description = "Cart returned")
    public ResponseEntity<ApiResult<CartResponse>> getCart(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(cartService.getCart(userId(jwt))));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a variant; the quantity is added to an existing line, capped at "
            + CartService.MAX_LINE_QUANTITY)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added, cart returned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_VARIANT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "PRODUCT_NOT_AVAILABLE, CART_LIMIT_EXCEEDED")
    })
    public ResponseEntity<ApiResult<CartResponse>> addItem(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody CartItemAddRequest request) {
        return ResponseEntity.ok(ApiResult.ok(cartService.addItem(userId(jwt), request)));
    }

    @PutMapping("/items/{variantId}")
    @Operation(summary = "Set the quantity of a line")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantity changed, cart returned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "CART_ITEM_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<CartResponse>> updateItem(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable Long variantId,
                                                              @Valid @RequestBody CartItemUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(cartService.updateItem(userId(jwt), variantId, request)));
    }

    @DeleteMapping("/items/{variantId}")
    @Operation(summary = "Remove a line")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Line removed, cart returned"),
            @ApiResponse(responseCode = "404", description = "CART_ITEM_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<CartResponse>> removeItem(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResult.ok(cartService.removeItem(userId(jwt), variantId)));
    }

    @DeleteMapping
    @Operation(summary = "Remove every line")
    @ApiResponse(responseCode = "200", description = "Cart emptied, cart returned")
    public ResponseEntity<ApiResult<CartResponse>> clear(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(cartService.clear(userId(jwt))));
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
