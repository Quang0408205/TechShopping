/* Hoá đơn của đơn hàng mình đã đặt (customer/invoice.html?id=…): GET /orders/{id}, mở hộp thoại in của trình duyệt. */

document.addEventListener("DOMContentLoaded", async function () {

    const root = document.getElementById("invoiceRoot");

    const printButton = document.getElementById("invoicePrintBtn");

    const id = new URLSearchParams(window.location.search).get("id");


    if (!isLoggedIn()) {

        root.innerHTML = 'Vui lòng <a href="../auth/login.html?redirect=' + encodeURIComponent(window.location.pathname.split("/").slice(-1)[0] + window.location.search) + '">đăng nhập</a> để xem hoá đơn.';

        return;

    }

    if (!id || !/^\d+$/.test(id)) {

        root.textContent = "Không tìm thấy đơn hàng.";

        return;

    }


    try {

        const order = await apiRequest("/orders/" + id, { auth: true });

        if (order.status === "CANCELLED") {

            root.textContent = "Đơn hàng đã huỷ nên không có hoá đơn.";

            return;

        }

        root.className = "invoice-sheet";

        root.innerHTML = renderInvoiceHtml(order, "invoice");

        document.title = "Hoá đơn " + order.code + " - POY";

        printButton.hidden = false;

        printButton.addEventListener("click", function () { window.print(); });

    } catch (error) {

        root.textContent = error.status === 404 ? "Không tìm thấy đơn hàng." : getErrorMessage(error);

    }

});
