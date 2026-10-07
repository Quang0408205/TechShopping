/* ================= ĐƠN HÀNG (admin/orders.html) — Phase 4.5 + 5 ================= */

/*
 * API thật (STAFF và ADMIN; backend kiểm tra lại vai trò trong DB ở mọi request):
 *   GET   /admin/orders?keyword=&status=&fromDate=&toDate=&storeId=&page=&size=  mới nhất trước
 *         (nhân viên: backend chỉ trả đơn của chi nhánh mình, Phase 7)
 *   PATCH /admin/orders/{id}/status  { status, trackingNumber? }
 *   POST  /admin/orders/{id}/payment/confirm { transactionId? }   chuyển khoản đã nhận tiền
 *   POST  /admin/orders/{id}/payment/refund                       đã hoàn tiền đơn hủy
 *   POST  /admin/orders/{id}/installment/approve | reject { reason }
 *   PATCH /admin/orders/{id}/store { storeId }   chỉ ADMIN, đơn đang chờ (vd. chi nhánh thiếu hàng)
 * Chỉ cho chọn bước kế tiếp hợp lệ (giống OrderStatus.canMoveTo ở backend).
 * 409 INVALID_ORDER_STATUS (khách vừa hủy / người khác vừa đổi) → báo và tải lại;
 * 409 PAYMENT_REQUIRED / INSTALLMENT_NOT_APPROVED / INSUFFICIENT_STOCK / ORDER_STORE_MISSING → câu của server.
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

const STAFF_DELIVERY_TYPE_LABELS = {
    HOME_DELIVERY: "Giao tận nhà",
    PICKUP: "Nhận tại cửa hàng"
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

    const storeFilter = document.getElementById("orderStoreFilter");

    const isAdmin = staff.role === "ADMIN";

    const filterError = document.getElementById("orderFilterError");

    const tbody = document.getElementById("orderTableBody");

    const pagination = document.getElementById("orderPagination");


    let currentPage = 0;

    let currentOrders = [];

    let requestCounter = 0;

    const expandedIds = new Set();

    let allStores = null;


    /* admin/orders.html?keyword=DH00000042 (vd. link mã đơn ở lịch sử Tồn kho) */
    keywordInput.value = new URLSearchParams(window.location.search).get("keyword") || "";


    form.addEventListener("submit", function (event) {

        event.preventDefault();

        reloadFromFirstPage();

    });

    [statusFilter, fromDateInput, toDateInput, storeFilter].forEach(function (control) {
        control.addEventListener("change", reloadFromFirstPage);
    });

    setupStoreFilter(storeFilter, staff).catch(function (error) {
        handleError(error, function (message) {
            showToast("Không tải được danh sách chi nhánh: " + message, "error");
        });
    });


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const order = button && findOrder(button.dataset.id);

        if (!order) {
            return;
        }

        const action = button.dataset.action;

        if (action === "toggle") {
            toggleDetail(order.id);
        } else if (action === "tracking") {
            editTrackingNumber(order);
        } else if (action === "confirm-payment") {
            confirmPayment(order);
        } else if (action === "refund") {
            confirmRefund(order);
        } else if (action === "approve") {
            approveInstallment(order);
        } else if (action === "reject") {
            rejectInstallment(order);
        } else if (action === "store") {
            reassignStore(order);
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

        if (isAdmin && storeFilter.value) {
            params.set("storeId", storeFilter.value);
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
                    <span class="admin-subtext">${escapeHtml(order.storeName || "Chưa có chi nhánh")}</span>
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
                <td>
                    ${escapeHtml(STAFF_PAYMENT_METHOD_LABELS[order.paymentMethod] || order.paymentMethod)}
                    <span class="admin-subtext admin-payment-cell">${orderPaymentBadgeHtml(order)}</span>
                </td>
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

        const waiting = waitingFor(order);

        return statusSelectHtml(order.id, order.status, labels, "Trạng thái đơn " + order.code)
            + (waiting ? `<span class="admin-subtext admin-waiting">${escapeHtml(waiting)}</span>` : "");

    }


    /* Đơn chờ xác nhận nhưng chưa xác nhận được (backend sẽ trả 409) */

    function waitingFor(order) {

        if (order.status !== "PENDING") {
            return "";
        }

        if (!order.storeId) {
            return "Chưa có chi nhánh xử lý";
        }

        if (order.paymentMethod === "BANK_TRANSFER" && order.payment && order.payment.status !== "PAID") {
            return "Chờ nhận tiền chuyển khoản";
        }

        if (order.installment && order.installment.status !== "APPROVED") {
            return "Chờ duyệt trả góp";
        }

        return "";

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


        const storeHtml = escapeHtml(order.storeName || "Chưa có chi nhánh") +
            (isAdmin && order.status === "PENDING"
                ? ` <button type="button" class="admin-link-btn" data-action="store" data-id="${escapeHtml(String(order.id))}">Đổi</button>`
                : "");


        const facts = [
            ["Chi nhánh xử lý", storeHtml],
            ["Hình thức nhận", escapeHtml(STAFF_DELIVERY_TYPE_LABELS[order.deliveryType] || order.deliveryType || "—")],
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
                ${paymentSectionHtml(order)}
            </div>
        `;

    }


    /* Khối thanh toán trong dòng chi tiết: thông tin + nút thao tác hợp lệ ở trạng thái hiện tại */

    function paymentSectionHtml(order) {

        const id = escapeHtml(String(order.id));

        const facts = [];

        const actions = [];


        if (order.installment) {

            const plan = order.installment;

            facts.push(["Trả góp", installmentBadgeHtml(plan)]);
            facts.push(["Kỳ hạn", escapeHtml(installmentTermText(plan))]);
            facts.push(["Số CCCD", escapeHtml(plan.citizenId)]);
            facts.push(["Ngân hàng thẻ", escapeHtml(plan.cardBankName || plan.cardBank)]);

            if (plan.reviewedAt) {
                facts.push([plan.status === "REJECTED" ? "Từ chối lúc" : "Duyệt lúc", escapeHtml(formatDateTimeVi(plan.reviewedAt))]);
            }

            if (plan.rejectionReason) {
                facts.push(["Lý do từ chối", escapeHtml(plan.rejectionReason)]);
            }

            if (plan.status === "ACTIVE" || plan.status === "COMPLETED") {

                facts.push(["Đã thu", escapeHtml(plan.paidPeriods + "/" + plan.numMonths + " kỳ · " + formatPrice(Number(plan.paidAmount)))]);

                actions.push(`<a class="btn btn-outline-dark admin-action-btn" href="${escapeHtml(siteUrl("admin/installments.html?keyword=" + encodeURIComponent(order.code)))}">XEM LỊCH TRẢ GÓP</a>`);

            }

            if (plan.status === "PENDING_APPROVAL") {
                actions.push(`<button type="button" class="btn btn-dark admin-action-btn" data-action="approve" data-id="${id}">DUYỆT TRẢ GÓP</button>`);
                actions.push(`<button type="button" class="btn btn-outline-dark admin-action-btn admin-action-danger" data-action="reject" data-id="${id}">TỪ CHỐI</button>`);
            }

        } else if (order.payment) {

            const payment = order.payment;

            facts.push(["Thanh toán", orderPaymentBadgeHtml(order)]);
            facts.push(["Số tiền", escapeHtml(formatPrice(Number(payment.amount)))]);

            if (payment.paidAt) {
                facts.push(["Đã thu lúc", escapeHtml(formatDateTimeVi(payment.paidAt))]);
            }

            if (payment.transactionId) {
                facts.push(["Mã giao dịch", escapeHtml(payment.transactionId)]);
            }

            if (payment.refundedAt) {
                facts.push(["Hoàn tiền lúc", escapeHtml(formatDateTimeVi(payment.refundedAt))]);
            }

            if (order.paymentMethod === "BANK_TRANSFER" && payment.status === "PENDING") {
                actions.push(`<button type="button" class="btn btn-dark admin-action-btn" data-action="confirm-payment" data-id="${id}">ĐÃ NHẬN TIỀN</button>`);
            }

            if (payment.status === "REFUND_PENDING") {
                actions.push(`<button type="button" class="btn btn-dark admin-action-btn" data-action="refund" data-id="${id}">ĐÃ HOÀN TIỀN</button>`);
            }

        } else {

            return "";

        }


        return `
            <div class="admin-payment-box">
                <h4>Thanh toán</h4>
                <dl class="admin-order-facts">
                    ${facts.map(function (fact) {
                        return `<div><dt>${fact[0]}</dt><dd>${fact[1]}</dd></div>`;
                    }).join("")}
                </dl>
                ${actions.length ? `<div class="admin-payment-actions">${actions.join("")}</div>` : ""}
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

            const paymentNote = order.paymentMethod === "COD"
                ? " Tiền COD được ghi nhận là đã thu."
                : order.installment ? " Lịch trả góp bắt đầu từ hôm nay (kỳ đầu đến hạn sau 1 tháng)." : "";

            openConfirmModal({
                title: "Đã giao đơn " + order.code + "?",
                message: "Đơn chuyển sang Đã giao hàng và được cộng vào tổng chi tiêu của khách." + paymentNote + " Không thể hoàn tác.",
                confirmLabel: "ĐÃ GIAO",
                cancelLabel: "Quay lại",
                onConfirm: function () {
                    updateStatus(order, { status: status }, "Đơn " + order.code + " đã giao xong.");
                }
            });

        } else if (status === "CANCELLED") {

            const paid = order.payment && order.payment.status === "PAID";

            openConfirmModal({
                title: "Hủy đơn " + order.code + "?",
                message: "Khách sẽ thấy đơn ở trạng thái Đã hủy."
                    + (paid ? " Khoản tiền khách đã trả sẽ chuyển sang Chờ hoàn tiền." : "") + " Không thể hoàn tác.",
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


    /* ================= THANH TOÁN / TRẢ GÓP ================= */

    function confirmPayment(order) {

        openPaymentConfirmModal({
            title: "Đã nhận tiền đơn " + order.code + "?",
            message: "Đối chiếu sao kê: " + formatPrice(Number(order.payment.amount)) + ", nội dung " + order.code
                + ". Sau khi xác nhận, đơn mới chuyển được sang Đã xác nhận.",
            confirmLabel: "ĐÃ NHẬN TIỀN",
            onConfirm: function (body) {
                postAction(order, "payment/confirm", body, "Đã ghi nhận tiền chuyển khoản của đơn " + order.code + ".");
            }
        });

    }


    function confirmRefund(order) {

        openConfirmModal({
            title: "Đã hoàn tiền đơn " + order.code + "?",
            message: "Xác nhận đã chuyển trả " + formatPrice(Number(order.payment.amount)) + " cho khách. Không thể hoàn tác.",
            confirmLabel: "ĐÃ HOÀN TIỀN",
            cancelLabel: "Quay lại",
            onConfirm: function () {
                postAction(order, "payment/refund", undefined, "Đã ghi nhận hoàn tiền đơn " + order.code + ".");
            }
        });

    }


    function approveInstallment(order) {

        openConfirmModal({
            title: "Duyệt trả góp đơn " + order.code + "?",
            message: "Đã xác minh khách (CCCD " + order.installment.citizenId + ", thẻ "
                + (order.installment.cardBankName || order.installment.cardBank) + "). Sau khi duyệt, đơn mới chuyển được sang Đã xác nhận.",
            confirmLabel: "DUYỆT",
            cancelLabel: "Quay lại",
            onConfirm: function () {
                postAction(order, "installment/approve", undefined, "Đã duyệt trả góp đơn " + order.code + ".");
            }
        });

    }


    function rejectInstallment(order) {

        openConfirmModal({
            title: "Từ chối trả góp đơn " + order.code + "?",
            message: "Đơn sẽ bị hủy và khách thấy lý do bên dưới. Không thể hoàn tác.",
            confirmLabel: "TỪ CHỐI",
            cancelLabel: "Quay lại",
            input: {
                label: "Lý do từ chối (bắt buộc)",
                placeholder: "VD: Không xác minh được thông tin CCCD",
                maxLength: REJECTION_REASON_MAX_LENGTH
            },
            onConfirm: function (reason) {

                if (!reason) {

                    showToast("Vui lòng nhập lý do từ chối.", "error");

                    return;

                }

                postAction(order, "installment/reject", { reason: reason }, "Đã từ chối trả góp, đơn " + order.code + " đã hủy.");

            }
        });

    }


    /* ================= ĐỔI CHI NHÁNH (ADMIN, đơn đang chờ) ================= */

    async function reassignStore(order) {

        try {

            if (!allStores) {
                allStores = await loadAllStores();
            }

        } catch (error) {

            handleError(error, function (message) {
                showToast(message, "error");
            });

            return;

        }


        const openStores = allStores.filter(function (store) {
            return store.active !== false;
        });

        if (openStores.length === 0) {

            showToast("Chưa có chi nhánh nào đang mở.", "error");

            return;

        }


        openConfirmModal({
            title: "Đổi chi nhánh xử lý đơn " + order.code + "?",
            message: order.deliveryType === "PICKUP"
                ? "Đơn nhận tại cửa hàng: địa chỉ nhận hàng của khách sẽ đổi theo chi nhánh mới."
                : "Tồn kho được trừ ở chi nhánh mới khi xác nhận đơn.",
            confirmLabel: "ĐỔI CHI NHÁNH",
            cancelLabel: "Quay lại",
            select: {
                label: "Chi nhánh",
                value: order.storeId || openStores[0].id,
                options: openStores.map(function (store) {
                    return { value: store.id, label: store.name };
                })
            },
            onConfirm: async function (storeId) {

                tbody.querySelectorAll('[data-id="' + order.id + '"]').forEach(function (control) {
                    control.disabled = true;
                });

                try {

                    const updated = await apiRequest("/admin/orders/" + order.id + "/store", {
                        method: "PATCH",
                        body: { storeId: Number(storeId) },
                        auth: true
                    });

                    showToast("Đơn " + order.code + " chuyển sang " + updated.order.storeName + ".", "success");

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

                loadOrders();

            }
        });

    }


    async function postAction(order, action, body, successMessage) {

        tbody.querySelectorAll('[data-id="' + order.id + '"]').forEach(function (control) {
            control.disabled = true;
        });


        try {

            await apiRequest("/admin/orders/" + order.id + "/" + action, {
                method: "POST",
                body: body,
                auth: true
            });

            showToast(successMessage, "success");

        } catch (error) {

            handleError(error, function (message) {
                showToast(message, "error");
            });

        }

        loadOrders();

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

                let text = message;

                if (error.code === "INVALID_ORDER_STATUS") {
                    text = "Đơn " + order.code + " vừa được cập nhật ở nơi khác (khách hủy hoặc nhân viên khác xử lý). Danh sách đã được tải lại.";
                } else if (error.code === "INSUFFICIENT_STOCK" || error.code === "ORDER_STORE_MISSING") {
                    text = message + (isAdmin
                        ? ". Mở ▸ để đổi chi nhánh xử lý."
                        : ". Hãy nhập thêm hàng hoặc báo ADMIN chuyển đơn sang chi nhánh khác.");
                }

                showToast(text, "error");

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
