package com.example.Tech.service.impl.product;

import com.example.Tech.cache.ReadCache;
import com.example.Tech.dto.response.excel.ExcelImportResponse;
import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductSpecification;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.repository.product.BrandRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductSpecificationRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.service.excel.ExcelSheets;
import com.example.Tech.service.excel.ExcelSheets.Record;
import com.example.Tech.service.product.ProductImportService;
import com.example.Tech.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sheet SanPham: one row per variant; rows with the same "Mã SP" (= products.sku) are one product, whose fields come
 * from its first row (later rows may leave them blank, a different value is an error). Existing products / variants
 * (by Mã SP / SKU phiên bản) are updated, blank cells keep the old value; new ones are created (a new product needs at
 * least one image link). Sheet ThongSo replaces the whole specification list of the products it names. Categories
 * must exist; unknown brands are created. Prices > 0. Any error → nothing is written.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImportServiceImpl implements ProductImportService {

    static final String PRODUCTS = "SanPham";
    static final String SPECS = "ThongSo";

    static final String CODE = "Mã SP";
    static final String NAME = "Tên sản phẩm";
    static final String CATEGORY = "Danh mục";
    static final String BRAND = "Thương hiệu";
    static final String DESCRIPTION = "Mô tả";
    static final String WARRANTY = "Bảo hành (tháng)";
    static final String IMAGES = "Ảnh (link)";
    static final String ACTIVE = "Đang bán";
    static final String VARIANT = "Tên phiên bản";
    static final String RAM = "RAM";
    static final String STORAGE = "Bộ nhớ";
    static final String COLOR = "Màu";
    static final String PRICE = "Giá";
    static final String DISCOUNT = "Giá KM";
    static final String SKU = "SKU phiên bản";
    static final String SPEC_NAME = "Tên thông số";
    static final String SPEC_VALUE = "Giá trị";

    private static final List<String> PRODUCT_COLUMNS = List.of(CODE, NAME, CATEGORY, BRAND, DESCRIPTION, WARRANTY,
            IMAGES, ACTIVE, VARIANT, RAM, STORAGE, COLOR, PRICE, DISCOUNT, SKU);
    private static final List<String> PRODUCT_FIELDS = List.of(NAME, CATEGORY, BRAND, DESCRIPTION, WARRANTY, IMAGES, ACTIVE);
    private static final List<String> SPEC_COLUMNS = List.of(CODE, SPEC_NAME, SPEC_VALUE);
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999999");

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductSpecificationRepository specificationRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ReadCache cache;
    private final com.example.Tech.service.product.ProductFilterService filterService;

    @Override
    public byte[] template() {
        Map<String, List<List<String>>> sheets = new LinkedHashMap<>();
        sheets.put(PRODUCTS, List.of(
                PRODUCT_COLUMNS.stream().map(c -> List.of(CODE, NAME, CATEGORY, PRICE, VARIANT, SKU).contains(c) ? c + " *" : c).toList(),
                List.of("SP-DEMO-01", "Điện thoại Demo X1", "Điện Thoại", "DemoBrand", "Mô tả ngắn", "12",
                        "https://example.com/anh-1.jpg | https://example.com/anh-2.jpg", "Có",
                        "8GB/256GB - Đen", "8 GB", "256 GB", "Đen", "9990000", "9490000", "SP-DEMO-01-8-256-DEN"),
                List.of("SP-DEMO-01", "", "", "", "", "", "", "",
                        "12GB/512GB - Bạc", "12 GB", "512 GB", "Bạc", "11990000", "", "SP-DEMO-01-12-512-BAC")));
        sheets.put(SPECS, List.of(
                List.of(CODE + " *", SPEC_NAME + " *", SPEC_VALUE + " *"),
                List.of("SP-DEMO-01", "Màn hình", "6.7 inch, OLED, 120Hz"),
                List.of("SP-DEMO-01", "Pin", "5000 mAh")));
        return ExcelSheets.template(sheets, "HuongDan", List.of(
                "Hướng dẫn nhập sản phẩm bằng Excel (xoá 2 sản phẩm DEMO trước khi nhập)",
                "Sheet SanPham: mỗi dòng là một PHIÊN BẢN. Các dòng cùng \"Mã SP\" là cùng một sản phẩm.",
                "Thông tin chung (tên, danh mục, thương hiệu, mô tả, bảo hành, ảnh, đang bán) điền ở dòng đầu của sản phẩm; dòng sau để trống.",
                "Cột có dấu * là bắt buộc. Giá, giá KM là số đồng (vd 9990000), giá phải lớn hơn 0, giá KM không lớn hơn giá.",
                "Danh mục phải có sẵn trong hệ thống (đúng tên). Thương hiệu chưa có sẽ được tạo mới.",
                "Ảnh: link http(s), nhiều ảnh cách nhau bằng dấu | ; ảnh đầu là ảnh chính. Sản phẩm mới cần ít nhất 1 ảnh.",
                "Đang bán: Có / Không (để trống = Có với sản phẩm mới, giữ nguyên với sản phẩm cũ).",
                "Mã SP / SKU phiên bản đã có trong hệ thống → CẬP NHẬT (ô để trống giữ giá trị cũ); mã mới → THÊM.",
                "Sheet ThongSo (không bắt buộc): mỗi dòng một thông số; sản phẩm có dòng ở đây sẽ được THAY toàn bộ thông số theo thứ tự dòng.",
                "Có bất kỳ lỗi nào thì hệ thống không ghi gì; sửa theo danh sách lỗi rồi tải lại file. Tối đa 2000 dòng mỗi sheet."));
    }

    @Override
    @Transactional
    public ExcelImportResponse importProducts(MultipartFile file, boolean dryRun) {
        List<Record> rows;
        List<Record> specRows;
        try (Workbook workbook = ExcelSheets.open(file)) {
            rows = ExcelSheets.read(workbook, PRODUCTS, PRODUCT_COLUMNS, true);
            specRows = ExcelSheets.read(workbook, SPECS, SPEC_COLUMNS, false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Plan plan = new Plan();
        check(rows, specRows, plan);
        int rowCount = rows.size() + specRows.size();
        if (!plan.errors.isEmpty()) {
            return new ExcelImportResponse(dryRun, false, rowCount, Map.of(), plan.errors);
        }
        Map<String, Integer> summary = apply(plan);
        if (dryRun) {
            // same code as a real import, then thrown away: the summary is exact
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } else {
            log.info("Excel import: {}", summary);
        }
        return new ExcelImportResponse(dryRun, !dryRun, rowCount, summary, List.of());
    }

    // ---------- check ----------

    private void check(List<Record> rows, List<Record> specRows, Plan plan) {
        if (rows.isEmpty()) {
            plan.error(PRODUCTS, 1, CODE, "Sheet SanPham chưa có dòng dữ liệu nào");
            return;
        }
        Map<String, Category> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(c -> norm(c.getName()), Function.identity(), (a, b) -> a));
        plan.brands = brandRepository.findAll().stream()
                .collect(Collectors.toMap(b -> norm(b.getName()), Function.identity(), (a, b) -> a));

        Set<String> skus = new HashSet<>();
        for (Record row : rows) {
            String code = row.get(CODE);
            if (code == null) {
                plan.error(PRODUCTS, row.row(), CODE, "Thiếu mã sản phẩm");
                continue;
            }
            if (code.length() > 50) {
                plan.error(PRODUCTS, row.row(), CODE, "Mã sản phẩm tối đa 50 ký tự");
                continue;
            }
            ProductPlan product = plan.products.computeIfAbsent(code, ProductPlan::new);
            if (product.firstRow == 0) {
                product.firstRow = row.row();
            }
            for (String field : PRODUCT_FIELDS) {
                String value = row.get(field);
                if (value == null) {
                    continue;
                }
                String previous = product.fields.putIfAbsent(field, value);
                if (previous != null && !previous.equals(value)) {
                    plan.error(PRODUCTS, row.row(), field,
                            "Khác với dòng đầu của sản phẩm %s (dòng %d); để trống hoặc ghi giống nhau".formatted(code, product.firstRow));
                }
            }
            checkVariant(row, product, skus, plan);
        }

        for (ProductPlan product : plan.products.values()) {
            checkProduct(product, categories, plan);
        }
        checkSpecs(specRows, plan);
    }

    private void checkVariant(Record row, ProductPlan product, Set<String> skus, Plan plan) {
        VariantPlan variant = new VariantPlan(row.row());
        variant.name = required(row, VARIANT, 255, plan);
        variant.ram = optional(row, RAM, 50, plan);
        variant.storage = optional(row, STORAGE, 50, plan);
        variant.color = optional(row, COLOR, 50, plan);
        variant.sku = required(row, SKU, 50, plan);
        variant.price = money(row, PRICE, true, plan);
        variant.discount = money(row, DISCOUNT, false, plan);
        if (variant.price != null && variant.price.signum() <= 0) {
            plan.error(PRODUCTS, row.row(), PRICE, "Giá phải lớn hơn 0");
        }
        if (variant.price != null && variant.discount != null && variant.discount.compareTo(variant.price) > 0) {
            plan.error(PRODUCTS, row.row(), DISCOUNT, "Giá KM không được lớn hơn giá");
        }
        if (variant.sku != null) {
            if (!skus.add(variant.sku.toLowerCase(Locale.ROOT))) {
                plan.error(PRODUCTS, row.row(), SKU, "SKU phiên bản bị trùng trong file");
            } else {
                variantRepository.findBySkuVariant(variant.sku).ifPresent(existing -> {
                    String owner = existing.getProduct().getSku();
                    if (existing.getProduct().getDeletedAt() != null || !product.code.equals(owner)) {
                        plan.error(PRODUCTS, row.row(), SKU, "SKU phiên bản đã thuộc sản phẩm khác (Mã SP %s)"
                                .formatted(owner == null ? "#" + existing.getProduct().getId() : owner));
                    } else {
                        variant.existing = existing;
                    }
                });
            }
        }
        product.variants.add(variant);
    }

    private void checkProduct(ProductPlan product, Map<String, Category> categories, Plan plan) {
        int row = product.firstRow;
        productRepository.findBySku(product.code).ifPresent(existing -> {
            if (existing.getDeletedAt() != null) {
                plan.error(PRODUCTS, row, CODE, "Mã sản phẩm thuộc một sản phẩm đã xoá, hãy dùng mã khác");
            } else {
                product.existing = existing;
            }
        });
        boolean isNew = product.existing == null;
        String name = product.fields.get(NAME);
        if (name == null && isNew) {
            plan.error(PRODUCTS, row, NAME, "Sản phẩm mới cần tên");
        } else if (name != null && name.length() > 255) {
            plan.error(PRODUCTS, row, NAME, "Tên tối đa 255 ký tự");
        }
        String category = product.fields.get(CATEGORY);
        if (category == null && isNew) {
            plan.error(PRODUCTS, row, CATEGORY, "Sản phẩm mới cần danh mục");
        } else if (category != null) {
            product.category = categories.get(norm(category));
            if (product.category == null) {
                plan.error(PRODUCTS, row, CATEGORY, "Không có danh mục \"%s\"; hãy tạo ở trang Danh mục trước".formatted(category));
            }
        }
        String brand = product.fields.get(BRAND);
        if (brand != null && brand.length() > 100) {
            plan.error(PRODUCTS, row, BRAND, "Thương hiệu tối đa 100 ký tự");
        }
        String warranty = product.fields.get(WARRANTY);
        if (warranty != null) {
            try {
                product.warranty = Integer.valueOf(warranty);
                if (product.warranty < 0 || product.warranty > 120) {
                    plan.error(PRODUCTS, row, WARRANTY, "Bảo hành từ 0 đến 120 tháng");
                }
            } catch (NumberFormatException e) {
                plan.error(PRODUCTS, row, WARRANTY, "Bảo hành phải là số tháng (vd 12)");
            }
        }
        String active = product.fields.get(ACTIVE);
        if (active != null) {
            String value = norm(active);
            if (Set.of("co", "x", "yes", "1", "true").contains(value)) {
                product.active = true;
            } else if (Set.of("khong", "no", "0", "false").contains(value)) {
                product.active = false;
            } else {
                plan.error(PRODUCTS, row, ACTIVE, "Đang bán chỉ nhận Có hoặc Không");
            }
        }
        String images = product.fields.get(IMAGES);
        if (images != null) {
            for (String link : images.split("\\|")) {
                String url = link.trim();
                if (url.isEmpty()) {
                    continue;
                }
                if (url.length() > 2048 || !url.matches("(?i)^https?://\\S+$")) {
                    plan.error(PRODUCTS, row, IMAGES, "Link ảnh phải bắt đầu bằng http:// hoặc https:// (\"%s\")".formatted(shorten(url)));
                } else if (!product.images.contains(url)) {
                    product.images.add(url);
                }
            }
        }
        if (isNew && product.images.isEmpty()) {
            plan.error(PRODUCTS, row, IMAGES, "Sản phẩm mới cần ít nhất 1 link ảnh");
        }
    }

    private void checkSpecs(List<Record> specRows, Plan plan) {
        for (Record row : specRows) {
            String code = row.get(CODE);
            String name = row.get(SPEC_NAME);
            String value = row.get(SPEC_VALUE);
            if (code == null) {
                plan.error(SPECS, row.row(), CODE, "Thiếu mã sản phẩm");
                continue;
            }
            if (name == null) {
                plan.error(SPECS, row.row(), SPEC_NAME, "Thiếu tên thông số");
            } else if (name.length() > 100) {
                plan.error(SPECS, row.row(), SPEC_NAME, "Tên thông số tối đa 100 ký tự");
            }
            if (value == null) {
                plan.error(SPECS, row.row(), SPEC_VALUE, "Thiếu giá trị");
            }
            ProductPlan product = plan.products.get(code);
            if (product == null) {
                // a product that is only in ThongSo: must exist already
                Product existing = productRepository.findBySku(code).filter(p -> p.getDeletedAt() == null).orElse(null);
                if (existing == null) {
                    plan.error(SPECS, row.row(), CODE, "Không có sản phẩm mã %s (trong sheet SanPham hoặc hệ thống)".formatted(code));
                    continue;
                }
                product = plan.specOnly.computeIfAbsent(code, ProductPlan::new);
                product.existing = existing;
            }
            if (name != null && value != null) {
                product.specs.add(new String[]{name, value});
            }
        }
    }

    // ---------- apply ----------

    private Map<String, Integer> apply(Plan plan) {
        Map<String, Integer> summary = new LinkedHashMap<>();
        for (String key : List.of("productsCreated", "productsUpdated", "variantsCreated", "variantsUpdated",
                "imagesAdded", "specsWritten", "brandsCreated")) {
            summary.put(key, 0);
        }
        Set<String> slugs = new HashSet<>();
        for (ProductPlan item : plan.products.values()) {
            Product product = item.existing != null ? item.existing : new Product();
            boolean isNew = item.existing == null;
            String name = item.fields.get(NAME);
            if (name != null) {
                product.setName(name);
            }
            if (item.category != null) {
                product.setCategory(item.category);
            }
            String brand = item.fields.get(BRAND);
            if (brand != null) {
                product.setBrand(brandFor(brand, plan, summary));
            }
            if (item.fields.containsKey(DESCRIPTION)) {
                product.setDescription(item.fields.get(DESCRIPTION));
            }
            if (item.warranty != null) {
                product.setWarrantyMonths(item.warranty);
            }
            if (item.active != null) {
                product.setActive(item.active);
            }
            VariantPlan cheapest = item.variants.stream().min(Comparator.comparing(v -> effective(v.price, v.discount))).orElseThrow();
            if (isNew) {
                product.setSku(item.code);
                product.setSlug(uniqueSlug(product.getName(), slugs));
                product.setBasePrice(cheapest.price);
                product.setDiscountPrice(cheapest.discount);
            }
            product = productRepository.save(product);
            summary.merge(isNew ? "productsCreated" : "productsUpdated", 1, Integer::sum);

            for (VariantPlan v : item.variants) {
                ProductVariant variant = v.existing != null ? v.existing : new ProductVariant();
                variant.setProduct(product);
                variant.setVariantName(v.name);
                variant.setSkuVariant(v.sku);
                variant.setPrice(v.price);
                variant.setDiscountPrice(v.discount);
                if (v.color != null) {
                    variant.setColor(v.color);
                }
                if (v.storage != null) {
                    variant.setStorage(v.storage);
                }
                if (v.ram != null) {
                    variant.setRam(v.ram);
                }
                variantRepository.save(variant);
                summary.merge(v.existing == null ? "variantsCreated" : "variantsUpdated", 1, Integer::sum);
            }
            if (!isNew) {
                // the card price follows the cheapest variant the product has now
                variantRepository.flush();
                variantRepository.findAllByProductIdOrderByIdAsc(product.getId()).stream()
                        .min(Comparator.comparing(v -> effective(v.getPrice(), v.getDiscountPrice())))
                        .ifPresent(min -> {
                            item.existing.setBasePrice(min.getPrice());
                            item.existing.setDiscountPrice(min.getDiscountPrice());
                        });
            }
            summary.merge("imagesAdded", addImages(product, item.images, isNew), Integer::sum);
            writeSpecs(product, item, summary);
            evict(product.getId());
        }
        for (ProductPlan item : plan.specOnly.values()) {
            writeSpecs(item.existing, item, summary);
            evict(item.existing.getId());
        }
        cache.evictAll(ReadCache.CATEGORY_BRANDS, ReadCache.BRANDS);
        return summary;
    }

    private int addImages(Product product, List<String> urls, boolean isNew) {
        List<ProductImage> current = isNew ? List.of() : imageRepository.findAllByProductIdOrderByDisplayOrderAscIdAsc(product.getId());
        Set<String> known = current.stream().map(ProductImage::getImageUrl).collect(Collectors.toSet());
        boolean hasPrimary = current.stream().anyMatch(i -> Boolean.TRUE.equals(i.getPrimary()));
        int order = current.stream().map(ProductImage::getDisplayOrder).filter(o -> o != null).max(Integer::compare).orElse(-1);
        int added = 0;
        for (String url : urls) {
            if (known.contains(url)) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(url);
            image.setAltText(product.getName());
            image.setDisplayOrder(++order);
            image.setPrimary(!hasPrimary);
            hasPrimary = true;
            imageRepository.save(image);
            added++;
        }
        return added;
    }

    private void writeSpecs(Product product, ProductPlan item, Map<String, Integer> summary) {
        if (item.specs.isEmpty()) {
            return;
        }
        specificationRepository.deleteAllByProductId(product.getId());
        specificationRepository.flush();
        int order = 0;
        for (String[] spec : item.specs) {
            ProductSpecification entity = new ProductSpecification();
            entity.setProduct(product);
            entity.setSpecName(spec[0]);
            entity.setSpecValue(spec[1]);
            entity.setSpecOrder(order++);
            specificationRepository.save(entity);
        }
        summary.merge("specsWritten", item.specs.size(), Integer::sum);
    }

    private Brand brandFor(String name, Plan plan, Map<String, Integer> summary) {
        Brand brand = plan.brands.get(norm(name));
        if (brand != null) {
            return brand;
        }
        brand = new Brand();
        brand.setName(name);
        String base = SlugUtil.toSlug(name).isEmpty() ? "thuong-hieu" : SlugUtil.toSlug(name);
        String slug = base;
        for (int i = 2; brandRepository.existsBySlug(slug); i++) {
            slug = base + "-" + i;
        }
        brand.setSlug(slug);
        brand = brandRepository.save(brand);
        plan.brands.put(norm(name), brand);
        summary.merge("brandsCreated", 1, Integer::sum);
        return brand;
    }

    private String uniqueSlug(String name, Set<String> taken) {
        String base = SlugUtil.toSlug(name);
        if (base.isEmpty()) {
            base = "san-pham";
        }
        base = base.length() > 240 ? base.substring(0, 240) : base;
        String slug = base;
        for (int i = 2; taken.contains(slug) || productRepository.existsBySlug(slug); i++) {
            slug = base + "-" + i;
        }
        taken.add(slug);
        return slug;
    }

    private void evict(Long productId) {
        cache.evict(ReadCache.PRODUCT, productId);
        cache.evict(ReadCache.VARIANTS, productId);
        cache.evict(ReadCache.IMAGES, productId);
        cache.evict(ReadCache.SPECS, productId);
        filterService.reindex(productId);
    }

    // ---------- helpers ----------

    private static String required(Record row, String column, int max, Plan plan) {
        String value = row.get(column);
        if (value == null) {
            plan.error(PRODUCTS, row.row(), column, "Bắt buộc");
            return null;
        }
        return optional(row, column, max, plan);
    }

    private static String optional(Record row, String column, int max, Plan plan) {
        String value = row.get(column);
        if (value != null && value.length() > max) {
            plan.error(PRODUCTS, row.row(), column, "Tối đa %d ký tự".formatted(max));
            return null;
        }
        return value;
    }

    /** "9990000", "9.990.000", "9,990,000đ" → 9990000 (whole đồng). */
    private static BigDecimal money(Record row, String column, boolean required, Plan plan) {
        String value = row.get(column);
        if (value == null) {
            if (required) {
                plan.error(PRODUCTS, row.row(), column, "Bắt buộc");
            }
            return null;
        }
        String digits = value.replaceAll("(?i)[\\s.,đ]|vnd", "");
        try {
            BigDecimal amount = new BigDecimal(digits);
            if (amount.signum() < 0 || amount.compareTo(MAX_PRICE) > 0 || amount.stripTrailingZeros().scale() > 0) {
                throw new NumberFormatException();
            }
            return amount;
        } catch (NumberFormatException e) {
            plan.error(PRODUCTS, row.row(), column, "Phải là số tiền (đồng), vd 9990000");
            return null;
        }
    }

    private static BigDecimal effective(BigDecimal price, BigDecimal discount) {
        BigDecimal base = price == null ? BigDecimal.ZERO : price;
        return discount != null && discount.compareTo(base) < 0 ? discount : base;
    }

    private static String norm(String value) {
        return SlugUtil.toSlug(value).replace("-", "");
    }

    private static String shorten(String value) {
        return value.length() > 60 ? value.substring(0, 57) + "..." : value;
    }

    private static final class Plan {
        final Map<String, ProductPlan> products = new LinkedHashMap<>();
        final Map<String, ProductPlan> specOnly = new LinkedHashMap<>();
        final List<ExcelImportResponse.Error> errors = new ArrayList<>();
        Map<String, Brand> brands = new HashMap<>();

        void error(String sheet, int row, String column, String message) {
            errors.add(new ExcelImportResponse.Error(sheet, row, column, message));
        }
    }

    private static final class ProductPlan {
        final String code;
        final Map<String, String> fields = new HashMap<>();
        final List<VariantPlan> variants = new ArrayList<>();
        final List<String> images = new ArrayList<>();
        final List<String[]> specs = new ArrayList<>();
        int firstRow;
        Product existing;
        Category category;
        Integer warranty;
        Boolean active;

        ProductPlan(String code) {
            this.code = code;
        }
    }

    private static final class VariantPlan {
        final int row;
        String name;
        String ram;
        String storage;
        String color;
        String sku;
        BigDecimal price;
        BigDecimal discount;
        ProductVariant existing;

        VariantPlan(int row) {
            this.row = row;
        }
    }
}
