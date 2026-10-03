/* ================= ĐƠN HÀNG (Phase 4): ĐƠN TRÊN SERVER ================= */

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

const PAYMENT_METHOD_LABELS = {
    COD: "Thanh toán khi nhận hàng (COD)",
    BANK_TRANSFER: "Chuyển khoản ngân hàng",
    INSTALLMENT: "Trả góp qua thẻ tín dụng"
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
 * input: { recipientName, recipientPhone, shippingAddress, note, paymentMethod }
 * Không gửi sản phẩm / giá: server lấy từ giỏ hàng của tài khoản.
 */

async function placeOrder(input) {

    const order = await apiRequest("/orders", { method: "POST", body: input, auth: true });

    return toOrderView(order);

}


/* { orders, page, totalPages, totalElements } */

async function fetchMyOrders(page) {

    const params = new URLSearchParams({
        page: String(page || 0),
        size: String(ORDERS_PAGE_SIZE)
    });

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
            lineTotal: Number(item.subtotal) || 0
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
        note: order.note || "",
        items: items,
        totalQuantity: order.totalQuantity || 0,
        subtotal: Number(order.subtotal) || 0,
        shippingFee: Number(order.shippingFee) || 0,
        total: Number(order.total) || 0,
        trackingNumber: order.trackingNumber || "",
        orderDate: order.orderDate,
        deliveredAt: order.deliveredAt,
        cancelledAt: order.cancelledAt,
        cancellable: order.cancellable === true
    };

}


function getOrderDetailUrl(orderId, justPlaced) {

    return siteUrl("customer/order-detail.html?id=" + encodeURIComponent(orderId) + (justPlaced ? "&justPlaced=1" : ""));

}


function getOrderStatusLabel(statusCode) {

    return ORDER_STATUS_LABELS[statusCode] || statusCode;

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
