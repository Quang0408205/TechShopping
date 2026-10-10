/* ================= TRẢ GÓP (admin/installments.html) — Phase 5 ================= */

/*
 * API thật (STAFF và ADMIN; backend kiểm tra lại vai trò trong DB ở mọi request):
 *   GET  /admin/installments?keyword=&status=&overdue=true&page=&size=   mới nhất trước
 *   POST /admin/installments/{id}/periods/{number}/pay { transactionId? }
 * Chỉ ghi nhận được kỳ sớm nhất chưa thu (nextPeriod) của hợp đồng đang trả góp;
 * 409 INSTALLMENT_PERIOD_OUT_OF_ORDER / INVALID_INSTALLMENT_STATUS (người khác vừa ghi) → báo và tải lại.
 * ?keyword= trên URL (link "Xem lịch trả góp" ở trang Đơn hàng) được điền sẵn vào ô tìm.
 */

const INSTALLMENT_PAGE_SIZE = 20;

const INSTALLMENT_ORDER_STATUS_LABELS = {
    PENDING: "Chờ xác nhận",
    CONFIRMED: "Đã xác nhận",
    SHIPPING: "Đang giao hàng",
    DELIVERED: "Đã giao hàng",
    CANCELLED: "Đã hủy"
};


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const isAdmin = staff.role === "ADMIN";

    const form = document.getElementById("installmentFilterForm");

    const keywordInput = document.getElementById("installmentKeyword");

    const statusFilter = document.getElementById("installmentStatusFilter");

    const overdueFilter = document.getElementById("installmentOverdueFilter");

    const tbody = document.getElementById("installmentTableBody");

    const pagination = document.getElementById("installmentPagination");


    let currentPage = 0;

    let currentPlans = [];

    let requestCounter = 0;

    const expandedIds = new Set();


    keywordInput.value = new URLSearchParams(window.location.search).get("keyword") || "";

    /* ?status=PENDING_APPROVAL (lối tắt ở trang Tổng quan); giá trị lạ thì bỏ qua */
    const initialStatus = new URLSearchParams(window.location.search).get("status");

    if (initialStatus && Array.from(statusFilter.options).some(function (o) { return o.value === initialStatus; })) {
        statusFilter.value = initialStatus;
    }


    form.addEventListener("submit", function (event) {

        event.preventDefault();

        reloadFromFirstPage();

    });

    [statusFilter, overdueFilter].forEach(function (control) {
        control.addEventListener("change", reloadFromFirstPage);
    });


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const entry = button && findPlan(button.dataset.id);

        if (!entry) {
            return;
        }

        if (button.dataset.action === "toggle") {
            toggleDetail(entry.installment.id);
        } else if (button.dataset.action === "pay") {
            payNextPeriod(entry);
        }

    });


    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        loadPlans();

    });


    loadPlans();


    /* ================= TẢI DANH SÁCH ================= */

    function reloadFromFirstPage() {

        currentPage = 0;

        loadPlans();

    }


    async function loadPlans() {

        const requestId = ++requestCounter;

        const params = new URLSearchParams({
            page: String(currentPage),
            size: String(INSTALLMENT_PAGE_SIZE)
        });

        const keyword = keywordInput.value.trim();

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (statusFilter.value) {
            params.set("status", statusFilter.value);
        }

        if (overdueFilter.checked) {
            params.set("overdue", "true");
        }


        tbody.innerHTML = adminEmptyRow(7, "Đang tải…");


        try {

            const page = await apiRequest("/admin/installments?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            /* Hợp đồng cuối của trang cuối vừa rời bộ lọc (vd. vừa trả đủ) → lùi một trang */
            if ((page.content || []).length === 0 && currentPage > 0 && page.totalPages > 0) {

                currentPage = page.totalPages - 1;

                loadPlans();

                return;

            }

            currentPlans = page.content || [];

            renderTable(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(7, message);
                    pagination.innerHTML = "";
                });
            }

        }

    }


    function findPlan(id) {

        return currentPlans.find(function (entry) {
            return String(entry.installment.id) === String(id);
        });

    }


    /* ================= HIỂN THỊ ================= */

    function renderTable(page) {

        document.getElementById("installmentRowCount").textContent = page.totalElements + " hợp đồng";

        tbody.innerHTML = currentPlans.map(renderPlanRows).join("") ||
            adminEmptyRow(7, "Không có hợp đồng trả góp nào phù hợp bộ lọc");

        renderPagination(page);

    }


    function renderPlanRows(entry) {

        const plan = entry.installment;

        const order = entry.order;

        const customer = entry.customer || {};

        const id = escapeHtml(String(plan.id));

        const expanded = expandedIds.has(plan.id);

        const next = entry.nextPeriod;

        const started = plan.status === "ACTIVE" || plan.status === "COMPLETED";


        const nextHtml = next
            ? `Kỳ ${escapeHtml(String(next.number))} · ${escapeHtml(formatDateVi(next.dueDate))}
               <span class="admin-subtext">${escapeHtml(formatPrice(Number(next.amount)))}</span>
               ${next.overdue ? '<span class="admin-badge admin-badge-danger">Quá hạn</span>' : ""}`
            : "—";

        const payButton = !isAdmin && plan.status === "ACTIVE" && next
            ? `<div class="admin-pay-cell"><button type="button" class="btn btn-dark admin-action-btn" data-action="pay" data-id="${id}">GHI NHẬN KỲ ${escapeHtml(String(next.number))}</button></div>`
            : "";


        return `
            <tr data-installment-id="${id}">
                <td>
                    <button
                        type="button"
                        class="admin-row-toggle"
                        data-action="toggle"
                        data-id="${id}"
                        aria-label="Xem lịch trả góp đơn ${escapeHtml(order.code)}"
                        aria-expanded="${expanded}"
                    >${expanded ? "▾" : "▸"}</button>
                </td>
                <td>
                    <strong>${escapeHtml(order.code)}</strong>
                    <span class="admin-subtext">${escapeHtml(order.deliveredAt
                        ? "Giao " + formatDateVi(order.deliveredAt)
                        : INSTALLMENT_ORDER_STATUS_LABELS[order.status] || order.status)}</span>
                </td>
                <td>
                    ${escapeHtml(customer.fullname || customer.username || "—")}
                    ${customer.deleted ? ' <span class="admin-badge admin-badge-danger">Đã xoá</span>' : ""}
                    <span class="admin-subtext">${escapeHtml(customer.email || "")}</span>
                </td>
                <td>
                    ${escapeHtml(plan.numMonths + " tháng")}
                    <span class="admin-subtext">${escapeHtml(formatPrice(Number(plan.monthlyPayment)) + "/kỳ")}</span>
                </td>
                <td>
                    ${started ? escapeHtml(plan.paidPeriods + "/" + plan.numMonths + " kỳ · " + formatPrice(Number(plan.paidAmount))) : "—"}
                    <span class="admin-subtext">${escapeHtml("Còn " + formatPrice(Number(plan.remainingAmount)))}</span>
                </td>
                <td>${nextHtml}${payButton}</td>
                <td>${installmentBadgeHtml(plan)}</td>
            </tr>
            <tr class="js-installment-detail" data-installment-id="${id}" ${expanded ? "" : "hidden"}>
                <td></td>
                <td colspan="6">${planDetailHtml(entry)}</td>
            </tr>
        `;

    }


    function planDetailHtml(entry) {

        const plan = entry.installment;

        const order = entry.order;

        const facts = [
            ["Số CCCD", escapeHtml(plan.citizenId)],
            ["Ngân hàng thẻ", escapeHtml(plan.cardBankName || plan.cardBank)],
            ["Người nhận", escapeHtml(order.recipientName + " · " + order.recipientPhone)],
            ["Tổng trả góp", escapeHtml(formatPrice(Number(plan.totalAmount)) + " · lãi suất 0%")],
            ["Kỳ hạn", escapeHtml(installmentTermText(plan))],
            ["Trạng thái đơn", escapeHtml(INSTALLMENT_ORDER_STATUS_LABELS[order.status] || order.status)]
        ];

        if (plan.reviewedAt) {
            facts.push([plan.status === "REJECTED" ? "Từ chối lúc" : "Duyệt lúc", escapeHtml(formatDateTimeVi(plan.reviewedAt))]);
        }

        if (plan.rejectionReason) {
            facts.push(["Lý do từ chối", escapeHtml(plan.rejectionReason)]);
        }


        return `
            <div class="admin-order-detail">
                <dl class="admin-order-facts">
                    ${facts.map(function (fact) {
                        return `<div><dt>${fact[0]}</dt><dd>${fact[1]}</dd></div>`;
                    }).join("")}
                </dl>
                ${installmentScheduleHtml(plan)}
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


    function toggleDetail(planId) {

        const detailRow = tbody.querySelector('.js-installment-detail[data-installment-id="' + planId + '"]');

        const button = tbody.querySelector('button[data-action="toggle"][data-id="' + planId + '"]');

        if (!detailRow || !button) {
            return;
        }

        detailRow.hidden = !detailRow.hidden;

        if (detailRow.hidden) {
            expandedIds.delete(planId);
        } else {
            expandedIds.add(planId);
        }

        button.textContent = detailRow.hidden ? "▸" : "▾";

        button.setAttribute("aria-expanded", String(!detailRow.hidden));

    }


    /* ================= GHI NHẬN KỲ ================= */

    function payNextPeriod(entry) {

        const plan = entry.installment;

        const next = entry.nextPeriod;

        if (plan.status !== "ACTIVE" || !next) {
            return;
        }

        const last = next.number === plan.numMonths;

        openPaymentConfirmModal({
            title: "Ghi nhận kỳ " + next.number + "/" + plan.numMonths + " đơn " + entry.order.code + "?",
            message: "Đã thu đủ " + formatPrice(Number(next.amount)) + " (hạn " + formatDateVi(next.dueDate) + ")."
                + (last ? " Đây là kỳ cuối: hợp đồng sẽ hoàn tất." : ""),
            confirmLabel: "ĐÃ THU",
            onConfirm: function (body) {
                recordPeriod(entry, next.number, body);
            }
        });

    }


    async function recordPeriod(entry, number, body) {

        const planId = entry.installment.id;

        tbody.querySelectorAll('[data-id="' + planId + '"]').forEach(function (control) {
            control.disabled = true;
        });


        try {

            const updated = await apiRequest("/admin/installments/" + planId + "/periods/" + number + "/pay", {
                method: "POST",
                body: body,
                auth: true
            });

            showToast(updated.installment.status === "COMPLETED"
                ? "Đã ghi nhận kỳ " + number + ". Hợp đồng đơn " + entry.order.code + " đã trả đủ."
                : "Đã ghi nhận kỳ " + number + " của đơn " + entry.order.code + ".", "success");

        } catch (error) {

            handleError(error, function (message) {

                showToast(
                    error.code === "INSTALLMENT_PERIOD_OUT_OF_ORDER" || error.code === "INVALID_INSTALLMENT_STATUS"
                        ? message.replace(/\.?$/, ".") + " Danh sách đã được tải lại."
                        : message,
                    "error"
                );

            });

        }

        loadPlans();

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
