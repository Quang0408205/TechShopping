/* ================= BIỂU ĐỒ CHO admin/ (SVG THUẦN, KHÔNG THƯ VIỆN) ================= */

/*
 * Lấy từ bản frontend tham chiếu: dự án không thêm thư viện ngoài nên biểu
 * đồ vẽ bằng SVG + JS thuần. Cột (so sánh hạng mục) và đường (xu hướng theo
 * thời gian). Rê chuột hiện giá trị nhờ thẻ <title> có sẵn của SVG.
 * Mọi nhãn đi qua escapeHtml (js/core/ui.js).
 *
 * Nạp trước script của trang có gọi renderBarChart / renderLineChart.
 */

function cssVar(name, fallback) {

    const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();

    return value || fallback;

}


/* data: [{ label, value, title? }] (title: tên đầy đủ cho tooltip) */

function renderBarChart(container, data, options) {

    if (!data || data.length === 0) {

        container.innerHTML = '<p class="admin-chart-empty">Chưa có dữ liệu để hiển thị.</p>';

        return;

    }


    const opts = options || {};

    const formatValue = opts.formatValue || function (v) { return String(v); };

    const barColor = opts.color || cssVar("--color-ink", "#111318");

    const height = opts.height || 220;

    const axisHeight = 34;

    const topPadding = 16;

    const minSlotWidth = 74;


    /*
     * Đo bề rộng thật của khung để làm viewBox: ít cột thì dàn đều, nhiều cột
     * thì giữ tối thiểu minSlotWidth và cho khung cuộn ngang.
     */

    const measuredWidth = container.clientWidth || 0;

    const slotWidth = Math.max(minSlotWidth, measuredWidth / data.length);

    const barWidth = Math.min(slotWidth * 0.5, 90);

    const width = Math.max(data.length * slotWidth, measuredWidth, 260);

    const maxValue = Math.max.apply(null, data.map(function (d) { return d.value; }).concat([1]));

    const availableHeight = height - axisHeight - topPadding;


    const bars = data.map(function (d, i) {

        const barHeight = Math.max(2, Math.round((d.value / maxValue) * availableHeight));

        const x = i * slotWidth + (slotWidth - barWidth) / 2;

        const y = height - axisHeight - barHeight;

        return `
            <g>
                <rect x="${x}" y="${y}" width="${barWidth}" height="${barHeight}" rx="4" fill="${escapeHtml(barColor)}" class="chart-bar">
                    <title>${escapeHtml(d.title || d.label)}: ${escapeHtml(formatValue(d.value))}</title>
                </rect>
                <text x="${x + barWidth / 2}" y="${height - axisHeight + 18}" text-anchor="middle" class="chart-axis-label">${escapeHtml(d.label)}</text>
            </g>
        `;

    }).join("");


    container.innerHTML = `
        <svg class="admin-chart" viewBox="0 0 ${width} ${height}" preserveAspectRatio="xMinYMid meet" role="img" aria-label="Biểu đồ cột">
            <line x1="0" y1="${height - axisHeight}" x2="${width}" y2="${height - axisHeight}" class="chart-axis-line"></line>
            ${bars}
        </svg>
    `;

}


/* data: [{ label, value }] theo thứ tự thời gian */

function renderLineChart(container, data, options) {

    if (!data || data.length === 0) {

        container.innerHTML = '<p class="admin-chart-empty">Chưa có dữ liệu để hiển thị.</p>';

        return;

    }


    const opts = options || {};

    const formatValue = opts.formatValue || function (v) { return String(v); };

    const lineColor = opts.color || cssVar("--color-accent", "#8a5a22");

    const height = opts.height || 220;

    const axisHeight = 34;

    const topPadding = 20;

    const sidePadding = 24;

    const minSlotWidth = 96;


    const measuredWidth = container.clientWidth || 0;

    const width = Math.max((data.length - 1) * minSlotWidth + sidePadding * 2, measuredWidth, 260);

    const maxValue = Math.max.apply(null, data.map(function (d) { return d.value; }).concat([1]));


    const points = data.map(function (d, i) {

        const x = data.length > 1
            ? sidePadding + i * ((width - sidePadding * 2) / (data.length - 1))
            : width / 2;

        const y = topPadding + (height - axisHeight - topPadding) * (1 - d.value / maxValue);

        return { x: x, y: y, d: d };

    });


    const pathD = points.map(function (p, i) {
        return (i === 0 ? "M" : "L") + p.x.toFixed(1) + " " + p.y.toFixed(1);
    }).join(" ");

    const areaD = pathD +
        " L " + points[points.length - 1].x.toFixed(1) + " " + (height - axisHeight) +
        " L " + points[0].x.toFixed(1) + " " + (height - axisHeight) + " Z";


    const dots = points.map(function (p) {

        return `
            <circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="4" fill="${escapeHtml(lineColor)}" class="chart-dot">
                <title>${escapeHtml(p.d.title || p.d.label)}: ${escapeHtml(formatValue(p.d.value))}</title>
            </circle>
            <text x="${p.x.toFixed(1)}" y="${height - axisHeight + 18}" text-anchor="middle" class="chart-axis-label">${escapeHtml(p.d.label)}</text>
        `;

    }).join("");


    container.innerHTML = `
        <svg class="admin-chart" viewBox="0 0 ${width} ${height}" preserveAspectRatio="xMinYMid meet" role="img" aria-label="Biểu đồ đường">
            <line x1="0" y1="${height - axisHeight}" x2="${width}" y2="${height - axisHeight}" class="chart-axis-line"></line>
            <path d="${areaD}" class="chart-area" fill="${escapeHtml(lineColor)}"></path>
            <path d="${pathD}" fill="none" stroke="${escapeHtml(lineColor)}" stroke-width="2.5"></path>
            ${dots}
        </svg>
    `;

}
