/* ================= CHI TIẾT ĐƠN HÀNG ================= */

/*
 * customer/order-detail.html?id=<id đơn>[&justPlaced=1]: GET /api/v1/orders/{id}
 * (fetchMyOrder, js/core/order-store.js). Đơn của tài khoản khác, id sai hoặc
 * không tồn tại → "Không tìm thấy đơn hàng" (API trả 404). Lỗi khác → hộp lỗi
 * + "Thử lại". Đơn còn "Chờ xác nhận" có nút "Hủy đơn hàng" (POST /cancel).
 * Trạng thái thanh toán, thông tin chuyển khoản + mã QR (khi đang chờ
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

        document.getElementById("orderBuyAgainBtn").addEventListener("click", function () {
            buyAgain(orderId, this);
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

            initOrderAfterSales(order);

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


    document.getElementById("orderItemCount").textContent = "(" + order.items.length + ")";

    document.getElementById("orderItemsList").innerHTML =
        order.items.map(function (item) {

            return `
                <div class="order-item-row">
                    <img src="${escapeHtml(cartItemImageUrl(item))}" alt="${escapeHtml(item.name)}" referrerpolicy="no-referrer">
                    <div class="order-item-info">
                        <a href="${escapeHtml(getProductDetailUrl(item.productId))}">
                            ${escapeHtml(item.name)}
                        </a>
                        <span>
                            ${escapeHtml(item.variantLabel ? item.variantLabel + " · " : "")}${formatPrice(item.price)} × ${item.quantity}
                            ${item.oldPrice ? `<s class="order-item-old-price">${formatPrice(item.oldPrice)}</s>` : ""}
                        </span>
                        ${item.comboDiscount > 0 ? `<span class="combo-tag">Combo −${formatPrice(item.comboDiscount)}</span>` : ""}
                    </div>
                    <strong>${formatPrice(item.lineTotal)}</strong>
                </div>
            `;

        }).join("");


    renderDelivery(order);

    document.getElementById("orderPaymentMethod").textContent =
        getPaymentMethodLabel(order.paymentMethod);

    renderPayment(order);

    document.getElementById("orderTracking").textContent = order.trackingNumber;

    document.getElementById("orderTrackingBlock").hidden = !order.trackingNumber;

    document.getElementById("orderNote").textContent = order.note;

    document.getElementById("orderNoteBlock").hidden = !order.note;

    document.getElementById("orderSubtotal").textContent = formatPrice(order.subtotal);

    document.getElementById("orderComboRow").hidden = !(order.comboDiscount > 0);

    document.getElementById("orderComboNames").textContent = order.combos.map(function (combo) {
        return combo.name;
    }).join(", ");

    document.getElementById("orderComboDiscount").textContent = "−" + formatPrice(order.comboDiscount);

    document.getElementById("orderVoucherRow").hidden = !(order.voucherDiscount > 0);

    document.getElementById("orderVoucherCode").textContent = order.voucherCode;

    document.getElementById("orderVoucherDiscount").textContent = "−" + formatPrice(order.voucherDiscount);

    document.getElementById("orderShippingFee").textContent =
        order.shippingFee === 0 ? "Miễn phí" : formatPrice(order.shippingFee);

    document.getElementById("orderTotal").textContent = formatPrice(order.total);

    document.getElementById("orderPointsRow").hidden = !(order.pointsDiscount > 0);

    document.getElementById("orderPointsUsed").textContent = new Intl.NumberFormat("vi-VN").format(order.pointsUsed);

    document.getElementById("orderPointsDiscount").textContent = "−" + formatPrice(order.pointsDiscount);

    const earned = document.getElementById("orderPointsEarned");

    earned.hidden = !(order.pointsEarned > 0);

    earned.textContent = "Đơn này đã được cộng +" + new Intl.NumberFormat("vi-VN").format(order.pointsEarned) + " điểm tích luỹ.";

    document.getElementById("orderActions").hidden = !order.cancellable;

    document.getElementById("orderBuyAgain").hidden = order.status !== "DELIVERED" && order.status !== "CANCELLED";

}


/*
 * Giao tận nhà → người nhận + địa chỉ; nhận tại cửa hàng → người nhận +
 * cửa hàng (server ghi "Nhận tại cửa hàng: tên, địa chỉ…" vào địa chỉ đơn, và đổi
 * theo nếu cửa hàng ADMIN chuyển đơn sang). Chi nhánh xử lý đơn giao tận nhà là
 * việc nội bộ nên không hiện.
 */

const PICKUP_ADDRESS_PREFIX = "Nhận tại cửa hàng:";

function renderDelivery(order) {

    const pickup = order.deliveryType === "PICKUP";

    const pickupStore = document.getElementById("orderPickupStore");

    document.getElementById("orderShippingTitle").textContent = getDeliveryTypeLabel(order.deliveryType);

    document.getElementById("orderShippingInfo").textContent = pickup
        ? "Người nhận: " + order.recipientName + " - " + order.recipientPhone
        : order.recipientName + " - " + order.recipientPhone + " - " + order.shippingAddress;

    pickupStore.hidden = !pickup;

    if (pickup) {

        const address = String(order.shippingAddress || "");

        pickupStore.textContent = "Cửa hàng: " + (address.indexOf(PICKUP_ADDRESS_PREFIX) === 0
            ? address.slice(PICKUP_ADDRESS_PREFIX.length).trim()
            : address || order.storeName);

    }

}


/* ================= THANH TOÁN ================= */

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

    if (payment && order.paymentMethod === "CARD") {

        if (payment.status === "PENDING" && order.status === "PENDING") {
            return { tone: "pending", text: "Đơn hàng đang chờ thanh toán bằng thẻ. Cửa hàng sẽ xác nhận đơn sau khi bạn thanh toán." };
        }

        if (payment.status === "PAID" && order.status === "PENDING") {
            return { tone: "success", text: "Đã thanh toán bằng thẻ, đơn hàng đang chờ cửa hàng xác nhận." };
        }

        if (payment.status === "REFUND_PENDING") {
            return { tone: "warning", text: "Đơn hàng đã hủy. Số tiền đã thanh toán bằng thẻ sẽ được cửa hàng hoàn lại." };
        }

    }

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

        const partner = installment.partnerName || "Đối tác trả góp";

        if (installment.status === "PENDING_APPROVAL") {
            return { tone: "pending", text: partner + " đang xét hồ sơ trả góp của bạn. Cửa hàng sẽ xác nhận đơn ngay khi đối tác chấp thuận." };
        }

        if ((installment.status === "APPROVED" || installment.status === "SETTLED") && order.status === "PENDING") {
            return { tone: "success", text: partner + " đã duyệt hồ sơ trả góp, đơn hàng đang chờ cửa hàng xác nhận." };
        }

        if (installment.status === "REJECTED" && order.status === "PENDING") {
            return { tone: "danger", text: partner + " không duyệt hồ sơ trả góp. Bạn có thể gửi hồ sơ cho đối tác khác hoặc chọn cách thanh toán khác." };
        }

        if (installment.status === "ACTIVE" && installment.periods.some(function (period) { return period.overdue; })) {
            return { tone: "danger", text: "Có kỳ trả góp đã quá hạn. Vui lòng thanh toán sớm cho cửa hàng." };
        }

    }

    return null;

}


/* Khối chuyển khoản: chỉ có khi server gửi bankTransfer (đơn chuyển khoản đang chờ tiền); đơn thẻ chờ tiền dùng chung khối */

function renderTransfer(order) {

    const block = document.getElementById("orderTransferBlock");

    const transfer = order.payment ? order.payment.bankTransfer : null;

    const card = order.payment ? order.payment.card : null;

    block.hidden = !transfer && !card;

    if (card && order.status === "PENDING") {

        document.getElementById("orderTransferTitle").textContent = "Chờ thanh toán thẻ";

        document.getElementById("orderTransferPayLink").href = getPaymentUrl(order.id);

        document.getElementById("orderTransferPayLink").textContent = "THANH TOÁN BẰNG THẺ";

        document.getElementById("orderTransferFacts").innerHTML =
            factHtml("Số tiền", `<strong>${formatPrice(order.payment.amount)}</strong>`);

        document.getElementById("orderTransferDeadline").textContent = card.payBefore
            ? "Vui lòng thanh toán trước " + formatOrderDateTime(card.payBefore) + "."
            : "";

        return;

    }

    block.hidden = !transfer;

    if (!transfer) {
        return;
    }


    /* QR + sao chép nằm ở trang thanh toán riêng; ở đây chỉ nhắc số tiền / nội dung */
    document.getElementById("orderTransferPayLink").href = getPaymentUrl(order.id);

    document.getElementById("orderTransferFacts").innerHTML = [
        factHtml("Số tiền", `<strong>${formatPrice(transfer.amount)}</strong>`),
        factHtml("Nội dung", `<strong>${escapeHtml(transfer.transferContent)}</strong>`)
    ].join("");


    document.getElementById("orderTransferDeadline").textContent = transfer.payBefore
        ? "Vui lòng chuyển khoản trước " + formatOrderDateTime(transfer.payBefore) + "."
        : "";

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
        factHtml("Trạng thái", `<span class="payment-badge payment-badge--${escapeHtml(status.tone)}">${escapeHtml(status.label)}</span>`)
    ];

    if (installment.partnerName) {
        facts.push(factHtml("Đối tác", escapeHtml(installment.partnerName)));
    }

    facts.push(
        factHtml("Kỳ hạn", installment.numMonths + " tháng"),
        factHtml("Mỗi kỳ", escapeHtml(monthly)),
        factHtml("Tổng trả góp", formatPrice(installment.totalAmount) + " · lãi suất 0%")
    );

    if (installment.cardBankName) {
        facts.push(factHtml("Ngân hàng thẻ", escapeHtml(installment.cardBankName)));
    }

    facts.push(factHtml("Số CCCD", escapeHtml(installment.citizenId)));

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

    if (installment.status === "REJECTED" && order.status === "PENDING") {
        renderResubmitForm(order);
    }

}


/* Hồ sơ bị từ chối: gửi cho đối tác khác (đối tác nhận đơn này, kỳ hạn của đối tác, CCCD nhập lại) hoặc đổi cách thanh toán */

async function renderResubmitForm(order) {

    const container = document.getElementById("orderInstallmentSchedule");

    let options;

    try {

        options = await fetchInstallmentOptions(order.total);

    } catch (error) {

        container.innerHTML = "";

        return;

    }

    const partners = options.partners || [];

    container.innerHTML = `
        <form class="order-resubmit" id="orderResubmitForm" novalidate>
            <label for="resubmitPartner">Gửi hồ sơ cho đối tác khác</label>
            <select id="resubmitPartner">
                <option value="">-- Chọn đối tác --</option>
                ${partners.map(function (partner) {
                    return `<option value="${escapeHtml(partner.code)}">${escapeHtml(partner.name)}</option>`;
                }).join("")}
            </select>
            <label for="resubmitMonths">Kỳ hạn</label>
            <select id="resubmitMonths"><option value="">-- Chọn kỳ hạn --</option></select>
            <label for="resubmitCitizen">Số CCCD</label>
            <input type="text" id="resubmitCitizen" inputmode="numeric" autocomplete="off" maxlength="${options.citizenIdLength}">
            <div id="resubmitBankGroup" hidden>
                <label for="resubmitBank">Ngân hàng phát hành thẻ tín dụng</label>
                <select id="resubmitBank">
                    <option value="">-- Chọn ngân hàng --</option>
                    ${(options.banks || []).map(function (bank) {
                        return `<option value="${escapeHtml(bank.code)}">${escapeHtml(bank.name)}</option>`;
                    }).join("")}
                </select>
            </div>
            <p class="field-error" id="resubmitError" hidden></p>
            <div class="order-detail-actions">
                <button type="submit" class="btn btn-dark" id="resubmitBtn">GỬI HỒ SƠ</button>
                <button type="button" class="btn btn-outline-dark" id="resubmitOtherMethodBtn">CHỌN CÁCH THANH TOÁN KHÁC</button>
            </div>
        </form>
    `;

    const partnerSelect = document.getElementById("resubmitPartner");

    const monthsSelect = document.getElementById("resubmitMonths");

    const bankGroup = document.getElementById("resubmitBankGroup");

    const bankSelect = document.getElementById("resubmitBank");

    const citizenInput = document.getElementById("resubmitCitizen");

    const errorBox = document.getElementById("resubmitError");

    function chosenPartner() {
        return partners.find(function (partner) { return partner.code === partnerSelect.value; }) || null;
    }

    partnerSelect.addEventListener("change", function () {

        const partner = chosenPartner();

        monthsSelect.innerHTML = `<option value="">-- Chọn kỳ hạn --</option>` + (partner ? partner.termsInMonths : []).map(function (months) {
            return `<option value="${months}">${months} tháng</option>`;
        }).join("");

        bankGroup.hidden = !(partner && partner.requiresCardBank);

    });

    citizenInput.addEventListener("input", function () {
        citizenInput.value = citizenInput.value.replace(/\D/g, "");
    });

    function showError(message) {
        errorBox.textContent = message;
        errorBox.hidden = !message;
    }

    document.getElementById("orderResubmitForm").addEventListener("submit", async function (event) {

        event.preventDefault();

        const partner = chosenPartner();

        if (!partner) {
            return showError("Vui lòng chọn đối tác trả góp.");
        }

        if (!monthsSelect.value) {
            return showError("Vui lòng chọn kỳ hạn trả góp.");
        }

        if (citizenInput.value.length !== options.citizenIdLength) {
            return showError("Số CCCD phải gồm đúng " + options.citizenIdLength + " chữ số.");
        }

        if (partner.requiresCardBank && !bankSelect.value) {
            return showError("Vui lòng chọn ngân hàng phát hành thẻ.");
        }

        showError("");

        const button = document.getElementById("resubmitBtn");

        button.disabled = true;

        try {

            await resubmitInstallment(order.id, {
                partner: partner.code,
                months: Number(monthsSelect.value),
                citizenId: citizenInput.value,
                cardBank: partner.requiresCardBank ? bankSelect.value : null
            });

            showToast("Đã gửi hồ sơ cho " + partner.name + ".", "success");

            window.location.reload();

        } catch (error) {

            showError(getErrorMessage(error));

            button.disabled = false;

        }

    });

    document.getElementById("resubmitOtherMethodBtn").addEventListener("click", async function () {

        this.disabled = true;

        try {

            await cancelOrderPayment(order.id);

            window.location.href = "checkout.html";

        } catch (error) {

            showError(getErrorMessage(error));

            this.disabled = false;

        }

    });

}


function scheduleHtml(installment) {

    if (installment.periods.length === 0) {

        if (installment.status === "PENDING_APPROVAL" || installment.status === "APPROVED" || installment.status === "SETTLED") {
            return `<p class="order-installment-hint">Đối tác trả góp thu tiền hằng tháng của bạn theo hợp đồng với đối tác; cửa hàng không thu tiền trả góp.</p>`;
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
