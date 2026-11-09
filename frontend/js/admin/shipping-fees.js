/* Phí giao hàng theo tỉnh (admin/shipping-fees.html) */

document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }

    const newButton = document.getElementById("feeNewBtn");
    const tbody = document.getElementById("feeTableBody");
    const rowCount = document.getElementById("feeRowCount");
    const panel = document.getElementById("feeFormPanel");
    const formTitle = document.getElementById("feeFormTitle");
    const form = document.getElementById("feeForm");
    const formError = document.getElementById("feeFormError");
    const saveButton = document.getElementById("feeSaveBtn");
    const provinceInput = document.getElementById("feeProvince");
    const amountInput = document.getElementById("feeAmount");

    let rows = [];
    let editingId = null;


    loadVnUnits().then(function (units) {

        document.getElementById("feeProvinceList").innerHTML = (units || []).map(function (unit) {
            return `<option value="${escapeHtml(unit.n)}"></option>`;
        }).join("");

    }).catch(function () {
        /* không tải được danh sách gợi ý thì vẫn gõ tay được */
    });

    newButton.addEventListener("click", function () { openForm(null); });

    document.getElementById("feeCancelBtn").addEventListener("click", closeForm);

    form.addEventListener("submit", save);

    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const rule = rows.find(function (row) { return String(row.id) === button.dataset.id; });

        if (!rule) {
            return;
        }

        if (button.dataset.action === "edit") {
            openForm(rule);
        } else if (button.dataset.action === "delete") {
            remove(rule);
        }

    });


    load();


    async function load() {

        tbody.innerHTML = adminSkeletonRow(4);

        try {

            rows = (await apiRequest("/admin/shipping-fees", { auth: true })) || [];

            rowCount.textContent = rows.length + " tỉnh / thành";

            tbody.innerHTML = rows.length === 0
                ? adminEmptyRow(4, "Chưa có tỉnh / thành nào có phí riêng: mọi nơi tính phí mặc định 30.000đ.")
                : rows.map(rowHtml).join("");

        } catch (error) {

            handleError(error, function (message) {
                tbody.innerHTML = adminEmptyRow(4, message);
            });

        }

    }


    function rowHtml(rule) {

        const id = escapeHtml(String(rule.id));

        return `
            <tr>
                <td><strong>${escapeHtml(rule.provinceName)}</strong></td>
                <td>${Number(rule.fee) === 0 ? "Miễn phí" : escapeHtml(formatPrice(rule.fee))}</td>
                <td>${rule.updatedAt ? escapeHtml(formatDateTimeVi(rule.updatedAt)) : "—"}</td>
                <td class="admin-actions-cell">
                    <button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>
                    <button type="button" class="admin-link-btn admin-link-danger" data-action="delete" data-id="${id}">Về phí mặc định</button>
                </td>
            </tr>
        `;

    }


    function openForm(rule) {

        editingId = rule ? rule.id : null;

        formTitle.textContent = rule ? "Sửa phí riêng" : "Thêm phí riêng";

        provinceInput.value = rule ? rule.provinceName : "";

        amountInput.value = rule ? String(Number(rule.fee)) : "";

        formError.hidden = true;

        panel.hidden = false;

        provinceInput.focus();

        panel.scrollIntoView({ behavior: "smooth", block: "start" });

    }


    function closeForm() {

        panel.hidden = true;

        editingId = null;

    }


    async function save(event) {

        event.preventDefault();

        const provinceName = provinceInput.value.trim();

        const fee = amountInput.value === "" ? NaN : Number(amountInput.value);

        if (!provinceName) {
            showFormError("Vui lòng nhập tên tỉnh / thành.");
            return;
        }

        if (!Number.isFinite(fee) || fee < 0 || fee > 10000000) {
            showFormError("Phí giao hàng phải từ 0 đến 10.000.000đ.");
            return;
        }

        formError.hidden = true;

        saveButton.disabled = true;

        try {

            await apiRequest(editingId ? "/admin/shipping-fees/" + editingId : "/admin/shipping-fees", {
                method: editingId ? "PUT" : "POST",
                body: { provinceName: provinceName, fee: fee },
                auth: true
            });

            showToast("Đã lưu phí giao hàng của " + provinceName + ".", "success");

            closeForm();

            load();

        } catch (error) {

            handleError(error, showFormError);

        } finally {

            saveButton.disabled = false;

        }

    }


    async function remove(rule) {

        if (!window.confirm("Đưa " + rule.provinceName + " về phí mặc định 30.000đ?")) {
            return;
        }

        try {

            await apiRequest("/admin/shipping-fees/" + rule.id, { method: "DELETE", auth: true });

            showToast(rule.provinceName + " đã về phí mặc định.", "success");

            load();

        } catch (error) {

            handleError(error, function (message) { showToast(message, "error"); });

        }

    }


    function showFormError(message) {

        formError.textContent = message;

        formError.hidden = false;

    }


    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
