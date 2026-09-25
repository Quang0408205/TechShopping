package com.example.Tech.controller.product;

import com.example.Tech.dto.request.product.ProductImageCreateRequest;
import com.example.Tech.dto.request.product.ProductImageUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.product.ProductImageResponse;
import com.example.Tech.service.product.ProductImageService;
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
@Tag(name = "Product Images", description = "Product image management")
public class ProductImageController {

    private final ProductImageService imageService;

    @GetMapping("/products/{productId}/images")
    @Operation(summary = "List images of a product (ordered by display order)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Images returned"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<List<ProductImageResponse>>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResult.ok(imageService.getByProductId(productId)));
    }

    @GetMapping("/product-images/{id}")
    @Operation(summary = "Get a product image by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image found"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_IMAGE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ProductImageResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(imageService.getById(id)));
    }

    @PostMapping("/product-images")
    @Operation(summary = "Add an image to a product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Image created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ProductImageResponse>> create(
            @Valid @RequestBody ProductImageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(imageService.create(request)));
    }

    @PutMapping("/product-images/{id}")
    @Operation(summary = "Update a product image (the owning product cannot be changed)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_IMAGE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ProductImageResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ProductImageUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(imageService.update(id, request)));
    }

    @DeleteMapping("/product-images/{id}")
    @Operation(summary = "Delete a product image")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Image deleted"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_IMAGE_NOT_FOUND")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        imageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
