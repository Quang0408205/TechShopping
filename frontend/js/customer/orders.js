/* ================= ĐƠN HÀNG CỦA TÔI (Phase 4) ================= */

/*
 * Danh sách đơn hàng của tài khoản đang đăng nhập: GET /api/v1/orders
 * (fetchMyOrders, js/core/order-store.js), mới nhất trước, 10 đơn / trang.
 * Chưa đăng nhập: account-sidebar.js đã chuyển tới trang đăng nhập.
 * Mọi dữ liệu đưa vào innerHTML đều escape.
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


        const container = document.getElementById("ordersList");

        let currentPage = 0;

        let requestCounter = 0;


        container.addEventListener("click", function (event) {

            const pageButton = event.target.closest("button[data-page]");

            if (pageButton && !pageButton.disabled) {

                currentPage = Number(pageButton.dataset.page);

                loadOrders();

                container.scrollIntoView({ behavior: "smooth", block: "start" });

                return;

            }

            if (event.target.closest("#ordersRetryBtn")) {
                loadOrders();
            }

        });


        loadOrders();


        async function loadOrders() {

            const requestId = ++requestCounter;

            container.innerHTML = `<div class="state-box"><p>Đang tải đơn hàng…</p></div>`;


            let result;

            try {

                result = await fetchMyOrders(currentPage);

            } catch (error) {

                if (requestId !== requestCounter) {
                    return;
                }

                if (!isLoggedIn()) {

                    redirectToLogin();

                    return;

                }

                container.innerHTML = errorStateHtml(getErrorMessage(error), "ordersRetryBtn");

                return;

            }


            if (requestId !== requestCounter) {
                return;
            }

            renderOrders(result);

        }


        function renderOrders(result) {

            if (result.totalElements === 0) {

                container.innerHTML = `
                    <div class="state-box empty-state">
                        <h3>Bạn chưa có đơn hàng nào</h3>
                        <p>Các đơn hàng bạn đặt sẽ xuất hiện tại đây.</p>
                        <a href="${escapeHtml(siteUrl("customer/products.html"))}" class="btn btn-dark">MUA SẮM NGAY</a>
                    </div>
                `;

                return;

            }


            container.innerHTML = result.orders.map(orderCardHtml).join("") + paginationHtml(result);

        }


        function orderCardHtml(order) {

            return `
                <a href="${escapeHtml(getOrderDetailUrl(order.id))}" class="order-card" data-order-id="${escapeHtml(String(order.id))}">

                    <div class="order-card-header">
                        <div>
                            <strong>${escapeHtml(order.code)}</strong>
                            <span class="order-card-date">${escapeHtml(formatOrderDateTime(order.orderDate))}</span>
                        </div>
                        <span class="order-status-badge status-${escapeHtml(String(order.status).toLowerCase())}">
                            ${escapeHtml(getOrderStatusLabel(order.status))}
                        </span>
                    </div>

                    <div class="order-card-items">
                        ${order.items.map(function (item) {
                            return `<img src="${escapeHtml(cartItemImageUrl(item))}" alt="${escapeHtml(item.name)}">`;
                        }).join("")}
                        <span class="order-card-count">${order.totalQuantity} sản phẩm</span>
                    </div>

                    <div class="order-card-footer">
                        <span>Tổng cộng</span>
                        <strong>${formatPrice(order.total)}</strong>
                    </div>

                </a>
            `;

        }


        function paginationHtml(result) {

            if (result.totalPages <= 1) {
                return "";
            }

            return `
                <div class="orders-pagination">
                    <button type="button" class="btn btn-outline-dark" data-page="${result.page - 1}" ${result.page <= 0 ? "disabled" : ""}>‹ TRƯỚC</button>
                    <span>Trang ${result.page + 1} / ${result.totalPages}</span>
                    <button type="button" class="btn btn-outline-dark" data-page="${result.page + 1}" ${result.page + 1 >= result.totalPages ? "disabled" : ""}>SAU ›</button>
                </div>
            `;

        }

    }
);
