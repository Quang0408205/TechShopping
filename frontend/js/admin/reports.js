/* ================= BÁO CÁO DOANH THU (admin/reports.html) ================= */

/*
 * Quản lý chi nhánh (khoá chi nhánh mình) + ADMIN (mọi chi nhánh, có biểu
 * đồ so sánh khi xem tất cả). Lọc theo khoảng ngày. Dữ liệu mẫu (B1), nối
 * sales_records / v_sales_by_store thật ở Phase 7.
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
