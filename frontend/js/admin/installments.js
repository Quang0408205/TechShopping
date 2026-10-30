/* ================= TRẢ GÓP (admin/installments.html) ================= */

/*
 * Hồ sơ trả góp do đối tác (thẻ tín dụng, công ty tài chính) xét và thu tiền; cửa hàng chỉ theo dõi và đối soát.
 * API thật (STAFF và ADMIN; backend kiểm tra lại vai trò trong DB ở mọi request):
 *   GET  /admin/installments?keyword=&status=&partner=&page=&size=   mới nhất trước
 *   POST /admin/installments/{id}/settle     đối tác đã chuyển tiền (hồ sơ đã duyệt, đơn đã giao)
 *   GET  /admin/installment-partners         (ADMIN) điều kiện + bảng phí của đối tác
 *   PUT  /admin/installment-partners/{code}  (ADMIN) { active, minOrderTotal, maxOrderTotal, rates: [{ numMonths, feePercent }] }
 * Hợp đồng cũ (trước khi có đối tác) vẫn xem được cùng lịch kỳ nhưng không thao tác.
 * ?keyword= và ?status= trên URL (link ở trang Đơn hàng và Tổng quan) được điền sẵn vào bộ lọc.
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

    const partnerFilter = document.getElementById("installmentPartnerFilter");

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

    [statusFilter, partnerFilter].forEach(function (control) {
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
        } else if (button.dataset.action === "settle") {
            settlePlan(entry);
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

    if (isAdmin) {
        setupPartnerPanel();
    }


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

        if (partnerFilter.value) {
            params.set("partner", partnerFilter.value);
        }


        tbody.innerHTML = adminEmptyRow(7, "Đang tải…");


        try {

            const page = await apiRequest("/admin/installments?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            /* Hồ sơ cuối của trang cuối vừa rời bộ lọc → lùi một trang */
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

        document.getElementById("installmentRowCount").textContent = page.totalElements + " hồ sơ";

        tbody.innerHTML = currentPlans.map(renderPlanRows).join("") ||
            adminEmptyRow(7, "Không có hồ sơ trả góp nào phù hợp bộ lọc");

        renderPagination(page);

    }


    function renderPlanRows(entry) {

        const plan = entry.installment;

        const order = entry.order;

        const customer = entry.customer || {};

        const id = escapeHtml(String(plan.id));

        const expanded = expandedIds.has(plan.id);

        const canSettle = !isAdmin && plan.status === "APPROVED" && plan.partner && order.status === "DELIVERED";

        const settleButton = canSettle
            ? `<div class="admin-pay-cell"><button type="button" class="btn btn-dark admin-action-btn" data-action="settle" data-id="${id}">ĐÃ NHẬN TIỀN TỪ ĐỐI TÁC</button></div>`
            : "";


        return `
            <tr data-installment-id="${id}">
                <td>
                    <button
                        type="button"
                        class="admin-row-toggle"
                        data-action="toggle"
                        data-id="${id}"
                        aria-label="Xem hồ sơ trả góp đơn ${escapeHtml(order.code)}"
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
                    ${escapeHtml(plan.partnerName || "—")}
                    <span class="admin-subtext">${escapeHtml(plan.partnerRef || (plan.partner ? "" : "Hợp đồng cũ"))}</span>
                </td>
                <td>
                    ${escapeHtml(plan.numMonths + " tháng")}
                    <span class="admin-subtext">${escapeHtml(formatPrice(Number(plan.monthlyPayment)) + "/kỳ")}</span>
                </td>
                <td>
                    ${plan.feeAmount != null ? escapeHtml(installmentFeeText(plan)) : "—"}
                    ${plan.settledAt ? `<span class="admin-subtext">Nhận tiền ${escapeHtml(formatDateTimeVi(plan.settledAt))}</span>` : ""}
                </td>
                <td>${installmentBadgeHtml(plan)}${settleButton}</td>
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
            ["Người nhận", escapeHtml(order.recipientName + " · " + order.recipientPhone)],
            ["Tổng trả góp", escapeHtml(formatPrice(Number(plan.totalAmount)) + " · lãi suất 0% với khách")],
            ["Kỳ hạn", escapeHtml(installmentTermText(plan))],
            ["Trạng thái đơn", escapeHtml(INSTALLMENT_ORDER_STATUS_LABELS[order.status] || order.status)]
        ];

        if (plan.cardBankName || plan.cardBank) {
            facts.splice(1, 0, ["Ngân hàng thẻ", escapeHtml(plan.cardBankName || plan.cardBank)]);
        }

        if (plan.attemptCount > 1) {
            facts.push(["Số lần gửi hồ sơ", escapeHtml(String(plan.attemptCount))]);
        }

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


    /* ================= ĐỐI SOÁT: ĐỐI TÁC ĐÃ CHUYỂN TIỀN ================= */

    function settlePlan(entry) {

        const plan = entry.installment;

        openConfirmModal({
            title: "Đã nhận tiền từ " + plan.partnerName + " cho đơn " + entry.order.code + "?",
            message: "Đối tác chuyển " + formatPrice(Number(plan.netAmount)) + " (đơn " + formatPrice(Number(plan.totalAmount))
                + " trừ phí " + formatPrice(Number(plan.feeAmount)) + ").",
            confirmLabel: "ĐÃ NHẬN",
            cancelLabel: "Quay lại",
            onConfirm: function () {
                recordSettlement(entry);
            }
        });

    }


    async function recordSettlement(entry) {

        const planId = entry.installment.id;

        tbody.querySelectorAll('[data-id="' + planId + '"]').forEach(function (control) {
            control.disabled = true;
        });


        try {

            await apiRequest("/admin/installments/" + planId + "/settle", { method: "POST", auth: true });

            showToast("Đã ghi nhận đối tác chuyển tiền đơn " + entry.order.code + ".", "success");

        } catch (error) {

            handleError(error, function (message) {

                showToast(
                    error.code === "INVALID_INSTALLMENT_STATUS" ? message.replace(/\.?$/, ".") + " Danh sách đã được tải lại." : message,
                    "error"
                );

            });

        }

        loadPlans();

    }


    /* ================= ĐỐI TÁC VÀ PHÍ (ADMIN) ================= */

    async function setupPartnerPanel() {

        const panel = document.getElementById("partnerPanel");

        const body = document.getElementById("partnerTableBody");

        let partners;

        try {

            partners = await apiRequest("/admin/installment-partners", { auth: true });

        } catch (error) {

            return;

        }

        panel.hidden = false;

        body.innerHTML = partners.map(function (partner) {

            const code = escapeHtml(partner.code);

            const rates = partner.rates.map(function (rate) {
                return rate.numMonths + "=" + Number(rate.feePercent);
            }).join(", ");

            return `
                <tr data-partner="${code}">
                    <td><strong>${escapeHtml(partner.name)}</strong><span class="admin-subtext">${code}</span></td>
                    <td><input type="number" min="0" step="1" class="js-partner-min" value="${escapeHtml(String(Number(partner.minOrderTotal)))}" aria-label="Đơn tối thiểu ${code}"></td>
                    <td><input type="number" min="0" step="1" class="js-partner-max" value="${partner.maxOrderTotal == null ? "" : escapeHtml(String(Number(partner.maxOrderTotal)))}" aria-label="Đơn tối đa ${code}"></td>
                    <td><input type="text" class="js-partner-rates" value="${escapeHtml(rates)}" aria-label="Phí theo kỳ hạn ${code}"></td>
                    <td><input type="checkbox" class="js-partner-active" ${partner.active ? "checked" : ""} aria-label="Đối tác ${code} hoạt động"></td>
                    <td><button type="button" class="btn btn-dark admin-action-btn" data-partner-save="${code}">LƯU</button></td>
                </tr>
            `;

        }).join("");


        body.addEventListener("click", async function (event) {

            const button = event.target.closest("button[data-partner-save]");

            if (!button) {
                return;
            }

            const row = button.closest("tr");

            const rates = parseRates(row.querySelector(".js-partner-rates").value);

            if (!rates) {

                showToast("Phí nhập theo dạng 3=2, 6=3 (kỳ hạn tháng = phần trăm phí).", "error");

                return;

            }

            const maxValue = row.querySelector(".js-partner-max").value.trim();

            button.disabled = true;

            try {

                await apiRequest("/admin/installment-partners/" + encodeURIComponent(button.dataset.partnerSave), {
                    method: "PUT",
                    auth: true,
                    body: {
                        active: row.querySelector(".js-partner-active").checked,
                        minOrderTotal: Number(row.querySelector(".js-partner-min").value || 0),
                        maxOrderTotal: maxValue === "" ? null : Number(maxValue),
                        rates: rates
                    }
                });

                showToast("Đã lưu đối tác " + button.dataset.partnerSave + ".", "success");

            } catch (error) {

                handleError(error, function (message) {
                    showToast(message, "error");
                });

            } finally {

                button.disabled = false;

            }

        });

    }


    /* "3=2, 6=3.5" → [{ numMonths: 3, feePercent: 2 }, …]; sai định dạng → null */
    function parseRates(text) {

        const rates = [];

        for (const part of text.split(",")) {

            const match = part.trim().match(/^(\d{1,2})\s*=\s*(\d{1,3}(?:\.\d{1,2})?)$/);

            if (!match) {
                return null;
            }

            rates.push({ numMonths: Number(match[1]), feePercent: Number(match[2]) });

        }

        return rates;

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
