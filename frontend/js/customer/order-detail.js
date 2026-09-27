/* ================= CHI TIẾT ĐƠN HÀNG (F2, mô phỏng) ================= */

/*
 * customer/order-detail.html?id=<mã đơn>[&justPlaced=1]: đơn của tài khoản
 * đang đăng nhập (js/core/order-store.js). Đơn của tài khoản khác hoặc mã sai
 * → "Không tìm thấy đơn hàng". Mọi dữ liệu đưa vào innerHTML đều escape.
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


        const params = new URLSearchParams(window.location.search);

        const orderId = params.get("id");

        const order = orderId ? getOrderById(orderId) : null;


        if (!order) {

            document.getElementById("orderNotFound").hidden = false;

            return;

        }


        if (params.get("justPlaced") === "1") {
            document.getElementById("justPlacedBanner").hidden = false;
        }

        renderOrder(order);

    }
);


function renderOrder(order) {

    document.getElementById("orderDetailBox").hidden = false;

    document.getElementById("pageTitle").textContent = `Đơn hàng ${order.id} - POY`;

    document.getElementById("orderId").textContent = `Đơn hàng ${order.id}`;

    document.getElementById("orderDate").textContent =
        new Date(order.createdAt).toLocaleString("vi-VN");


    const statusBadge = document.getElementById("orderStatusBadge");

    statusBadge.textContent = getOrderStatusLabel(order.status);

    statusBadge.className = "order-status-badge status-" + String(order.status).toLowerCase();


    renderTimeline(order.status);


    document.getElementById("orderItemsList").innerHTML =
        order.items.map(function (item) {

            return `
                <div class="order-item-row">
                    <img src="${escapeHtml(cartItemImageUrl(item))}" alt="${escapeHtml(item.name)}">
                    <div class="order-item-info">
                        <a href="${escapeHtml(getProductDetailUrl(item.productId))}">
                            ${escapeHtml(item.name)}
                        </a>
                        <span>${escapeHtml(item.variantLabel ? item.variantLabel + " × " : "× ")}${item.quantity}</span>
                    </div>
                    <strong>${formatPrice(item.price * item.quantity)}</strong>
                </div>
            `;

        }).join("");


    document.getElementById("orderShippingInfo").textContent =
        (order.recipientName ? order.recipientName + " - " : "") +
        (order.recipientPhone ? order.recipientPhone + " - " : "") +
        order.shippingAddress;

    document.getElementById("orderPaymentMethod").textContent =
        getPaymentMethodLabel(order.paymentMethod);

    document.getElementById("orderSubtotal").textContent = formatPrice(order.subtotal);

    document.getElementById("orderShippingFee").textContent =
        order.shippingFee === 0 ? "Miễn phí" : formatPrice(order.shippingFee);

    document.getElementById("orderTotal").textContent = formatPrice(order.total);

}


function renderTimeline(currentStatus) {

    const container = document.getElementById("orderTimeline");

    const currentIndex = ORDER_STATUS_FLOW.findIndex(function (step) {
        return step.code === currentStatus;
    });


    container.innerHTML = ORDER_STATUS_FLOW.map(function (step, index) {

        const state =
            index < currentIndex ? "done" :
                index === currentIndex ? "current" : "pending";

        return `
            <div class="timeline-step timeline-${state}">
                <span class="timeline-dot"></span>
                <span class="timeline-label">${escapeHtml(step.label)}</span>
            </div>
        `;

    }).join("");

}
