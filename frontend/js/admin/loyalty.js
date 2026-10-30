/* ================= HẠNG THÀNH VIÊN (admin/loyalty.html) ================= */

/*
 * API thật (chỉ ADMIN; backend kiểm tra lại vai trò trong DB):
 *   GET /admin/loyalty/tiers     4 hạng, thấp → cao
 *   PUT /admin/loyalty/tiers     { tiers: [{ code, minSpent, pointMultiplier }] } — gửi đủ 4 hạng
 * Kiểm tra trước khi gửi (server kiểm lại): hạng đầu = 0, ngưỡng tăng dần, hệ số 1–5 (2 số lẻ).
 */

const TIER_EXAMPLE_ORDER = 20000000;

document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const form = document.getElementById("tierForm");

    const tbody = document.getElementById("tierTableBody");

    const errorBox = document.getElementById("tierFormError");

    const saveButton = document.getElementById("tierSaveBtn");

    let tiers = [];


    tbody.addEventListener("input", renderExamples);

    form.addEventListener("submit", save);

    await load();


    async function load() {

        try {

            tiers = await apiRequest("/admin/loyalty/tiers", { auth: true });

        } catch (error) {

            tbody.innerHTML = `<tr><td colspan="4">${escapeHtml(getErrorMessage(error))}</td></tr>`;

            return;

        }

        render();

    }


    function render() {

        tbody.innerHTML = tiers.map(function (tier, index) {

            return `
                <tr data-code="${escapeHtml(tier.code)}">
                    <td><strong>${escapeHtml(tier.name)}</strong></td>
                    <td>
                        <input type="number" class="tier-min" min="0" step="1000000" value="${escapeHtml(String(Math.round(Number(tier.minSpent))))}"
                            aria-label="Ngưỡng hạng ${escapeHtml(tier.name)}" ${index === 0 ? "readonly" : ""}>
                    </td>
                    <td>
                        <input type="number" class="tier-multiplier" min="1" max="5" step="0.1" value="${escapeHtml(String(Number(tier.pointMultiplier)))}"
                            aria-label="Hệ số hạng ${escapeHtml(tier.name)}">
                    </td>
                    <td class="tier-example"></td>
                </tr>
            `;

        }).join("");

        renderExamples();

    }


    function renderExamples() {

        tbody.querySelectorAll("tr[data-code]").forEach(function (row) {

            const multiplier = Number(row.querySelector(".tier-multiplier").value);

            const points = Number.isFinite(multiplier) ? Math.floor(Math.floor(TIER_EXAMPLE_ORDER / 10000) * multiplier) : 0;

            row.querySelector(".tier-example").textContent = "+" + new Intl.NumberFormat("vi-VN").format(points) + " điểm";

        });

    }


    async function save(event) {

        event.preventDefault();

        errorBox.hidden = true;

        const rows = [...tbody.querySelectorAll("tr[data-code]")].map(function (row) {
            return {
                code: row.dataset.code,
                name: row.querySelector("strong").textContent,
                minSpent: row.querySelector(".tier-min").value.trim(),
                pointMultiplier: row.querySelector(".tier-multiplier").value.trim()
            };
        });

        const problem = check(rows);

        if (problem) {
            showError(problem);
            return;
        }

        saveButton.disabled = true;

        try {

            tiers = await apiRequest("/admin/loyalty/tiers", {
                method: "PUT",
                auth: true,
                body: { tiers: rows.map(function (row) {
                    return { code: row.code, minSpent: Number(row.minSpent), pointMultiplier: Number(row.pointMultiplier) };
                }) }
            });

            render();

            showToast("Đã lưu hạng thành viên.", "success");

        } catch (error) {

            showError(error.details ? Object.values(error.details).join(" ") : getErrorMessage(error));

        } finally {

            saveButton.disabled = false;

        }

    }


    function check(rows) {

        for (let i = 0; i < rows.length; i++) {

            const min = Number(rows[i].minSpent);

            const multiplier = Number(rows[i].pointMultiplier);

            if (rows[i].minSpent === "" || !Number.isInteger(min) || min < 0) {
                return "Ngưỡng hạng " + rows[i].name + " phải là số đồng nguyên, không âm.";
            }

            if (i === 0 && min !== 0) {
                return "Hạng " + rows[i].name + " là hạng khởi đầu, ngưỡng phải là 0.";
            }

            if (i > 0 && min <= Number(rows[i - 1].minSpent)) {
                return "Ngưỡng hạng " + rows[i].name + " phải cao hơn hạng " + rows[i - 1].name + ".";
            }

            if (rows[i].pointMultiplier === "" || !(multiplier >= 1 && multiplier <= 5) || Math.round(multiplier * 100) !== multiplier * 100) {
                return "Hệ số hạng " + rows[i].name + " từ 1 đến 5, tối đa 2 chữ số thập phân.";
            }

        }

        return "";

    }


    function showError(message) {

        errorBox.textContent = message;

        errorBox.hidden = false;

    }

});
