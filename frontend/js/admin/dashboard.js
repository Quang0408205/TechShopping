/* ================= TỔNG QUAN (admin/dashboard.html) ================= */

/*
 * Một trang, ba bố cục theo vai trò (dữ liệu thật):
 *   ADMIN           "Toàn hệ thống": thẻ số GET /admin/dashboard/summary (doanh thu hôm nay / tháng này so với
 *                   cùng kỳ tháng trước, đơn chờ xác nhận / đang giao, yêu cầu dịch vụ mở, hàng sắp hết, chi nhánh,
 *                   nhân viên) + 30 ngày từ GET /admin/reports/sales (doanh thu thuần, danh mục, so sánh chi nhánh,
 *                   sản phẩm bán chạy) + lối tắt.
 *   BRANCH_MANAGER  "Chi nhánh <tên>": cùng các số của chi nhánh mình + đội ngũ (GET /branch/employees).
 *   EMPLOYEE        "Việc của tôi hôm nay": KHÔNG có doanh thu; danh sách việc có số đếm và link lọc sẵn.
 * Nhân viên chưa có chi nhánh: màn hình chờ, không gọi API nào (backend sẽ trả 403).
 */

document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const isAdmin = staff.role === "ADMIN";

    const isManager = staff.role === "BRANCH_MANAGER";

    const errorBox = document.getElementById("dashboardError");


    if (new URLSearchParams(window.location.search).get("denied") === "1") {
        document.getElementById("deniedNotice").hidden = false;
    }


    document.getElementById("dashboardTitle").textContent = isAdmin
        ? "Toàn hệ thống"
        : (isManager ? "Chi nhánh " + staff.storeName : "Việc của tôi hôm nay");

    document.getElementById("dashboardGreeting").textContent = "Chào " + staff.fullname + ", hôm nay là " +
        new Date().toLocaleDateString("vi-VN", { weekday: "long", year: "numeric", month: "long", day: "numeric" }) + ".";


    if (!isAdmin && !staff.storeId) {

        document.getElementById("dashboardWaiting").hidden = false;

        return;

    }


    renderShortcuts();

    document.getElementById("statGrid").innerHTML = '<span class="admin-skeleton admin-skeleton--card"></span>'.repeat(4);


    let summary;

    try {

        summary = await apiRequest("/admin/dashboard/summary", { auth: true });

    } catch (error) {

        showError(error);

        document.getElementById("statGrid").innerHTML = "";

        return;

    }


    if (isAdmin || isManager) {

        renderManagementCards(summary);

        loadSalesPanels();

        if (isManager) {
            loadTeam();
        }

    } else {

        document.getElementById("statGrid").innerHTML = "";

        /* Nhân viên không xem doanh thu: bỏ hẳn các khối quản lý khỏi trang */
        ["chartGrid", "storeBreakdownPanel", "teamPanel", "topProductsPanel"].forEach(function (id) {
            document.getElementById(id).remove();
        });

        loadTasks(summary);

    }


    /* ================= LỐI TẮT ================= */

    function renderShortcuts() {

        const links = isAdmin
            ? [["employees.html?hire=1", "Tuyển nhân sự"], ["products.html", "Thêm sản phẩm"],
                ["promotions.html", "Tạo khuyến mãi"], ["stores.html", "Chi nhánh"]]
            : isManager
                ? [["employees.html?hire=1", "Tuyển nhân viên"], ["inventory.html", "Nhập kho"], ["reports.html", "Báo cáo"]]
                : [["orders.html?status=PENDING", "Đơn chờ xác nhận"], ["inventory.html", "Tồn kho"]];

        document.getElementById("dashboardShortcuts").innerHTML = links.map(function (link) {
            return `<a href="${escapeHtml(link[0])}">${escapeHtml(link[1])}</a>`;
        }).join("");

    }


    /* ================= ADMIN / QUẢN LÝ ================= */

    function renderManagementCards(s) {

        const cards = [
            {
                label: "Doanh thu thuần hôm nay",
                value: formatPrice(s.revenueToday || 0),
                sub: s.deliveredToday + " đơn đã giao"
            },
            {
                label: "Doanh thu thuần tháng này",
                value: formatPrice(s.revenueThisMonth || 0),
                sub: trendText(s.revenueThisMonth, s.revenuePreviousMonthSamePeriod)
            },
            {
                label: "Đơn cần xử lý",
                value: String(s.pendingOrders + s.confirmedOrders),
                sub: s.pendingOrders + " chờ xác nhận · " + s.confirmedOrders + " chờ giao · " + s.shippingOrders + " đang giao"
            },
            {
                label: "Yêu cầu dịch vụ đang mở",
                value: String(s.openServiceRequests),
                sub: "Bảo hành / bảo trì / đổi trả"
            },
            {
                label: "Hàng sắp hết / hết hàng",
                value: s.lowStockVariants + " / " + s.outOfStockVariants,
                sub: "Phiên bản còn 1–5 cái / còn 0"
            }
        ];

        if (isAdmin) {
            cards.push({
                label: "Chi nhánh đang mở",
                value: String(s.openStores != null ? s.openStores : "—"),
                sub: (s.activeEmployees != null ? s.activeEmployees : "—") + " nhân viên đang làm việc"
            });
        }

        document.getElementById("statGrid").innerHTML = adminStatCardsHtml(cards);

    }


    /* "+12,5% so với cùng kỳ tháng trước" */

    function trendText(current, previous) {

        const now = Number(current || 0);

        const before = Number(previous || 0);

        if (before === 0) {
            return now > 0 ? "Cùng kỳ tháng trước: 0đ" : "Chưa có doanh thu";
        }

        const change = (now - before) * 100 / before;

        return (change >= 0 ? "+" : "") + change.toFixed(1).replace(".", ",") + "% so với cùng kỳ tháng trước";

    }


    async function loadSalesPanels() {

        document.getElementById("chartGrid").hidden = false;

        document.getElementById("topProductsPanel").hidden = false;

        document.querySelector("#topProductsTable tbody").innerHTML = adminSkeletonRow(3);


        let report;

        try {

            report = await apiRequest("/admin/reports/sales?groupBy=DAY", { auth: true });

        } catch (error) {

            showError(error);

            return;

        }


        document.getElementById("revenuePeriod").textContent = formatDateVi(report.fromDate) + " – " + formatDateVi(report.toDate);

        const hasSales = report.summary.deliveredOrders > 0 || Number(report.summary.refundAmount) > 0;

        if (hasSales) {

            renderBarChart(document.getElementById("revenueChart"), report.series.map(function (p) {
                return { label: dayLabel(p.start), title: formatDateVi(p.start), value: Number(p.netRevenue) };
            }), { formatValue: formatPrice, minSlotWidth: 8, labelEvery: 7 });

        } else {

            document.getElementById("revenueChart").innerHTML = '<p class="admin-chart-empty">Chưa có đơn đã giao trong 30 ngày qua.</p>';

        }

        renderPieChart(document.getElementById("categoryChart"), report.byCategory.map(function (row) {
            return { label: row.categoryName || "Chưa phân loại", value: Number(row.revenue) };
        }), { formatValue: formatPrice, emptyText: "Chưa có đơn đã giao trong 30 ngày qua.", ariaLabel: "Doanh thu theo danh mục" });


        document.querySelector("#topProductsTable tbody").innerHTML = report.topProducts.map(function (row) {
            return `
                <tr>
                    <td>${escapeHtml(row.productName)}</td>
                    <td class="admin-number">${row.units}</td>
                    <td class="admin-number">${escapeHtml(formatPrice(row.revenue))}</td>
                </tr>
            `;
        }).join("") || adminEmptyRow(3, "Chưa có đơn đã giao trong 30 ngày qua.");


        if (isAdmin) {

            document.getElementById("storeBreakdownPanel").hidden = false;

            document.querySelector("#storeBreakdownTable tbody").innerHTML = report.byStore.map(function (row) {
                return `
                    <tr>
                        <td>${escapeHtml(row.storeName)}${row.storeActive ? "" : ' <span class="admin-subtext">(tạm đóng)</span>'}</td>
                        <td class="admin-number">${row.orders}</td>
                        <td class="admin-number">${escapeHtml(formatPrice(row.grossRevenue))}</td>
                        <td class="admin-number">${escapeHtml(formatPrice(row.refundAmount))}</td>
                        <td class="admin-number">${escapeHtml(formatPrice(row.netRevenue))}</td>
                    </tr>
                `;
            }).join("") || adminEmptyRow(5, "Chưa có chi nhánh nào đang mở.");

        }

    }


    async function loadTeam() {

        const panel = document.getElementById("teamPanel");

        panel.hidden = false;

        const list = document.getElementById("teamList");

        list.innerHTML = '<li><span class="admin-skeleton"></span></li>';


        try {

            const page = await apiRequest("/branch/employees?active=true&size=6&sort=id,asc", { auth: true });

            document.getElementById("teamCount").textContent = page.totalElements + " người đang làm · Xem tất cả";

            list.innerHTML = (page.content || []).map(function (employee) {
                const label = employee.assignment ? employee.assignment.positionAtStore : "";
                return `
                    <li>
                        <span class="admin-avatar" aria-hidden="true">${escapeHtml(initialsOf(employee.fullname))}</span>
                        <span>${escapeHtml(employee.fullname)}${employee.userId === staff.id ? " (bạn)" : ""}<small>${escapeHtml(label || "—")}</small></span>
                    </li>
                `;
            }).join("") || "<li>Chưa có nhân viên nào.</li>";

        } catch (error) {

            list.innerHTML = "<li>" + escapeHtml(getErrorMessage(error)) + "</li>";

        }

    }


    /* ================= NHÂN VIÊN: VIỆC CẦN LÀM ================= */

    async function loadTasks(s) {

        document.getElementById("taskPanel").hidden = false;

        /* Trả góp chờ duyệt không có trong summary: đếm bằng danh sách có sẵn (size=1 → totalElements) */
        let installmentsWaiting = null;

        try {
            installmentsWaiting = (await apiRequest("/admin/installments?status=PENDING_APPROVAL&size=1", { auth: true })).totalElements;
        } catch (error) {
            installmentsWaiting = null;
        }


        const tasks = [
            { href: "orders.html?status=PENDING", label: "Đơn chờ xác nhận", hint: "Kiểm tra hàng, xác nhận hoặc huỷ", count: s.pendingOrders },
            { href: "orders.html?status=CONFIRMED", label: "Đơn chờ giao", hint: "Đã xác nhận, cần chuyển cho đơn vị vận chuyển", count: s.confirmedOrders },
            { href: "orders.html?status=SHIPPING", label: "Đơn đang giao", hint: "Cập nhật khi khách đã nhận", count: s.shippingOrders },
            { href: "installments.html?status=PENDING_APPROVAL", label: "Trả góp chờ duyệt", hint: "Hồ sơ trả góp cần duyệt", count: installmentsWaiting },
            { href: "service-requests.html", label: "Bảo hành / đổi trả đang mở", hint: "Yêu cầu dịch vụ chưa xong", count: s.openServiceRequests },
            { href: "inventory.html", label: "Sản phẩm sắp hết", hint: s.outOfStockVariants + " phiên bản đã hết hàng", count: s.lowStockVariants }
        ];

        const total = tasks.reduce(function (sum, task) { return sum + (task.count || 0); }, 0);

        document.getElementById("taskTotal").textContent = total + " việc";

        document.getElementById("taskList").innerHTML = tasks.map(function (task) {
            const count = task.count == null ? "—" : String(task.count);
            return `
                <li>
                    <a href="${escapeHtml(task.href)}" data-task="${escapeHtml(task.href)}">
                        <span class="admin-task-label">${escapeHtml(task.label)}<span class="admin-task-hint">${escapeHtml(task.hint)}</span></span>
                        <span class="admin-task-count ${task.count > 0 ? "has-work" : ""}">${escapeHtml(count)}</span>
                    </a>
                </li>
            `;
        }).join("");

    }


    /* ================= TIỆN ÍCH ================= */

    /* "2026-10-09" → "9/10" */

    function dayLabel(value) {

        const parts = String(value).split("-");

        return Number(parts[2]) + "/" + Number(parts[1]);

    }


    function showError(error) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        errorBox.textContent = getErrorMessage(error);

        errorBox.hidden = false;

    }

});
