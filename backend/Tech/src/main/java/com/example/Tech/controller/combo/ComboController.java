package com.example.Tech.controller.combo;

import com.example.Tech.dto.response.combo.ComboOptionResponse;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.service.combo.ComboQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Plan v5 GĐ10, public: the combos running for a product ("Mua kèm tiết kiệm" on its page). */
@RestController
@RequestMapping("/api/v1/products/{productId}/combos")
@RequiredArgsConstructor
@Tag(name = "Combos", description = "Buy-together combos on a product page (public)")
public class ComboController {

    private final ComboQueryService comboQueryService;

    @GetMapping
    @Operation(summary = "Combos running now with this product as the main one (empty list when none)")
    @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    public ResponseEntity<ApiResult<List<ComboOptionResponse>>> forProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResult.ok(comboQueryService.forProduct(productId)));
    }
}
