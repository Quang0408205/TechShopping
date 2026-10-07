/* ================= BẢO HÀNH / BẢO TRÌ / ĐỔI TRẢ (admin/service-requests.html) ================= */

/*
 * Mọi vai trò đều vào được; ngoài ADMIN chỉ thấy yêu cầu của chi nhánh
 * mình. Đổi trạng thái ngay trong bảng (mô phỏng PATCH, dữ liệu mẫu B1).
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        const staff = await adminLayoutReady;

        if (!staff) {
            return;
        }


        const typeFilter = document.getElementById("typeFilter");

        const statusFilter = document.getElementById("statusFilter");

        const storeFilter = document.getElementById("storeFilter");

        const tbody = document.getElementById("requestTableBody");


        setupMockStoreFilter(storeFilter, staff);

        [typeFilter, statusFilter, storeFilter].forEach(function (select) {
            select.addEventListener("change", render);
        });

        render();


        function render() {

            const requests = getServiceRequests({
                type: typeFilter.value || undefined,
                status: statusFilter.value || undefined,
                storeId: getScopedStoreId(staff, storeFilter)
            }).sort(function (a, b) {
                return b.createdAt.localeCompare(a.createdAt);
            });


            document.getElementById("requestRowCount").textContent =
                requests.length + " yêu cầu";

            tbody.innerHTML = requests.map(renderRow).join("") ||
                adminEmptyRow(8, "Không có yêu cầu nào phù hợp bộ lọc");


            bindStatusSelects(tbody, function (id, status) {

                updateServiceRequestStatus(id, status);

                showToast("Đã cập nhật trạng thái yêu cầu " + id + ".", "success");

                render();

            });

        }


        function renderRow(request) {

            const store = getMockStoreById(request.storeId);

            const employee = request.assignedEmployeeId
                ? getMockEmployeeById(request.assignedEmployeeId)
                : null;


            return `
                <tr>
                    <td>${escapeHtml(request.id)}</td>
                    <td>${escapeHtml(SERVICE_TYPE_LABELS[request.type] || request.type)}</td>
                    <td>
                        ${escapeHtml(request.productName)}
                        <span class="admin-subtext">${escapeHtml(request.description)}</span>
                    </td>
                    <td>
                        ${escapeHtml(request.customerName)}
                        <span class="admin-subtext">${escapeHtml(request.customerPhone)}</span>
                    </td>
                    <td>${escapeHtml(store ? store.name : request.storeId)}</td>
                    <td>${escapeHtml(employee ? employee.fullname : "Chưa phân công")}</td>
                    <td>${escapeHtml(formatDateVi(request.createdAt))}</td>
                    <td>${statusSelectHtml(request.id, request.status, SERVICE_STATUS_LABELS, "Trạng thái yêu cầu " + request.id)}</td>
                </tr>
            `;

        }

    }
);
