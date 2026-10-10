package com.example.Tech.service.report;

import com.example.Tech.dto.request.report.SalesReportRequest;
import com.example.Tech.dto.response.report.DashboardSummaryResponse;
import com.example.Tech.dto.response.report.ReportFile;
import com.example.Tech.dto.response.report.SalesReportResponse;

public interface ReportService {

    /** ADMIN: every store or one; branch manager: their store only. */
    SalesReportResponse sales(Long userId, SalesReportRequest filter);

    /** The same report (same filter, same access rules) as an .xlsx file, plus the list of delivered orders. */
    ReportFile exportSales(Long userId, SalesReportRequest filter);

    /** ADMIN: every store; STAFF: their store. */
    DashboardSummaryResponse dashboard(Long userId);
}
