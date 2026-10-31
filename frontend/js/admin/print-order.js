/*
 * Hoá đơn / phiếu giao hàng của một đơn cho nhân viên (admin/print-order.html?type=invoice|delivery&id=…):
 * GET /admin/orders/{id}; nhân viên chỉ in được đơn của chi nhánh mình (backend kiểm tra).
 */

document.addEventListener("DOMContentLoaded", async function () {

    const root = document.getElementById("invoiceRoot");

    const printButton = document.getElementById("invoicePrintBtn");

    const params = new URLSearchParams(window.location.search);

    const id = params.get("id");

    const kind = params.get("type") === "delivery" ? "delivery" : "invoice";


    if (!id || !/^\d+$/.test(id)) {

        root.textContent = "Không tìm thấy đơn hàng.";

        return;

    }


    try {

        const response = await apiRequest("/admin/orders/" + id, { auth: true });

        const order = response.order;

        if (kind === "invoice" && order.status === "CANCELLED") {

            root.textContent = "Đơn hàng đã huỷ nên không có hoá đơn.";

            return;

        }

        root.className = "invoice-sheet";

        root.innerHTML = renderInvoiceHtml(order, kind);

        document.title = (kind === "delivery" ? "Phiếu giao " : "Hoá đơn ") + order.code + " - POY";

        printButton.textContent = kind === "delivery" ? "IN PHIẾU GIAO HÀNG" : "IN HOÁ ĐƠN";

        printButton.hidden = false;

        printButton.addEventListener("click", function () { window.print(); });

    } catch (error) {

        if (error.status === 401) {
            requireStaffLogin();
            return;
        }

        root.textContent = error.status === 404 ? "Không tìm thấy đơn hàng." : getErrorMessage(error);

    }

});
