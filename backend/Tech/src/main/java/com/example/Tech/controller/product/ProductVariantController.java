package com.example.Tech.controller.product;

import com.example.Tech.dto.request.product.ProductVariantCreateRequest;
import com.example.Tech.dto.request.product.ProductVariantUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.product.ProductVariantResponse;
import com.example.Tech.service.product.ProductVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Product Variants", description = "Product variant management")
public class ProductVariantController {

    private final ProductVariantService variantService;

    @GetMapping("/products/{productId}/variants")
    @Operation(summary = "List variants of a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variants returned"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<List<ProductVariantResponse>>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResult.ok(variantService.getByProductId(productId)));
    }

    @GetMapping("/product-variants/{id}")
    @Operation(summary = "Get a product variant by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variant found"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_VARIANT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ProductVariantResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(variantService.getById(id)));
    }

    @PostMapping("/product-variants")
    @Operation(summary = "Create a product variant")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Variant created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / INVALID_PRODUCT_VARIANT_DATA"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_PRODUCT_VARIANT")
    })
    public ResponseEntity<ApiResult<ProductVariantResponse>> create(
            @Valid @RequestBody ProductVariantCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(variantService.create(request)));
    }

    @PutMapping("/product-variants/{id}")
    @Operation(summary = "Update a product variant (the owning product cannot be changed)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variant updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / INVALID_PRODUCT_VARIANT_DATA"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_VARIANT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_PRODUCT_VARIANT")
    })
    public ResponseEntity<ApiResult<ProductVariantResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ProductVariantUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(variantService.update(id, request)));
    }

    @DeleteMapping("/product-variants/{id}")
    @Operation(summary = "Delete a product variant")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Variant deleted"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_VARIANT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DATA_INTEGRITY_VIOLATION (variant referenced by other data)")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        variantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/product-variants/{id}/attribute-values/{valueId}")
    @Operation(summary = "Assign an attribute value to a variant (one value per attribute)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Value assigned; returns the updated variant"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_VARIANT_NOT_FOUND / ATTRIBUTE_VALUE_NOT_FOUND"),
            @ApiResponse(responseCode = "409",
                    description = "DUPLICATE_VARIANT_ATTRIBUTE_VALUE / VARIANT_ATTRIBUTE_CONFLICT")
    })
    public ResponseEntity<ApiResult<ProductVariantResponse>> addAttributeValue(@PathVariable Long id,
                                                                               @PathVariable Integer valueId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(variantService.addAttributeValue(id, valueId)));
    }

    @DeleteMapping("/product-variants/{id}/attribute-values/{valueId}")
    @Operation(summary = "Remove an attribute value from a variant")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Value removed"),
            @ApiResponse(responseCode = "404",
                    description = "PRODUCT_VARIANT_NOT_FOUND / VARIANT_ATTRIBUTE_VALUE_NOT_FOUND")
    })
    public ResponseEntity<Void> removeAttributeValue(@PathVariable Long id, @PathVariable Integer valueId) {
        variantService.removeAttributeValue(id, valueId);
        return ResponseEntity.noContent().build();
    }
}
