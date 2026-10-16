package com.example.Tech.service.impl.inventory;

import com.example.Tech.dto.request.inventory.StockInRequest;
import com.example.Tech.dto.response.excel.ExcelImportResponse;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.service.excel.ExcelSheets;
import com.example.Tech.service.excel.ExcelSheets.Record;
import com.example.Tech.service.inventory.InventoryService;
import com.example.Tech.service.store.StoreAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nhập kho bằng Excel for one store: each row is an ordinary stock-in (InventoryService.stockIn, so the same access
 * rule and stock book entry). All rows are checked first; any error → nothing is stocked in.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockImportService {

    static final String SHEET = "NhapKho";
    static final String SKU = "SKU phiên bản";
    static final String PRODUCT = "Tên sản phẩm (tham khảo)";
    static final String QUANTITY = "Số lượng";
    static final String SUPPLIER = "Nhà cung cấp";
    static final String NOTE = "Ghi chú";

    private static final List<String> COLUMNS = List.of(SKU, QUANTITY, SUPPLIER, NOTE);

    private final InventoryService inventoryService;
    private final ProductVariantRepository variantRepository;
    private final StoreAccessGuard storeAccessGuard;

    public byte[] template() {
        Map<String, List<List<String>>> sheets = new LinkedHashMap<>();
        sheets.put(SHEET, List.of(
                List.of(SKU + " *", PRODUCT, QUANTITY + " *", SUPPLIER, NOTE),
                List.of("TGDD-232466-V1", "iPhone 18 Pro Max 256GB", "20", "Công ty Phân phối ABC", "Lô hàng tháng 10")));
        return ExcelSheets.template(sheets, "HuongDan", List.of(
                "Hướng dẫn nhập kho bằng Excel (xoá dòng mẫu trước khi nhập)",
                "File nhập vào chi nhánh đang chọn ở trang Tồn kho; mỗi dòng là một lần nhập kho, ghi vào sổ kho.",
                "SKU phiên bản: mã SKU của phiên bản (xem ở trang Sản phẩm, khối Phiên bản). Cột tên sản phẩm chỉ để dễ đọc.",
                "Số lượng: số nguyên từ 1 đến 100.000. Nhà cung cấp tối đa 150 ký tự, ghi chú tối đa 1000 ký tự.",
                "Có bất kỳ lỗi nào thì không nhập dòng nào; sửa theo danh sách lỗi rồi tải lại. Tối đa 2000 dòng."));
    }

    @Transactional
    public ExcelImportResponse importStock(Long userId, Integer storeId, MultipartFile file, boolean dryRun) {
        storeAccessGuard.require(userId, storeId);
        List<Record> rows;
        try (Workbook workbook = ExcelSheets.open(file)) {
            rows = ExcelSheets.read(workbook, SHEET, COLUMNS, true);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        List<ExcelImportResponse.Error> errors = new ArrayList<>();
        List<StockInRequest> requests = new ArrayList<>();
        if (rows.isEmpty()) {
            errors.add(new ExcelImportResponse.Error(SHEET, 1, SKU, "Sheet NhapKho chưa có dòng dữ liệu nào"));
        }
        for (Record row : rows) {
            ProductVariant variant = null;
            String sku = row.get(SKU);
            if (sku == null) {
                errors.add(new ExcelImportResponse.Error(SHEET, row.row(), SKU, "Bắt buộc"));
            } else {
                variant = variantRepository.findBySkuVariant(sku).filter(v -> v.getProduct().getDeletedAt() == null).orElse(null);
                if (variant == null) {
                    errors.add(new ExcelImportResponse.Error(SHEET, row.row(), SKU, "Không có phiên bản SKU \"%s\"".formatted(sku)));
                }
            }
            Integer quantity = null;
            String text = row.get(QUANTITY);
            try {
                quantity = text == null ? null : Integer.valueOf(text.replaceAll("[\\s.,]", ""));
            } catch (NumberFormatException ignored) {
                // reported below
            }
            if (quantity == null || quantity < 1 || quantity > 100_000) {
                errors.add(new ExcelImportResponse.Error(SHEET, row.row(), QUANTITY, "Số lượng là số nguyên từ 1 đến 100.000"));
            }
            String supplier = row.get(SUPPLIER);
            if (supplier != null && supplier.length() > 150) {
                errors.add(new ExcelImportResponse.Error(SHEET, row.row(), SUPPLIER, "Tối đa 150 ký tự"));
            }
            String note = row.get(NOTE);
            if (note != null && note.length() > 1000) {
                errors.add(new ExcelImportResponse.Error(SHEET, row.row(), NOTE, "Tối đa 1000 ký tự"));
            }
            if (variant != null && quantity != null) {
                requests.add(new StockInRequest(variant.getId(), quantity, supplier, note));
            }
        }
        if (!errors.isEmpty()) {
            return new ExcelImportResponse(dryRun, false, rows.size(), Map.of(), errors);
        }

        int units = 0;
        for (StockInRequest request : requests) {
            inventoryService.stockIn(userId, storeId, request);
            units += request.quantity();
        }
        Map<String, Integer> summary = new LinkedHashMap<>();
        summary.put("stockIns", requests.size());
        summary.put("units", units);
        summary.put("variants", (int) requests.stream().map(StockInRequest::variantId).distinct().count());
        if (dryRun) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } else {
            log.info("User id={} imported {} stock-in row(s), {} unit(s) at store id={}", userId, requests.size(), units, storeId);
        }
        return new ExcelImportResponse(dryRun, !dryRun, rows.size(), summary, List.of());
    }
}
