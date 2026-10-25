/* ================= PHIÊN BẢN + MÀU + THÔNG SỐ KỸ THUẬT (form admin/products.html) ================= */

/*
 * Dùng bởi js/admin/products.js. Sửa trong form, ghi khi bấm LƯU (sau khi lưu sản phẩm).
 * Phiên bản nằm TRONG sản phẩm: mỗi thẻ = 1 phiên bản (RAM + bộ nhớ, giá), trong thẻ có các màu.
 * Mỗi (phiên bản, màu) là một dòng product_variants — dòng đã có giữ nguyên id (giỏ / đơn / tồn kho không hỏng).
 *   Phiên bản: GET /products/{id}/variants, POST /product-variants, PUT /product-variants/{id},
 *              DELETE /product-variants/{id} (409 RESOURCE_IN_USE nếu đã bán / còn tồn kho).
 *   Thông số:  GET /products/{id}/specifications, POST / PUT / DELETE /product-specifications[/{id}];
 *              dòng không có variantId là thông số chung; thông số riêng của thẻ phiên bản được ghi
 *              cho từng màu của thẻ (variantId) và ghi đè dòng chung cùng tên trên trang khách.
 *              Mẫu tên theo danh mục: GET /categories/{id}/spec-names (tên hay gặp nhất).
 * Dòng thông số để trống giá trị không được lưu (dòng cũ bị để trống thì xoá).
 */

function createProductExtras() {

    const variantRows = document.getElementById("pfVariantRows");

    const specRows = document.getElementById("pfSpecRows");

    const highlightBar = document.getElementById("pfHighlightBar");

    /* Tên thông số đã dùng trong danh mục (gợi ý + để cảnh báo tên lạ), chữ thường */
    let knownSpecNames = [];

    const TEMPLATE_ROWS = 15;

    /* Bản gốc từ server (id → dữ liệu) để biết dòng nào thêm / sửa / xoá */
    let originalVariants = new Map();

    let originalSpecs = new Map();

    let session = 0;

    let rowKey = 0;

    let colorsListener = null;

    const needBox = document.getElementById("pfNeeds");

    const needList = document.getElementById("pfNeedList");

    let originalNeedIds = [];

    let needsLoaded = false;


    document.getElementById("pfVariantAdd").addEventListener("click", function () {
        appendVersionCard(null);
        notifyColors();
    });

    document.getElementById("pfSpecAdd").addEventListener("click", function () {
        appendSpecRow(specRows, null).querySelector("input").focus();
    });

    variantRows.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-version-action], button[data-row-action]");

        if (!button) {
            return;
        }

        const card = button.closest(".admin-version-card");

        const action = button.dataset.versionAction || button.dataset.rowAction;

        if (action === "remove-version") {

            card.remove();

            renumberCards();

        } else if (action === "clone-version") {

            const copy = appendVersionCard(readCard(card, true));

            copy.scrollIntoView({ block: "nearest", behavior: "smooth" });

        } else if (action === "add-color") {

            appendColorRow(card.querySelector(".admin-version-colors"), null).querySelector("input").focus();

        } else if (action === "remove-color") {

            const row = button.closest(".admin-color-row");

            if (card.querySelectorAll(".admin-color-row").length > 1) {
                row.remove();
            } else {
                row.querySelectorAll("input").forEach(function (input) { input.value = ""; });
            }

        } else if (action === "add-spec") {

            appendSpecRow(card.querySelector(".admin-version-specs"), null).querySelector("input").focus();

        } else if (action === "remove" || action === "up" || action === "down") {

            moveOrRemoveSpec(button.closest(".admin-extra-row"), action);

        }

        notifyColors();

    });

    variantRows.addEventListener("input", function (event) {

        if (event.target.name === "color") {
            notifyColors();
        }

    });

    /* Nhãn nhóm, xem trước thẻ nhỏ và ô nổi bật cập nhật ngay khi gõ */
    [specRows, variantRows].forEach(function (box) {

        box.addEventListener("input", function (event) {

            const row = event.target.closest(".admin-spec-row");

            if (row) {
                refreshSpecRow(row);
                refreshHighlights();
            }

        });

    });

    specRows.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-row-action]");

        if (button) {
            moveOrRemoveSpec(button.closest(".admin-extra-row"), button.dataset.rowAction);

            refreshHighlights();
        }

    });


    /* ----- nhu cầu của danh mục: ô chọn nhiều, ẩn khi danh mục chưa có nhu cầu nào ----- */

    async function loadNeeds(categoryId, productId, current) {

        needsLoaded = false;

        needList.innerHTML = "";

        needBox.hidden = true;

        if (!categoryId) {
            return;
        }

        let needs;

        let own = [];

        try {

            needs = await apiRequest("/categories/" + encodeURIComponent(categoryId) + "/needs");

            if (productId) {
                own = (await apiRequest("/products/" + encodeURIComponent(productId) + "/needs")).map(function (need) { return need.id; });
            }

        } catch (error) {
            return;
        }

        if (current !== session || needs.length === 0) {
            return;
        }

        originalNeedIds = own;

        needsLoaded = true;

        needBox.hidden = false;

        needList.innerHTML = needs.map(function (need) {

            return `<label class="admin-need-item"><input type="checkbox" name="need" value="${need.id}" ${own.indexOf(need.id) !== -1 ? "checked" : ""}> ${escapeHtml(need.name)}</label>`;

        }).join("");

    }

    function readNeedIds() {

        return Array.from(needList.querySelectorAll('input[name="need"]:checked')).map(function (box) { return Number(box.value); });

    }


    function moveOrRemoveSpec(row, action) {

        if (action === "remove") {
            row.remove();
        } else if (action === "up" && row.previousElementSibling && row.previousElementSibling.classList.contains("admin-spec-row")) {
            row.parentNode.insertBefore(row, row.previousElementSibling);
        } else if (action === "down" && row.nextElementSibling) {
            row.parentNode.insertBefore(row.nextElementSibling, row);
        }

    }


    function rowValue(row, name) {

        const element = row.querySelector('[name="' + name + '"]');

        const value = element ? element.value.trim() : "";

        return value === "" ? null : value;

    }

    function norm(value) {

        return String(value || "").replace(/\s+/g, "").toLowerCase();

    }


    /* ----- màu đang khai báo (cho ô chọn màu của ảnh) ----- */

    function getColors() {

        const seen = new Set();

        const colors = [];

        variantRows.querySelectorAll('.admin-color-row [name="color"]').forEach(function (input) {

            const value = input.value.trim();

            if (value && !seen.has(value.toLowerCase())) {
                seen.add(value.toLowerCase());
                colors.push(value);
            }

        });

        return colors;

    }

    function notifyColors() {

        if (colorsListener) {
            colorsListener(getColors());
        }

    }

    function onColorsChange(listener) {

        colorsListener = listener;

    }


    function inputHtml(name, value, attrs) {

        return `<input name="${name}" value="${escapeHtml(value === null || value === undefined ? "" : String(value))}" ${attrs || ""}>`;

    }


    function renumberCards() {

        variantRows.querySelectorAll(".admin-version-card").forEach(function (card, index) {
            card.querySelector(".admin-version-title").textContent = "Phiên bản " + (index + 1);
        });

    }


    /*
     * data = { ram, storage, price, discountPrice, colors: [{ id, color, skuVariant, price }], specs: [{ name, value }],
     *          initPrice, initDiscount } hoặc null (thẻ trống: 1 dòng màu trống)
     */

    function appendVersionCard(data) {

        const card = document.createElement("div");

        card.className = "admin-version-card";

        card.dataset.key = String(++rowKey);

        card.dataset.initDiscount = data && data.discountPrice !== null && data.discountPrice !== undefined && data.initDiscount !== false
            ? String(data.discountPrice) : "";

        card.innerHTML = `
            <div class="admin-version-head">
                <strong class="admin-version-title">Phiên bản</strong>
                <span class="admin-version-actions">
                    <button type="button" class="admin-link-btn" data-version-action="clone-version">Nhân bản</button>
                    <button type="button" class="admin-link-btn admin-link-danger" data-version-action="remove-version">Xoá phiên bản</button>
                </span>
            </div>
            <div class="admin-version-fields">
                <label>RAM ${inputHtml("ram", data && data.ram, 'maxlength="50" placeholder="vd 8 GB"')}</label>
                <label>Bộ nhớ ${inputHtml("storage", data && data.storage, 'maxlength="50" placeholder="vd 256 GB"')}</label>
                <label>Giá * ${inputHtml("price", data && data.price !== null && data.price !== undefined ? Number(data.price) : "", 'type="number" min="1" step="1000" placeholder="Giá bán"')}</label>
                <label>Giá KM ${inputHtml("discountPrice", data && data.discountPrice !== null && data.discountPrice !== undefined ? Number(data.discountPrice) : "", 'type="number" min="0" step="1000" placeholder="Tuỳ chọn"')}</label>
            </div>
            <div class="admin-version-block">
                <p class="admin-version-label">Màu của phiên bản <span class="admin-field-hint">(giá riêng để trống = cùng giá phiên bản; không có màu thì để 1 dòng trống)</span></p>
                <div class="admin-version-colors"></div>
                <button type="button" class="btn btn-outline-dark admin-extra-add" data-version-action="add-color">+ THÊM MÀU</button>
            </div>
            <div class="admin-version-block">
                <p class="admin-version-label">Thông số riêng của phiên bản <span class="admin-field-hint">(chỉ dòng khác thông số chung, vd Pin, Camera; ghi đè dòng chung cùng tên)</span></p>
                <div class="admin-version-specs"></div>
                <button type="button" class="btn btn-outline-dark admin-extra-add" data-version-action="add-spec">+ THÊM THÔNG SỐ RIÊNG</button>
            </div>
        `;

        const colors = data && data.colors && data.colors.length ? data.colors : [{}];

        colors.forEach(function (color) {
            appendColorRow(card.querySelector(".admin-version-colors"), color);
        });

        ((data && data.specs) || []).forEach(function (spec) {
            appendSpecRow(card.querySelector(".admin-version-specs"), spec);
        });

        variantRows.appendChild(card);

        renumberCards();

        return card;

    }


    function appendColorRow(container, color) {

        const row = document.createElement("div");

        row.className = "admin-extra-row admin-color-row";

        row.dataset.id = color && color.id ? String(color.id) : "";

        row.innerHTML = `
            ${inputHtml("color", color && color.color, 'maxlength="50" placeholder="Màu, vd Đen" aria-label="Tên màu"')}
            ${inputHtml("skuVariant", color && color.skuVariant, 'maxlength="50" placeholder="SKU (tuỳ chọn)" aria-label="SKU"')}
            ${inputHtml("price", color && color.price !== null && color.price !== undefined ? Number(color.price) : "", 'type="number" min="1" step="1000" placeholder="Giá riêng (tuỳ chọn)" aria-label="Giá riêng của màu"')}
            <button type="button" class="admin-extra-remove" data-version-action="remove-color" aria-label="Xoá màu">✕</button>
        `;

        container.appendChild(row);

        return row;

    }


    /* spec = { name, value } (thẻ phiên bản) hoặc ProductSpecificationResponse (thông số chung) */

    function appendSpecRow(container, spec, name) {

        const row = document.createElement("div");

        row.className = "admin-extra-row admin-spec-row";

        row.dataset.id = spec && spec.id ? String(spec.id) : "";

        const specName = spec ? (spec.specName !== undefined ? spec.specName : spec.name) : name;

        const specValue = spec ? (spec.specValue !== undefined ? spec.specValue : spec.value) : "";

        row.innerHTML = `
            <span class="admin-spec-move">
                <button type="button" data-row-action="up" aria-label="Đưa lên">↑</button>
                <button type="button" data-row-action="down" aria-label="Đưa xuống">↓</button>
            </span>
            <div class="admin-spec-cell">
                ${inputHtml("specName", specName, 'maxlength="100" list="pfSpecNameList" placeholder="Tên thông số" aria-label="Tên thông số"')}
                <small class="admin-spec-tag"></small>
            </div>
            <div class="admin-spec-cell">
                ${inputHtml("specValue", specValue, 'placeholder="Giá trị (nhiều mục: ngăn bằng |)" aria-label="Giá trị thông số"')}
                <span class="admin-spec-preview"></span>
            </div>
            <button type="button" class="admin-extra-remove" data-row-action="remove" aria-label="Xoá thông số">✕</button>
        `;

        container.appendChild(row);

        refreshSpecRow(row);

        return row;

    }


    /*
     * Hiện trước cách trang khách sẽ xếp dòng này: nhóm (cùng quy tắc specGroupKey ở ui.js), cảnh báo khi tên lạ
     * (không thuộc tên đã dùng trong danh mục và rơi vào "Tiện ích khác"), và các thẻ nhỏ khi giá trị có dấu |.
     */

    function refreshSpecRow(row) {

        const name = rowValue(row, "specName");

        const tag = row.querySelector(".admin-spec-tag");

        const preview = row.querySelector(".admin-spec-preview");

        if (!name) {
            tag.textContent = "";
            tag.className = "admin-spec-tag";
        } else {

            const group = specGroupLabel(name);

            const known = knownSpecNames.indexOf(name.toLowerCase()) !== -1;

            const warn = group.key === "other" && !known;

            tag.className = "admin-spec-tag" + (warn ? " is-warn" : "");

            tag.textContent = warn
                ? "Tên mới, sẽ nằm ở Tiện ích khác"
                : "→ " + group.label + (known ? "" : " · tên mới");

        }

        const parts = String(rowValue(row, "specValue") || "").split("|").map(function (part) { return part.trim(); }).filter(Boolean);

        preview.innerHTML = parts.length > 1
            ? parts.map(function (part) { return '<span class="spec-chip">' + escapeHtml(part) + "</span>"; }).join("")
            : "";

    }


    /* Các ô "Điểm nổi bật" trang khách sẽ hiện, tính từ thông số chung + thông số riêng của phiên bản đầu */
    function refreshHighlights() {

        if (!highlightBar) {
            return;
        }

        const rows = [];

        Array.from(specRows.querySelectorAll(".admin-spec-row")).concat(
            Array.from(variantRows.querySelectorAll(".admin-version-card:first-child .admin-spec-row"))
        ).forEach(function (row) {

            const name = rowValue(row, "specName");

            const value = rowValue(row, "specValue");

            if (name && value) {
                rows.push([name, value]);
            }

        });

        const picked = pickHighlights(rows);

        const have = {};

        picked.forEach(function (item) { have[item.tile.key] = item.value; });

        highlightBar.innerHTML = "<strong>Điểm nổi bật trên trang khách: " + picked.length + "/4</strong>"
            + HIGHLIGHT_TILES.map(function (tile) {
                return '<span class="admin-hl' + (have[tile.key] ? " is-on" : "") + '">' + escapeHtml(tile.label) + ": "
                    + (have[tile.key] ? escapeHtml(have[tile.key]) : "thiếu") + "</span>";
            }).join("")
            + (picked.length < 3 ? '<em class="admin-hl-warn">Dưới 3 ô thì trang khách ẩn cả khối. Thêm thông số như Chip xử lý (CPU), Kích thước màn hình, Dung lượng pin, Độ phân giải camera sau.</em>' : "");

    }


    /* ----- tải: nhóm phiên bản đã có theo (RAM, bộ nhớ) thành thẻ ----- */

    function cardsFromVariants(variants, specs) {

        const groups = new Map();

        variants.forEach(function (variant) {

            const key = norm(variant.ram) + "|" + norm(variant.storage);

            if (!groups.has(key)) {
                groups.set(key, []);
            }

            groups.get(key).push(variant);

        });

        return Array.from(groups.values()).map(function (group) {

            const minPrice = Math.min.apply(null, group.map(function (variant) { return Number(variant.price); }));

            const discounted = group.find(function (variant) { return variant.discountPrice !== null && variant.discountPrice !== undefined; });

            const ids = new Set(group.map(function (variant) { return variant.id }));

            const seen = new Set();

            const versionSpecs = [];

            specs.filter(function (spec) { return spec.variantId && ids.has(spec.variantId); })
                .sort(function (a, b) { return (a.specOrder || 0) - (b.specOrder || 0); })
                .forEach(function (spec) {

                    const key = String(spec.specName).trim().toLowerCase();

                    if (!seen.has(key)) {
                        seen.add(key);
                        versionSpecs.push({ name: spec.specName, value: spec.specValue });
                    }

                });

            return {
                ram: group[0].ram,
                storage: group[0].storage,
                price: minPrice,
                discountPrice: discounted ? Number(discounted.discountPrice) : null,
                colors: group.map(function (variant) {
                    return {
                        id: variant.id,
                        color: variant.color,
                        skuVariant: variant.skuVariant,
                        price: Number(variant.price) !== minPrice ? Number(variant.price) : null
                    };
                }),
                specs: versionSpecs
            };

        });

    }


    /* Mở form: sửa = tải phiên bản + thông số của sản phẩm; thêm mới = 1 thẻ phiên bản trống + mẫu thông số */

    async function reset(product, categoryId) {

        const current = ++session;

        originalVariants = new Map();

        originalSpecs = new Map();

        variantRows.innerHTML = "";

        specRows.innerHTML = "";

        loadNeeds(categoryId, product ? product.id : null, current);

        if (!product) {

            appendVersionCard(null);

            notifyColors();

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
            });

            results[1].forEach(function (spec) {

                originalSpecs.set(String(spec.id), spec);

                if (!spec.variantId) {
                    appendSpecRow(specRows, spec);
                }

            });

            cardsFromVariants(results[0], results[1]).forEach(appendVersionCard);

            if (results[0].length === 0) {
                appendVersionCard(null);
            }

            notifyColors();

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
            names = await apiRequest("/categories/" + encodeURIComponent(categoryId) + "/spec-names?limit=60");
        } catch (error) {
            return;
        }

        if (current !== session) {
            return;
        }

        const existing = new Set(Array.from(specRows.querySelectorAll('[name="specName"]')).map(function (input) {
            return input.value.trim().toLowerCase();
        }));

        /* Mọi tên đã dùng làm gợi ý + mốc "tên quen"; chỉ TEMPLATE_ROWS tên hay gặp nhất được thêm sẵn thành dòng trống */
        knownSpecNames = names.map(function (name) { return String(name).toLowerCase(); });

        document.getElementById("pfSpecNameList").innerHTML = names.map(function (name) {
            return '<option value="' + escapeHtml(name) + '"></option>';
        }).join("");

        names.slice(0, TEMPLATE_ROWS).filter(function (name) {
            return !existing.has(String(name).toLowerCase());
        }).forEach(function (name) {
            appendSpecRow(specRows, null, name);
        });

        document.querySelectorAll("#pfSpecs .admin-spec-row, #pfVariantRows .admin-spec-row").forEach(refreshSpecRow);

        refreshHighlights();

    }


    function changeCategory(categoryId) {

        /* Đổi danh mục: bỏ các dòng mẫu chưa điền, thêm mẫu của danh mục mới */
        specRows.querySelectorAll(".admin-spec-row").forEach(function (row) {
            if (!row.dataset.id && !rowValue(row, "specValue")) {
                row.remove();
            }
        });

        applyTemplate(categoryId, session);

        /* nhu cầu thuộc danh mục: đổi danh mục thì bỏ các nhu cầu cũ */
        originalNeedIds = [];

        loadNeeds(categoryId, null, session);

    }


    /* Đọc một thẻ phiên bản; forClone = true: bản sao không mang id, SKU */

    function readCard(card, forClone) {

        const colors = Array.from(card.querySelectorAll(".admin-color-row")).map(function (row) {

            return {
                id: forClone ? null : (row.dataset.id || null),
                color: rowValue(row, "color"),
                skuVariant: forClone ? null : rowValue(row, "skuVariant"),
                price: rowValue(row, "price") === null ? null : Number(rowValue(row, "price"))
            };

        });

        const specs = Array.from(card.querySelectorAll(".admin-version-specs .admin-spec-row")).map(function (row) {
            return { name: rowValue(row, "specName"), value: rowValue(row, "specValue") };
        });

        return {
            ram: rowValue(card, "ram"),
            storage: rowValue(card, "storage"),
            price: rowValue(card, "price") === null ? null : Number(rowValue(card, "price")),
            discountPrice: rowValue(card, "discountPrice") === null ? null : Number(rowValue(card, "discountPrice")),
            initDiscount: !forClone,
            colors: colors,
            specs: specs
        };

    }


    /* Tên phiên bản tự sinh, cùng kiểu catalogue: '8GB/256GB - Đen', '256 GB - Đỏ', 'Đen', 'Tiêu chuẩn' */

    function variantNameOf(ram, storage, color) {

        const size = ram && storage ? ram.replace(/\s+/g, "") + "/" + storage.replace(/\s+/g, "") : (storage || ram || "");

        if (size && color) {
            return size + " - " + color;
        }

        return size || color || "Tiêu chuẩn";

    }


    /* { variants, specs, versionCards } đã kiểm tra, hoặc { error } */

    function read() {

        if (variantRows.querySelector(".form-error")) {
            return { error: "Chưa tải được phiên bản của sản phẩm, vui lòng đóng form và mở lại." };
        }

        const cards = Array.from(variantRows.querySelectorAll(".admin-version-card"));

        const versionCards = [];

        const variants = [];

        const versionKeys = new Set();

        for (let index = 0; index < cards.length; index++) {

            const card = cards[index];

            const label = "Phiên bản " + (index + 1);

            const data = readCard(card, false);

            const initDiscount = card.dataset.initDiscount;

            if (!(data.price > 0)) {
                return { error: label + ": giá phải lớn hơn 0." };
            }

            const versionKey = norm(data.ram) + "|" + norm(data.storage);

            if (versionKeys.has(versionKey)) {
                return { error: label + ": trùng RAM / bộ nhớ với một phiên bản khác." };
            }

            versionKeys.add(versionKey);

            if (data.colors.length > 1 && data.colors.some(function (color) { return !color.color; })) {
                return { error: label + ": mỗi màu cần có tên (hoặc xoá dòng màu trống)." };
            }

            const colorNames = data.colors.map(function (color) { return (color.color || "").toLowerCase(); });

            if (new Set(colorNames).size !== colorNames.length) {
                return { error: label + ": tên màu bị trùng nhau." };
            }

            const cardVariants = [];

            for (const color of data.colors) {

                const price = color.price !== null ? color.price : data.price;

                if (!(price > 0)) {
                    return { error: label + ": giá của màu phải lớn hơn 0." };
                }

                if (data.discountPrice !== null && !(data.discountPrice >= 0 && data.discountPrice <= price)) {
                    return { error: label + ": giá khuyến mãi phải từ 0 tới giá." };
                }

                const variant = {
                    id: color.id,
                    variantName: variantNameOf(data.ram, data.storage, color.color),
                    ram: data.ram,
                    storage: data.storage,
                    color: color.color,
                    price: price,
                    /* giá KM không đổi so với lúc mở form → phiên bản đã có giữ giá KM riêng của nó */
                    discountPrice: data.discountPrice,
                    keepOwnDiscount: color.id !== null && String(data.discountPrice === null ? "" : data.discountPrice) === initDiscount,
                    skuVariant: color.skuVariant
                };

                variants.push(variant);

                cardVariants.push(variant);

            }

            const specNames = new Set();

            for (const spec of data.specs) {

                if (spec.value && !spec.name) {
                    return { error: label + ": thông số riêng có giá trị \"" + spec.value + "\" chưa có tên." };
                }

                if (spec.name && specNames.has(spec.name.toLowerCase())) {
                    return { error: label + ": thông số riêng \"" + spec.name + "\" bị trùng tên." };
                }

                if (spec.name) {
                    specNames.add(spec.name.toLowerCase());
                }

            }

            versionCards.push({ variants: cardVariants, specs: data.specs.filter(function (spec) { return spec.name && spec.value; }) });

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

        return { variants: variants, specs: specs, versionCards: versionCards, needIds: needsLoaded ? readNeedIds() : null };

    }


    /* Ghi thay đổi sau khi sản phẩm đã lưu; trả danh sách lỗi (rỗng = xong hết) */

    async function save(productId, data) {

        const problems = [];

        const run = async function (label, action) {

            try {
                return await action();
            } catch (error) {
                problems.push(label + ": " + getErrorMessage(error));
                return null;
            }

        };


        /* ----- phiên bản (mỗi màu của mỗi thẻ) ----- */

        const keptVariants = new Set();

        for (const variant of data.variants) {

            const old = variant.id ? originalVariants.get(variant.id) : null;

            const body = {
                variantName: variant.variantName,
                skuVariant: variant.skuVariant,
                price: variant.price,
                discountPrice: variant.keepOwnDiscount && old
                    ? (old.discountPrice === null || old.discountPrice === undefined ? null : Number(old.discountPrice))
                    : variant.discountPrice,
                color: variant.color,
                storage: variant.storage,
                ram: variant.ram
            };

            if (old) {

                keptVariants.add(variant.id);

                const changed = ["variantName", "skuVariant", "color", "storage", "ram"].some(function (key) {
                    return (old[key] || null) !== (body[key] || null);
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

                const created = await run(variant.variantName, function () {
                    return apiRequest("/product-variants", { method: "POST", body: body, auth: true });
                });

                variant.id = created ? String(created.id) : null;

            }

        }

        for (const id of originalVariants.keys()) {

            if (!keptVariants.has(id)) {
                await run("Xoá " + originalVariants.get(id).variantName, function () {
                    return apiRequest("/product-variants/" + id, { method: "DELETE", auth: true });
                });
            }

        }


        /* ----- thông số chung (variantId trống) ----- */

        const commonOld = Array.from(originalSpecs.values()).filter(function (spec) { return !spec.variantId; });

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

        for (const old of commonOld) {

            if (!keptSpecs.has(String(old.id))) {
                await run("Xoá thông số " + old.specName, function () {
                    return apiRequest("/product-specifications/" + old.id, { method: "DELETE", auth: true });
                });
            }

        }


        /* ----- thông số riêng của thẻ phiên bản: ghi cho từng màu (variantId) của thẻ ----- */

        for (const card of data.versionCards) {

            for (const variant of card.variants) {

                if (!variant.id) {
                    continue;
                }

                const existing = Array.from(originalSpecs.values()).filter(function (spec) {
                    return String(spec.variantId) === String(variant.id);
                });

                const wanted = new Set();

                let specOrder = 0;

                for (const spec of card.specs) {

                    const key = spec.name.toLowerCase();

                    wanted.add(key);

                    const old = existing.find(function (item) { return String(item.specName).toLowerCase() === key; });

                    if (old) {

                        if (old.specName !== spec.name || old.specValue !== spec.value || old.specOrder !== specOrder) {
                            await run(spec.name, function () {
                                return apiRequest("/product-specifications/" + old.id, {
                                    method: "PUT", auth: true,
                                    body: { specName: spec.name, specValue: spec.value, specOrder: specOrder }
                                });
                            });
                        }

                    } else {

                        await run(spec.name, function () {
                            return apiRequest("/product-specifications", {
                                method: "POST", auth: true,
                                body: { productId: productId, variantId: Number(variant.id), specName: spec.name, specValue: spec.value, specOrder: specOrder }
                            });
                        });

                    }

                    specOrder++;

                }

                for (const old of existing) {

                    if (!wanted.has(String(old.specName).toLowerCase())) {
                        await run("Xoá thông số " + old.specName, function () {
                            return apiRequest("/product-specifications/" + old.id, { method: "DELETE", auth: true });
                        });
                    }

                }

            }

        }

        /* ----- nhu cầu ----- */

        if (data.needIds !== null && data.needIds !== undefined) {

            const same = data.needIds.length === originalNeedIds.length
                && data.needIds.every(function (id) { return originalNeedIds.indexOf(id) !== -1; });

            if (!same) {
                await run("Nhu cầu", function () {
                    return apiRequest("/products/" + productId + "/needs", { method: "PUT", body: { needIds: data.needIds }, auth: true });
                });
            }

        }

        return problems;

    }


    function clear() {

        session++;

        variantRows.innerHTML = "";

        specRows.innerHTML = "";

        needList.innerHTML = "";

        needBox.hidden = true;

        needsLoaded = false;

        notifyColors();

    }


    return {
        reset: reset, changeCategory: changeCategory, read: read, save: save, clear: clear,
        getColors: getColors, onColorsChange: onColorsChange
    };

}
