package com.example.Tech.controller.product;

import com.example.Tech.dto.request.product.AttributeCreateRequest;
import com.example.Tech.dto.request.product.AttributeUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.AttributeResponse;
import com.example.Tech.dto.response.product.AttributeValueResponse;
import com.example.Tech.service.product.AttributeService;
import com.example.Tech.service.product.AttributeValueService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/attributes")
@RequiredArgsConstructor
@Tag(name = "Attributes", description = "Product attribute management")
public class AttributeController {

    private final AttributeService attributeService;
    private final AttributeValueService attributeValueService;

    @GetMapping
    @Operation(summary = "List attributes with pagination (default sort: name)")
    @ApiResponse(responseCode = "200", description = "Attributes returned")
    public ResponseEntity<ApiResult<PageResponse<AttributeResponse>>> getAll(
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(attributeService.getAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an attribute by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attribute found"),
            @ApiResponse(responseCode = "404", description = "ATTRIBUTE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<AttributeResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResult.ok(attributeService.getById(id)));
    }

    @GetMapping("/{id}/values")
    @Operation(summary = "List values of an attribute")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Values returned"),
            @ApiResponse(responseCode = "404", description = "ATTRIBUTE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<List<AttributeValueResponse>>> getValues(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResult.ok(attributeValueService.getByAttributeId(id)));
    }

    @PostMapping
    @Operation(summary = "Create an attribute")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attribute created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_ATTRIBUTE")
    })
    public ResponseEntity<ApiResult<AttributeResponse>> create(@Valid @RequestBody AttributeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(attributeService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an attribute")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attribute updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "ATTRIBUTE_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_ATTRIBUTE")
    })
    public ResponseEntity<ApiResult<AttributeResponse>> update(@PathVariable Integer id,
                                                               @Valid @RequestBody AttributeUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(attributeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an attribute (only when it has no values)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Attribute deleted"),
            @ApiResponse(responseCode = "404", description = "ATTRIBUTE_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "RESOURCE_IN_USE")
    })
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        attributeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
