/* ================= KHUYẾN MÃI (admin/promotions.html) ================= */

/*
 * API thật (chỉ ADMIN; backend kiểm tra lại vai trò trong DB ở mọi request):
 *   GET    /admin/promotions?keyword=&status=&fromDate=&toDate=&page=&size=  mới nhất trước
 *   POST   /admin/promotions          { name, description, discountType, discountValue, maxDiscountAmount,
 *   PUT    /admin/promotions/{id}       startDate, endDate, active, products: [{ productId, discountType?, discountValue? }] }
 *   DELETE /admin/promotions/{id}
 * Sản phẩm được tìm qua GET /products. Trạng thái (status) do backend tính, trang chỉ đổi sang nhãn.
 * Thời điểm kết thúc không tính vào chương trình: [bắt đầu, kết thúc).
 */

const PROMOTION_PAGE_SIZE = 20;

const PROMOTION_PICKER_SIZE = 10;

const PROMOTION_MAX_PRODUCTS = 200;

const PROMOTION_STATUS_LABELS = {
    RUNNING: "Đang diễn ra",
    UPCOMING: "Sắp diễn ra",
    PAUSED: "Tạm dừng",
    ENDED: "Đã kết thúc"
};

const PROMOTION_STATUS_BADGES = {
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


    const filterForm = document.getElementById("promotionFilterForm");

    const keywordInput = document.getElementById("promotionKeyword");

    const statusFilter = document.getElementById("promotionStatusFilter");

    const fromDateInput = document.getElementById("promotionFromDate");

    const toDateInput = document.getElementById("promotionToDate");

    const filterError = document.getElementById("promotionFilterError");

    const tbody = document.getElementById("promotionTableBody");

    const pagination = document.getElementById("promotionPagination");

    const panel = document.getElementById("promotionFormPanel");

    const form = document.getElementById("promotionForm");

    const formError = document.getElementById("promotionFormError");

    const productKeywordInput = document.getElementById("pmProductKeyword");

    const productResults = document.getElementById("pmProductResults");

    const selectedBody = document.getElementById("pmSelectedBody");


    let currentPage = 0;

    let currentPromotions = [];

    let requestCounter = 0;

    let pickerCounter = 0;

    let editingPromotion = null;

    let pickerResults = [];

    /* { productId, name, basePrice, imageUrl, available, discountType, discountValue } — null = mặc định */
    let selected = [];


    /* ================= SỰ KIỆN ================= */

    filterForm.addEventListener("submit", function (event) {

        event.preventDefault();

        reloadFromFirstPage();

    });

    [statusFilter, fromDateInput, toDateInput].forEach(function (control) {
        control.addEventListener("change", reloadFromFirstPage);
    });


    document.getElementById("newPromotionBtn").addEventListener("click", function () {
        openForm(null);
    });

    document.getElementById("promotionCancelBtn").addEventListener("click", closeForm);

    form.addEventListener("submit", savePromotion);

    ["pmDiscountType", "pmDiscountValue", "pmMaxDiscount"].forEach(function (id) {
        field(id).addEventListener("input", refreshCampaignDiscount);
    });


    document.getElementById("pmProductSearchBtn").addEventListener("click", searchProducts);

    /* Enter trong ô tìm sản phẩm: tìm, không gửi form */
    productKeywordInput.addEventListener("keydown", function (event) {

        if (event.key === "Enter") {

            event.preventDefault();

            searchProducts();

        }

    });

    productResults.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action='add']");

        if (button) {
            addProduct(button.dataset.id);
        }

    });


    selectedBody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action='remove']");

        if (button) {
            removeProduct(button.dataset.id);
        }

    });

    selectedBody.addEventListener("change", function (event) {

        const select = event.target.closest("select[data-role='type']");

        const item = select && findSelected(select.dataset.id);

        if (!item) {
            return;
        }

        item.discountType = select.value || null;

        if (!item.discountType) {
            item.discountValue = null;
        }

        renderSelected();

        const valueInput = selectedBody.querySelector("input[data-role='value'][data-id='" + item.productId + "']");

        if (valueInput && !valueInput.hidden) {
            valueInput.focus();
        }

    });

    selectedBody.addEventListener("input", function (event) {

        const input = event.target.closest("input[data-role='value']");

        const item = input && findSelected(input.dataset.id);

        if (!item) {
            return;
        }

        item.discountValue = input.value.trim() === "" ? null : Number(input.value);

        updatePreview(item);

    });


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const promotion = button && findPromotion(button.dataset.id);

        if (!promotion) {
            return;
        }

        if (button.dataset.action === "edit") {
            openForm(promotion);
        } else if (button.dataset.action === "toggle") {
            toggleActive(promotion);
        } else if (button.dataset.action === "delete") {
            confirmDelete(promotion);
        }

    });


    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        loadPromotions();

    });


    loadPromotions();


    /* ================= DANH SÁCH ================= */

    function reloadFromFirstPage() {

        currentPage = 0;

        loadPromotions();

    }


    async function loadPromotions() {

        const fromDate = fromDateInput.value;

        const toDate = toDateInput.value;

        if (fromDate && toDate && fromDate > toDate) {

            filterError.textContent = "Ngày bắt đầu lọc phải trước hoặc trùng ngày kết thúc lọc.";

            filterError.hidden = false;

            return;

        }

        filterError.hidden = true;


        const requestId = ++requestCounter;

        const params = new URLSearchParams({
            page: String(currentPage),
            size: String(PROMOTION_PAGE_SIZE)
        });

        const keyword = keywordInput.value.trim();

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (statusFilter.value) {
            params.set("status", statusFilter.value);
        }

        if (fromDate) {
            params.set("fromDate", fromDate);
        }

        if (toDate) {
            params.set("toDate", toDate);
        }


        tbody.innerHTML = adminEmptyRow(6, "Đang tải…");


        try {

            const page = await apiRequest("/admin/promotions?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            /* Dòng cuối của trang cuối vừa bị xoá → lùi một trang */
            if ((page.content || []).length === 0 && currentPage > 0 && page.totalPages > 0) {

                currentPage = page.totalPages - 1;

                loadPromotions();

                return;

            }

            currentPromotions = page.content || [];

            renderTable(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(6, message);
                    pagination.innerHTML = "";
                });
            }

        }

    }


    function findPromotion(id) {

        return currentPromotions.find(function (promotion) {
            return String(promotion.id) === String(id);
        });

    }


    function renderTable(page) {

        document.getElementById("promotionRowCount").textContent = page.totalElements + " chương trình";

        tbody.innerHTML = currentPromotions.map(renderRow).join("") ||
            adminEmptyRow(6, "Chưa có chương trình khuyến mãi nào phù hợp bộ lọc");

        renderPagination(page);

    }


    function renderRow(promotion) {

        const id = escapeHtml(String(promotion.id));

        const products = promotion.products || [];

        const productNames = products.map(function (product) {
            return product.productName;
        }).join(", ");

        const capText = promotion.maxDiscountAmount
            ? `<span class="admin-subtext">Tối đa ${escapeHtml(formatPrice(Number(promotion.maxDiscountAmount)))}</span>`
            : "";

        const overrides = products.filter(function (product) {
            return product.override;
        }).length;

        const toggleButton = promotion.status === "ENDED"
            ? ""
            : `<button type="button" class="admin-link-btn" data-action="toggle" data-id="${id}">${promotion.active ? "Tạm dừng" : "Bật"}</button>`;


        return `
            <tr data-promotion-id="${id}">
                <td>
                    <strong>${escapeHtml(promotion.name)}</strong>
                    <span class="admin-subtext">#${id} · ${escapeHtml(promotion.createdByName || "")}</span>
                </td>
                <td>
                    ${escapeHtml(discountLabel(promotion.discountType, promotion.discountValue))}
                    ${capText}
                    ${overrides > 0 ? `<span class="admin-subtext">${overrides} sản phẩm có mức riêng</span>` : ""}
                </td>
                <td>
                    ${escapeHtml(formatDateTimeVi(promotion.startDate))}
                    <span class="admin-subtext">→ ${escapeHtml(formatDateTimeVi(promotion.endDate))}</span>
                </td>
                <td>
                    ${products.length} sản phẩm
                    <span class="admin-subtext" title="${escapeHtml(productNames)}">${escapeHtml(truncateText(productNames, 60))}</span>
                </td>
                <td>
                    <span class="admin-badge ${PROMOTION_STATUS_BADGES[promotion.status] || "admin-badge-neutral"}">
                        ${escapeHtml(PROMOTION_STATUS_LABELS[promotion.status] || promotion.status)}
                    </span>
                </td>
                <td class="admin-actions-cell">
                    <button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>
                    ${toggleButton}
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


    /* ================= THÊM / SỬA ================= */

    function field(id) {

        return document.getElementById(id);

    }


    function openForm(promotion) {

        editingPromotion = promotion;

        field("promotionFormTitle").textContent = promotion
            ? "Sửa chương trình #" + promotion.id
            : "Thêm chương trình khuyến mãi";

        const now = new Date();

        field("pmName").value = promotion ? promotion.name : "";

        field("pmDescription").value = promotion && promotion.description ? promotion.description : "";

        field("pmDiscountType").value = promotion ? promotion.discountType : "PERCENTAGE";

        field("pmDiscountValue").value = promotion ? Number(promotion.discountValue) : "";

        field("pmMaxDiscount").value = promotion && promotion.maxDiscountAmount ? Number(promotion.maxDiscountAmount) : "";

        field("pmStartDate").value = promotion ? promotion.startDate.slice(0, 16) : toInputDateTime(now);

        field("pmEndDate").value = promotion
            ? promotion.endDate.slice(0, 16)
            : toInputDateTime(new Date(now.getTime() + 7 * 86400000));

        field("pmActive").checked = promotion ? promotion.active : true;


        selected = promotion
            ? (promotion.products || []).map(function (product) {
                return {
                    productId: product.productId,
                    name: product.productName,
                    basePrice: Number(product.basePrice),
                    imageUrl: product.primaryImageUrl,
                    available: product.productAvailable,
                    discountType: product.override ? product.discountType : null,
                    discountValue: product.override ? Number(product.discountValue) : null
                };
            })
            : [];

        pickerResults = [];

        pickerCounter++;

        productKeywordInput.value = "";

        productResults.hidden = true;

        productResults.innerHTML = "";

        formError.hidden = true;

        refreshCampaignDiscount();

        panel.hidden = false;

        field("pmName").focus();

        panel.scrollIntoView({ behavior: "smooth", block: "start" });

    }


    function closeForm() {

        editingPromotion = null;

        selected = [];

        pickerCounter++;

        panel.hidden = true;

    }


    /* "2026-10-03T09:05" theo giờ máy, đúng định dạng của <input type="datetime-local"> */

    function toInputDateTime(date) {

        const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);

        return local.toISOString().slice(0, 16);

    }


    /* "2026-10-03T09:05" → "2026-10-03T09:05:00" cho LocalDateTime của backend */

    function toApiDateTime(value) {

        return value.length === 16 ? value + ":00" : value;

    }


    function numberOrNull(id) {

        const value = field(id).value.trim();

        return value === "" ? null : Number(value);

    }


    /* Body cho POST / PUT, kiểm tra trước những gì backend sẽ từ chối */

    function readForm() {

        const body = {
            name: field("pmName").value.trim(),
            description: field("pmDescription").value.trim() || null,
            discountType: field("pmDiscountType").value,
            discountValue: numberOrNull("pmDiscountValue"),
            maxDiscountAmount: numberOrNull("pmMaxDiscount"),
            startDate: field("pmStartDate").value,
            endDate: field("pmEndDate").value,
            active: field("pmActive").checked
        };


        if (!body.name) {
            return { error: "Vui lòng nhập tên chương trình." };
        }

        const discountProblem = discountError(body.discountType, body.discountValue, "Mức giảm của chương trình");

        if (discountProblem) {
            return { error: discountProblem };
        }

        if (body.maxDiscountAmount !== null && !(body.maxDiscountAmount > 0)) {
            return { error: "Giảm tối đa phải lớn hơn 0 (hoặc để trống)." };
        }

        if (!body.startDate || !body.endDate) {
            return { error: "Vui lòng chọn thời gian bắt đầu và kết thúc." };
        }

        if (body.endDate <= body.startDate) {
            return { error: "Thời gian kết thúc phải sau thời gian bắt đầu." };
        }

        if (selected.length === 0) {
            return { error: "Vui lòng chọn ít nhất một sản phẩm." };
        }

        for (const item of selected) {

            if (item.discountType) {

                const problem = discountError(item.discountType, item.discountValue, "Mức giảm riêng của " + item.name);

                if (problem) {
                    return { error: problem };
                }

            }

        }


        body.startDate = toApiDateTime(body.startDate);

        body.endDate = toApiDateTime(body.endDate);

        body.products = selected.map(function (item) {
            return {
                productId: item.productId,
                discountType: item.discountType,
                discountValue: item.discountType ? item.discountValue : null
            };
        });

        return { body: body };

    }


    function discountError(type, value, label) {

        if (value === null || !(value > 0)) {
            return label + " phải lớn hơn 0.";
        }

        if (type === "PERCENTAGE" && value > 100) {
            return label + " theo % tối đa 100%.";
        }

        return null;

    }


    async function savePromotion(event) {

        event.preventDefault();

        const result = readForm();

        if (result.error) {

            formError.textContent = result.error;

            formError.hidden = false;

            return;

        }


        const saveButton = field("promotionSaveBtn");

        saveButton.disabled = true;

        formError.hidden = true;


        try {

            const wasEditing = editingPromotion !== null;

            const saved = wasEditing
                ? await apiRequest("/admin/promotions/" + editingPromotion.id, { method: "PUT", body: result.body, auth: true })
                : await apiRequest("/admin/promotions", { method: "POST", body: result.body, auth: true });

            showToast((wasEditing ? "Đã lưu " : "Đã thêm ") + saved.name + ".", "success");

            closeForm();

            if (!wasEditing) {
                currentPage = 0;
            }

            loadPromotions();

        } catch (error) {

            handleError(error, function (message) {
                formError.textContent = message + detailsText(error);
                formError.hidden = false;
            });

        } finally {

            saveButton.disabled = false;

        }

    }


    /* Lỗi từng trường của VALIDATION_ERROR (backend trả câu tiếng Việt) */

    function detailsText(error) {

        if (!error.details || typeof error.details !== "object") {
            return "";
        }

        const parts = Object.keys(error.details).map(function (key) {
            return error.details[key];
        });

        return parts.length > 0 ? " (" + parts.join("; ") + ")" : "";

    }


    /* ================= MỨC GIẢM & GIÁ SAU GIẢM ================= */

    function discountLabel(type, value) {

        const amount = Number(value);

        return type === "PERCENTAGE"
            ? "Giảm " + amount.toLocaleString("vi-VN") + "%"
            : "Giảm " + formatPrice(amount);

    }


    function campaignDiscount() {

        return {
            type: field("pmDiscountType").value,
            value: numberOrNull("pmDiscountValue"),
            cap: numberOrNull("pmMaxDiscount")
        };

    }


    /* Giống PromotionPricingService ở backend: % làm tròn 2 số lẻ, áp trần; giá ≤ 0 → không áp dụng */

    function discountedPrice(price, type, value, cap) {

        let amount = type === "PERCENTAGE" ? Math.round(price * value) / 100 : value;

        if (type === "PERCENTAGE" && cap > 0 && amount > cap) {
            amount = cap;
        }

        return price - amount;

    }


    function previewHtml(item) {

        const campaign = campaignDiscount();

        const type = item.discountType || campaign.type;

        const value = item.discountType ? item.discountValue : campaign.value;

        if (!(value > 0) || !(item.basePrice > 0) || (type === "PERCENTAGE" && value > 100)) {
            return "—";
        }

        const price = discountedPrice(item.basePrice, type, value, campaign.cap);

        if (price <= 0) {
            return `<span class="admin-subtext">Không áp dụng (giảm hết giá)</span>`;
        }

        return escapeHtml(formatPrice(price));

    }


    function updatePreview(item) {

        const cell = selectedBody.querySelector("td[data-role='preview'][data-id='" + item.productId + "']");

        if (cell) {
            cell.innerHTML = previewHtml(item);
        }

    }


    /* Đổi kiểu / mức giảm của chương trình: nhãn ô mức giảm + giá sau giảm của mọi sản phẩm dùng mặc định */

    function refreshCampaignDiscount() {

        field("pmDiscountValueLabel").textContent = field("pmDiscountType").value === "PERCENTAGE"
            ? "Mức giảm (%) *"
            : "Mức giảm (đ) *";

        renderSelected();

    }


    /* ================= CHỌN SẢN PHẨM ================= */

    function findSelected(productId) {

        return selected.find(function (item) {
            return String(item.productId) === String(productId);
        });

    }


    async function searchProducts() {

        const requestId = ++pickerCounter;

        const params = new URLSearchParams({ size: String(PROMOTION_PICKER_SIZE) });

        const keyword = productKeywordInput.value.trim();

        if (keyword) {
            params.set("keyword", keyword);
        }

        productResults.hidden = false;

        productResults.innerHTML = `<li class="admin-subtext">Đang tìm…</li>`;


        try {

            const page = await apiRequest("/products?" + params.toString());

            if (requestId !== pickerCounter) {
                return;
            }

            pickerResults = page.content || [];

            renderResults();

        } catch (error) {

            if (requestId === pickerCounter) {
                handleError(error, function (message) {
                    productResults.innerHTML = `<li class="admin-subtext">${escapeHtml(message)}</li>`;
                });
            }

        }

    }


    function renderResults() {

        if (pickerResults.length === 0) {

            productResults.innerHTML = `<li class="admin-subtext">Không tìm thấy sản phẩm nào.</li>`;

            return;

        }

        productResults.innerHTML = pickerResults.map(function (product) {

            const id = escapeHtml(String(product.id));

            const action = findSelected(product.id)
                ? `<span class="admin-subtext">Đã chọn</span>`
                : `<button type="button" class="btn btn-outline-dark" data-action="add" data-id="${id}">THÊM</button>`;

            return `
                <li>
                    ${thumbHtml(product.primaryImageUrl)}
                    <span class="admin-promo-result-name">
                        ${escapeHtml(product.name)}
                        <span class="admin-subtext">
                            #${id} · ${escapeHtml(formatPrice(Number(product.basePrice)))}${product.isActive === false ? " · Đang ẩn" : ""}
                        </span>
                    </span>
                    ${action}
                </li>
            `;

        }).join("");

    }


    function addProduct(productId) {

        const product = pickerResults.find(function (candidate) {
            return String(candidate.id) === String(productId);
        });

        if (!product || findSelected(product.id)) {
            return;
        }

        if (selected.length >= PROMOTION_MAX_PRODUCTS) {

            showToast("Một chương trình tối đa " + PROMOTION_MAX_PRODUCTS + " sản phẩm.", "error");

            return;

        }

        selected.push({
            productId: product.id,
            name: product.name,
            basePrice: Number(product.basePrice),
            imageUrl: product.primaryImageUrl,
            available: product.isActive !== false,
            discountType: null,
            discountValue: null
        });

        renderSelected();

        renderResults();

    }


    function removeProduct(productId) {

        selected = selected.filter(function (item) {
            return String(item.productId) !== String(productId);
        });

        renderSelected();

        if (!productResults.hidden && pickerResults.length > 0) {
            renderResults();
        }

    }


    function renderSelected() {

        if (selected.length === 0) {

            selectedBody.innerHTML = adminEmptyRow(5, "Chưa chọn sản phẩm nào. Tìm và bấm THÊM ở trên.");

            return;

        }

        selectedBody.innerHTML = selected.map(function (item) {

            const id = escapeHtml(String(item.productId));

            const name = escapeHtml(item.name);

            return `
                <tr>
                    <td>
                        <div class="admin-product-cell">
                            ${thumbHtml(item.imageUrl)}
                            <div>
                                ${name}
                                <div class="admin-subtext">#${id}${item.available ? "" : " · Đã ẩn / ngừng bán"}</div>
                            </div>
                        </div>
                    </td>
                    <td>${escapeHtml(formatPrice(item.basePrice))}</td>
                    <td>
                        <div class="admin-promo-override">
                            <select data-role="type" data-id="${id}" aria-label="Mức giảm của ${name}">
                                <option value="" ${item.discountType ? "" : "selected"}>Mặc định</option>
                                <option value="PERCENTAGE" ${item.discountType === "PERCENTAGE" ? "selected" : ""}>Riêng: %</option>
                                <option value="FIXED_AMOUNT" ${item.discountType === "FIXED_AMOUNT" ? "selected" : ""}>Riêng: số tiền</option>
                            </select>
                            <input
                                type="number"
                                data-role="value"
                                data-id="${id}"
                                min="0"
                                step="any"
                                value="${item.discountValue === null ? "" : escapeHtml(String(item.discountValue))}"
                                aria-label="Mức giảm riêng của ${name}"
                                ${item.discountType ? "" : "hidden"}
                            >
                        </div>
                    </td>
                    <td data-role="preview" data-id="${id}">${previewHtml(item)}</td>
                    <td>
                        <button type="button" class="admin-link-btn admin-link-danger" data-action="remove" data-id="${id}">Bỏ</button>
                    </td>
                </tr>
            `;

        }).join("");

    }


    function thumbHtml(url) {

        return isSafeImageUrl(url)
            ? `<img class="admin-product-thumb" src="${escapeHtml(url.trim())}" alt="" loading="lazy">`
            : `<span class="admin-product-thumb admin-product-thumb--empty" title="Chưa có ảnh">—</span>`;

    }


    /* ================= TẠM DỪNG / BẬT, XOÁ ================= */

    /* PUT thay toàn bộ: gửi lại đúng dữ liệu hiện có, chỉ đổi active */

    async function toggleActive(promotion) {

        const body = {
            name: promotion.name,
            description: promotion.description,
            discountType: promotion.discountType,
            discountValue: promotion.discountValue,
            maxDiscountAmount: promotion.maxDiscountAmount,
            startDate: promotion.startDate,
            endDate: promotion.endDate,
            active: !promotion.active,
            products: (promotion.products || []).map(function (product) {
                return {
                    productId: product.productId,
                    discountType: product.override ? product.discountType : null,
                    discountValue: product.override ? product.discountValue : null
                };
            })
        };


        try {

            await apiRequest("/admin/promotions/" + promotion.id, { method: "PUT", body: body, auth: true });

            showToast((body.active ? "Đã bật " : "Đã tạm dừng ") + promotion.name + ".", "success");

        } catch (error) {

            handleError(error, function (message) {
                showToast(message, "error");
            });

        }

        loadPromotions();

    }


    function confirmDelete(promotion) {

        openConfirmModal({
            title: "Xoá chương trình " + promotion.name + "?",
            message: "Giá khuyến mãi ngừng áp dụng ngay cho giỏ hàng. Đơn đã đặt vẫn giữ giá đã mua. Không thể hoàn tác.",
            confirmLabel: "XOÁ",
            cancelLabel: "Quay lại",
            onConfirm: async function () {

                try {

                    await apiRequest("/admin/promotions/" + promotion.id, { method: "DELETE", auth: true });

                    showToast("Đã xoá " + promotion.name + ".", "success");

                    if (editingPromotion && editingPromotion.id === promotion.id) {
                        closeForm();
                    }

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

                loadPromotions();

            }
        });

    }


    /*
     * Hết phiên (401, kể cả sau khi đã thử làm mới token) → kiểm tra lại phiên
     * và về trang đăng nhập; lỗi khác (403, 404, 409…) → hiển thị bằng show(message).
     */

    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
