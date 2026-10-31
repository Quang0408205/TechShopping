package com.example.Tech.controller.inventory;

import com.example.Tech.dto.request.inventory.SupplierRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.SupplierResponse;
import com.example.Tech.service.inventory.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Suppliers. Reading: STAFF, BRANCH_MANAGER, ADMIN (to fill a receipt); changing: ADMIN only. */
@RestController
@RequestMapping("/api/v1/admin/suppliers")
@RequiredArgsConstructor
@Tag(name = "Admin - Suppliers", description = "Suppliers of purchase receipts")
@ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
@ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'BRANCH_MANAGER', 'ADMIN')")
    @Operation(summary = "Suppliers by name; keyword matches name, phone or tax code")
    public ResponseEntity<ApiResult<PageResponse<SupplierResponse>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(supplierService.list(keyword, active, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'BRANCH_MANAGER', 'ADMIN')")
    @Operation(summary = "One supplier")
    @ApiResponse(responseCode = "404", description = "SUPPLIER_NOT_FOUND")
    public ResponseEntity<ApiResult<SupplierResponse>> get(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResult.ok(supplierService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a supplier (ADMIN)")
    @ApiResponse(responseCode = "409", description = "DUPLICATE_SUPPLIER")
    public ResponseEntity<ApiResult<SupplierResponse>> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(supplierService.create(Long.valueOf(jwt.getSubject()), request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edit a supplier, or stop working with it (active=false) (ADMIN)")
    @ApiResponse(responseCode = "404", description = "SUPPLIER_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "DUPLICATE_SUPPLIER")
    public ResponseEntity<ApiResult<SupplierResponse>> update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable Integer id,
                                                              @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(ApiResult.ok(supplierService.update(Long.valueOf(jwt.getSubject()), id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a supplier that never had a receipt (ADMIN)")
    @ApiResponse(responseCode = "404", description = "SUPPLIER_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "SUPPLIER_HAS_RECEIPTS")
    public ResponseEntity<ApiResult<Void>> delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable Integer id) {
        supplierService.delete(Long.valueOf(jwt.getSubject()), id);
        return ResponseEntity.ok(ApiResult.ok(null));
    }
}
