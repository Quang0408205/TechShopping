/* ================= THEO DÕI ĐƠN HÀNG (link trong email) ================= */

/*
 * customer/track.html?token=<mã ngẫu nhiên>: không cần đăng nhập. API chỉ trả mã đơn, sản phẩm, tiền và tiến trình
 * (không có số điện thoại / địa chỉ chi tiết). Mọi dữ liệu từ API đều escape khi đưa vào HTML.
 */

document.addEventListener("DOMContentLoaded", async function () {

    const token = new URLSearchParams(location.search).get("token") || "";

    const show = function (id) {

        ["trackLoading", "trackNotFound", "trackError", "trackBox"].forEach(function (name) {
            document.getElementById(name).hidden = name !== id;
        });

    };

    if (!/^[0-9a-f]{64}$/.test(token)) {
        show("trackNotFound");
        return;
    }

    let order;

    try {

        order = await apiRequest("/orders/track/" + token);

    } catch (error) {

        show(error && error.status === 404 ? "trackNotFound" : "trackError");

        return;

    }

    renderTracking(order);

    show("trackBox");

});


function renderTracking(order) {

    const setText = function (id, text) {
        document.getElementById(id).textContent = text;
    };

    setText("trackCode", order.code);

    setText("trackDate", "Đặt lúc " + formatOrderDateTime(order.orderDate));

    setText("trackStatus", getOrderStatusLabel(order.status));

    document.getElementById("trackStatus").dataset.status = order.status;

    document.title = "Đơn " + order.code + " - POY";


    const reached = {};

    (order.steps || []).forEach(function (step) {
        reached[step.status] = step.at;
    });

    const cancelled = order.status === "CANCELLED";

    const pickup = order.deliveryType === "PICKUP";

    /* nhận tại cửa hàng: "Đang giao hàng" là "Sẵn sàng tại cửa hàng" */
    const flow = ORDER_STATUS_FLOW.map(function (step) {
        return step.code === "SHIPPING" && pickup ? { code: step.code, label: "Sẵn sàng tại cửa hàng" } : step;
    });

    const currentIndex = flow.findIndex(function (step) {
        return step.code === order.status;
    });

    document.getElementById("trackSteps").hidden = cancelled;

    document.getElementById("trackCancelled").hidden = !cancelled;

    document.getElementById("trackSteps").innerHTML = flow.map(function (step, index) {

        const done = index <= currentIndex;

        const when = reached[step.code] ? formatOrderDateTime(reached[step.code]) : "";

        return `<li class="${done ? "is-done" : ""}${index === currentIndex ? " is-current" : ""}">
            <span class="track-dot" aria-hidden="true"></span>
            <strong>${escapeHtml(step.label)}</strong>
            <small>${escapeHtml(when)}</small>
        </li>`;

    }).join("");


    setText("trackPayment", PAYMENT_METHOD_LABELS[order.paymentMethod] || order.paymentMethod || "");

    setText("trackReceiving", (DELIVERY_TYPE_LABELS[order.deliveryType] || "")
        + (order.receiving ? " – " + order.receiving : ""));

    document.getElementById("trackNumberRow").hidden = !order.trackingNumber;

    setText("trackNumber", order.trackingNumber || "");

    document.getElementById("trackItems").innerHTML = (order.items || []).map(function (item) {

        return `<li>
            <span>${escapeHtml(item.name)} <small>× ${escapeHtml(String(item.quantity))}</small></span>
            <strong>${escapeHtml(formatPrice(item.subtotal))}</strong>
        </li>`;

    }).join("");

    setText("trackTotal", formatPrice(order.total));

}
