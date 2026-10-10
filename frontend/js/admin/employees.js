/* ================= NHÂN VIÊN (admin/employees.html) ================= */

/*
 * Dữ liệu thật. Hai vai trò dùng cùng một trang, khác API:
 *
 * ADMIN (mọi chi nhánh):
 *   GET    /admin/employees?keyword&storeId&active&page&size   danh sách (mới nhất trước)
 *   POST   /admin/employees/hire                               TUYỂN: tạo tài khoản mới + hồ sơ + chi nhánh (201),
 *                                                              vai trò STAFF hoặc BRANCH_MANAGER; trả mật khẩu tạm một lần
 *   PUT    /admin/employees/{id}                               sửa toàn bộ; active=false = đã nghỉ (kết thúc phân công)
 *   POST   /admin/employees/{id}/assignment                    gán / chuyển chi nhánh (giữ lịch sử)
 *   DELETE /admin/employees/{id}/assignment                    rút khỏi chi nhánh
 *
 * BRANCH_MANAGER (chỉ chi nhánh của mình, không có ô / cột chi nhánh):
 *   GET    /branch/employees?keyword&active&page&size          nhân viên đang làm và đã nghỉ của chi nhánh
 *   POST   /branch/employees/hire                              tuyển STAFF vào chi nhánh của mình
 *   PUT    /branch/employees/{id}                              sửa hồ sơ + chức danh
 *   POST   /branch/employees/{id}/deactivate                   cho nghỉ (khoá tài khoản)
 *
 * Tuyển nhân sự luôn tạo tài khoản MỚI, không chọn tài khoản khách có sẵn. Chức danh tại chi nhánh
 * (STORE_POSITIONS, staff-auth.js) chỉ để hiển thị; "Quản lý chi nhánh" gắn với vai trò BRANCH_MANAGER.
 */

const EMPLOYEE_PAGE_SIZE = 20;


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const isAdmin = staff.role === "ADMIN";

    const API = isAdmin ? "/admin/employees" : "/branch/employees";


    const filterForm = document.getElementById("employeeFilterForm");

    const keywordInput = document.getElementById("employeeKeyword");

    const storeFilter = document.getElementById("storeFilter");

    const statusFilter = document.getElementById("employeeStatusFilter");

    const formPanel = document.getElementById("employeeFormPanel");

    const form = document.getElementById("employeeForm");

    const formError = document.getElementById("employeeFormError");

    const saveButton = document.getElementById("employeeSaveBtn");

    const roleSelect = document.getElementById("empRole");

    const createStoreSelect = document.getElementById("empStoreId");

    const createPositionSelect = document.getElementById("empStorePosition");

    const assignPanel = document.getElementById("assignPanel");

    const assignForm = document.getElementById("assignForm");

    const assignError = document.getElementById("assignError");

    const assignStoreSelect = document.getElementById("assignStoreId");

    const tbody = document.getElementById("employeeTableBody");

    const pagination = document.getElementById("employeePagination");


    let stores = [];

    let currentPage = 0;

    let currentEmployees = [];

    let editingEmployee = null;

    let assigningEmployee = null;

    let requestCounter = 0;


    /* Quản lý chi nhánh: không có lọc / cột / ô chi nhánh, không có vai trò để chọn */
    if (!isAdmin) {

        storeFilter.hidden = true;

        document.querySelectorAll("[data-admin-only]").forEach(function (element) {
            element.hidden = true;
        });

        document.getElementById("employeeStoreHeader").textContent = "Chức danh tại chi nhánh";

    }


    [createPositionSelect, document.getElementById("assignPosition")].forEach(function (select) {
        select.innerHTML = STORE_POSITIONS.map(function (position) {
            return `<option value="${escapeHtml(position)}">${escapeHtml(position)}</option>`;
        }).join("");
    });


    if (isAdmin) {

        try {

            stores = await loadAllStores();

        } catch (error) {

            handleError(error, function (message) {
                showToast("Không tải được danh sách chi nhánh: " + message, "error");
            });

        }

        fillStoreOptions(storeFilter, [{ value: "", label: "Tất cả chi nhánh" }], stores);

    }


    filterForm.addEventListener("submit", function (event) {
        event.preventDefault();
        reloadFromFirstPage();
    });

    [storeFilter, statusFilter].forEach(function (select) {
        select.addEventListener("change", reloadFromFirstPage);
    });

    document.getElementById("newEmployeeBtn").addEventListener("click", function () {
        openForm(null);
    });

    document.getElementById("employeeCancelBtn").addEventListener("click", closeForm);

    form.addEventListener("submit", saveEmployee);

    roleSelect.addEventListener("change", syncCreatePosition);

    document.getElementById("assignCancelBtn").addEventListener("click", closeAssign);

    assignForm.addEventListener("submit", saveAssignment);


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const employee = button && findEmployee(button.dataset.id);

        if (!employee) {
            return;
        }

        if (button.dataset.action === "edit") {
            openForm(employee);
        } else if (button.dataset.action === "assign") {
            openAssign(employee);
        } else if (button.dataset.action === "unassign") {
            confirmUnassign(employee);
        } else if (button.dataset.action === "deactivate") {
            confirmDeactivate(employee);
        }

    });


    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        loadEmployees();

    });


    loadEmployees();

    /* ?hire=1 (lối tắt "Tuyển nhân sự" ở trang Tổng quan) mở ngay form tuyển */
    if (new URLSearchParams(window.location.search).get("hire") === "1") {
        openForm(null);
    }


    /* ================= DANH SÁCH ================= */

    function reloadFromFirstPage() {

        currentPage = 0;

        loadEmployees();

    }


    async function loadEmployees() {

        const requestId = ++requestCounter;

        const params = new URLSearchParams({ page: String(currentPage), size: String(EMPLOYEE_PAGE_SIZE) });

        const keyword = keywordInput.value.trim();

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (isAdmin && storeFilter.value) {
            params.set("storeId", storeFilter.value);
        }

        if (statusFilter.value) {
            params.set("active", statusFilter.value);
        }


        tbody.innerHTML = adminEmptyRow(7, "Đang tải…");


        try {

            const page = await apiRequest(API + "?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            currentEmployees = page.content || [];

            document.getElementById("employeeRowCount").textContent = page.totalElements + " nhân viên";

            tbody.innerHTML = currentEmployees.map(renderRow).join("") ||
                adminEmptyRow(7, "Không có nhân viên nào phù hợp bộ lọc");

            renderPagination(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(7, message);
                    pagination.innerHTML = "";
                });
            }

        }

    }


    function findEmployee(id) {

        return currentEmployees.find(function (employee) {
            return String(employee.id) === String(id);
        });

    }


    function renderRow(employee) {

        const id = escapeHtml(String(employee.id));

        const assignment = employee.assignment;

        const manager = assignment && assignment.positionAtStore === BRANCH_MANAGER_POSITION;

        const isSelf = employee.userId === staff.id;

        const storeCell = isAdmin ? adminStoreCell(assignment, manager) : branchPositionCell(employee, assignment, manager);

        const actions = [];

        if (isAdmin) {

            actions.push(`<button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>`);

            if (employee.active) {
                actions.push(`<button type="button" class="admin-link-btn" data-action="assign" data-id="${id}">${assignment ? "Chuyển chi nhánh" : "Gán chi nhánh"}</button>`);
            }

            if (assignment) {
                actions.push(`<button type="button" class="admin-link-btn admin-link-danger" data-action="unassign" data-id="${id}">Rút khỏi chi nhánh</button>`);
            }

        } else if (employee.active && !isSelf && !manager) {

            actions.push(`<button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>`);

            actions.push(`<button type="button" class="admin-link-btn admin-link-danger" data-action="deactivate" data-id="${id}">Cho nghỉ</button>`);

        }


        return `
            <tr data-employee-id="${id}">
                <td>
                    <strong>${escapeHtml(employee.fullname)}</strong>${isSelf ? ' <span class="admin-subtext">(bạn)</span>' : ""}
                    <span class="admin-subtext">
                        @${escapeHtml(employee.username)}${employee.employeeCode ? " · " + escapeHtml(employee.employeeCode) : ""}
                    </span>
                </td>
                <td>
                    ${escapeHtml(employee.email)}
                    <span class="admin-subtext">${escapeHtml(employee.phone || "—")}</span>
                </td>
                <td>${storeCell}</td>
                <td>
                    ${escapeHtml(employee.position || "—")}
                    ${employee.department ? `<span class="admin-subtext">${escapeHtml(employee.department)}</span>` : ""}
                </td>
                <td>${escapeHtml(employee.hiringDate ? formatDateVi(employee.hiringDate) : "—")}</td>
                <td>
                    <span class="admin-badge ${employee.active ? "admin-badge-success" : "admin-badge-neutral"}">
                        ${employee.active ? "Đang làm việc" : "Đã nghỉ"}
                    </span>
                </td>
                <td class="admin-actions-cell">${actions.join("") || "—"}</td>
            </tr>
        `;

    }


    function adminStoreCell(assignment, manager) {

        return assignment
            ? `
                ${escapeHtml(assignment.storeName)}
                <span class="admin-subtext">
                    ${escapeHtml(assignment.positionAtStore || "—")} · từ ${escapeHtml(formatDateVi(assignment.startDate))}
                </span>
                ${manager ? '<span class="admin-badge admin-badge-info">Quản lý chi nhánh</span>' : ""}
            `
            : '<span class="admin-subtext">Chưa gán chi nhánh</span>';

    }


    /* Quản lý chi nhánh chỉ thấy chi nhánh mình, nên cột này chỉ ghi chức danh */
    function branchPositionCell(employee, assignment, manager) {

        if (!assignment) {
            return '<span class="admin-subtext">' + (employee.active ? "Chưa gán chi nhánh" : "Đã rời chi nhánh") + "</span>";
        }

        return `
            ${escapeHtml(assignment.positionAtStore || "—")}
            <span class="admin-subtext">từ ${escapeHtml(formatDateVi(assignment.startDate))}</span>
            ${manager ? '<span class="admin-badge admin-badge-info">Quản lý chi nhánh</span>' : ""}
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


    /* ================= TUYỂN / SỬA HỒ SƠ ================= */

    function field(id) {

        return document.getElementById(id);

    }


    function openStores() {

        return stores.filter(function (store) {
            return store.active !== false;
        });

    }


    function openForm(employee) {

        closeAssign();

        editingEmployee = employee;

        const creating = !employee;

        field("employeeFormTitle").textContent = creating ? "Tuyển nhân sự" : "Sửa hồ sơ " + employee.fullname;

        /* Ô của tài khoản mới chỉ có khi tuyển; ô chỉ ADMIN thì quản lý chi nhánh không bao giờ thấy */
        document.querySelectorAll("[data-hire-only]").forEach(function (element) {
            element.hidden = !creating || (!isAdmin && element.hasAttribute("data-admin-only"));
        });

        field("empAccountField").hidden = creating;

        field("empStoreField").hidden = !creating || !isAdmin;

        field("empActiveField").hidden = creating || !isAdmin;

        /* Chức danh: ADMIN chỉnh bằng form "Gán chi nhánh"; quản lý chi nhánh chỉnh ngay ở đây */
        field("empStorePositionField").hidden = creating ? false : isAdmin;


        ["empFullname", "empEmail", "empUsername", "empPhone", "empPassword"].forEach(function (id) {
            field(id).value = "";
        });

        field("empCode").value = employee ? employee.employeeCode || "" : "";

        field("empDepartment").value = employee ? employee.department || "" : "";

        field("empPosition").value = employee ? employee.position || "" : "";

        field("empSalary").value = employee && employee.salary != null ? employee.salary : "";

        field("empHiringDate").value = employee ? employee.hiringDate || "" : "";

        field("empActive").checked = employee ? employee.active : true;

        roleSelect.value = "STAFF";

        formError.hidden = true;

        formPanel.hidden = false;


        if (!creating) {

            field("empAccountText").value = employee.fullname + " (@" + employee.username + " · " + employee.email + ")";

            const label = employee.assignment ? employee.assignment.positionAtStore : null;

            createPositionSelect.value = STORE_POSITIONS.indexOf(label) !== -1 ? label : STORE_POSITIONS[0];

            field("empCode").focus();

            return;

        }


        if (isAdmin) {

            createStoreSelect.innerHTML = '<option value="">Chọn chi nhánh…</option>' +
                openStores().map(function (store) {
                    return `<option value="${escapeHtml(store.id)}">${escapeHtml(store.name)}</option>`;
                }).join("");

        }

        syncCreatePosition();

        field("empFullname").focus();

    }


    /* Quản lý chi nhánh có chức danh cố định, nên không chọn chức danh khi tuyển quản lý */

    function syncCreatePosition() {

        if (editingEmployee) {
            return;
        }

        field("empStorePositionField").hidden = isAdmin && roleSelect.value === "BRANCH_MANAGER";

    }


    function closeForm() {

        editingEmployee = null;

        form.reset();

        formError.hidden = true;

        formPanel.hidden = true;

        saveButton.disabled = false;

    }


    function textOrNull(id) {

        const value = field(id).value.trim();

        return value === "" ? null : value;

    }


    /* Trả về { body } hoặc { error } */

    function readForm() {

        const salaryInput = field("empSalary");

        if (salaryInput.validity.badInput || (salaryInput.value !== "" && Number(salaryInput.value) < 0)) {
            return { error: "Lương phải là số không âm." };
        }

        const body = {
            employeeCode: textOrNull("empCode"),
            department: textOrNull("empDepartment"),
            position: textOrNull("empPosition"),
            salary: salaryInput.value === "" ? null : Number(salaryInput.value),
            hiringDate: textOrNull("empHiringDate")
        };


        if (editingEmployee) {

            if (isAdmin) {
                body.active = field("empActive").checked;
            } else {
                body.positionAtStore = createPositionSelect.value;
            }

            return { body: body };

        }


        if (!field("empFullname").value.trim()) {
            return { error: "Vui lòng nhập họ tên." };
        }

        if (!field("empEmail").value.trim()) {
            return { error: "Vui lòng nhập email." };
        }

        if (!field("empUsername").value.trim()) {
            return { error: "Vui lòng nhập tên đăng nhập." };
        }

        const password = field("empPassword").value;

        if (password !== "" && password.length < 8) {
            return { error: "Mật khẩu tạm tối thiểu 8 ký tự (hoặc để trống để hệ thống tự sinh)." };
        }

        if (isAdmin && !createStoreSelect.value) {
            return { error: "Vui lòng chọn chi nhánh." };
        }


        body.fullname = field("empFullname").value.trim();

        body.email = field("empEmail").value.trim();

        body.username = field("empUsername").value.trim();

        body.phone = textOrNull("empPhone");

        body.temporaryPassword = password === "" ? null : password;

        if (isAdmin) {

            body.role = roleSelect.value;

            body.storeId = Number(createStoreSelect.value);

        }

        body.positionAtStore = field("empStorePositionField").hidden ? null : createPositionSelect.value;

        return { body: body };

    }


    async function saveEmployee(event) {

        event.preventDefault();


        const result = readForm();

        if (result.error) {

            formError.textContent = result.error;

            formError.hidden = false;

            return;

        }


        const wasEditing = editingEmployee;

        const endsAssignment = wasEditing && isAdmin && wasEditing.active && !result.body.active && wasEditing.assignment;

        saveButton.disabled = true;


        try {

            if (wasEditing) {

                const saved = await apiRequest(API + "/" + wasEditing.id, { method: "PUT", body: result.body, auth: true });

                showToast(
                    "Đã lưu hồ sơ " + saved.fullname +
                    (endsAssignment ? " (đã kết thúc phân công tại " + wasEditing.assignment.storeName + ")" : "") + ".",
                    "success"
                );

                closeForm();

                loadEmployees();

                return;

            }


            const hired = await apiRequest(API + "/hire", { method: "POST", body: result.body, auth: true });

            closeForm();

            loadEmployees();

            showHiredPassword(hired);

        } catch (error) {

            handleError(error, function (message) {
                formError.textContent = message + detailsText(error);
                formError.hidden = false;
            });

        } finally {

            saveButton.disabled = false;

        }

    }


    /* Mật khẩu tạm chỉ hiện đúng một lần, kèm nút sao chép */

    function showHiredPassword(hired) {

        const employee = hired.employee;

        openSecretModal({
            title: "Đã tuyển " + employee.fullname,
            message: "Tài khoản @" + employee.username + " (" + (hired.role === "BRANCH_MANAGER" ? "Quản lý chi nhánh" : "Nhân viên chi nhánh") +
                (employee.assignment ? ", " + employee.assignment.storeName : "") + ") đã được tạo. Gửi mật khẩu tạm cho người đó và nhắc đổi mật khẩu sau khi đăng nhập.",
            label: "Mật khẩu tạm",
            secret: hired.temporaryPassword
        });

    }


    /* ================= CHO NGHỈ (quản lý chi nhánh) ================= */

    function confirmDeactivate(employee) {

        openConfirmModal({
            title: "Cho " + employee.fullname + " nghỉ?",
            message: "Phân công kết thúc hôm nay, tài khoản bị khoá và đăng xuất khỏi mọi thiết bị. Hồ sơ vẫn nằm trong danh sách \"Đã nghỉ\".",
            confirmLabel: "CHO NGHỈ",
            cancelLabel: "Quay lại",
            onConfirm: async function () {

                try {

                    await apiRequest(API + "/" + employee.id + "/deactivate", { method: "POST", auth: true });

                    showToast("Đã cho " + employee.fullname + " nghỉ.", "success");

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

                loadEmployees();

            }
        });

    }


    /* ================= GÁN / CHUYỂN / RÚT CHI NHÁNH (ADMIN) ================= */

    function openAssign(employee) {

        closeForm();

        assigningEmployee = employee;

        const current = employee.assignment;

        const isManager = Boolean(current) && current.positionAtStore === BRANCH_MANAGER_POSITION;

        field("assignTitle").textContent = (current ? "Chuyển chi nhánh: " : "Gán chi nhánh: ") + employee.fullname;

        field("assignHint").textContent = current
            ? "Đang ở " + current.storeName + " (" + (current.positionAtStore || "—") + "). Chọn chi nhánh khác thì phân công cũ " +
              "kết thúc hôm nay và lịch sử được giữ lại; chọn đúng chi nhánh hiện tại thì chỉ đổi chức danh."
            : "Nhân viên chỉ thấy đơn hàng, trả góp và tồn kho của chi nhánh được gán.";

        if (isManager) {
            field("assignHint").textContent += " Quản lý luôn mang chức danh Quản lý chi nhánh, và mỗi chi nhánh chỉ có một quản lý.";
        }


        assignStoreSelect.innerHTML = openStores().map(function (store) {
            return `<option value="${escapeHtml(store.id)}">${escapeHtml(store.name)}</option>`;
        }).join("") || '<option value="">Chưa có chi nhánh nào đang mở</option>';

        if (current) {
            assignStoreSelect.value = String(current.storeId);
        }

        field("assignPosition").disabled = isManager;

        field("assignPosition").value = current && STORE_POSITIONS.indexOf(current.positionAtStore) !== -1
            ? current.positionAtStore
            : STORE_POSITIONS[0];

        field("assignStartDate").value = todayIso();

        assignError.hidden = true;

        assignPanel.hidden = false;

        assignStoreSelect.focus();

    }


    function closeAssign() {

        assigningEmployee = null;

        assignForm.reset();

        field("assignPosition").disabled = false;

        assignError.hidden = true;

        assignPanel.hidden = true;

    }


    async function saveAssignment(event) {

        event.preventDefault();

        const employee = assigningEmployee;

        if (!employee) {
            return;
        }

        if (!assignStoreSelect.value) {

            assignError.textContent = "Vui lòng chọn chi nhánh (thêm hoặc mở lại chi nhánh ở trang Chi nhánh).";

            assignError.hidden = false;

            return;

        }


        const saveAssignButton = document.getElementById("assignSaveBtn");

        saveAssignButton.disabled = true;


        try {

            const saved = await apiRequest("/admin/employees/" + employee.id + "/assignment", {
                method: "POST",
                auth: true,
                body: {
                    storeId: Number(assignStoreSelect.value),
                    positionAtStore: field("assignPosition").value,
                    startDate: textOrNull("assignStartDate")
                }
            });

            showToast("Đã gán " + saved.fullname + " vào " + saved.assignment.storeName + ".", "success");

            closeAssign();

            loadEmployees();

        } catch (error) {

            handleError(error, function (message) {
                assignError.textContent = message + detailsText(error);
                assignError.hidden = false;
            });

        } finally {

            saveAssignButton.disabled = false;

        }

    }


    function confirmUnassign(employee) {

        const wasManager = employee.assignment.positionAtStore === BRANCH_MANAGER_POSITION;

        openConfirmModal({
            title: "Rút " + employee.fullname + " khỏi " + employee.assignment.storeName + "?",
            message: "Phân công kết thúc hôm nay (lịch sử được giữ). Người đó sẽ không thấy đơn hàng, trả góp hay tồn kho nào cho tới khi được gán chi nhánh khác" +
                (wasManager ? "; một quản lý rút khỏi chi nhánh sẽ trở lại là nhân viên." : "."),
            confirmLabel: "RÚT KHỎI CHI NHÁNH",
            cancelLabel: "Quay lại",
            onConfirm: async function () {

                try {

                    await apiRequest("/admin/employees/" + employee.id + "/assignment", { method: "DELETE", auth: true });

                    showToast("Đã rút " + employee.fullname + " khỏi " + employee.assignment.storeName + ".", "success");

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

                loadEmployees();

            }
        });

    }


    /* ================= TIỆN ÍCH ================= */

    function todayIso() {

        const now = new Date();

        return now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0") + "-" +
            String(now.getDate()).padStart(2, "0");

    }


    /* " (Mã nhân viên: …; Chi nhánh: …)" từ error.details của VALIDATION_ERROR */

    function detailsText(error) {

        if (!error.details || typeof error.details !== "object") {
            return "";
        }

        const parts = Object.keys(error.details).map(function (key) {
            return error.details[key];
        });

        return parts.length > 0 ? " (" + parts.join("; ") + ")" : "";

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
