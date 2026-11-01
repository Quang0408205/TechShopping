/* ================= HỎI ĐÁP SẢN PHẨM (admin/questions.html) ================= */

/*
 * API: GET /admin/questions?status&keyword&page&size, PUT /admin/questions/{id}/answer { answer },
 *      PATCH /admin/questions/{id}/hidden { hidden }. Nhân viên, quản lý chi nhánh và ADMIN đều trả lời được.
 */

const QUESTION_PAGE_SIZE = 20;


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }

    const form = document.getElementById("questionFilterForm");
    const keyword = document.getElementById("questionKeyword");
    const status = document.getElementById("questionStatus");
    const tbody = document.getElementById("questionTableBody");
    const rowCount = document.getElementById("questionRowCount");
    const pagination = document.getElementById("questionPagination");

    let rows = [];
    let currentPage = 0;
    let requestCounter = 0;
    const open = new Set();


    form.addEventListener("submit", function (event) {
        event.preventDefault();
        load(0);
    });

    status.addEventListener("change", function () { load(0); });

    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (button && !button.disabled) {
            load(Number(button.dataset.page));
        }

    });

    tbody.addEventListener("click", async function (event) {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const id = button.dataset.id;

        if (button.dataset.action === "toggle") {

            if (open.has(id)) {
                open.delete(id);
            } else {
                open.add(id);
            }

            render();

        } else if (button.dataset.action === "save") {

            const box = tbody.querySelector('textarea[data-answer-for="' + id + '"]');

            const text = box.value.trim();

            if (!text) {
                showToast("Vui lòng nhập câu trả lời", "error");
                return;
            }

            await send("/admin/questions/" + id + "/answer", "PUT", { answer: text }, "Đã lưu câu trả lời");

        } else if (button.dataset.action === "hide") {

            const row = rows.find(function (item) { return String(item.id) === id; });

            await send("/admin/questions/" + id + "/hidden", "PATCH", { hidden: !row.hidden }, row.hidden ? "Đã hiện lại" : "Đã ẩn câu hỏi");

        }

    });

    load(0);


    async function send(path, method, body, message) {

        try {

            await apiRequest(path, { method: method, body: body, auth: true });

            showToast(message, "success");

            await load(currentPage);

        } catch (error) {

            handleError(error);

        }

    }


    async function load(page) {

        currentPage = page;

        const requestId = ++requestCounter;

        tbody.innerHTML = adminSkeletonRow(5);

        const params = new URLSearchParams({ page: String(page), size: String(QUESTION_PAGE_SIZE) });

        if (status.value) {
            params.set("status", status.value);
        }

        if (keyword.value.trim()) {
            params.set("keyword", keyword.value.trim());
        }

        try {

            const result = await apiRequest("/admin/questions?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            rows = result.content;

            rowCount.textContent = result.totalElements + " câu hỏi";

            render();

            renderPagination(result);

        } catch (error) {

            if (requestId === requestCounter) {
                tbody.innerHTML = adminEmptyRow(5, getErrorMessage(error));
            }

        }

    }


    function render() {

        tbody.innerHTML = rows.length === 0
            ? adminEmptyRow(5, status.value === "PENDING" ? "Không có câu hỏi nào đang chờ trả lời." : "Không có câu hỏi nào khớp bộ lọc.")
            : rows.map(rowHtml).join("");

    }


    function rowHtml(item) {

        const id = escapeHtml(String(item.id));

        const answered = Boolean(item.answer);

        const badge = item.hidden
            ? '<span class="admin-badge admin-badge-neutral">Đã ẩn</span>'
            : answered
                ? '<span class="admin-badge admin-badge-success">Đã trả lời</span>'
                : '<span class="admin-badge admin-badge-danger">Chờ trả lời</span>';

        const expanded = open.has(String(item.id)) || !answered;

        return `
            <tr>
                <td>${escapeHtml(formatDateTimeVi(item.createdAt))}</td>
                <td>
                    <a href="${escapeHtml(siteUrl("customer/product-detail.html?id=" + item.productId))}" target="_blank" rel="noopener">${escapeHtml(item.productName)}</a>
                </td>
                <td>
                    <div><strong>${escapeHtml(item.askerName)}</strong> <span class="admin-subtext">${escapeHtml(item.askerEmail)}</span></div>
                    <div>${escapeHtml(item.question)}</div>
                    ${answered ? `<div class="admin-subtext">Trả lời${item.answeredByName ? " bởi " + escapeHtml(item.answeredByName) : ""}: ${escapeHtml(item.answer)}</div>` : ""}
                    ${expanded ? `
                        <textarea rows="3" maxlength="1000" data-answer-for="${id}" placeholder="Nhập câu trả lời…" aria-label="Câu trả lời">${escapeHtml(item.answer || "")}</textarea>
                        <button type="button" class="btn btn-dark" data-action="save" data-id="${id}">${answered ? "CẬP NHẬT TRẢ LỜI" : "TRẢ LỜI"}</button>
                    ` : ""}
                </td>
                <td>${badge}</td>
                <td>
                    ${answered ? `<button type="button" class="admin-link-btn" data-action="toggle" data-id="${id}">${expanded ? "Thu gọn" : "Sửa"}</button>` : ""}
                    <button type="button" class="admin-link-btn admin-link-danger" data-action="hide" data-id="${id}">${item.hidden ? "Hiện lại" : "Ẩn"}</button>
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


    function handleError(error) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        showToast(getErrorMessage(error), "error");

    }

});
