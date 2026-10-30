package com.example.Tech.controller.voucher;

import com.example.Tech.dto.request.voucher.VoucherRequest;
import com.example.Tech.dto.request.voucher.VoucherSearchRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.voucher.VoucherResponse;
import com.example.Tech.dto.response.voucher.VoucherUsageResponse;
import com.example.Tech.service.voucher.AdminVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Voucher management (ADMIN only: URL rule + @PreAuthorize + a database re-check). */
@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Vouchers", description = "Order vouchers (ADMIN only)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
})
public class AdminVoucherController {

    private final AdminVoucherService adminVoucherService;

    public record StatusRequest(@NotNull(message = "active is required") Boolean active) {
    }

    @GetMapping
    @Operation(summary = "Search vouchers (newest first by default)")
    public ResponseEntity<ApiResult<PageResponse<VoucherResponse>>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject VoucherSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(adminVoucherService.search(adminId(jwt), filter, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a voucher")
    @ApiResponse(responseCode = "404", description = "VOUCHER_NOT_FOUND")
    public ResponseEntity<ApiResult<VoucherResponse>> getById(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(adminVoucherService.getById(adminId(jwt), id)));
    }

    @PostMapping
    @Operation(summary = "Create a voucher")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Voucher created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (code format, endsAt, categoryIds…)"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_VOUCHER")
    })
    public ResponseEntity<ApiResult<VoucherResponse>> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(adminVoucherService.create(adminId(jwt), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a voucher (orders already placed keep their discount)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (incl. usageLimit below the uses)"),
            @ApiResponse(responseCode = "404", description = "VOUCHER_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_VOUCHER, VOUCHER_IN_USE (code change after use)")
    })
    public ResponseEntity<ApiResult<VoucherResponse>> update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                             @PathVariable Long id, @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminVoucherService.update(adminId(jwt), id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Switch a voucher on / off")
    public ResponseEntity<ApiResult<VoucherResponse>> setActive(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminVoucherService.setActive(adminId(jwt), id, request.active())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a voucher no order used")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "409", description = "VOUCHER_IN_USE (switch it off instead)")
    })
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        adminVoucherService.delete(adminId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/usages")
    @Operation(summary = "Orders that used the voucher, newest first")
    public ResponseEntity<ApiResult<PageResponse<VoucherUsageResponse>>> usages(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @ParameterObject @PageableDefault(size = 20, sort = "usedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(adminVoucherService.usages(adminId(jwt), id, pageable)));
    }

    private static Long adminId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
