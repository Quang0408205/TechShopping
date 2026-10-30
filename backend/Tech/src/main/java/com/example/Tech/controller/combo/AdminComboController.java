package com.example.Tech.controller.combo;

import com.example.Tech.dto.request.combo.ComboRequest;
import com.example.Tech.dto.response.combo.ComboResponse;
import com.example.Tech.dto.response.combo.ComboSuggestionResponse;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.service.combo.AdminComboService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Combo management (ADMIN only: URL rule + @PreAuthorize + a database re-check). */
@RestController
@RequestMapping("/api/v1/admin/combos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Combos", description = "Buy-together combos (ADMIN only)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
})
public class AdminComboController {

    private final AdminComboService adminComboService;

    public record StatusRequest(@NotNull(message = "active is required") Boolean active) {
    }

    @GetMapping
    @Operation(summary = "Search combos (newest first by default); status = PAUSED, UPCOMING, RUNNING or ENDED")
    public ResponseEntity<ApiResult<PageResponse<ComboResponse>>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String status,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(adminComboService.search(adminId(jwt), keyword, status, pageable)));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Add-on ideas for a main product from the AI 'bought together' relations")
    public ResponseEntity<ApiResult<List<ComboSuggestionResponse>>> suggestions(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @RequestParam Long productId,
            @RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(ApiResult.ok(adminComboService.suggestions(adminId(jwt), productId, Math.max(1, Math.min(limit, 30)))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a combo")
    @ApiResponse(responseCode = "404", description = "COMBO_NOT_FOUND")
    public ResponseEntity<ApiResult<ComboResponse>> getById(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(adminComboService.getById(adminId(jwt), id)));
    }

    @PostMapping
    @Operation(summary = "Create a combo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Combo created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (main product, add-ons, discounts, dates)")
    })
    public ResponseEntity<ApiResult<ComboResponse>> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody ComboRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(adminComboService.create(adminId(jwt), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a combo (orders already placed keep their discount)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "COMBO_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ComboResponse>> update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable Long id, @Valid @RequestBody ComboRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminComboService.update(adminId(jwt), id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Switch a combo on / off")
    public ResponseEntity<ApiResult<ComboResponse>> setActive(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminComboService.setActive(adminId(jwt), id, request.active())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a combo no order used")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "409", description = "COMBO_IN_USE (switch it off instead)")
    })
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        adminComboService.delete(adminId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    private static Long adminId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
