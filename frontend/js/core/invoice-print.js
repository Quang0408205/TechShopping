/* Hoá đơn / phiếu giao hàng để in */

const INVOICE_PAYMENT_LABELS = {
    COD: "Thanh toán khi nhận hàng (COD)",
    BANK_TRANSFER: "Chuyển khoản ngân hàng",
    INSTALLMENT: "Trả góp qua đối tác",
    CARD: "Thẻ quốc tế"
};


function invoiceMoney(value) {

    return formatPrice(Number(value) || 0);

}


function renderInvoiceHtml(order, kind) {

    const delivery = kind === "delivery";

    const home = order.deliveryType !== "PICKUP";

    const paid = order.payment ? order.payment.status === "PAID" : order.status === "DELIVERED";

    const collect = order.paymentMethod === "COD" && !paid ? Number(order.total) : 0;


    const company = !delivery && order.invoiceCompany
        ? `
            <section class="invoice-block">
                <h2>Đơn vị mua hàng</h2>
                <p><strong>${escapeHtml(order.invoiceCompany)}</strong></p>
                <p>Mã số thuế: ${escapeHtml(order.invoiceTaxCode)}</p>
                <p>Địa chỉ: ${escapeHtml(order.invoiceAddress)}</p>
            </section>
        `
        : "";


    const rows = (order.items || []).map(function (item, index) {

        const name = escapeHtml(item.productName) + (item.variantName ? ` <span class="invoice-muted">(${escapeHtml(item.variantName)})</span>` : "");

        return delivery
            ? `<tr><td>${index + 1}</td><td>${name}</td><td class="invoice-num">${escapeHtml(String(item.quantity))}</td></tr>`
            : `<tr><td>${index + 1}</td><td>${name}</td><td class="invoice-num">${escapeHtml(String(item.quantity))}</td>`
                + `<td class="invoice-num">${escapeHtml(invoiceMoney(item.unitPrice))}</td>`
                + `<td class="invoice-num">${escapeHtml(invoiceMoney(item.subtotal))}</td></tr>`;

    }).join("");


    const totals = delivery ? "" : `
        <table class="invoice-totals">
            <tr><td>Tạm tính</td><td>${escapeHtml(invoiceMoney(order.subtotal))}</td></tr>
            ${Number(order.comboDiscount) > 0 ? `<tr><td>Giảm combo</td><td>−${escapeHtml(invoiceMoney(order.comboDiscount))}</td></tr>` : ""}
            ${Number(order.voucherDiscount) > 0 ? `<tr><td>Voucher ${escapeHtml(order.voucherCode || "")}</td><td>−${escapeHtml(invoiceMoney(order.voucherDiscount))}</td></tr>` : ""}
            ${Number(order.pointsDiscount) > 0 ? `<tr><td>Điểm tích luỹ (${escapeHtml(String(order.pointsUsed))} điểm)</td><td>−${escapeHtml(invoiceMoney(order.pointsDiscount))}</td></tr>` : ""}
            <tr><td>Phí giao hàng</td><td>${Number(order.shippingFee) > 0 ? escapeHtml(invoiceMoney(order.shippingFee)) : "Miễn phí"}</td></tr>
            <tr class="invoice-grand"><td>Tổng thanh toán</td><td>${escapeHtml(invoiceMoney(order.total))}</td></tr>
        </table>
    `;


    return `
        <header class="invoice-head">
            <div>
                <div class="invoice-brand">POY</div>
                <div class="invoice-muted">Đỉnh cao công nghệ</div>
                ${order.storeName ? `<div class="invoice-muted">Chi nhánh: ${escapeHtml(order.storeName)}</div>` : ""}
            </div>
            <div class="invoice-title">
                <h1>${delivery ? "PHIẾU GIAO HÀNG" : "HOÁ ĐƠN BÁN HÀNG"}</h1>
                <div>Mã đơn: <strong>${escapeHtml(order.code)}</strong></div>
                <div>Ngày đặt: ${escapeHtml(invoiceDate(order.orderDate))}</div>
                ${!delivery && order.deliveredAt ? `<div>Ngày giao: ${escapeHtml(invoiceDate(order.deliveredAt))}</div>` : ""}
            </div>
        </header>

        <section class="invoice-block">
            <h2>${delivery ? "Người nhận" : "Khách hàng"}</h2>
            <p><strong>${escapeHtml(order.recipientName)}</strong> · ${escapeHtml(order.recipientPhone)}</p>
            <p>${home ? "Giao tận nhà: " : "Nhận tại cửa hàng: "}${escapeHtml(order.shippingAddress || "—")}</p>
            ${delivery && order.trackingNumber ? `<p>Mã vận đơn: <strong>${escapeHtml(order.trackingNumber)}</strong></p>` : ""}
        </section>

        ${company}

        <table class="invoice-items">
            <thead>
                <tr>
                    <th>STT</th><th>Sản phẩm</th><th class="invoice-num">SL</th>
                    ${delivery ? "" : '<th class="invoice-num">Đơn giá</th><th class="invoice-num">Thành tiền</th>'}
                </tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>

        ${totals}

        <section class="invoice-block">
            <p>Thanh toán: ${escapeHtml(INVOICE_PAYMENT_LABELS[order.paymentMethod] || order.paymentMethod)}${paid ? " — đã thanh toán" : ""}</p>
            ${delivery ? `<p class="invoice-collect">Tiền cần thu khi giao: <strong>${escapeHtml(invoiceMoney(collect))}</strong></p>` : ""}
            ${order.note ? `<p>Ghi chú của khách: ${escapeHtml(order.note)}</p>` : ""}
        </section>

        <footer class="invoice-foot">
            ${delivery
                ? '<div><strong>Người giao hàng</strong><br><span class="invoice-muted">(ký, ghi rõ họ tên)</span></div><div><strong>Người nhận hàng</strong><br><span class="invoice-muted">(ký, ghi rõ họ tên)</span></div>'
                : '<div><strong>Người mua hàng</strong><br><span class="invoice-muted">(ký, ghi rõ họ tên)</span></div><div><strong>Người bán hàng</strong><br><span class="invoice-muted">(ký, ghi rõ họ tên)</span></div>'}
        </footer>
    `;

}


/* dd/mm/yyyy hh:mm từ chuỗi thời gian của API (các trang khách không nạp admin-layout.js) */

function invoiceDate(value) {

    const date = new Date(value);

    if (!value || Number.isNaN(date.getTime())) {
        return "—";
    }

    const two = function (n) { return String(n).padStart(2, "0"); };

    return two(date.getDate()) + "/" + two(date.getMonth() + 1) + "/" + date.getFullYear() + " " + two(date.getHours()) + ":" + two(date.getMinutes());

}
