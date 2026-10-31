package com.example.Tech.service.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns the path of a write request into what an auditor reads: the kind of thing it touched, its id and a Vietnamese
 * label ("Xác nhận · Đơn hàng"). Works from the URL alone, so a new endpoint is logged without registering it anywhere;
 * unknown parts fall back to their raw name.
 */
public record AuditRoute(String entityType, String entityId, String label) {

    private static final String PREFIX = "/api/v1/";

    private static final Map<String, String> ENTITIES = Map.ofEntries(
            Map.entry("orders", "Đơn hàng"),
            Map.entry("installments", "Trả góp"),
            Map.entry("service-requests", "Bảo hành / bảo trì / đổi trả"),
            Map.entry("contact-requests", "Liên hệ khách hàng"),
            Map.entry("inventory", "Tồn kho"),
            Map.entry("purchase-receipts", "Phiếu nhập kho"),
            Map.entry("stock-transfers", "Chuyển kho"),
            Map.entry("suppliers", "Nhà cung cấp"),
            Map.entry("users", "Người dùng"),
            Map.entry("employees", "Nhân viên"),
            Map.entry("stores", "Chi nhánh"),
            Map.entry("products", "Sản phẩm"),
            Map.entry("product-variants", "Phiên bản sản phẩm"),
            Map.entry("product-images", "Ảnh sản phẩm"),
            Map.entry("product-specifications", "Thông số sản phẩm"),
            Map.entry("categories", "Danh mục"),
            Map.entry("brands", "Thương hiệu"),
            Map.entry("attributes", "Thuộc tính"),
            Map.entry("attribute-values", "Giá trị thuộc tính"),
            Map.entry("reviews", "Đánh giá"),
            Map.entry("promotions", "Khuyến mãi"),
            Map.entry("vouchers", "Voucher"),
            Map.entry("combos", "Combo"),
            Map.entry("loyalty", "Hạng thành viên"),
            Map.entry("bank-transactions", "Chuyển khoản ngân hàng"),
            Map.entry("installment-partners", "Đối tác trả góp"),
            Map.entry("uploads", "Tải ảnh lên"),
            Map.entry("wishlist", "Yêu thích"),
            Map.entry("reports", "Báo cáo"));

    private static final Map<String, String> VERBS = Map.ofEntries(
            Map.entry("confirm", "Xác nhận"),
            Map.entry("cancel", "Huỷ"),
            Map.entry("complete", "Hoàn tất"),
            Map.entry("send", "Gửi hàng"),
            Map.entry("receive", "Nhận hàng"),
            Map.entry("approve", "Duyệt"),
            Map.entry("reject", "Từ chối"),
            Map.entry("status", "Đổi trạng thái"),
            Map.entry("import", "Nhập từ Excel"),
            Map.entry("stock-in", "Nhập kho"),
            Map.entry("settle", "Đối soát"),
            Map.entry("hide", "Ẩn"),
            Map.entry("show", "Hiện"),
            Map.entry("assign", "Gán"),
            Map.entry("roles", "Phân quyền"),
            Map.entry("password", "Đổi mật khẩu"),
            Map.entry("reset-password", "Đặt lại mật khẩu"),
            Map.entry("ship", "Giao hàng"),
            Map.entry("deliver", "Đã giao"),
            Map.entry("refund", "Hoàn tiền"),
            Map.entry("reindex-filters", "Dựng lại bộ lọc"));

    /** Entity id: the number right after the entity in the path; none when the path carries no such number. */
    public static AuditRoute of(String method, String uri) {
        String path = uri.startsWith(PREFIX) ? uri.substring(PREFIX.length()) : uri;
        List<String> parts = new ArrayList<>(List.of(path.split("/")));
        parts.removeIf(String::isBlank);
        if (!parts.isEmpty() && (parts.getFirst().equals("admin") || parts.getFirst().equals("branch"))) {
            parts.removeFirst();
        }
        String entityId = null;
        String entity = null;
        String verb = null;
        int entityIndex = -1;
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if (isNumber(part)) {
                // only an id right after the entity belongs to it (stores/3/purchase-receipts/12: 12, not 3)
                if (i == entityIndex + 1) {
                    entityId = part;
                }
            } else if (ENTITIES.containsKey(part)) {
                entity = part;
                entityIndex = i;
                entityId = null;
                verb = null;
            } else if (entity != null) {
                verb = part;
            }
        }
        if (entity == null) {
            entity = parts.isEmpty() ? "other" : parts.stream().filter(p -> !isNumber(p)).findFirst().orElse("other");
        }
        String entityLabel = ENTITIES.getOrDefault(entity, entity);
        String action = verb != null ? VERBS.getOrDefault(verb, verb) : switch (method) {
            case "POST" -> "Tạo";
            case "DELETE" -> "Xoá";
            default -> "Cập nhật";
        };
        return new AuditRoute(entity, entityId, action + " · " + entityLabel);
    }

    /** Vietnamese name of an entity type, for the filter of the admin page. */
    public static String entityLabel(String entityType) {
        return ENTITIES.getOrDefault(entityType, entityType);
    }

    private static boolean isNumber(String part) {
        return !part.isEmpty() && part.chars().allMatch(Character::isDigit);
    }
}
