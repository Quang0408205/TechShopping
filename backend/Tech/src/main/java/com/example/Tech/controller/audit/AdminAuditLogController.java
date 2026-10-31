package com.example.Tech.controller.audit;

import com.example.Tech.dto.response.audit.AuditLogResponse;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.service.audit.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** The audit trail. Read-only and ADMIN only (URL rule, @PreAuthorize and a role check in the service). */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Audit log", description = "Who changed what: write requests of staff accounts")
@ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
@ApiResponse(responseCode = "403", description = "ACCESS_DENIED")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Audit lines, newest first",
            description = "keyword matches the username, full name, action or path; entityType is the code of "
                    + "GET /admin/audit-logs/entity-types; method is POST, PUT, PATCH or DELETE; dates are inclusive")
    public ResponseEntity<ApiResult<PageResponse<AuditLogResponse>>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(auditLogService.list(Long.valueOf(jwt.getSubject()), keyword, entityType,
                method, userId, fromDate, toDate, pageable)));
    }

    @GetMapping("/entity-types")
    @Operation(summary = "Kinds of things that appear in the trail, for the filter")
    public ResponseEntity<ApiResult<List<AuditLogService.EntityTypeOption>>> entityTypes(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(auditLogService.entityTypes(Long.valueOf(jwt.getSubject()))));
    }
}
