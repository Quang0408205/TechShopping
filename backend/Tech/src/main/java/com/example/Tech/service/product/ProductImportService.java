package com.example.Tech.service.product;

import com.example.Tech.dto.response.excel.ExcelImportResponse;
import org.springframework.web.multipart.MultipartFile;

/** Products, variants and specifications from an Excel file (ADMIN). */
public interface ProductImportService {

    /** The .xlsx template: sheets SanPham, ThongSo and a guide. */
    byte[] template();

    /** Checks every row; writes everything in one transaction only when nothing is wrong and dryRun is false. */
    ExcelImportResponse importProducts(MultipartFile file, boolean dryRun);
}
