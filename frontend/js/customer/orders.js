/* ================= LỊCH SỬ MUA HÀNG ================= */

/*
 * Đơn của tài khoản đang đăng nhập: GET /api/v1/orders (fetchMyOrders, js/core/order-store.js), mới nhất trước,
 * 10 đơn / trang. Tab theo trạng thái (Tất cả / Chờ xác nhận / Đang giao = đã xác nhận + đang giao / Đã giao /
 * Đã huỷ) kèm số đơn (GET /orders/summary), ô tìm theo mã đơn / tên sản phẩm; tab + từ khoá nằm trên địa chỉ
 * (?tab=&q=) để Back / tải lại giữ nguyên. Đơn đã giao / đã huỷ có nút "Mua lại" (buyAgain).
 * Chưa đăng nhập: account-sidebar.js đã chuyển tới trang đăng nhập. Mọi dữ liệu đưa vào innerHTML đều escape.
 */

const ORDER_TABS = {
    all: [],
    pending: ["PENDING"],
    processing: ["CONFIRMED", "SHIPPING"],
    delivered: ["DELIVERED"],
    cancelled: ["CANCELLED"]
};

const ORDER_SEARCH_DELAY_MS = 350;

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

        const tabs = document.getElementById("orderTabs");

        const search = document.getElementById("orderSearch");

        const params = new URLSearchParams(window.location.search);

        let currentTab = ORDER_TABS[params.get("tab")] ? params.get("tab") : "all";

        let currentPage = 0;

        let requestCounter = 0;

        let searchTimer = null;

        search.value = (params.get("q") || "").slice(0, 100);

        markTab();


        tabs.addEventListener("click", function (event) {

            const button = event.target.closest("button[data-tab]");

            if (!button || button.dataset.tab === currentTab) {
                return;
            }

            currentTab = button.dataset.tab;

            currentPage = 0;

            markTab();

            syncAddress();

            loadOrders();

        });

        search.addEventListener("input", function () {

            clearTimeout(searchTimer);

            searchTimer = setTimeout(function () {
                currentPage = 0;
                syncAddress();
                loadOrders();
            }, ORDER_SEARCH_DELAY_MS);

        });

        document.getElementById("orderSearchForm").addEventListener("submit", function (event) {

            event.preventDefault();

            clearTimeout(searchTimer);

            currentPage = 0;

            syncAddress();

            loadOrders();

        });

        container.addEventListener("click", function (event) {

            const pageButton = event.target.closest("button[data-page]");

            if (pageButton && !pageButton.disabled) {

                currentPage = Number(pageButton.dataset.page);

                loadOrders();

                container.scrollIntoView({ behavior: "smooth", block: "start" });

                return;

            }

            const again = event.target.closest("button[data-buy-again]");

            if (again) {
                buyAgain(again.dataset.buyAgain, again);
                return;
            }

            if (event.target.closest("#ordersRetryBtn")) {
                loadOrders();
            }

        });


        loadCounts();

        loadOrders();


        async function loadCounts() {

            let counts;

            try {
                counts = await fetchOrderCounts();
            } catch (error) {
                return;
            }

            tabs.querySelectorAll("[data-count]").forEach(function (badge) {
                const value = counts[badge.dataset.count] || 0;
                badge.textContent = value > 0 ? String(value) : "";
            });

        }


        async function loadOrders() {

            const requestId = ++requestCounter;

            container.innerHTML = `<div class="state-box"><p>Đang tải đơn hàng…</p></div>`;


            let result;

            try {

                result = await fetchMyOrders(currentPage, { statuses: ORDER_TABS[currentTab], keyword: search.value.trim() });

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

                const filtered = currentTab !== "all" || search.value.trim() !== "";

                container.innerHTML = filtered
                    ? `
                        <div class="state-box empty-state">
                            <h3>Không có đơn hàng phù hợp</h3>
                            <p>Thử chọn tab khác hoặc đổi từ khoá tìm kiếm.</p>
                        </div>
                    `
                    : `
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

            const payment = getPaymentBadge(order);

            const detailUrl = escapeHtml(getOrderDetailUrl(order.id));

            const canBuyAgain = order.status === "DELIVERED" || order.status === "CANCELLED";

            const shown = order.items.slice(0, 2);

            const more = order.items.length - shown.length;

            return `
                <article class="order-card order-card--${escapeHtml(String(order.status).toLowerCase())}" data-order-id="${escapeHtml(String(order.id))}">

                    <a href="${detailUrl}" class="order-card-header">
                        <div>
                            <strong>${escapeHtml(order.code)}</strong>
                            <span class="order-card-date">${escapeHtml(formatOrderDateTime(order.orderDate)
                                + (order.deliveryType === "PICKUP" ? " · " + getDeliveryTypeLabel(order.deliveryType) : ""))}</span>
                        </div>
                        <span class="order-status-badge status-${escapeHtml(String(order.status).toLowerCase())}">
                            ${escapeHtml(getOrderStatusLabel(order.status))}
                        </span>
                    </a>

                    <a href="${detailUrl}" class="order-card-lines">
                        ${shown.map(function (item) {
                            return `
                                <span class="order-card-line">
                                    <img src="${escapeHtml(cartItemImageUrl(item))}" alt="" referrerpolicy="no-referrer">
                                    <span class="order-card-line-text">
                                        <strong>${escapeHtml(item.name)}</strong>
                                        <small>${escapeHtml(item.variantLabel ? item.variantLabel + " · " : "")}x${escapeHtml(String(item.quantity))}</small>
                                    </span>
                                    <span class="order-card-line-price">${escapeHtml(formatPrice(item.lineTotal))}</span>
                                </span>
                            `;
                        }).join("")}
                        ${more > 0 ? `<span class="order-card-more">+ ${more} sản phẩm khác</span>` : ""}
                    </a>

                    <div class="order-card-footer">
                        <span class="order-card-total">
                            <span class="order-card-count">${order.totalQuantity} sản phẩm</span>
                            ${payment ? `<span class="payment-badge payment-badge--${escapeHtml(payment.tone)}">${escapeHtml(payment.label)}</span>` : ""}
                            ${order.pointsEarned > 0 && order.status !== "CANCELLED" ? `<span class="order-card-points">+${escapeHtml(new Intl.NumberFormat("vi-VN").format(order.pointsEarned))} điểm</span>` : ""}
                        </span>
                        <span class="order-card-sum">Tổng cộng <strong>${formatPrice(order.total)}</strong></span>
                        <span class="order-card-actions">
                            <a href="${detailUrl}" class="btn btn-outline-dark">Xem chi tiết</a>
                            ${canBuyAgain ? `<button type="button" class="btn btn-dark" data-buy-again="${escapeHtml(String(order.id))}">Mua lại</button>` : ""}
                        </span>
                    </div>

                </article>
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


        function markTab() {

            tabs.querySelectorAll("button[data-tab]").forEach(function (button) {
                const active = button.dataset.tab === currentTab;
                button.classList.toggle("is-active", active);
                button.setAttribute("aria-selected", active ? "true" : "false");
            });

        }


        function syncAddress() {

            const next = new URLSearchParams();

            if (currentTab !== "all") {
                next.set("tab", currentTab);
            }

            if (search.value.trim()) {
                next.set("q", search.value.trim());
            }

            history.replaceState(null, "", window.location.pathname + (next.toString() ? "?" + next.toString() : ""));

        }

    }
);
