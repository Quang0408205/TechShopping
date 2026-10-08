/* ================= BẢO HÀNH / BẢO TRÌ / ĐỔI TRẢ: nhãn dùng chung (khách + quản trị) — Phase 6 ================= */

const AFTER_SALES_TYPE_LABELS = {
    WARRANTY: "Bảo hành",
    MAINTENANCE: "Bảo trì",
    RETURN: "Trả hàng"
};

/* Bảo hành / bảo trì và trả hàng dùng chung một số mã trạng thái nhưng nghĩa khác nhau */
const AFTER_SALES_REPAIR_STATUS_LABELS = {
    PENDING: "Chờ tiếp nhận",
    RECEIVED: "Đã nhận máy",
    PROCESSING: "Đang xử lý",
    COMPLETED: "Hoàn tất",
    REJECTED: "Từ chối",
    CANCELLED: "Khách đã huỷ"
};

const AFTER_SALES_RETURN_STATUS_LABELS = {
    PENDING: "Chờ duyệt",
    APPROVED: "Đã duyệt",
    RECEIVED: "Đã nhận hàng",
    REFUNDED: "Đã hoàn tiền",
    REJECTED: "Từ chối",
    CANCELLED: "Khách đã huỷ"
};

const AFTER_SALES_MAINTENANCE_TYPE_LABELS = {
    CLEANING: "Vệ sinh",
    SOFTWARE: "Cài đặt / phần mềm",
    REPAIR: "Sửa chữa",
    OTHER: "Khác"
};

const AFTER_SALES_RETURN_REASON_LABELS = {
    DEFECTIVE: "Lỗi kỹ thuật",
    NOT_AS_DESCRIBED: "Không đúng mô tả",
    CHANGED_MIND: "Không ưng ý",
    OTHER: "Khác"
};

const AFTER_SALES_FINAL_STATUSES = ["COMPLETED", "REFUNDED", "REJECTED", "CANCELLED"];


function afterSalesStatusLabel(type, status) {

    const labels = type === "RETURN" ? AFTER_SALES_RETURN_STATUS_LABELS : AFTER_SALES_REPAIR_STATUS_LABELS;

    return labels[status] || status;

}


/* "success" | "danger" | "neutral" | "warning" | "info" — hậu tố class huy hiệu */

function afterSalesStatusTone(status) {

    if (status === "COMPLETED" || status === "REFUNDED") {
        return "success";
    }

    if (status === "REJECTED") {
        return "danger";
    }

    if (status === "CANCELLED") {
        return "neutral";
    }

    return status === "PENDING" ? "warning" : "info";

}


/* Tên hiển thị của các sản phẩm trong một yêu cầu: "iPhone 15 (Đen)" hoặc "iPhone 15 (Đen) + 1 sản phẩm" */

function afterSalesItemsSummary(request) {

    const items = request.items || [];

    if (items.length === 0) {
        return "";
    }

    const first = items[0].productName + (items[0].variantName ? " (" + items[0].variantName + ")" : "");

    return items.length > 1 ? first + " + " + (items.length - 1) + " sản phẩm" : first;

}


/* "2026-10-15" → "15/10/2026" (ngày thuần, không qua Date nên không lệch múi giờ) */

function afterSalesDate(value) {

    const parts = String(value || "").split("-");

    return parts.length === 3 ? Number(parts[2]) + "/" + Number(parts[1]) + "/" + parts[0] : "";

}
