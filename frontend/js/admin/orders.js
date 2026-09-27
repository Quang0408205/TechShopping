/* ================= ĐƠN HÀNG CHI NHÁNH (admin/orders.html) ================= */

/*
 * Quản lý chi nhánh + ADMIN. Quản lý chỉ thấy đơn của chi nhánh mình; ADMIN
 * thấy tất cả và lọc được đơn online chưa gán chi nhánh. Bấm ▸ để xem chi
 * tiết. Dữ liệu mẫu (B1), nối orders thật ở Phase 4.
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        const staff = await adminLayoutReady;

        if (!staff) {
            return;
        }


        const storeFilter = document.getElementById("storeFilter");

        const statusFilter = document.getElementById("statusFilter");

        const tbody = document.getElementById("orderTableBody");


        setupStoreFilter(storeFilter, staff, [
            { value: "UNASSIGNED", label: "Đơn online chưa gán chi nhánh" }
        ]);

        [storeFilter, statusFilter].forEach(function (select) {
            select.addEventListener("change", render);
        });

        render();


        function render() {

            const orders = getOrdersForStaff({
                storeId: getScopedStoreId(staff, storeFilter),
                status: statusFilter.value || undefined
            }).sort(function (a, b) {
                return b.createdAt.localeCompare(a.createdAt);
            });


            document.getElementById("orderRowCount").textContent =
                orders.length + " đơn hàng";

            tbody.innerHTML = orders.map(renderOrderRows).join("") ||
                adminEmptyRow(8, "Không có đơn hàng nào phù hợp bộ lọc");


            tbody.querySelectorAll(".js-toggle-detail").forEach(function (button) {

                button.addEventListener("click", function () {

                    const detailRow = tbody.querySelector(
                        '.js-order-detail[data-order-id="' + button.dataset.orderId + '"]'
                    );

                    detailRow.hidden = !detailRow.hidden;

                    button.textContent = detailRow.hidden ? "▸" : "▾";

                    button.setAttribute("aria-expanded", String(!detailRow.hidden));

                });

            });


            bindStatusSelects(tbody, function (id, status) {

                updateOrderStatus(id, status);

                showToast("Đã cập nhật trạng thái đơn " + id + ".", "success");

                render();

            });

        }


        function renderOrderRows(order) {

            const store = order.storeId ? getStoreById(order.storeId) : null;

            const firstItem = order.items[0];

            const productSummary = order.items.length > 1
                ? firstItem.name + " và " + (order.items.length - 1) + " sản phẩm khác"
                : firstItem.name;


            const itemRows = order.items.map(function (item) {
                return `
                    <tr>
                        <td>${escapeHtml(item.name)}</td>
                        <td>${escapeHtml(item.variantLabel || "—")}</td>
                        <td>${item.quantity}</td>
                        <td>${escapeHtml(formatPrice(item.price))}</td>
                    </tr>
                `;
            }).join("");


            return `
                <tr>
                    <td>
                        <button
                            type="button"
                            class="admin-row-toggle js-toggle-detail"
                            data-order-id="${escapeHtml(order.id)}"
                            aria-label="Xem chi tiết đơn ${escapeHtml(order.id)}"
                            aria-expanded="false"
                        >▸</button>
                    </td>
                    <td>${escapeHtml(order.id)}</td>
                    <td>${escapeHtml(formatDateVi(order.createdAt))}</td>
                    <td>
                        ${store
                            ? escapeHtml(store.name)
                            : '<span class="admin-badge admin-badge-neutral">Online, chưa gán</span>'}
                    </td>
                    <td>${escapeHtml(productSummary)}</td>
                    <td>${escapeHtml(formatPrice(order.total))}</td>
                    <td>${escapeHtml(PAYMENT_METHOD_LABELS[order.paymentMethod] || order.paymentMethod)}</td>
                    <td>${statusSelectHtml(order.id, order.status, ORDER_STATUS_LABELS, "Trạng thái đơn " + order.id)}</td>
                </tr>
                <tr class="js-order-detail" data-order-id="${escapeHtml(order.id)}" hidden>
                    <td></td>
                    <td colspan="7">
                        <div class="admin-order-detail">
                            <strong>Khách hàng:</strong> ${escapeHtml(order.customerName)} ·
                            <strong>Giao tới:</strong> ${escapeHtml(order.shippingAddress)} ·
                            <strong>Phí giao hàng:</strong> ${escapeHtml(formatPrice(order.shippingFee || 0))}
                            <table class="admin-table">
                                <thead>
                                    <tr><th>Sản phẩm</th><th>Phân loại</th><th>Số lượng</th><th>Đơn giá</th></tr>
                                </thead>
                                <tbody>${itemRows}</tbody>
                            </table>
                        </div>
                    </td>
                </tr>
            `;

        }

    }
);
