package com.example.Tech.controller.employee;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.employee.EmployeeResponse;
import com.example.Tech.service.employee.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The logged-in staff member's own employee profile and current store (the admin area uses it to show the
 * store and to lock store filters for non-admin staff).
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'BRANCH_MANAGER', 'ADMIN')")
@Tag(name = "Employees", description = "Own employee profile (STAFF / ADMIN)")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/me")
    @Operation(summary = "My employee profile and current store")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile; assignment is null when not assigned to a store"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
            @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED"),
            @ApiResponse(responseCode = "404", description = "EMPLOYEE_NOT_FOUND (no profile yet)")
    })
    public ResponseEntity<ApiResult<EmployeeResponse>> me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(employeeService.getMine(Long.valueOf(jwt.getSubject()))));
    }
}
