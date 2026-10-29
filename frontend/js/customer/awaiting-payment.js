/* ================= ĐƠN ĐANG CHỜ THANH TOÁN (kế hoạch v5 GĐ5) ================= */

/*
 * Giỏ hàng xoá ngay khi đặt đơn, nên khách bấm Back từ trang thanh toán sẽ thấy giỏ trống.
 * Trang giỏ / checkout gọi showAwaitingPaymentNotice(box): có đơn chuyển khoản / thẻ còn chờ
 * tiền (GET /orders/awaiting-payment) → khung "Đơn DH… đang chờ thanh toán" với
 * "Tiếp tục thanh toán" | "Huỷ đơn & lấy lại giỏ" (POST /orders/{id}/cancel-payment: server
 * huỷ đơn, trả lượt voucher, đưa đúng sản phẩm về giỏ). Cần order-store.js.
 */

/* Back / Forward lấy trang từ bộ nhớ đệm (bfcache) → nạp lại để thấy trạng thái mới */
window.addEventListener("pageshow", function (event) {

    if (event.persisted) {
        window.location.reload();
    }

});


async function showAwaitingPaymentNotice(box, onCancelled) {

    if (!box || !isLoggedIn()) {
        return null;
    }

    let order;

    try {

        order = await fetchAwaitingPaymentOrder();

    } catch (error) {

        /* chỉ là lời nhắc: lỗi thì thôi, trang vẫn dùng bình thường */
        return null;

    }

    if (!order) {

        box.hidden = true;

        return null;

    }

    const card = order.paymentMethod === "CARD";

    box.innerHTML = `
        <div class="awaiting-pay" role="status">
            <span class="awaiting-pay-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
            </span>
            <div class="awaiting-pay-text">
                <strong>Đơn ${escapeHtml(order.code)} đang chờ thanh toán</strong>
                <span>${escapeHtml(formatPrice(order.total))} · ${escapeHtml(card ? "Thẻ quốc tế" : "Chuyển khoản ngân hàng")} · ${escapeHtml(order.totalQuantity)} sản phẩm.
                    Sản phẩm của đơn này đã rời giỏ hàng; huỷ đơn để lấy lại giỏ.</span>
            </div>
            <div class="awaiting-pay-actions">
                <a class="btn btn-dark" href="${escapeHtml(getPaymentUrl(order.id))}" data-awaiting="continue">Tiếp tục thanh toán</a>
                <button type="button" class="btn btn-outline-dark" data-awaiting="cancel">Huỷ đơn &amp; lấy lại giỏ</button>
            </div>
        </div>
    `;

    box.hidden = false;

    box.querySelector('[data-awaiting="cancel"]').addEventListener("click", function () {

        confirmCancelPayment(order, onCancelled);

    });

    return order;

}


/* Hộp xác nhận → huỷ thanh toán; onDone(order) sau khi server đã đưa sản phẩm về giỏ */

function confirmCancelPayment(order, onDone) {

    openConfirmModal({
        title: "Huỷ thanh toán đơn " + order.code + "?",
        message: "Đơn hàng sẽ bị huỷ, voucher đã dùng được hoàn lại và các sản phẩm của đơn được đưa lại vào giỏ hàng.",
        cancelLabel: "Giữ đơn",
        confirmLabel: "Huỷ đơn & lấy lại giỏ",
        onConfirm: async function () {

            let cancelled;

            try {

                cancelled = await cancelOrderPayment(order.id);

            } catch (error) {

                showToast(getErrorMessage(error), "error");

                return;

            }

            showToast("Đã huỷ đơn " + cancelled.code + " và đưa sản phẩm về giỏ hàng.", "success");

            if (typeof onDone === "function") {
                onDone(cancelled);
            }

        }
    });

}
