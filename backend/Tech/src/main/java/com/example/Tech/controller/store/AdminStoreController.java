package com.example.Tech.controller.store;

import com.example.Tech.dto.request.store.StoreRequest;
import com.example.Tech.dto.request.store.StoreSearchRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.store.StoreResponse;
import com.example.Tech.service.store.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Store (chi nhánh) management for administrators. Protected by URL (/api/v1/admin/** = ADMIN) and by
 * @PreAuthorize; the service also re-checks the caller's ADMIN role in the database.
 */
@RestController
@RequestMapping("/api/v1/admin/stores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Stores", description = "Store management (ADMIN only)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
})
public class AdminStoreController {

    private final StoreService storeService;

    @GetMapping
    @Operation(summary = "Search stores, by name by default")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of stores"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    })
    public ResponseEntity<ApiResult<PageResponse<StoreResponse>>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject StoreSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(storeService.search(adminId(jwt), filter, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a store")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store found"),
            @ApiResponse(responseCode = "404", description = "STORE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<StoreResponse>> getById(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResult.ok(storeService.getById(adminId(jwt), id)));
    }

    @PostMapping
    @Operation(summary = "Create a store")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Store created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    })
    public ResponseEntity<ApiResult<StoreResponse>> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody StoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(storeService.create(adminId(jwt), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a store's details; active = false closes it")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "STORE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<StoreResponse>> update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable Integer id,
                                                           @Valid @RequestBody StoreRequest request) {
        return ResponseEntity.ok(ApiResult.ok(storeService.update(adminId(jwt), id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a store that nothing references yet (otherwise close it instead)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Store deleted"),
            @ApiResponse(responseCode = "404", description = "STORE_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "STORE_IN_USE")
    })
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Integer id) {
        storeService.delete(adminId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long adminId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
