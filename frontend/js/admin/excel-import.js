/* ================= NHẬP EXCEL (dùng chung: Sản phẩm + Tồn kho) ================= */

/*
 * mountExcelImport(container, options) vẽ khối: TẢI FILE MẪU, chọn file .xlsx, KIỂM TRA (dryRun=true, không ghi gì),
 * NHẬP VÀO HỆ THỐNG (dryRun=false, chỉ bật khi lần kiểm tra không có lỗi). Backend kiểm tra hết mọi dòng; có lỗi thì
 * không ghi gì, trả danh sách lỗi theo sheet / dòng / cột.
 * options: { hint, templatePath, templateName, importPath() → path hoặc null (+ options.missingTarget),
 *            labels: { key: "nhãn" } cho summary, onApplied() }
 */

function mountExcelImport(container, options) {

    container.innerHTML = `
        <div class="excel-import">
            <p class="admin-field-hint">${escapeHtml(options.hint)}</p>
            <div class="excel-import-actions">
                <button type="button" class="btn btn-outline-dark" data-excel="template">TẢI FILE MẪU</button>
                <label class="btn btn-outline-dark excel-import-file">
                    CHỌN FILE .XLSX
                    <input type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" data-excel="file">
                </label>
                <span class="excel-import-name" data-excel="name">Chưa chọn file</span>
                <button type="button" class="btn btn-dark" data-excel="check" disabled>KIỂM TRA</button>
                <button type="button" class="btn btn-dark" data-excel="apply" disabled>NHẬP VÀO HỆ THỐNG</button>
            </div>
            <div class="excel-import-result" data-excel="result" aria-live="polite"></div>
        </div>
    `;

    const part = function (name) {
        return container.querySelector('[data-excel="' + name + '"]');
    };

    const fileInput = part("file");

    const checkButton = part("check");

    const applyButton = part("apply");

    const result = part("result");

    let file = null;

    let busy = false;


    part("template").addEventListener("click", async function () {

        try {

            const blob = await apiDownload(options.templatePath);

            const link = document.createElement("a");

            link.href = URL.createObjectURL(blob);

            link.download = options.templateName;

            document.body.appendChild(link);

            link.click();

            link.remove();

            setTimeout(function () { URL.revokeObjectURL(link.href); }, 10000);

        } catch (error) {

            showToast(getErrorMessage(error), "error");

        }

    });

    fileInput.addEventListener("change", function () {

        file = fileInput.files[0] || null;

        fileInput.value = "";

        part("name").textContent = file ? file.name : "Chưa chọn file";

        applyButton.disabled = true;

        checkButton.disabled = !file;

        result.innerHTML = "";

    });

    checkButton.addEventListener("click", function () {
        run(true);
    });

    applyButton.addEventListener("click", function () {
        run(false);
    });


    async function run(dryRun) {

        if (!file || busy) {
            return;
        }

        const path = options.importPath();

        if (!path) {
            result.innerHTML = `<p class="form-error">${escapeHtml(options.missingTarget || "Chưa chọn nơi nhập.")}</p>`;
            return;
        }

        busy = true;

        checkButton.disabled = true;

        applyButton.disabled = true;

        result.innerHTML = `<p class="admin-field-hint">${dryRun ? "Đang kiểm tra file…" : "Đang nhập…"}</p>`;

        try {

            const formData = new FormData();

            formData.append("file", file);

            const response = await apiRequest(path + (path.includes("?") ? "&" : "?") + "dryRun=" + dryRun,
                { method: "POST", body: formData, auth: true });

            render(response);

            if (response.applied) {

                showToast("Đã nhập xong file " + file.name + ".", "success");

                file = null;

                part("name").textContent = "Chưa chọn file";

                options.onApplied();

            } else {

                applyButton.disabled = response.errors.length > 0;

            }

        } catch (error) {

            handleError(error, function (message) {
                result.innerHTML = `<p class="form-error">${escapeHtml(message)}</p>`;
            });

        } finally {

            busy = false;

            checkButton.disabled = !file;

        }

    }


    function render(response) {

        if (response.errors.length > 0) {

            result.innerHTML = `
                <p class="form-error">File có ${response.errors.length} lỗi — chưa ghi gì. Sửa file theo danh sách dưới rồi chọn lại.</p>
                <div class="admin-table-wrapper">
                    <table class="admin-table excel-import-errors">
                        <thead><tr><th>Sheet</th><th>Dòng</th><th>Cột</th><th>Lỗi</th></tr></thead>
                        <tbody>${response.errors.map(function (error) {
                            return `<tr><td>${escapeHtml(error.sheet)}</td><td>${escapeHtml(String(error.row))}</td>`
                                + `<td>${escapeHtml(error.column)}</td><td>${escapeHtml(error.message)}</td></tr>`;
                        }).join("")}</tbody>
                    </table>
                </div>
            `;

            return;

        }

        const items = Object.keys(response.summary).map(function (key) {
            return `<li>${escapeHtml(options.labels[key] || key)}: <strong>${escapeHtml(String(response.summary[key]))}</strong></li>`;
        }).join("");

        result.innerHTML = `
            <p class="${response.applied ? "form-success" : "admin-field-hint"}">${response.applied
                ? "Đã nhập " + response.rowCount + " dòng:"
                : "Kiểm tra xong " + response.rowCount + " dòng, không có lỗi. Chưa ghi gì — bấm NHẬP VÀO HỆ THỐNG để ghi:"}</p>
            <ul class="excel-import-summary">${items}</ul>
        `;

    }

    function handleError(error, show) {

        if (typeof adminHandleError === "function") {
            adminHandleError(error, show);
        } else {
            show(getErrorMessage(error));
        }

    }

}
