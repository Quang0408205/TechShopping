/* ================= ĐƠN HÀNG (admin/orders.html) ================= */

/*
 * API thật (STAFF và ADMIN; backend kiểm tra lại vai trò trong DB ở mọi request):
 *   GET   /admin/orders?keyword=&status=&fromDate=&toDate=&storeId=&page=&size=  mới nhất trước
 *         (nhân viên: backend chỉ trả đơn của chi nhánh mình)
 *   PATCH /admin/orders/{id}/status  { status, trackingNumber? }
 *   POST  /admin/orders/{id}/payment/confirm { transactionId? }   chuyển khoản đã nhận tiền
 *   POST  /admin/orders/{id}/payment/refund                       đã hoàn tiền đơn hủy
 *   POST  /admin/orders/{id}/installment/approve | reject { reason }
 *   PATCH /admin/orders/{id}/store { storeId }   chỉ ADMIN, đơn đang chờ (vd. chi nhánh thiếu hàng)
 * Chỉ hiện nút cho bước kế tiếp hợp lệ (giống OrderStatus.canMoveTo ở backend).
 * Số đơn trên tab trạng thái = totalElements của GET /admin/orders?size=1 với cùng bộ lọc.
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

/* Nhãn nút chuyển sang từng trạng thái */
const ORDER_STATUS_ACTION_LABELS = {
    CONFIRMED: "XÁC NHẬN",
    SHIPPING: "GIAO HÀNG",
    DELIVERED: "ĐÃ GIAO",
    CANCELLED: "HỦY"
};

const ORDER_STATUS_BADGE_CLASSES = {
    PENDING: "admin-badge-warning",
    CONFIRMED: "admin-badge-info",
    SHIPPING: "admin-badge-info",
    DELIVERED: "admin-badge-success",
    CANCELLED: "admin-badge-danger"
};

const STAFF_DELIVERY_TYPE_LABELS = {
    HOME_DELIVERY: "Giao tận nhà",
    PICKUP: "Nhận tại cửa hàng"
};

const STAFF_PAYMENT_METHOD_LABELS = {
    COD: "Tiền mặt khi nhận hàng",
    BANK_TRANSFER: "Chuyển khoản",
    INSTALLMENT: "Trả góp",
    CARD: "Thẻ quốc tế (Stripe)"
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

    const statusTabs = document.getElementById("orderStatusTabs");

    const quickDates = document.getElementById("orderQuickDates");


    let currentPage = 0;

    let currentOrders = [];

    let requestCounter = 0;

    const expandedIds = new Set();

    let allStores = null;


    /* admin/orders.html?keyword=DH00000042 (vd. link mã đơn ở lịch sử Tồn kho) */
    keywordInput.value = new URLSearchParams(window.location.search).get("keyword") || "";

    /* ?status=PENDING (lối tắt "Việc của tôi" ở trang Tổng quan); giá trị lạ thì bỏ qua */
    const initialStatus = new URLSearchParams(window.location.search).get("status");

    if (initialStatus && Array.from(statusFilter.options).some(function (o) { return o.value === initialStatus; })) {
        statusFilter.value = initialStatus;
    }


    form.addEventListener("submit", function (event) {

        event.preventDefault();

        reloadFromFirstPage();

    });

    [statusFilter, fromDateInput, toDateInput, storeFilter].forEach(function (control) {
        control.addEventListener("change", reloadFromFirstPage);
    });

    statusTabs.addEventListener("click", function (event) {

        const tab = event.target.closest("[data-status]");

        if (!tab || statusFilter.value === tab.dataset.status) {
            return;
        }

        statusFilter.value = tab.dataset.status;

        reloadFromFirstPage();

    });

    quickDates.addEventListener("click", function (event) {

        const button = event.target.closest("[data-range]");

        if (!button) {
            return;
        }

        const range = quickDateRange(button.dataset.range);

        fromDateInput.value = range[0];

        toDateInput.value = range[1];

        reloadFromFirstPage();

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

        if (action === "status") {
            chooseStatus(order, button.dataset.status);
        } else if (action === "toggle") {
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

        syncFilterButtons();


        const requestId = ++requestCounter;

        const params = filterParams();

        params.set("page", String(currentPage));

        params.set("size", String(ORDER_PAGE_SIZE));

        if (statusFilter.value) {
            params.set("status", statusFilter.value);
        }

        loadStatusCounts(requestId);


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


    /* Bộ lọc chung (trừ trạng thái và phân trang) */

    function filterParams() {

        const params = new URLSearchParams();

        const keyword = keywordInput.value.trim();

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (fromDateInput.value) {
            params.set("fromDate", fromDateInput.value);
        }

        if (toDateInput.value) {
            params.set("toDate", toDateInput.value);
        }

        if (isAdmin && storeFilter.value) {
            params.set("storeId", storeFilter.value);
        }

        return params;

    }


    /* Số đơn từng tab; lỗi thì để trống số, danh sách vẫn hiển thị */

    async function loadStatusCounts(requestId) {

        const tabs = Array.from(statusTabs.querySelectorAll("[data-status]"));

        const counts = await Promise.all(tabs.map(function (tab) {

            const params = filterParams();

            params.set("size", "1");

            if (tab.dataset.status) {
                params.set("status", tab.dataset.status);
            }

            return apiRequest("/admin/orders?" + params.toString(), { auth: true })
                .then(function (page) { return page.totalElements; })
                .catch(function () { return null; });

        }));

        if (requestId !== requestCounter) {
            return;
        }

        tabs.forEach(function (tab, index) {
            tab.querySelector(".admin-tab-count").textContent = counts[index] === null ? "" : String(counts[index]);
        });

    }


    /* Tab đang chọn + nút ngày nhanh khớp với ô Từ / đến */

    function syncFilterButtons() {

        statusTabs.querySelectorAll("[data-status]").forEach(function (tab) {

            const active = tab.dataset.status === statusFilter.value;

            tab.classList.toggle("is-active", active);

            tab.setAttribute("aria-selected", String(active));

        });

        quickDates.querySelectorAll("[data-range]").forEach(function (button) {

            const range = quickDateRange(button.dataset.range);

            const pressed = range[0] === fromDateInput.value && range[1] === toDateInput.value;

            button.setAttribute("aria-pressed", String(pressed));

        });

    }


    /* [từ, đến] dạng yyyy-mm-dd theo giờ máy; "all" = bỏ trống */

    function quickDateRange(range) {

        const today = new Date();

        const daysAgo = function (days) {
            return new Date(today.getFullYear(), today.getMonth(), today.getDate() - days);
        };

        if (range === "today") {
            return [isoDate(today), isoDate(today)];
        }

        if (range === "yesterday") {
            return [isoDate(daysAgo(1)), isoDate(daysAgo(1))];
        }

        if (range === "7d") {
            return [isoDate(daysAgo(6)), isoDate(today)];
        }

        if (range === "month") {
            return [isoDate(new Date(today.getFullYear(), today.getMonth(), 1)), isoDate(today)];
        }

        return ["", ""];

    }


    function isoDate(date) {

        return date.getFullYear() + "-" + String(date.getMonth() + 1).padStart(2, "0") + "-" + String(date.getDate()).padStart(2, "0");

    }


    /* Tiêu đề nhóm ngày: Hôm nay / Hôm qua / Thứ Bảy, 11/10/2026 */

    function dayGroupLabel(key) {

        const range = quickDateRange("yesterday");

        if (key === quickDateRange("today")[0]) {
            return "Hôm nay";
        }

        if (key === range[0]) {
            return "Hôm qua";
        }

        const label = new Date(key + "T00:00:00").toLocaleDateString("vi-VN", {
            weekday: "long", day: "numeric", month: "numeric", year: "numeric"
        });

        return label.charAt(0).toUpperCase() + label.slice(1);

    }


    /* ================= HIỂN THỊ ================= */

    function renderTable(page) {

        document.getElementById("orderRowCount").textContent = page.totalElements + " đơn hàng";

        /* Danh sách mới nhất trước → các đơn cùng ngày đặt nằm liền nhau */
        const groups = [];

        currentOrders.forEach(function (order) {

            const key = isoDate(new Date(order.orderDate));

            const last = groups[groups.length - 1];

            if (last && last.key === key) {
                last.orders.push(order);
            } else {
                groups.push({ key: key, orders: [order] });
            }

        });

        tbody.innerHTML = groups.map(function (group) {

            return `
                <tr class="admin-date-group">
                    <th colspan="8" scope="colgroup">
                        ${escapeHtml(dayGroupLabel(group.key))}
                        <span>${group.orders.length} đơn</span>
                    </th>
                </tr>
            ` + group.orders.map(renderOrderRows).join("");

        }).join("") || adminEmptyRow(8,"Không có đơn hàng nào phù hợp bộ lọc");

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
                    <span class="admin-subtext" title="${escapeHtml(formatDateTimeVi(order.orderDate))}">${escapeHtml(new Date(order.orderDate).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }))}</span>
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


    /* Huy hiệu trạng thái + nút cho các bước kế tiếp (và việc đang chặn bước Xác nhận) */

    function statusCellHtml(order) {

        const id = escapeHtml(String(order.id));

        const label = STAFF_ORDER_STATUS_LABELS[order.status] || order.status;

        const badgeHtml = `<span class="admin-badge ${ORDER_STATUS_BADGE_CLASSES[order.status] || "admin-badge-neutral"}">${escapeHtml(label)}</span>`;

        const waiting = waitingFor(order);

        const waitingHtml = waiting ? `<span class="admin-subtext admin-waiting">${escapeHtml(waiting)}</span>` : "";

        const actionButton = function (action, text, extraClass, extraAttrs) {
            return `<button type="button" class="btn ${extraClass} admin-action-btn" data-action="${action}" data-id="${id}" ${extraAttrs || ""}>${text}</button>`;
        };

        const buttons = [];


        /* ADMIN chỉ xem; được gán / đổi chi nhánh cho đơn đang chờ */
        if (isAdmin) {

            if (order.status === "PENDING") {
                buttons.push(actionButton("store", order.storeId ? "ĐỔI CHI NHÁNH" : "GÁN CHI NHÁNH", "btn-outline-dark"));
            }

        } else {

            const plan = order.installment;

            const payment = order.payment;

            if (order.status === "PENDING" && plan && plan.status === "PENDING_APPROVAL") {
                buttons.push(actionButton("approve", "ĐỐI TÁC DUYỆT (THỬ)", "btn-outline-dark"));
                buttons.push(actionButton("reject", "ĐỐI TÁC TỪ CHỐI (THỬ)", "btn-outline-dark"));
            }

            if (order.status === "PENDING" && order.paymentMethod === "BANK_TRANSFER" && payment && payment.status === "PENDING") {
                buttons.push(actionButton("confirm-payment", "ĐÃ NHẬN TIỀN", "btn-dark"));
            }

            (ORDER_NEXT_STATUSES[order.status] || []).forEach(function (status) {

                if (status === "CANCELLED") {

                    buttons.push(actionButton("status", ORDER_STATUS_ACTION_LABELS[status], "btn-outline-dark admin-action-danger", `data-status="${status}"`));

                    return;

                }

                const blocked = status === "CONFIRMED" && waiting;

                buttons.push(actionButton(
                    "status",
                    ORDER_STATUS_ACTION_LABELS[status],
                    blocked ? "btn-outline-dark" : "btn-dark",
                    `data-status="${status}"` + (blocked ? ` disabled title="${escapeHtml(waiting)}"` : "")
                ));

            });

            if (payment && payment.status === "REFUND_PENDING") {
                buttons.push(actionButton("refund", "ĐÃ HOÀN TIỀN", "btn-dark"));
            }

        }

        return badgeHtml + waitingHtml +
            (buttons.length ? `<div class="admin-order-actions">${buttons.join("")}</div>` : "");

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

        if (order.paymentMethod === "CARD" && order.payment && order.payment.status !== "PAID") {
            return "Chờ khách thanh toán thẻ";
        }

        if (order.installment && order.installment.status !== "APPROVED" && order.installment.status !== "SETTLED") {
            return "Chờ đối tác duyệt trả góp";
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
                        ${Number(item.comboDiscount) > 0 ? `<span class="admin-subtext">Combo −${escapeHtml(formatPrice(Number(item.comboDiscount)))}</span>` : ""}
                    </td>
                    <td>${escapeHtml(formatPrice(Number(item.unitPrice)))}${oldPrice}</td>
                    <td>${escapeHtml(String(item.quantity))}</td>
                    <td>${escapeHtml(formatPrice(Number(item.subtotal)))}</td>
                </tr>
            `;

        }).join("");


        const canEditTracking = !isAdmin && order.status === "SHIPPING";

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
            ...(Number(order.comboDiscount) > 0
                ? [["Giảm combo", "−" + escapeHtml(formatPrice(Number(order.comboDiscount))) +
                    ((order.combos || []).length ? " (" + escapeHtml(order.combos.map(function (combo) { return combo.name; }).join(", ")) + ")" : "")]]
                : []),
            ...(Number(order.voucherDiscount) > 0
                ? [["Voucher " + escapeHtml(order.voucherCode || ""), "−" + escapeHtml(formatPrice(Number(order.voucherDiscount)))]]
                : []),
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

            if (plan.partnerName) {
                facts.push(["Đối tác", escapeHtml(plan.partnerName + (plan.attemptCount > 1 ? " (lần gửi " + plan.attemptCount + ")" : ""))]);
            }

            facts.push(["Kỳ hạn", escapeHtml(installmentTermText(plan))]);
            facts.push(["Số CCCD", escapeHtml(plan.citizenId)]);

            if (plan.cardBankName || plan.cardBank) {
                facts.push(["Ngân hàng thẻ", escapeHtml(plan.cardBankName || plan.cardBank)]);
            }

            if (plan.feeAmount != null) {
                facts.push(["Phí đối tác", escapeHtml(installmentFeeText(plan))]);
            }

            if (plan.reviewedAt) {
                facts.push([plan.status === "REJECTED" ? "Từ chối lúc" : "Duyệt lúc", escapeHtml(formatDateTimeVi(plan.reviewedAt))]);
            }

            if (plan.rejectionReason) {
                facts.push(["Lý do từ chối", escapeHtml(plan.rejectionReason)]);
            }

            if (plan.status === "ACTIVE" || plan.status === "COMPLETED") {
                facts.push(["Đã thu", escapeHtml(plan.paidPeriods + "/" + plan.numMonths + " kỳ · " + formatPrice(Number(plan.paidAmount)))]);
            }

            actions.push(`<a class="btn btn-outline-dark admin-action-btn" href="${escapeHtml(siteUrl("admin/installments.html?keyword=" + encodeURIComponent(order.code)))}">XEM HỒ SƠ TRẢ GÓP</a>`);

            if (!isAdmin && plan.status === "PENDING_APPROVAL") {
                actions.push(`<button type="button" class="btn btn-outline-dark admin-action-btn" data-action="approve" data-id="${id}">ĐỐI TÁC DUYỆT (THỬ NGHIỆM)</button>`);
                actions.push(`<button type="button" class="btn btn-outline-dark admin-action-btn" data-action="reject" data-id="${id}">ĐỐI TÁC TỪ CHỐI (THỬ NGHIỆM)</button>`);
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

            if (!isAdmin && order.paymentMethod === "BANK_TRANSFER" && payment.status === "PENDING") {
                actions.push(`<button type="button" class="btn btn-dark admin-action-btn" data-action="confirm-payment" data-id="${id}">ĐÃ NHẬN TIỀN</button>`);
            }

            if (!isAdmin && payment.status === "REFUND_PENDING") {
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
                : order.installment ? " Sau khi giao, ghi nhận khi đối tác chuyển tiền ở trang Trả góp." : "";

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
            title: "Đối tác duyệt trả góp đơn " + order.code + "? (thử nghiệm)",
            message: "Giả lập câu trả lời của " + (order.installment.partnerName || "đối tác") + ": đơn được tính là đã thanh toán và khách nhận email. "
                + "Chỉ dùng khi bật chế độ thử nghiệm.",
            confirmLabel: "DUYỆT",
            cancelLabel: "Quay lại",
            onConfirm: function () {
                postAction(order, "installment/approve", undefined, "Đối tác đã duyệt trả góp đơn " + order.code + ".");
            }
        });

    }


    function rejectInstallment(order) {

        openConfirmModal({
            title: "Đối tác từ chối trả góp đơn " + order.code + "? (thử nghiệm)",
            message: "Giả lập câu trả lời của " + (order.installment.partnerName || "đối tác") + ". Đơn vẫn còn, khách thấy lý do bên dưới và chọn đối tác khác hoặc cách thanh toán khác.",
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

                postAction(order, "installment/reject", { reason: reason }, "Đối tác đã từ chối trả góp đơn " + order.code + ".");

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
                    text = message + ". Hãy nhập thêm hàng hoặc báo ADMIN chuyển đơn sang chi nhánh khác.";
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
