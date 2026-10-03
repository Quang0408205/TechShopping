/* ================= ĐƠN HÀNG (admin/orders.html) — Phase 4.5 ================= */

/*
 * API thật (STAFF và ADMIN; backend kiểm tra lại vai trò trong DB ở mọi request):
 *   GET   /admin/orders?keyword=&status=&fromDate=&toDate=&page=&size=  mới nhất trước
 *   PATCH /admin/orders/{id}/status  { status, trackingNumber? }
 * Chỉ cho chọn bước kế tiếp hợp lệ (giống OrderStatus.canMoveTo ở backend).
 * 409 INVALID_ORDER_STATUS (khách vừa hủy / người khác vừa đổi) → báo và tải lại.
 * Chưa lọc theo chi nhánh: orders chỉ gắn chi nhánh qua sales_records ở Phase 7.
 */

const ORDER_PAGE_SIZE = 20;

const TRACKING_NUMBER_MAX_LENGTH = 100;

const STAFF_ORDER_STATUS_LABELS = {
    PENDING: "Chờ xác nhận",
    CONFIRMED: "Đã xác nhận",
    SHIPPING: "Đang giao hàng",
    DELIVERED: "Đã giao hàng",
    CANCELLED: "Đã hủy"
};

const ORDER_NEXT_STATUSES = {
    PENDING: ["CONFIRMED", "CANCELLED"],
    CONFIRMED: ["SHIPPING", "CANCELLED"],
    SHIPPING: ["DELIVERED"]
};

const STAFF_PAYMENT_METHOD_LABELS = {
    COD: "Tiền mặt khi nhận hàng",
    BANK_TRANSFER: "Chuyển khoản",
    INSTALLMENT: "Trả góp"
};


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const form = document.getElementById("orderFilterForm");

    const keywordInput = document.getElementById("orderKeyword");

    const statusFilter = document.getElementById("orderStatusFilter");

    const fromDateInput = document.getElementById("orderFromDate");

    const toDateInput = document.getElementById("orderToDate");

    const filterError = document.getElementById("orderFilterError");

    const tbody = document.getElementById("orderTableBody");

    const pagination = document.getElementById("orderPagination");


    let currentPage = 0;

    let currentOrders = [];

    let requestCounter = 0;

    const expandedIds = new Set();


    form.addEventListener("submit", function (event) {

        event.preventDefault();

        reloadFromFirstPage();

    });

    [statusFilter, fromDateInput, toDateInput].forEach(function (control) {
        control.addEventListener("change", reloadFromFirstPage);
    });


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const order = button && findOrder(button.dataset.id);

        if (!order) {
            return;
        }

        if (button.dataset.action === "toggle") {
            toggleDetail(order.id);
        } else if (button.dataset.action === "tracking") {
            editTrackingNumber(order);
        }

    });

    tbody.addEventListener("change", function (event) {

        const select = event.target.closest(".js-status-select");

        const order = select && findOrder(select.dataset.id);

        if (!order) {
            return;
        }

        const nextStatus = select.value;

        /* Ô chọn giữ trạng thái hiện tại cho tới khi máy chủ xác nhận thay đổi */
        select.value = order.status;

        chooseStatus(order, nextStatus);

    });


    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        loadOrders();

    });


    loadOrders();


    /* ================= TẢI DANH SÁCH ================= */

    function reloadFromFirstPage() {

        currentPage = 0;

        loadOrders();

    }


    async function loadOrders() {

        const fromDate = fromDateInput.value;

        const toDate = toDateInput.value;

        if (fromDate && toDate && fromDate > toDate) {

            filterError.textContent = "Ngày bắt đầu phải trước hoặc trùng ngày kết thúc.";

            filterError.hidden = false;

            return;

        }

        filterError.hidden = true;


        const requestId = ++requestCounter;

        const params = new URLSearchParams({
            page: String(currentPage),
            size: String(ORDER_PAGE_SIZE)
        });

        const keyword = keywordInput.value.trim();

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (statusFilter.value) {
            params.set("status", statusFilter.value);
        }

        if (fromDate) {
            params.set("fromDate", fromDate);
        }

        if (toDate) {
            params.set("toDate", toDate);
        }


        tbody.innerHTML = adminEmptyRow(8,"Đang tải…");


        try {

            const page = await apiRequest("/admin/orders?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            /* Đơn cuối của trang cuối vừa rời bộ lọc (vd. đổi trạng thái) → lùi một trang */
            if ((page.content || []).length === 0 && currentPage > 0 && page.totalPages > 0) {

                currentPage = page.totalPages - 1;

                loadOrders();

                return;

            }

            currentOrders = (page.content || []).map(function (entry) {
                return Object.assign({}, entry.order, { customer: entry.customer });
            });

            renderTable(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(8,message);
                    pagination.innerHTML = "";
                });
            }

        }

    }


    function findOrder(id) {

        return currentOrders.find(function (order) {
            return String(order.id) === String(id);
        });

    }


    /* ================= HIỂN THỊ ================= */

    function renderTable(page) {

        document.getElementById("orderRowCount").textContent = page.totalElements + " đơn hàng";

        tbody.innerHTML = currentOrders.map(renderOrderRows).join("") ||
            adminEmptyRow(8,"Không có đơn hàng nào phù hợp bộ lọc");

        renderPagination(page);

    }


    function renderOrderRows(order) {

        const id = escapeHtml(String(order.id));

        const expanded = expandedIds.has(order.id);

        const customer = order.customer || {};

        const customerName = customer.fullname || customer.username || "—";


        return `
            <tr data-order-id="${id}">
                <td>
                    <button
                        type="button"
                        class="admin-row-toggle"
                        data-action="toggle"
                        data-id="${id}"
                        aria-label="Xem chi tiết đơn ${escapeHtml(order.code)}"
                        aria-expanded="${expanded}"
                    >${expanded ? "▾" : "▸"}</button>
                </td>
                <td>
                    <strong>${escapeHtml(order.code)}</strong>
                    <span class="admin-subtext">${escapeHtml(formatDateTimeVi(order.orderDate))}</span>
                </td>
                <td>
                    ${escapeHtml(customerName)}
                    ${customer.deleted ? ' <span class="admin-badge admin-badge-danger">Đã xoá</span>' : ""}
                    <span class="admin-subtext">${escapeHtml(customer.email || "")}</span>
                </td>
                <td>
                    ${escapeHtml(order.recipientName)}
                    <span class="admin-subtext">${escapeHtml(order.recipientPhone)}</span>
                </td>
                <td>${escapeHtml(String(order.totalQuantity))}</td>
                <td>${escapeHtml(formatPrice(Number(order.total)))}</td>
                <td>${escapeHtml(STAFF_PAYMENT_METHOD_LABELS[order.paymentMethod] || order.paymentMethod)}</td>
                <td>${statusCellHtml(order)}</td>
            </tr>
            <tr class="js-order-detail" data-order-id="${id}" ${expanded ? "" : "hidden"}>
                <td></td>
                <td colspan="7">${orderDetailHtml(order)}</td>
            </tr>
        `;

    }


    /* Ô chọn chỉ gồm trạng thái hiện tại + các bước kế tiếp; hết bước → huy hiệu */

    function statusCellHtml(order) {

        const nextStatuses = ORDER_NEXT_STATUSES[order.status] || [];

        const label = STAFF_ORDER_STATUS_LABELS[order.status] || order.status;

        if (nextStatuses.length === 0) {

            const badgeClass = order.status === "DELIVERED" ? "admin-badge-success" : "admin-badge-danger";

            return `<span class="admin-badge ${badgeClass}">${escapeHtml(label)}</span>`;

        }


        const labels = {};

        labels[order.status] = label;

        nextStatuses.forEach(function (status) {
            labels[status] = "→ " + STAFF_ORDER_STATUS_LABELS[status];
        });

        return statusSelectHtml(order.id, order.status, labels, "Trạng thái đơn " + order.code);

    }


    function orderDetailHtml(order) {

        const itemRows = (order.items || []).map(function (item) {

            const oldPrice = item.originalPrice
                ? ` <span class="admin-subtext admin-old-price">${escapeHtml(formatPrice(Number(item.originalPrice)))}</span>`
                : "";

            return `
                <tr>
                    <td>
                        ${escapeHtml(item.productName)}
                        <span class="admin-subtext">${escapeHtml(item.variantName || "")}</span>
                    </td>
                    <td>${escapeHtml(formatPrice(Number(item.unitPrice)))}${oldPrice}</td>
                    <td>${escapeHtml(String(item.quantity))}</td>
                    <td>${escapeHtml(formatPrice(Number(item.subtotal)))}</td>
                </tr>
            `;

        }).join("");


        const canEditTracking = order.status === "SHIPPING";

        const trackingHtml = (order.trackingNumber ? escapeHtml(order.trackingNumber) : "—") +
            (canEditTracking
                ? ` <button type="button" class="admin-link-btn" data-action="tracking" data-id="${escapeHtml(String(order.id))}">Sửa</button>`
                : "");

        const customerPhone = order.customer && order.customer.phone;


        const facts = [
            ["Giao tới", escapeHtml(order.shippingAddress)],
            ["Ghi chú", order.note ? escapeHtml(order.note) : "—"],
            ["Tạm tính", escapeHtml(formatPrice(Number(order.subtotal)))],
            ["Phí giao hàng", Number(order.shippingFee) > 0 ? escapeHtml(formatPrice(Number(order.shippingFee))) : "Miễn phí"],
            ["Mã vận đơn", trackingHtml],
            ["SĐT tài khoản khách", customerPhone ? escapeHtml(customerPhone) : "—"]
        ];

        if (order.deliveredAt) {
            facts.push(["Giao xong lúc", escapeHtml(formatDateTimeVi(order.deliveredAt))]);
        }

        if (order.cancelledAt) {
            facts.push(["Hủy lúc", escapeHtml(formatDateTimeVi(order.cancelledAt))]);
        }


        return `
            <div class="admin-order-detail">
                <dl class="admin-order-facts">
                    ${facts.map(function (fact) {
                        return `<div><dt>${fact[0]}</dt><dd>${fact[1]}</dd></div>`;
                    }).join("")}
                </dl>
                <table class="admin-table">
                    <thead>
                        <tr><th>Sản phẩm</th><th>Đơn giá</th><th>Số lượng</th><th>Thành tiền</th></tr>
                    </thead>
                    <tbody>${itemRows}</tbody>
                </table>
            </div>
        `;

    }


    function renderPagination(page) {

        if (page.totalPages <= 1) {

            pagination.innerHTML = "";

            return;

        }

        pagination.innerHTML = `
            <button type="button" class="btn btn-outline-dark" data-page="${page.page - 1}" ${page.page <= 0 ? "disabled" : ""}>‹ TRƯỚC</button>
            <span>Trang ${page.page + 1} / ${page.totalPages}</span>
            <button type="button" class="btn btn-outline-dark" data-page="${page.page + 1}" ${page.page + 1 >= page.totalPages ? "disabled" : ""}>SAU ›</button>
        `;

    }


    function toggleDetail(orderId) {

        const detailRow = tbody.querySelector('.js-order-detail[data-order-id="' + orderId + '"]');

        const button = tbody.querySelector('button[data-action="toggle"][data-id="' + orderId + '"]');

        if (!detailRow || !button) {
            return;
        }

        detailRow.hidden = !detailRow.hidden;

        if (detailRow.hidden) {
            expandedIds.delete(orderId);
        } else {
            expandedIds.add(orderId);
        }

        button.textContent = detailRow.hidden ? "▸" : "▾";

        button.setAttribute("aria-expanded", String(!detailRow.hidden));

    }


    /* ================= ĐỔI TRẠNG THÁI ================= */

    function chooseStatus(order, status) {

        if ((ORDER_NEXT_STATUSES[order.status] || []).indexOf(status) === -1) {
            return;
        }


        if (status === "CONFIRMED") {

            updateStatus(order, { status: status }, "Đã xác nhận đơn " + order.code + ".");

        } else if (status === "SHIPPING") {

            openConfirmModal({
                title: "Giao đơn " + order.code,
                message: "Đơn chuyển sang Đang giao hàng. Nhập mã vận đơn để khách tra cứu (có thể bỏ trống).",
                confirmLabel: "GIAO HÀNG",
                cancelLabel: "Quay lại",
                input: {
                    label: "Mã vận đơn",
                    value: order.trackingNumber || "",
                    placeholder: "VD: GHN123456789",
                    maxLength: TRACKING_NUMBER_MAX_LENGTH
                },
                onConfirm: function (trackingNumber) {
                    updateStatus(
                        order,
                        { status: status, trackingNumber: trackingNumber },
                        "Đơn " + order.code + " đang được giao."
                    );
                }
            });

        } else if (status === "DELIVERED") {

            openConfirmModal({
                title: "Đã giao đơn " + order.code + "?",
                message: "Đơn chuyển sang Đã giao hàng và được cộng vào tổng chi tiêu của khách. Không thể hoàn tác.",
                confirmLabel: "ĐÃ GIAO",
                cancelLabel: "Quay lại",
                onConfirm: function () {
                    updateStatus(order, { status: status }, "Đơn " + order.code + " đã giao xong.");
                }
            });

        } else if (status === "CANCELLED") {

            openConfirmModal({
                title: "Hủy đơn " + order.code + "?",
                message: "Khách sẽ thấy đơn ở trạng thái Đã hủy. Không thể hoàn tác.",
                confirmLabel: "HỦY ĐƠN",
                cancelLabel: "Quay lại",
                onConfirm: function () {
                    updateStatus(order, { status: status }, "Đã hủy đơn " + order.code + ".");
                }
            });

        }

    }


    function editTrackingNumber(order) {

        openConfirmModal({
            title: "Mã vận đơn " + order.code,
            message: "Bỏ trống để xoá mã vận đơn.",
            confirmLabel: "LƯU",
            cancelLabel: "Quay lại",
            input: {
                label: "Mã vận đơn",
                value: order.trackingNumber || "",
                maxLength: TRACKING_NUMBER_MAX_LENGTH
            },
            onConfirm: function (trackingNumber) {
                updateStatus(
                    order,
                    { status: order.status, trackingNumber: trackingNumber },
                    "Đã lưu mã vận đơn của đơn " + order.code + "."
                );
            }
        });

    }


    async function updateStatus(order, body, successMessage) {

        tbody.querySelectorAll('[data-id="' + order.id + '"]').forEach(function (control) {
            control.disabled = true;
        });


        try {

            await apiRequest("/admin/orders/" + order.id + "/status", {
                method: "PATCH",
                body: body,
                auth: true
            });

            showToast(successMessage, "success");

        } catch (error) {

            handleError(error, function (message) {

                showToast(
                    error.code === "INVALID_ORDER_STATUS"
                        ? "Đơn " + order.code + " vừa được cập nhật ở nơi khác (khách hủy hoặc nhân viên khác xử lý). Danh sách đã được tải lại."
                        : message,
                    "error"
                );

            });

        }

        loadOrders();

    }


    /*
     * Hết phiên (401, kể cả sau khi đã thử làm mới token) → kiểm tra lại phiên
     * và về trang đăng nhập; lỗi khác (403, 404, 409…) → hiển thị bằng show(message).
     */

    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
