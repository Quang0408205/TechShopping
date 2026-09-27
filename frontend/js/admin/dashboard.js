/* ================= TỔNG QUAN (admin/dashboard.html) ================= */

/*
 * Mọi vai trò đều vào được. ADMIN xem toàn hệ thống; nhân viên / quản lý
 * chi nhánh chỉ xem số liệu chi nhánh của mình. Dữ liệu mẫu (B1).
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        const staff = await adminLayoutReady;

        if (!staff) {
            return;
        }


        const isAdmin = staff.role === "ADMIN";

        const scope = isAdmin ? {} : { storeId: getScopedStoreId(staff) };


        if (new URLSearchParams(window.location.search).get("denied") === "1") {
            document.getElementById("deniedNotice").hidden = false;
        }


        renderGreeting();

        renderStats();

        renderRevenueByMonth();

        renderTopProducts();

        if (isAdmin) {
            renderStoreBreakdown();
        }


        function renderGreeting() {

            const today = new Date().toLocaleDateString("vi-VN", {
                weekday: "long",
                year: "numeric",
                month: "long",
                day: "numeric"
            });

            document.getElementById("dashboardGreeting").textContent =
                "Chào " + staff.fullname + ", hôm nay là " + today + ".";

        }


        function renderStats() {

            const summary = getSalesSummary(scope);

            const requests = getServiceRequests(scope);

            const openRequests = requests.filter(function (r) {
                return r.status === "RECEIVED" || r.status === "PROCESSING";
            });

            const tickets = getSupportTickets(scope);

            const openTickets = tickets.filter(function (t) {
                return t.status !== "RESOLVED";
            });


            const cards = [
                {
                    label: "Doanh thu" + (isAdmin ? " (toàn hệ thống)" : " (" + staff.storeName + ")"),
                    value: formatPrice(summary.totalRevenue),
                    sub: summary.totalOrders + " lượt bán · " + summary.totalUnits + " sản phẩm"
                },
                {
                    label: "Bảo hành / đổi trả đang xử lý",
                    value: String(openRequests.length),
                    sub: "Tổng cộng " + requests.length + " yêu cầu"
                },
                {
                    label: "Ticket hỗ trợ đang mở",
                    value: String(openTickets.length),
                    sub: "Tổng cộng " + tickets.length + " ticket"
                }
            ];


            if (isAdmin) {

                const activeEmployees = getEmployees().filter(function (e) {
                    return e.status === "ACTIVE";
                });

                cards.push({
                    label: "Chi nhánh",
                    value: String(getStores().length),
                    sub: activeEmployees.length + " nhân viên đang làm việc"
                });

            }


            document.getElementById("statGrid").innerHTML = adminStatCardsHtml(cards);

        }


        function renderRevenueByMonth() {

            const rows = getSalesSummary(scope).byMonth;

            renderLineChart(
                document.getElementById("revenueChart"),
                rows.map(function (row) {
                    return { label: formatMonthLabel(row.month, true), value: row.revenue };
                }),
                { formatValue: formatPrice }
            );

            document.querySelector("#revenueByMonthTable tbody").innerHTML =
                rows.map(function (row) {
                    return `
                        <tr>
                            <td>${escapeHtml(formatMonthLabel(row.month))}</td>
                            <td>${row.orders}</td>
                            <td>${escapeHtml(formatPrice(row.revenue))}</td>
                        </tr>
                    `;
                }).join("") || adminEmptyRow(3, "Chưa có dữ liệu");

        }


        function renderTopProducts() {

            const rows = getSalesSummary(scope).topProducts;

            renderBarChart(
                document.getElementById("topProductsChart"),
                rows.map(function (row) {
                    return { label: shortProductLabel(row.name), title: row.name, value: row.revenue };
                }),
                { formatValue: formatPrice }
            );

            document.querySelector("#topProductsTable tbody").innerHTML =
                rows.map(function (row) {
                    return `
                        <tr>
                            <td>${escapeHtml(row.name)}</td>
                            <td>${row.quantity}</td>
                            <td>${escapeHtml(formatPrice(row.revenue))}</td>
                        </tr>
                    `;
                }).join("") || adminEmptyRow(3, "Chưa có dữ liệu");

        }


        function renderStoreBreakdown() {

            const rows = getSalesSummary({}).byStore;

            document.getElementById("storeBreakdownPanel").hidden = false;

            renderBarChart(
                document.getElementById("storeBreakdownChart"),
                rows.map(function (row) {
                    return { label: truncateText(row.storeName, 16), value: row.revenue };
                }),
                { formatValue: formatPrice, color: cssVar("--color-accent", "#8a5a22") }
            );

            document.querySelector("#storeBreakdownTable tbody").innerHTML =
                rows.map(function (row) {
                    return `
                        <tr>
                            <td>${escapeHtml(row.storeName)}</td>
                            <td>${row.orders}</td>
                            <td>${escapeHtml(formatPrice(row.revenue))}</td>
                        </tr>
                    `;
                }).join("") || adminEmptyRow(3, "Chưa có dữ liệu");

        }

    }
);
