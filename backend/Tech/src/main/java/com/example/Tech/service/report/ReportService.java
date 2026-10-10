package com.example.Tech.service.report;

import com.example.Tech.dto.request.report.SalesReportRequest;
import com.example.Tech.dto.response.report.DashboardSummaryResponse;
import com.example.Tech.dto.response.report.SalesReportResponse;

public interface ReportService {

    /** ADMIN: every store or one; branch manager: their store only. */
    SalesReportResponse sales(Long userId, SalesReportRequest filter);

    /** ADMIN: every store; STAFF: their store. */
    DashboardSummaryResponse dashboard(Long userId);
}
