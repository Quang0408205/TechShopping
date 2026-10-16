/* ================= ĐỐI SOÁT CHUYỂN KHOẢN (admin/bank-transactions.html) ================= */

/*
 * Chỉ ADMIN. Tiền khách chuyển về tài khoản công ty do webhook ngân hàng báo; đơn khớp mã + đúng số tiền được
 * tự xác nhận, phần còn lại nằm ở đây cho ADMIN xử lý.
 *   GET  /admin/bank-transactions?status=&attention=&page=&size=
 *   GET  /admin/bank-transactions/info
 *   POST /admin/bank-transactions/{id}/assign   { orderId, note }
 *   POST /admin/bank-transactions/{id}/ignore   { note }
 *   POST /admin/bank-transactions/simulate      { content, amount }   (chỉ khi PAYMENT_SIMULATOR_ENABLED)
 * Nội dung chuyển khoản do người ngoài nhập nên luôn escape.
 */

const BANK_PAGE_SIZE = 20;

const BANK_STATUS_LABELS = {
    MATCHED: "Đã tự khớp",
    UNMATCHED: "Chưa có đơn",
    REVIEW: "Cần xem lại",
    RESOLVED: "Đã gán tay",
    IGNORED: "Đã bỏ qua"
};

const BANK_STATUS_TONES = {
    MATCHED: "success",
    UNMATCHED: "warning",
    REVIEW: "danger",
    RESOLVED: "info",
    IGNORED: "neutral"
};


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const statusFilter = document.getElementById("bankStatusFilter");
    const tbody = document.getElementById("bankTableBody");
    const pagination = document.getElementById("bankPagination");

    let currentPage = 0;
    let currentRows = [];
    let requestCounter = 0;


    statusFilter.addEventListener("change", function () {
        currentPage = 0;
        load();
    });

    document.getElementById("bankFilterForm").addEventListener("submit", function (event) {
        event.preventDefault();
    });


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const row = button && currentRows.find(function (item) {
            return String(item.id) === button.dataset.id;
        });

        if (!row) {
            return;
        }

        if (button.dataset.action === "assign") {
            openModal(row, button, "assign");
        } else if (button.dataset.action === "ignore") {
            openModal(row, button, "ignore");
        }

    });


    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        load();

    });


    loadInfo();

    load();


    /* ================= THÔNG TIN THIẾT LẬP ================= */

    async function loadInfo() {

        try {

            const info = await apiRequest("/admin/bank-transactions/info", { auth: true });

            document.getElementById("bankAccountLine").textContent =
                info.bankName + " · số tài khoản " + info.accountNumberMasked;

            document.getElementById("bankAttentionCount").textContent =
                info.needsAttention > 0 ? info.needsAttention + " giao dịch cần xử lý" : "Không có giao dịch cần xử lý";

            const warnings = [];

            if (info.demoAccount) {
                warnings.push("Đang dùng tài khoản DEMO: đặt PAYMENT_BANK_ACCOUNT_NUMBER trong .env để QR trỏ về tài khoản thật.");
            }

            if (!info.webhookConfigured) {
                warnings.push("Webhook ngân hàng đang tắt: đặt PAYMENT_WEBHOOK_SECRET trong .env để nhận báo biến động số dư.");
            }

            const warning = document.getElementById("bankSetupWarning");

            warning.textContent = warnings.join(" ");

            warning.hidden = warnings.length === 0;

            document.getElementById("bankSimulatorPanel").hidden = !info.simulatorEnabled;

        } catch (error) {

            handleError(error, function (message) {
                document.getElementById("bankAccountLine").textContent = message;
            });

        }

    }


    /* ================= GIẢ LẬP ================= */

    document.getElementById("bankSimulatorForm").addEventListener("submit", async function (event) {

        event.preventDefault();

        const content = document.getElementById("bankSimContent").value.trim();

        const amount = Number(document.getElementById("bankSimAmount").value);

        const result = document.getElementById("bankSimResult");

        const submit = document.getElementById("bankSimSubmit");

        if (!content || !(amount > 0)) {
            return;
        }

        submit.disabled = true;

        try {

            const answer = await apiRequest("/admin/bank-transactions/simulate", {
                method: "POST", body: { content: content, amount: amount }, auth: true
            });

            result.textContent = "Kết quả: " + (BANK_STATUS_LABELS[answer.status] || answer.status)
                + (answer.orderCode ? " · đơn " + answer.orderCode : "");

            result.hidden = false;

            showToast("Đã giả lập tiền về.", "success");

            loadInfo();

            load();

        } catch (error) {

            handleError(error, function (message) {
                result.textContent = message;
                result.hidden = false;
            });

        } finally {

            submit.disabled = false;

        }

    });


    /* ================= TẢI DANH SÁCH ================= */

    async function load() {

        const requestId = ++requestCounter;

        const params = new URLSearchParams({ page: String(currentPage), size: String(BANK_PAGE_SIZE) });

        if (statusFilter.value === "ATTENTION") {
            params.set("attention", "true");
        } else if (statusFilter.value) {
            params.set("status", statusFilter.value);
        }


        tbody.innerHTML = adminEmptyRow(7, "Đang tải…");

        try {

            const page = await apiRequest("/admin/bank-transactions?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            currentRows = page.content || [];

            document.getElementById("bankRowCount").textContent = page.totalElements + " giao dịch";

            tbody.innerHTML = currentRows.map(rowHtml).join("") ||
                adminEmptyRow(7, statusFilter.value === "ATTENTION" ? "Không có giao dịch nào cần xử lý" : "Chưa có giao dịch nào");

            renderPagination(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    document.getElementById("bankRowCount").textContent = "";
                    tbody.innerHTML = adminEmptyRow(7, message);
                    pagination.innerHTML = "";
                });
            }

        }

    }


    function rowHtml(item) {

        const id = escapeHtml(item.id);

        const needsAttention = item.status === "UNMATCHED" || item.status === "REVIEW";

        const orderCell = item.orderCode
            ? `<a href="orders.html?keyword=${encodeURIComponent(item.orderCode)}">${escapeHtml(item.orderCode)}</a>`
            : "—";

        const detail = [item.reason, item.note ? "Ghi chú: " + item.note : ""].filter(Boolean).join(" · ");

        return `
            <tr data-bank-id="${id}">
                <td><strong>${escapeHtml(item.externalId)}</strong><span class="admin-subtext">${escapeHtml(item.provider)}</span></td>
                <td>${escapeHtml(formatDateTimeVi(item.receivedAt))}</td>
                <td>${escapeHtml(formatPrice(item.amount))}</td>
                <td>${escapeHtml(truncateText(item.content || "", 80))}</td>
                <td>${orderCell}</td>
                <td>
                    <span class="admin-badge admin-badge-${BANK_STATUS_TONES[item.status] || "neutral"}">${escapeHtml(BANK_STATUS_LABELS[item.status] || item.status)}</span>
                    ${detail ? `<span class="admin-subtext">${escapeHtml(detail)}</span>` : ""}
                </td>
                <td class="admin-actions-cell">${needsAttention ? `
                    <button type="button" class="admin-link-btn" data-action="assign" data-id="${id}">Gán vào đơn</button>
                    <button type="button" class="admin-link-btn" data-action="ignore" data-id="${id}">Bỏ qua</button>` : ""}
                </td>
            </tr>
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


    /* ================= GÁN / BỎ QUA ================= */

    function openModal(item, opener, mode) {

        closeModal();

        const assigning = mode === "assign";

        const overlay = document.createElement("div");

        overlay.className = "modal-overlay";

        overlay.innerHTML = `
            <div class="modal-box admin-action-modal" role="dialog" aria-modal="true" aria-labelledby="bankModalTitle">
                <h3 id="bankModalTitle">${assigning ? "Gán giao dịch vào đơn" : "Bỏ qua giao dịch"} ${escapeHtml(item.externalId)}</h3>
                <p>${escapeHtml(formatPrice(item.amount))} · ${escapeHtml(item.content || "không có nội dung")}</p>
                ${assigning ? `
                <label class="modal-field">
                    <span>Mã đơn hàng (ví dụ DH00000042 hoặc 42)</span>
                    <input type="text" name="order" maxlength="20" autocomplete="off">
                </label>` : ""}
                <label class="modal-field">
                    <span>${assigning ? "Ghi chú (không bắt buộc)" : "Lý do bỏ qua (bắt buộc)"}</span>
                    <textarea name="note" rows="3" maxlength="500"></textarea>
                </label>
                <p class="form-error" data-role="error" role="alert" hidden></p>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline-dark" data-role="cancel">Quay lại</button>
                    <button type="button" class="btn btn-dark" data-role="confirm">${assigning ? "GÁN VÀO ĐƠN" : "BỎ QUA"}</button>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        modalOverlayElement = overlay;

        const orderInput = overlay.querySelector('input[name="order"]');

        const textarea = overlay.querySelector("textarea");

        const errorBox = overlay.querySelector('[data-role="error"]');

        const confirmButton = overlay.querySelector('[data-role="confirm"]');


        function close() {

            if (modalOverlayElement === overlay) {
                closeModal();
            }

            if (opener && document.body.contains(opener)) {
                opener.focus();
            }

        }

        function showError(message) {
            errorBox.textContent = message;
            errorBox.hidden = false;
        }


        overlay.querySelector('[data-role="cancel"]').addEventListener("click", close);

        overlay.addEventListener("click", function (event) {
            if (event.target === overlay) {
                close();
            }
        });

        overlay.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                close();
            }
        });


        confirmButton.addEventListener("click", async function () {

            const note = textarea.value.trim();

            let url = "/admin/bank-transactions/" + item.id + "/";

            let body;

            if (assigning) {

                const digits = (orderInput.value.match(/\d+/) || [""])[0];

                if (!digits) {
                    showError("Vui lòng nhập mã đơn hàng.");
                    orderInput.focus();
                    return;
                }

                url += "assign";

                body = { orderId: Number(digits), note: note || null };

            } else {

                if (!note) {
                    showError("Vui lòng nhập lý do bỏ qua.");
                    textarea.focus();
                    return;
                }

                url += "ignore";

                body = { note: note };

            }

            confirmButton.disabled = true;

            try {

                await apiRequest(url, { method: "POST", body: body, auth: true });

                showToast(assigning ? "Đã gán giao dịch vào đơn." : "Đã bỏ qua giao dịch.", "success");

                close();

                loadInfo();

                load();

            } catch (error) {

                if (error.status === 409) {
                    load();
                }

                handleError(error, showError, error.details && error.details.note ? error.details.note : null);

            } finally {

                confirmButton.disabled = false;

            }

        });

        (orderInput || textarea).focus();

    }


    function handleError(error, show, message) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(message || getErrorMessage(error));

    }

});
