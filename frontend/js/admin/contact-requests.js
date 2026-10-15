/* ================= LIÊN HỆ KHÁCH HÀNG (admin/contact-requests.html) — kiemthu GĐ7 ================= */

/*
 * API thật (một hộp thư chung cho nhân viên / quản lý mọi chi nhánh; ADMIN chỉ xem):
 *   GET   /admin/contact-requests?keyword=&status=&topic=&page=&size=
 *   PATCH /admin/contact-requests/{id}  { status, staffNote }
 * Luồng: Mới → Đang xử lý → Đã xử lý (bắt buộc ghi chú), Đã xử lý mở lại được; không quay về Mới.
 */

const CONTACT_PAGE_SIZE = 20;

const CONTACT_NOTE_MAX_LENGTH = 2000;

const CONTACT_STATUS_LABELS = {
    NEW: "Mới",
    IN_PROGRESS: "Đang xử lý",
    RESOLVED: "Đã xử lý"
};

const CONTACT_STATUS_TONES = {
    NEW: "warning",
    IN_PROGRESS: "info",
    RESOLVED: "success"
};

const CONTACT_TOPIC_LABELS = {
    ORDER: "Đơn hàng",
    PRODUCT: "Sản phẩm",
    AFTER_SALES: "Bảo hành / trả hàng",
    PAYMENT: "Thanh toán / trả góp",
    OTHER: "Khác"
};


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const form = document.getElementById("contactFilterForm");
    const keywordInput = document.getElementById("contactKeyword");
    const statusFilter = document.getElementById("contactStatusFilter");
    const topicFilter = document.getElementById("contactTopicFilter");
    const tbody = document.getElementById("contactTableBody");
    const pagination = document.getElementById("contactPagination");
    const isAdmin = staff.role === "ADMIN";

    let currentPage = 0;
    let currentRows = [];
    let requestCounter = 0;
    const expandedIds = new Set();


    /* admin/contact-requests.html?status=NEW (thẻ trên trang Tổng quan) */
    const query = new URLSearchParams(window.location.search);

    if (CONTACT_STATUS_LABELS[query.get("status")]) {
        statusFilter.value = query.get("status");
    }


    form.addEventListener("submit", function (event) {
        event.preventDefault();
        reloadFromFirstPage();
    });

    [statusFilter, topicFilter].forEach(function (control) {
        control.addEventListener("change", reloadFromFirstPage);
    });


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const row = button && currentRows.find(function (item) {
            return String(item.id) === button.dataset.id;
        });

        if (!row) {
            return;
        }

        const action = button.dataset.action;

        if (action === "toggle") {
            toggleDetail(row.id, button);
        } else if (action === "take") {
            update(row, { status: "IN_PROGRESS" }, "Đã nhận xử lý tin #" + row.id + ".");
        } else if (action === "resolve") {
            openNoteModal(row, button, "RESOLVED");
        } else if (action === "note") {
            openNoteModal(row, button, row.status);
        } else if (action === "reopen") {
            update(row, { status: "IN_PROGRESS" }, "Đã mở lại tin #" + row.id + ".");
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


    load();


    /* ================= TẢI DANH SÁCH ================= */

    function reloadFromFirstPage() {

        currentPage = 0;

        expandedIds.clear();

        load();

    }


    async function load() {

        const requestId = ++requestCounter;

        const params = new URLSearchParams({ page: String(currentPage), size: String(CONTACT_PAGE_SIZE) });

        [
            ["keyword", keywordInput.value.trim()],
            ["status", statusFilter.value],
            ["topic", topicFilter.value]
        ].forEach(function (pair) {
            if (pair[1]) {
                params.set(pair[0], pair[1]);
            }
        });


        tbody.innerHTML = adminEmptyRow(8, "Đang tải…");

        try {

            const page = await apiRequest("/admin/contact-requests?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            currentRows = page.content || [];

            render(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    document.getElementById("contactRowCount").textContent = "";
                    tbody.innerHTML = adminEmptyRow(8, message);
                    pagination.innerHTML = "";
                });
            }

        }

    }


    /* ================= HIỂN THỊ ================= */

    function render(page) {

        document.getElementById("contactRowCount").textContent = page.totalElements + " tin nhắn";

        tbody.innerHTML = currentRows.map(rowHtml).join("") ||
            adminEmptyRow(8, "Không có tin nhắn nào phù hợp bộ lọc");

        renderPagination(page);

    }


    function rowHtml(item) {

        const id = escapeHtml(item.id);

        const expanded = expandedIds.has(item.id);

        return `
            <tr data-contact-id="${id}">
                <td>
                    <button
                        type="button"
                        class="admin-row-toggle"
                        data-action="toggle"
                        data-id="${id}"
                        aria-label="Xem đầy đủ tin #${id}"
                        aria-expanded="${expanded}"
                    >${expanded ? "▾" : "▸"}</button>
                </td>
                <td><strong>#${id}</strong></td>
                <td>
                    ${escapeHtml(item.fullName)}
                    <span class="admin-subtext">${escapeHtml(item.email)}${item.phone ? " · " + escapeHtml(item.phone) : ""}</span>
                </td>
                <td>${escapeHtml(CONTACT_TOPIC_LABELS[item.topic] || item.topic)}</td>
                <td>${escapeHtml(truncateText(item.message, 80))}</td>
                <td>${escapeHtml(formatDateTimeVi(item.createdAt))}</td>
                <td><span class="admin-badge admin-badge-${CONTACT_STATUS_TONES[item.status] || "neutral"}">${escapeHtml(CONTACT_STATUS_LABELS[item.status] || item.status)}</span></td>
                <td class="admin-actions-cell">${actionsHtml(item)}</td>
            </tr>
            <tr class="js-contact-detail" data-contact-id="${id}" ${expanded ? "" : "hidden"}>
                <td></td>
                <td colspan="7">${detailHtml(item)}</td>
            </tr>
        `;

    }


    /* Nhân viên / quản lý: nút cho bước kế tiếp; ADMIN chỉ xem */

    function actionsHtml(item) {

        if (isAdmin) {
            return '<span class="admin-subtext">Chỉ xem</span>';
        }

        const actions = {
            NEW: [["take", "Nhận xử lý"], ["resolve", "Đã xử lý"]],
            IN_PROGRESS: [["resolve", "Đã xử lý"], ["note", "Ghi chú"]],
            RESOLVED: [["reopen", "Mở lại"], ["note", "Sửa ghi chú"]]
        }[item.status] || [];

        return actions.map(function (action) {
            return `<button type="button" class="admin-link-btn" data-action="${action[0]}" data-id="${escapeHtml(item.id)}">${escapeHtml(action[1])}</button>`;
        }).join(" ");

    }


    function detailHtml(item) {

        const facts = [
            ["Tài khoản", item.username ? escapeHtml(item.username) : "Khách chưa đăng nhập"],
            ["Email", `<a href="mailto:${escapeHtml(item.email)}">${escapeHtml(item.email)}</a>`],
            ["Số điện thoại", item.phone ? `<a href="tel:${escapeHtml(item.phone)}">${escapeHtml(item.phone)}</a>` : "—"],
            ["Người xử lý", escapeHtml(item.handledByName || "Chưa có")],
            ["Cập nhật lúc", escapeHtml(item.handledAt ? formatDateTimeVi(item.handledAt) : "—")],
            ["Ghi chú nội bộ", escapeHtml(item.staffNote || "Chưa có")]
        ];

        return `
            <div class="admin-order-detail">
                <p class="admin-review-comment">${escapeHtml(item.message)}</p>
                <dl class="admin-order-facts">
                    ${facts.map(function (fact) { return `<div><dt>${fact[0]}</dt><dd>${fact[1]}</dd></div>`; }).join("")}
                </dl>
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


    function toggleDetail(id, button) {

        const detailRow = tbody.querySelector('.js-contact-detail[data-contact-id="' + id + '"]');

        if (!detailRow) {
            return;
        }

        detailRow.hidden = !detailRow.hidden;

        button.setAttribute("aria-expanded", String(!detailRow.hidden));

        button.textContent = detailRow.hidden ? "▸" : "▾";

        if (detailRow.hidden) {
            expandedIds.delete(id);
        } else {
            expandedIds.add(id);
        }

    }


    /* ================= CẬP NHẬT ================= */

    /* Hộp ghi chú: "Đã xử lý" bắt buộc ghi lại đã làm gì; "Ghi chú" giữ nguyên trạng thái */

    function openNoteModal(item, opener, targetStatus) {

        closeModal();

        const resolving = targetStatus === "RESOLVED" && item.status !== "RESOLVED";

        const overlay = document.createElement("div");

        overlay.className = "modal-overlay";

        overlay.innerHTML = `
            <div class="modal-box admin-action-modal" role="dialog" aria-modal="true" aria-labelledby="contactModalTitle">
                <h3 id="contactModalTitle">${resolving ? "Đánh dấu đã xử lý" : "Ghi chú"} tin #${escapeHtml(item.id)}</h3>
                <p>${resolving ? "Ghi lại đã trả lời / xử lý cho khách thế nào (bắt buộc, khách không thấy)." : "Ghi chú nội bộ, khách không thấy."}</p>
                <label class="modal-field">
                    <span>Ghi chú</span>
                    <textarea name="staffNote" rows="4" maxlength="${CONTACT_NOTE_MAX_LENGTH}">${escapeHtml(item.staffNote || "")}</textarea>
                </label>
                <p class="form-error" data-role="error" role="alert" hidden></p>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline-dark" data-role="cancel">Quay lại</button>
                    <button type="button" class="btn btn-dark" data-role="confirm">${resolving ? "ĐÃ XỬ LÝ" : "LƯU"}</button>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        modalOverlayElement = overlay;

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

            if (resolving && !note && !item.staffNote) {

                errorBox.textContent = "Vui lòng ghi lại đã xử lý thế nào.";

                errorBox.hidden = false;

                textarea.focus();

                return;

            }

            confirmButton.disabled = true;

            const ok = await update(item, { status: targetStatus, staffNote: note || null },
                resolving ? "Đã đánh dấu tin #" + item.id + " là đã xử lý." : "Đã lưu ghi chú tin #" + item.id + ".",
                function (message) {
                    errorBox.textContent = message;
                    errorBox.hidden = false;
                });

            confirmButton.disabled = false;

            if (ok) {
                close();
            }

        });

        textarea.focus();

    }


    /* PATCH rồi tải lại trang hiện tại; 409 (người khác vừa đổi) → báo và tải lại */

    async function update(item, body, successMessage, showError) {

        try {

            await apiRequest("/admin/contact-requests/" + item.id, { method: "PATCH", body: body, auth: true });

            showToast(successMessage, "success");

            load();

            return true;

        } catch (error) {

            const message = error.details && error.details.staffNote ? error.details.staffNote : getErrorMessage(error);

            if (error.status === 409) {
                load();
            }

            handleError(error, showError || function (text) { showToast(text, "error"); }, message);

            return false;

        }

    }


    function handleError(error, show, message) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(message || getErrorMessage(error));

    }

});
