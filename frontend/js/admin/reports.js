/* ================= BÁO CÁO DOANH THU (admin/reports.html) ================= */

/*
 * Dữ liệu THẬT. Quản lý chi nhánh (khoá vào chi nhánh mình) và ADMIN (mọi chi nhánh, chọn từ /admin/stores):
 *   GET /admin/reports/sales?storeId&fromDate&toDate&groupBy           số tổng, chuỗi theo ngày / tháng, danh mục,
 *                                                                      thanh toán, chi nhánh, nhân viên, top sản phẩm
 *   GET /admin/reports/sales/export?… (cùng bộ lọc)                    file Excel .xlsx (tải bằng fetch có token)
 * Doanh thu = đơn đã giao (theo ngày giao, gồm phí giao hàng); tiền hoàn = trả hàng đã hoàn tiền (theo ngày hoàn).
 * Mặc định 30 ngày gần nhất; tối đa 366 ngày; theo ngày tối đa 62 ngày (lỗi 400 hiện dưới bộ lọc).
 *
 * Phase 7.10, chỉ ADMIN, bộ lọc riêng (chi nhánh thật + khoảng ngày):
 *   GET /admin/inventory/stock-in-stats?storeId&fromDate&toDate
 *   → tổng số lượng / số lần nhập / số phiên bản / số nhà cung cấp + từng chi nhánh.
 */

const PAYMENT_METHOD_LABELS = {
    COD: "Thanh toán khi nhận hàng",
    BANK_TRANSFER: "Chuyển khoản",
    INSTALLMENT: "Trả góp"
};


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

        const groupByFilter = document.getElementById("groupByFilter");

        const dateError = document.getElementById("reportDateError");

        const exportButton = document.getElementById("exportExcelBtn");

        let requestCounter = 0;


        try {

            await setupStoreFilter(storeFilter, staff);

        } catch (error) {

            showReportError(error);

        }

        [storeFilter, fromDateFilter, toDateFilter, groupByFilter].forEach(function (input) {
            input.addEventListener("change", render);
        });


        document.getElementById("clearReportFiltersBtn")
            .addEventListener("click", function () {

                fromDateFilter.value = "";

                toDateFilter.value = "";

                groupByFilter.value = "";

                if (isAdmin) {
                    storeFilter.value = "";
                }

                render();

            });

        exportButton.addEventListener("click", exportExcel);


        render();

        /* Phase 7.10: hàng nhập kho (dữ liệu thật), chỉ ADMIN */
        if (isAdmin) {
            setupStockInStats();
        }


        /* Bộ lọc → query string (quản lý chi nhánh không gửi storeId: backend tự dùng chi nhánh của họ) */

        function filterParams() {

            const params = new URLSearchParams();

            if (isAdmin && storeFilter.value) {
                params.set("storeId", storeFilter.value);
            }

            if (fromDateFilter.value) {
                params.set("fromDate", fromDateFilter.value);
            }

            if (toDateFilter.value) {
                params.set("toDate", toDateFilter.value);
            }

            if (groupByFilter.value) {
                params.set("groupBy", groupByFilter.value);
            }

            return params;

        }


        async function render() {

            if (fromDateFilter.value && toDateFilter.value && fromDateFilter.value > toDateFilter.value) {

                dateError.textContent = "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.";

                dateError.hidden = false;

                return;

            }

            dateError.hidden = true;


            const requestId = ++requestCounter;

            document.getElementById("reportStatGrid").innerHTML = '<span class="admin-skeleton admin-skeleton--card"></span>'.repeat(4);


            let report;

            try {

                report = await apiRequest("/admin/reports/sales?" + filterParams().toString(), { auth: true });

            } catch (error) {

                if (requestId === requestCounter) {
                    document.getElementById("reportStatGrid").innerHTML = "";
                    showReportError(error);
                }

                return;

            }

            if (requestId !== requestCounter) {
                return;
            }


            renderReport(report);

        }


        function renderReport(report) {

            const s = report.summary;

            const empty = s.deliveredOrders === 0 && Number(s.refundAmount) === 0;

            const monthly = report.groupBy === "MONTH";

            document.getElementById("reportPeriodLabel").textContent =
                formatDateVi(report.fromDate) + " – " + formatDateVi(report.toDate) + " · theo " + (monthly ? "tháng" : "ngày") +
                (report.storeName ? " · " + report.storeName : " · tất cả chi nhánh");

            document.getElementById("reportEmpty").hidden = !empty;


            document.getElementById("reportStatGrid").innerHTML = adminStatCardsHtml([
                { label: "Doanh thu thuần", value: formatPrice(s.netRevenue), sub: "Gộp " + formatPrice(s.grossRevenue) + " − hoàn " + formatPrice(s.refundAmount) },
                { label: "Đơn đã giao", value: String(s.deliveredOrders), sub: s.unitsSold + " sản phẩm · TB " + formatPrice(s.averageOrderValue) + " / đơn" },
                { label: "Tiền hoàn", value: formatPrice(s.refundAmount), sub: s.refundCount + " lần hoàn tiền" },
                { label: "Phí giao hàng", value: formatPrice(s.shippingFees), sub: "Đã tính trong doanh thu gộp" },
                { label: "Hoa hồng nhân viên", value: formatPrice(s.totalCommission), sub: "Theo đơn đã giao trong kỳ" }
            ]);


            const series = report.series.map(function (p) {
                return {
                    label: monthly ? formatMonthLabel(String(p.start).slice(0, 7), true) : Number(String(p.start).slice(8, 10)) + "/" + Number(String(p.start).slice(5, 7)),
                    title: monthly ? formatMonthLabel(String(p.start).slice(0, 7)) : formatDateVi(p.start),
                    value: Number(p.netRevenue)
                };
            });

            if (empty) {
                document.getElementById("reportRevenueChart").innerHTML = '<p class="admin-chart-empty">Chưa có đơn đã giao trong khoảng này.</p>';
            } else {
                renderBarChart(document.getElementById("reportRevenueChart"), series, {
                    formatValue: formatPrice,
                    minSlotWidth: series.length > 20 ? 22 : 74,
                    labelEvery: series.length > 20 ? Math.ceil(series.length / 12) : 1
                });
            }


            renderPieChart(document.getElementById("reportCategoryChart"), report.byCategory.map(function (row) {
                return { label: row.categoryName || "Chưa phân loại", value: Number(row.revenue) };
            }), { formatValue: formatPrice, emptyText: "Chưa có đơn đã giao trong khoảng này.", ariaLabel: "Doanh thu theo danh mục" });

            renderPieChart(document.getElementById("reportPaymentChart"), report.byPaymentMethod.map(function (row) {
                return { label: PAYMENT_METHOD_LABELS[row.paymentMethod] || row.paymentMethod, value: Number(row.revenue) };
            }), { formatValue: formatPrice, emptyText: "Chưa có đơn đã giao trong khoảng này.", ariaLabel: "Doanh thu theo phương thức thanh toán" });


            /* Theo chi nhánh: chỉ ADMIN khi xem tất cả chi nhánh */
            const storePanel = document.getElementById("reportStoreChartPanel");

            storePanel.hidden = !(isAdmin && !report.storeId);

            if (!storePanel.hidden) {

                renderBarChart(document.getElementById("reportStoreChart"), report.byStore.map(function (row) {
                    return { label: truncateText(row.storeName, 16), title: row.storeName, value: Number(row.netRevenue) };
                }), { formatValue: formatPrice, color: cssVar("--color-accent", "#8a5a22") });

                document.querySelector("#reportStoreTable tbody").innerHTML = report.byStore.map(function (row) {
                    return `
                        <tr>
                            <td>${escapeHtml(row.storeName)}${row.storeActive ? "" : ' <span class="admin-subtext">(tạm đóng)</span>'}</td>
                            <td class="admin-number">${row.orders}</td>
                            <td class="admin-number">${escapeHtml(formatPrice(row.grossRevenue))}</td>
                            <td class="admin-number">${escapeHtml(formatPrice(row.refundAmount))}</td>
                            <td class="admin-number">${escapeHtml(formatPrice(row.netRevenue))}</td>
                        </tr>
                    `;
                }).join("") || adminEmptyRow(5, "Chưa có chi nhánh nào.");

            }


            renderBarChart(document.getElementById("reportEmployeeChart"), report.byEmployee
                .filter(function (row) { return row.employeeId !== null; })
                .map(function (row) {
                    return { label: truncateText(row.fullname, 14), title: row.fullname + " · " + row.storeName, value: Number(row.salesAmount) };
                }), { formatValue: formatPrice });

            document.querySelector("#reportEmployeeTable tbody").innerHTML = report.byEmployee.map(function (row) {
                return `
                    <tr>
                        <td>${row.employeeId === null ? '<span class="admin-subtext">Không gán nhân viên (đơn xác nhận trước Phase 10)</span>'
                            : escapeHtml(row.fullname) + (row.employeeCode ? ' <span class="admin-subtext">' + escapeHtml(row.employeeCode) + "</span>" : "")}</td>
                        <td>${escapeHtml(row.storeName || "—")}</td>
                        <td class="admin-number">${row.orders}</td>
                        <td class="admin-number">${escapeHtml(formatPrice(row.salesAmount))}</td>
                        <td class="admin-number">${escapeHtml(formatPrice(row.commission))}</td>
                        <td class="admin-number">${escapeHtml(formatPrice(row.refundAmount))}</td>
                    </tr>
                `;
            }).join("") || adminEmptyRow(6, "Chưa có đơn đã giao trong khoảng này.");


            document.querySelector("#reportTopProductsTable tbody").innerHTML = report.topProducts.map(function (row, i) {
                return `
                    <tr>
                        <td>${i + 1}</td>
                        <td>${escapeHtml(row.productName)}</td>
                        <td>${escapeHtml(row.categoryName || "—")}</td>
                        <td class="admin-number">${row.units}</td>
                        <td class="admin-number">${escapeHtml(formatPrice(row.revenue))}</td>
                    </tr>
                `;
            }).join("") || adminEmptyRow(5, "Chưa có đơn đã giao trong khoảng này.");

        }


        /* Tải file .xlsx: fetch có token (một thẻ <a href> không gửi được Authorization) */

        async function exportExcel() {

            exportButton.disabled = true;

            const label = exportButton.textContent;

            exportButton.textContent = "Đang tạo file…";

            try {

                const blob = await apiDownload("/admin/reports/sales/export?" + filterParams().toString());

                const from = fromDateFilter.value || "";

                const to = toDateFilter.value || "";

                const link = document.createElement("a");

                link.href = URL.createObjectURL(blob);

                link.download = "TechShopping_BaoCao" + (from ? "_" + from : "") + (to ? "_" + to : "") + ".xlsx";

                document.body.appendChild(link);

                link.click();

                link.remove();

                setTimeout(function () { URL.revokeObjectURL(link.href); }, 10000);

                showToast("Đã tạo file Excel.", "success");

            } catch (error) {

                showReportError(error);

            } finally {

                exportButton.disabled = false;

                exportButton.textContent = label;

            }

        }


        function showReportError(error) {

            if (error.status === 401) {
                requireStaffLogin();
            }

            const details = error.details && typeof error.details === "object"
                ? Object.keys(error.details).map(function (key) { return error.details[key]; }).join("; ")
                : "";

            dateError.textContent = details || getErrorMessage(error);

            dateError.hidden = false;

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
