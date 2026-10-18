package com.example.Tech.controller.order;

import com.example.Tech.dto.request.order.OrderCreateRequest;
import com.example.Tech.dto.request.payment.CardCheckoutRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.order.OrderCountsResponse;
import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.dto.response.order.ReorderResponse;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.service.impl.order.OrderReorderService;
import com.example.Tech.dto.response.payment.BankWebhookResponse;
import com.example.Tech.dto.response.payment.CardCheckoutResponse;
import com.example.Tech.service.order.OrderService;
import com.example.Tech.service.payment.BankTransactionService;
import com.example.Tech.service.payment.CardPaymentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private final BankTransactionService bankTransactionService;
    private final CardPaymentService cardPaymentService;
    private final OrderReorderService orderReorderService;

    @PostMapping
    @Operation(summary = "Place an order from my cart (items, prices and shipping come from the server cart)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed, cart emptied"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (incl. installment data missing / not "
                    + "allowed / bad term), MALFORMED_REQUEST (unknown payment method or bank), INSTALLMENT_NOT_ELIGIBLE, "
                    + "VOUCHER_NOT_APPLICABLE, CARD_PAYMENT_DISABLED, CARD_AMOUNT_TOO_LARGE, VALIDATION_ERROR pointsToUse "
                    + "(more than the balance / than 10% of the goods)"),
            @ApiResponse(responseCode = "404", description = "VOUCHER_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "CART_EMPTY (also a double submit), PRODUCT_NOT_AVAILABLE, OUT_OF_STOCK (no open store holds the whole cart / the pickup store lacks items)")
    })
    public ResponseEntity<ApiResult<OrderResponse>> placeOrder(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                               @Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(orderService.placeOrder(userId(jwt), request)));
    }

    @GetMapping
    @Operation(summary = "My orders (purchase history), newest first",
            description = "Plan v5 GĐ6: optional status (repeat it for several, e.g. status=CONFIRMED&status=SHIPPING) "
                    + "and keyword (order code such as DH00000042, or a product name)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of orders with their items"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (keyword over 100 characters), MALFORMED_REQUEST (unknown status)")
    })
    public ResponseEntity<ApiResult<PageResponse<OrderResponse>>> getMyOrders(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) List<OrderStatus> status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(orderService.getMyOrders(userId(jwt), status, keyword, pageable)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Number of my orders per purchase-history tab (all / pending / in progress / delivered / cancelled)")
    public ResponseEntity<ApiResult<OrderCountsResponse>> countMyOrders(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(orderService.countMyOrders(userId(jwt))));
    }

    @PostMapping("/{id}/reorder")
    @Operation(summary = "Buy again: add the items of my delivered / cancelled order to my cart at today's prices",
            description = "Items no longer sold, out of stock or over the cart limits are skipped with a reason")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "added + skipped lines and the cart quantity"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND (also another user's order)"),
            @ApiResponse(responseCode = "409", description = "INVALID_ORDER_STATUS (order still in progress)")
    })
    public ResponseEntity<ApiResult<ReorderResponse>> reorder(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(orderReorderService.reorder(userId(jwt), id)));
    }

    @GetMapping("/awaiting-payment")
    @Operation(summary = "My newest bank-transfer / card order still waiting for its money (data null when none)",
            description = "The cart and checkout pages show it after the customer pressed Back on the payment page")
    @ApiResponse(responseCode = "200", description = "Order or null")
    public ResponseEntity<ApiResult<OrderResponse>> getAwaitingPayment(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(orderService.findAwaitingPayment(userId(jwt))));
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

    @PostMapping("/{id}/cancel-payment")
    @Operation(summary = "Cancel the payment: cancel my unpaid bank-transfer / card order and put its items back into my cart",
            description = "The voucher use is given back; an open Stripe page is closed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled, items back in the cart"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND (also another user's order)"),
            @ApiResponse(responseCode = "409", description = "INVALID_PAYMENT_STATUS (paid already, not PENDING, COD / installment)")
    })
    public ResponseEntity<ApiResult<OrderResponse>> cancelPayment(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                  @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(orderService.cancelPayment(userId(jwt), id)));
    }

    @PostMapping("/{id}/payment/card-checkout")
    @Operation(summary = "Open the Stripe Checkout page (test mode) of my card order",
            description = "Reuses the open Stripe page of the order; Stripe sends the customer back to returnUrl?id=…&card=paid|back")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "checkoutUrl to open, or paid = true"),
            @ApiResponse(responseCode = "400", description = "CARD_PAYMENT_DISABLED, VALIDATION_ERROR (returnUrl of another site)"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND (also another user's order)"),
            @ApiResponse(responseCode = "409", description = "INVALID_PAYMENT_STATUS (not a card order / no longer awaited)"),
            @ApiResponse(responseCode = "502", description = "PAYMENT_GATEWAY_ERROR (Stripe unreachable or refused)")
    })
    public ResponseEntity<ApiResult<CardCheckoutResponse>> startCardCheckout(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                             @PathVariable Long id,
                                                                             @Valid @RequestBody CardCheckoutRequest request) {
        return ResponseEntity.ok(ApiResult.ok(cardPaymentService.startCheckout(userId(jwt), id, request.returnUrl())));
    }

    @PostMapping("/{id}/payment/card-refresh")
    @Operation(summary = "Ask Stripe whether my card order is paid and record it (works without the webhook)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The order, payment PAID when Stripe has the money"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND (also another user's order)"),
            @ApiResponse(responseCode = "502", description = "PAYMENT_GATEWAY_ERROR")
    })
    public ResponseEntity<ApiResult<OrderResponse>> refreshCardPayment(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                       @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(cardPaymentService.refresh(userId(jwt), id)));
    }

    @PostMapping("/{id}/payment/demo-confirm")
    @Operation(summary = "Demo: take the bank transfer of my order as received",
            description = "Only when PAYMENT_SIMULATOR_ENABLED=true; the payment page calls it after showing the QR for a minute")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paid (or already paid / cancelled: duplicate = true)"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND (also another user's order), SIMULATOR_DISABLED")
    })
    public ResponseEntity<ApiResult<BankWebhookResponse>> demoConfirmTransfer(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                              @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(bankTransactionService.demoConfirm(userId(jwt), id)));
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
