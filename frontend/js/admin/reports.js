/* ================= BÁO CÁO DOANH THU (admin/reports.html) ================= */

/*
 * Quản lý chi nhánh (khoá chi nhánh mình) + ADMIN (mọi chi nhánh, có biểu
 * đồ so sánh khi xem tất cả). Lọc theo khoảng ngày. Phần doanh thu là dữ liệu
 * mẫu (B1) tới Phase 10 (sales_records / v_sales_by_store).
 *
 * Phase 7.10, chỉ ADMIN, dữ liệu THẬT, bộ lọc riêng (chi nhánh thật + khoảng ngày):
 *   GET /admin/inventory/stock-in-stats?storeId&fromDate&toDate
 *   → tổng số lượng / số lần nhập / số phiên bản / số nhà cung cấp + từng chi nhánh.
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

        const fromDateFilter = document.getElementById("fromDateFilter");

        const toDateFilter = document.getElementById("toDateFilter");

        const dateError = document.getElementById("reportDateError");


        setupMockStoreFilter(storeFilter, staff);

        [storeFilter, fromDateFilter, toDateFilter].forEach(function (input) {
            input.addEventListener("change", render);
        });


        document.getElementById("clearReportFiltersBtn")
            .addEventListener("click", function () {

                fromDateFilter.value = "";

                toDateFilter.value = "";

                if (isAdmin) {
                    storeFilter.value = "";
                }

                render();

            });


        render();

        /* Phase 7.10: hàng nhập kho (dữ liệu thật), chỉ ADMIN */
        if (isAdmin) {
            setupStockInStats();
        }


        function render() {

            if (fromDateFilter.value && toDateFilter.value &&
                fromDateFilter.value > toDateFilter.value) {

                dateError.textContent = "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.";

                dateError.hidden = false;

                return;

            }

            dateError.hidden = true;


            const filter = {
                storeId: getScopedStoreId(staff, storeFilter),
                from: fromDateFilter.value || undefined,
                to: toDateFilter.value || undefined
            };

            const summary = getSalesSummary(filter);

            const records = getSalesRecords(filter).sort(function (a, b) {
                return b.soldAt.localeCompare(a.soldAt);
            });


            renderStatCards(summary);


            renderLineChart(
                document.getElementById("reportRevenueChart"),
                summary.byMonth.map(function (row) {
                    return { label: formatMonthLabel(row.month, true), value: row.revenue };
                }),
                { formatValue: formatPrice }
            );


            renderBarChart(
                document.getElementById("reportTopProductsChart"),
                summary.topProducts.map(function (row) {
                    return { label: shortProductLabel(row.name), title: row.name, value: row.revenue };
                }),
                { formatValue: formatPrice }
            );


            /* So sánh chi nhánh chỉ có nghĩa khi ADMIN xem tất cả */

            const showStoreComparison = isAdmin && !filter.storeId;

            document.getElementById("reportStoreChartPanel").hidden = !showStoreComparison;

            if (showStoreComparison) {

                renderBarChart(
                    document.getElementById("reportStoreChart"),
                    summary.byStore.map(function (row) {
                        return { label: truncateText(row.storeName, 16), value: row.revenue };
                    }),
                    { formatValue: formatPrice, color: cssVar("--color-accent", "#8a5a22") }
                );

            }


            renderDetailTable(records);

        }


        function renderStatCards(summary) {

            const average = summary.totalOrders > 0
                ? Math.round(summary.totalRevenue / summary.totalOrders)
                : 0;


            document.getElementById("reportStatGrid").innerHTML = adminStatCardsHtml([
                {
                    label: "Tổng doanh thu",
                    value: formatPrice(summary.totalRevenue),
                    sub: summary.totalOrders + " lượt bán"
                },
                {
                    label: "Số sản phẩm đã bán",
                    value: String(summary.totalUnits),
                    sub: ""
                },
                {
                    label: "Giá trị trung bình / lượt bán",
                    value: formatPrice(average),
                    sub: ""
                }
            ]);

        }


        function renderDetailTable(records) {

            document.getElementById("reportRowCount").textContent =
                records.length + " bản ghi";


            document.querySelector("#salesDetailTable tbody").innerHTML =
                records.map(function (record) {

                    const store = getMockStoreById(record.storeId);

                    const employee = getMockEmployeeById(record.employeeId);

                    return `
                        <tr>
                            <td>${escapeHtml(formatDateVi(record.soldAt))}</td>
                            <td>${escapeHtml(store ? store.name : record.storeId)}</td>
                            <td>${escapeHtml(employee ? employee.fullname : record.employeeId)}</td>
                            <td>${escapeHtml(record.productName)}</td>
                            <td>${record.quantity}</td>
                            <td>${escapeHtml(formatPrice(record.revenue))}</td>
                        </tr>
                    `;

                }).join("") || adminEmptyRow(6, "Không có dữ liệu phù hợp bộ lọc");

        }

    }
);


/* ================= HÀNG NHẬP KHO THEO CHI NHÁNH (Phase 7.10, chỉ ADMIN) ================= */

async function setupStockInStats() {

    const panel = document.getElementById("stockInPanel");

    const storeFilter = document.getElementById("stockInStoreFilter");

    const fromDateInput = document.getElementById("stockInFromDate");

    const toDateInput = document.getElementById("stockInToDate");

    const errorBox = document.getElementById("stockInError");

    const tbody = document.querySelector("#stockInStoreTable tbody");

    let requestCounter = 0;


    panel.hidden = false;

    storeFilter.innerHTML = '<option value="">Tất cả chi nhánh</option>';

    [storeFilter, fromDateInput, toDateInput].forEach(function (input) {
        input.addEventListener("change", loadStats);
    });

    document.getElementById("clearStockInFiltersBtn").addEventListener("click", function () {

        storeFilter.value = "";

        fromDateInput.value = "";

        toDateInput.value = "";

        loadStats();

    });


    try {

        fillStoreOptions(storeFilter, [{ value: "", label: "Tất cả chi nhánh" }], await loadAllStores());

    } catch (error) {

        showStockInError(error);

    }

    loadStats();


    async function loadStats() {

        const fromDate = fromDateInput.value;

        const toDate = toDateInput.value;

        if (fromDate && toDate && fromDate > toDate) {

            errorBox.textContent = "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.";

            errorBox.hidden = false;

            return;

        }

        errorBox.hidden = true;


        const params = new URLSearchParams();

        if (storeFilter.value) {
            params.set("storeId", storeFilter.value);
        }

        if (fromDate) {
            params.set("fromDate", fromDate);
        }

        if (toDate) {
            params.set("toDate", toDate);
        }


        const requestId = ++requestCounter;

        tbody.innerHTML = adminEmptyRow(5, "Đang tải…");

        try {

            const stats = await apiRequest("/admin/inventory/stock-in-stats?" + params.toString(), { auth: true });

            if (requestId === requestCounter) {
                renderStats(stats);
            }

        } catch (error) {

            if (requestId === requestCounter) {
                showStockInError(error);
                tbody.innerHTML = adminEmptyRow(5, getErrorMessage(error));
            }

        }

    }


    function renderStats(stats) {

        document.getElementById("stockInPeriod").textContent = periodText(stats.fromDate, stats.toDate);

        document.getElementById("stockInStatGrid").innerHTML = adminStatCardsHtml([
            { label: "Tổng số lượng nhập", value: formatNumberVi(stats.totalQuantity), sub: "sản phẩm" },
            { label: "Số lần nhập kho", value: formatNumberVi(stats.stockInCount), sub: "" },
            { label: "Số phiên bản đã nhập", value: formatNumberVi(stats.variantCount), sub: "" },
            { label: "Số nhà cung cấp", value: formatNumberVi(stats.supplierCount), sub: "không tính lần nhập không ghi NCC" }
        ]);


        const stores = stats.stores || [];

        /* So sánh chi nhánh chỉ có nghĩa khi xem tất cả */
        const chart = document.getElementById("stockInStoreChart");

        chart.hidden = Boolean(stats.storeId);

        if (!stats.storeId) {

            renderBarChart(
                chart,
                stats.totalQuantity > 0
                    ? stores.map(function (row) {
                        return { label: truncateText(row.storeName, 16), title: row.storeName, value: row.quantity };
                    })
                    : [],
                { formatValue: formatNumberVi, color: cssVar("--color-accent", "#8a5a22") }
            );

        }


        tbody.innerHTML = stores.map(function (row) {

            return `
                <tr>
                    <td>
                        ${escapeHtml(row.storeName)}
                        ${row.storeActive ? "" : ' <span class="admin-badge admin-badge-neutral">Tạm đóng</span>'}
                    </td>
                    <td><strong>${escapeHtml(formatNumberVi(row.quantity))}</strong></td>
                    <td>${escapeHtml(formatNumberVi(row.stockInCount))}</td>
                    <td>${escapeHtml(formatNumberVi(row.variantCount))}</td>
                    <td>${escapeHtml(row.lastStockInAt ? formatDateTimeVi(row.lastStockInAt) : "—")}</td>
                </tr>
            `;

        }).join("") || adminEmptyRow(5, "Chưa có chi nhánh nào.");

    }


    function periodText(fromDate, toDate) {

        if (fromDate && toDate) {
            return formatDateVi(fromDate) + " – " + formatDateVi(toDate);
        }

        if (fromDate) {
            return "Từ " + formatDateVi(fromDate);
        }

        if (toDate) {
            return "Đến " + formatDateVi(toDate);
        }

        return "Toàn bộ thời gian";

    }


    function formatNumberVi(value) {

        return Number(value || 0).toLocaleString("vi-VN");

    }


    function showStockInError(error) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        errorBox.textContent = getErrorMessage(error);

        errorBox.hidden = false;

    }

}
