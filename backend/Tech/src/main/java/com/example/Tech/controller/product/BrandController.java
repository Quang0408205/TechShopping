package com.example.Tech.controller.product;

import com.example.Tech.dto.request.product.BrandCreateRequest;
import com.example.Tech.dto.request.product.BrandUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.BrandResponse;
import com.example.Tech.service.product.BrandService;
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
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
@Tag(name = "Brands", description = "Brand management")
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    @Operation(summary = "List brands with pagination (default sort: name)")
    @ApiResponse(responseCode = "200", description = "Brands returned")
    public ResponseEntity<ApiResult<PageResponse<BrandResponse>>> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(brandService.getAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a brand by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brand found"),
            @ApiResponse(responseCode = "404", description = "BRAND_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<BrandResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResult.ok(brandService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a brand")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Brand created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_BRAND")
    })
    public ResponseEntity<ApiResult<BrandResponse>> create(@Valid @RequestBody BrandCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(brandService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a brand")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brand updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "BRAND_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_BRAND")
    })
    public ResponseEntity<ApiResult<BrandResponse>> update(@PathVariable Integer id,
                                                           @Valid @RequestBody BrandUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(brandService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a brand")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Brand deleted"),
            @ApiResponse(responseCode = "404", description = "BRAND_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "RESOURCE_IN_USE")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
