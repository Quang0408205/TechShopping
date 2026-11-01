/* ================= THÔNG BÁO NỘI BỘ (admin/notifications.html) ================= */

/*
 * API: GET /admin/notifications?unreadOnly&page&size, POST /admin/notifications/{id}/read,
 *      POST /admin/notifications/read-all
 */

const NOTIF_PAGE_SIZE = 20;


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }

    const unreadOnly = document.getElementById("notifUnreadOnly");
    const tbody = document.getElementById("notifTableBody");
    const rowCount = document.getElementById("notifRowCount");
    const pagination = document.getElementById("notifPagination");

    let requestCounter = 0;
    let currentPage = 0;


    unreadOnly.addEventListener("change", function () { load(0); });

    document.getElementById("notifReadAllBtn").addEventListener("click", async function () {

        try {

            await apiRequest("/admin/notifications/read-all", { method: "POST", auth: true });

            showToast("Đã đánh dấu đã đọc tất cả", "success");

            load(0);

        } catch (error) {

            handleError(error);

        }

    });

    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (button && !button.disabled) {
            load(Number(button.dataset.page));
        }

    });

    tbody.addEventListener("click", async function (event) {

        const button = event.target.closest("button[data-open]");

        if (!button) {
            return;
        }

        try {
            await apiRequest("/admin/notifications/" + button.dataset.open + "/read", { method: "POST", auth: true });
        } catch (error) {
            /* vẫn mở trang liên quan */
        }

        if (button.dataset.link) {
            window.location.href = siteUrl("admin/" + button.dataset.link);
        } else {
            load(currentPage);
        }

    });

    load(0);


    async function load(page) {

        currentPage = page;

        const requestId = ++requestCounter;

        tbody.innerHTML = adminSkeletonRow(4);

        try {

            const result = await apiRequest("/admin/notifications?size=" + NOTIF_PAGE_SIZE + "&page=" + page
                + (unreadOnly.checked ? "&unreadOnly=true" : ""), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            rowCount.textContent = result.totalElements + " thông báo";

            tbody.innerHTML = result.content.length === 0
                ? adminEmptyRow(4, unreadOnly.checked ? "Không còn thông báo chưa đọc." : "Chưa có thông báo nào.")
                : result.content.map(rowHtml).join("");

            renderPagination(result);

        } catch (error) {

            if (requestId === requestCounter) {
                tbody.innerHTML = adminEmptyRow(4, getErrorMessage(error));
            }

        }

    }


    function rowHtml(item) {

        return `
            <tr>
                <td>${escapeHtml(formatDateTimeVi(item.createdAt))}</td>
                <td>
                    <strong>${item.read ? "" : '<span class="admin-badge admin-badge-danger">Mới</span> '}${escapeHtml(item.title)}</strong>
                    ${item.message ? `<div class="admin-subtext">${escapeHtml(item.message)}</div>` : ""}
                </td>
                <td>${item.storeName ? escapeHtml(item.storeName) : "Chung"}</td>
                <td>
                    <button type="button" class="admin-link-btn" data-open="${escapeHtml(String(item.id))}"
                            data-link="${escapeHtml(item.link || "")}">${item.link ? "Mở" : (item.read ? "" : "Đã đọc")}</button>
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
