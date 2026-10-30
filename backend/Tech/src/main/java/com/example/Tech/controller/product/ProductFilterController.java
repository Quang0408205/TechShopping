package com.example.Tech.controller.product;

import com.example.Tech.dto.request.product.ProductNeedsRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.product.FilterGroupResponse;
import com.example.Tech.dto.response.product.NeedResponse;
import com.example.Tech.service.product.ProductFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Product filters", description = "Spec filters and needs")
public class ProductFilterController {

    private final ProductFilterService filterService;

    @GetMapping("/categories/{categoryId}/filters")
    @Operation(summary = "Filter groups of a category with the number of products on sale per value",
            description = "Use as GET /products?categoryId=..&filter=group:value (same group = OR, different groups = AND) "
                    + "and need=slug (several needs = OR). Only values that have products are returned.")
    public ResponseEntity<ApiResult<List<FilterGroupResponse>>> filters(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(ApiResult.ok(filterService.getFilters(categoryId)));
    }

    @GetMapping("/categories/{categoryId}/needs")
    @Operation(summary = "Needs of a category (with the number of products on sale)")
    public ResponseEntity<ApiResult<List<NeedResponse>>> needs(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(ApiResult.ok(filterService.getNeeds(categoryId)));
    }

    @GetMapping("/products/{productId}/needs")
    @Operation(summary = "Needs of a product")
    public ResponseEntity<ApiResult<List<NeedResponse>>> productNeeds(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResult.ok(filterService.getProductNeeds(productId)));
    }

    @PutMapping("/products/{productId}/needs")
    @Operation(summary = "Replace the needs of a product (ADMIN)",
            description = "Each need must belong to the product's category (400 INVALID_PRODUCT_DATA otherwise)")
    public ResponseEntity<ApiResult<List<NeedResponse>>> setProductNeeds(@PathVariable Long productId,
                                                                         @Valid @RequestBody ProductNeedsRequest request) {
        return ResponseEntity.ok(ApiResult.ok(filterService.setProductNeeds(productId, request.needIds())));
    }

    @PostMapping("/admin/products/reindex-filters")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rebuild the filter values of every product (ADMIN); run after importing products")
    public ResponseEntity<ApiResult<Map<String, Integer>>> reindex() {
        return ResponseEntity.ok(ApiResult.ok(Map.of("products", filterService.reindexAll())));
    }
}
