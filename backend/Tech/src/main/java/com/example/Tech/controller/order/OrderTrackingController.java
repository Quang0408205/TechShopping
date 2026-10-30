package com.example.Tech.controller.order;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.order.OrderTrackingResponse;
import com.example.Tech.service.order.OrderTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/track")
@RequiredArgsConstructor
@Tag(name = "Order tracking", description = "Public order progress page, opened from the link in the order emails")
public class OrderTrackingController {

    private final OrderTrackingService trackingService;

    @GetMapping("/{token}")
    @Operation(summary = "Progress of an order by the random token of its email link (no login)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progress returned (no phone, no street address)"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND (unknown or malformed token)")
    })
    public ResponseEntity<ApiResult<OrderTrackingResponse>> track(@PathVariable String token) {
        return ResponseEntity.ok(ApiResult.ok(trackingService.track(token)));
    }
}
