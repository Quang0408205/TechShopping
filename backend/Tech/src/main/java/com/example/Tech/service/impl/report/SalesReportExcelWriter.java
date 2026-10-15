package com.example.Tech.service.impl.report;

import com.example.Tech.dto.response.report.SalesReportResponse;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.report.SalesReportQueries.DeliveredOrder;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * The sales report as an .xlsx workbook: one sheet per table of the report page plus the delivered orders.
 * Money cells are numbers formatted as "#,##0 đ" (so Excel can still sum them), header rows are bold and frozen.
 */
final class SalesReportExcelWriter {

    static final String SUMMARY = "Tổng hợp";
    static final String SERIES = "Theo thời gian";
    static final String STORES = "Theo chi nhánh";
    static final String EMPLOYEES = "Theo nhân viên";
    static final String CATEGORIES = "Theo danh mục";
    static final String PRODUCTS = "Top sản phẩm";
    static final String ORDERS = "Danh sách đơn";

    private static final Map<String, String> PAYMENT_LABELS = Map.of(
            "COD", "Thanh toán khi nhận hàng",
            "BANK_TRANSFER", "Chuyển khoản",
            "INSTALLMENT", "Trả góp");

    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final CellStyle header;
    private final CellStyle money;
    private final CellStyle date;
    private final CellStyle dateTime;
    private final CellStyle bold;

    private SalesReportExcelWriter() {
        DataFormat format = workbook.createDataFormat();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        header = workbook.createCellStyle();
        header.setFont(boldFont);
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setBorderBottom(BorderStyle.THIN);

        bold = workbook.createCellStyle();
        bold.setFont(boldFont);

        money = workbook.createCellStyle();
        money.setDataFormat(format.getFormat("#,##0 \"đ\""));

        date = workbook.createCellStyle();
        date.setDataFormat(format.getFormat("dd/mm/yyyy"));

        dateTime = workbook.createCellStyle();
        dateTime.setDataFormat(format.getFormat("dd/mm/yyyy hh:mm"));
    }

    /**
     * @param withStores the per-store sheet (ADMIN looking at every store)
     * @param ordersCut  true when the order list was cut at {@code orders.size()} rows
     */
    static byte[] write(SalesReportResponse report, List<DeliveredOrder> orders, boolean withStores, boolean ordersCut) {
        SalesReportExcelWriter writer = new SalesReportExcelWriter();
        writer.summary(report);
        writer.series(report);
        if (withStores) {
            writer.stores(report);
        }
        writer.employees(report);
        writer.categories(report);
        writer.products(report);
        writer.orders(orders, ordersCut);
        try (XSSFWorkbook workbook = writer.workbook; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void summary(SalesReportResponse report) {
        Sheet sheet = workbook.createSheet(SUMMARY);
        SalesReportResponse.Summary s = report.summary();
        int r = 0;
        r = label(sheet, r, "Báo cáo doanh thu TechShopping", null);
        r = text(sheet, r, "Chi nhánh", report.storeName() != null ? report.storeName() : "Tất cả chi nhánh");
        r = dateRow(sheet, r, "Từ ngày", report.fromDate());
        r = dateRow(sheet, r, "Đến ngày", report.toDate());
        r = text(sheet, r, "Nhóm theo", report.groupBy() != null && report.groupBy().name().equals("MONTH") ? "Tháng" : "Ngày");
        r++;
        r = moneyRow(sheet, r, "Doanh thu gộp (đơn đã giao, gồm phí giao hàng)", s.grossRevenue());
        r = moneyRow(sheet, r, "Tiền hoàn (trả hàng đã hoàn tiền trong kỳ)", s.refundAmount());
        r = moneyRow(sheet, r, "Doanh thu thuần", s.netRevenue());
        r = number(sheet, r, "Số đơn đã giao", s.deliveredOrders());
        r = number(sheet, r, "Số sản phẩm bán ra", s.unitsSold());
        r = moneyRow(sheet, r, "Giá trị trung bình / đơn", s.averageOrderValue());
        r = moneyRow(sheet, r, "Phí giao hàng", s.shippingFees());
        r = number(sheet, r, "Số lần hoàn tiền", s.refundCount());
        moneyRow(sheet, r, "Tổng hoa hồng", s.totalCommission());
        sheet.setColumnWidth(0, 52 * 256);
        sheet.setColumnWidth(1, 24 * 256);
    }

    private void series(SalesReportResponse report) {
        boolean monthly = report.groupBy() != null && report.groupBy().name().equals("MONTH");
        Sheet sheet = table(SERIES, monthly ? "Tháng" : "Ngày", "Số đơn", "Doanh thu gộp", "Tiền hoàn", "Doanh thu thuần");
        int r = 1;
        for (SalesReportResponse.Period p : report.series()) {
            Row row = sheet.createRow(r++);
            if (monthly) {
                row.createCell(0).setCellValue("%02d/%d".formatted(p.start().getMonthValue(), p.start().getYear()));
            } else {
                dateCell(row, 0, p.start());
            }
            row.createCell(1).setCellValue(p.orders());
            moneyCell(row, 2, p.grossRevenue());
            moneyCell(row, 3, p.refundAmount());
            moneyCell(row, 4, p.netRevenue());
        }
        widths(sheet, 14, 10, 18, 18, 18);
    }

    private void stores(SalesReportResponse report) {
        Sheet sheet = table(STORES, "Chi nhánh", "Trạng thái", "Số đơn", "Doanh thu gộp", "Tiền hoàn", "Doanh thu thuần");
        int r = 1;
        for (SalesReportResponse.StoreRow s : report.byStore()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(s.storeName());
            row.createCell(1).setCellValue(s.storeActive() ? "Đang mở" : "Tạm đóng");
            row.createCell(2).setCellValue(s.orders());
            moneyCell(row, 3, s.grossRevenue());
            moneyCell(row, 4, s.refundAmount());
            moneyCell(row, 5, s.netRevenue());
        }
        widths(sheet, 32, 12, 10, 18, 18, 18);
    }

    private void employees(SalesReportResponse report) {
        Sheet sheet = table(EMPLOYEES, "Mã NV", "Nhân viên", "Chi nhánh", "Số đơn", "Doanh số", "Hoa hồng", "Đã hoàn tiền");
        int r = 1;
        for (SalesReportResponse.EmployeeRow e : report.byEmployee()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(e.employeeCode() != null ? e.employeeCode() : "");
            row.createCell(1).setCellValue(e.employeeId() == null ? "Không gán nhân viên" : e.fullname());
            row.createCell(2).setCellValue(e.storeName() != null ? e.storeName() : "");
            row.createCell(3).setCellValue(e.orders());
            moneyCell(row, 4, e.salesAmount());
            moneyCell(row, 5, e.commission());
            moneyCell(row, 6, e.refundAmount());
        }
        widths(sheet, 12, 28, 28, 10, 18, 16, 16);
    }

    private void categories(SalesReportResponse report) {
        Sheet sheet = table(CATEGORIES, "Danh mục", "Số sản phẩm", "Doanh thu (không gồm phí giao hàng)");
        int r = 1;
        for (SalesReportResponse.CategorySlice c : report.byCategory()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(c.categoryName() != null ? c.categoryName() : "Chưa phân loại");
            row.createCell(1).setCellValue(c.units());
            moneyCell(row, 2, c.revenue());
        }
        Row note = sheet.createRow(r + 1);
        note.createCell(0).setCellValue("Phương thức thanh toán");
        note.getCell(0).setCellStyle(bold);
        r += 2;
        for (SalesReportResponse.PaymentSlice p : report.byPaymentMethod()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(PAYMENT_LABELS.getOrDefault(p.paymentMethod(), p.paymentMethod()));
            row.createCell(1).setCellValue(p.orders());
            moneyCell(row, 2, p.revenue());
        }
        widths(sheet, 30, 14, 34);
    }

    private void products(SalesReportResponse report) {
        Sheet sheet = table(PRODUCTS, "Hạng", "Sản phẩm", "Danh mục", "Số lượng", "Doanh thu");
        int r = 1;
        for (SalesReportResponse.ProductRow p : report.topProducts()) {
            Row row = sheet.createRow(r);
            row.createCell(0).setCellValue(r);
            row.createCell(1).setCellValue(p.productName());
            row.createCell(2).setCellValue(p.categoryName() != null ? p.categoryName() : "");
            row.createCell(3).setCellValue(p.units());
            moneyCell(row, 4, p.revenue());
            r++;
        }
        widths(sheet, 8, 48, 22, 10, 18);
    }

    private void orders(List<DeliveredOrder> orders, boolean cut) {
        Sheet sheet = table(ORDERS, "Mã đơn", "Ngày giao", "Chi nhánh", "Khách hàng", "NV xác nhận",
                "Thanh toán", "Tổng tiền", "Đã hoàn tiền");
        int r = 1;
        for (DeliveredOrder o : orders) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(OrderMapper.code(o.orderId()));
            dateTimeCell(row, 1, o.deliveredAt());
            row.createCell(2).setCellValue(o.storeName() != null ? o.storeName() : "");
            row.createCell(3).setCellValue(o.customer() != null ? o.customer() : "");
            row.createCell(4).setCellValue(o.confirmedBy() != null ? o.confirmedBy() : "");
            row.createCell(5).setCellValue(PAYMENT_LABELS.getOrDefault(o.paymentMethod(), o.paymentMethod()));
            moneyCell(row, 6, o.total());
            moneyCell(row, 7, o.refunded());
        }
        if (cut) {
            Row row = sheet.createRow(r + 1);
            row.createCell(0).setCellValue("Chỉ xuất %d đơn đầu tiên; hãy chọn khoảng ngày ngắn hơn để có đủ danh sách."
                    .formatted(orders.size()));
            row.getCell(0).setCellStyle(bold);
        }
        widths(sheet, 14, 18, 28, 26, 26, 24, 16, 16);
    }

    /* ---------- helpers ---------- */

    private Sheet table(String name, String... columns) {
        Sheet sheet = workbook.createSheet(name);
        Row row = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(header);
        }
        sheet.createFreezePane(0, 1);
        return sheet;
    }

    private static void widths(Sheet sheet, int... chars) {
        for (int i = 0; i < chars.length; i++) {
            sheet.setColumnWidth(i, chars[i] * 256);
        }
    }

    private int label(Sheet sheet, int r, String text, CellStyle style) {
        Cell cell = sheet.createRow(r).createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style != null ? style : bold);
        return r + 1;
    }

    private int text(Sheet sheet, int r, String label, String value) {
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return r + 1;
    }

    private int dateRow(Sheet sheet, int r, String label, LocalDate value) {
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue(label);
        dateCell(row, 1, value);
        return r + 1;
    }

    private int moneyRow(Sheet sheet, int r, String label, BigDecimal value) {
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue(label);
        moneyCell(row, 1, value);
        return r + 1;
    }

    private int number(Sheet sheet, int r, String label, long value) {
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return r + 1;
    }

    private void moneyCell(Row row, int column, BigDecimal value) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? 0 : value.doubleValue());
        cell.setCellStyle(money);
    }

    private void dateCell(Row row, int column, LocalDate value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
            cell.setCellStyle(date);
        }
    }

    private void dateTimeCell(Row row, int column, LocalDateTime value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
            cell.setCellStyle(dateTime);
        }
    }
}
