package com.example.Tech.controller.product;

import com.example.Tech.dto.request.product.AttributeValueCreateRequest;
import com.example.Tech.dto.request.product.AttributeValueUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.product.AttributeValueResponse;
import com.example.Tech.service.product.AttributeValueService;
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

@RestController
@RequestMapping("/api/v1/attribute-values")
@RequiredArgsConstructor
@Tag(name = "Attribute Values", description = "Attribute value management")
public class AttributeValueController {

    private final AttributeValueService attributeValueService;

    @GetMapping("/{id}")
    @Operation(summary = "Get an attribute value by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Value found"),
            @ApiResponse(responseCode = "404", description = "ATTRIBUTE_VALUE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<AttributeValueResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResult.ok(attributeValueService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create an attribute value")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Value created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "ATTRIBUTE_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_ATTRIBUTE_VALUE")
    })
    public ResponseEntity<ApiResult<AttributeValueResponse>> create(
            @Valid @RequestBody AttributeValueCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(attributeValueService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an attribute value (the owning attribute cannot be changed)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Value updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "ATTRIBUTE_VALUE_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_ATTRIBUTE_VALUE")
    })
    public ResponseEntity<ApiResult<AttributeValueResponse>> update(
            @PathVariable Integer id, @Valid @RequestBody AttributeValueUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(attributeValueService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an attribute value (its variant links are removed by the database)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Value deleted"),
            @ApiResponse(responseCode = "404", description = "ATTRIBUTE_VALUE_NOT_FOUND")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        attributeValueService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
