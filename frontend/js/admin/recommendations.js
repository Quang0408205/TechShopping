/* ================= GỢI Ý SẢN PHẨM (admin/recommendations.html) ================= */

/*
 * API thật (chỉ ADMIN):
 *   GET  /admin/recommendations/overview?days=   { aiAvailable, model, evaluation, days, online[] }
 *   POST /admin/recommendations/train             huấn luyện lại ngay (vài giây), trả số liệu mô hình mới
 *   POST /admin/recommendations/evaluation        chạy lại đánh giá offline ở nền (vài phút: tinh chỉnh + kiểm thử)
 * model / evaluation lấy từ AI Service (null khi AI ngừng); evaluation.history = các lần đánh giá trước (bảng
 * recommendation_evaluations); online lấy từ database (bảng recommendations).
 * Đang đánh giá thì tải lại tổng quan mỗi 3 giây đến khi xong.
 */

const REC_LIST_LABELS = {
    PERSONAL: "Gợi ý dành cho bạn",
    SIMILAR: "Sản phẩm tương tự",
    UPSELL: "Có thể nâng cấp lên",
    CROSS_SELL: "Thường được mua kèm"
};

const REC_POLL_MS = 3000;

document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const trainButton = document.getElementById("recTrainBtn");

    const evaluateButton = document.getElementById("recEvaluateBtn");

    const daysSelect = document.getElementById("recDays");

    let pollTimer = null;


    trainButton.addEventListener("click", train);

    evaluateButton.addEventListener("click", startEvaluation);

    daysSelect.addEventListener("change", load);

    await load();


    async function load() {

        clearTimeout(pollTimer);

        let overview;

        try {

            overview = await apiRequest("/admin/recommendations/overview?days=" + encodeURIComponent(daysSelect.value),
                { auth: true });

        } catch (error) {

            const message = escapeHtml(getErrorMessage(error));
            document.getElementById("recOnlineBody").innerHTML = `<tr><td colspan="7">${message}</td></tr>`;
            document.getElementById("recEvalBody").innerHTML = `<tr><td colspan="7">${message}</td></tr>`;
            document.getElementById("recEvalMeta").textContent = "";
            return;

        }

        document.getElementById("recAiDown").hidden = overview.aiAvailable;

        trainButton.disabled = !overview.aiAvailable;

        renderModel(overview.model);

        renderEvaluation(overview.aiAvailable ? overview.evaluation : null, overview.aiAvailable);

        renderHistory(overview.aiAvailable && overview.evaluation ? overview.evaluation.history || [] : []);

        renderOnline(overview.online);

        if (overview.evaluation && overview.evaluation.running) {
            pollTimer = setTimeout(load, REC_POLL_MS);
        }

    }


    /* ---------- mô hình đang dùng ---------- */

    function renderModel(model) {

        const grid = document.getElementById("recModelStats");

        if (!model) {
            grid.innerHTML = "";
            return;
        }

        grid.innerHTML = adminStatCardsHtml([
            { label: "Huấn luyện lúc", value: formatDateTimeVi(model.trainedAt) || "—", sub: "Mất " + model.seconds + " giây" },
            { label: "Sản phẩm", value: formatNumber(model.products), sub: "Vector " + model.features + " chiều" },
            { label: "Khách có hành vi", value: formatNumber(model.users), sub: "Học bởi ALS" },
            { label: "Cặp khách – sản phẩm", value: formatNumber(model.interactions), sub: "Ô khác 0 của ma trận r(u,i)" },
            { label: "Có \"mua kèm\"", value: formatNumber(model.co_purchase_items), sub: "Sản phẩm có cặp lift > 1" }
        ]);

    }


    async function train() {

        trainButton.disabled = true;

        trainButton.textContent = "ĐANG HUẤN LUYỆN…";

        try {

            await apiRequest("/admin/recommendations/train", { method: "POST", auth: true });

            showToast("Đã huấn luyện lại mô hình gợi ý.", "success");

        } catch (error) {

            showToast(getErrorMessage(error), "error");

        } finally {

            trainButton.textContent = "HUẤN LUYỆN LẠI";

            await load();

        }

    }


    /* ---------- đánh giá offline ---------- */

    function renderEvaluation(evaluation, aiAvailable) {

        const meta = document.getElementById("recEvalMeta");

        const body = document.getElementById("recEvalBody");

        const chart = document.getElementById("recAlphaChart");

        const running = !!(evaluation && evaluation.running);

        evaluateButton.disabled = !aiAvailable || running;

        evaluateButton.textContent = running ? "ĐANG ĐÁNH GIÁ…" : "ĐÁNH GIÁ LẠI";

        const result = evaluation && evaluation.result;

        if (!result) {

            meta.textContent = !aiAvailable ? "Không có dữ liệu (AI Service không trả lời)."
                : running ? "Đang đánh giá lần đầu…" : "Chưa có kết quả. Bấm \"Đánh giá lại\" để chạy.";
            body.innerHTML = '<tr><td colspan="7">—</td></tr>';
            chart.innerHTML = "";
            document.getElementById("recEvalExtra").innerHTML = "";
            return;

        }

        const n = result.n;

        [["recColPrecision", "Precision@"], ["recColRecall", "Recall@"], ["recColNdcg", "NDCG@"], ["recColHit", "HitRate@"]]
            .forEach(function (column) {
                document.getElementById(column[0]).textContent = column[1] + n;
            });

        if (result.validationRows) {
            document.getElementById("recColNdcg").textContent += " (KTC 95%)";
        }

        const validationRows = result.validationRows || result.rows;

        document.getElementById("recAlphaChartTitle").textContent =
            "NDCG@" + n + " theo α" + (result.validationRows ? " (tập validation)" : "");

        const failed = evaluation.error ? " Lần chạy gần nhất lỗi: " + evaluation.error + "." : "";

        meta.textContent = result.validationRows
            ? "Lúc " + (formatDateTimeVi(result.evaluatedAt) || result.evaluatedAt) + ". Hành vi mỗi khách chia theo thời gian: " +
              formatPercent(result.trainRatio) + " đầu để học (" + formatNumber(result.trainEvents) + " lượt), " +
              formatPercent(result.validationRatio) + " tiếp theo để chọn α (" + formatNumber(result.validationEvents) +
              " lượt, " + formatNumber(result.validationUsers) + " khách), " + formatPercent(result.testRatio) +
              " cuối để kiểm tra (" + formatNumber(result.testEvents) + " lượt). α chọn trên validation: " +
              formatAlpha(result.bestAlpha) + ", đang dùng: " + formatAlpha(result.configuredAlpha) + ". Bảng dưới là " +
              "kết quả trên tập kiểm tra (" + formatNumber(result.users) + " khách, danh sách " + n + " sản phẩm), " +
              "khoảng tin cậy 95% bootstrap " + formatNumber(result.bootstrapSamples) + " lần." + failed
            : "Lúc " + (formatDateTimeVi(result.evaluatedAt) || result.evaluatedAt) + ": " + formatNumber(result.users) +
              " khách, danh sách " + n + " sản phẩm (cách đánh giá cũ, bấm \"Đánh giá lại\")." + failed;

        /* λ của MMR: 1 (hoặc lần đánh giá cũ chưa có MMR) = không xếp lại, dòng tương ứng có mmrLambda null */
        const mmrOf = function (params) {
            return params && params.mmrLambda != null && params.mmrLambda < 1 ? Number(params.mmrLambda) : null;
        };

        const matches = function (row, alpha, mmrLambda) {
            return row.alpha !== null && Number(row.alpha) === Number(alpha) &&
                (row.mmrLambda == null ? null : Number(row.mmrLambda)) === mmrLambda;
        };

        body.innerHTML = result.rows.map(function (row) {

            const tags = [];

            const ci = row.ci && row.ci.ndcg;

            if (matches(row, result.bestAlpha, null)) {
                tags.push('<span class="admin-badge admin-badge-success">α đã chọn</span>');
            }

            if (row.mmrLambda != null && matches(row, result.bestAlpha, mmrOf(result.chosen))) {
                tags.push('<span class="admin-badge admin-badge-success">λ đã chọn</span>');
            }

            if (matches(row, result.configuredAlpha, mmrOf(result.configured))) {
                tags.push('<span class="admin-badge admin-badge-neutral">Đang dùng</span>');
            }

            return `
                <tr>
                    <td>${escapeHtml(row.model)} ${tags.join(" ")}</td>
                    <td class="admin-number">${formatMetric(row.precision)}</td>
                    <td class="admin-number">${formatMetric(row.recall)}</td>
                    <td class="admin-number">${formatMetric(row.ndcg)}${ci ? `<br><small>${formatMetric(ci[0])}–${formatMetric(ci[1])}</small>` : ""}</td>
                    <td class="admin-number">${formatPercent(row.hitRate)}</td>
                    <td class="admin-number">${formatPercent(row.coverage)}</td>
                    <td class="admin-number">${row.diversity == null ? "—" : formatMetric(row.diversity)}</td>
                </tr>
            `;

        }).join("");

        renderExtra(result);

        document.getElementById("recEvalCompare").innerHTML =
            comparisonText(result.comparisons) + comparisonText(result.mmrComparisons);

        const byAlpha = validationRows
            .filter(function (row) { return row.alpha !== null; })
            .sort(function (a, b) { return a.alpha - b.alpha; });

        const peak = Math.max.apply(null, byAlpha.map(function (row) { return row.ndcg; }));

        renderLineChart(chart, byAlpha.map(function (row) {
            return { label: formatAlpha(row.alpha), title: row.model, value: row.ndcg };
        }), { formatValue: formatMetric, maxValue: peak > 0 ? peak * 1.15 : 1 });

    }


    /*
     * Phần bổ sung của lần đánh giá: tham số chọn trên validation, chỉ tính thêm giỏ / mua, khách mới, mua kèm,
     * nâng cấp, tinh chỉnh ALS, nửa đời giảm trọng số và λ của MMR. Lần đánh giá cũ chưa có các phần này thì để trống.
     */

    function renderExtra(result) {

        const box = document.getElementById("recEvalExtra");

        if (!result.chosen) {
            box.innerHTML = "";
            return;
        }

        const n = result.n;

        const halfLife = function (days) { return days ? Math.round(days) + " ngày" : "không giảm"; };

        const mmrText = function (lambda) {
            return lambda == null || lambda >= 1 ? "không xếp lại" : formatAlpha(lambda);
        };

        const paramRow = function (label, p) {
            return [label, formatAlpha(p.alpha), String(p.factors), String(p.regularization).replace(".", ","),
                String(p.confidence).replace(".", ","), halfLife(p.halfLifeDays), mmrText(p.mmrLambda),
                p.coldStartK == null ? "–" : String(p.coldStartK)];
        };

        const metricRows = function (rows) {
            return rows.map(function (row) {
                const ci = row.ci && row.ci.ndcg;
                return [row.model, formatMetric(row.precision), formatMetric(row.recall),
                    formatMetric(row.ndcg) + (ci ? " (" + formatMetric(ci[0]) + "–" + formatMetric(ci[1]) + ")" : ""),
                    formatPercent(row.hitRate)];
            });
        };

        const metricHead = ["Mô hình", "Precision@" + n, "Recall@" + n, "NDCG@" + n + " (KTC 95%)", "HitRate@" + n];

        const cold = result.coldStartRows || [];

        const coldAllZero = cold.length > 0 && cold.every(function (row) { return row.ndcg === 0; });

        const cross = result.crossSell;

        const coldCurve = result.coldStartCurve || [];

        const likeRows = result.likeRows || [];

        const upsell = result.upsell;

        const tuning = (result.tuning.als || []).slice().sort(function (a, b) { return b.ndcg - a.ndcg; }).slice(0, 5);

        const mmrTuning = result.tuning.mmr || [];

        const keepShare = 1 - (result.tuning.mmrMaxLoss || 0);

        box.innerHTML =
            subHeading("Tham số") +
            miniTable(["", "α", "Số chiều ALS", "λ", "a", "Nửa đời", "λ của MMR", "Ngưỡng k"],
                [paramRow("Chọn trên validation", result.chosen), paramRow("Đang dùng", result.configured)]) +

            subHeading("Chỉ tính thêm giỏ / mua là đúng", formatNumber(result.strongUsers) + " khách") +
            miniTable(metricHead, metricRows(result.strongRows)) +
            comparisonText(result.strongComparisons) +

            subHeading("Khách mới (1–" + (result.coldStartK - 1) + " lượt, chỉ dùng CBF)", formatNumber(result.coldUsers) + " khách") +
            miniTable(metricHead, metricRows(cold)) +
            (coldAllZero ? '<p class="admin-field-hint">Không mô hình nào đoán trúng: các khách này chỉ có vài lượt mua ' +
                "ngẫu nhiên trong dữ liệu mẫu, nên chưa đánh giá được trên dữ liệu hiện có.</p>" : comparisonText(result.coldStartComparisons)) +

            (coldCurve.length === 0 ? "" :
                subHeading("Cold-start: NDCG theo số lượt của khách mới", "kiểm thử") +
                '<p class="admin-field-hint">Cắt lịch sử của một phần khách còn m lượt đầu rồi học lại; Hybrid được ' +
                "dùng ALS ngay từ lượt đầu để so với chỉ CBF. Ngưỡng k chọn trên validation: " +
                escapeHtml(String(result.chosen.coldStartK)) + " lượt.</p>" +
                miniTable(["Số lượt", "Khách", "Phổ biến", "Chỉ CBF", "Hybrid"], coldCurve.map(function (row) {
                    return [String(row.events), formatNumber(row.users), formatMetric(row.popular),
                        formatMetric(row.cbf), formatMetric(row.hybrid)];
                }))) +

            (likeRows.length === 0 ? "" :
                subHeading("Danh sách yêu thích (LIKE): có so với bỏ", formatNumber(result.likeEvents) + " lượt LIKE") +
                miniTable(metricHead, metricRows(likeRows)) +
                comparisonText(result.likeComparisons)) +

            subHeading("Mua kèm", formatNumber(cross.pairs) + " cặp đơn – sản phẩm") +
            '<p class="admin-field-hint">Luật cùng đơn và luật “khách mua A cũng mua B” học từ ' + formatNumber(cross.trainOrders) + " đơn đặt trước và hành vi trước đó, chấm trên " +
            formatNumber(cross.testOrders) + " đơn sau; " + formatPercent(cross.withSuggestions) +
            " sản phẩm trong đơn có danh sách mua kèm.</p>" +
            miniTable(["Cách gợi ý", "HitRate@" + cross.n, "Precision@" + cross.n], cross.rows.map(function (row) {
                return [row.model, formatPercent(row.hitRate), formatMetric(row.precision)];
            })) +

            subHeading("Nâng cấp") +
            '<p class="admin-field-hint">' + formatPercent(upsell.withSuggestions) + " trong " + formatNumber(upsell.products) +
            " sản phẩm đang bán có gợi ý nâng cấp; gợi ý đầu tiên đắt hơn trung bình " + formatPercent(upsell.meanPriceIncrease) + ".</p>" +

            subHeading("Tinh chỉnh ALS trên validation", "5 bộ tốt nhất / " + formatNumber((result.tuning.als || []).length)) +
            miniTable(["Số chiều", "λ", "a", "NDCG@" + n + " (chỉ ALS)"], tuning.map(function (row) {
                return [String(row.factors), String(row.regularization).replace(".", ","),
                    String(row.confidence).replace(".", ","), formatMetric(row.ndcg)];
            })) +
            miniTable(["Nửa đời giảm trọng số", "NDCG@" + n + " (α đã chọn)"], (result.tuning.decay || []).map(function (row) {
                return [halfLife(row.halfLifeDays), formatMetric(row.ndcg)];
            })) +

            (mmrTuning.length === 0 ? "" :
                subHeading("Xếp lại cho đa dạng (MMR) trên validation", "λ = 1: không xếp lại") +
                '<p class="admin-field-hint">Chọn λ có độ đa dạng cao nhất mà NDCG vẫn giữ được ít nhất ' +
                formatPercent(keepShare) + " so với không xếp lại.</p>" +
                miniTable(["λ", "NDCG@" + n, "Đa dạng"], mmrTuning.map(function (row) {
                    return [formatAlpha(row.lambda) + (row.lambda === result.chosen.mmrLambda ? " (đã chọn)" : ""),
                        formatMetric(row.ndcg), formatMetric(row.diversity)];
                })));

    }

    function subHeading(title, note) {
        return `<div class="admin-panel-header"><h3>${escapeHtml(title)}</h3>` +
            (note ? `<span class="admin-row-count">${escapeHtml(note)}</span>` : "") + "</div>";
    }

    /* cells: chuỗi thuần (được escape); cột 2 trở đi canh phải như số */
    function miniTable(head, rows) {
        return '<div class="admin-table-wrapper"><table class="admin-table"><thead><tr>' +
            head.map(function (h, i) { return `<th${i ? ' class="admin-number"' : ""}>${escapeHtml(h)}</th>`; }).join("") +
            "</tr></thead><tbody>" +
            rows.map(function (cells) {
                return "<tr>" + cells.map(function (c, i) {
                    return `<td${i ? ' class="admin-number"' : ""}>${escapeHtml(c)}</td>`;
                }).join("") + "</tr>";
            }).join("") +
            "</tbody></table></div>";
    }

    function comparisonText(comparisons) {
        if (!comparisons || comparisons.length === 0) {
            return "";
        }
        return '<p class="admin-field-hint">' + comparisons.map(function (c) {
            const sure = c.low > 0 || c.high < 0;
            const metric = c.metric === "diversity" ? "độ đa dạng" : "NDCG";
            return `${escapeHtml(c.model)} so với ${escapeHtml(c.versus)}: ${metric} ${formatSigned(c.diff)} ` +
                `(KTC 95%: ${formatSigned(c.low)} đến ${formatSigned(c.high)}) · ` +
                (sure ? "<strong>chênh lệch có ý nghĩa</strong>" : "chưa đủ chắc chắn");
        }).join("<br>") + "</p>";
    }


    /* Mỗi dòng một lần đánh giá: NDCG@N của từng mô hình, so được giữa các lần (dữ liệu, α thay đổi) */

    function renderHistory(history) {

        const body = document.getElementById("recHistoryBody");

        document.getElementById("recHistoryCount").textContent = history.length ? history.length + " lần gần nhất · NDCG" : "";

        if (history.length === 0) {
            body.innerHTML = '<tr><td colspan="9">Chưa có lần đánh giá nào được lưu.</td></tr>';
            return;
        }

        body.innerHTML = history.map(function (run) {

            const best = run.bestAlpha === null ? "—" : formatMetric(run.bestNdcg) + " (α = " + formatAlpha(run.bestAlpha) + ")" +
                (run.bestNdcgLow === null ? "" : " · " + formatMetric(run.bestNdcgLow) + "–" + formatMetric(run.bestNdcgHigh));

            const mmr = run.mmrNdcg == null ? "—" : formatMetric(run.mmrNdcg) + " (λ = " + formatAlpha(run.mmrLambda) + ")" +
                (run.mmrDiversity == null ? "" : " · đa dạng " + formatMetric(run.bestDiversity) + " → " + formatMetric(run.mmrDiversity));

            return `
                <tr>
                    <td>${escapeHtml(formatDateTimeVi(run.evaluatedAt) || run.evaluatedAt)}</td>
                    <td class="admin-number">${formatNumber(run.users)}</td>
                    <td class="admin-number">${run.configuredNdcg === null ? "—" : formatMetric(run.configuredNdcg) + " (α = " + formatAlpha(run.configuredAlpha) + ")"}</td>
                    <td class="admin-number">${escapeHtml(best)}</td>
                    <td class="admin-number">${escapeHtml(mmr)}</td>
                    <td class="admin-number">${formatMetric(run.cbfNdcg)}</td>
                    <td class="admin-number">${formatMetric(run.cfNdcg)}</td>
                    <td class="admin-number">${run.knnNdcg == null ? "—" : formatMetric(run.knnNdcg)}</td>
                    <td class="admin-number">${formatMetric(run.popularNdcg)}</td>
                </tr>
            `;

        }).join("");

    }


    async function startEvaluation() {

        evaluateButton.disabled = true;

        try {

            await apiRequest("/admin/recommendations/evaluation", { method: "POST", auth: true });

            showToast("Đang đánh giá lại (tinh chỉnh + kiểm thử), kết quả hiện sau vài phút.", "success");

        } catch (error) {

            showToast(getErrorMessage(error), "error");

        }

        await load();

    }


    /* ---------- hiệu quả trên web ---------- */

    function renderOnline(online) {

        const body = document.getElementById("recOnlineBody");

        const total = online.reduce(function (sum, row) {
            return {
                impressions: sum.impressions + row.impressions,
                clicks: sum.clicks + row.clicks,
                converted: sum.converted + row.converted
            };
        }, { impressions: 0, clicks: 0, converted: 0 });

        const rows = online.map(function (row) {
            return `
                <tr>
                    <td>${escapeHtml(REC_LIST_LABELS[row.type] || row.type)}</td>
                    <td class="admin-number">${formatNumber(row.impressions)}</td>
                    <td class="admin-number">${formatNumber(row.clicks)}</td>
                    <td class="admin-number">${row.clickRate === null ? "—" : formatPercent(row.clickRate)}</td>
                    <td class="admin-number">${formatNumber(row.converted)}</td>
                    <td class="admin-number">${row.conversionRate === null ? "—" : formatPercent(row.conversionRate)}</td>
                    <td class="admin-number">${formatNumber(row.users)}</td>
                </tr>
            `;
        });

        rows.push(`
            <tr>
                <td><strong>Tổng</strong></td>
                <td class="admin-number"><strong>${formatNumber(total.impressions)}</strong></td>
                <td class="admin-number"><strong>${formatNumber(total.clicks)}</strong></td>
                <td class="admin-number"><strong>${total.impressions ? formatPercent(total.clicks / total.impressions) : "—"}</strong></td>
                <td class="admin-number"><strong>${formatNumber(total.converted)}</strong></td>
                <td class="admin-number"><strong>${total.clicks ? formatPercent(total.converted / total.clicks) : "—"}</strong></td>
                <td class="admin-number"></td>
            </tr>
        `);

        body.innerHTML = rows.join("");

        const chart = document.getElementById("recClickChart");

        if (total.clicks === 0) {
            chart.innerHTML = '<p class="admin-chart-empty">' + (total.impressions === 0
                ? "Chưa có lượt hiển thị gợi ý nào trong khoảng này."
                : "Chưa có lượt bấm vào gợi ý nào trong khoảng này.") + "</p>";
            return;
        }

        const rates = online.map(function (row) { return row.clickRate === null ? 0 : row.clickRate; });

        renderBarChart(chart, online.map(function (row, index) {
            return { label: REC_LIST_LABELS[row.type] || row.type, value: rates[index] };
        }), { formatValue: formatPercent, maxValue: Math.max.apply(null, rates) * 1.15 });

    }


    /* ---------- định dạng ---------- */

    function formatNumber(value) {
        return new Intl.NumberFormat("vi-VN").format(Number(value) || 0);
    }

    function formatMetric(value) {
        return Number(value).toLocaleString("vi-VN", { minimumFractionDigits: 4, maximumFractionDigits: 4 });
    }

    function formatPercent(value) {
        return (Number(value) * 100).toLocaleString("vi-VN", { maximumFractionDigits: 1 }) + "%";
    }

    function formatSigned(value) {
        return (value > 0 ? "+" : "") + formatMetric(value);
    }

    function formatAlpha(value) {
        return value === null || value === undefined ? "—"
            : Number(value).toLocaleString("vi-VN", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
    }

});
