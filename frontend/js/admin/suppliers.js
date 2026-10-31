/* ================= NHÀ CUNG CẤP (admin/suppliers.html) ================= */

/*
 * API: GET /admin/suppliers?keyword&active&page&size, POST /admin/suppliers, PUT /admin/suppliers/{id},
 * DELETE /admin/suppliers/{id} (409 khi nhà cung cấp đã có phiếu nhập: chuyển sang "ngừng hợp tác").
 */

const SUPPLIER_PAGE_SIZE = 20;


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }

    const filterForm = document.getElementById("supplierFilterForm");
    const keywordInput = document.getElementById("supplierKeyword");
    const statusFilter = document.getElementById("supplierStatusFilter");
    const newButton = document.getElementById("supplierNewBtn");
    const tbody = document.getElementById("supplierTableBody");
    const rowCount = document.getElementById("supplierRowCount");
    const pagination = document.getElementById("supplierPagination");
    const panel = document.getElementById("supplierFormPanel");
    const formTitle = document.getElementById("supplierFormTitle");
    const form = document.getElementById("supplierForm");
    const formError = document.getElementById("supplierFormError");
    const saveButton = document.getElementById("supplierSaveBtn");

    const fields = {
        name: document.getElementById("supplierName"),
        phone: document.getElementById("supplierPhone"),
        email: document.getElementById("supplierEmail"),
        taxCode: document.getElementById("supplierTaxCode"),
        address: document.getElementById("supplierAddress"),
        note: document.getElementById("supplierNote"),
        active: document.getElementById("supplierActive")
    };

    let currentPage = 0;
    let rows = [];
    let editingId = null;
    let requestCounter = 0;


    filterForm.addEventListener("submit", function (event) {
        event.preventDefault();
        load(0);
    });

    statusFilter.addEventListener("change", function () { load(0); });

    newButton.addEventListener("click", function () { openForm(null); });

    document.getElementById("supplierCancelBtn").addEventListener("click", closeForm);

    form.addEventListener("submit", save);

    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (button && !button.disabled) {
            load(Number(button.dataset.page));
        }

    });

    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const supplier = rows.find(function (row) { return String(row.id) === button.dataset.id; });

        if (!supplier) {
            return;
        }

        if (button.dataset.action === "edit") {
            openForm(supplier);
        } else if (button.dataset.action === "toggle") {
            toggleActive(supplier);
        } else if (button.dataset.action === "delete") {
            remove(supplier);
        }

    });


    load(0);


    async function load(page) {

        const requestId = ++requestCounter;

        currentPage = page;

        tbody.innerHTML = adminSkeletonRow(5);

        const params = new URLSearchParams({ page: String(page), size: String(SUPPLIER_PAGE_SIZE) });

        if (keywordInput.value.trim()) {
            params.set("keyword", keywordInput.value.trim());
        }

        if (statusFilter.value) {
            params.set("active", statusFilter.value);
        }

        try {

            const result = await apiRequest("/admin/suppliers?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            rows = result.content || [];

            rowCount.textContent = result.totalElements + " nhà cung cấp";

            tbody.innerHTML = rows.length === 0
                ? adminEmptyRow(5, "Chưa có nhà cung cấp nào.")
                : rows.map(rowHtml).join("");

            renderPagination(result);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(5, message);
                });
            }

        }

    }


    function rowHtml(supplier) {

        const id = escapeHtml(String(supplier.id));

        const contact = [supplier.phone, supplier.email].filter(Boolean).map(escapeHtml).join("<br>") || "—";

        return `
            <tr>
                <td>
                    <strong>${escapeHtml(supplier.name)}</strong>
                    ${supplier.address ? `<div class="admin-subtext">${escapeHtml(supplier.address)}</div>` : ""}
                </td>
                <td>${contact}</td>
                <td>${supplier.taxCode ? escapeHtml(supplier.taxCode) : "—"}</td>
                <td>
                    <span class="admin-badge ${supplier.active ? "admin-badge-success" : "admin-badge-neutral"}">${supplier.active ? "Đang hợp tác" : "Ngừng hợp tác"}</span>
                </td>
                <td class="admin-actions-cell">
                    <button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>
                    <button type="button" class="admin-link-btn" data-action="toggle" data-id="${id}">${supplier.active ? "Ngừng hợp tác" : "Hợp tác lại"}</button>
                    <button type="button" class="admin-link-btn admin-link-danger" data-action="delete" data-id="${id}">Xoá</button>
                </td>
            </tr>
        `;

    }


    function renderPagination(page) {

        if (page.totalPages <= 1) {
            pagination.innerHTML = "";
            return;
        }

        pagination.innerHTML = `
            <button type="button" class="btn btn-outline-dark" data-page="${page.page - 1}" ${page.page <= 0 ? "disabled" : ""}>‹ TRƯỚC</button>
            <span>Trang ${page.page + 1} / ${page.totalPages}</span>
            <button type="button" class="btn btn-outline-dark" data-page="${page.page + 1}" ${page.page + 1 >= page.totalPages ? "disabled" : ""}>SAU ›</button>
        `;

    }


    function openForm(supplier) {

        editingId = supplier ? supplier.id : null;

        formTitle.textContent = supplier ? "Sửa nhà cung cấp" : "Thêm nhà cung cấp";

        fields.name.value = supplier ? supplier.name : "";
        fields.phone.value = supplier && supplier.phone ? supplier.phone : "";
        fields.email.value = supplier && supplier.email ? supplier.email : "";
        fields.taxCode.value = supplier && supplier.taxCode ? supplier.taxCode : "";
        fields.address.value = supplier && supplier.address ? supplier.address : "";
        fields.note.value = supplier && supplier.note ? supplier.note : "";
        fields.active.checked = supplier ? supplier.active : true;

        formError.hidden = true;

        panel.hidden = false;

        fields.name.focus();

        panel.scrollIntoView({ behavior: "smooth", block: "start" });

    }


    function closeForm() {

        panel.hidden = true;

        editingId = null;

    }


    function bodyFromForm() {

        return {
            name: fields.name.value.trim(),
            phone: fields.phone.value.trim(),
            email: fields.email.value.trim(),
            taxCode: fields.taxCode.value.trim(),
            address: fields.address.value.trim(),
            note: fields.note.value.trim(),
            active: fields.active.checked
        };

    }


    async function save(event) {

        event.preventDefault();

        const body = bodyFromForm();

        if (!body.name) {
            showFormError("Vui lòng nhập tên nhà cung cấp.");
            return;
        }

        formError.hidden = true;

        saveButton.disabled = true;

        try {

            await apiRequest(editingId ? "/admin/suppliers/" + editingId : "/admin/suppliers", {
                method: editingId ? "PUT" : "POST",
                body: body,
                auth: true
            });

            showToast(editingId ? "Đã lưu nhà cung cấp." : "Đã thêm nhà cung cấp.", "success");

            closeForm();

            load(editingId ? currentPage : 0);

        } catch (error) {

            handleError(error, showFormError);

        } finally {

            saveButton.disabled = false;

        }

    }


    async function toggleActive(supplier) {

        try {

            await apiRequest("/admin/suppliers/" + supplier.id, {
                method: "PUT",
                body: Object.assign({}, supplier, { active: !supplier.active }),
                auth: true
            });

            showToast(supplier.active ? "Đã ngừng hợp tác." : "Đã hợp tác lại.", "success");

            load(currentPage);

        } catch (error) {

            handleError(error, function (message) { showToast(message, "error"); });

        }

    }


    async function remove(supplier) {

        if (!window.confirm("Xoá nhà cung cấp \"" + supplier.name + "\"?")) {
            return;
        }

        try {

            await apiRequest("/admin/suppliers/" + supplier.id, { method: "DELETE", auth: true });

            showToast("Đã xoá nhà cung cấp.", "success");

            load(currentPage);

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
