/* ================= VOUCHER (admin/vouchers.html, kế hoạch v5 GĐ4) ================= */

/*
 * API thật (chỉ ADMIN; backend kiểm tra lại vai trò trong DB ở mọi request):
 *   GET    /admin/vouchers?keyword=&status=&page=&size=     mới nhất trước
 *   POST   /admin/vouchers   { code, name, description, discountPercent, maxDiscount, minOrderAmount,
 *   PUT    /admin/vouchers/{id}  startsAt, endsAt, usageLimit, perCustomerLimit, allCategories, categoryIds, active }
 *   PATCH  /admin/vouchers/{id}/status { active }
 *   DELETE /admin/vouchers/{id}          (chỉ khi chưa đơn nào dùng)
 *   GET    /admin/vouchers/{id}/usages   đơn đã dùng (kể cả đơn đã huỷ = đã trả lượt)
 * Danh mục lấy từ GET /categories. Trạng thái (status) do backend tính.
 */

const VOUCHER_PAGE_SIZE = 20;

const VOUCHER_STATUS_LABELS = {
    RUNNING: "Đang áp dụng",
    UPCOMING: "Sắp áp dụng",
    PAUSED: "Tạm dừng",
    ENDED: "Đã kết thúc",
    USED_UP: "Hết lượt"
};

const VOUCHER_STATUS_BADGES = {
    RUNNING: "admin-badge-success",
    UPCOMING: "admin-badge-neutral",
    PAUSED: "admin-badge-warning",
    ENDED: "admin-badge-neutral",
    USED_UP: "admin-badge-warning"
};

const VOUCHER_CODE_PATTERN = /^[A-Z0-9_-]{3,30}$/;


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const filterForm = document.getElementById("voucherFilterForm");

    const keywordInput = document.getElementById("voucherKeyword");

    const statusFilter = document.getElementById("voucherStatusFilter");

    const tbody = document.getElementById("voucherTableBody");

    const pagination = document.getElementById("voucherPagination");

    const panel = document.getElementById("voucherFormPanel");

    const form = document.getElementById("voucherForm");

    const formError = document.getElementById("voucherFormError");

    const categoryBox = document.getElementById("vcCategories");

    const usagePanel = document.getElementById("voucherUsagePanel");

    const usageBody = document.getElementById("voucherUsageBody");

    const usagePagination = document.getElementById("voucherUsagePagination");


    let currentPage = 0;

    let currentVouchers = [];

    let requestCounter = 0;

    let editingVoucher = null;

    let categories = null;

    let usageVoucher = null;

    let usagePage = 0;


    /* ================= SỰ KIỆN ================= */

    filterForm.addEventListener("submit", function (event) {
        event.preventDefault();
        reloadFromFirstPage();
    });

    statusFilter.addEventListener("change", reloadFromFirstPage);

    document.getElementById("newVoucherBtn").addEventListener("click", function () {
        openForm(null);
    });

    document.getElementById("voucherCancelBtn").addEventListener("click", closeForm);

    form.addEventListener("submit", saveVoucher);

    form.querySelectorAll('input[name="vcScope"]').forEach(function (radio) {
        radio.addEventListener("change", syncScope);
    });

    ["vcPercent", "vcMax", "vcMin", "vcCode"].forEach(function (id) {
        field(id).addEventListener("input", renderPreview);
    });

    field("vcCode").addEventListener("blur", function () {
        field("vcCode").value = field("vcCode").value.trim().toUpperCase();
    });

    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const voucher = button && currentVouchers.find(function (item) {
            return String(item.id) === button.dataset.id;
        });

        if (!voucher) {
            return;
        }

        if (button.dataset.action === "edit") {
            openForm(voucher);
        } else if (button.dataset.action === "toggle") {
            toggleActive(voucher);
        } else if (button.dataset.action === "delete") {
            confirmDelete(voucher);
        } else if (button.dataset.action === "usages") {
            openUsages(voucher);
        }

    });

    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (button && !button.disabled) {
            currentPage = Number(button.dataset.page);
            loadVouchers();
        }

    });

    usagePagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (button && !button.disabled) {
            usagePage = Number(button.dataset.page);
            loadUsages();
        }

    });

    document.getElementById("voucherUsageClose").addEventListener("click", function () {
        usagePanel.hidden = true;
        usageVoucher = null;
    });


    loadVouchers();


    /* ================= DANH SÁCH ================= */

    function reloadFromFirstPage() {

        currentPage = 0;

        loadVouchers();

    }


    async function loadVouchers() {

        const requestId = ++requestCounter;

        const params = new URLSearchParams({ page: String(currentPage), size: String(VOUCHER_PAGE_SIZE) });

        const keyword = keywordInput.value.trim();

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (statusFilter.value) {
            params.set("status", statusFilter.value);
        }

        tbody.innerHTML = adminEmptyRow(7, "Đang tải…");

        try {

            const page = await apiRequest("/admin/vouchers?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            if ((page.content || []).length === 0 && currentPage > 0 && page.totalPages > 0) {
                currentPage = page.totalPages - 1;
                loadVouchers();
                return;
            }

            currentVouchers = page.content || [];

            document.getElementById("voucherRowCount").textContent = page.totalElements + " voucher";

            tbody.innerHTML = currentVouchers.map(renderRow).join("")
                || adminEmptyRow(7, "Chưa có voucher nào phù hợp bộ lọc");

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


    function percentText(value) {

        return Number(value).toLocaleString("vi-VN", { maximumFractionDigits: 2 }) + "%";

    }


    function renderRow(voucher) {

        const id = escapeHtml(String(voucher.id));

        const scope = voucher.allCategories
            ? "Toàn bộ sản phẩm"
            : (voucher.categories || []).map(function (category) { return category.name; }).join(", ");

        const uses = voucher.usageLimit
            ? voucher.usedCount + " / " + voucher.usageLimit
            : voucher.usedCount + " / không giới hạn";

        const toggle = voucher.status === "ENDED"
            ? ""
            : `<button type="button" class="admin-link-btn" data-action="toggle" data-id="${id}">${voucher.active ? "Tạm dừng" : "Bật"}</button>`;

        return `
            <tr data-voucher-id="${id}">
                <td>
                    <strong class="admin-voucher-code">${escapeHtml(voucher.code)}</strong>
                    <span class="admin-subtext">${escapeHtml(voucher.name)}</span>
                </td>
                <td>
                    Giảm ${escapeHtml(percentText(voucher.discountPercent))}
                    <span class="admin-subtext">${voucher.maxDiscount ? "Tối đa " + escapeHtml(formatPrice(Number(voucher.maxDiscount))) : "Không giới hạn mức giảm"}</span>
                </td>
                <td>
                    Đơn từ ${escapeHtml(formatPrice(Number(voucher.minOrderAmount || 0)))}
                    <span class="admin-subtext" title="${escapeHtml(scope)}">${escapeHtml(truncateText(scope, 50))}</span>
                </td>
                <td>
                    ${escapeHtml(formatDateTimeVi(voucher.startsAt))}
                    <span class="admin-subtext">→ ${escapeHtml(formatDateTimeVi(voucher.endsAt))}</span>
                </td>
                <td>
                    ${escapeHtml(uses)}
                    <span class="admin-subtext">Mỗi khách ${escapeHtml(String(voucher.perCustomerLimit))} lần</span>
                </td>
                <td>
                    <span class="admin-badge ${VOUCHER_STATUS_BADGES[voucher.status] || "admin-badge-neutral"}">
                        ${escapeHtml(VOUCHER_STATUS_LABELS[voucher.status] || voucher.status)}
                    </span>
                </td>
                <td class="admin-actions-cell">
                    <button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>
                    ${toggle}
                    <button type="button" class="admin-link-btn" data-action="usages" data-id="${id}">Đơn đã dùng</button>
                    ${voucher.deletable ? `<button type="button" class="admin-link-btn admin-link-danger" data-action="delete" data-id="${id}">Xoá</button>` : ""}
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


    /* ================= THÊM / SỬA ================= */

    function field(id) {

        return document.getElementById(id);

    }


    async function loadCategories() {

        if (categories) {
            return categories;
        }

        try {

            const page = await apiRequest("/categories?size=100");

            categories = (page.content || []).slice().sort(function (a, b) {
                return String(a.name).localeCompare(String(b.name), "vi");
            });

        } catch (error) {

            categories = [];

        }

        return categories;

    }


    async function openForm(voucher) {

        editingVoucher = voucher;

        field("voucherFormTitle").textContent = voucher ? "Sửa voucher " + voucher.code : "Thêm voucher";

        const now = new Date();

        field("vcCode").value = voucher ? voucher.code : "";

        /* đã từng có đơn dùng (kể cả đơn đã huỷ) → không đổi được mã (backend cũng chặn) */
        field("vcCode").readOnly = Boolean(voucher && !voucher.deletable);

        field("vcName").value = voucher ? voucher.name : "";

        field("vcDescription").value = voucher && voucher.description ? voucher.description : "";

        field("vcPercent").value = voucher ? Number(voucher.discountPercent) : "";

        field("vcMax").value = voucher && voucher.maxDiscount ? Number(voucher.maxDiscount) : "";

        field("vcMin").value = voucher && Number(voucher.minOrderAmount) > 0 ? Number(voucher.minOrderAmount) : "";

        field("vcStart").value = voucher ? voucher.startsAt.slice(0, 16) : toInputDateTime(now);

        field("vcEnd").value = voucher ? voucher.endsAt.slice(0, 16) : toInputDateTime(new Date(now.getTime() + 30 * 86400000));

        field("vcUsageLimit").value = voucher && voucher.usageLimit ? voucher.usageLimit : "";

        field("vcPerCustomer").value = voucher ? voucher.perCustomerLimit : 1;

        field("vcActive").checked = voucher ? voucher.active : true;

        form.querySelector('input[name="vcScope"][value="' + (voucher && !voucher.allCategories ? "CATEGORIES" : "ALL") + '"]').checked = true;

        formError.hidden = true;

        panel.hidden = false;

        const chosen = voucher ? (voucher.categories || []).map(function (category) { return String(category.id); }) : [];

        const list = await loadCategories();

        categoryBox.innerHTML = list.length
            ? list.map(function (category) {
                const id = escapeHtml(String(category.id));
                return `
                    <label class="admin-field-check">
                        <input type="checkbox" value="${id}" ${chosen.indexOf(String(category.id)) !== -1 ? "checked" : ""}>
                        ${escapeHtml(category.name)}
                    </label>
                `;
            }).join("")
            : `<span class="admin-subtext">Không tải được danh mục.</span>`;

        syncScope();

        renderPreview();

        field(voucher && !voucher.deletable ? "vcName" : "vcCode").focus();

        panel.scrollIntoView({ behavior: "smooth", block: "start" });

    }


    function closeForm() {

        editingVoucher = null;

        panel.hidden = true;

    }


    function syncScope() {

        categoryBox.hidden = form.querySelector('input[name="vcScope"]:checked').value !== "CATEGORIES";

    }


    /* Ví dụ cho người nhập: đơn đạt đúng mức tối thiểu được giảm bao nhiêu */
    function renderPreview() {

        const percent = numberOrNull("vcPercent");

        const max = numberOrNull("vcMax");

        const min = numberOrNull("vcMin") || 0;

        const preview = field("vcPreview");

        if (!(percent > 0) || percent > 100) {
            preview.textContent = "";
            return;
        }

        const sample = Math.max(min, 1000000);

        const discount = Math.min(Math.floor(sample * percent / 100), max > 0 ? max : Infinity);

        const capAt = max > 0 ? Math.ceil(max * 100 / percent) : null;

        preview.textContent = "Ví dụ: đơn " + formatPrice(sample) + " được giảm " + formatPrice(discount) + "."
            + (capAt ? " Từ " + formatPrice(capAt) + " trở lên luôn giảm tối đa " + formatPrice(max) + "." : "");

    }


    function toInputDateTime(date) {

        const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);

        return local.toISOString().slice(0, 16);

    }


    function toApiDateTime(value) {

        return value.length === 16 ? value + ":00" : value;

    }


    function numberOrNull(id) {

        const value = field(id).value.trim();

        return value === "" ? null : Number(value);

    }


    function readForm() {

        const scope = form.querySelector('input[name="vcScope"]:checked').value;

        const body = {
            code: field("vcCode").value.trim().toUpperCase(),
            name: field("vcName").value.trim(),
            description: field("vcDescription").value.trim() || null,
            discountPercent: numberOrNull("vcPercent"),
            maxDiscount: numberOrNull("vcMax"),
            minOrderAmount: numberOrNull("vcMin"),
            startsAt: field("vcStart").value,
            endsAt: field("vcEnd").value,
            usageLimit: numberOrNull("vcUsageLimit"),
            perCustomerLimit: numberOrNull("vcPerCustomer"),
            allCategories: scope === "ALL",
            categoryIds: scope === "ALL" ? [] : Array.from(categoryBox.querySelectorAll("input:checked")).map(function (box) {
                return Number(box.value);
            }),
            active: field("vcActive").checked
        };

        if (!VOUCHER_CODE_PATTERN.test(body.code)) {
            return { error: "Mã gồm 3–30 ký tự chữ không dấu, số, dấu - hoặc _ (không dấu cách)." };
        }

        if (!body.name) {
            return { error: "Vui lòng nhập tên voucher." };
        }

        if (!(body.discountPercent > 0) || body.discountPercent > 100) {
            return { error: "Phần trăm giảm phải lớn hơn 0 và tối đa 100." };
        }

        if (body.maxDiscount !== null && !(body.maxDiscount > 0)) {
            return { error: "Giảm tối đa phải lớn hơn 0 (hoặc để trống)." };
        }

        if (body.minOrderAmount !== null && body.minOrderAmount < 0) {
            return { error: "Đơn tối thiểu không được âm." };
        }

        if (!body.startsAt || !body.endsAt) {
            return { error: "Vui lòng chọn thời gian bắt đầu và kết thúc." };
        }

        if (body.endsAt <= body.startsAt) {
            return { error: "Thời gian kết thúc phải sau thời gian bắt đầu." };
        }

        if (body.usageLimit !== null && !(Number.isInteger(body.usageLimit) && body.usageLimit >= 1)) {
            return { error: "Tổng lượt dùng phải là số nguyên từ 1 (hoặc để trống)." };
        }

        if (editingVoucher && body.usageLimit !== null && body.usageLimit < editingVoucher.usedCount) {
            return { error: "Tổng lượt không được nhỏ hơn số lượt đã dùng (" + editingVoucher.usedCount + ")." };
        }

        if (!(Number.isInteger(body.perCustomerLimit) && body.perCustomerLimit >= 1)) {
            return { error: "Lượt mỗi khách phải là số nguyên từ 1." };
        }

        if (!body.allCategories && body.categoryIds.length === 0) {
            return { error: "Chọn ít nhất 1 danh mục áp dụng." };
        }

        body.startsAt = toApiDateTime(body.startsAt);

        body.endsAt = toApiDateTime(body.endsAt);

        return { body: body };

    }


    async function saveVoucher(event) {

        event.preventDefault();

        const result = readForm();

        if (result.error) {
            formError.textContent = result.error;
            formError.hidden = false;
            return;
        }

        const saveButton = field("voucherSaveBtn");

        saveButton.disabled = true;

        formError.hidden = true;

        try {

            const wasEditing = editingVoucher !== null;

            const saved = wasEditing
                ? await apiRequest("/admin/vouchers/" + editingVoucher.id, { method: "PUT", body: result.body, auth: true })
                : await apiRequest("/admin/vouchers", { method: "POST", body: result.body, auth: true });

            showToast((wasEditing ? "Đã lưu voucher " : "Đã thêm voucher ") + saved.code + ".", "success");

            closeForm();

            if (!wasEditing) {
                currentPage = 0;
            }

            loadVouchers();

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

    async function toggleActive(voucher) {

        try {

            await apiRequest("/admin/vouchers/" + voucher.id + "/status", {
                method: "PATCH", body: { active: !voucher.active }, auth: true
            });

            showToast((voucher.active ? "Đã tạm dừng " : "Đã bật ") + voucher.code + ".", "success");

        } catch (error) {

            handleError(error, function (message) {
                showToast(message, "error");
            });

        }

        loadVouchers();

    }


    function confirmDelete(voucher) {

        openConfirmModal({
            title: "Xoá voucher " + voucher.code + "?",
            message: "Khách sẽ không dùng được mã này nữa. Không thể hoàn tác.",
            confirmLabel: "XOÁ",
            cancelLabel: "Quay lại",
            onConfirm: async function () {

                try {

                    await apiRequest("/admin/vouchers/" + voucher.id, { method: "DELETE", auth: true });

                    showToast("Đã xoá voucher " + voucher.code + ".", "success");

                    if (editingVoucher && editingVoucher.id === voucher.id) {
                        closeForm();
                    }

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

                loadVouchers();

            }
        });

    }


    /* ================= ĐƠN ĐÃ DÙNG ================= */

    function openUsages(voucher) {

        usageVoucher = voucher;

        usagePage = 0;

        document.getElementById("voucherUsageTitle").textContent = "Đơn đã dùng voucher " + voucher.code;

        usagePanel.hidden = false;

        loadUsages();

        usagePanel.scrollIntoView({ behavior: "smooth", block: "start" });

    }


    async function loadUsages() {

        if (!usageVoucher) {
            return;
        }

        const voucher = usageVoucher;

        usageBody.innerHTML = adminEmptyRow(5, "Đang tải…");

        try {

            const page = await apiRequest("/admin/vouchers/" + voucher.id + "/usages?size=20&page=" + usagePage, { auth: true });

            if (usageVoucher !== voucher) {
                return;
            }

            usageBody.innerHTML = (page.content || []).map(function (usage) {

                return `
                    <tr>
                        <td>
                            <a href="${escapeHtml(siteUrl("admin/orders.html") + "?keyword=" + encodeURIComponent(usage.orderCode))}">${escapeHtml(usage.orderCode)}</a>
                            ${usage.releasedAt ? `<span class="admin-subtext">Đã huỷ · đã trả lượt</span>` : ""}
                        </td>
                        <td>
                            ${escapeHtml(usage.customerName || "")}
                            <span class="admin-subtext">${escapeHtml(usage.customerEmail || "")}</span>
                        </td>
                        <td>${escapeHtml(formatPrice(Number(usage.discountAmount)))}</td>
                        <td>${escapeHtml(formatPrice(Number(usage.orderTotal)))}</td>
                        <td>${escapeHtml(formatDateTimeVi(usage.usedAt))}</td>
                    </tr>
                `;

            }).join("") || adminEmptyRow(5, "Chưa có đơn nào dùng voucher này");

            usagePagination.innerHTML = pagerHtml(page);

        } catch (error) {

            handleError(error, function (message) {
                usageBody.innerHTML = adminEmptyRow(5, message);
            });

        }

    }


    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
