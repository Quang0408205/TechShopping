/* ================= CHI TIẾT ĐƠN HÀNG (Phase 4) ================= */

/*
 * customer/order-detail.html?id=<id đơn>[&justPlaced=1]: GET /api/v1/orders/{id}
 * (fetchMyOrder, js/core/order-store.js). Đơn của tài khoản khác, id sai hoặc
 * không tồn tại → "Không tìm thấy đơn hàng" (API trả 404). Lỗi khác → hộp lỗi
 * + "Thử lại". Đơn còn "Chờ xác nhận" có nút "Hủy đơn hàng" (POST /cancel).
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


        const params = new URLSearchParams(window.location.search);

        const orderId = params.get("id");

        const loadingBox = document.getElementById("orderLoading");

        const errorBox = document.getElementById("orderError");

        const cancelButton = document.getElementById("orderCancelBtn");

        let currentOrder = null;


        /* id phải là số (mã cũ "DH12345678" của đơn mô phỏng không còn tồn tại) */
        if (!orderId || !/^\d{1,18}$/.test(orderId)) {

            showNotFound();

            return;

        }


        errorBox.addEventListener("click", function (event) {

            if (event.target.closest("#orderRetryBtn")) {
                loadOrder();
            }

        });

        cancelButton.addEventListener("click", confirmCancel);


        await loadOrder();

        if (currentOrder && params.get("justPlaced") === "1") {
            document.getElementById("justPlacedBanner").hidden = false;
        }


        async function loadOrder() {

            loadingBox.hidden = false;

            errorBox.hidden = true;


            let order;

            try {

                order = await fetchMyOrder(orderId);

            } catch (error) {

                loadingBox.hidden = true;

                if (!isLoggedIn()) {

                    redirectToLogin();

                    return;

                }

                errorBox.innerHTML = errorStateHtml(getErrorMessage(error), "orderRetryBtn");

                errorBox.hidden = false;

                return;

            }


            loadingBox.hidden = true;

            if (!order) {

                showNotFound();

                return;

            }

            currentOrder = order;

            renderOrder(order);

        }


        function showNotFound() {

            loadingBox.hidden = true;

            document.getElementById("orderNotFound").hidden = false;

        }


        function confirmCancel() {

            if (!currentOrder || !currentOrder.cancellable) {
                return;
            }

            openConfirmModal({
                title: "Hủy đơn hàng?",
                message: "Đơn " + currentOrder.code + " sẽ bị hủy và không khôi phục được. Sản phẩm không được đưa lại vào giỏ hàng.",
                confirmLabel: "HỦY ĐƠN",
                onConfirm: async function () {

                    cancelButton.disabled = true;

                    try {

                        currentOrder = await cancelMyOrder(currentOrder.id);

                        renderOrder(currentOrder);

                        showToast("Đã hủy đơn hàng " + currentOrder.code + ".", "success");

                    } catch (error) {

                        showToast(getErrorMessage(error), "error");

                        /* Trạng thái đã đổi (ví dụ cửa hàng vừa xác nhận): tải lại đơn */
                        await loadOrder();

                    } finally {

                        cancelButton.disabled = false;

                    }

                }
            });

        }

    }
);


function renderOrder(order) {

    document.getElementById("orderDetailBox").hidden = false;

    document.getElementById("pageTitle").textContent = `Đơn hàng ${order.code} - POY`;

    document.getElementById("orderId").textContent = `Đơn hàng ${order.code}`;

    document.getElementById("orderDate").textContent = "Đặt lúc " + formatOrderDateTime(order.orderDate);


    const statusBadge = document.getElementById("orderStatusBadge");

    statusBadge.textContent = getOrderStatusLabel(order.status);

    statusBadge.className = "order-status-badge status-" + String(order.status).toLowerCase();


    renderTimeline(order);


    document.getElementById("orderItemsList").innerHTML =
        order.items.map(function (item) {

            return `
                <div class="order-item-row">
                    <img src="${escapeHtml(cartItemImageUrl(item))}" alt="${escapeHtml(item.name)}">
                    <div class="order-item-info">
                        <a href="${escapeHtml(getProductDetailUrl(item.productId))}">
                            ${escapeHtml(item.name)}
                        </a>
                        <span>
                            ${escapeHtml(item.variantLabel ? item.variantLabel + " · " : "")}${formatPrice(item.price)} × ${item.quantity}
                            ${item.oldPrice ? `<s class="order-item-old-price">${formatPrice(item.oldPrice)}</s>` : ""}
                        </span>
                    </div>
                    <strong>${formatPrice(item.lineTotal)}</strong>
                </div>
            `;

        }).join("");


    document.getElementById("orderShippingInfo").textContent =
        order.recipientName + " - " + order.recipientPhone + " - " + order.shippingAddress;

    document.getElementById("orderPaymentMethod").textContent =
        getPaymentMethodLabel(order.paymentMethod);

    document.getElementById("orderTracking").textContent = order.trackingNumber;

    document.getElementById("orderTrackingBlock").hidden = !order.trackingNumber;

    document.getElementById("orderNote").textContent = order.note;

    document.getElementById("orderNoteBlock").hidden = !order.note;

    document.getElementById("orderSubtotal").textContent = formatPrice(order.subtotal);

    document.getElementById("orderShippingFee").textContent =
        order.shippingFee === 0 ? "Miễn phí" : formatPrice(order.shippingFee);

    document.getElementById("orderTotal").textContent = formatPrice(order.total);

    document.getElementById("orderActions").hidden = !order.cancellable;

}


/* Đơn đã hủy: chỉ hiện dòng "Đã hủy lúc …" thay cho các bước xử lý */

function renderTimeline(order) {

    const container = document.getElementById("orderTimeline");

    const cancelledNote = document.getElementById("orderCancelledNote");


    if (order.status === "CANCELLED") {

        container.hidden = true;

        cancelledNote.textContent = "Đơn hàng đã được hủy"
            + (order.cancelledAt ? " lúc " + formatOrderDateTime(order.cancelledAt) : "") + ".";

        cancelledNote.hidden = false;

        return;

    }


    cancelledNote.hidden = true;

    container.hidden = false;

    const currentIndex = ORDER_STATUS_FLOW.findIndex(function (step) {
        return step.code === order.status;
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
