package com.example.Tech.controller.product;

import com.example.Tech.dto.request.product.CategoryCreateRequest;
import com.example.Tech.dto.request.product.CategoryUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.CategoryResponse;
import com.example.Tech.service.product.CategoryService;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category management")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List categories with pagination (default sort: displayOrder, id)")
    @ApiResponse(responseCode = "200", description = "Categories returned")
    public ResponseEntity<ApiResult<PageResponse<CategoryResponse>>> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = {"displayOrder", "id"}) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(categoryService.getAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<CategoryResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResult.ok(categoryService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a category")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "Parent CATEGORY_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_CATEGORY")
    })
    public ResponseEntity<ApiResult<CategoryResponse>> create(@Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / INVALID_CATEGORY_PARENT"),
            @ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_CATEGORY")
    })
    public ResponseEntity<ApiResult<CategoryResponse>> update(@PathVariable Integer id,
                                                              @Valid @RequestBody CategoryUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(categoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted"),
            @ApiResponse(responseCode = "404", description = "CATEGORY_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "RESOURCE_IN_USE")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
