/* ================= NGƯỜI DÙNG & PHÂN QUYỀN (admin/users.html) — B2 ================= */

/*
 * API thật (chỉ ADMIN, backend kiểm tra lại quyền ở mọi request):
 *   GET    /admin/users?keyword=&role=&isActive=&page=&size=   tìm kiếm, phân trang
 *   PATCH  /admin/users/{id}/status  { active }                khoá / mở khoá
 *   PUT    /admin/users/{id}/roles   { roles: ["STAFF" | "BRANCH_MANAGER"] }   đổi vai trò nội bộ
 *   DELETE /admin/users/{id}                                   xoá mềm (204)
 * Hai tab: Khách hàng (role=CUSTOMER: chỉ khoá / mở khoá / xoá) và Nội bộ (role=INTERNAL hoặc
 * STAFF / BRANCH_MANAGER / ADMIN: thêm "Đổi vai trò" cho nhân viên và quản lý; quản trị viên và chính mình
 * không đổi được). Lỗi 409 (chi nhánh đã có quản lý, tài khoản khách, quản trị viên cuối cùng…) hiển thị bằng
 * thông báo của máy chủ.
 */

const USER_PAGE_SIZE = 20;

const BACKEND_ROLE_LABELS = {
    CUSTOMER: "Khách hàng",
    STAFF: "Nhân viên chi nhánh",
    BRANCH_MANAGER: "Quản lý chi nhánh",
    ADMIN: "Quản trị viên"
};


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const form = document.getElementById("userFilterForm");

    const tbody = document.getElementById("userTableBody");

    const pagination = document.getElementById("userPagination");

    const rolePanel = document.getElementById("rolePanel");

    const roleForm = document.getElementById("roleForm");

    const roleError = document.getElementById("roleFormError");


    const roleFilterSelect = document.getElementById("userRoleFilter");

    let currentTab = "CUSTOMER";

    let currentPage = 0;

    let currentUsers = [];

    let editingUser = null;

    let requestCounter = 0;


    form.addEventListener("submit", function (event) {

        event.preventDefault();

        currentPage = 0;

        loadUsers();

    });

    roleFilterSelect.addEventListener("change", function () {
        currentPage = 0;
        loadUsers();
    });

    document.querySelectorAll("[data-user-tab]").forEach(function (tab) {

        tab.addEventListener("click", function () {
            selectTab(tab.dataset.userTab);
        });

    });

    document.getElementById("userStatusFilter").addEventListener("change", function () {
        currentPage = 0;
        loadUsers();
    });


    roleForm.addEventListener("submit", saveRoles);

    document.getElementById("roleCancelBtn").addEventListener("click", closeRolePanel);


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const user = currentUsers.find(function (u) {
            return String(u.id) === button.dataset.id;
        });

        if (!user) {
            return;
        }


        if (button.dataset.action === "status") {
            toggleStatus(user);
        } else if (button.dataset.action === "roles") {
            openRolePanel(user);
        } else if (button.dataset.action === "delete") {
            confirmDelete(user);
        }

    });


    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        loadUsers();

    });


    loadUsers();


    /* ================= TAB ================= */

    function selectTab(tab) {

        if (tab === currentTab) {
            return;
        }

        currentTab = tab;

        document.querySelectorAll("[data-user-tab]").forEach(function (button) {

            const active = button.dataset.userTab === tab;

            button.classList.toggle("is-active", active);

            button.setAttribute("aria-selected", String(active));

        });

        roleFilterSelect.value = "";

        roleFilterSelect.hidden = tab === "CUSTOMER";

        closeRolePanel();

        currentPage = 0;

        loadUsers();

    }


    /* ================= TẢI DANH SÁCH ================= */

    async function loadUsers() {

        const requestId = ++requestCounter;

        const params = new URLSearchParams({
            page: String(currentPage),
            size: String(USER_PAGE_SIZE),
            sort: "id,asc"
        });

        const keyword = document.getElementById("userKeyword").value.trim();

        const role = currentTab === "CUSTOMER" ? "CUSTOMER" : (roleFilterSelect.value || "INTERNAL");

        const status = document.getElementById("userStatusFilter").value;

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (role) {
            params.set("role", role);
        }

        if (status) {
            params.set("isActive", status);
        }


        tbody.innerHTML = adminEmptyRow(8, "Đang tải…");


        try {

            const page = await apiRequest("/admin/users?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            currentUsers = page.content || [];

            renderTable(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(8, message);
                });
            }

        }

    }


    function renderTable(page) {

        document.getElementById("userRowCount").textContent = page.totalElements + " tài khoản";

        tbody.innerHTML = currentUsers.map(renderRow).join("") ||
            adminEmptyRow(8, "Không có tài khoản nào phù hợp bộ lọc");

        renderPagination(page);

    }


    function renderRow(user) {

        const isSelf = user.id === staff.id;

        const active = user.isActive !== false;

        /* Chỉ nhân viên và quản lý đổi vai trò được; khách hàng, quản trị viên và chính mình thì không */
        const canChangeRole = !isSelf && (user.roles || []).some(function (role) {
            return role === "STAFF" || role === "BRANCH_MANAGER";
        }) && (user.roles || []).indexOf("ADMIN") === -1;

        const roles = (user.roles || []).map(function (role) {
            return `<span class="admin-badge ${role === "ADMIN" ? "admin-badge-warning" : (role === "STAFF" || role === "BRANCH_MANAGER") ? "admin-badge-success" : "admin-badge-neutral"}">${escapeHtml(BACKEND_ROLE_LABELS[role] || role)}</span>`;
        }).join(" ");


        return `
            <tr data-user-id="${escapeHtml(String(user.id))}">
                <td>
                    ${escapeHtml(user.fullname)}${isSelf ? ' <span class="admin-subtext">(bạn)</span>' : ""}
                    <div class="admin-subtext">@${escapeHtml(user.username)} · #${escapeHtml(String(user.id))}</div>
                </td>
                <td>${escapeHtml(user.email)}</td>
                <td>${escapeHtml(user.phone || "—")}</td>
                <td>${roles}</td>
                <td>
                    <span class="admin-badge ${active ? "admin-badge-success" : "admin-badge-danger"}">
                        ${active ? "Hoạt động" : "Đã khoá"}
                    </span>
                </td>
                <td>${user.lastLogin ? escapeHtml(formatDateTimeVi(user.lastLogin)) : "—"}</td>
                <td>${user.createdAt ? escapeHtml(formatDateVi(user.createdAt)) : "—"}</td>
                <td class="admin-actions-cell">
                    ${isSelf ? "" : `<button type="button" class="admin-link-btn" data-action="status" data-id="${escapeHtml(String(user.id))}">${active ? "Khoá" : "Mở khoá"}</button>`}
                    ${canChangeRole ? `<button type="button" class="admin-link-btn" data-action="roles" data-id="${escapeHtml(String(user.id))}">Đổi vai trò</button>` : ""}
                    ${isSelf ? "" : `<button type="button" class="admin-link-btn admin-link-danger" data-action="delete" data-id="${escapeHtml(String(user.id))}">Xoá</button>`}
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


    /* ================= THAO TÁC ================= */

    function toggleStatus(user) {

        const activate = user.isActive === false;

        openConfirmModal({
            title: activate ? "Mở khoá tài khoản?" : "Khoá tài khoản?",
            message: activate
                ? user.email + " sẽ đăng nhập lại được."
                : user.email + " sẽ bị đăng xuất khỏi mọi thiết bị và không đăng nhập được nữa.",
            confirmLabel: activate ? "MỞ KHOÁ" : "KHOÁ",
            onConfirm: async function () {

                try {

                    await apiRequest("/admin/users/" + user.id + "/status", {
                        method: "PATCH",
                        body: { active: activate },
                        auth: true
                    });

                    showToast(activate ? "Đã mở khoá " + user.email + "." : "Đã khoá " + user.email + ".", "success");

                    loadUsers();

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

            }
        });

    }


    function openRolePanel(user) {

        editingUser = user;

        document.getElementById("rolePanelTitle").textContent = "Đổi vai trò: " + user.email;

        roleForm.querySelectorAll('input[name="role"]').forEach(function (radio) {
            radio.checked = (user.roles || []).indexOf(radio.value) !== -1;
        });

        roleError.hidden = true;

        rolePanel.hidden = false;

        rolePanel.scrollIntoView({ behavior: "smooth", block: "start" });

    }


    function closeRolePanel() {

        editingUser = null;

        rolePanel.hidden = true;

    }


    async function saveRoles(event) {

        event.preventDefault();

        if (!editingUser) {
            return;
        }


        const roles = Array.from(roleForm.querySelectorAll('input[name="role"]:checked'))
            .map(function (radio) { return radio.value; });

        if (roles.length === 0) {

            roleError.textContent = "Chọn một vai trò.";

            roleError.hidden = false;

            return;

        }


        const saveButton = document.getElementById("roleSaveBtn");

        saveButton.disabled = true;


        try {

            await apiRequest("/admin/users/" + editingUser.id + "/roles", {
                method: "PUT",
                body: { roles: roles },
                auth: true
            });

            showToast("Đã cập nhật vai trò của " + editingUser.email + ".", "success");

            closeRolePanel();

            loadUsers();

        } catch (error) {

            handleError(error, function (message) {
                roleError.textContent = message;
                roleError.hidden = false;
            });

        } finally {

            saveButton.disabled = false;

        }

    }


    function confirmDelete(user) {

        openConfirmModal({
            title: "Xoá tài khoản?",
            message: user.email + " sẽ bị xoá (xoá mềm): không đăng nhập được và không hiện trong danh sách. Không thể khôi phục từ giao diện.",
            confirmLabel: "XOÁ",
            onConfirm: async function () {

                try {

                    await apiRequest("/admin/users/" + user.id, { method: "DELETE", auth: true });

                    showToast("Đã xoá " + user.email + ".", "success");

                    loadUsers();

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

            }
        });

    }


    /*
     * Hết phiên (401, kể cả sau khi đã thử làm mới token) → kiểm tra lại phiên
     * và về trang đăng nhập; lỗi khác (403, 409…) → hiển thị bằng show(message).
     */

    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
