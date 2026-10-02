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
 *
 * Ảnh sản phẩm (IMG-2):
 *   POST   /admin/uploads/product-images   upload file (multipart "file") → { url }
 *   Thêm mới: ảnh gom ở danh sách nháp, gửi kèm POST /products (images: 1 ảnh
 *             chính + ≥ 1 ảnh phụ). File đã upload mà không lưu sản phẩm thì
 *             vẫn nằm trên server (chưa có API dọn).
 *   Sửa:      mỗi thao tác lưu ngay: GET /products/{id}/images,
 *             POST /product-images, PUT /product-images/{id} (đặt ảnh chính),
 *             DELETE /product-images/{id} (không xoá được ảnh cuối cùng).
 */

const PRODUCT_ADMIN_PAGE_SIZE = 20;

const PRODUCT_IMAGE_MAX_COUNT = 10;

const PRODUCT_IMAGE_MAX_BYTES = 5 * 1024 * 1024;

const PRODUCT_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

const PRODUCT_IMAGE_HINTS = {
    create: "Bắt buộc 1 ảnh chính và ít nhất 1 ảnh phụ (tối đa 10 ảnh). Chọn file JPG / PNG / WebP tối đa 5 MB "
        + "hoặc dán link http(s). Ảnh đầu tiên là ảnh chính; bấm \"Đặt làm ảnh chính\" để đổi.",
    edit: "Thay đổi ảnh được lưu ngay, không cần bấm LƯU. Tối đa 10 ảnh; không xoá được ảnh cuối cùng."
};


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


    /* Ảnh: draftImages khi thêm mới [{ url, isPrimary }], liveImages khi sửa (ProductImageResponse) */

    const imageList = document.getElementById("pfImageList");

    const imageError = document.getElementById("pfImagesError");

    const imageFileInput = document.getElementById("pfImageFile");

    const imageUrlInput = document.getElementById("pfImageUrl");

    let draftImages = [];

    let liveImages = [];

    let liveImagesLoading = false;

    let pendingUploads = 0;

    let imageBusy = false;

    /* imageSession: tăng mỗi lần mở / đóng form (bỏ kết quả upload cũ); imageLoadCounter: mỗi lần tải danh sách ảnh */
    let imageSession = 0;

    let imageLoadCounter = 0;


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


    imageFileInput.addEventListener("change", function () {

        const files = Array.from(imageFileInput.files || []);

        imageFileInput.value = "";

        addImageFiles(files);

    });

    document.getElementById("pfImageUrlBtn").addEventListener("click", addImageLink);

    imageUrlInput.addEventListener("keydown", function (event) {

        /* Enter trong ô link: thêm ảnh, không gửi form sản phẩm */
        if (event.key === "Enter") {
            event.preventDefault();
            addImageLink();
        }

    });

    imageList.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-image-action]");

        if (!button || button.disabled) {
            return;
        }

        if (button.dataset.imageAction === "primary") {
            setPrimaryImage(button.dataset.key);
        } else if (button.dataset.imageAction === "remove") {
            removeImage(button.dataset.key);
        }

    });


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
                    <div class="admin-product-cell">
                        ${isSafeImageUrl(product.primaryImageUrl)
                            ? `<img class="admin-product-thumb" src="${escapeHtml(product.primaryImageUrl.trim())}" alt="" loading="lazy">`
                            : `<span class="admin-product-thumb admin-product-thumb--empty" title="Chưa có ảnh">—</span>`}
                        <div>
                            <a href="${escapeHtml(getProductDetailUrl(product.id))}" target="_blank" rel="noopener">${escapeHtml(product.name)}</a>
                            <div class="admin-subtext">#${id}${product.sku ? " · " + escapeHtml(product.sku) : ""}</div>
                        </div>
                    </div>
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

        resetImages(product);

        panel.hidden = false;

        field("pfName").focus();

        panel.scrollIntoView({ behavior: "smooth", block: "start" });

    }


    function closeForm() {

        editingProduct = null;

        panel.hidden = true;

        /* Bỏ phản hồi ảnh còn đang chờ của sản phẩm vừa đóng */
        imageSession++;

        imageLoadCounter++;

        liveImagesLoading = false;

        draftImages = [];

        liveImages = [];

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


        /* Thêm mới: gửi ảnh cùng sản phẩm, ảnh chính đứng đầu (thứ tự = thứ tự hiển thị) */

        if (!editingProduct) {

            if (pendingUploads > 0) {
                return { error: "Ảnh đang được tải lên, vui lòng chờ xong rồi bấm LƯU." };
            }

            const primary = draftImages.filter(function (image) { return image.isPrimary; });

            if (primary.length !== 1 || draftImages.length < 2) {
                return { error: "Sản phẩm mới cần 1 ảnh chính và ít nhất 1 ảnh phụ." };
            }

            body.images = primary.concat(draftImages.filter(function (image) { return !image.isPrimary; }))
                .map(function (image) {
                    return { imageUrl: image.url, isPrimary: image.isPrimary };
                });

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

            const wasEditing = editingProduct !== null;

            showToast((wasEditing ? "Đã lưu " : "Đã thêm ") + saved.name + ".", "success");

            closeForm();

            /* closeForm() xoá editingProduct, nên phải nhớ chế độ trước đó: sửa thì giữ trang hiện tại */
            if (!wasEditing) {
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


    /* ================= ẢNH SẢN PHẨM (IMG-2) ================= */

    function resetImages(product) {

        imageSession++;

        imageLoadCounter++;

        liveImagesLoading = false;

        draftImages = [];

        liveImages = [];

        pendingUploads = 0;

        imageBusy = false;

        imageUrlInput.value = "";

        hideImageError();

        field("pfImagesHint").textContent = product ? PRODUCT_IMAGE_HINTS.edit : PRODUCT_IMAGE_HINTS.create;

        if (product) {
            loadLiveImages();
        } else {
            renderImages();
        }

    }


    async function loadLiveImages() {

        const product = editingProduct;

        const requestId = ++imageLoadCounter;

        liveImagesLoading = true;

        renderImages();


        try {

            const images = await apiRequest("/products/" + encodeURIComponent(product.id) + "/images");

            if (requestId !== imageLoadCounter) {
                return;
            }

            liveImages = Array.isArray(images) ? images : [];

        } catch (error) {

            if (requestId !== imageLoadCounter) {
                return;
            }

            handleError(error, showImageError);

        }

        liveImagesLoading = false;

        renderImages();

    }


    /* Danh sách chung cho cả hai chế độ: { key, url, isPrimary } */

    function currentImages() {

        if (editingProduct) {

            return liveImages.map(function (image) {
                return { key: "id-" + image.id, url: image.imageUrl, isPrimary: image.isPrimary === true };
            });

        }

        return draftImages.map(function (image, index) {
            return { key: "draft-" + index, url: image.url, isPrimary: image.isPrimary };
        });

    }


    function renderImages() {

        if (liveImagesLoading) {

            imageList.innerHTML = `<p class="admin-subtext">Đang tải ảnh…</p>`;

            setImageAddEnabled(false);

            return;

        }


        const images = currentImages();

        const tiles = images.map(function (image) {

            const key = escapeHtml(image.key);

            const disabled = imageBusy ? "disabled" : "";

            return `
                <figure class="admin-image-tile ${image.isPrimary ? "is-primary" : ""}" data-key="${key}">
                    <div class="admin-image-thumb">
                        ${isSafeImageUrl(image.url) ? `<img src="${escapeHtml(image.url.trim())}" alt="" loading="lazy">` : ""}
                    </div>
                    <figcaption>
                        ${image.isPrimary
                            ? `<span class="admin-badge admin-badge-success">Ảnh chính</span>`
                            : `<button type="button" class="admin-link-btn" data-image-action="primary" data-key="${key}" ${disabled}>Đặt làm ảnh chính</button>`}
                        <button type="button" class="admin-link-btn admin-link-danger" data-image-action="remove" data-key="${key}" ${disabled}>Xoá</button>
                    </figcaption>
                </figure>
            `;

        });

        for (let i = 0; i < pendingUploads; i++) {
            tiles.push(`<figure class="admin-image-tile is-uploading"><div class="admin-image-thumb">Đang tải lên…</div></figure>`);
        }


        imageList.innerHTML = tiles.length > 0
            ? tiles.join("") + `<p class="admin-image-count admin-subtext">${images.length} / ${PRODUCT_IMAGE_MAX_COUNT} ảnh</p>`
            : `<p class="admin-subtext">Chưa có ảnh nào.</p>`;


        /* Ảnh hỏng (link sai, CDN lỗi): hiện ô xám thay vì biểu tượng ảnh vỡ */
        imageList.querySelectorAll(".admin-image-thumb img").forEach(function (img) {
            img.addEventListener("error", function () {
                img.closest(".admin-image-thumb").classList.add("is-broken");
                img.remove();
            });
        });

        setImageAddEnabled(!imageBusy && images.length + pendingUploads < PRODUCT_IMAGE_MAX_COUNT);

    }


    function setImageAddEnabled(enabled) {

        imageFileInput.disabled = !enabled;

        imageUrlInput.disabled = !enabled;

        field("pfImageUrlBtn").disabled = !enabled;

        field("pfImageAdd").classList.toggle("is-disabled", !enabled);

    }


    function showImageError(message) {

        imageError.textContent = message;

        imageError.hidden = false;

    }


    function hideImageError() {

        imageError.textContent = "";

        imageError.hidden = true;

    }


    function remainingImageSlots() {

        return PRODUCT_IMAGE_MAX_COUNT - currentImages().length - pendingUploads;

    }


    /* File từ máy: kiểm tra trước (backend kiểm tra lại theo nội dung file), upload lần lượt */

    async function addImageFiles(files) {

        hideImageError();

        if (files.length === 0) {
            return;
        }


        const problems = [];

        const accepted = [];

        files.forEach(function (file) {

            if (PRODUCT_IMAGE_TYPES.indexOf(file.type) === -1) {
                problems.push(file.name + ": chỉ nhận JPG, PNG hoặc WebP");
            } else if (file.size > PRODUCT_IMAGE_MAX_BYTES) {
                problems.push(file.name + ": lớn hơn 5 MB");
            } else if (accepted.length >= remainingImageSlots()) {
                problems.push(file.name + ": đã đủ " + PRODUCT_IMAGE_MAX_COUNT + " ảnh");
            } else {
                accepted.push(file);
            }

        });


        const session = imageSession;

        pendingUploads += accepted.length;

        renderImages();


        for (const file of accepted) {

            let url = null;

            try {

                const formData = new FormData();

                formData.append("file", file);

                const uploaded = await apiRequest("/admin/uploads/product-images", {
                    method: "POST", body: formData, auth: true
                });

                url = uploaded.url;

            } catch (error) {

                handleError(error, function (message) {
                    problems.push(file.name + ": " + message);
                });

            }


            /* Form đã đóng / chuyển sang sản phẩm khác trong lúc upload: bỏ kết quả */
            if (session !== imageSession) {
                return;
            }

            pendingUploads--;

            if (url) {
                await addImageUrl(url, problems);
            }

            renderImages();

        }


        if (problems.length > 0) {
            showImageError(problems.join(" · "));
        }

    }


    async function addImageLink() {

        hideImageError();

        const url = imageUrlInput.value.trim();

        if (!url) {

            showImageError("Vui lòng dán link ảnh.");

            return;

        }

        if (!isSafeImageUrl(url) || /\s/.test(url) || url.length > 2048) {

            showImageError("Link ảnh phải bắt đầu bằng http:// hoặc https:// và không chứa khoảng trắng.");

            return;

        }

        if (remainingImageSlots() <= 0) {

            showImageError("Mỗi sản phẩm có tối đa " + PRODUCT_IMAGE_MAX_COUNT + " ảnh.");

            return;

        }


        const problems = [];

        if (await addImageUrl(url, problems)) {
            imageUrlInput.value = "";
        }

        renderImages();

        if (problems.length > 0) {
            showImageError(problems.join(" · "));
        }

    }


    /* Thêm một ảnh (đã có URL): nháp khi thêm mới, POST /product-images khi sửa. Trả về true nếu thêm được */

    async function addImageUrl(url, problems) {

        if (currentImages().some(function (image) { return image.url === url; })) {

            problems.push("Ảnh này đã có trong danh sách.");

            return false;

        }


        if (!editingProduct) {

            draftImages.push({ url: url, isPrimary: draftImages.length === 0 });

            return true;

        }


        const nextOrder = liveImages.reduce(function (max, image) {
            return Math.max(max, image.displayOrder === null || image.displayOrder === undefined ? -1 : image.displayOrder);
        }, -1) + 1;

        return runImageAction(function () {
            return apiRequest("/product-images", {
                method: "POST",
                body: { productId: editingProduct.id, imageUrl: url, displayOrder: nextOrder },
                auth: true
            });
        }, "Đã thêm ảnh.", problems);

    }


    function findImage(key) {

        if (editingProduct) {

            return liveImages.find(function (image) {
                return "id-" + image.id === key;
            }) || null;

        }

        const index = Number(String(key).replace("draft-", ""));

        return draftImages[index] || null;

    }


    async function setPrimaryImage(key) {

        hideImageError();

        const image = findImage(key);

        if (!image) {
            return;
        }


        if (!editingProduct) {

            draftImages.forEach(function (draft) {
                draft.isPrimary = draft === image;
            });

            renderImages();

            return;

        }


        const problems = [];

        await runImageAction(function () {
            return apiRequest("/product-images/" + image.id, {
                method: "PUT",
                body: { imageUrl: image.imageUrl, altText: image.altText, displayOrder: image.displayOrder, isPrimary: true },
                auth: true
            });
        }, "Đã đổi ảnh chính.", problems);

        renderImages();

        if (problems.length > 0) {
            showImageError(problems.join(" · "));
        }

    }


    function removeImage(key) {

        hideImageError();

        const image = findImage(key);

        if (!image) {
            return;
        }


        if (!editingProduct) {

            draftImages.splice(draftImages.indexOf(image), 1);

            if (image.isPrimary && draftImages.length > 0) {
                draftImages[0].isPrimary = true;
            }

            renderImages();

            return;

        }


        if (liveImages.length <= 1) {

            showImageError(API_ERROR_MESSAGES.LAST_PRODUCT_IMAGE);

            return;

        }


        openConfirmModal({
            title: "Xoá ảnh?",
            message: image.isPrimary
                ? "Đây là ảnh chính: ảnh kế tiếp sẽ trở thành ảnh chính. Ảnh bị xoá ngay khỏi website."
                : "Ảnh bị xoá ngay khỏi website.",
            confirmLabel: "XOÁ",
            onConfirm: async function () {

                const problems = [];

                await runImageAction(function () {
                    return apiRequest("/product-images/" + image.id, { method: "DELETE", auth: true });
                }, "Đã xoá ảnh.", problems);

                renderImages();

                if (problems.length > 0) {
                    showImageError(problems.join(" · "));
                }

            }
        });

    }


    /* Chế độ sửa: chạy một thao tác ảnh, khoá nút trong lúc chờ, rồi tải lại danh sách ảnh từ server */

    async function runImageAction(action, successMessage, problems) {

        const product = editingProduct;

        imageBusy = true;

        renderImages();

        let ok = false;

        try {

            await action();

            ok = true;

            showToast(successMessage, "success");

        } catch (error) {

            handleError(error, function (message) {
                problems.push(message);
            });

        }

        imageBusy = false;

        if (editingProduct === product) {

            await loadLiveImages();

            /* Ảnh chính đổi → cập nhật ảnh nhỏ ở bảng */
            loadProducts();

        }

        return ok;

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
