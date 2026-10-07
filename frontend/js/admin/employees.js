/* ================= NHÂN VIÊN (admin/employees.html) ================= */

/*
 * Chỉ ADMIN, dữ liệu thật (Phase 7):
 *   GET    /admin/employees?keyword&storeId&active&page&size   danh sách (mới nhất trước)
 *   POST   /admin/employees                                   tạo hồ sơ cho tài khoản đã có quyền STAFF (201),
 *                                                             có thể gán chi nhánh ngay
 *   PUT    /admin/employees/{id}                              sửa toàn bộ; active=false = đã nghỉ (kết thúc phân công)
 *   POST   /admin/employees/{id}/assignment                   gán / chuyển chi nhánh (giữ lịch sử)
 *   DELETE /admin/employees/{id}/assignment                   rút khỏi chi nhánh
 * Tài khoản chọn được khi thêm: GET /admin/users?role=STAFF, trừ tài khoản đã có hồ sơ.
 * Vị trí tại chi nhánh chọn từ STORE_POSITIONS (staff-auth.js); "Quản lý chi nhánh"
 * là vai trò Quản lý chi nhánh ở giao diện.
 */

const EMPLOYEE_PAGE_SIZE = 20;


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const filterForm = document.getElementById("employeeFilterForm");

    const keywordInput = document.getElementById("employeeKeyword");

    const storeFilter = document.getElementById("storeFilter");

    const statusFilter = document.getElementById("employeeStatusFilter");

    const formPanel = document.getElementById("employeeFormPanel");

    const form = document.getElementById("employeeForm");

    const formError = document.getElementById("employeeFormError");

    const saveButton = document.getElementById("employeeSaveBtn");

    const userSelect = document.getElementById("empUserId");

    const userHint = document.getElementById("empUserHint");

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


    [createPositionSelect, document.getElementById("assignPosition")].forEach(function (select) {
        select.innerHTML = STORE_POSITIONS.map(function (position) {
            return `<option value="${escapeHtml(position)}">${escapeHtml(position)}</option>`;
        }).join("");
    });


    try {

        stores = await loadAllStores();

    } catch (error) {

        handleError(error, function (message) {
            showToast("Không tải được danh sách chi nhánh: " + message, "error");
        });

    }

    fillStoreOptions(storeFilter, [{ value: "", label: "Tất cả chi nhánh" }], stores);


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

    createStoreSelect.addEventListener("change", syncCreatePosition);

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

        if (storeFilter.value) {
            params.set("storeId", storeFilter.value);
        }

        if (statusFilter.value) {
            params.set("active", statusFilter.value);
        }


        tbody.innerHTML = adminEmptyRow(7, "Đang tải…");


        try {

            const page = await apiRequest("/admin/employees?" + params.toString(), { auth: true });

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

        const storeCell = assignment
            ? `
                ${escapeHtml(assignment.storeName)}
                <span class="admin-subtext">
                    ${escapeHtml(assignment.positionAtStore || "—")} · từ ${escapeHtml(formatDateVi(assignment.startDate))}
                </span>
                ${manager ? '<span class="admin-badge admin-badge-info">Quản lý chi nhánh</span>' : ""}
            `
            : '<span class="admin-subtext">Chưa gán chi nhánh</span>';

        const actions = [`<button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>`];

        if (employee.active) {
            actions.push(`<button type="button" class="admin-link-btn" data-action="assign" data-id="${id}">${assignment ? "Chuyển chi nhánh" : "Gán chi nhánh"}</button>`);
        }

        if (assignment) {
            actions.push(`<button type="button" class="admin-link-btn admin-link-danger" data-action="unassign" data-id="${id}">Rút khỏi chi nhánh</button>`);
        }


        return `
            <tr data-employee-id="${id}">
                <td>
                    <strong>${escapeHtml(employee.fullname)}</strong>
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
                <td class="admin-actions-cell">${actions.join("")}</td>
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


    /* ================= THÊM / SỬA HỒ SƠ ================= */

    function field(id) {

        return document.getElementById(id);

    }


    function openStores() {

        return stores.filter(function (store) {
            return store.active !== false;
        });

    }


    async function openForm(employee) {

        closeAssign();

        editingEmployee = employee;

        const creating = !employee;

        field("employeeFormTitle").textContent = creating ? "Thêm nhân viên" : "Sửa hồ sơ " + employee.fullname;

        field("empUserField").hidden = !creating;

        field("empAccountField").hidden = creating;

        field("empStoreField").hidden = !creating;

        field("empStorePositionField").hidden = !creating;

        field("empActiveField").hidden = creating;


        field("empCode").value = employee ? employee.employeeCode || "" : "";

        field("empDepartment").value = employee ? employee.department || "" : "";

        field("empPosition").value = employee ? employee.position || "" : "";

        field("empSalary").value = employee && employee.salary != null ? employee.salary : "";

        field("empHiringDate").value = employee ? employee.hiringDate || "" : "";

        field("empActive").checked = employee ? employee.active : true;

        formError.hidden = true;

        formPanel.hidden = false;


        if (!creating) {

            field("empAccountText").value = employee.fullname + " (@" + employee.username + " · " + employee.email + ")";

            field("empCode").focus();

            return;

        }


        createStoreSelect.innerHTML = '<option value="">Chưa gán chi nhánh</option>' +
            openStores().map(function (store) {
                return `<option value="${escapeHtml(store.id)}">${escapeHtml(store.name)}</option>`;
            }).join("");

        syncCreatePosition();

        await loadSelectableAccounts();

    }


    /* Tài khoản STAFF đang hoạt động chưa có hồ sơ nhân viên */

    async function loadSelectableAccounts() {

        userSelect.innerHTML = '<option value="">Đang tải…</option>';

        userSelect.disabled = true;

        saveButton.disabled = true;

        userHint.textContent = "";


        try {

            const results = await Promise.all([
                fetchAllPages("/admin/users?role=STAFF&isActive=true&sort=fullname"),
                fetchAllPages("/admin/employees")
            ]);

            const taken = new Set(results[1].map(function (employee) {
                return employee.userId;
            }));

            const accounts = results[0].filter(function (user) {
                return !taken.has(user.id);
            });


            if (accounts.length === 0) {

                userSelect.innerHTML = '<option value="">Không có tài khoản phù hợp</option>';

                userHint.textContent = "Mọi tài khoản có quyền Nhân viên đều đã có hồ sơ. Cấp quyền Nhân viên cho tài khoản mới ở trang Người dùng trước.";

                return;

            }


            userSelect.innerHTML = accounts.map(function (user) {
                return `<option value="${escapeHtml(user.id)}">${escapeHtml(user.fullname + " (@" + user.username + " · " + user.email + ")")}</option>`;
            }).join("");

            userSelect.disabled = false;

            saveButton.disabled = false;

            userHint.textContent = "Chỉ hiện tài khoản đang hoạt động, có quyền Nhân viên và chưa có hồ sơ.";

            userSelect.focus();

        } catch (error) {

            handleError(error, function (message) {
                userSelect.innerHTML = '<option value="">Không tải được danh sách tài khoản</option>';
                userHint.textContent = message;
            });

        }

    }


    /* Vị trí tại chi nhánh chỉ có nghĩa khi chọn chi nhánh (backend báo lỗi nếu gửi riêng) */

    function syncCreatePosition() {

        createPositionSelect.disabled = !createStoreSelect.value;

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

            body.active = field("empActive").checked;

            return { body: body };

        }


        if (!userSelect.value) {
            return { error: "Vui lòng chọn tài khoản nhân viên." };
        }

        body.userId = Number(userSelect.value);

        body.storeId = createStoreSelect.value ? Number(createStoreSelect.value) : null;

        body.positionAtStore = body.storeId ? createPositionSelect.value : null;

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

        const endsAssignment = wasEditing && wasEditing.active && !result.body.active && wasEditing.assignment;

        saveButton.disabled = true;


        try {

            const saved = wasEditing
                ? await apiRequest("/admin/employees/" + wasEditing.id, { method: "PUT", body: result.body, auth: true })
                : await apiRequest("/admin/employees", { method: "POST", body: result.body, auth: true });

            showToast(
                (wasEditing ? "Đã lưu hồ sơ " : "Đã thêm nhân viên ") + saved.fullname +
                (endsAssignment ? " (đã kết thúc phân công tại " + wasEditing.assignment.storeName + ")" : "") + ".",
                "success"
            );

            closeForm();

            loadEmployees();

        } catch (error) {

            handleError(error, function (message) {
                formError.textContent = message + detailsText(error);
                formError.hidden = false;
            });

        } finally {

            saveButton.disabled = false;

        }

    }


    /* ================= GÁN / CHUYỂN / RÚT CHI NHÁNH ================= */

    function openAssign(employee) {

        closeForm();

        assigningEmployee = employee;

        const current = employee.assignment;

        field("assignTitle").textContent = (current ? "Chuyển chi nhánh: " : "Gán chi nhánh: ") + employee.fullname;

        field("assignHint").textContent = current
            ? "Đang ở " + current.storeName + " (" + (current.positionAtStore || "—") + "). Chọn chi nhánh khác thì phân công cũ " +
              "kết thúc hôm nay và lịch sử được giữ lại; chọn đúng chi nhánh hiện tại thì chỉ đổi vị trí."
            : "Nhân viên chỉ thấy đơn hàng, trả góp và tồn kho của chi nhánh được gán.";


        assignStoreSelect.innerHTML = openStores().map(function (store) {
            return `<option value="${escapeHtml(store.id)}">${escapeHtml(store.name)}</option>`;
        }).join("") || '<option value="">Chưa có chi nhánh nào đang mở</option>';

        if (current) {
            assignStoreSelect.value = String(current.storeId);
        }

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

        openConfirmModal({
            title: "Rút " + employee.fullname + " khỏi " + employee.assignment.storeName + "?",
            message: "Phân công kết thúc hôm nay (lịch sử được giữ). Nhân viên sẽ không thấy đơn hàng, trả góp hay tồn kho nào cho tới khi được gán chi nhánh khác.",
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
