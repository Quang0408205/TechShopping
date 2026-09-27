/* ================= NHÂN VIÊN (admin/employees.html) ================= */

/*
 * Quản lý chi nhánh: xem nhân viên chi nhánh mình (chỉ đọc).
 * ADMIN: xem tất cả, thêm nhân viên, khoá / mở tài khoản (trừ chính mình).
 * Dữ liệu mẫu (B1); tài khoản nhân viên thật (users + employees) ở B2 / Phase 7.
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        const staff = await adminLayoutReady;

        if (!staff) {
            return;
        }


        const isAdmin = staff.role === "ADMIN";

        const storeFilter = document.getElementById("storeFilter");

        const tbody = document.getElementById("employeeTableBody");


        setupStoreFilter(storeFilter, staff);

        storeFilter.addEventListener("change", render);


        if (isAdmin) {
            setupCreateForm();
        }

        render();


        function render() {

            const storeId = getScopedStoreId(staff, storeFilter);

            const employees = storeId ? getEmployeesByStore(storeId) : getEmployees();


            document.getElementById("employeeRowCount").textContent =
                employees.length + " nhân viên";

            tbody.innerHTML = employees.map(renderRow).join("") ||
                adminEmptyRow(8, "Không có nhân viên nào phù hợp bộ lọc");


            tbody.querySelectorAll(".js-toggle-status").forEach(function (button) {

                button.addEventListener("click", function () {

                    const nextStatus = button.dataset.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";

                    const updated = updateEmployee(button.dataset.id, { status: nextStatus });

                    showToast(
                        (nextStatus === "ACTIVE" ? "Đã mở khoá " : "Đã khoá ") + updated.fullname + ".",
                        "success"
                    );

                    render();

                });

            });

        }


        function renderRow(employee) {

            /* Không cho tự khoá chính mình (giống CANNOT_MODIFY_OWN_ACCOUNT của backend) */

            const isSelf = employee.id === staff.id;

            const statusCell = isAdmin && !isSelf
                ? statusToggleHtml(employee)
                : statusBadgeHtml(employee);


            return `
                <tr>
                    <td>
                        ${escapeHtml(employee.fullname)}
                        ${isSelf ? '<span class="admin-subtext">(bạn)</span>' : ""}
                    </td>
                    <td>${escapeHtml(employee.email)}</td>
                    <td>${escapeHtml(employee.phone || "—")}</td>
                    <td>${escapeHtml(employee.roleLabel)}</td>
                    <td>${escapeHtml(employee.storeName)}</td>
                    <td>${escapeHtml(employee.position)}</td>
                    <td>${escapeHtml(formatDateVi(employee.joinedAt))}</td>
                    <td>${statusCell}</td>
                </tr>
            `;

        }


        function statusBadgeHtml(employee) {

            return employee.status === "ACTIVE"
                ? '<span class="admin-badge admin-badge-success">Đang làm việc</span>'
                : '<span class="admin-badge admin-badge-danger">Đã khoá</span>';

        }


        function statusToggleHtml(employee) {

            const active = employee.status === "ACTIVE";

            return `
                <button
                    type="button"
                    class="admin-badge ${active ? "admin-badge-success" : "admin-badge-danger"} js-toggle-status"
                    data-id="${escapeHtml(employee.id)}"
                    data-status="${escapeHtml(employee.status)}"
                    title="${active ? "Bấm để khoá tài khoản" : "Bấm để mở khoá"}"
                >
                    ${active ? "Đang làm việc" : "Đã khoá"}
                </button>
            `;

        }


        /* ================= THÊM NHÂN VIÊN (ADMIN) ================= */

        function setupCreateForm() {

            const toggleButton = document.getElementById("toggleCreateFormBtn");

            const panel = document.getElementById("createEmployeePanel");

            const form = document.getElementById("createEmployeeForm");

            const errorBox = document.getElementById("createEmployeeError");

            const roleSelect = document.getElementById("newRole");

            const storeSelect = document.getElementById("newStoreId");


            toggleButton.hidden = false;

            storeSelect.innerHTML = getStores().map(function (store) {
                return `<option value="${escapeHtml(store.id)}">${escapeHtml(store.name)}</option>`;
            }).join("");


            /* Quản trị viên không thuộc chi nhánh nào */

            roleSelect.addEventListener("change", function () {
                storeSelect.disabled = roleSelect.value === "ADMIN";
            });


            toggleButton.addEventListener("click", function () {
                panel.hidden = !panel.hidden;
            });


            document.getElementById("cancelCreateBtn").addEventListener("click", function () {

                panel.hidden = true;

                errorBox.hidden = true;

                form.reset();

                storeSelect.disabled = false;

            });


            form.addEventListener("submit", function (event) {

                event.preventDefault();


                const fullname = document.getElementById("newFullname").value.trim();

                const email = document.getElementById("newEmail").value.trim();

                const phone = document.getElementById("newPhone").value.trim();


                let error = "";

                if (!fullname || !email) {
                    error = "Vui lòng nhập họ tên và email.";
                } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                    error = "Email không hợp lệ.";
                } else if (isEmployeeEmailTaken(email)) {
                    error = "Email này đã được dùng cho một nhân viên khác.";
                } else if (phone && !/^[0-9 +().-]{8,20}$/.test(phone)) {
                    error = "Số điện thoại không hợp lệ.";
                }


                if (error) {

                    errorBox.textContent = error;

                    errorBox.hidden = false;

                    return;

                }


                errorBox.hidden = true;

                const created = createEmployee({
                    fullname: fullname,
                    email: email,
                    phone: phone,
                    position: document.getElementById("newPosition").value.trim(),
                    role: roleSelect.value,
                    storeId: storeSelect.value || null
                });


                form.reset();

                storeSelect.disabled = false;

                panel.hidden = true;

                showToast("Đã thêm nhân viên " + created.fullname + ".", "success");

                render();

            });

        }

    }
);
