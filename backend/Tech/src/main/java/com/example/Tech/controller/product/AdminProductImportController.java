package com.example.Tech.controller.product;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.excel.ExcelImportResponse;
import com.example.Tech.service.product.ProductImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/admin/products/import")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Product import", description = "Products, variants and specifications from Excel (ADMIN)")
public class AdminProductImportController {

    static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ProductImportService importService;

    @GetMapping("/template")
    @Operation(summary = "Download the .xlsx template (sheets SanPham, ThongSo, HuongDan)")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("mau-nhap-san-pham.xlsx", StandardCharsets.UTF_8).build().toString())
                .body(importService.template());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import products from Excel",
            description = "dryRun=true (default) checks and reports what would change without writing. Any row error → "
                    + "nothing written, errors listed per sheet / row / column")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checked (and written when applied = true)"),
            @ApiResponse(responseCode = "400", description = "EXCEL_INVALID_FILE (not .xlsx, missing sheet / column, too many rows)"),
            @ApiResponse(responseCode = "413", description = "File larger than 5 MB")
    })
    public ResponseEntity<ApiResult<ExcelImportResponse>> importProducts(@RequestPart("file") MultipartFile file,
                                                                         @RequestParam(defaultValue = "true") boolean dryRun) {
        return ResponseEntity.ok(ApiResult.ok(importService.importProducts(file, dryRun)));
    }
}
