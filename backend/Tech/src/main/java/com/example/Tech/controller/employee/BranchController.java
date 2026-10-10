package com.example.Tech.controller.employee;

import com.example.Tech.dto.request.employee.BranchEmployeeUpdateRequest;
import com.example.Tech.dto.request.employee.EmployeeSearchRequest;
import com.example.Tech.dto.request.employee.HireRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.employee.BranchResponse;
import com.example.Tech.dto.response.employee.EmployeeResponse;
import com.example.Tech.dto.response.employee.HireResponse;
import com.example.Tech.service.employee.BranchService;
import com.example.Tech.service.employee.EmployeeHiringService;
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
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A branch manager's own branch: the staff list, hiring STAFF and editing / releasing them. The branch is always
 * the manager's current assignment; no endpoint takes a store id.
 */
@RestController
@RequestMapping("/api/v1/branch")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BRANCH_MANAGER')")
@Tag(name = "Branch", description = "Own-branch management (BRANCH_MANAGER only)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED, NO_ACTIVE_STORE_ASSIGNMENT")
})
public class BranchController {

    private final BranchService branchService;

    private final EmployeeHiringService hiringService;

    @GetMapping("/me")
    @Operation(summary = "My branch and how many employees work there")
    public ResponseEntity<ApiResult<BranchResponse>> me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(branchService.me(userId(jwt))));
    }

    @GetMapping("/employees")
    @Operation(summary = "Employees of my branch, working and gone (storeId in the filter is ignored)")
    public ResponseEntity<ApiResult<PageResponse<EmployeeResponse>>> employees(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject EmployeeSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(branchService.employees(userId(jwt), filter, pageable)));
    }

    @PostMapping("/employees/hire")
    @Operation(summary = "Hire a STAFF member into my branch; the temporary password is returned once")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hired"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, ROLE_NOT_ASSIGNABLE (managers hire "
                    + "STAFF only)"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_EMAIL, DUPLICATE_USERNAME, "
                    + "DUPLICATE_EMPLOYEE_CODE")
    })
    public ResponseEntity<ApiResult<HireResponse>> hire(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody HireRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ApiResult.ok(hiringService.hireAsManager(userId(jwt), request)));
    }

    @PutMapping("/employees/{id}")
    @Operation(summary = "Edit the profile of a STAFF member of my branch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (myself, another manager or another branch)"),
            @ApiResponse(responseCode = "404", description = "EMPLOYEE_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_EMPLOYEE_CODE")
    })
    public ResponseEntity<ApiResult<EmployeeResponse>> update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable Long id,
                                                              @Valid @RequestBody BranchEmployeeUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(branchService.updateEmployee(userId(jwt), id, request)));
    }

    @PostMapping("/employees/{id}/deactivate")
    @Operation(summary = "Let a STAFF member of my branch go: ends the assignment and locks the account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Done"),
            @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (myself, another manager or another branch)"),
            @ApiResponse(responseCode = "404", description = "EMPLOYEE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<EmployeeResponse>> deactivate(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(branchService.deactivateEmployee(userId(jwt), id)));
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
