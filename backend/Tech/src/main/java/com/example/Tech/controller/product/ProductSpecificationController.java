package com.example.Tech.controller.product;

import com.example.Tech.dto.request.product.ProductSpecificationCreateRequest;
import com.example.Tech.dto.request.product.ProductSpecificationUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.product.ProductSpecificationResponse;
import com.example.Tech.service.product.ProductSpecificationService;
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
@Tag(name = "Product Specifications", description = "Product technical specification management")
public class ProductSpecificationController {

    private final ProductSpecificationService specificationService;

    @GetMapping("/products/{productId}/specifications")
    @Operation(summary = "List specifications of a product (ordered by spec order)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Specifications returned"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<List<ProductSpecificationResponse>>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResult.ok(specificationService.getByProductId(productId)));
    }

    @GetMapping("/product-specifications/{id}")
    @Operation(summary = "Get a product specification by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Specification found"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_SPECIFICATION_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ProductSpecificationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(specificationService.getById(id)));
    }

    @PostMapping("/product-specifications")
    @Operation(summary = "Add a specification to a product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Specification created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ProductSpecificationResponse>> create(
            @Valid @RequestBody ProductSpecificationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(specificationService.create(request)));
    }

    @PutMapping("/product-specifications/{id}")
    @Operation(summary = "Update a product specification (the owning product cannot be changed)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Specification updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_SPECIFICATION_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ProductSpecificationResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ProductSpecificationUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(specificationService.update(id, request)));
    }

    @DeleteMapping("/product-specifications/{id}")
    @Operation(summary = "Delete a product specification")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Specification deleted"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_SPECIFICATION_NOT_FOUND")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        specificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
