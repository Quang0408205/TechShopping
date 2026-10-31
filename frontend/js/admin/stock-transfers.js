/* ================= CHUYỂN KHO (admin/stock-transfers.html) ================= */

/*
 * API (backend kiểm tra vai trò + chi nhánh trong DB ở mọi request):
 *   GET  /admin/stock-transfers?direction&status&storeId&page&size     nhân viên: phiếu của chi nhánh mình; ADMIN: tất cả
 *   GET  /admin/stock-transfers/{id}                                   phiếu + các dòng
 *   POST /admin/stock-transfers                                        tạo phiếu nháp (từ chi nhánh của mình)
 *   PUT  /admin/stock-transfers/{id}                                   thay nội dung phiếu nháp
 *   POST /admin/stock-transfers/{id}/send                              gửi hàng: trừ tồn chi nhánh gửi
 *   POST /admin/stock-transfers/{id}/receive                           nhận hàng: cộng tồn chi nhánh nhận
 *   POST /admin/stock-transfers/{id}/cancel                            huỷ nháp / huỷ phiếu đang chuyển (hoàn lại chi nhánh gửi)
 *   GET  /stores (chi nhánh đang mở), GET /products?keyword, GET /products/{id}/variants
 * Chi nhánh gửi lập, gửi, huỷ; chi nhánh nhận xác nhận nhận hàng; ADMIN làm được mọi bước.
 */

const TRANSFER_PAGE_SIZE = 20;

const TRANSFER_PICKER_SIZE = 10;

const TRANSFER_STATUSES = {
    DRAFT: { label: "Nháp", badge: "admin-badge-warning" },
    IN_TRANSIT: { label: "Đang chuyển", badge: "admin-badge-info" },
    RECEIVED: { label: "Đã nhận", badge: "admin-badge-success" },
    CANCELLED: { label: "Đã huỷ", badge: "admin-badge-neutral" }
};


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }

    const isAdmin = staff.role === "ADMIN";

    const filterForm = document.getElementById("transferFilterForm");
    const storeFilter = document.getElementById("transferStoreFilter");
    const directionFilter = document.getElementById("transferDirectionFilter");
    const statusFilter = document.getElementById("transferStatusFilter");
    const newButton = document.getElementById("transferNewBtn");
    const tbody = document.getElementById("transferTableBody");
    const rowCount = document.getElementById("transferRowCount");
    const pagination = document.getElementById("transferPagination");

    const panel = document.getElementById("transferPanel");
    const panelTitle = document.getElementById("transferPanelTitle");
    const panelStatus = document.getElementById("transferPanelStatus");
    const form = document.getElementById("transferForm");
    const errorBox = document.getElementById("transferError");
    const fromSelect = document.getElementById("transferFrom");
    const toSelect = document.getElementById("transferTo");
    const noteInput = document.getElementById("transferNote");
    const addBox = document.getElementById("transferAddBox");
    const searchBox = document.getElementById("transferSearchBox");
    const productKeyword = document.getElementById("transferProductKeyword");
    const productResults = document.getElementById("transferProductResults");
    const chosenText = document.getElementById("transferChosenProduct");
    const variantSelect = document.getElementById("transferVariant");
    const quantityInput = document.getElementById("transferQuantity");
    const linesBody = document.getElementById("transferLines");
    const totalQuantity = document.getElementById("transferTotalQuantity");
    const meta = document.getElementById("transferMeta");
    const saveButton = document.getElementById("transferSaveBtn");
    const sendButton = document.getElementById("transferSendBtn");

    let currentPage = 0;
    let rows = [];
    let listCounter = 0;
    let pickerCounter = 0;
    let variantCounter = 0;
    let pickerResults = [];
    let chosenProduct = null;
    let allStores = [];
    let openStores = [];

    /* Phiếu đang mở: id (null = phiếu mới), trạng thái, các dòng { variantId, label, quantity } */
    let editing = { id: null, status: "DRAFT", lines: [] };


    try {

        openStores = (await apiRequest("/stores")) || [];

        allStores = isAdmin ? await loadAllStores() : openStores;

    } catch (error) {

        showToast(getErrorMessage(error), "error");

    }

    if (isAdmin) {

        fillStoreOptions(storeFilter, [{ value: "", label: "Tất cả chi nhánh" }], allStores);

    } else {

        await setupStoreFilter(storeFilter, staff);

    }

    if (!isAdmin && !staff.storeId) {

        tbody.innerHTML = adminEmptyRow(6, "Bạn chưa được gán chi nhánh nên chưa dùng được chuyển kho.");

        newButton.disabled = true;

        return;

    }


    filterForm.addEventListener("submit", function (event) {
        event.preventDefault();
        load(0);
    });

    [storeFilter, directionFilter, statusFilter].forEach(function (select) {
        select.addEventListener("change", function () { load(0); });
    });

    newButton.addEventListener("click", function () { openEditor(null); });

    document.getElementById("transferCloseBtn").addEventListener("click", closePanel);

    document.getElementById("transferProductSearchBtn").addEventListener("click", searchProducts);

    productKeyword.addEventListener("keydown", function (event) {
        if (event.key === "Enter") {
            event.preventDefault();
            searchProducts();
        }
    });

    document.getElementById("transferAddLineBtn").addEventListener("click", addLine);

    fromSelect.addEventListener("change", fillToSelect);

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        save(false);
    });

    sendButton.addEventListener("click", function () { save(true); });

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

        if (input) {
            editing.lines[Number(input.dataset.index)].quantity = input.value === "" ? "" : Number(input.value);
            renderTotals();
        }

    });

    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const transfer = rows.find(function (row) { return String(row.id) === button.dataset.id; });

        if (!transfer) {
            return;
        }

        if (button.dataset.action === "open") {
            openEditor(transfer.id);
        } else {
            runStep(transfer, button.dataset.action);
        }

    });


    load(0);


    /* Bước nào thuộc phía nào: gửi / huỷ = chi nhánh gửi, nhận = chi nhánh nhận; ADMIN làm được hết */
    function isFromSide(transfer) {
        return isAdmin || transfer.fromStoreId === staff.storeId;
    }

    function isToSide(transfer) {
        return isAdmin || transfer.toStoreId === staff.storeId;
    }


    async function load(page) {

        const requestId = ++listCounter;

        currentPage = page;

        tbody.innerHTML = adminSkeletonRow(6);

        const params = new URLSearchParams({ page: String(page), size: String(TRANSFER_PAGE_SIZE) });

        if (statusFilter.value) {
            params.set("status", statusFilter.value);
        }

        if (directionFilter.value) {
            params.set("direction", directionFilter.value);
        }

        if (isAdmin && storeFilter.value) {
            params.set("storeId", storeFilter.value);
        }

        try {

            const result = await apiRequest("/admin/stock-transfers?" + params.toString(), { auth: true });

            if (requestId !== listCounter) {
                return;
            }

            rows = result.content || [];

            rowCount.textContent = result.totalElements + " phiếu";

            tbody.innerHTML = rows.length === 0
                ? adminEmptyRow(6, "Chưa có phiếu chuyển kho nào.")
                : rows.map(rowHtml).join("");

            renderPagination(result);

        } catch (error) {

            if (requestId === listCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(6, message);
                });
            }

        }

    }


    function rowHtml(transfer) {

        const id = escapeHtml(String(transfer.id));

        const status = TRANSFER_STATUSES[transfer.status] || { label: transfer.status, badge: "admin-badge-neutral" };

        const actions = [`<button type="button" class="admin-link-btn" data-action="open" data-id="${id}">${transfer.status === "DRAFT" && isFromSide(transfer) ? "Sửa" : "Xem"}</button>`];

        if (transfer.status === "DRAFT" && isFromSide(transfer)) {
            actions.push(`<button type="button" class="admin-link-btn" data-action="send" data-id="${id}">Gửi hàng</button>`);
        }

        if (transfer.status === "IN_TRANSIT" && isToSide(transfer)) {
            actions.push(`<button type="button" class="admin-link-btn" data-action="receive" data-id="${id}">Nhận hàng</button>`);
        }

        if ((transfer.status === "DRAFT" || transfer.status === "IN_TRANSIT") && isFromSide(transfer)) {
            actions.push(`<button type="button" class="admin-link-btn admin-link-danger" data-action="cancel" data-id="${id}">Huỷ phiếu</button>`);
        }

        return `
            <tr>
                <td><strong>${escapeHtml(transfer.code)}</strong><div class="admin-subtext">${escapeHtml(formatDateTimeVi(transfer.createdAt))}</div></td>
                <td>${escapeHtml(transfer.fromStoreName)} → ${escapeHtml(transfer.toStoreName)}</td>
                <td><span class="admin-badge ${status.badge}">${escapeHtml(status.label)}</span></td>
                <td>${transfer.lineCount} dòng / ${transfer.totalQuantity} sp</td>
                <td>${transfer.createdByName ? escapeHtml(transfer.createdByName) : "—"}</td>
                <td class="admin-actions-cell">${actions.join("")}</td>
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

    async function openEditor(transferId) {

        hideError();

        editing = { id: null, status: "DRAFT", lines: [] };

        let transfer = null;

        if (transferId != null) {

            try {

                transfer = await apiRequest("/admin/stock-transfers/" + transferId, { auth: true });

            } catch (error) {

                handleError(error, function (message) { showToast(message, "error"); });

                return;

            }

            editing = {
                id: transfer.id,
                status: transfer.status,
                lines: transfer.items.map(function (item) {
                    return { variantId: item.variantId, label: item.productName + " - " + item.variantName, quantity: item.quantity };
                })
            };

        }

        const editable = editing.status === "DRAFT" && (!transfer || isFromSide(transfer));

        panelTitle.textContent = transfer ? "Phiếu " + transfer.code : "Lập phiếu chuyển kho";

        const status = TRANSFER_STATUSES[editing.status];

        panelStatus.hidden = !transfer;

        panelStatus.className = "admin-badge " + status.badge;

        panelStatus.textContent = status.label;

        fillFromSelect(transfer);

        toSelect.innerHTML = "";

        fillToSelect(transfer ? transfer.toStoreId : null);

        noteInput.value = transfer && transfer.note ? transfer.note : "";

        /* Chi nhánh gửi không đổi sau khi tạo phiếu */
        fromSelect.disabled = !editable || Boolean(transfer) || !isAdmin;

        toSelect.disabled = !editable;

        noteInput.disabled = !editable;

        addBox.hidden = !editable;

        saveButton.hidden = !editable;

        sendButton.hidden = !editable;

        meta.textContent = transfer ? metaText(transfer) : "";

        clearChosenProduct();

        productKeyword.value = "";

        productResults.hidden = true;

        renderLines(editable);

        panel.hidden = false;

        panel.scrollIntoView({ behavior: "smooth", block: "start" });

    }


    function metaText(transfer) {

        const parts = ["lập bởi " + (transfer.createdByName || "—") + " lúc " + formatDateTimeVi(transfer.createdAt)];

        if (transfer.sentAt) {
            parts.push("gửi bởi " + (transfer.sentByName || "—") + " lúc " + formatDateTimeVi(transfer.sentAt));
        }

        if (transfer.receivedAt) {
            parts.push("nhận bởi " + (transfer.receivedByName || "—") + " lúc " + formatDateTimeVi(transfer.receivedAt));
        }

        if (transfer.cancelledAt) {
            parts.push("huỷ bởi " + (transfer.cancelledByName || "—") + " lúc " + formatDateTimeVi(transfer.cancelledAt));
        }

        return parts.join(" · ");

    }


    function fillFromSelect(transfer) {

        const stores = transfer
            ? [{ id: transfer.fromStoreId, name: transfer.fromStoreName }]
            : (isAdmin ? openStores : [{ id: staff.storeId, name: staff.storeName }]);

        fromSelect.innerHTML = stores.map(function (store) {
            return `<option value="${escapeHtml(String(store.id))}">${escapeHtml(store.name)}</option>`;
        }).join("");

        if (transfer) {
            fromSelect.value = String(transfer.fromStoreId);
        }

    }


    /* Chi nhánh nhận: mọi chi nhánh đang mở trừ chi nhánh gửi; phiếu cũ giữ chi nhánh nhận của nó để xem */
    function fillToSelect(selected) {

        const keep = typeof selected === "number" ? selected : Number(toSelect.value) || null;

        const options = openStores.filter(function (store) { return String(store.id) !== fromSelect.value; });

        if (keep && !options.some(function (store) { return store.id === keep; })) {

            const known = allStores.find(function (store) { return store.id === keep; });

            if (known && String(known.id) !== fromSelect.value) {
                options.push(known);
            }

        }

        toSelect.innerHTML = '<option value="">Chọn chi nhánh nhận</option>' + options.map(function (store) {
            return `<option value="${escapeHtml(String(store.id))}">${escapeHtml(store.name)}</option>`;
        }).join("");

        toSelect.value = keep ? String(keep) : "";

    }


    function closePanel() {

        panel.hidden = true;

        editing = { id: null, status: "DRAFT", lines: [] };

    }


    function renderLines(editable) {

        const canEdit = editable === undefined ? editing.status === "DRAFT" : editable;

        linesBody.innerHTML = editing.lines.length === 0
            ? adminEmptyRow(3, "Chưa có sản phẩm nào trong phiếu.")
            : editing.lines.map(function (line, index) {

                const quantityCell = canEdit
                    ? `<input type="number" min="1" max="100000" step="1" value="${escapeHtml(String(line.quantity))}" data-index="${index}" aria-label="Số lượng">`
                    : escapeHtml(String(line.quantity));

                return `
                    <tr>
                        <td>${escapeHtml(line.label)}</td>
                        <td>${quantityCell}</td>
                        <td>${canEdit ? `<button type="button" class="admin-link-btn admin-link-danger" data-action="remove-line" data-index="${index}">Bỏ</button>` : ""}</td>
                    </tr>
                `;

            }).join("");

        renderTotals();

    }


    function renderTotals() {

        totalQuantity.textContent = String(editing.lines.reduce(function (sum, line) { return sum + (Number(line.quantity) || 0); }, 0));

    }


    function addLine() {

        hideError();

        const variantId = Number(variantSelect.value);

        const quantity = Number(quantityInput.value);

        if (!chosenProduct || !variantId) {
            showError("Vui lòng chọn sản phẩm và phiên bản.");
            return;
        }

        if (!Number.isInteger(quantity) || quantity < 1 || quantity > 100000) {
            showError("Số lượng phải là số nguyên từ 1 đến 100.000.");
            return;
        }

        if (editing.lines.some(function (line) { return line.variantId === variantId; })) {
            showError("Phiên bản này đã có trong phiếu, hãy sửa số lượng ở dòng đó.");
            return;
        }

        const option = variantSelect.options[variantSelect.selectedIndex];

        editing.lines.push({ variantId: variantId, label: chosenProduct.name + " - " + option.dataset.name, quantity: quantity });

        quantityInput.value = "";

        clearChosenProduct();

        renderLines();

    }


    /* ---------- chọn sản phẩm ---------- */

    async function searchProducts() {

        const requestId = ++pickerCounter;

        const params = new URLSearchParams({ size: String(TRANSFER_PICKER_SIZE), isActive: "true" });

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

                    return `<option value="${escapeHtml(String(variant.id))}" data-name="${escapeHtml(variant.variantName)}">${escapeHtml(label)}</option>`;

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


    /* ---------- lưu / gửi / nhận / huỷ ---------- */

    function requestBody() {

        return {
            fromStoreId: Number(fromSelect.value),
            toStoreId: Number(toSelect.value),
            note: noteInput.value.trim(),
            items: editing.lines.map(function (line) {
                return { variantId: line.variantId, quantity: Number(line.quantity) };
            })
        };

    }


    function validate() {

        if (!fromSelect.value) {
            return "Vui lòng chọn chi nhánh gửi.";
        }

        if (!toSelect.value) {
            return "Vui lòng chọn chi nhánh nhận.";
        }

        if (editing.lines.length === 0) {
            return "Phiếu chuyển cần ít nhất một sản phẩm.";
        }

        const bad = editing.lines.find(function (line) {
            return !Number.isInteger(Number(line.quantity)) || Number(line.quantity) < 1;
        });

        return bad ? "Số lượng của " + bad.label + " phải là số nguyên từ 1 trở lên." : null;

    }


    async function save(send) {

        hideError();

        const problem = validate();

        if (problem) {
            showError(problem);
            return;
        }

        if (send && !window.confirm("Gửi hàng ngay? Tồn kho chi nhánh gửi sẽ giảm đi ngay.")) {
            return;
        }

        saveButton.disabled = true;

        sendButton.disabled = true;

        const wasEditing = editing.id != null;

        try {

            const saved = await apiRequest(wasEditing ? "/admin/stock-transfers/" + editing.id : "/admin/stock-transfers", {
                method: wasEditing ? "PUT" : "POST",
                body: requestBody(),
                auth: true
            });

            if (send) {
                await apiRequest("/admin/stock-transfers/" + saved.id + "/send", { method: "POST", auth: true });
            }

            showToast(send ? "Đã gửi hàng theo phiếu " + saved.code + "." : "Đã lưu phiếu nháp " + saved.code + ".", "success");

            closePanel();

            load(wasEditing ? currentPage : 0);

        } catch (error) {

            handleError(error, showError);

            if (send) {
                load(currentPage);
            }

        } finally {

            saveButton.disabled = false;

            sendButton.disabled = false;

        }

    }


    const STEPS = {
        send: { confirm: "Gửi hàng phiếu {code}? Tồn kho chi nhánh gửi sẽ giảm đi ngay.", done: "Đã gửi hàng theo phiếu {code}." },
        receive: { confirm: "Xác nhận đã nhận đủ hàng của phiếu {code}? Tồn kho chi nhánh nhận sẽ tăng lên.", done: "Đã nhận hàng theo phiếu {code}." },
        cancel: { confirm: "Huỷ phiếu {code}? Hàng đã gửi sẽ được trả lại chi nhánh gửi.", done: "Đã huỷ phiếu {code}." }
    };


    async function runStep(transfer, step) {

        const text = STEPS[step];

        if (!text || !window.confirm(text.confirm.replace("{code}", transfer.code))) {
            return;
        }

        try {

            await apiRequest("/admin/stock-transfers/" + transfer.id + "/" + step, { method: "POST", auth: true });

            showToast(text.done.replace("{code}", transfer.code), "success");

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
