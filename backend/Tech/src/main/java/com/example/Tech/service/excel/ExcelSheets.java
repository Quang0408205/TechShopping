package com.example.Tech.service.excel;

import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads and writes the simple sheets of the Excel imports: first row = headers (matched without accents / case /
 * "*"), one record per row, blank rows skipped. Numbers come back as plain text ("41990000", never "4.199E7").
 */
public final class ExcelSheets {

    public static final int MAX_ROWS = 2000;

    private static final DataFormatter FORMATTER = new DataFormatter(Locale.ROOT);

    private ExcelSheets() {
    }

    /** One data row: its Excel row number (1-based, as shown in Excel) and the cells by column name. */
    public record Record(int row, Map<String, String> cells) {

        /** Trimmed text, null when blank. */
        public String get(String column) {
            String value = cells.get(column);
            return value == null || value.isBlank() ? null : value.trim();
        }
    }

    public static Workbook open(MultipartFile file) {
        String name = file == null ? null : file.getOriginalFilename();
        if (file == null || file.isEmpty() || name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessException(ErrorCode.EXCEL_INVALID_FILE, "Vui lòng chọn file Excel .xlsx (tải file mẫu nếu chưa có)");
        }
        try (InputStream in = file.getInputStream()) {
            return new XSSFWorkbook(in);
        } catch (IOException | RuntimeException e) {
            throw new BusinessException(ErrorCode.EXCEL_INVALID_FILE, "Không đọc được file Excel: file hỏng hoặc không phải .xlsx");
        }
    }

    /**
     * Rows of the named sheet. {@code columns} are the expected headers; a missing sheet is an empty list unless
     * {@code required}; a missing column or more than MAX_ROWS rows refuses the whole file.
     */
    public static List<Record> read(Workbook workbook, String sheetName, List<String> columns, boolean required) {
        Sheet sheet = findSheet(workbook, sheetName);
        if (sheet == null) {
            if (required) {
                throw new BusinessException(ErrorCode.EXCEL_INVALID_FILE, "File thiếu sheet \"%s\"".formatted(sheetName));
            }
            return List.of();
        }
        Row header = sheet.getRow(sheet.getFirstRowNum());
        Map<String, Integer> index = new HashMap<>();
        if (header != null) {
            for (Cell cell : header) {
                index.putIfAbsent(key(text(cell)), cell.getColumnIndex());
            }
        }
        for (String column : columns) {
            if (!index.containsKey(key(column))) {
                throw new BusinessException(ErrorCode.EXCEL_INVALID_FILE,
                        "Sheet \"%s\" thiếu cột \"%s\" (dùng đúng file mẫu)".formatted(sheetName, column));
            }
        }
        List<Record> records = new ArrayList<>();
        for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            Map<String, String> cells = new LinkedHashMap<>();
            boolean blank = true;
            for (String column : columns) {
                String value = text(row.getCell(index.get(key(column))));
                cells.put(column, value);
                blank &= value.isBlank();
            }
            if (blank) {
                continue;
            }
            if (records.size() == MAX_ROWS) {
                throw new BusinessException(ErrorCode.EXCEL_INVALID_FILE,
                        "Sheet \"%s\" quá %d dòng, hãy chia thành nhiều file".formatted(sheetName, MAX_ROWS));
            }
            records.add(new Record(r + 1, cells));
        }
        return records;
    }

    /** A workbook with one sheet per entry (bold header row + example rows) and a guide sheet of plain lines. */
    public static byte[] template(Map<String, List<List<String>>> sheets, String guideSheet, List<String> guideLines) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(bold);
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            for (Map.Entry<String, List<List<String>>> entry : sheets.entrySet()) {
                Sheet sheet = workbook.createSheet(entry.getKey());
                List<List<String>> rows = entry.getValue();
                for (int r = 0; r < rows.size(); r++) {
                    Row row = sheet.createRow(r);
                    for (int c = 0; c < rows.get(r).size(); c++) {
                        Cell cell = row.createCell(c);
                        cell.setCellValue(rows.get(r).get(c));
                        cell.setCellStyle(r == 0 ? headerStyle : textStyle);
                    }
                }
                for (int c = 0; c < rows.getFirst().size(); c++) {
                    sheet.setColumnWidth(c, 20 * 256);
                    sheet.setDefaultColumnStyle(c, textStyle);
                }
                sheet.createFreezePane(0, 1);
            }
            Sheet guide = workbook.createSheet(guideSheet);
            guide.setColumnWidth(0, 120 * 256);
            for (int i = 0; i < guideLines.size(); i++) {
                Cell cell = guide.createRow(i).createCell(0);
                cell.setCellValue(guideLines.get(i));
                if (i == 0) {
                    cell.setCellStyle(headerStyle);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot build the Excel template", e);
        }
    }

    private static Sheet findSheet(Workbook workbook, String name) {
        for (Sheet sheet : workbook) {
            if (key(sheet.getSheetName()).equals(key(name))) {
                return sheet;
            }
        }
        return null;
    }

    private static String text(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (type == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
        return FORMATTER.formatCellValue(cell).trim();
    }

    /** "Mã SP *" and "ma sp" are the same column. */
    static String key(String header) {
        String plain = Normalizer.normalize(header == null ? "" : header, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D');
        return plain.replace("*", "").trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
