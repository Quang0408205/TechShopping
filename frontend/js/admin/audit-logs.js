/* ================= NHẬT KÝ THAO TÁC (admin/audit-logs.html) ================= */

/*
 * API (chỉ ADMIN, chỉ đọc):
 *   GET /admin/audit-logs?keyword&entityType&method&fromDate&toDate&page&size
 *   GET /admin/audit-logs/entity-types      các loại đối tượng đã có trong nhật ký (để lọc)
 */

const AUDIT_PAGE_SIZE = 20;

const AUDIT_ROLES = {
    ADMIN: { label: "ADMIN", badge: "admin-badge-danger" },
    BRANCH_MANAGER: { label: "Quản lý chi nhánh", badge: "admin-badge-info" },
    STAFF: { label: "Nhân viên", badge: "admin-badge-neutral" }
};


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }

    const form = document.getElementById("auditFilterForm");
    const keywordInput = document.getElementById("auditKeyword");
    const entityFilter = document.getElementById("auditEntityFilter");
    const methodFilter = document.getElementById("auditMethodFilter");
    const fromInput = document.getElementById("auditFrom");
    const toInput = document.getElementById("auditTo");
    const tbody = document.getElementById("auditTableBody");
    const rowCount = document.getElementById("auditRowCount");
    const pagination = document.getElementById("auditPagination");

    let rows = [];
    let requestCounter = 0;
    const expanded = new Set();


    try {

        const types = await apiRequest("/admin/audit-logs/entity-types", { auth: true });

        entityFilter.innerHTML = '<option value="">Mọi đối tượng</option>' + (types || []).map(function (type) {
            return `<option value="${escapeHtml(type.code)}">${escapeHtml(type.label)}</option>`;
        }).join("");

    } catch (error) {

        handleError(error, function (message) { showToast(message, "error"); });

    }

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        load(0);
    });

    [entityFilter, methodFilter, fromInput, toInput].forEach(function (control) {
        control.addEventListener("change", function () { load(0); });
    });

    document.getElementById("auditResetBtn").addEventListener("click", function () {
        form.reset();
        load(0);
    });

    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (button && !button.disabled) {
            load(Number(button.dataset.page));
        }

    });

    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action='toggle']");

        if (!button) {
            return;
        }

        const id = button.dataset.id;

        const detail = tbody.querySelector('tr[data-detail-for="' + id + '"]');

        if (detail) {

            detail.hidden = !detail.hidden;

            button.textContent = detail.hidden ? "▸" : "▾";

            button.setAttribute("aria-expanded", String(!detail.hidden));

            if (detail.hidden) {
                expanded.delete(id);
            } else {
                expanded.add(id);
            }

        }

    });


    load(0);


    async function load(page) {

        const requestId = ++requestCounter;

        tbody.innerHTML = adminSkeletonRow(6);

        const params = new URLSearchParams({ page: String(page), size: String(AUDIT_PAGE_SIZE) });

        if (keywordInput.value.trim()) {
            params.set("keyword", keywordInput.value.trim());
        }

        if (entityFilter.value) {
            params.set("entityType", entityFilter.value);
        }

        if (methodFilter.value) {
            params.set("method", methodFilter.value);
        }

        if (fromInput.value) {
            params.set("fromDate", fromInput.value);
        }

        if (toInput.value) {
            params.set("toDate", toInput.value);
        }

        try {

            const result = await apiRequest("/admin/audit-logs?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            rows = result.content || [];

            expanded.clear();

            rowCount.textContent = result.totalElements + " dòng";

            tbody.innerHTML = rows.length === 0
                ? adminEmptyRow(6, "Không có thao tác nào khớp bộ lọc.")
                : rows.map(rowHtml).join("");

            renderPagination(result);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(6, message);
                });
            }

        }

    }


    function rowHtml(log) {

        const id = escapeHtml(String(log.id));

        const role = AUDIT_ROLES[log.role] || { label: log.role, badge: "admin-badge-neutral" };

        const target = escapeHtml(log.entityLabel) + (log.entityId ? ` <span class="admin-subtext">#${escapeHtml(log.entityId)}</span>` : "");

        return `
            <tr>
                <td>${escapeHtml(formatDateTimeVi(log.createdAt))}</td>
                <td>
                    <strong>${escapeHtml(log.fullName || log.username)}</strong>
                    <div class="admin-subtext">${escapeHtml(log.username)} <span class="admin-badge ${role.badge}">${escapeHtml(role.label)}</span></div>
                </td>
                <td>${escapeHtml(log.actionLabel)}</td>
                <td>${target}</td>
                <td>${log.ip ? escapeHtml(log.ip) : "—"}</td>
                <td><button type="button" class="admin-link-btn" data-action="toggle" data-id="${id}" aria-expanded="false" aria-label="Chi tiết">▸</button></td>
            </tr>
            <tr class="admin-detail-row" data-detail-for="${id}" hidden>
                <td colspan="6">
                    <div><strong>${escapeHtml(log.method)}</strong> <code>${escapeHtml(log.path)}</code> → ${escapeHtml(String(log.status))}</div>
                    ${log.requestData
                        ? `<div class="admin-subtext">Dữ liệu gửi lên (đã che mật khẩu, mã thẻ, CCCD, token):</div><pre class="admin-code">${escapeHtml(log.requestData)}</pre>`
                        : '<div class="admin-subtext">Không có dữ liệu gửi kèm.</div>'}
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


    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
