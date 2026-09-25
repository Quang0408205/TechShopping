package com.example.Tech.controller.product;

import com.example.Tech.dto.request.product.ProductCreateRequest;
import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.dto.request.product.ProductUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.ProductResponse;
import com.example.Tech.service.product.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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


@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search products with pagination (soft-deleted products are excluded)",
            description = "Filters are optional and combined with AND. Sort example: sort=basePrice,desc")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of products returned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / MALFORMED_REQUEST (e.g. unknown sort field)")
    })
    public ResponseEntity<ApiResult<PageResponse<ProductResponse>>> search(
            @Valid @ParameterObject ProductSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(productService.search(filter, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ProductResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(productService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / INVALID_PRODUCT_DATA"),
            @ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND / BRAND_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_PRODUCT")
    })
    public ResponseEntity<ApiResult<ProductResponse>> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(productService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / INVALID_PRODUCT_DATA"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND / CATEGORY_NOT_FOUND / BRAND_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_PRODUCT")
    })
    public ResponseEntity<ApiResult<ProductResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
