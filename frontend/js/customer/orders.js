/* ================= ĐƠN HÀNG CỦA TÔI (F2, mô phỏng) ================= */

/*
 * Danh sách đơn hàng của tài khoản đang đăng nhập (js/core/order-store.js).
 * Chưa đăng nhập: account-sidebar.js đã chuyển tới trang đăng nhập.
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        if (typeof layoutReady !== "undefined") {
            await layoutReady;
        }

        if (!isLoggedIn()) {
            return;
        }

        renderOrdersList();

    }
);


function renderOrdersList() {

    const container = document.getElementById("ordersList");

    const orders = getOrders();


    if (orders.length === 0) {

        container.innerHTML = `
            <div class="state-box empty-state">
                <h3>Bạn chưa có đơn hàng nào</h3>
                <p>Các đơn hàng bạn đặt sẽ xuất hiện tại đây.</p>
                <a href="${escapeHtml(siteUrl("customer/products.html"))}" class="btn btn-dark">MUA SẮM NGAY</a>
            </div>
        `;

        return;

    }


    container.innerHTML = orders.map(function (order) {

        const count = order.items.reduce(function (sum, item) {
            return sum + item.quantity;
        }, 0);

        return `
            <a href="${escapeHtml(siteUrl("customer/order-detail.html?id=" + encodeURIComponent(order.id)))}" class="order-card">

                <div class="order-card-header">
                    <div>
                        <strong>${escapeHtml(order.id)}</strong>
                        <span class="order-card-date">${escapeHtml(formatOrderDate(order.createdAt))}</span>
                    </div>
                    <span class="order-status-badge status-${escapeHtml(String(order.status).toLowerCase())}">
                        ${escapeHtml(getOrderStatusLabel(order.status))}
                    </span>
                </div>

                <div class="order-card-items">
                    ${order.items.map(function (item) {
                        return `<img src="${escapeHtml(cartItemImageUrl(item))}" alt="${escapeHtml(item.name)}">`;
                    }).join("")}
                    <span class="order-card-count">${count} sản phẩm</span>
                </div>

                <div class="order-card-footer">
                    <span>Tổng cộng</span>
                    <strong>${formatPrice(order.total)}</strong>
                </div>

            </a>
        `;

    }).join("");

}


function formatOrderDate(isoString) {

    return new Date(isoString).toLocaleDateString("vi-VN", {
        day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit"
    });

}
