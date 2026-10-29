/* ================= COMBO (admin/combos.html, kế hoạch v5 GĐ10) ================= */

/*
 * API thật (chỉ ADMIN; backend kiểm tra lại vai trò trong DB ở mọi request):
 *   GET    /admin/combos?keyword=&status=&page=&size=   mới nhất trước
 *   POST   /admin/combos   { name, description, mainProductId, startsAt, endsAt, active,
 *   PUT    /admin/combos/{id}   items: [{ productId, discountType: PERCENT|AMOUNT, discountValue }] }
 *   PATCH  /admin/combos/{id}/status { active }
 *   DELETE /admin/combos/{id}                (chỉ khi chưa đơn nào áp combo)
 *   GET    /admin/combos/suggestions?productId=   gợi ý món kèm từ product_relations (AI)
 * Tìm sản phẩm: GET /products?keyword= (công khai). Trạng thái (status) do backend tính.
 */

const COMBO_PAGE_SIZE = 20;

const COMBO_MAX_ITEMS = 10;

const COMBO_STATUS_LABELS = {
    RUNNING: "Đang áp dụng",
    UPCOMING: "Sắp áp dụng",
    PAUSED: "Tạm dừng",
    ENDED: "Đã kết thúc"
};

const COMBO_STATUS_BADGES = {
    RUNNING: "admin-badge-success",
    UPCOMING: "admin-badge-neutral",
    PAUSED: "admin-badge-warning",
    ENDED: "admin-badge-neutral"
};


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const filterForm = document.getElementById("comboFilterForm");

    const keywordInput = document.getElementById("comboKeyword");

    const statusFilter = document.getElementById("comboStatusFilter");

    const tbody = document.getElementById("comboTableBody");

    const pagination = document.getElementById("comboPagination");

    const panel = document.getElementById("comboFormPanel");

    const form = document.getElementById("comboForm");

    const formError = document.getElementById("comboFormError");

    const itemsBox = document.getElementById("cbItems");


    let currentPage = 0;

    let currentCombos = [];

    let requestCounter = 0;

    let editingCombo = null;

    /* sản phẩm chính đã chọn { id, name, imageUrl } và các món kèm [{ productId, name, imageUrl, type, value }] */
    let mainProduct = null;

    let items = [];


    /* ================= SỰ KIỆN ================= */

    filterForm.addEventListener("submit", function (event) {
        event.preventDefault();
        reloadFromFirstPage();
    });

    statusFilter.addEventListener("change", reloadFromFirstPage);

    document.getElementById("newComboBtn").addEventListener("click", function () {
        openForm(null);
    });

    document.getElementById("comboCancelBtn").addEventListener("click", closeForm);

    form.addEventListener("submit", saveCombo);

    document.getElementById("cbSuggestBtn").addEventListener("click", loadSuggestions);

    bindProductSearch("cbMainSearch", "cbMainResults", function (product) {
        chooseMain(product);
    });

    bindProductSearch("cbAddSearch", "cbAddResults", function (product) {
        addItem(product, "PERCENT", 10);
    });

    itemsBox.addEventListener("input", function (event) {

        const row = event.target.closest("[data-index]");

        if (!row) {
            return;
        }

        const item = items[Number(row.dataset.index)];

        if (event.target.matches("select")) {
            item.type = event.target.value;
            applyValueLimits(row, item);
        } else if (event.target.matches('input[type="number"]')) {
            item.value = event.target.value === "" ? null : Number(event.target.value);
        }

        renderPreview();

    });

    itemsBox.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-remove]");

        if (button) {
            items.splice(Number(button.dataset.remove), 1);
            renderItems();
        }

    });

    document.getElementById("cbMainChosen").addEventListener("click", function (event) {

        if (event.target.closest("button")) {
            mainProduct = null;
            renderMain();
        }

    });

    document.getElementById("cbSuggestions").addEventListener("click", function (event) {

        const button = event.target.closest("button[data-suggest]");

        const idea = button && lastSuggestions[Number(button.dataset.suggest)];

        if (idea) {
            addItem({ id: idea.productId, name: idea.name, imageUrl: idea.imageUrl }, "PERCENT", 10);
        }

    });

    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const combo = button && currentCombos.find(function (item) {
            return String(item.id) === button.dataset.id;
        });

        if (!combo) {
            return;
        }

        if (button.dataset.action === "edit") {
            openForm(combo);
        } else if (button.dataset.action === "toggle") {
            toggleActive(combo);
        } else if (button.dataset.action === "delete") {
            confirmDelete(combo);
        }

    });

    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (button && !button.disabled) {
            currentPage = Number(button.dataset.page);
            loadCombos();
        }

    });


    loadCombos();


    /* ================= DANH SÁCH ================= */

    function reloadFromFirstPage() {

        currentPage = 0;

        loadCombos();

    }


    async function loadCombos() {

        const requestId = ++requestCounter;

        const params = new URLSearchParams({ page: String(currentPage), size: String(COMBO_PAGE_SIZE) });

        const keyword = keywordInput.value.trim();

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (statusFilter.value) {
            params.set("status", statusFilter.value);
        }

        tbody.innerHTML = adminEmptyRow(7, "Đang tải…");

        try {

            const page = await apiRequest("/admin/combos?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            if ((page.content || []).length === 0 && currentPage > 0 && page.totalPages > 0) {
                currentPage = page.totalPages - 1;
                loadCombos();
                return;
            }

            currentCombos = page.content || [];

            document.getElementById("comboRowCount").textContent = page.totalElements + " combo";

            tbody.innerHTML = currentCombos.map(renderRow).join("")
                || adminEmptyRow(7, "Chưa có combo nào phù hợp bộ lọc");

            pagination.innerHTML = pagerHtml(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(7, message);
                    pagination.innerHTML = "";
                });
            }

        }

    }


    function discountText(item) {

        return item.discountType === "PERCENT"
            ? "−" + Number(item.discountValue).toLocaleString("vi-VN", { maximumFractionDigits: 2 }) + "%"
            : "−" + formatPrice(Number(item.discountValue));

    }


    function renderRow(combo) {

        const id = escapeHtml(String(combo.id));

        const addOns = (combo.items || []).map(function (item) {
            return escapeHtml(truncateText(item.product.name, 36)) + " <strong>" + escapeHtml(discountText(item)) + "</strong>";
        }).join("<br>");

        const toggle = combo.status === "ENDED"
            ? ""
            : `<button type="button" class="admin-link-btn" data-action="toggle" data-id="${id}">${combo.active ? "Tạm dừng" : "Bật"}</button>`;

        return `
            <tr data-combo-id="${id}">
                <td>
                    <strong>${escapeHtml(combo.name)}</strong>
                    ${combo.description ? `<span class="admin-subtext" title="${escapeHtml(combo.description)}">${escapeHtml(truncateText(combo.description, 50))}</span>` : ""}
                </td>
                <td>
                    ${escapeHtml(truncateText(combo.mainProduct.name, 44))}
                    <span class="admin-subtext">${combo.mainProduct.price ? "từ " + escapeHtml(formatPrice(Number(combo.mainProduct.price))) : ""}</span>
                </td>
                <td class="admin-combo-addons">${addOns}</td>
                <td>
                    ${escapeHtml(formatDateTimeVi(combo.startsAt))}
                    <span class="admin-subtext">→ ${escapeHtml(formatDateTimeVi(combo.endsAt))}</span>
                </td>
                <td>${escapeHtml(String(combo.orderCount))}</td>
                <td>
                    <span class="admin-badge ${COMBO_STATUS_BADGES[combo.status] || "admin-badge-neutral"}">
                        ${escapeHtml(COMBO_STATUS_LABELS[combo.status] || combo.status)}
                    </span>
                </td>
                <td class="admin-actions-cell">
                    <button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>
                    ${toggle}
                    ${combo.deletable ? `<button type="button" class="admin-link-btn admin-link-danger" data-action="delete" data-id="${id}">Xoá</button>` : ""}
                </td>
            </tr>
        `;

    }


    function pagerHtml(page) {

        if (page.totalPages <= 1) {
            return "";
        }

        return `
            <button type="button" class="btn btn-outline-dark" data-page="${page.page - 1}" ${page.page <= 0 ? "disabled" : ""}>‹ TRƯỚC</button>
            <span>Trang ${page.page + 1} / ${page.totalPages}</span>
            <button type="button" class="btn btn-outline-dark" data-page="${page.page + 1}" ${page.page + 1 >= page.totalPages ? "disabled" : ""}>SAU ›</button>
        `;

    }


    /* ================= TÌM SẢN PHẨM ================= */

    function bindProductSearch(inputId, resultsId, onPick) {

        const input = document.getElementById(inputId);

        const results = document.getElementById(resultsId);

        let timer = null;

        let counter = 0;

        let found = [];

        input.addEventListener("input", function () {

            clearTimeout(timer);

            const keyword = input.value.trim();

            if (keyword.length < 2) {
                results.hidden = true;
                return;
            }

            timer = setTimeout(async function () {

                const requestId = ++counter;

                try {

                    const page = await apiRequest("/products?size=8&keyword=" + encodeURIComponent(keyword));

                    if (requestId !== counter) {
                        return;
                    }

                    found = page.content || [];

                    results.innerHTML = found.map(function (product, index) {
                        return productButtonHtml("data-pick", index, product.name, product.primaryImageUrl,
                            product.categoryName || "");
                    }).join("") || `<span class="admin-subtext">Không có sản phẩm nào khớp.</span>`;

                    results.hidden = false;

                } catch (error) {

                    results.innerHTML = `<span class="admin-subtext">${escapeHtml(getErrorMessage(error))}</span>`;
                    results.hidden = false;

                }

            }, 300);

        });

        results.addEventListener("click", function (event) {

            const button = event.target.closest("button[data-pick]");

            const product = button && found[Number(button.dataset.pick)];

            if (product) {
                onPick({ id: product.id, name: product.name, imageUrl: product.primaryImageUrl });
                input.value = "";
                results.hidden = true;
            }

        });

    }


    function productButtonHtml(attribute, index, name, imageUrl, hint) {

        const image = isSafeImageUrl(imageUrl)
            ? `<img src="${escapeHtml(imageUrl.trim())}" alt="" loading="lazy">`
            : `<span class="admin-combo-noimg"></span>`;

        return `
            <button type="button" class="admin-combo-result" ${attribute}="${index}">
                ${image}
                <span>${escapeHtml(truncateText(name, 70))}<small>${escapeHtml(hint || "")}</small></span>
            </button>
        `;

    }


    /* ================= GỢI Ý AI ================= */

    let lastSuggestions = [];

    async function loadSuggestions() {

        const box = document.getElementById("cbSuggestions");

        if (!mainProduct) {
            formError.textContent = "Chọn sản phẩm chính trước để lấy gợi ý.";
            formError.hidden = false;
            return;
        }

        formError.hidden = true;

        box.hidden = false;

        box.innerHTML = `<span class="admin-subtext">Đang lấy gợi ý…</span>`;

        try {

            lastSuggestions = await apiRequest("/admin/combos/suggestions?productId=" + mainProduct.id + "&limit=12", { auth: true });

            const taken = new Set(items.map(function (item) { return item.productId; }));

            const shown = lastSuggestions.map(function (idea, index) {
                return { idea: idea, index: index };
            }).filter(function (entry) {
                return !taken.has(entry.idea.productId);
            });

            box.innerHTML = shown.map(function (entry) {
                const hint = (entry.idea.price ? "từ " + formatPrice(Number(entry.idea.price)) : "")
                    + (entry.idea.strength ? " · độ liên quan " + Number(entry.idea.strength).toFixed(2) : "");
                return productButtonHtml("data-suggest", entry.index, entry.idea.name, entry.idea.imageUrl, hint);
            }).join("") || `<span class="admin-subtext">Chưa có gợi ý nào cho sản phẩm này (chạy lại dữ liệu AI để có quan hệ “thường mua cùng”).</span>`;

        } catch (error) {

            handleError(error, function (message) {
                box.innerHTML = `<span class="admin-subtext">${escapeHtml(message)}</span>`;
            });

        }

    }


    /* ================= THÊM / SỬA ================= */

    function field(id) {

        return document.getElementById(id);

    }


    function chooseMain(product) {

        mainProduct = product;

        /* món kèm không được trùng sản phẩm chính */
        items = items.filter(function (item) { return item.productId !== product.id; });

        renderMain();

        renderItems();

        document.getElementById("cbSuggestions").hidden = true;

    }


    function renderMain() {

        const box = field("cbMainChosen");

        box.hidden = !mainProduct;

        field("cbMainSearch").hidden = Boolean(mainProduct);

        box.innerHTML = mainProduct
            ? productChipHtml(mainProduct.name, mainProduct.imageUrl, "Đổi sản phẩm chính")
            : "";

    }


    function productChipHtml(name, imageUrl, buttonLabel) {

        const image = isSafeImageUrl(imageUrl)
            ? `<img src="${escapeHtml(imageUrl.trim())}" alt="">`
            : `<span class="admin-combo-noimg"></span>`;

        return `${image}<strong>${escapeHtml(name)}</strong><button type="button" class="admin-link-btn">${escapeHtml(buttonLabel)}</button>`;

    }


    function addItem(product, type, value) {

        if (mainProduct && product.id === mainProduct.id) {
            showToast("Món mua kèm không được trùng sản phẩm chính.", "error");
            return;
        }

        if (items.some(function (item) { return item.productId === product.id; })) {
            showToast("Sản phẩm này đã có trong combo.", "error");
            return;
        }

        if (items.length >= COMBO_MAX_ITEMS) {
            showToast("Mỗi combo tối đa " + COMBO_MAX_ITEMS + " món mua kèm.", "error");
            return;
        }

        items.push({ productId: product.id, name: product.name, imageUrl: product.imageUrl, type: type, value: value });

        renderItems();

        const taken = new Set(items.map(function (item) { return item.productId; }));

        document.querySelectorAll("#cbSuggestions button[data-suggest]").forEach(function (button) {
            const idea = lastSuggestions[Number(button.dataset.suggest)];
            if (idea && taken.has(idea.productId)) {
                button.remove();
            }
        });

    }


    function renderItems() {

        field("cbItemCount").textContent = "(" + items.length + " / " + COMBO_MAX_ITEMS + ")";

        itemsBox.innerHTML = items.map(function (item, index) {

            return `
                <div class="admin-combo-item" data-index="${index}">
                    ${productChipHtml(item.name, item.imageUrl, "").replace(/<button.*<\/button>/, "")}
                    <select aria-label="Kiểu giảm">
                        <option value="PERCENT" ${item.type === "PERCENT" ? "selected" : ""}>Giảm %</option>
                        <option value="AMOUNT" ${item.type === "AMOUNT" ? "selected" : ""}>Giảm số tiền (đ)</option>
                    </select>
                    <input type="number" aria-label="Mức giảm" value="${item.value === null ? "" : escapeHtml(String(item.value))}"
                           min="${item.type === "PERCENT" ? "0.01" : "1000"}" max="${item.type === "PERCENT" ? "100" : ""}"
                           step="${item.type === "PERCENT" ? "0.5" : "1000"}">
                    <button type="button" class="admin-link-btn admin-link-danger" data-remove="${index}">Bỏ</button>
                </div>
            `;

        }).join("") || `<span class="admin-subtext">Chưa có món mua kèm nào. Gõ tên sản phẩm bên dưới hoặc bấm “Gợi ý từ AI”.</span>`;

        renderPreview();

    }


    function applyValueLimits(row, item) {

        const input = row.querySelector('input[type="number"]');

        input.min = item.type === "PERCENT" ? "0.01" : "1000";

        input.max = item.type === "PERCENT" ? "100" : "";

        input.step = item.type === "PERCENT" ? "0.5" : "1000";

    }


    /* Ví dụ cho người nhập: giá món kèm sau giảm khi khách mua kèm sản phẩm chính */
    function renderPreview() {

        const preview = field("cbPreview");

        if (items.length === 0) {
            preview.textContent = "Mỗi sản phẩm chính trong giỏ cho giảm giá 1 món kèm cùng loại (2 điện thoại + 2 ốp → cả 2 ốp được giảm). "
                + "Món kèm thuộc nhiều combo lấy mức giảm lớn nhất; giảm combo tính trước voucher và điểm.";
            return;
        }

        preview.textContent = "Ví dụ với món kèm giá 500.000đ: "
            + items.slice(0, 3).map(function (item) {
                if (!(item.value > 0)) {
                    return item.name + ": —";
                }
                const discount = item.type === "PERCENT"
                    ? Math.floor(500000 * Math.min(item.value, 100) / 100)
                    : Math.min(item.value, 500000);
                return truncateText(item.name, 30) + " còn " + formatPrice(500000 - discount);
            }).join("; ") + ".";

    }


    function openForm(combo) {

        editingCombo = combo;

        field("comboFormTitle").textContent = combo ? "Sửa combo" : "Thêm combo";

        const now = new Date();

        field("cbName").value = combo ? combo.name : "";

        field("cbDescription").value = combo && combo.description ? combo.description : "";

        field("cbStart").value = combo ? combo.startsAt.slice(0, 16) : toInputDateTime(now);

        field("cbEnd").value = combo ? combo.endsAt.slice(0, 16) : toInputDateTime(new Date(now.getTime() + 30 * 86400000));

        field("cbActive").checked = combo ? combo.active : true;

        mainProduct = combo ? { id: combo.mainProduct.id, name: combo.mainProduct.name, imageUrl: combo.mainProduct.imageUrl } : null;

        items = combo ? combo.items.map(function (item) {
            return {
                productId: item.product.id,
                name: item.product.name,
                imageUrl: item.product.imageUrl,
                type: item.discountType,
                value: Number(item.discountValue)
            };
        }) : [];

        field("cbSuggestions").hidden = true;

        formError.hidden = true;

        panel.hidden = false;

        renderMain();

        renderItems();

        field(combo ? "cbName" : "cbName").focus();

        panel.scrollIntoView({ behavior: "smooth", block: "start" });

    }


    function closeForm() {

        editingCombo = null;

        panel.hidden = true;

    }


    function toInputDateTime(date) {

        const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);

        return local.toISOString().slice(0, 16);

    }


    function toApiDateTime(value) {

        return value.length === 16 ? value + ":00" : value;

    }


    function readForm() {

        const body = {
            name: field("cbName").value.trim(),
            description: field("cbDescription").value.trim() || null,
            mainProductId: mainProduct ? mainProduct.id : null,
            startsAt: field("cbStart").value,
            endsAt: field("cbEnd").value,
            active: field("cbActive").checked,
            items: items.map(function (item) {
                return { productId: item.productId, discountType: item.type, discountValue: item.value };
            })
        };

        if (!body.name) {
            return { error: "Vui lòng nhập tên combo." };
        }

        if (!body.mainProductId) {
            return { error: "Vui lòng chọn sản phẩm chính." };
        }

        if (!body.startsAt || !body.endsAt) {
            return { error: "Vui lòng chọn thời gian bắt đầu và kết thúc." };
        }

        if (body.endsAt <= body.startsAt) {
            return { error: "Thời gian kết thúc phải sau thời gian bắt đầu." };
        }

        if (body.items.length === 0) {
            return { error: "Chọn ít nhất 1 món mua kèm." };
        }

        for (const item of items) {

            if (!(item.value > 0)) {
                return { error: "Nhập mức giảm cho “" + item.name + "”." };
            }

            if (item.type === "PERCENT" && item.value > 100) {
                return { error: "Giảm theo % của “" + item.name + "” tối đa 100." };
            }

        }

        body.startsAt = toApiDateTime(body.startsAt);

        body.endsAt = toApiDateTime(body.endsAt);

        return { body: body };

    }


    async function saveCombo(event) {

        event.preventDefault();

        const result = readForm();

        if (result.error) {
            formError.textContent = result.error;
            formError.hidden = false;
            return;
        }

        const saveButton = field("comboSaveBtn");

        saveButton.disabled = true;

        formError.hidden = true;

        try {

            const wasEditing = editingCombo !== null;

            const saved = wasEditing
                ? await apiRequest("/admin/combos/" + editingCombo.id, { method: "PUT", body: result.body, auth: true })
                : await apiRequest("/admin/combos", { method: "POST", body: result.body, auth: true });

            showToast((wasEditing ? "Đã lưu combo " : "Đã thêm combo ") + saved.name + ".", "success");

            closeForm();

            if (!wasEditing) {
                currentPage = 0;
            }

            loadCombos();

        } catch (error) {

            handleError(error, function (message) {
                formError.textContent = message + detailsText(error);
                formError.hidden = false;
            });

        } finally {

            saveButton.disabled = false;

        }

    }


    function detailsText(error) {

        if (!error.details || typeof error.details !== "object") {
            return "";
        }

        const parts = Object.keys(error.details).map(function (key) {
            return error.details[key];
        });

        return parts.length > 0 ? " (" + parts.join("; ") + ")" : "";

    }


    /* ================= BẬT / TẮT, XOÁ ================= */

    async function toggleActive(combo) {

        try {

            await apiRequest("/admin/combos/" + combo.id + "/status", {
                method: "PATCH", body: { active: !combo.active }, auth: true
            });

            showToast((combo.active ? "Đã tạm dừng " : "Đã bật ") + combo.name + ".", "success");

        } catch (error) {

            handleError(error, function (message) {
                showToast(message, "error");
            });

        }

        loadCombos();

    }


    function confirmDelete(combo) {

        openConfirmModal({
            title: "Xoá combo " + combo.name + "?",
            message: "Khách sẽ không còn thấy combo này. Không thể hoàn tác.",
            confirmLabel: "XOÁ",
            cancelLabel: "Quay lại",
            onConfirm: async function () {

                try {

                    await apiRequest("/admin/combos/" + combo.id, { method: "DELETE", auth: true });

                    showToast("Đã xoá combo " + combo.name + ".", "success");

                    if (editingCombo && editingCombo.id === combo.id) {
                        closeForm();
                    }

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

                loadCombos();

            }
        });

    }


    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
