/* ================= ĐƠN HÀNG MÔ PHỎNG (F2) ================= */

/*
 * Chưa có backend Order (Phase 4), nên đơn hàng khách đặt ở trang Thanh toán
 * được lưu tạm trên trình duyệt, theo TÀI KHOẢN: localStorage
 * "poy_orders_<userId>". Không có đơn hàng mẫu dựng sẵn (các đơn mẫu của bản
 * frontend mới tham chiếu sản phẩm giả): trang Đơn hàng chỉ hiện đơn do chính
 * người dùng đặt. Trạng thái luôn là "Chờ xác nhận" cho tới khi có backend.
 *
 * CHỜ BACKEND: createLocalOrder → POST /api/v1/orders, getOrders → GET /orders,
 * getOrderById → GET /orders/{id}; giữ cùng hình dạng dữ liệu.
 * Cần nạp sau js/core/api.js.
 */

const ORDERS_STORAGE_PREFIX = "poy_orders_";

const ORDER_STATUS_FLOW = [
    { code: "PENDING", label: "Chờ xác nhận" },
    { code: "CONFIRMED", label: "Đã xác nhận" },
    { code: "SHIPPING", label: "Đang giao hàng" },
    { code: "DELIVERED", label: "Đã giao hàng" }
];

const PAYMENT_METHOD_LABELS = {
    COD: "Thanh toán khi nhận hàng (COD)",
    BANK_TRANSFER: "Chuyển khoản ngân hàng",
    INSTALLMENT: "Trả góp qua thẻ tín dụng"
};


function getOrdersStorageKey() {

    const user = isLoggedIn() ? getCurrentUser() : null;

    return user && user.id !== undefined && user.id !== null
        ? ORDERS_STORAGE_PREFIX + user.id
        : null;

}


/* Đơn hàng của người đang đăng nhập, mới nhất trước */

function getOrders() {

    const key = getOrdersStorageKey();

    if (!key) {
        return [];
    }


    try {

        const orders = JSON.parse(localStorage.getItem(key) || "[]");

        return Array.isArray(orders)
            ? orders.slice().sort(function (a, b) {
                return new Date(b.createdAt) - new Date(a.createdAt);
            })
            : [];

    } catch (error) {

        return [];

    }

}


function getOrderById(id) {

    return getOrders().find(function (order) {
        return order.id === id;
    }) || null;

}


/*
 * orderInput: { recipientName, recipientPhone, shippingAddress, note,
 *               paymentMethod, items, subtotal, shippingFee, total }
 * Mã đơn "DH" + 8 số theo thời điểm đặt (không trùng trong một tài khoản).
 */

function createLocalOrder(orderInput) {

    const key = getOrdersStorageKey();

    if (!key) {
        return null;
    }


    const orders = getOrders();

    let id = "DH" + String(Date.now()).slice(-8);

    while (orders.some(function (order) { return order.id === id; })) {
        id = "DH" + String(Number(id.slice(2)) + 1).padStart(8, "0");
    }


    const order = Object.assign({}, orderInput, {
        id: id,
        createdAt: new Date().toISOString(),
        status: "PENDING"
    });

    orders.unshift(order);

    localStorage.setItem(key, JSON.stringify(orders));

    return order;

}


function getOrderStatusLabel(statusCode) {

    const step = ORDER_STATUS_FLOW.find(function (s) {
        return s.code === statusCode;
    });

    return step ? step.label : statusCode;

}


function getPaymentMethodLabel(code) {

    return PAYMENT_METHOD_LABELS[code] || code;

}
