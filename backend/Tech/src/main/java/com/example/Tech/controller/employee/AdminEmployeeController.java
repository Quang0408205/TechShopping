package com.example.Tech.controller.employee;

import com.example.Tech.dto.request.employee.AssignmentRequest;
import com.example.Tech.dto.request.employee.EmployeeCreateRequest;
import com.example.Tech.dto.request.employee.EmployeeSearchRequest;
import com.example.Tech.dto.request.employee.EmployeeUpdateRequest;
import com.example.Tech.dto.request.employee.HireRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.employee.EmployeeResponse;
import com.example.Tech.dto.response.employee.HireResponse;
import com.example.Tech.service.employee.EmployeeHiringService;
import com.example.Tech.service.employee.EmployeeService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Employee profiles and store assignments, for administrators. Protected by URL (/api/v1/admin/** = ADMIN) and
 * by @PreAuthorize; the service also re-checks the caller's ADMIN role in the database.
 */
@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Employees", description = "Employee profiles and store assignments (ADMIN only)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
})
public class AdminEmployeeController {

    private final EmployeeService employeeService;

    private final EmployeeHiringService hiringService;

    @GetMapping
    @Operation(summary = "Search employees, newest first by default")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of employees with their current store"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    })
    public ResponseEntity<ApiResult<PageResponse<EmployeeResponse>>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject EmployeeSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(employeeService.search(adminId(jwt), filter, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an employee")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "404", description = "EMPLOYEE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<EmployeeResponse>> getById(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(employeeService.getById(adminId(jwt), id)));
    }

    @PostMapping("/hire")
    @Operation(summary = "Hire a new employee: creates the account (STAFF or BRANCH_MANAGER), the profile and the "
            + "assignment; the temporary password is returned once")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hired; the response carries the temporary password"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, ROLE_NOT_ASSIGNABLE (only STAFF / "
                    + "BRANCH_MANAGER), closed store"),
            @ApiResponse(responseCode = "404", description = "STORE_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_EMAIL, DUPLICATE_USERNAME, "
                    + "DUPLICATE_EMPLOYEE_CODE, STORE_ALREADY_HAS_MANAGER")
    })
    public ResponseEntity<ApiResult<HireResponse>> hire(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody HireRequest request) {
        // the response holds a password: never cache it
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ApiResult.ok(hiringService.hireAsAdmin(adminId(jwt), request)));
    }

    @PostMapping
    @Deprecated
    @Operation(deprecated = true, summary = "Deprecated, use /hire. Create the employee profile of an existing "
            + "STAFF account (accounts that also have CUSTOMER are refused), optionally assigned to a store")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employee created"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (unknown / locked / non-STAFF account, "
                    + "closed store)"),
            @ApiResponse(responseCode = "404", description = "STORE_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "EMPLOYEE_ALREADY_EXISTS, DUPLICATE_EMPLOYEE_CODE, "
                    + "CUSTOMER_ACCOUNT_NOT_ELIGIBLE")
    })
    public ResponseEntity<ApiResult<EmployeeResponse>> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @Valid @RequestBody EmployeeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(employeeService.create(adminId(jwt), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an employee profile; active = false also ends the current assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "EMPLOYEE_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_EMPLOYEE_CODE")
    })
    public ResponseEntity<ApiResult<EmployeeResponse>> update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable Long id,
                                                              @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(employeeService.update(adminId(jwt), id, request)));
    }

    @PostMapping("/{id}/assignment")
    @Operation(summary = "Assign the employee to a store (ends the current assignment, keeps the history)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assigned"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (closed store, employee no longer working)"),
            @ApiResponse(responseCode = "404", description = "EMPLOYEE_NOT_FOUND, STORE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<EmployeeResponse>> assign(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable Long id,
                                                              @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(ApiResult.ok(employeeService.assign(adminId(jwt), id, request)));
    }

    @DeleteMapping("/{id}/assignment")
    @Operation(summary = "End the employee's current store assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "No current assignment any more"),
            @ApiResponse(responseCode = "404", description = "EMPLOYEE_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<EmployeeResponse>> unassign(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(employeeService.unassign(adminId(jwt), id)));
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long adminId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
