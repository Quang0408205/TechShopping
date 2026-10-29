/* ================= DANH MỤC / THƯƠNG HIỆU (admin/categories.html, admin/brands.html) — kế hoạch v3 GĐ C ================= */

/*
 * Một file cho cả 2 trang; body[data-taxonomy] chọn cấu hình bên dưới. Chỉ ADMIN (data-roles="ADMIN").
 * API (đọc công khai, ghi chỉ ADMIN — SecurityConfig CATALOG_PATHS):
 *   GET    /categories | /brands?size=100&page=      tải hết rồi tìm / lọc / phân trang ngay trên trang (ít dòng)
 *   POST   /categories | /brands                     201
 *   PUT    /categories/{id} | /brands/{id}           gửi đủ trường (ẩn / hiện = PUT với isActive đảo)
 *   DELETE /categories/{id} | /brands/{id}           409 RESOURCE_IN_USE khi còn sản phẩm (danh mục: hoặc còn danh mục con)
 *   GET    /products?categoryId= | brandId=&size=1   số sản phẩm của từng dòng (totalElements)
 */

const TAXONOMY_PAGE_SIZE = 20;

const TAXONOMY_SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;

const TAXONOMY_CONFIGS = {

    categories: {
        path: "/categories",
        noun: "danh mục",
        productParam: "categoryId",
        imageField: "iconUrl",
        columns: 6,
        hasParent: true
    },

    brands: {
        path: "/brands",
        noun: "thương hiệu",
        productParam: "brandId",
        imageField: "logoUrl",
        columns: 5,
        hasParent: false
    }

};


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const config = TAXONOMY_CONFIGS[document.body.dataset.taxonomy];

    const tbody = document.getElementById("taxTableBody");

    const pagination = document.getElementById("taxPagination");

    const formPanel = document.getElementById("taxFormPanel");

    const form = document.getElementById("taxForm");

    const formError = document.getElementById("taxFormError");

    const keywordInput = document.getElementById("taxKeyword");

    const statusFilter = document.getElementById("taxStatusFilter");


    let items = [];

    let currentPage = 0;

    let editing = null;

    let saving = false;

    /* id → số sản phẩm (null = đang tải / lỗi) */
    const productCounts = new Map();


    document.getElementById("taxFilterForm").addEventListener("submit", function (event) {
        event.preventDefault();
    });

    keywordInput.addEventListener("input", function () {
        currentPage = 0;
        render();
    });

    statusFilter.addEventListener("change", function () {
        currentPage = 0;
        render();
    });

    document.getElementById("taxNewBtn").addEventListener("click", function () {
        openForm(null);
    });

    document.getElementById("taxCancelBtn").addEventListener("click", closeForm);

    form.addEventListener("submit", save);

    /* Chỉ trang Danh mục có nút tải ảnh: dùng chung chỗ lưu ảnh sản phẩm, điền link vào ô ảnh */
    const imageFile = document.getElementById("taxImageFile");

    if (imageFile) {

        field("taxImage").addEventListener("input", showImagePreview);

        imageFile.addEventListener("change", async function () {

            const file = imageFile.files[0];

            imageFile.value = "";

            if (!file) {
                return;
            }

            const formData = new FormData();

            formData.append("file", file);

            try {

                const uploaded = await apiRequest("/admin/uploads/product-images", { method: "POST", body: formData, auth: true });

                field("taxImage").value = uploaded.url;

                showImagePreview();

            } catch (error) {

                handleError(error, function (message) {
                    formError.textContent = message;
                    formError.hidden = false;
                });

            }

        });

    }


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const item = button && findItem(button.dataset.id);

        if (!item) {
            return;
        }

        if (button.dataset.action === "edit") {
            openForm(item);
        } else if (button.dataset.action === "toggle") {
            toggleActive(item);
        } else if (button.dataset.action === "delete") {
            confirmDelete(item);
        }

    });

    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        render();

    });


    loadItems();


    /* ================= TẢI / HIỂN THỊ ================= */

    async function loadItems() {

        tbody.innerHTML = adminEmptyRow(config.columns, "Đang tải…");

        try {

            items = await fetchAllPages(config.path + "?sort=name,asc");

            render();

        } catch (error) {

            handleError(error, function (message) {
                tbody.innerHTML = adminEmptyRow(config.columns, message);
            });

        }

    }


    function findItem(id) {

        return items.find(function (item) {
            return String(item.id) === String(id);
        });

    }


    /* Danh mục: cha trước, con ngay dưới (thụt lề), theo thứ tự hiển thị rồi tên; thương hiệu: theo tên */

    function orderedItems() {

        if (!config.hasParent) {
            return items.map(function (item) { return { item: item, depth: 0 }; });
        }

        const byOrder = function (a, b) {
            return (a.displayOrder ?? 0) - (b.displayOrder ?? 0) || a.name.localeCompare(b.name, "vi");
        };

        const ids = new Set(items.map(function (item) { return item.id; }));

        const result = [];

        const visit = function (parentId, depth) {

            items.filter(function (item) {
                return parentId === null
                    ? item.parentId == null || !ids.has(item.parentId)
                    : item.parentId === parentId;
            }).sort(byOrder).forEach(function (item) {
                result.push({ item: item, depth: depth });
                visit(item.id, depth + 1);
            });

        };

        visit(null, 0);

        return result;

    }


    function filteredRows() {

        const keyword = normalizeText(keywordInput.value);

        const status = statusFilter.value;

        return orderedItems().filter(function (row) {

            const item = row.item;

            if (status && String(item.isActive !== false) !== status) {
                return false;
            }

            return !keyword || normalizeText(item.name + " " + item.slug).includes(keyword);

        });

    }


    function render() {

        const rows = filteredRows();

        const totalPages = Math.max(1, Math.ceil(rows.length / TAXONOMY_PAGE_SIZE));

        currentPage = Math.min(currentPage, totalPages - 1);

        const pageRows = rows.slice(currentPage * TAXONOMY_PAGE_SIZE, (currentPage + 1) * TAXONOMY_PAGE_SIZE);


        document.getElementById("taxRowCount").textContent = rows.length + " " + config.noun;

        tbody.innerHTML = pageRows.map(renderRow).join("") ||
            adminEmptyRow(config.columns, "Không có " + config.noun + " nào phù hợp");


        pagination.innerHTML = totalPages <= 1 ? "" : `
            <button type="button" class="btn btn-outline-dark" data-page="${currentPage - 1}" ${currentPage <= 0 ? "disabled" : ""}>‹ TRƯỚC</button>
            <span>Trang ${currentPage + 1} / ${totalPages}</span>
            <button type="button" class="btn btn-outline-dark" data-page="${currentPage + 1}" ${currentPage + 1 >= totalPages ? "disabled" : ""}>SAU ›</button>
        `;


        loadProductCounts(pageRows.map(function (row) { return row.item; }));

    }


    function renderRow(row) {

        const item = row.item;

        const id = escapeHtml(String(item.id));

        const active = item.isActive !== false;

        const image = item[config.imageField];

        const nameCell = `
            <div class="admin-product-cell${row.depth ? " admin-tax-child" : ""}" style="--tax-depth: ${row.depth}">
                ${isSafeImageUrl(image)
                    ? `<img class="admin-product-thumb admin-tax-thumb" src="${escapeHtml(image.trim())}" alt="" loading="lazy">`
                    : `<span class="admin-product-thumb admin-product-thumb--empty admin-tax-thumb" aria-hidden="true">${escapeHtml(item.name.charAt(0).toUpperCase())}</span>`}
                <div>
                    <strong>${row.depth ? "↳ " : ""}${escapeHtml(item.name)}</strong>
                    <div class="admin-subtext">#${id} · ${escapeHtml(item.slug)}</div>
                </div>
            </div>
        `;

        const middleCells = config.hasParent
            ? `<td>${escapeHtml(item.parentName || "—")}</td><td>${escapeHtml(String(item.displayOrder ?? 0))}</td>`
            : `<td>${isSafeHttpUrl(item.websiteUrl)
                ? `<a href="${escapeHtml(item.websiteUrl)}" target="_blank" rel="noopener noreferrer">${escapeHtml(shortUrl(item.websiteUrl))}</a>`
                : "—"}</td>`;

        return `
            <tr data-tax-id="${id}">
                <td>${nameCell}</td>
                ${middleCells}
                <td class="js-product-count" data-id="${id}">${productCountHtml(item.id)}</td>
                <td>
                    <span class="admin-badge ${active ? "admin-badge-success" : "admin-badge-neutral"}">${active ? "Đang hiển thị" : "Đã ẩn"}</span>
                </td>
                <td class="admin-actions-cell">
                    <button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>
                    <button type="button" class="admin-link-btn" data-action="toggle" data-id="${id}">${active ? "Ẩn" : "Hiện"}</button>
                    <button type="button" class="admin-link-btn admin-link-danger" data-action="delete" data-id="${id}">Xoá</button>
                </td>
            </tr>
        `;

    }


    function productCountHtml(id) {

        if (!productCounts.has(id) || productCounts.get(id) === null) {
            return `<span class="admin-subtext">…</span>`;
        }

        const count = productCounts.get(id);

        return count === 0
            ? `<span class="admin-subtext">0</span>`
            : `<a href="${escapeHtml(siteUrl("admin/products.html?" + config.productParam + "=" + id))}">${count} sản phẩm</a>`;

    }


    /* Chỉ tải số của các dòng đang hiện, mỗi dòng một lần */

    function loadProductCounts(visibleItems) {

        visibleItems.filter(function (item) {
            return !productCounts.has(item.id);
        }).forEach(function (item) {

            productCounts.set(item.id, null);

            apiRequest("/products?size=1&" + config.productParam + "=" + item.id)
                .then(function (page) { productCounts.set(item.id, page.totalElements); })
                .catch(function () { productCounts.delete(item.id); })
                .finally(function () {
                    const cell = tbody.querySelector('.js-product-count[data-id="' + item.id + '"]');
                    if (cell) {
                        cell.innerHTML = productCounts.has(item.id) ? productCountHtml(item.id) : "—";
                    }
                });

        });

    }


    /* ================= FORM THÊM / SỬA ================= */

    function field(id) {
        return document.getElementById(id);
    }


    function openForm(item) {

        editing = item;

        formError.hidden = true;

        field("taxFormTitle").textContent = item
            ? "Sửa " + config.noun + ": " + item.name
            : "Thêm " + config.noun;

        field("taxName").value = item ? item.name : "";
        field("taxSlug").value = item ? item.slug : "";
        field("taxImage").value = item ? item[config.imageField] || "" : "";
        field("taxDescription").value = item ? item.description || "" : "";
        field("taxActive").checked = item ? item.isActive !== false : true;

        showImagePreview();

        if (config.hasParent) {
            fillParentOptions(item);
            field("taxOrder").value = item && item.displayOrder != null ? item.displayOrder : "";
        } else {
            field("taxWebsite").value = item ? item.websiteUrl || "" : "";
        }

        formPanel.hidden = false;

        formPanel.scrollIntoView({ behavior: "smooth", block: "start" });

        field("taxName").focus({ preventScroll: true });

    }


    function showImagePreview() {

        const preview = document.getElementById("taxImagePreview");

        if (!preview) {
            return;
        }

        const url = field("taxImage").value.trim();

        preview.hidden = !isSafeHttpUrl(url);

        if (!preview.hidden) {
            preview.src = url;
        }

    }


    function closeForm() {

        editing = null;

        formPanel.hidden = true;

        form.reset();

    }


    /* Danh mục cha: bỏ chính nó và các danh mục con cháu (backend cũng chặn: INVALID_CATEGORY_PARENT) */

    function fillParentOptions(item) {

        const blocked = new Set();

        if (item) {

            blocked.add(item.id);

            let grew = true;

            while (grew) {
                grew = false;
                items.forEach(function (candidate) {
                    if (!blocked.has(candidate.id) && blocked.has(candidate.parentId)) {
                        blocked.add(candidate.id);
                        grew = true;
                    }
                });
            }

        }

        field("taxParent").innerHTML = `<option value="">— Không có (danh mục gốc) —</option>` +
            orderedItems().filter(function (row) {
                return !blocked.has(row.item.id);
            }).map(function (row) {
                return `<option value="${escapeHtml(String(row.item.id))}">${"— ".repeat(row.depth)}${escapeHtml(row.item.name)}</option>`;
            }).join("");

        field("taxParent").value = item && item.parentId != null ? String(item.parentId) : "";

    }


    function textOrNull(id) {

        const value = field(id).value.trim();

        return value === "" ? null : value;

    }


    /* { body } hoặc { error } */

    function readForm() {

        const name = field("taxName").value.trim();

        if (!name) {
            return { error: "Vui lòng nhập tên " + config.noun + "." };
        }

        const slug = textOrNull("taxSlug");

        if (slug && !TAXONOMY_SLUG_PATTERN.test(slug)) {
            return { error: "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang (vd. dien-thoai)." };
        }

        const image = textOrNull("taxImage");

        if (image && !isSafeHttpUrl(image)) {
            return { error: "Link ảnh phải bắt đầu bằng http:// hoặc https://." };
        }

        const body = {
            name: name,
            slug: slug,
            description: textOrNull("taxDescription"),
            isActive: field("taxActive").checked
        };

        body[config.imageField] = image;

        if (config.hasParent) {

            const order = field("taxOrder").value.trim();

            if (order !== "" && !(Number.isInteger(Number(order)) && Number(order) >= 0)) {
                return { error: "Thứ tự hiển thị phải là số nguyên không âm." };
            }

            body.parentId = field("taxParent").value ? Number(field("taxParent").value) : null;
            body.displayOrder = order === "" ? null : Number(order);

        } else {

            const website = textOrNull("taxWebsite");

            if (website && !isSafeHttpUrl(website)) {
                return { error: "Website phải bắt đầu bằng http:// hoặc https://." };
            }

            body.websiteUrl = website;

        }

        return { body: body };

    }


    async function save(event) {

        event.preventDefault();

        if (saving) {
            return;
        }

        const result = readForm();

        if (result.error) {
            showFormError(result.error);
            return;
        }

        saving = true;

        field("taxSaveBtn").disabled = true;

        try {

            const saved = editing
                ? await apiRequest(config.path + "/" + editing.id, { method: "PUT", body: result.body, auth: true })
                : await apiRequest(config.path, { method: "POST", body: result.body, auth: true });

            showToast((editing ? "Đã lưu " : "Đã thêm ") + config.noun + " " + saved.name + ".", "success");

            closeForm();

            await loadItems();

        } catch (error) {

            handleError(error, function (message) {
                showFormError(message + detailsText(error));
            });

        } finally {

            saving = false;

            field("taxSaveBtn").disabled = false;

        }

    }


    function showFormError(message) {

        formError.textContent = message;

        formError.hidden = false;

    }


    function detailsText(error) {

        if (!error.details || typeof error.details !== "object") {
            return "";
        }

        const parts = Object.keys(error.details).map(function (key) {
            return key + ": " + error.details[key];
        });

        return parts.length ? " (" + parts.join("; ") + ")" : "";

    }


    /* ================= ẨN / HIỆN, XOÁ ================= */

    /* PUT cần đủ trường: dựng lại từ dữ liệu đang có, chỉ đảo isActive */

    async function toggleActive(item) {

        const body = {
            name: item.name,
            slug: item.slug,
            description: item.description,
            isActive: item.isActive === false
        };

        body[config.imageField] = item[config.imageField];

        if (config.hasParent) {
            body.parentId = item.parentId;
            body.displayOrder = item.displayOrder;
        } else {
            body.websiteUrl = item.websiteUrl;
        }

        try {

            await apiRequest(config.path + "/" + item.id, { method: "PUT", body: body, auth: true });

            showToast((body.isActive ? "Đã hiện " : "Đã ẩn ") + config.noun + " " + item.name + ".", "success");

            await loadItems();

        } catch (error) {

            handleError(error, function (message) {
                showToast(message, "error");
            });

        }

    }


    /*
     * Xoá luôn hỏi lại. Danh mục còn sản phẩm: xoá KÈM sản phẩm (DELETE ?withProducts=true) và phải gõ đúng
     * tên danh mục; máy chủ từ chối nếu có sản phẩm đã bán / có tồn kho / có đánh giá (details.reason HISTORY).
     */

    async function confirmDelete(item) {

        let count = productCounts.get(item.id);

        if (count === undefined || count === null) {

            try {
                count = (await apiRequest("/products?size=1&" + config.productParam + "=" + item.id)).totalElements;
            } catch (error) {
                count = null;
            }

        }

        const withProducts = config.hasParent && count !== 0;

        if (!withProducts) {

            openConfirmModal({
                title: "Xoá " + config.noun + " " + item.name + "?",
                message: count > 0
                    ? "Đang có " + count + " sản phẩm thuộc " + config.noun + " này nên máy chủ sẽ từ chối xoá. "
                        + "Hãy chuyển các sản phẩm sang " + config.noun + " khác, hoặc bấm Ẩn thay vì xoá."
                    : "Không thể hoàn tác.",
                confirmLabel: "XOÁ",
                cancelLabel: "Quay lại",
                onConfirm: function () {
                    deleteItem(item, false);
                }
            });

            return;

        }

        confirmDeleteWithProducts(item, count);

    }


    function confirmDeleteWithProducts(item, count) {

        openConfirmModal({
            title: "Xoá danh mục " + item.name + " và " + (count === null ? "toàn bộ" : count) + " sản phẩm?",
            message: "Xoá danh mục sẽ XOÁ VĨNH VIỄN " + (count === null ? "mọi" : count) + " sản phẩm của nó "
                + "(cả ảnh, phiên bản, thông số) và không thể hoàn tác. Chỉ xoá được khi chưa sản phẩm nào có đơn hàng, "
                + "tồn kho hay đánh giá. Nếu chỉ muốn tạm ngừng bán, hãy bấm Quay lại rồi chọn Ẩn.",
            confirmLabel: "XOÁ VĨNH VIỄN",
            cancelLabel: "Quay lại",
            input: {
                label: "Gõ đúng tên danh mục \"" + item.name + "\" để xác nhận",
                placeholder: item.name,
                maxLength: 100
            },
            onConfirm: function (typed) {

                if ((typed || "").trim() !== item.name.trim()) {

                    showToast("Tên gõ vào không khớp, chưa xoá gì.", "error");

                    return;

                }

                deleteItem(item, true);

            }
        });

    }


    async function deleteItem(item, withProducts) {

        try {

            await apiRequest(config.path + "/" + item.id + (withProducts ? "?withProducts=true" : ""), {
                method: "DELETE",
                auth: true
            });

            showToast("Đã xoá " + config.noun + " " + item.name + (withProducts ? " và các sản phẩm của nó." : "."), "success");

            if (editing && editing.id === item.id) {
                closeForm();
            }

            productCounts.delete(item.id);

            await loadItems();

        } catch (error) {

            handleError(error, function (message) {

                let text = message;

                if (error.code === "RESOURCE_IN_USE") {

                    const reason = error.details && error.details.reason;

                    /* Trang đếm 0 nhưng máy chủ còn thấy sản phẩm (vd. đã xoá mềm) → hỏi xoá kèm sản phẩm */
                    if (reason === "PRODUCTS" && config.hasParent && !withProducts) {

                        confirmDeleteWithProducts(item, Number(error.details.products) || null);

                        return;

                    }

                    text = reason === "CHILDREN" || /child/i.test(error.message || "")
                        ? "Không xoá được: danh mục " + item.name + " còn danh mục con. Hãy chuyển hoặc xoá các danh mục con trước."
                        : reason === "HISTORY"
                            ? "Không xoá được: " + error.message + "."
                            : "Không xoá được: " + config.noun + " " + item.name + " vẫn còn sản phẩm. Hãy chuyển sản phẩm sang "
                                + config.noun + " khác hoặc bấm Ẩn.";

                }

                showToast(text, "error");

            });

        }

    }


    /* ================= TIỆN ÍCH ================= */

    function normalizeText(value) {

        return String(value || "").normalize("NFD").replace(/\p{M}+/gu, "").replace(/đ/g, "d").replace(/Đ/g, "D").toLowerCase().trim();

    }


    function isSafeHttpUrl(value) {

        return /^https?:\/\/\S+$/i.test(String(value || "").trim());

    }


    function shortUrl(value) {

        return String(value).replace(/^https?:\/\/(www\.)?/i, "").replace(/\/$/, "");

    }


    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
