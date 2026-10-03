/* ================= CHI TIẾT ĐƠN HÀNG (Phase 4–5) ================= */

/*
 * customer/order-detail.html?id=<id đơn>[&justPlaced=1]: GET /api/v1/orders/{id}
 * (fetchMyOrder, js/core/order-store.js). Đơn của tài khoản khác, id sai hoặc
 * không tồn tại → "Không tìm thấy đơn hàng" (API trả 404). Lỗi khác → hộp lỗi
 * + "Thử lại". Đơn còn "Chờ xác nhận" có nút "Hủy đơn hàng" (POST /cancel).
 * Phase 5: trạng thái thanh toán, thông tin chuyển khoản + mã QR (khi đang chờ
 * tiền), hợp đồng trả góp + lịch các kỳ, lý do từ chối trả góp.
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

        document.getElementById("orderTransferBlock").addEventListener("click", copyTransferValue);

        /* Không tải được ảnh QR (không có mạng / dịch vụ VietQR lỗi): chỉ còn thông tin chữ */
        document.getElementById("orderTransferQr").addEventListener("error", function () {

            this.hidden = true;

            document.getElementById("orderTransferQrError").hidden = false;

        });


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

            const paid = currentOrder.payment && currentOrder.payment.status === "PAID";

            openConfirmModal({
                title: "Hủy đơn hàng?",
                message: "Đơn " + currentOrder.code + " sẽ bị hủy và không khôi phục được. Sản phẩm không được đưa lại vào giỏ hàng."
                    + (paid ? " Số tiền bạn đã chuyển sẽ được cửa hàng hoàn lại." : ""),
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


        async function copyTransferValue(event) {

            const button = event.target.closest("button[data-copy]");

            if (!button) {
                return;
            }

            try {

                await navigator.clipboard.writeText(button.dataset.copy);

                showToast("Đã sao chép " + button.dataset.copyLabel + ".", "success");

            } catch (error) {

                showToast("Trình duyệt không cho sao chép, vui lòng chép thủ công.", "error");

            }

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

    renderPayment(order);

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


/* ================= PHASE 5: THANH TOÁN ================= */

function renderPayment(order) {

    const badge = document.getElementById("orderPaymentBadge");

    const paymentBadge = getPaymentBadge(order);

    badge.hidden = !paymentBadge;

    if (paymentBadge) {
        badge.textContent = paymentBadge.label;
        badge.className = "payment-badge payment-badge--" + paymentBadge.tone;
    }


    const info = document.getElementById("orderPaymentInfo");

    info.textContent = paymentInfoText(order.payment);

    info.hidden = !info.textContent;


    renderPaymentNotice(order);

    renderTransfer(order);

    renderInstallment(order);

}


function paymentInfoText(payment) {

    if (!payment) {
        return "";
    }

    const reference = payment.transactionId ? " (mã giao dịch " + payment.transactionId + ")" : "";

    if (payment.status === "PAID") {
        return "Đã thanh toán lúc " + formatOrderDateTime(payment.paidAt) + reference + ".";
    }

    if (payment.status === "REFUND_PENDING") {
        return "Cửa hàng đã nhận " + formatPrice(payment.amount) + reference + " và sẽ hoàn lại cho bạn.";
    }

    if (payment.status === "REFUNDED") {
        return "Đã hoàn " + formatPrice(payment.amount) + " lúc " + formatOrderDateTime(payment.refundedAt) + ".";
    }

    return "";

}


/* Một câu báo phía trên chi tiết: khách cần làm gì / đơn đang chờ gì */

function renderPaymentNotice(order) {

    const notice = document.getElementById("orderPaymentNotice");

    const message = paymentNotice(order);

    notice.hidden = !message;

    if (message) {
        notice.textContent = message.text;
        notice.className = "order-payment-notice order-payment-notice--" + message.tone;
    }

}


function paymentNotice(order) {

    const payment = order.payment;

    const installment = order.installment;

    if (payment && order.paymentMethod === "BANK_TRANSFER") {

        if (payment.status === "PENDING" && order.status === "PENDING") {
            return { tone: "pending", text: "Đơn hàng đang chờ chuyển khoản. Cửa hàng sẽ xác nhận đơn sau khi nhận được tiền." };
        }

        if (payment.status === "PAID" && order.status === "PENDING") {
            return { tone: "success", text: "Cửa hàng đã nhận được tiền chuyển khoản, đơn hàng đang chờ xác nhận." };
        }

        if (payment.status === "REFUND_PENDING") {
            return { tone: "warning", text: "Đơn hàng đã hủy. Số tiền bạn đã chuyển sẽ được cửa hàng hoàn lại." };
        }

    }

    if (installment) {

        if (installment.status === "PENDING_APPROVAL") {
            return { tone: "pending", text: "Hồ sơ trả góp đang chờ duyệt. Nhân viên sẽ gọi điện cho bạn để xác minh." };
        }

        if (installment.status === "APPROVED") {
            return { tone: "info", text: "Hồ sơ trả góp đã được duyệt. Lịch trả góp bắt đầu từ ngày bạn nhận hàng." };
        }

        if (installment.status === "REJECTED") {
            return { tone: "danger", text: "Hồ sơ trả góp không được duyệt nên đơn hàng đã bị hủy." };
        }

        if (installment.status === "ACTIVE" && installment.periods.some(function (period) { return period.overdue; })) {
            return { tone: "danger", text: "Có kỳ trả góp đã quá hạn. Vui lòng thanh toán sớm cho cửa hàng." };
        }

    }

    return null;

}


/* Khối chuyển khoản: chỉ có khi server gửi bankTransfer (đơn chuyển khoản đang chờ tiền) */

function renderTransfer(order) {

    const block = document.getElementById("orderTransferBlock");

    const transfer = order.payment ? order.payment.bankTransfer : null;

    block.hidden = !transfer;

    if (!transfer) {
        return;
    }


    const qr = document.getElementById("orderTransferQr");

    document.getElementById("orderTransferQrError").hidden = true;

    qr.hidden = !transfer.qrImageUrl;

    if (transfer.qrImageUrl && qr.getAttribute("src") !== transfer.qrImageUrl) {
        qr.src = transfer.qrImageUrl;
    }


    document.getElementById("orderTransferFacts").innerHTML = [
        factHtml("Ngân hàng", escapeHtml(transfer.bankName)),
        factHtml("Số tài khoản", escapeHtml(transfer.accountNumber) + copyButtonHtml(transfer.accountNumber, "số tài khoản")),
        factHtml("Chủ tài khoản", escapeHtml(transfer.accountName)),
        factHtml("Số tiền", `<strong>${formatPrice(transfer.amount)}</strong>`),
        factHtml("Nội dung", `<strong>${escapeHtml(transfer.transferContent)}</strong>`
            + copyButtonHtml(transfer.transferContent, "nội dung chuyển khoản"))
    ].join("");


    document.getElementById("orderTransferDeadline").textContent = transfer.payBefore
        ? "Vui lòng chuyển khoản trước " + formatOrderDateTime(transfer.payBefore)
            + " và ghi đúng nội dung để cửa hàng đối chiếu."
        : "Vui lòng ghi đúng nội dung để cửa hàng đối chiếu.";

}


function renderInstallment(order) {

    const block = document.getElementById("orderInstallmentBlock");

    const installment = order.installment;

    block.hidden = !installment;

    if (!installment) {
        return;
    }


    const status = INSTALLMENT_STATUS_LABELS[installment.status] || { label: installment.status, tone: "muted" };

    const monthly = formatPrice(installment.monthlyPayment)
        + (installment.lastPayment !== installment.monthlyPayment
            ? " (kỳ cuối " + formatPrice(installment.lastPayment) + ")"
            : "");

    const facts = [
        factHtml("Trạng thái", `<span class="payment-badge payment-badge--${escapeHtml(status.tone)}">${escapeHtml(status.label)}</span>`),
        factHtml("Kỳ hạn", installment.numMonths + " tháng"),
        factHtml("Mỗi kỳ", escapeHtml(monthly)),
        factHtml("Tổng trả góp", formatPrice(installment.totalAmount) + " · lãi suất 0%"),
        factHtml("Ngân hàng thẻ", escapeHtml(installment.cardBankName)),
        factHtml("Số CCCD", escapeHtml(installment.citizenId))
    ];

    if (installment.status === "ACTIVE" || installment.status === "COMPLETED") {
        facts.push(factHtml("Đã trả", installment.paidPeriods + "/" + installment.numMonths + " kỳ · "
            + formatPrice(installment.paidAmount)));
        facts.push(factHtml("Còn lại", `<strong>${formatPrice(installment.remainingAmount)}</strong>`));
    }

    document.getElementById("orderInstallmentFacts").innerHTML = facts.join("");


    const reason = document.getElementById("orderInstallmentReason");

    reason.textContent = installment.rejectionReason ? "Lý do từ chối: " + installment.rejectionReason : "";

    reason.hidden = !installment.rejectionReason;


    document.getElementById("orderInstallmentSchedule").innerHTML = scheduleHtml(installment);

}


function scheduleHtml(installment) {

    if (installment.periods.length === 0) {

        if (installment.status === "PENDING_APPROVAL" || installment.status === "APPROVED") {
            return `<p class="order-installment-hint">Lịch trả góp được tạo khi bạn nhận hàng; kỳ đầu tiên đến hạn sau 1 tháng.</p>`;
        }

        return "";

    }


    const rows = installment.periods.map(function (period) {

        const state = period.overdue ? "overdue" : String(period.status).toLowerCase();

        const paid = period.paidDate ? " · " + formatOrderDate(period.paidDate) : "";

        return `
            <tr class="installment-period installment-period--${escapeHtml(state)}">
                <td>Kỳ ${period.number}</td>
                <td>${escapeHtml(formatOrderDate(period.dueDate))}</td>
                <td>${formatPrice(period.amount)}</td>
                <td>${escapeHtml(getInstallmentPeriodLabel(period) + paid)}</td>
            </tr>
        `;

    }).join("");


    return `
        <table class="order-installment-table">
            <thead>
                <tr><th>Kỳ</th><th>Hạn thanh toán</th><th>Số tiền</th><th>Trạng thái</th></tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>
    `;

}


/* valueHtml phải là HTML đã escape */

function factHtml(label, valueHtml) {

    return `<div class="order-fact"><dt>${escapeHtml(label)}</dt><dd>${valueHtml}</dd></div>`;

}


function copyButtonHtml(value, label) {

    return `<button type="button" class="order-copy-btn" data-copy="${escapeHtml(value)}" data-copy-label="${escapeHtml(label)}">Sao chép</button>`;

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
