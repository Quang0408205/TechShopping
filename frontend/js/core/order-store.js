/* ================= ĐƠN HÀNG (Phase 4–5): ĐƠN TRÊN SERVER ================= */

/*
 * Đơn hàng lưu ở backend theo tài khoản: /api/v1/orders (cần đăng nhập).
 *   placeOrder(input)      POST /orders: đặt hàng từ GIỎ TRÊN SERVER (giá,
 *                          phí ship do server tính; giỏ được xoá sau khi đặt)
 *   fetchMyOrders(page)    GET  /orders?page=&size=  (mới nhất trước)
 *   fetchMyOrder(id)       GET  /orders/{id}  (không có / của người khác → null)
 *   cancelMyOrder(id)      POST /orders/{id}/cancel  (chỉ khi PENDING)
 * Mọi hàm trả về đơn đã đổi sang dạng dùng ở giao diện (toOrderView); lỗi API
 * thì ném ApiError (api.js).
 *
 * Đơn mô phỏng cũ trên trình duyệt ("poy_orders_<userId>", F2) được xoá im
 * lặng khi nạp file này (quyết định 2026-10-02).
 * Cần nạp sau js/core/api.js và js/core/cart-store.js (cartItemImageUrl).
 */

const LEGACY_ORDERS_STORAGE_PREFIX = "poy_orders_";

const ORDERS_PAGE_SIZE = 10;

/* Các bước của đơn đang xử lý (timeline); CANCELLED nằm ngoài luồng này */
const ORDER_STATUS_FLOW = [
    { code: "PENDING", label: "Chờ xác nhận" },
    { code: "CONFIRMED", label: "Đã xác nhận" },
    { code: "SHIPPING", label: "Đang giao hàng" },
    { code: "DELIVERED", label: "Đã giao hàng" }
];

const ORDER_STATUS_LABELS = {
    PENDING: "Chờ xác nhận",
    CONFIRMED: "Đã xác nhận",
    SHIPPING: "Đang giao hàng",
    DELIVERED: "Đã giao hàng",
    CANCELLED: "Đã hủy"
};

/* Phase 7: hình thức nhận hàng */
const DELIVERY_TYPE_LABELS = {
    HOME_DELIVERY: "Giao tận nhà",
    PICKUP: "Nhận tại cửa hàng"
};

const PAYMENT_METHOD_LABELS = {
    COD: "Thanh toán khi nhận hàng (COD)",
    BANK_TRANSFER: "Chuyển khoản ngân hàng",
    INSTALLMENT: "Trả góp 0% qua thẻ tín dụng",
    CARD: "Thẻ quốc tế (Visa / Mastercard)"
};

/* Phase 5: trạng thái thanh toán (COD / chuyển khoản) và hợp đồng trả góp; tone = màu huy hiệu */
const PAYMENT_STATUS_LABELS = {
    PENDING: { label: "Chờ thanh toán", tone: "pending" },
    PAID: { label: "Đã thanh toán", tone: "success" },
    REFUND_PENDING: { label: "Chờ hoàn tiền", tone: "warning" },
    REFUNDED: { label: "Đã hoàn tiền", tone: "muted" },
    CANCELLED: { label: "Đã hủy", tone: "muted" }
};

const INSTALLMENT_STATUS_LABELS = {
    PENDING_APPROVAL: { label: "Chờ duyệt trả góp", tone: "pending" },
    APPROVED: { label: "Đã duyệt trả góp", tone: "info" },
    REJECTED: { label: "Từ chối trả góp", tone: "danger" },
    ACTIVE: { label: "Đang trả góp", tone: "info" },
    COMPLETED: { label: "Đã trả góp xong", tone: "success" },
    CANCELLED: { label: "Đã hủy trả góp", tone: "muted" }
};

const INSTALLMENT_PERIOD_LABELS = {
    PENDING: "Chưa thanh toán",
    PAID: "Đã thanh toán"
};


removeLegacyOrders();


function removeLegacyOrders() {

    try {

        Object.keys(localStorage)
            .filter(function (key) {
                return key.indexOf(LEGACY_ORDERS_STORAGE_PREFIX) === 0;
            })
            .forEach(function (key) {
                localStorage.removeItem(key);
            });

    } catch (error) {
        /* localStorage bị chặn: không có gì để xoá */
    }

}


/*
 * input: { recipientName, recipientPhone, deliveryType (HOME_DELIVERY | PICKUP),
 *          shippingAddress (giao tận nhà) | pickupStoreId (nhận tại cửa hàng), note, paymentMethod,
 *          installment?: { months, citizenId, cardBank } (chỉ khi INSTALLMENT) }
 * Không gửi sản phẩm / giá: server lấy từ giỏ hàng của tài khoản.
 */

async function placeOrder(input) {

    const order = await apiRequest("/orders", { method: "POST", body: input, auth: true });

    return toOrderView(order);

}


/* { orders, page, totalPages, totalElements } */

async function fetchMyOrders(page, filter) {

    const params = new URLSearchParams({
        page: String(page || 0),
        size: String(ORDERS_PAGE_SIZE)
    });

    /* Kế hoạch v5 GĐ6: lọc theo trạng thái (lặp tham số) + từ khoá (mã đơn / tên sản phẩm) */
    ((filter && filter.statuses) || []).forEach(function (status) {
        params.append("status", status);
    });

    if (filter && filter.keyword) {
        params.set("keyword", filter.keyword);
    }

    const result = await apiRequest("/orders?" + params.toString(), { auth: true });

    return {
        orders: (result.content || []).map(toOrderView),
        page: result.page,
        totalPages: result.totalPages,
        totalElements: result.totalElements
    };

}


/* null khi đơn không tồn tại hoặc là của tài khoản khác (API trả 404) */

async function fetchMyOrder(id) {

    try {

        return toOrderView(await apiRequest("/orders/" + encodeURIComponent(id), { auth: true }));

    } catch (error) {

        if (error.status === 404) {
            return null;
        }

        throw error;

    }

}


async function cancelMyOrder(id) {

    const order = await apiRequest("/orders/" + encodeURIComponent(id) + "/cancel", {
        method: "POST", auth: true
    });

    return toOrderView(order);

}


/* Kế hoạch v5 GĐ6: số đơn theo tab lịch sử { all, pending, inProgress, delivered, cancelled } */

async function fetchOrderCounts() {

    return apiRequest("/orders/summary", { auth: true });

}


/*
 * "Mua lại" (đơn đã giao / đã huỷ): server thêm các sản phẩm còn bán vào giỏ với giá mới nhất. Có sản phẩm bị bỏ
 * qua → hộp thông báo liệt kê lý do rồi mới mở giỏ; không thêm được gì → chỉ báo lỗi.
 */

async function buyAgain(orderId, button) {

    if (button) {
        button.disabled = true;
    }

    let result;

    try {

        result = await apiRequest("/orders/" + encodeURIComponent(orderId) + "/reorder", { method: "POST", auth: true });

    } catch (error) {

        showToast(getErrorMessage(error), "error");

        if (button) {
            button.disabled = false;
        }

        return;

    }

    if (typeof setCartCount === "function") {
        setCartCount(result.cartQuantity);
    }

    const cartUrl = siteUrl("customer/cart.html");

    const skipped = result.skipped.map(function (line) {
        return line.productName + (line.variantName ? " (" + line.variantName + ")" : "") + ": " + line.reason;
    });

    if (result.added.length === 0) {

        showToast("Không thêm được sản phẩm nào vào giỏ. " + skipped.join("; "), "error");

        if (button) {
            button.disabled = false;
        }

        return;

    }

    if (skipped.length === 0) {

        window.location.href = cartUrl;

        return;

    }

    openConfirmModal({
        title: "Đã thêm " + result.added.length + " sản phẩm vào giỏ",
        message: "Một số sản phẩm không thêm được: " + skipped.join("; ") + ".",
        cancelLabel: "Ở lại",
        confirmLabel: "Xem giỏ hàng",
        onConfirm: function () {
            window.location.href = cartUrl;
        }
    });

    if (button) {
        button.disabled = false;
    }

}


/* Kế hoạch v5 GĐ5: "Huỷ thanh toán" — huỷ đơn chuyển khoản / thẻ chưa trả tiền, server đưa lại đúng sản phẩm vào giỏ */

async function cancelOrderPayment(id) {

    const order = await apiRequest("/orders/" + encodeURIComponent(id) + "/cancel-payment", {
        method: "POST", auth: true
    });

    return toOrderView(order);

}


/* Đơn chuyển khoản / thẻ mới nhất còn chờ tiền (trong hạn), hoặc null */

async function fetchAwaitingPaymentOrder() {

    const order = await apiRequest("/orders/awaiting-payment", { auth: true });

    return order ? toOrderView(order) : null;

}


/* Thẻ quốc tế (Stripe, chế độ Test): { checkoutUrl, paid } — Stripe đưa khách về returnUrl?id=…&card=paid|back */

async function startCardCheckout(id, returnUrl) {

    return apiRequest("/orders/" + encodeURIComponent(id) + "/payment/card-checkout", {
        method: "POST", auth: true, body: { returnUrl: returnUrl }
    });

}


/* Khách vừa quay về từ Stripe: server hỏi Stripe đã trả chưa (chạy cả khi không có webhook) */

async function refreshCardPayment(id) {

    return toOrderView(await apiRequest("/orders/" + encodeURIComponent(id) + "/payment/card-refresh", {
        method: "POST", auth: true
    }));

}


/* OrderResponse của API → dạng dùng ở giao diện (số tiền đổi sang Number) */

function toOrderView(order) {

    const items = (Array.isArray(order.items) ? order.items : []).map(function (item) {

        return {
            productId: item.productId,
            variantId: item.variantId,
            name: item.productName,
            variantLabel: item.variantName || "",
            image: isSafeImageUrl(item.imageUrl) ? item.imageUrl : null,
            price: Number(item.unitPrice) || 0,
            oldPrice: item.originalPrice !== null && item.originalPrice !== undefined
                ? Number(item.originalPrice)
                : null,
            quantity: item.quantity,
            lineTotal: Number(item.subtotal) || 0,
            comboDiscount: Number(item.comboDiscount) || 0
        };

    });


    return {
        id: order.id,
        code: order.code,
        status: order.status,
        paymentMethod: order.paymentMethod,
        recipientName: order.recipientName,
        recipientPhone: order.recipientPhone,
        shippingAddress: order.shippingAddress,
        deliveryType: order.deliveryType || "HOME_DELIVERY",
        storeName: order.storeName || "",
        note: order.note || "",
        items: items,
        totalQuantity: order.totalQuantity || 0,
        subtotal: Number(order.subtotal) || 0,
        comboDiscount: Number(order.comboDiscount) || 0,
        combos: Array.isArray(order.combos) ? order.combos : [],
        voucherCode: order.voucherCode || "",
        voucherDiscount: Number(order.voucherDiscount) || 0,
        pointsUsed: Number(order.pointsUsed) || 0,
        pointsDiscount: Number(order.pointsDiscount) || 0,
        pointsEarned: Number(order.pointsEarned) || 0,
        shippingFee: Number(order.shippingFee) || 0,
        total: Number(order.total) || 0,
        trackingNumber: order.trackingNumber || "",
        orderDate: order.orderDate,
        deliveredAt: order.deliveredAt,
        cancelledAt: order.cancelledAt,
        cancellable: order.cancellable === true,
        payment: toPaymentView(order.payment),
        installment: toInstallmentView(order.installment)
    };

}


/* Khoản thanh toán chính của đơn COD / chuyển khoản (null với đơn trả góp) */

function toPaymentView(payment) {

    if (!payment) {
        return null;
    }

    const transfer = payment.bankTransfer;

    return {
        status: payment.status,
        amount: Number(payment.amount) || 0,
        transactionId: payment.transactionId || "",
        paidAt: payment.paidAt,
        refundedAt: payment.refundedAt,
        /* chỉ có khi đơn chuyển khoản đang chờ tiền */
        bankTransfer: transfer ? {
            bankName: transfer.bankName || "",
            accountNumber: transfer.accountNumber || "",
            accountName: transfer.accountName || "",
            amount: Number(transfer.amount) || 0,
            transferContent: transfer.transferContent || "",
            qrImageUrl: isSafeImageUrl(transfer.qrImageUrl) ? transfer.qrImageUrl.trim() : null,
            payBefore: transfer.payBefore,
            /* chế độ demo: trang thanh toán tự báo đã nhận tiền sau 1 phút hiện QR */
            demoAutoPay: transfer.demoAutoPay === true,
            bankBin: transfer.bankBin || ""
        } : null,
        /* chỉ có khi đơn thẻ quốc tế đang chờ thanh toán */
        card: payment.card ? {
            testMode: payment.card.testMode === true,
            payBefore: payment.card.payBefore
        } : null
    };

}


/* Hợp đồng trả góp (null với đơn COD / chuyển khoản); CCCD đã được server che */

function toInstallmentView(installment) {

    if (!installment) {
        return null;
    }

    return {
        status: installment.status,
        numMonths: installment.numMonths,
        monthlyPayment: Number(installment.monthlyPayment) || 0,
        lastPayment: Number(installment.lastPayment) || 0,
        totalAmount: Number(installment.totalAmount) || 0,
        citizenId: installment.citizenId || "",
        cardBankName: installment.cardBankName || installment.cardBank || "",
        rejectionReason: installment.rejectionReason || "",
        reviewedAt: installment.reviewedAt,
        paidPeriods: installment.paidPeriods || 0,
        paidAmount: Number(installment.paidAmount) || 0,
        remainingAmount: Number(installment.remainingAmount) || 0,
        periods: (Array.isArray(installment.periods) ? installment.periods : []).map(function (period) {
            return {
                number: period.number,
                amount: Number(period.amount) || 0,
                dueDate: period.dueDate,
                paidDate: period.paidDate,
                status: period.status,
                overdue: period.overdue === true
            };
        })
    };

}


/*
 * Huy hiệu thanh toán của một đơn: { label, tone }. Đơn trả góp theo trạng thái
 * hợp đồng; COD chưa thu tiền thì ghi "Trả khi nhận hàng".
 */

function getPaymentBadge(order) {

    if (order.installment) {
        return INSTALLMENT_STATUS_LABELS[order.installment.status]
            || { label: order.installment.status, tone: "muted" };
    }

    if (!order.payment) {
        return null;
    }

    if (order.paymentMethod === "COD" && order.payment.status === "PENDING") {
        return { label: "Trả khi nhận hàng", tone: "muted" };
    }

    return PAYMENT_STATUS_LABELS[order.payment.status] || { label: order.payment.status, tone: "muted" };

}


function getInstallmentPeriodLabel(period) {

    if (period.overdue) {
        return "Quá hạn";
    }

    return INSTALLMENT_PERIOD_LABELS[period.status] || period.status;

}


/* Ngày "2026-11-03" (LocalDate từ server) → "03/11/2026"; tách chuỗi để không lệch múi giờ */

function formatOrderDate(value) {

    const match = typeof value === "string" ? /^(\d{4})-(\d{2})-(\d{2})/.exec(value) : null;

    return match ? match[3] + "/" + match[2] + "/" + match[1] : "";

}


function getOrderDetailUrl(orderId, justPlaced) {

    return siteUrl("customer/order-detail.html?id=" + encodeURIComponent(orderId) + (justPlaced ? "&justPlaced=1" : ""));

}


/* Trang thanh toán chuyển khoản (QR + nội dung) / thẻ quốc tế, tự cập nhật khi nhận được tiền */

function getPaymentUrl(orderId) {

    return siteUrl("customer/payment.html?id=" + encodeURIComponent(orderId));

}


function getOrderStatusLabel(statusCode) {

    return ORDER_STATUS_LABELS[statusCode] || statusCode;

}


function getDeliveryTypeLabel(code) {

    return DELIVERY_TYPE_LABELS[code] || DELIVERY_TYPE_LABELS.HOME_DELIVERY;

}


function getPaymentMethodLabel(code) {

    return PAYMENT_METHOD_LABELS[code] || code;

}


/* "2026-10-02T21:44:02" (giờ Việt Nam từ server) → "02/10/2026 21:44" */

function formatOrderDateTime(value) {

    const date = value ? new Date(value) : null;

    if (!date || Number.isNaN(date.getTime())) {
        return "";
    }

    return date.toLocaleString("vi-VN", {
        day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit"
    });

}
