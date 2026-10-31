package com.example.Tech.service.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRouteTest {

    @Test
    void create_update_delete_getTheirLabelFromTheMethod() {
        assertThat(AuditRoute.of("POST", "/api/v1/admin/suppliers")).isEqualTo(new AuditRoute("suppliers", null, "Tạo · Nhà cung cấp"));
        assertThat(AuditRoute.of("PUT", "/api/v1/products/42")).isEqualTo(new AuditRoute("products", "42", "Cập nhật · Sản phẩm"));
        assertThat(AuditRoute.of("PATCH", "/api/v1/admin/users/7/status").label()).isEqualTo("Đổi trạng thái · Người dùng");
        assertThat(AuditRoute.of("DELETE", "/api/v1/categories/3")).isEqualTo(new AuditRoute("categories", "3", "Xoá · Danh mục"));
    }

    @Test
    void aVerbAfterTheIdNamesTheAction() {
        assertThat(AuditRoute.of("POST", "/api/v1/admin/orders/12/confirm")).isEqualTo(new AuditRoute("orders", "12", "Xác nhận · Đơn hàng"));
        assertThat(AuditRoute.of("POST", "/api/v1/admin/stock-transfers/5/receive")).isEqualTo(new AuditRoute("stock-transfers", "5", "Nhận hàng · Chuyển kho"));
        assertThat(AuditRoute.of("PUT", "/api/v1/users/me/password")).isEqualTo(new AuditRoute("users", null, "Đổi mật khẩu · Người dùng"));
    }

    @Test
    void theIdBelongsToTheLastEntityOfTheNestedPath() {
        assertThat(AuditRoute.of("POST", "/api/v1/admin/stores/3/purchase-receipts"))
                .isEqualTo(new AuditRoute("purchase-receipts", null, "Tạo · Phiếu nhập kho"));
        assertThat(AuditRoute.of("POST", "/api/v1/admin/stores/3/purchase-receipts/12/complete"))
                .isEqualTo(new AuditRoute("purchase-receipts", "12", "Hoàn tất · Phiếu nhập kho"));
        assertThat(AuditRoute.of("POST", "/api/v1/admin/stores/3/inventory/stock-in"))
                .isEqualTo(new AuditRoute("inventory", null, "Nhập kho · Tồn kho"));
    }

    @Test
    void anUnknownPathStillGetsAnEntryFromItsRawNames() {
        AuditRoute route = AuditRoute.of("POST", "/api/v1/admin/something-new/9/launch");
        assertThat(route.entityType()).isEqualTo("something-new");
        assertThat(route.label()).isEqualTo("Tạo · something-new");
    }
}
