package com.example.Tech.controller.report;

import com.example.Tech.dto.request.report.SalesReportRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.report.DashboardSummaryResponse;
import com.example.Tech.dto.response.report.SalesReportResponse;
import com.example.Tech.service.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Revenue reports and the overview cards; the store scope is decided in the service from the database. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@Tag(name = "Admin - Reports", description = "Sales report and dashboard (ADMIN, branch managers, staff)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED, NO_ACTIVE_STORE_ASSIGNMENT")
})
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/reports/sales")
    @Operation(summary = "Sales report over a period",
            description = "ADMIN: every store or one (storeId); a branch manager: their store only (another storeId → "
                    + "403). Revenue = delivered orders by delivery day; refunds = refunded returns by refund day. "
                    + "Default period: the last 30 days; at most 366 days; DAY grouping up to 62 days.")
    @ApiResponse(responseCode = "200", description = "Summary, series, pies, per store / employee, top products")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (details.fromDate / toDate / groupBy)")
    @ApiResponse(responseCode = "404", description = "STORE_NOT_FOUND")
    public ResponseEntity<ApiResult<SalesReportResponse>> sales(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @ParameterObject SalesReportRequest filter) {
        return ResponseEntity.ok(ApiResult.ok(reportService.sales(Long.valueOf(jwt.getSubject()), filter)));
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Overview cards: today / this month revenue, orders to handle, open requests, stock alerts",
            description = "STAFF: their store; ADMIN: every store, plus open stores and active employees")
    @ApiResponse(responseCode = "200", description = "Overview figures")
    public ResponseEntity<ApiResult<DashboardSummaryResponse>> dashboard(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(reportService.dashboard(Long.valueOf(jwt.getSubject()))));
    }
}
