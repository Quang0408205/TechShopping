/* ================= HỖ TRỢ KHÁCH HÀNG (admin/support-tickets.html) ================= */

/*
 * Mọi vai trò đều vào được; ngoài ADMIN chỉ thấy ticket của chi nhánh mình.
 * Dữ liệu mẫu (B1), nối support_tickets thật ở Phase 9.
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        const PRIORITY_BADGES = {
            LOW: "admin-badge-neutral",
            MEDIUM: "admin-badge-warning",
            HIGH: "admin-badge-danger"
        };


        const staff = await adminLayoutReady;

        if (!staff) {
            return;
        }


        const statusFilter = document.getElementById("statusFilter");

        const storeFilter = document.getElementById("storeFilter");

        const tbody = document.getElementById("ticketTableBody");


        setupMockStoreFilter(storeFilter, staff);

        [statusFilter, storeFilter].forEach(function (select) {
            select.addEventListener("change", render);
        });

        render();


        function render() {

            const tickets = getSupportTickets({
                status: statusFilter.value || undefined,
                storeId: getScopedStoreId(staff, storeFilter)
            }).sort(function (a, b) {
                return b.createdAt.localeCompare(a.createdAt);
            });


            document.getElementById("ticketRowCount").textContent =
                tickets.length + " ticket";

            tbody.innerHTML = tickets.map(renderRow).join("") ||
                adminEmptyRow(8, "Không có ticket nào phù hợp bộ lọc");


            bindStatusSelects(tbody, function (id, status) {

                updateSupportTicketStatus(id, status);

                showToast("Đã cập nhật trạng thái ticket " + id + ".", "success");

                render();

            });

        }


        function renderRow(ticket) {

            const store = getMockStoreById(ticket.storeId);

            const employee = ticket.assignedEmployeeId
                ? getMockEmployeeById(ticket.assignedEmployeeId)
                : null;


            return `
                <tr>
                    <td>${escapeHtml(ticket.id)}</td>
                    <td>${escapeHtml(ticket.subject)}</td>
                    <td>
                        ${escapeHtml(ticket.customerName)}
                        <span class="admin-subtext">${escapeHtml(ticket.customerPhone)}</span>
                    </td>
                    <td>${escapeHtml(store ? store.name : ticket.storeId)}</td>
                    <td>${escapeHtml(employee ? employee.fullname : "Chưa phân công")}</td>
                    <td>
                        <span class="admin-badge ${PRIORITY_BADGES[ticket.priority] || "admin-badge-neutral"}">
                            ${escapeHtml(TICKET_PRIORITY_LABELS[ticket.priority] || ticket.priority)}
                        </span>
                    </td>
                    <td>${escapeHtml(formatDateVi(ticket.createdAt))}</td>
                    <td>${statusSelectHtml(ticket.id, ticket.status, TICKET_STATUS_LABELS, "Trạng thái ticket " + ticket.id)}</td>
                </tr>
            `;

        }

    }
);
