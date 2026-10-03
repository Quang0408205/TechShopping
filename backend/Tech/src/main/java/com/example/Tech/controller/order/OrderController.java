package com.example.Tech.controller.order;

import com.example.Tech.dto.request.order.OrderCreateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.service.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The logged-in customer's orders. Requires a valid access token (the /api/v1/** rule of SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Checkout from the server cart and the logged-in customer's orders")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED")
})
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place an order from my cart (items, prices and shipping come from the server cart)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed, cart emptied"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (incl. installment data missing / not "
                    + "allowed / bad term), MALFORMED_REQUEST (unknown payment method or bank), INSTALLMENT_NOT_ELIGIBLE"),
            @ApiResponse(responseCode = "409", description = "CART_EMPTY (also a double submit), PRODUCT_NOT_AVAILABLE")
    })
    public ResponseEntity<ApiResult<OrderResponse>> placeOrder(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                               @Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(orderService.placeOrder(userId(jwt), request)));
    }

    @GetMapping
    @Operation(summary = "My orders, newest first")
    @ApiResponse(responseCode = "200", description = "Page of orders with their items")
    public ResponseEntity<ApiResult<PageResponse<OrderResponse>>> getMyOrders(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(orderService.getMyOrders(userId(jwt), pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One of my orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order returned"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND (also another user's order)")
    })
    public ResponseEntity<ApiResult<OrderResponse>> getMyOrder(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(orderService.getMyOrder(userId(jwt), id)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel one of my orders while it is still PENDING")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_ORDER_STATUS")
    })
    public ResponseEntity<ApiResult<OrderResponse>> cancelMyOrder(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                  @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(orderService.cancelMyOrder(userId(jwt), id)));
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
