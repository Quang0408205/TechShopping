package com.example.Tech.dto.response.report;

/** A generated report file (the Excel export). */
public record ReportFile(String filename, String contentType, byte[] content) {
}
