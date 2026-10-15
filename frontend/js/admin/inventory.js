/* ================= TỒN KHO (admin/inventory.html) — Phase 7.8 ================= */

/*
 * API thật (backend kiểm tra lại vai trò + chi nhánh trong DB ở mọi request):
 *   GET  /admin/stores/{storeId}/inventory?keyword&outOfStock&page&size     tồn kho 1 chi nhánh
 *        (STAFF: chỉ chi nhánh mình đang được gán; ADMIN: mọi chi nhánh)
 *   GET  /admin/inventory?keyword&outOfStock&page&size                      "Tất cả chi nhánh" (chỉ ADMIN):
 *        mỗi dòng 1 phiên bản + số lượng từng chi nhánh; outOfStock = không chi nhánh nào còn
 *   POST /admin/stores/{storeId}/inventory/stock-in { variantId, quantity, supplierName?, note? }
 *   GET  /admin/stores/{storeId}/inventory/{variantId}/movements?page&size  lịch sử, mới nhất trước
 *   GET  /products?keyword&isActive=true&size, GET /products/{id}/variants   chọn hàng khi nhập kho
 * Bảng chỉ có phiên bản đã từng nhập ở chi nhánh; "Sắp hết" (≤ 5) chỉ tô màu ở trang này.
 */

const INVENTORY_PAGE_SIZE = 20;

const MOVEMENT_PAGE_SIZE = 10;

const STOCK_PICKER_SIZE = 10;

const LOW_STOCK_THRESHOLD = 5;

const STOCK_IN_MAX_QUANTITY = 100000;

const MOVEMENT_TYPES = {
    IN: { label: "Nhập kho", badge: "admin-badge-success" },
    OUT: { label: "Xuất bán", badge: "admin-badge-neutral" },
    RETURN: { label: "Hoàn kho", badge: "admin-badge-info" },
    ADJUSTMENT: { label: "Điều chỉnh", badge: "admin-badge-warning" }
};


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const isAdmin = staff.role === "ADMIN";

    /* Nhân viên chưa được gán chi nhánh: backend trả 403 NO_ACTIVE_STORE_ASSIGNMENT cho mọi thao tác */
    const unassigned = !isAdmin && !staff.storeId;

    const filterForm = document.getElementById("inventoryFilterForm");

    const keywordInput = document.getElementById("inventoryKeyword");

    const storeFilter = document.getElementById("inventoryStoreFilter");

    const outOfStockInput = document.getElementById("inventoryOutOfStock");

    const newStockInButton = document.getElementById("newStockInBtn");

    const thead = document.getElementById("inventoryTableHead");

    const tbody = document.getElementById("inventoryTableBody");

    const pagination = document.getElementById("inventoryPagination");

    const panel = document.getElementById("stockInPanel");

    const form = document.getElementById("stockInForm");

    const formError = document.getElementById("stockInError");

    const saveButton = document.getElementById("stockInSaveBtn");

    const storeSelect = document.getElementById("siStore");

    const chosenProductText = document.getElementById("siChosenProduct");

    const searchBox = document.getElementById("siSearchBox");

    const productKeywordInput = document.getElementById("siProductKeyword");

    const productResults = document.getElementById("siProductResults");

    const variantSelect = document.getElementById("siVariant");


    /* Chi nhánh chọn được ở bộ lọc / form nhập kho: ADMIN = mọi chi nhánh thật, nhân viên = chi nhánh mình */
    let stores = isAdmin ? [] : [{ id: staff.storeId, name: staff.storeName, active: true }];

    let currentPage = 0;

    let currentRows = [];

    /* Chế độ của trang đang hiển thị: storeId của chi nhánh, hoặc null = "Tất cả chi nhánh" */
    let shownStoreId = null;

    let requestCounter = 0;

    const expandedIds = new Set();

    /* "storeId:variantId" → { items, page, totalPages, loading, error } */
    let histories = new Map();

    /* "Tất cả chi nhánh": variantId → storeId đang xem lịch sử */
    const overviewHistoryStore = new Map();

    let pickerCounter = 0;

    let pickerResults = [];

    let chosenProduct = null;

    let variantCounter = 0;


    if (unassigned) {

        setupStoreFilter(storeFilter, staff);

        [keywordInput, outOfStockInput].forEach(function (control) {
            control.disabled = true;
        });

        filterForm.querySelector('button[type="submit"]').disabled = true;

        newStockInButton.hidden = true;

        renderHead(false);

        tbody.innerHTML = adminEmptyRow(5,
            "Tài khoản của bạn chưa được gán vào chi nhánh nào, nên chưa xem / nhập kho được. Hãy báo ADMIN.");

        return;

    }


    if (isAdmin) {

        storeFilter.innerHTML = '<option value="">Tất cả chi nhánh</option>';

        try {

            stores = await loadAllStores();

            fillStoreOptions(storeFilter, [{ value: "", label: "Tất cả chi nhánh" }], stores);

        } catch (error) {

            handleError(error, function (message) {
                showToast("Không tải được danh sách chi nhánh: " + message, "error");
            });

        }

    } else {

        setupStoreFilter(storeFilter, staff);

    }


    filterForm.addEventListener("submit", function (event) {
        event.preventDefault();
        reloadFromFirstPage();
    });

    [storeFilter, outOfStockInput].forEach(function (control) {
        control.addEventListener("change", reloadFromFirstPage);
    });

    newStockInButton.addEventListener("click", function () {
        openStockIn(null);
    });

    document.getElementById("stockInCancelBtn").addEventListener("click", closeStockIn);

    document.getElementById("siProductSearchBtn").addEventListener("click", searchProducts);

    productKeywordInput.addEventListener("keydown", function (event) {

        if (event.key === "Enter") {
            event.preventDefault();
            searchProducts();
        }

    });

    productResults.addEventListener("click", function (event) {

        const button = event.target.closest('button[data-action="choose"]');

        const product = button && pickerResults.find(function (candidate) {
            return String(candidate.id) === button.dataset.id;
        });

        if (product) {
            chooseProduct({ id: product.id, name: product.name }, null);
        }

    });

    chosenProductText.addEventListener("click", function (event) {

        if (event.target.closest('button[data-action="change-product"]')) {
            clearChosenProduct();
            productKeywordInput.focus();
        }

    });

    form.addEventListener("submit", saveStockIn);


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const action = button.dataset.action;

        if (action === "toggle") {
            toggleDetail(button.dataset.id);
        } else if (action === "restock") {
            restock(button.dataset.storeId, button.dataset.variantId);
        } else if (action === "history") {
            showStoreHistory(button.dataset.variantId, button.dataset.storeId);
        } else if (action === "more-history") {
            loadHistory(button.dataset.storeId, button.dataset.variantId, true);
        }

    });


    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        loadInventory();

    });


    loadInventory();


    /* ================= TẢI DANH SÁCH ================= */

    function reloadFromFirstPage() {

        currentPage = 0;

        expandedIds.clear();

        overviewHistoryStore.clear();

        loadInventory();

    }


    /* Chi nhánh đang lọc: nhân viên luôn là chi nhánh mình; ADMIN rỗng = "Tất cả chi nhánh" */

    function selectedStoreId() {

        return isAdmin ? (storeFilter.value || null) : String(staff.storeId);

    }


    async function loadInventory() {

        const requestId = ++requestCounter;

        const storeId = selectedStoreId();

        const params = new URLSearchParams({ page: String(currentPage), size: String(INVENTORY_PAGE_SIZE) });

        const keyword = keywordInput.value.trim();

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (outOfStockInput.checked) {
            params.set("outOfStock", "true");
        }


        renderHead(storeId === null);

        tbody.innerHTML = adminEmptyRow(5, "Đang tải…");


        try {

            const page = await apiRequest(
                (storeId === null ? "/admin/inventory?" : "/admin/stores/" + storeId + "/inventory?") + params.toString(),
                { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            if ((page.content || []).length === 0 && currentPage > 0 && page.totalPages > 0) {

                currentPage = page.totalPages - 1;

                loadInventory();

                return;

            }

            shownStoreId = storeId;

            currentRows = page.content || [];

            /* Lịch sử đọc lại sau mỗi lần tải (vd. vừa nhập kho / có đơn vừa xác nhận) */
            histories = new Map();

            renderTable(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(5, message);
                    pagination.innerHTML = "";
                });
            }

        }

    }


    function findRow(variantId) {

        return currentRows.find(function (row) {
            return String(row.variantId) === String(variantId);
        });

    }


    function storeName(storeId) {

        const store = stores.find(function (candidate) {
            return String(candidate.id) === String(storeId);
        });

        return store ? store.name : "chi nhánh #" + storeId;

    }


    /* ================= HIỂN THỊ ================= */

    function renderHead(overview) {

        document.getElementById("inventoryTableTitle").textContent = overview
            ? "Tồn kho tất cả chi nhánh"
            : "Tồn kho " + (isAdmin ? storeName(storeFilter.value) : staff.storeName);

        thead.innerHTML = overview
            ? "<tr><th></th><th>Sản phẩm</th><th>Tổng tồn</th><th>Theo chi nhánh</th><th>Thao tác</th></tr>"
            : "<tr><th></th><th>Sản phẩm</th><th>Tồn kho</th><th>Cập nhật</th><th>Thao tác</th></tr>";

    }


    function renderTable(page) {

        document.getElementById("inventoryRowCount").textContent = page.totalElements + " phiên bản";

        tbody.innerHTML = currentRows.map(renderRows).join("") || adminEmptyRow(5, emptyMessage());

        renderPagination(page);

        /* Dòng đang mở: nạp lại lịch sử */
        currentRows.forEach(function (row) {

            if (!expandedIds.has(String(row.variantId))) {
                return;
            }

            const historyStoreId = shownStoreId !== null ? shownStoreId : overviewHistoryStore.get(String(row.variantId));

            if (historyStoreId) {
                loadHistory(historyStoreId, row.variantId, false);
            }

        });

    }


    function emptyMessage() {

        if (keywordInput.value.trim() || outOfStockInput.checked) {
            return "Không có phiên bản nào phù hợp bộ lọc.";
        }

        return shownStoreId === null
            ? "Chưa chi nhánh nào nhập kho. Bấm \"+ NHẬP KHO\" để nhập lô hàng đầu tiên."
            : "Chi nhánh này chưa nhập phiên bản nào. Bấm \"+ NHẬP KHO\" để nhập lô hàng đầu tiên.";

    }


    function renderRows(row) {

        const id = escapeHtml(String(row.variantId));

        const expanded = expandedIds.has(String(row.variantId));

        const overview = shownStoreId === null;

        const cells = overview
            ? `
                <td>${quantityHtml(row.totalQuantity)}</td>
                <td>${escapeHtml(storesSummary(row.stores || []))}</td>
                <td class="admin-actions-cell">
                    <button type="button" class="admin-link-btn" data-action="restock" data-store-id="" data-variant-id="${id}">Nhập kho</button>
                </td>
            `
            : `
                <td>${quantityHtml(row.quantity)}</td>
                <td>${escapeHtml(row.updatedAt ? formatDateTimeVi(row.updatedAt) : "—")}</td>
                <td class="admin-actions-cell">
                    <button type="button" class="admin-link-btn" data-action="restock" data-store-id="${escapeHtml(String(shownStoreId))}" data-variant-id="${id}">Nhập thêm</button>
                </td>
            `;


        return `
            <tr data-variant-id="${id}">
                <td>
                    <button
                        type="button"
                        class="admin-row-toggle"
                        data-action="toggle"
                        data-id="${id}"
                        aria-label="${overview ? "Xem tồn kho từng chi nhánh của" : "Xem lịch sử nhập / xuất của"} ${escapeHtml(row.productName + " - " + row.variantName)}"
                        aria-expanded="${expanded}"
                    >${expanded ? "▾" : "▸"}</button>
                </td>
                <td>${productCellHtml(row)}</td>
                ${cells}
            </tr>
            <tr class="js-inventory-detail" data-variant-id="${id}" ${expanded ? "" : "hidden"}>
                <td></td>
                <td colspan="4">
                    <div class="admin-order-detail js-inventory-detail-body">${detailHtml(row)}</div>
                </td>
            </tr>
        `;

    }


    function productCellHtml(row) {

        const thumb = isSafeImageUrl(row.imageUrl)
            ? `<img class="admin-product-thumb" src="${escapeHtml(row.imageUrl.trim())}" alt="" loading="lazy">`
            : `<span class="admin-product-thumb admin-product-thumb--empty" title="Chưa có ảnh">—</span>`;

        const meta = [row.variantName, row.skuVariant].filter(Boolean).join(" · ");

        return `
            <div class="admin-product-cell">
                ${thumb}
                <div>
                    <a href="${escapeHtml(getProductDetailUrl(row.productId))}" target="_blank" rel="noopener">${escapeHtml(row.productName)}</a>
                    <div class="admin-subtext">${escapeHtml(meta)}</div>
                </div>
            </div>
        `;

    }


    /* Số lượng + "Hết hàng" (0) / "Sắp hết" (1–5) */

    function quantityHtml(quantity) {

        const value = Number(quantity) || 0;

        let badge = "";

        if (value <= 0) {
            badge = '<span class="admin-badge admin-badge-danger">Hết hàng</span>';
        } else if (value <= LOW_STOCK_THRESHOLD) {
            badge = '<span class="admin-badge admin-badge-warning">Sắp hết</span>';
        }

        return `<strong class="admin-stock-qty">${escapeHtml(String(value))}</strong> ${badge}`;

    }


    function storesSummary(storeRows) {

        const empty = storeRows.filter(function (store) {
            return store.quantity <= 0;
        }).length;

        return storeRows.length + " chi nhánh" + (empty > 0 ? " · " + empty + " hết hàng" : "");

    }


    function detailHtml(row) {

        if (shownStoreId !== null) {
            return historyContainerHtml(shownStoreId, row.variantId);
        }


        const variantId = escapeHtml(String(row.variantId));

        const historyStoreId = overviewHistoryStore.get(String(row.variantId));

        const storeRows = (row.stores || []).map(function (store) {

            const storeId = escapeHtml(String(store.storeId));

            const selected = String(store.storeId) === String(historyStoreId);

            return `
                <tr>
                    <td>
                        ${escapeHtml(store.storeName)}
                        ${store.storeActive ? "" : ' <span class="admin-badge admin-badge-neutral">Tạm đóng</span>'}
                    </td>
                    <td>${quantityHtml(store.quantity)}</td>
                    <td>${escapeHtml(store.updatedAt ? formatDateTimeVi(store.updatedAt) : "—")}</td>
                    <td class="admin-actions-cell">
                        <button type="button" class="admin-link-btn" data-action="restock" data-store-id="${storeId}" data-variant-id="${variantId}">Nhập thêm</button>
                        <button type="button" class="admin-link-btn" data-action="history" data-store-id="${storeId}" data-variant-id="${variantId}" aria-pressed="${selected}">${selected ? "▾ Lịch sử" : "Lịch sử"}</button>
                    </td>
                </tr>
            `;

        }).join("");


        const history = historyStoreId
            ? `<h4 class="admin-inventory-history-title">Lịch sử tại ${escapeHtml(storeNameOf(row, historyStoreId))}</h4>`
                + historyContainerHtml(historyStoreId, row.variantId)
            : '<p class="admin-subtext">Bấm "Lịch sử" ở một chi nhánh để xem các lần nhập / xuất.</p>';


        return `
            <table class="admin-table">
                <thead>
                    <tr><th>Chi nhánh</th><th>Tồn kho</th><th>Cập nhật</th><th>Thao tác</th></tr>
                </thead>
                <tbody>${storeRows}</tbody>
            </table>
            <div class="admin-inventory-history">${history}</div>
        `;

    }


    function storeNameOf(row, storeId) {

        const store = (row.stores || []).find(function (candidate) {
            return String(candidate.storeId) === String(storeId);
        });

        return store ? store.storeName : storeName(storeId);

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


    function toggleDetail(variantId) {

        const detailRow = tbody.querySelector('.js-inventory-detail[data-variant-id="' + variantId + '"]');

        const button = tbody.querySelector('button[data-action="toggle"][data-id="' + variantId + '"]');

        if (!detailRow || !button) {
            return;
        }

        detailRow.hidden = !detailRow.hidden;

        if (detailRow.hidden) {
            expandedIds.delete(variantId);
        } else {
            expandedIds.add(variantId);
        }

        button.textContent = detailRow.hidden ? "▸" : "▾";

        button.setAttribute("aria-expanded", String(!detailRow.hidden));


        if (!detailRow.hidden && shownStoreId !== null && !histories.has(historyKey(shownStoreId, variantId))) {
            loadHistory(shownStoreId, variantId, false);
        }

    }


    /* "Tất cả chi nhánh": chọn chi nhánh để xem lịch sử của phiên bản */

    function showStoreHistory(variantId, storeId) {

        const row = findRow(variantId);

        const body = tbody.querySelector('.js-inventory-detail[data-variant-id="' + variantId + '"] .js-inventory-detail-body');

        if (!row || !body) {
            return;
        }

        if (overviewHistoryStore.get(String(variantId)) === String(storeId)) {
            overviewHistoryStore.delete(String(variantId));
        } else {
            overviewHistoryStore.set(String(variantId), String(storeId));
        }

        body.innerHTML = detailHtml(row);

        if (overviewHistoryStore.has(String(variantId)) && !histories.has(historyKey(storeId, variantId))) {
            loadHistory(storeId, variantId, false);
        }

    }


    /* ================= LỊCH SỬ NHẬP / XUẤT ================= */

    function historyKey(storeId, variantId) {

        return storeId + ":" + variantId;

    }


    function historyContainerHtml(storeId, variantId) {

        const key = historyKey(storeId, variantId);

        return `<div class="admin-inventory-movements" data-history="${escapeHtml(key)}">${historyInnerHtml(storeId, variantId, histories.get(key))}</div>`;

    }


    function renderHistory(storeId, variantId) {

        const key = historyKey(storeId, variantId);

        tbody.querySelectorAll('[data-history="' + key + '"]').forEach(function (container) {
            container.innerHTML = historyInnerHtml(storeId, variantId, histories.get(key));
        });

    }


    async function loadHistory(storeId, variantId, append) {

        const key = historyKey(storeId, variantId);

        const previous = histories.get(key);

        if (previous && previous.loading) {
            return;
        }

        const state = {
            items: append && previous ? previous.items : [],
            page: append && previous ? previous.page : -1,
            totalPages: append && previous ? previous.totalPages : 0,
            loading: true,
            error: ""
        };

        histories.set(key, state);

        renderHistory(storeId, variantId);


        try {

            const result = await apiRequest("/admin/stores/" + storeId + "/inventory/" + variantId + "/movements?"
                + new URLSearchParams({ page: String(state.page + 1), size: String(MOVEMENT_PAGE_SIZE) }).toString(),
                { auth: true });

            if (histories.get(key) !== state) {
                return;
            }

            state.items = state.items.concat(result.content || []);

            state.page = result.page;

            state.totalPages = result.totalPages;

        } catch (error) {

            if (histories.get(key) !== state) {
                return;
            }

            handleError(error, function (message) {
                state.error = message;
            });

        }

        state.loading = false;

        renderHistory(storeId, variantId);

    }


    function historyInnerHtml(storeId, variantId, state) {

        if (!state || (state.loading && state.items.length === 0)) {
            return '<p class="admin-subtext">Đang tải lịch sử…</p>';
        }

        if (state.items.length === 0) {
            return `<p class="admin-subtext">${escapeHtml(state.error || "Chưa có lần nhập / xuất nào.")}</p>`;
        }


        const rows = state.items.map(function (movement) {

            const type = MOVEMENT_TYPES[movement.type] || { label: movement.type, badge: "admin-badge-neutral" };

            const change = Number(movement.quantityChange) || 0;

            const order = movement.orderCode
                ? `<a href="${escapeHtml(siteUrl("admin/orders.html?keyword=" + encodeURIComponent(movement.orderCode)))}">${escapeHtml(movement.orderCode)}</a>`
                : "—";

            const source = [movement.supplierName, movement.note].filter(Boolean);

            return `
                <tr>
                    <td>${escapeHtml(formatDateTimeVi(movement.createdAt))}</td>
                    <td><span class="admin-badge ${type.badge}">${escapeHtml(type.label)}</span></td>
                    <td class="${change >= 0 ? "admin-stock-in" : "admin-stock-out"}">${escapeHtml((change > 0 ? "+" : "") + change)}</td>
                    <td>${order}</td>
                    <td>
                        ${movement.supplierName ? escapeHtml(movement.supplierName) : ""}
                        ${movement.note ? `<span class="admin-subtext">${escapeHtml(movement.note)}</span>` : ""}
                        ${source.length === 0 ? "—" : ""}
                    </td>
                    <td>${escapeHtml(movement.createdByName || "—")}</td>
                </tr>
            `;

        }).join("");


        let footer = "";

        if (state.error) {
            footer = `<p class="admin-subtext">${escapeHtml(state.error)}</p>`;
        } else if (state.loading) {
            footer = '<p class="admin-subtext">Đang tải thêm…</p>';
        } else if (state.page + 1 < state.totalPages) {
            footer = `<button type="button" class="btn btn-outline-dark admin-action-btn" data-action="more-history" data-store-id="${escapeHtml(String(storeId))}" data-variant-id="${escapeHtml(String(variantId))}">XEM THÊM</button>`;
        }


        return `
            <table class="admin-table">
                <thead>
                    <tr><th>Thời gian</th><th>Loại</th><th>Số lượng</th><th>Đơn hàng</th><th>Nhà cung cấp / ghi chú</th><th>Người thực hiện</th></tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
            ${footer}
        `;

    }


    /* ================= NHẬP KHO ================= */

    /* "Nhập thêm" / "Nhập kho" ở một dòng: điền sẵn sản phẩm + phiên bản (+ chi nhánh nếu có) */

    function restock(storeId, variantId) {

        const row = findRow(variantId);

        if (!row) {
            return;
        }

        openStockIn({
            storeId: storeId || null,
            product: { id: row.productId, name: row.productName },
            variantId: row.variantId
        });

    }


    function openStockIn(prefill) {

        formError.hidden = true;

        form.reset();

        fillStockInStores(prefill && prefill.storeId);

        if (prefill) {
            chooseProduct(prefill.product, prefill.variantId);
        } else {
            clearChosenProduct();
        }

        panel.hidden = false;

        panel.scrollIntoView({ block: "nearest" });

        (prefill ? document.getElementById("siQuantity") : productKeywordInput).focus();

    }


    function closeStockIn() {

        form.reset();

        clearChosenProduct();

        formError.hidden = true;

        panel.hidden = true;

    }


    /* Chi nhánh mặc định: chi nhánh được truyền vào → chi nhánh đang lọc → chi nhánh đang mở đầu tiên */

    function fillStockInStores(preferredStoreId) {

        if (!isAdmin) {

            storeSelect.innerHTML =
                `<option value="${escapeHtml(String(staff.storeId))}">${escapeHtml(staff.storeName)}</option>`;

            storeSelect.disabled = true;

            return;

        }


        storeSelect.innerHTML = stores.length === 0
            ? '<option value="">Chưa có chi nhánh nào</option>'
            : stores.map(function (store) {
                return `<option value="${escapeHtml(String(store.id))}">${escapeHtml(storeOptionLabel(store))}</option>`;
            }).join("");

        const firstOpen = stores.find(function (store) {
            return store.active !== false;
        });

        const preferred = preferredStoreId || storeFilter.value || (firstOpen && String(firstOpen.id));

        if (preferred && stores.some(function (store) { return String(store.id) === String(preferred); })) {
            storeSelect.value = String(preferred);
        }

    }


    async function searchProducts() {

        const requestId = ++pickerCounter;

        /* Chỉ sản phẩm đang bán: sản phẩm ẩn (vd. bản cũ TGDD-OLD-*) không nhập kho */
        const params = new URLSearchParams({ size: String(STOCK_PICKER_SIZE), isActive: "true" });

        const keyword = productKeywordInput.value.trim();

        if (keyword) {
            params.set("keyword", keyword);
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

                    const thumb = isSafeImageUrl(product.primaryImageUrl)
                        ? `<img class="admin-product-thumb" src="${escapeHtml(product.primaryImageUrl.trim())}" alt="" loading="lazy">`
                        : '<span class="admin-product-thumb admin-product-thumb--empty" title="Chưa có ảnh">—</span>';

                    return `
                        <li>
                            ${thumb}
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


    /* Chọn sản phẩm → nạp phiên bản; variantId: phiên bản chọn sẵn (null = để người dùng chọn) */

    async function chooseProduct(product, variantId) {

        chosenProduct = product;

        chosenProductText.innerHTML =
            `Đã chọn: <strong>${escapeHtml(product.name)}</strong> `
            + '<button type="button" class="admin-link-btn" data-action="change-product">Đổi sản phẩm</button>';

        chosenProductText.hidden = false;

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

                    return `<option value="${escapeHtml(String(variant.id))}">${escapeHtml(label)}</option>`;

                }).join("");

            if (variantId != null) {
                variantSelect.value = String(variantId);
            }

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

        chosenProductText.hidden = true;

        chosenProductText.innerHTML = "";

        searchBox.hidden = false;

        productResults.hidden = true;

        productResults.innerHTML = "";

        variantSelect.innerHTML = '<option value="">Chọn sản phẩm trước</option>';

        variantSelect.disabled = true;

    }


    /* StockInRequest; trả về { storeId, body } hoặc { error } */

    function readStockIn() {

        const storeId = storeSelect.value;

        const variantId = variantSelect.value;

        const quantityText = document.getElementById("siQuantity").value.trim();

        const quantity = Number(quantityText);

        if (!storeId) {
            return { error: "Vui lòng chọn chi nhánh (thêm chi nhánh ở trang Chi nhánh nếu chưa có)." };
        }

        if (!chosenProduct || !variantId) {
            return { error: "Vui lòng chọn sản phẩm và phiên bản cần nhập." };
        }

        if (!/^\d+$/.test(quantityText) || quantity < 1 || quantity > STOCK_IN_MAX_QUANTITY) {
            return { error: "Số lượng nhập phải là số nguyên từ 1 đến " + formatNumberVi(STOCK_IN_MAX_QUANTITY) + "." };
        }

        const supplier = document.getElementById("siSupplier").value.trim();

        const note = document.getElementById("siNote").value.trim();

        return {
            storeId: storeId,
            body: {
                variantId: Number(variantId),
                quantity: quantity,
                supplierName: supplier || null,
                note: note || null
            }
        };

    }


    async function saveStockIn(event) {

        event.preventDefault();


        const result = readStockIn();

        if (result.error) {

            formError.textContent = result.error;

            formError.hidden = false;

            return;

        }


        saveButton.disabled = true;

        try {

            const saved = await apiRequest("/admin/stores/" + result.storeId + "/inventory/stock-in",
                { method: "POST", body: result.body, auth: true });

            showToast("Đã nhập " + formatNumberVi(result.body.quantity) + " × " + saved.productName + " - "
                + saved.variantName + " vào " + storeName(result.storeId) + ". Tồn kho hiện tại: "
                + formatNumberVi(saved.quantity) + ".", "success");

            closeStockIn();

            loadInventory();

        } catch (error) {

            handleError(error, function (message) {
                formError.textContent = message + detailsText(error);
                formError.hidden = false;
            });

        } finally {

            saveButton.disabled = false;

        }

    }


    function formatNumberVi(value) {

        return Number(value).toLocaleString("vi-VN");

    }


    /* " (Số lượng: …)" từ error.details của VALIDATION_ERROR */

    function detailsText(error) {

        if (!error.details || typeof error.details !== "object") {
            return "";
        }

        const parts = Object.keys(error.details).map(function (key) {
            return error.details[key];
        });

        return parts.length > 0 ? " (" + parts.join("; ") + ")" : "";

    }


    /*
     * Hết phiên (401, kể cả sau khi đã thử làm mới token) → kiểm tra lại phiên
     * và về trang đăng nhập; lỗi khác (403, 404…) → hiển thị bằng show(message).
     */

    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
