package com.example.Tech.controller.order;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.service.order.ShippingFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** The fee checkout would charge, so the page can show it as soon as the province is chosen (public). */
@RestController
@RequestMapping("/api/v1/shipping-fee")
@RequiredArgsConstructor
@Tag(name = "Shipping fee", description = "Fee of a delivery to a province (public)")
public class ShippingFeeController {

    private final ShippingFeeService shippingFeeService;

    public record FeeResponse(BigDecimal fee) {
    }

    @GetMapping
    @Operation(summary = "Shipping fee for goods worth subtotal, to a province; pickup and an empty order are free")
    public ResponseEntity<ApiResult<FeeResponse>> fee(
            @RequestParam(required = false) String province,
            @RequestParam(defaultValue = "0") BigDecimal subtotal,
            @RequestParam(defaultValue = "HOME_DELIVERY") DeliveryType deliveryType) {
        return ResponseEntity.ok(ApiResult.ok(new FeeResponse(shippingFeeService.feeFor(subtotal, deliveryType, province))));
    }
}
