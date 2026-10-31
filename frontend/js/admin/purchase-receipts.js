/* ================= PHIẾU NHẬP KHO (admin/purchase-receipts.html) ================= */

/*
 * API (backend kiểm tra vai trò + chi nhánh trong DB ở mọi request):
 *   GET  /admin/stores/{storeId}/purchase-receipts?status&keyword&page&size
 *   GET  /admin/stores/{storeId}/purchase-receipts/{id}                 phiếu + các dòng
 *   POST /admin/stores/{storeId}/purchase-receipts                      tạo phiếu nháp
 *   PUT  /admin/stores/{storeId}/purchase-receipts/{id}                 thay nội dung phiếu nháp
 *   POST /admin/stores/{storeId}/purchase-receipts/{id}/complete        cộng tồn kho + cập nhật giá vốn bình quân
 *   POST /admin/stores/{storeId}/purchase-receipts/{id}/cancel          huỷ phiếu nháp
 *   GET  /admin/suppliers?active=true, GET /products?keyword, GET /products/{id}/variants
 * Chỉ phiếu hoàn tất mới làm thay đổi tồn kho; phiếu nháp sửa / huỷ tự do.
 */

const RECEIPT_PAGE_SIZE = 20;

const RECEIPT_PICKER_SIZE = 10;

const RECEIPT_STATUSES = {
    DRAFT: { label: "Nháp", badge: "admin-badge-warning" },
    COMPLETED: { label: "Đã nhập kho", badge: "admin-badge-success" },
    CANCELLED: { label: "Đã huỷ", badge: "admin-badge-neutral" }
};


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }

    const isAdmin = staff.role === "ADMIN";

    const filterForm = document.getElementById("receiptFilterForm");
    const keywordInput = document.getElementById("receiptKeyword");
    const storeFilter = document.getElementById("receiptStoreFilter");
    const statusFilter = document.getElementById("receiptStatusFilter");
    const newButton = document.getElementById("receiptNewBtn");
    const tbody = document.getElementById("receiptTableBody");
    const rowCount = document.getElementById("receiptRowCount");
    const pagination = document.getElementById("receiptPagination");

    const panel = document.getElementById("receiptPanel");
    const panelTitle = document.getElementById("receiptPanelTitle");
    const panelStatus = document.getElementById("receiptPanelStatus");
    const form = document.getElementById("receiptForm");
    const errorBox = document.getElementById("receiptError");
    const supplierSelect = document.getElementById("receiptSupplier");
    const noteInput = document.getElementById("receiptNote");
    const addBox = document.getElementById("receiptAddBox");
    const searchBox = document.getElementById("receiptSearchBox");
    const productKeyword = document.getElementById("receiptProductKeyword");
    const productResults = document.getElementById("receiptProductResults");
    const chosenText = document.getElementById("receiptChosenProduct");
    const variantSelect = document.getElementById("receiptVariant");
    const quantityInput = document.getElementById("receiptQuantity");
    const unitCostInput = document.getElementById("receiptUnitCost");
    const linesBody = document.getElementById("receiptLines");
    const totalQuantity = document.getElementById("receiptTotalQuantity");
    const totalCost = document.getElementById("receiptTotalCost");
    const meta = document.getElementById("receiptMeta");
    const saveButton = document.getElementById("receiptSaveBtn");
    const completeButton = document.getElementById("receiptCompleteBtn");

    let storeId = isAdmin ? null : staff.storeId;
    let currentPage = 0;
    let rows = [];
    let listCounter = 0;
    let pickerCounter = 0;
    let variantCounter = 0;
    let pickerResults = [];
    let chosenProduct = null;
    let suppliers = [];

    /* Phiếu đang mở: id (null = phiếu mới), trạng thái, các dòng { variantId, label, quantity, unitCost, averageCost } */
    let editing = { id: null, status: "DRAFT", lines: [] };


    try {

        if (isAdmin) {

            const stores = await loadAllStores();

            storeFilter.innerHTML = stores.map(function (store) {
                return `<option value="${escapeHtml(String(store.id))}">${escapeHtml(storeOptionLabel(store))}</option>`;
            }).join("");

            storeId = stores.length ? stores[0].id : null;

        } else {

            await setupStoreFilter(storeFilter, staff);

        }

    } catch (error) {

        showToast(getErrorMessage(error), "error");

    }

    if (!storeId) {

        tbody.innerHTML = adminEmptyRow(7, isAdmin ? "Chưa có chi nhánh nào." : "Bạn chưa được gán chi nhánh nên chưa lập được phiếu nhập.");

        newButton.disabled = true;

        return;

    }

    await loadSuppliers();

    filterForm.addEventListener("submit", function (event) {
        event.preventDefault();
        load(0);
    });

    storeFilter.addEventListener("change", function () {
        storeId = Number(storeFilter.value);
        closePanel();
        load(0);
    });

    statusFilter.addEventListener("change", function () { load(0); });

    newButton.addEventListener("click", function () { openEditor(null); });

    document.getElementById("receiptCloseBtn").addEventListener("click", closePanel);

    document.getElementById("receiptProductSearchBtn").addEventListener("click", searchProducts);

    productKeyword.addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
            event.preventDefault();
            searchProducts();
        }
    });

    document.getElementById("receiptAddLineBtn").addEventListener("click", addLine);

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        save(false);
    });

    completeButton.addEventListener("click", function () { save(true); });

    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (button && !button.disabled) {
            load(Number(button.dataset.page));
        }

    });

    productResults.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action='choose']");

        if (button) {
            chooseProduct(pickerResults.find(function (product) { return String(product.id) === button.dataset.id; }));
        }

    });

    chosenText.addEventListener("click", function (event) {

        if (event.target.closest("button[data-action='change-product']")) {
            clearChosenProduct();
        }

    });

    linesBody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action='remove-line']");

        if (button) {
            editing.lines.splice(Number(button.dataset.index), 1);
            renderLines();
        }

    });

    linesBody.addEventListener("input", function (event) {

        const input = event.target.closest("input[data-index]");

        if (!input) {
            return;
        }

        const line = editing.lines[Number(input.dataset.index)];

        line[input.dataset.field] = input.value === "" ? "" : Number(input.value);

        renderTotals();

    });

    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const receipt = rows.find(function (row) { return String(row.id) === button.dataset.id; });

        if (!receipt) {
            return;
        }

        if (button.dataset.action === "open") {
            openEditor(receipt.id);
        } else if (button.dataset.action === "complete") {
            completeFromList(receipt);
        } else if (button.dataset.action === "cancel") {
            cancelFromList(receipt);
        }

    });


    load(0);


    async function loadSuppliers() {

        try {

            const page = await apiRequest("/admin/suppliers?active=true&size=100&sort=name", { auth: true });

            suppliers = page.content || [];

        } catch (error) {

            suppliers = [];

            showToast(getErrorMessage(error), "error");

        }

    }


    function receiptsUrl(suffix) {

        return "/admin/stores/" + storeId + "/purchase-receipts" + (suffix || "");

    }


    async function load(page) {

        const requestId = ++listCounter;

        currentPage = page;

        tbody.innerHTML = adminSkeletonRow(7);

        const params = new URLSearchParams({ page: String(page), size: String(RECEIPT_PAGE_SIZE) });

        if (statusFilter.value) {
            params.set("status", statusFilter.value);
        }

        if (keywordInput.value.trim()) {
            params.set("keyword", keywordInput.value.trim());
        }

        try {

            const result = await apiRequest(receiptsUrl("?" + params.toString()), { auth: true });

            if (requestId !== listCounter) {
                return;
            }

            rows = result.content || [];

            rowCount.textContent = result.totalElements + " phiếu";

            tbody.innerHTML = rows.length === 0
                ? adminEmptyRow(7, "Chưa có phiếu nhập nào.")
                : rows.map(rowHtml).join("");

            renderPagination(result);

        } catch (error) {

            if (requestId === listCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(7, message);
                });
            }

        }

    }


    function rowHtml(receipt) {

        const id = escapeHtml(String(receipt.id));

        const status = RECEIPT_STATUSES[receipt.status] || { label: receipt.status, badge: "admin-badge-neutral" };

        const draft = receipt.status === "DRAFT";

        return `
            <tr>
                <td><strong>${escapeHtml(receipt.code)}</strong><div class="admin-subtext">${escapeHtml(formatDateTimeVi(receipt.createdAt))}</div></td>
                <td>${escapeHtml(receipt.supplierName)}</td>
                <td><span class="admin-badge ${status.badge}">${escapeHtml(status.label)}</span></td>
                <td>${receipt.lineCount} dòng / ${receipt.totalQuantity} sp</td>
                <td>${escapeHtml(formatPrice(receipt.totalCost))}</td>
                <td>${receipt.createdByName ? escapeHtml(receipt.createdByName) : "—"}</td>
                <td class="admin-actions-cell">
                    <button type="button" class="admin-link-btn" data-action="open" data-id="${id}">${draft ? "Sửa" : "Xem"}</button>
                    ${draft ? `<button type="button" class="admin-link-btn" data-action="complete" data-id="${id}">Nhập kho</button>
                    <button type="button" class="admin-link-btn admin-link-danger" data-action="cancel" data-id="${id}">Huỷ phiếu</button>` : ""}
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


    /* ---------- editor ---------- */

    async function openEditor(receiptId) {

        hideError();

        editing = { id: null, status: "DRAFT", lines: [] };

        let receipt = null;

        if (receiptId != null) {

            try {

                receipt = await apiRequest(receiptsUrl("/" + receiptId), { auth: true });

            } catch (error) {

                handleError(error, function (message) { showToast(message, "error"); });

                return;

            }

            editing = {
                id: receipt.id,
                status: receipt.status,
                lines: receipt.items.map(function (item) {
                    return {
                        variantId: item.variantId,
                        label: item.productName + " - " + item.variantName,
                        quantity: item.quantity,
                        unitCost: item.unitCost,
                        averageCost: item.averageCost
                    };
                })
            };

        }

        const editable = editing.status === "DRAFT";

        panelTitle.textContent = receipt ? "Phiếu " + receipt.code : "Lập phiếu nhập kho";

        const status = RECEIPT_STATUSES[editing.status];

        panelStatus.hidden = !receipt;

        panelStatus.className = "admin-badge " + status.badge;

        panelStatus.textContent = status.label;

        fillSupplierSelect(receipt);

        noteInput.value = receipt && receipt.note ? receipt.note : "";

        supplierSelect.disabled = !editable;

        noteInput.disabled = !editable;

        addBox.hidden = !editable;

        saveButton.hidden = !editable;

        completeButton.hidden = !editable;

        meta.textContent = receipt ? metaText(receipt) : "";

        clearChosenProduct();

        productKeyword.value = "";

        productResults.hidden = true;

        renderLines();

        panel.hidden = false;

        panel.scrollIntoView({ behavior: "smooth", block: "start" });

    }


    function metaText(receipt) {

        const parts = ["Chi nhánh " + receipt.storeName, "lập bởi " + (receipt.createdByName || "—") + " lúc " + formatDateTimeVi(receipt.createdAt)];

        if (receipt.completedAt) {
            parts.push("nhập kho bởi " + (receipt.completedByName || "—") + " lúc " + formatDateTimeVi(receipt.completedAt));
        }

        return parts.join(" · ");

    }


    function fillSupplierSelect(receipt) {

        const options = suppliers.slice();

        /* Nhà cung cấp của phiếu cũ đã ngừng hợp tác vẫn phải hiện để xem phiếu */
        if (receipt && !options.some(function (supplier) { return supplier.id === receipt.supplierId; })) {
            options.push({ id: receipt.supplierId, name: receipt.supplierName + " (đã ngừng hợp tác)" });
        }

        supplierSelect.innerHTML = '<option value="">Chọn nhà cung cấp</option>' + options.map(function (supplier) {
            return `<option value="${escapeHtml(String(supplier.id))}">${escapeHtml(supplier.name)}</option>`;
        }).join("");

        supplierSelect.value = receipt ? String(receipt.supplierId) : "";

    }


    function closePanel() {

        panel.hidden = true;

        editing = { id: null, status: "DRAFT", lines: [] };

    }


    function renderLines() {

        const editable = editing.status === "DRAFT";

        linesBody.innerHTML = editing.lines.length === 0
            ? adminEmptyRow(6, "Chưa có sản phẩm nào trong phiếu.")
            : editing.lines.map(function (line, index) {

                const quantityCell = editable
                    ? `<input type="number" min="1" max="100000" step="1" value="${escapeHtml(String(line.quantity))}" data-index="${index}" data-field="quantity" aria-label="Số lượng">`
                    : escapeHtml(String(line.quantity));

                const costCell = editable
                    ? `<input type="number" min="0" step="1000" value="${escapeHtml(String(line.unitCost))}" data-index="${index}" data-field="unitCost" aria-label="Giá nhập">`
                    : escapeHtml(formatPrice(line.unitCost));

                return `
                    <tr>
                        <td>${escapeHtml(line.label)}</td>
                        <td>${quantityCell}</td>
                        <td>${costCell}</td>
                        <td data-line-total="${index}">${escapeHtml(formatPrice(lineTotal(line)))}</td>
                        <td>${line.averageCost != null ? escapeHtml(formatPrice(line.averageCost)) : "—"}</td>
                        <td>${editable ? `<button type="button" class="admin-link-btn admin-link-danger" data-action="remove-line" data-index="${index}">Bỏ</button>` : ""}</td>
                    </tr>
                `;

            }).join("");

        renderTotals();

    }


    function lineTotal(line) {

        return (Number(line.quantity) || 0) * (Number(line.unitCost) || 0);

    }


    function renderTotals() {

        totalQuantity.textContent = String(editing.lines.reduce(function (sum, line) { return sum + (Number(line.quantity) || 0); }, 0));

        totalCost.textContent = formatPrice(editing.lines.reduce(function (sum, line) { return sum + lineTotal(line); }, 0));

        editing.lines.forEach(function (line, index) {

            const cell = linesBody.querySelector('[data-line-total="' + index + '"]');

            if (cell) {
                cell.textContent = formatPrice(lineTotal(line));
            }

        });

    }


    function addLine() {

        hideError();

        const variantId = Number(variantSelect.value);

        const quantity = Number(quantityInput.value);

        const unitCost = unitCostInput.value === "" ? NaN : Number(unitCostInput.value);

        if (!chosenProduct || !variantId) {
            showError("Vui lòng chọn sản phẩm và phiên bản.");
            return;
        }

        if (!Number.isInteger(quantity) || quantity < 1 || quantity > 100000) {
            showError("Số lượng phải là số nguyên từ 1 đến 100.000.");
            return;
        }

        if (!Number.isFinite(unitCost) || unitCost < 0) {
            showError("Vui lòng nhập giá nhập (không âm).");
            return;
        }

        if (editing.lines.some(function (line) { return line.variantId === variantId; })) {
            showError("Phiên bản này đã có trong phiếu, hãy sửa số lượng ở dòng đó.");
            return;
        }

        const option = variantSelect.options[variantSelect.selectedIndex];

        editing.lines.push({
            variantId: variantId,
            label: chosenProduct.name + " - " + option.dataset.name,
            quantity: quantity,
            unitCost: unitCost,
            averageCost: null
        });

        quantityInput.value = "";

        unitCostInput.value = "";

        clearChosenProduct();

        renderLines();

    }


    /* ---------- chọn sản phẩm ---------- */

    async function searchProducts() {

        const requestId = ++pickerCounter;

        const params = new URLSearchParams({ size: String(RECEIPT_PICKER_SIZE), isActive: "true" });

        if (productKeyword.value.trim()) {
            params.set("keyword", productKeyword.value.trim());
        }

        productResults.hidden = false;

        productResults.innerHTML = '<li class="admin-subtext">Đang tìm…</li>';

        try {

            const page = await apiRequest("/products?" + params.toString());

            if (requestId !== pickerCounter) {
                return;
            }

            pickerResults = page.content || [];

            productResults.innerHTML = pickerResults.length === 0
                ? '<li class="admin-subtext">Không tìm thấy sản phẩm nào.</li>'
                : pickerResults.map(function (product) {

                    return `
                        <li>
                            <span class="admin-promo-result-name">
                                ${escapeHtml(product.name)}
                                <span class="admin-subtext">#${escapeHtml(String(product.id))}</span>
                            </span>
                            <button type="button" class="btn btn-outline-dark" data-action="choose" data-id="${escapeHtml(String(product.id))}">CHỌN</button>
                        </li>
                    `;

                }).join("");

        } catch (error) {

            if (requestId === pickerCounter) {
                handleError(error, function (message) {
                    productResults.innerHTML = `<li class="admin-subtext">${escapeHtml(message)}</li>`;
                });
            }

        }

    }


    async function chooseProduct(product) {

        if (!product) {
            return;
        }

        chosenProduct = product;

        chosenText.innerHTML = `Đã chọn: <strong>${escapeHtml(product.name)}</strong> `
            + '<button type="button" class="admin-link-btn" data-action="change-product">Đổi sản phẩm</button>';

        chosenText.hidden = false;

        searchBox.hidden = true;

        productResults.hidden = true;

        variantSelect.innerHTML = '<option value="">Đang tải phiên bản…</option>';

        variantSelect.disabled = true;

        const requestId = ++variantCounter;

        try {

            const variants = await apiRequest("/products/" + product.id + "/variants");

            if (requestId !== variantCounter) {
                return;
            }

            if (!variants || variants.length === 0) {
                variantSelect.innerHTML = '<option value="">Sản phẩm chưa có phiên bản nào</option>';
                return;
            }

            variantSelect.innerHTML = (variants.length > 1 ? '<option value="">Chọn phiên bản</option>' : "")
                + variants.map(function (variant) {

                    const label = variant.variantName + (variant.skuVariant ? " · " + variant.skuVariant : "");

                    return `<option value="${escapeHtml(String(variant.id))}" data-name="${escapeHtml(variant.variantName)}"`
                        + `>${escapeHtml(label)}</option>`;

                }).join("");

            variantSelect.disabled = false;

        } catch (error) {

            if (requestId === variantCounter) {
                handleError(error, function (message) {
                    variantSelect.innerHTML = `<option value="">${escapeHtml(message)}</option>`;
                });
            }

        }

    }


    function clearChosenProduct() {

        chosenProduct = null;

        variantCounter++;

        chosenText.hidden = true;

        chosenText.innerHTML = "";

        searchBox.hidden = false;

        variantSelect.innerHTML = '<option value="">Chọn sản phẩm trước</option>';

        variantSelect.disabled = true;

    }


    /* ---------- lưu / hoàn tất / huỷ ---------- */

    function requestBody() {

        return {
            supplierId: Number(supplierSelect.value),
            note: noteInput.value.trim(),
            items: editing.lines.map(function (line) {
                return { variantId: line.variantId, quantity: Number(line.quantity), unitCost: Number(line.unitCost) };
            })
        };

    }


    function validate() {

        if (!supplierSelect.value) {
            return "Vui lòng chọn nhà cung cấp.";
        }

        if (editing.lines.length === 0) {
            return "Phiếu nhập cần ít nhất một sản phẩm.";
        }

        const bad = editing.lines.find(function (line) {
            return !Number.isInteger(Number(line.quantity)) || Number(line.quantity) < 1
                || line.unitCost === "" || !(Number(line.unitCost) >= 0);
        });

        return bad ? "Kiểm tra lại số lượng (≥ 1) và giá nhập của " + bad.label + "." : null;

    }


    async function save(complete) {

        hideError();

        const problem = validate();

        if (problem) {
            showError(problem);
            return;
        }

        if (complete && !window.confirm("Nhập kho phiếu này? Tồn kho và giá vốn sẽ thay đổi ngay và không hoàn tác được.")) {
            return;
        }

        saveButton.disabled = true;

        completeButton.disabled = true;

        try {

            const saved = await apiRequest(editing.id ? receiptsUrl("/" + editing.id) : receiptsUrl(), {
                method: editing.id ? "PUT" : "POST",
                body: requestBody(),
                auth: true
            });

            if (complete) {
                await apiRequest(receiptsUrl("/" + saved.id + "/complete"), { method: "POST", auth: true });
            }

            const wasEditing = editing.id != null;

            showToast(complete ? "Đã nhập kho theo phiếu " + saved.code + "." : "Đã lưu phiếu nháp " + saved.code + ".", "success");

            closePanel();

            load(wasEditing ? currentPage : 0);

        } catch (error) {

            handleError(error, showError);

            if (complete) {
                load(currentPage);
            }

        } finally {

            saveButton.disabled = false;

            completeButton.disabled = false;

        }

    }


    async function completeFromList(receipt) {

        if (!window.confirm("Nhập kho phiếu " + receipt.code + "? Tồn kho và giá vốn sẽ thay đổi ngay và không hoàn tác được.")) {
            return;
        }

        try {

            await apiRequest(receiptsUrl("/" + receipt.id + "/complete"), { method: "POST", auth: true });

            showToast("Đã nhập kho theo phiếu " + receipt.code + ".", "success");

            load(currentPage);

        } catch (error) {

            handleError(error, function (message) { showToast(message, "error"); });

        }

    }


    async function cancelFromList(receipt) {

        if (!window.confirm("Huỷ phiếu nháp " + receipt.code + "?")) {
            return;
        }

        try {

            await apiRequest(receiptsUrl("/" + receipt.id + "/cancel"), { method: "POST", auth: true });

            showToast("Đã huỷ phiếu " + receipt.code + ".", "success");

            load(currentPage);

        } catch (error) {

            handleError(error, function (message) { showToast(message, "error"); });

        }

    }


    function showError(message) {

        errorBox.textContent = message;

        errorBox.hidden = false;

    }


    function hideError() {

        errorBox.textContent = "";

        errorBox.hidden = true;

    }


    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
