/* ================= SẢN PHẨM (admin/products.html) — B2 ================= */

/*
 * API catalogue thật (ghi cần ADMIN, backend kiểm tra ở mọi request):
 *   GET    /products?keyword=&categoryId=&isActive=&page=&size=&sort=
 *   POST   /products                 thêm
 *   PUT    /products/{id}            sửa: PUT thay toàn bộ, trường bỏ trống bị
 *                                    xoá (trừ tồn kho / bảo hành / isActive),
 *                                    nên luôn gửi đủ các trường hiện có
 *   DELETE /products/{id}            xoá mềm (sản phẩm biến mất khỏi website)
 *   GET    /categories, /brands      danh sách cho ô chọn
 */

const PRODUCT_ADMIN_PAGE_SIZE = 20;


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const tbody = document.getElementById("productTableBody");

    const pagination = document.getElementById("productPagination");

    const panel = document.getElementById("productFormPanel");

    const form = document.getElementById("productForm");

    const formError = document.getElementById("productFormError");


    let currentPage = 0;

    let currentProducts = [];

    let editingProduct = null;

    let requestCounter = 0;

    let categories = [];

    let brands = [];


    try {

        const results = await Promise.all([
            apiRequest("/categories?size=100&sort=name,asc"),
            apiRequest("/brands?size=100&sort=name,asc")
        ]);

        categories = results[0].content || [];

        brands = results[1].content || [];

    } catch (error) {

        showToast(getErrorMessage(error), "error");

    }


    fillSelect(document.getElementById("productCategoryFilter"), categories, "Mọi danh mục");

    fillSelect(document.getElementById("pfCategory"), categories, "— Chọn danh mục —");

    fillSelect(document.getElementById("pfBrand"), brands, "— Không có —");


    document.getElementById("productFilterForm").addEventListener("submit", function (event) {

        event.preventDefault();

        currentPage = 0;

        loadProducts();

    });

    ["productCategoryFilter", "productStatusFilter"].forEach(function (id) {

        document.getElementById(id).addEventListener("change", function () {
            currentPage = 0;
            loadProducts();
        });

    });


    document.getElementById("newProductBtn").addEventListener("click", function () {
        openForm(null);
    });

    document.getElementById("productCancelBtn").addEventListener("click", closeForm);

    form.addEventListener("submit", saveProduct);


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const product = currentProducts.find(function (p) {
            return String(p.id) === button.dataset.id;
        });

        if (!product) {
            return;
        }


        if (button.dataset.action === "edit") {
            openForm(product);
        } else if (button.dataset.action === "toggle") {
            toggleActive(product);
        } else if (button.dataset.action === "delete") {
            confirmDelete(product);
        }

    });


    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        loadProducts();

    });


    loadProducts();


    /* ================= DANH SÁCH ================= */

    function fillSelect(select, items, emptyLabel) {

        select.innerHTML = `<option value="">${escapeHtml(emptyLabel)}</option>` +
            items.map(function (item) {
                return `<option value="${escapeHtml(String(item.id))}">${escapeHtml(item.name)}</option>`;
            }).join("");

    }


    async function loadProducts() {

        const requestId = ++requestCounter;

        const params = new URLSearchParams({
            page: String(currentPage),
            size: String(PRODUCT_ADMIN_PAGE_SIZE),
            sort: "id,desc"
        });

        const keyword = document.getElementById("productKeyword").value.trim();

        const categoryId = document.getElementById("productCategoryFilter").value;

        const status = document.getElementById("productStatusFilter").value;

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (categoryId) {
            params.set("categoryId", categoryId);
        }

        if (status) {
            params.set("isActive", status);
        }


        tbody.innerHTML = adminEmptyRow(6, "Đang tải…");


        try {

            const page = await apiRequest("/products?" + params.toString());

            if (requestId !== requestCounter) {
                return;
            }

            currentProducts = page.content || [];

            document.getElementById("productRowCount").textContent = page.totalElements + " sản phẩm";

            tbody.innerHTML = currentProducts.map(renderRow).join("") ||
                adminEmptyRow(6, "Không có sản phẩm nào phù hợp bộ lọc");

            renderPagination(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(6, message);
                });
            }

        }

    }


    function renderRow(product) {

        const id = escapeHtml(String(product.id));

        const active = product.isActive !== false;

        const price = getDisplayPrice(product);

        const hasDiscount = product.discountPrice !== null && product.discountPrice !== undefined &&
            Number(product.discountPrice) < Number(product.basePrice);


        return `
            <tr data-product-id="${id}">
                <td>
                    <a href="${escapeHtml(getProductDetailUrl(product.id))}" target="_blank" rel="noopener">${escapeHtml(product.name)}</a>
                    <div class="admin-subtext">#${id}${product.sku ? " · " + escapeHtml(product.sku) : ""}</div>
                </td>
                <td>${escapeHtml(product.categoryName || "—")}</td>
                <td>${escapeHtml(product.brandName || "—")}</td>
                <td>
                    ${price > 0 ? formatPrice(price) : "Liên hệ"}
                    ${hasDiscount ? `<div class="admin-subtext"><s>${formatPrice(Number(product.basePrice))}</s></div>` : ""}
                </td>
                <td>
                    <span class="admin-badge ${active ? "admin-badge-success" : "admin-badge-neutral"}">
                        ${active ? "Đang bán" : "Đã ẩn"}
                    </span>
                </td>
                <td class="admin-actions-cell">
                    <button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>
                    <button type="button" class="admin-link-btn" data-action="toggle" data-id="${id}">${active ? "Ẩn" : "Hiện"}</button>
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


    function openForm(product) {

        editingProduct = product;

        field("productFormTitle").textContent = product ? "Sửa sản phẩm #" + product.id : "Thêm sản phẩm";

        field("pfName").value = product ? product.name : "";

        field("pfCategory").value = product ? String(product.categoryId) : "";

        field("pfBrand").value = product && product.brandId ? String(product.brandId) : "";

        field("pfBasePrice").value = product ? Number(product.basePrice) : "";

        field("pfDiscountPrice").value = product && product.discountPrice !== null && product.discountPrice !== undefined ? Number(product.discountPrice) : "";

        field("pfSku").value = product && product.sku ? product.sku : "";

        field("pfSlug").value = product ? product.slug : "";

        field("pfStock").value = product && product.stockQuantity !== null ? product.stockQuantity : "";

        field("pfWarranty").value = product && product.warrantyMonths !== null ? product.warrantyMonths : "";

        field("pfWeight").value = product && product.weight !== null && product.weight !== undefined ? Number(product.weight) : "";

        field("pfDescription").value = product && product.description ? product.description : "";

        field("pfActive").checked = product ? product.isActive !== false : true;


        formError.hidden = true;

        panel.hidden = false;

        field("pfName").focus();

        panel.scrollIntoView({ behavior: "smooth", block: "start" });

    }


    function closeForm() {

        editingProduct = null;

        panel.hidden = true;

    }


    function numberOrNull(id) {

        const value = field(id).value.trim();

        return value === "" ? null : Number(value);

    }


    function textOrNull(id) {

        const value = field(id).value.trim();

        return value === "" ? null : value;

    }


    /* Body cho POST / PUT, kiểm tra trước những gì backend sẽ từ chối */

    function readForm() {

        const body = {
            name: field("pfName").value.trim(),
            slug: textOrNull("pfSlug"),
            description: textOrNull("pfDescription"),
            categoryId: numberOrNull("pfCategory"),
            brandId: numberOrNull("pfBrand"),
            basePrice: numberOrNull("pfBasePrice"),
            discountPrice: numberOrNull("pfDiscountPrice"),
            stockQuantity: numberOrNull("pfStock"),
            sku: textOrNull("pfSku"),
            weight: numberOrNull("pfWeight"),
            warrantyMonths: numberOrNull("pfWarranty"),
            isActive: field("pfActive").checked
        };


        if (!body.name) {
            return { error: "Vui lòng nhập tên sản phẩm." };
        }

        if (!body.categoryId) {
            return { error: "Vui lòng chọn danh mục." };
        }

        if (body.basePrice === null || !(body.basePrice >= 0)) {
            return { error: "Giá gốc phải là số không âm." };
        }

        if (body.discountPrice !== null && !(body.discountPrice >= 0 && body.discountPrice <= body.basePrice)) {
            return { error: "Giá khuyến mãi phải từ 0 tới giá gốc." };
        }

        if (body.slug && !/^[a-z0-9]+(-[a-z0-9]+)*$/.test(body.slug)) {
            return { error: "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang (vd. dien-thoai-abc)." };
        }

        if ([body.stockQuantity, body.warrantyMonths].some(function (n) { return n !== null && !(Number.isInteger(n) && n >= 0); })) {
            return { error: "Tồn kho và bảo hành phải là số nguyên không âm." };
        }

        if (body.weight !== null && !(body.weight >= 0)) {
            return { error: "Khối lượng không được âm." };
        }

        return { body: body };

    }


    async function saveProduct(event) {

        event.preventDefault();

        const result = readForm();

        if (result.error) {

            formError.textContent = result.error;

            formError.hidden = false;

            return;

        }


        const saveButton = field("productSaveBtn");

        saveButton.disabled = true;

        formError.hidden = true;


        try {

            const saved = editingProduct
                ? await apiRequest("/products/" + editingProduct.id, { method: "PUT", body: result.body, auth: true })
                : await apiRequest("/products", { method: "POST", body: result.body, auth: true });

            showToast((editingProduct ? "Đã lưu " : "Đã thêm ") + saved.name + ".", "success");

            closeForm();

            if (!editingProduct) {
                currentPage = 0;
            }

            loadProducts();

        } catch (error) {

            handleError(error, function (message) {
                formError.textContent = message + detailsText(error);
                formError.hidden = false;
            });

        } finally {

            saveButton.disabled = false;

        }

    }


    /* Chi tiết lỗi từng trường của VALIDATION_ERROR (backend trả tiếng Anh) */

    function detailsText(error) {

        if (!error.details || typeof error.details !== "object") {
            return "";
        }

        const parts = Object.keys(error.details).map(function (key) {
            return key + ": " + error.details[key];
        });

        return parts.length > 0 ? " (" + parts.join("; ") + ")" : "";

    }


    /* ================= ẨN / HIỆN, XOÁ ================= */

    /* PUT đủ các trường hiện có, chỉ đổi isActive */

    async function toggleActive(product) {

        const body = {
            name: product.name,
            slug: product.slug,
            description: product.description,
            categoryId: product.categoryId,
            brandId: product.brandId,
            basePrice: product.basePrice,
            discountPrice: product.discountPrice,
            stockQuantity: product.stockQuantity,
            sku: product.sku,
            weight: product.weight,
            warrantyMonths: product.warrantyMonths,
            isActive: product.isActive === false
        };


        try {

            await apiRequest("/products/" + product.id, { method: "PUT", body: body, auth: true });

            showToast((body.isActive ? "Đã hiện " : "Đã ẩn ") + product.name + ".", "success");

            loadProducts();

        } catch (error) {

            handleError(error, function (message) {
                showToast(message, "error");
            });

        }

    }


    function confirmDelete(product) {

        openConfirmModal({
            title: "Xoá sản phẩm?",
            message: "\"" + product.name + "\" sẽ bị xoá khỏi website (xoá mềm, không khôi phục được từ giao diện). Muốn tạm dừng bán thì dùng \"Ẩn\".",
            confirmLabel: "XOÁ",
            onConfirm: async function () {

                try {

                    await apiRequest("/products/" + product.id, { method: "DELETE", auth: true });

                    showToast("Đã xoá " + product.name + ".", "success");

                    loadProducts();

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

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
