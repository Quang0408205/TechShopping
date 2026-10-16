/* ================= PHIÊN BẢN + THÔNG SỐ KỸ THUẬT (form admin/products.html) ================= */

/*
 * Dùng bởi js/admin/products.js. Sửa trong form, ghi khi bấm LƯU (sau khi lưu sản phẩm):
 *   Phiên bản: GET /products/{id}/variants, POST /product-variants, PUT /product-variants/{id},
 *              DELETE /product-variants/{id} (409 RESOURCE_IN_USE nếu đã bán / còn tồn kho).
 *   Thông số:  GET /products/{id}/specifications, POST / PUT / DELETE /product-specifications[/{id}];
 *              mẫu tên theo danh mục: GET /categories/{id}/spec-names (tên hay gặp nhất).
 * Dòng thông số để trống giá trị không được lưu (dòng cũ bị để trống thì xoá).
 */

function createProductExtras() {

    const variantRows = document.getElementById("pfVariantRows");

    const specRows = document.getElementById("pfSpecRows");

    /* Bản gốc từ server (id → dữ liệu) để biết dòng nào thêm / sửa / xoá */
    let originalVariants = new Map();

    let originalSpecs = new Map();

    let session = 0;

    let rowKey = 0;


    document.getElementById("pfVariantAdd").addEventListener("click", function () {
        appendVariantRow(null);
    });

    document.getElementById("pfSpecAdd").addEventListener("click", function () {
        appendSpecRow(null).querySelector("input").focus();
    });

    variantRows.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-row-remove]");

        if (button) {
            button.closest(".admin-extra-row").remove();
        }

    });

    specRows.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-row-action]");

        if (!button) {
            return;
        }

        const row = button.closest(".admin-extra-row");

        if (button.dataset.rowAction === "remove") {
            row.remove();
        } else if (button.dataset.rowAction === "up" && row.previousElementSibling) {
            row.parentNode.insertBefore(row, row.previousElementSibling);
        } else if (button.dataset.rowAction === "down" && row.nextElementSibling) {
            row.parentNode.insertBefore(row.nextElementSibling, row);
        }

    });


    function inputHtml(name, value, attrs) {

        return `<input name="${name}" value="${escapeHtml(value === null || value === undefined ? "" : String(value))}" ${attrs || ""}>`;

    }


    function appendVariantRow(variant) {

        const row = document.createElement("div");

        row.className = "admin-extra-row admin-variant-row";

        row.dataset.id = variant ? String(variant.id) : "";

        row.dataset.key = String(++rowKey);

        row.innerHTML = `
            ${inputHtml("variantName", variant && variant.variantName, 'maxlength="255" placeholder="Tên, vd 8GB/256GB - Đen" aria-label="Tên phiên bản"')}
            ${inputHtml("ram", variant && variant.ram, 'maxlength="50" placeholder="RAM" aria-label="RAM"')}
            ${inputHtml("storage", variant && variant.storage, 'maxlength="50" placeholder="Bộ nhớ" aria-label="Bộ nhớ"')}
            ${inputHtml("color", variant && variant.color, 'maxlength="50" placeholder="Màu" aria-label="Màu"')}
            ${inputHtml("price", variant && Number(variant.price), 'type="number" min="1" step="1000" placeholder="Giá *" aria-label="Giá"')}
            ${inputHtml("discountPrice", variant && variant.discountPrice !== null && variant.discountPrice !== undefined ? Number(variant.discountPrice) : "", 'type="number" min="0" step="1000" placeholder="Giá KM" aria-label="Giá khuyến mãi"')}
            ${inputHtml("skuVariant", variant && variant.skuVariant, 'maxlength="50" placeholder="SKU" aria-label="SKU phiên bản"')}
            <button type="button" class="admin-extra-remove" data-row-remove aria-label="Xoá phiên bản">✕</button>
        `;

        variantRows.appendChild(row);

        return row;

    }


    function appendSpecRow(spec, name) {

        const row = document.createElement("div");

        row.className = "admin-extra-row admin-spec-row";

        row.dataset.id = spec ? String(spec.id) : "";

        row.innerHTML = `
            <span class="admin-spec-move">
                <button type="button" data-row-action="up" aria-label="Đưa lên">↑</button>
                <button type="button" data-row-action="down" aria-label="Đưa xuống">↓</button>
            </span>
            ${inputHtml("specName", spec ? spec.specName : name, 'maxlength="100" placeholder="Tên thông số" aria-label="Tên thông số"')}
            ${inputHtml("specValue", spec && spec.specValue, 'placeholder="Giá trị" aria-label="Giá trị thông số"')}
            <button type="button" class="admin-extra-remove" data-row-action="remove" aria-label="Xoá thông số">✕</button>
        `;

        specRows.appendChild(row);

        return row;

    }


    function rowValue(row, name) {

        const value = row.querySelector('[name="' + name + '"]').value.trim();

        return value === "" ? null : value;

    }


    /* Mở form: sửa = tải phiên bản + thông số của sản phẩm; thêm mới = 1 dòng phiên bản trống + mẫu thông số */

    async function reset(product, categoryId) {

        const current = ++session;

        originalVariants = new Map();

        originalSpecs = new Map();

        variantRows.innerHTML = "";

        specRows.innerHTML = "";

        if (!product) {

            appendVariantRow(null);

            await applyTemplate(categoryId, current);

            return;

        }

        variantRows.innerHTML = '<p class="admin-field-hint">Đang tải phiên bản…</p>';

        try {

            const results = await Promise.all([
                apiRequest("/products/" + encodeURIComponent(product.id) + "/variants"),
                apiRequest("/products/" + encodeURIComponent(product.id) + "/specifications")
            ]);

            if (current !== session) {
                return;
            }

            variantRows.innerHTML = "";

            results[0].forEach(function (variant) {
                originalVariants.set(String(variant.id), variant);
                appendVariantRow(variant);
            });

            results[1].forEach(function (spec) {
                originalSpecs.set(String(spec.id), spec);
                appendSpecRow(spec);
            });

            if (results[0].length === 0) {
                appendVariantRow(null);
            }

            await applyTemplate(categoryId, current);

        } catch (error) {

            if (current === session) {
                variantRows.innerHTML = '<p class="form-error">' + escapeHtml(getErrorMessage(error)) + "</p>";
            }

        }

    }


    /* Thêm dòng trống cho các tên thông số hay gặp của danh mục mà sản phẩm chưa có */

    async function applyTemplate(categoryId, current) {

        if (!categoryId) {
            return;
        }

        let names = [];

        try {
            names = await apiRequest("/categories/" + encodeURIComponent(categoryId) + "/spec-names?limit=15");
        } catch (error) {
            return;
        }

        if (current !== session) {
            return;
        }

        const existing = new Set(Array.from(specRows.querySelectorAll('[name="specName"]')).map(function (input) {
            return input.value.trim().toLowerCase();
        }));

        names.filter(function (name) {
            return !existing.has(String(name).toLowerCase());
        }).forEach(function (name) {
            appendSpecRow(null, name);
        });

    }


    function changeCategory(categoryId) {

        /* Đổi danh mục: bỏ các dòng mẫu chưa điền, thêm mẫu của danh mục mới */
        specRows.querySelectorAll(".admin-spec-row").forEach(function (row) {
            if (!row.dataset.id && !rowValue(row, "specValue")) {
                row.remove();
            }
        });

        applyTemplate(categoryId, session);

    }


    /* { variants, specs } đã kiểm tra, hoặc { error } */

    function read() {

        if (variantRows.querySelector(".form-error")) {
            return { error: "Chưa tải được phiên bản của sản phẩm, vui lòng đóng form và mở lại." };
        }

        const variants = [];

        const rows = variantRows.querySelectorAll(".admin-variant-row");

        for (let index = 0; index < rows.length; index++) {

            const row = rows[index];

            const label = "Phiên bản " + (index + 1);

            const variant = {
                id: row.dataset.id || null,
                variantName: rowValue(row, "variantName"),
                ram: rowValue(row, "ram"),
                storage: rowValue(row, "storage"),
                color: rowValue(row, "color"),
                price: rowValue(row, "price") === null ? null : Number(rowValue(row, "price")),
                discountPrice: rowValue(row, "discountPrice") === null ? null : Number(rowValue(row, "discountPrice")),
                skuVariant: rowValue(row, "skuVariant")
            };

            if (!variant.variantName) {
                return { error: label + ": vui lòng nhập tên phiên bản." };
            }

            if (!(variant.price > 0)) {
                return { error: label + ": giá phải lớn hơn 0." };
            }

            if (variant.discountPrice !== null && !(variant.discountPrice >= 0 && variant.discountPrice <= variant.price)) {
                return { error: label + ": giá khuyến mãi phải từ 0 tới giá." };
            }

            variants.push(variant);

        }

        if (variants.length === 0) {
            return { error: "Sản phẩm cần ít nhất 1 phiên bản." };
        }

        const skus = variants.map(function (v) { return v.skuVariant; }).filter(Boolean);

        if (new Set(skus).size !== skus.length) {
            return { error: "SKU phiên bản bị trùng nhau." };
        }

        const specs = [];

        specRows.querySelectorAll(".admin-spec-row").forEach(function (row) {

            const name = rowValue(row, "specName");

            const value = rowValue(row, "specValue");

            specs.push({ id: row.dataset.id || null, specName: name, specValue: value });

        });

        const missingName = specs.find(function (spec) { return spec.specValue && !spec.specName; });

        if (missingName) {
            return { error: "Thông số có giá trị \"" + missingName.specValue + "\" chưa có tên." };
        }

        return { variants: variants, specs: specs };

    }


    /* Ghi thay đổi sau khi sản phẩm đã lưu; trả danh sách lỗi (rỗng = xong hết) */

    async function save(productId, data) {

        const problems = [];

        const run = async function (label, action) {

            try {
                await action();
            } catch (error) {
                problems.push(label + ": " + getErrorMessage(error));
            }

        };


        const keptVariants = new Set();

        for (const variant of data.variants) {

            const body = {
                variantName: variant.variantName,
                skuVariant: variant.skuVariant,
                price: variant.price,
                discountPrice: variant.discountPrice,
                color: variant.color,
                storage: variant.storage,
                ram: variant.ram
            };

            if (variant.id) {

                keptVariants.add(variant.id);

                const old = originalVariants.get(variant.id);

                const changed = ["variantName", "skuVariant", "color", "storage", "ram"].some(function (key) {
                    return (old[key] || null) !== body[key];
                }) || Number(old.price) !== body.price
                    || (old.discountPrice === null || old.discountPrice === undefined ? null : Number(old.discountPrice)) !== body.discountPrice;

                if (changed) {
                    /* stockQuantity (cột cũ, không dùng để bán) giữ nguyên */
                    body.stockQuantity = old.stockQuantity;
                    await run(variant.variantName, function () {
                        return apiRequest("/product-variants/" + variant.id, { method: "PUT", body: body, auth: true });
                    });
                }

            } else {

                body.productId = productId;

                await run(variant.variantName, function () {
                    return apiRequest("/product-variants", { method: "POST", body: body, auth: true });
                });

            }

        }

        for (const id of originalVariants.keys()) {

            if (!keptVariants.has(id)) {
                await run("Xoá " + originalVariants.get(id).variantName, function () {
                    return apiRequest("/product-variants/" + id, { method: "DELETE", auth: true });
                });
            }

        }


        const keptSpecs = new Set();

        let order = 0;

        for (const spec of data.specs) {

            if (spec.id && spec.specValue) {

                keptSpecs.add(spec.id);

                const old = originalSpecs.get(spec.id);

                const body = { specName: spec.specName, specValue: spec.specValue, specOrder: order };

                if (old.specName !== body.specName || old.specValue !== body.specValue || old.specOrder !== order) {
                    await run(spec.specName, function () {
                        return apiRequest("/product-specifications/" + spec.id, { method: "PUT", body: body, auth: true });
                    });
                }

                order++;

            } else if (!spec.id && spec.specValue) {

                const body = { productId: productId, specName: spec.specName, specValue: spec.specValue, specOrder: order };

                await run(spec.specName, function () {
                    return apiRequest("/product-specifications", { method: "POST", body: body, auth: true });
                });

                order++;

            }

        }

        for (const id of originalSpecs.keys()) {

            if (!keptSpecs.has(id)) {
                await run("Xoá thông số " + originalSpecs.get(id).specName, function () {
                    return apiRequest("/product-specifications/" + id, { method: "DELETE", auth: true });
                });
            }

        }

        return problems;

    }


    function clear() {

        session++;

        variantRows.innerHTML = "";

        specRows.innerHTML = "";

    }


    return { reset: reset, changeCategory: changeCategory, read: read, save: save, clear: clear };

}
