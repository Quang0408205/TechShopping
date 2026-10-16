package com.example.Tech.controller.product;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Excel imports (plan v4 steps 6 + 7) over HTTP. NOT transactional on purpose: a dry run marks the import's own
 * transaction rollback-only, which must be a real transaction to prove that nothing is written. Everything this test
 * creates (category, store, products, brand) is removed afterwards; the two accounts are reused between runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExcelImportApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final List<String> PRODUCT_HEADER = List.of("Mã SP *", "Tên sản phẩm *", "Danh mục *", "Thương hiệu",
            "Mô tả", "Bảo hành (tháng)", "Ảnh (link)", "Đang bán", "Tên phiên bản *", "RAM", "Bộ nhớ", "Màu", "Giá *",
            "Giá KM", "SKU phiên bản *");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbc;

    private final String tag = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    private String code;
    private String categoryName;
    private String brandName;
    private Integer categoryId;
    private Integer storeId;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        code = "XLT-" + tag;
        categoryName = "Danh mục Excel " + tag;
        brandName = "Hãng Excel " + tag;
        categoryId = jdbc.queryForObject("insert into categories (name, slug, is_active) values (?, ?, true) returning category_id",
                Integer.class, categoryName, "danh-muc-excel-" + tag.toLowerCase());
        storeId = jdbc.queryForObject("insert into stores (name, address, district, city, is_active) values (?, '1 Test', 'Quận 1', 'Hồ Chí Minh', true) returning store_id",
                Integer.class, "Kho Excel " + tag);
        adminToken = account("xl.import.admin", "ADMIN");
        customerToken = account("xl.import.khach", null);
    }

    @AfterEach
    void cleanUp() {
        String products = "select product_id from products where sku like 'XLT-" + tag + "%'";
        String variants = "select variant_id from product_variants where product_id in (" + products + ")";
        jdbc.update("delete from stock_movements where variant_id in (" + variants + ")");
        jdbc.update("delete from inventory where variant_id in (" + variants + ")");
        jdbc.update("delete from products where product_id in (" + products + ")");
        jdbc.update("delete from brands where name = ?", brandName);
        jdbc.update("delete from categories where category_id = ?", categoryId);
        jdbc.update("delete from stores where store_id = ?", storeId);
    }

    @Test
    void template_isAnXlsxWithTheSheets() throws Exception {
        byte[] file = mockMvc.perform(get("/api/v1/admin/products/import/template").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("mau-nhap-san-pham.xlsx"))
                .andReturn().getResponse().getContentAsByteArray();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            assertThat(workbook.getSheet("SanPham").getRow(0).getCell(0).getStringCellValue()).isEqualTo("Mã SP *");
            assertThat(workbook.getSheet("ThongSo")).isNotNull();
            assertThat(workbook.getSheet("HuongDan")).isNotNull();
        }
        mockMvc.perform(get("/api/v1/admin/products/import/template").header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void badRows_areAllListed_andNothingIsWritten() throws Exception {
        byte[] file = workbook(List.of(
                row(code, "Máy Excel", categoryName, brandName, "", "", "", "", "8GB/256GB", "", "", "", "0", "", code + "-A"),
                row(code, "Tên khác", "", "", "", "", "", "", "12GB/256GB", "", "", "", "1000000", "2000000", code + "-A"),
                row(code + "-2", "Máy hai", "Không có danh mục này", "", "", "", "ftp://x", "", "", "", "", "", "abc", "", code + "-B")),
                List.of(row("XLT-KHONG-CO", "Pin", "5000 mAh")));

        JsonNode result = data(importProducts(file, false).andExpect(status().isOk()));

        assertThat(result.get("applied").asBoolean()).isFalse();
        List<String> errors = new ArrayList<>();
        result.get("errors").forEach(e -> errors.add(e.get("sheet").asString() + "!" + e.get("row").asInt() + " " + e.get("column").asString()));
        assertThat(errors).contains("SanPham!2 Giá", "SanPham!3 Tên sản phẩm", "SanPham!3 Giá KM", "SanPham!3 SKU phiên bản",
                "SanPham!4 Danh mục", "SanPham!4 Ảnh (link)", "SanPham!4 Giá", "SanPham!4 Tên phiên bản", "SanPham!2 Ảnh (link)",
                "ThongSo!2 Mã SP");
        assertThat(count("select count(*) from products where sku like ?", code + "%")).isZero();
        assertThat(count("select count(*) from brands where name = ?", brandName)).isZero();
    }

    @Test
    void dryRun_reportsTheChanges_withoutWriting_thenTheRealImportCreates_andASecondFileUpdates() throws Exception {
        byte[] first = workbook(List.of(
                row(code, "Máy Excel " + tag, categoryName, brandName, "Mô tả <b>thử</b>", "24",
                        "https://example.com/a.jpg | https://example.com/b.jpg", "Có",
                        "8GB/256GB - Đen", "8 GB", "256 GB", "Đen", "9.990.000", "9490000", code + "-8-256"),
                row(code, "", "", "", "", "", "", "", "12GB/512GB - Bạc", "12 GB", "512 GB", "Bạc", "11990000", "", code + "-12-512")),
                List.of(row(code, "Màn hình", "6.7 inch"), row(code, "Pin", "5000 mAh")));

        JsonNode dry = data(importProducts(first, true).andExpect(status().isOk()));
        assertThat(dry.get("applied").asBoolean()).isFalse();
        assertThat(dry.get("errors")).isEmpty();
        assertThat(dry.get("summary").get("productsCreated").asInt()).isEqualTo(1);
        assertThat(dry.get("summary").get("variantsCreated").asInt()).isEqualTo(2);
        assertThat(dry.get("summary").get("brandsCreated").asInt()).isEqualTo(1);
        assertThat(count("select count(*) from products where sku = ?", code)).isZero();
        assertThat(count("select count(*) from brands where name = ?", brandName)).isZero();

        JsonNode real = data(importProducts(first, false).andExpect(status().isOk()));
        assertThat(real.get("applied").asBoolean()).isTrue();
        assertThat(real.get("summary")).isEqualTo(dry.get("summary"));
        Map<String, Object> product = jdbc.queryForMap("select product_id, name, base_price, discount_price, warranty_months, description, is_active from products where sku = ?", code);
        Long productId = ((Number) product.get("product_id")).longValue();
        assertThat(product.get("name")).isEqualTo("Máy Excel " + tag);
        assertThat(((Number) product.get("base_price")).longValue()).isEqualTo(9990000L);
        assertThat(((Number) product.get("discount_price")).longValue()).isEqualTo(9490000L);
        assertThat(product.get("warranty_months")).isEqualTo(24);
        assertThat(jdbc.queryForList("select variant_name from product_variants where product_id = ? order by variant_id", String.class, productId))
                .containsExactly("8GB/256GB - Đen", "12GB/512GB - Bạc");
        assertThat(jdbc.queryForList("select image_url || ':' || is_primary from product_images where product_id = ? order by display_order", String.class, productId))
                .containsExactly("https://example.com/a.jpg:true", "https://example.com/b.jpg:false");
        assertThat(jdbc.queryForList("select spec_name from product_specifications where product_id = ? order by spec_order", String.class, productId))
                .containsExactly("Màn hình", "Pin");
        assertThat(data(mockMvc.perform(get("/api/v1/products/" + productId))).get("brandName").asString()).isEqualTo(brandName);

        // second file: change a price, add a variant, replace the specifications; blank cells keep the old values
        byte[] second = workbook(List.of(
                row(code, "", "", "", "", "", "https://example.com/a.jpg | https://example.com/c.jpg", "",
                        "8GB/256GB - Đen", "", "", "", "8990000", "", code + "-8-256"),
                row(code, "", "", "", "", "", "", "", "16GB/1TB - Xanh", "16 GB", "1 TB", "Xanh", "14990000", "", code + "-16-1tb")),
                List.of(row(code, "Chip", "Demo X")));
        JsonNode update = data(importProducts(second, false).andExpect(status().isOk()));
        assertThat(update.get("summary").get("productsUpdated").asInt()).isEqualTo(1);
        assertThat(update.get("summary").get("variantsUpdated").asInt()).isEqualTo(1);
        assertThat(update.get("summary").get("variantsCreated").asInt()).isEqualTo(1);
        assertThat(update.get("summary").get("imagesAdded").asInt()).isEqualTo(1);
        Map<String, Object> after = jdbc.queryForMap("select name, base_price, discount_price, warranty_months from products where product_id = ?", productId);
        assertThat(after.get("name")).isEqualTo("Máy Excel " + tag);
        assertThat(after.get("warranty_months")).isEqualTo(24);
        assertThat(((Number) after.get("base_price")).longValue()).isEqualTo(8990000L);
        assertThat(after.get("discount_price")).isNull();
        assertThat(count("select count(*) from product_variants where product_id = ?", productId)).isEqualTo(3);
        assertThat(jdbc.queryForList("select spec_name from product_specifications where product_id = ?", String.class, productId))
                .containsExactly("Chip");

        // a variant SKU of this product cannot be claimed by another one
        byte[] stolen = workbook(List.of(row(code + "-X", "Máy khác", categoryName, "", "", "", "https://example.com/x.jpg", "",
                "Bản 1", "", "", "", "1000000", "", code + "-8-256")), List.of());
        assertThat(data(importProducts(stolen, true)).get("errors").get(0).get("message").asString())
                .contains("thuộc sản phẩm khác (Mã SP " + code + ")");

        // ---- step 7: stock-in by Excel at a store ----
        byte[] badStock = stockWorkbook(List.of(row(code + "-8-256", "", "0", "", ""), row("KHONG-CO-SKU", "", "5", "", "")));
        JsonNode bad = data(importStock(badStock, false).andExpect(status().isOk()));
        assertThat(bad.get("applied").asBoolean()).isFalse();
        assertThat(bad.get("errors")).hasSize(2);

        byte[] stock = stockWorkbook(List.of(row(code + "-8-256", "", "7", "NCC Excel", "Lô 1"), row(code + "-16-1tb", "", "3", "", "")));
        JsonNode stockDry = data(importStock(stock, true).andExpect(status().isOk()));
        assertThat(stockDry.get("summary").get("units").asInt()).isEqualTo(10);
        assertThat(count("select count(*) from inventory i join product_variants v using (variant_id) where v.product_id = ?", productId)).isZero();

        JsonNode stockReal = data(importStock(stock, false).andExpect(status().isOk()));
        assertThat(stockReal.get("applied").asBoolean()).isTrue();
        assertThat(count("select coalesce(sum(quantity), 0) from inventory where store_id = ? and variant_id in (select variant_id from product_variants where product_id = ?)",
                storeId, productId)).isEqualTo(10);
        assertThat(jdbc.queryForList("select supplier_name from stock_movements m join product_variants v using (variant_id) where v.product_id = ? and m.movement_type = 'IN' order by m.movement_id",
                String.class, productId)).containsExactly("NCC Excel", null);
        mockMvc.perform(multipart("/api/v1/admin/stores/" + storeId + "/inventory/import").file(new MockMultipartFile("file", "kho.xlsx", null, stock))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void notAnXlsx_isRefused() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/products/import")
                        .file(new MockMultipartFile("file", "san-pham.csv", "text/csv", "a,b".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EXCEL_INVALID_FILE"));
        mockMvc.perform(multipart("/api/v1/admin/products/import")
                        .file(new MockMultipartFile("file", "hong.xlsx", null, "not a zip".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EXCEL_INVALID_FILE"));
    }

    // ---------- helpers ----------

    private ResultActions importProducts(byte[] file, boolean dryRun) throws Exception {
        return mockMvc.perform(multipart("/api/v1/admin/products/import").file(new MockMultipartFile("file", "san-pham.xlsx", null, file))
                .param("dryRun", String.valueOf(dryRun)).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken));
    }

    private ResultActions importStock(byte[] file, boolean dryRun) throws Exception {
        return mockMvc.perform(multipart("/api/v1/admin/stores/" + storeId + "/inventory/import").file(new MockMultipartFile("file", "kho.xlsx", null, file))
                .param("dryRun", String.valueOf(dryRun)).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken));
    }

    private byte[] workbook(List<List<String>> products, List<List<String>> specs) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            fill(workbook.createSheet("SanPham"), PRODUCT_HEADER, products);
            fill(workbook.createSheet("ThongSo"), List.of("Mã SP *", "Tên thông số *", "Giá trị *"), specs);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] stockWorkbook(List<List<String>> rows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            fill(workbook.createSheet("NhapKho"), List.of("SKU phiên bản *", "Tên sản phẩm (tham khảo)", "Số lượng *", "Nhà cung cấp", "Ghi chú"), rows);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void fill(Sheet sheet, List<String> header, List<List<String>> rows) {
        Row first = sheet.createRow(0);
        for (int c = 0; c < header.size(); c++) {
            first.createCell(c).setCellValue(header.get(c));
        }
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < rows.get(r).size(); c++) {
                String value = rows.get(r).get(c);
                if (value.matches("\\d+")) {
                    row.createCell(c).setCellValue(Double.parseDouble(value));  // a real number cell, as Excel stores it
                } else {
                    row.createCell(c).setCellValue(value);
                }
            }
        }
    }

    private static List<String> row(String... cells) {
        return List.of(cells);
    }

    private long count(String sql, Object... args) {
        Number value = jdbc.queryForObject(sql, Number.class, args);
        return value == null ? 0 : value.longValue();
    }

    /** Logs in (registering the account and adding the role on first use). */
    private String account(String username, String role) throws Exception {
        if (count("select count(*) from users where username = ?", username) == 0) {
            mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(Map.of(
                    "email", username + "@example.com", "username", username, "password", PASSWORD, "fullname", "Kiểm Thử Excel"))))
                    .andExpect(status().isCreated());
        }
        if (role != null) {
            jdbc.update("insert into user_roles (user_id, role_id) select u.user_id, r.role_id from users u, roles r "
                    + "where u.username = ? and r.name = ? on conflict do nothing", username, role);
        }
        return data(mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("identifier", username, "password", PASSWORD))))
                .andExpect(status().isOk())).get("accessToken").asString();
    }

    private JsonNode data(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }
}
