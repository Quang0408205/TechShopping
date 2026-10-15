/* ================= THANH TOÁN / TRẢ GÓP: DÙNG CHUNG KHU ADMIN (Phase 5) ================= */

/*
 * Nhãn, huy hiệu và bảng lịch trả góp cho admin/orders.html và admin/installments.html.
 * Dữ liệu là PaymentResponse / InstallmentResponse thô của API (số tiền là chuỗi / số).
 * Mọi dữ liệu đưa vào innerHTML đều escape. Cần nạp sau js/core/ui.js và js/admin/admin-layout.js.
 */

const TRANSACTION_ID_MAX_LENGTH = 100;

const REJECTION_REASON_MAX_LENGTH = 1000;

/* [nhãn, class huy hiệu] */
const ADMIN_PAYMENT_STATUS = {
    PENDING: ["Chờ thanh toán", "admin-badge-warning"],
    PAID: ["Đã thanh toán", "admin-badge-success"],
    REFUND_PENDING: ["Chờ hoàn tiền", "admin-badge-danger"],
    REFUNDED: ["Đã hoàn tiền", "admin-badge-neutral"],
    CANCELLED: ["Đã hủy", "admin-badge-neutral"]
};

const ADMIN_INSTALLMENT_STATUS = {
    PENDING_APPROVAL: ["Chờ duyệt", "admin-badge-warning"],
    APPROVED: ["Đã duyệt", "admin-badge-info"],
    REJECTED: ["Từ chối", "admin-badge-danger"],
    ACTIVE: ["Đang trả góp", "admin-badge-info"],
    COMPLETED: ["Đã trả đủ", "admin-badge-success"],
    CANCELLED: ["Đã hủy", "admin-badge-neutral"]
};


function adminBadgeHtml(entry, fallback) {

    const label = entry ? entry[0] : fallback;

    const css = entry ? entry[1] : "admin-badge-neutral";

    return `<span class="admin-badge ${css}">${escapeHtml(label)}</span>`;

}


/* Huy hiệu thanh toán của một đơn (OrderResponse): hợp đồng trả góp, hoặc khoản thanh toán chính */

function orderPaymentBadgeHtml(order) {

    if (order.installment) {
        return adminBadgeHtml(ADMIN_INSTALLMENT_STATUS[order.installment.status], order.installment.status);
    }

    if (!order.payment) {
        return "";
    }

    if (order.paymentMethod === "COD" && order.payment.status === "PENDING") {
        return adminBadgeHtml(["Thu khi giao", "admin-badge-neutral"]);
    }

    return adminBadgeHtml(ADMIN_PAYMENT_STATUS[order.payment.status], order.payment.status);

}


function installmentBadgeHtml(installment) {

    return adminBadgeHtml(ADMIN_INSTALLMENT_STATUS[installment.status], installment.status);

}


/* "6 tháng · 1.666.666đ/kỳ (kỳ cuối 1.666.670đ)" */

function installmentTermText(installment) {

    const monthly = Number(installment.monthlyPayment);

    const last = Number(installment.lastPayment);

    return installment.numMonths + " tháng · " + formatPrice(monthly) + "/kỳ"
        + (last !== monthly ? " (kỳ cuối " + formatPrice(last) + ")" : "");

}


/* Bảng lịch các kỳ; trống (trước khi giao hàng) → câu giải thích */

function installmentScheduleHtml(installment) {

    const periods = installment.periods || [];

    if (periods.length === 0) {

        return installment.status === "PENDING_APPROVAL" || installment.status === "APPROVED"
            ? `<p class="admin-field-hint">Lịch các kỳ được tạo khi đơn chuyển sang Đã giao hàng (kỳ đầu đến hạn sau 1 tháng).</p>`
            : "";

    }


    const rows = periods.map(function (period) {

        const state = period.overdue ? "overdue" : String(period.status).toLowerCase();

        const status = period.status === "PAID"
            ? "Đã thu " + formatDateVi(period.paidDate)
            : period.overdue ? "Quá hạn" : "Chưa thu";

        return `
            <tr class="admin-period admin-period--${escapeHtml(state)}" data-period="${escapeHtml(String(period.number))}">
                <td>Kỳ ${escapeHtml(String(period.number))}</td>
                <td>${escapeHtml(formatDateVi(period.dueDate))}</td>
                <td>${escapeHtml(formatPrice(Number(period.amount)))}</td>
                <td>${escapeHtml(status)}</td>
            </tr>
        `;

    }).join("");


    return `
        <table class="admin-table admin-period-table">
            <thead>
                <tr><th>Kỳ</th><th>Hạn</th><th>Số tiền</th><th>Trạng thái</th></tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>
    `;

}


/* Hộp nhập mã giao dịch (không bắt buộc) trước khi ghi nhận tiền */

function openPaymentConfirmModal(options) {

    openConfirmModal({
        title: options.title,
        message: options.message,
        confirmLabel: options.confirmLabel || "XÁC NHẬN",
        cancelLabel: "Quay lại",
        input: {
            label: "Mã giao dịch (không bắt buộc)",
            placeholder: "VD: FT26276000123",
            maxLength: TRANSACTION_ID_MAX_LENGTH
        },
        onConfirm: function (transactionId) {
            options.onConfirm(transactionId ? { transactionId: transactionId } : {});
        }
    });

}
