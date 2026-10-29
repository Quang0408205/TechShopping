/* ================= TRANG SO SÁNH SẢN PHẨM ================= */

/*
 * customer/compare.html?ids=1,2,3 (2–4 sản phẩm). Mỗi sản phẩm: GET /products/{id} + GET /products/{id}/specifications.
 * Bảng: ảnh + tên + nút bỏ, các dòng cơ bản (giá, thương hiệu, đánh giá, bảo hành, tình trạng), rồi các thông số kỹ thuật
 * (gộp theo tên thông số). Dòng có giá trị khác nhau được tô nhạt; ô "Chỉ hiện điểm khác nhau" ẩn các dòng giống nhau.
 * Giá rẻ nhất được đánh dấu. Mọi dữ liệu từ API đều escape khi đưa vào HTML.
 */

document.addEventListener("DOMContentLoaded", async function () {

    if (typeof layoutReady !== "undefined") {
        await layoutReady;
    }

    const loading = document.getElementById("compareLoading");

    const empty = document.getElementById("compareEmpty");

    const box = document.getElementById("compareBox");

    const table = document.getElementById("compareTable");

    const onlyDiff = document.getElementById("compareOnlyDiff");

    let items = [];


    function show(which) {

        loading.hidden = which !== "loading";
        empty.hidden = which !== "empty";
        box.hidden = which !== "box";

    }


    function idsFromUrl() {

        const raw = new URLSearchParams(location.search).get("ids") || "";

        const seen = [];

        raw.split(",").forEach(function (part) {

            if (/^\d{1,18}$/.test(part.trim()) && seen.indexOf(part.trim()) === -1 && seen.length < COMPARE_MAX) {
                seen.push(part.trim());
            }

        });

        return seen;

    }


    async function loadOne(id) {

        try {

            const results = await Promise.all([
                apiRequest("/products/" + id),
                apiRequest("/products/" + id + "/specifications").catch(function () { return []; })
            ]);

            const product = results[0];

            if (!product || product.isActive === false) {
                return null;
            }

            const specs = {};

            (results[1] || []).forEach(function (spec) {

                /* thông số riêng của một phiên bản không so được giữa các sản phẩm */
                if (spec.variantId == null && spec.specName && spec.specValue && !(spec.specName in specs)) {
                    specs[spec.specName] = String(spec.specValue).trim();
                }

            });

            return { product: product, specs: specs, order: (results[1] || []).map(function (spec) { return spec.specName; }) };

        } catch (error) {

            return null;

        }

    }


    function price(item) {

        const value = Number(item.product.effectivePrice != null ? item.product.effectivePrice : item.product.basePrice);

        return value > 0 ? value : null;

    }


    function cell(html, extraClass) {

        return `<td${extraClass ? ` class="${extraClass}"` : ""}>${html}</td>`;

    }


    /* rows: [{ label, values: [html...], plain: [text...] (để so khác nhau), highlight?: index }] */

    function buildRows() {

        const rows = [];

        const prices = items.map(price);

        const lowest = Math.min.apply(null, prices.filter(function (value) { return value !== null; }));

        rows.push({
            label: "Giá bán",
            plain: prices.map(function (value) { return value === null ? "—" : String(value); }),
            values: prices.map(function (value) {

                if (value === null) {
                    return "Liên hệ";
                }

                return `<strong class="compare-price">${escapeHtml(formatPrice(value))}</strong>`
                    + (value === lowest && prices.filter(function (other) { return other === lowest; }).length < items.length
                        ? `<span class="compare-best">Rẻ nhất</span>` : "");

            })
        });

        const simple = function (label, pick) {

            const texts = items.map(function (item) {
                const value = pick(item.product);
                return value === null || value === undefined || String(value).trim() === "" ? "—" : String(value).trim();
            });

            rows.push({ label: label, plain: texts, values: texts.map(escapeHtml) });

        };

        simple("Thương hiệu", function (product) { return product.brandName; });

        simple("Đánh giá", function (product) {
            return product.totalReviews > 0 && Number(product.rating) > 0
                ? Number(product.rating).toFixed(1) + " ★ (" + product.totalReviews + " đánh giá)"
                : "Chưa có đánh giá";
        });

        simple("Bảo hành", function (product) { return product.warrantyMonths ? product.warrantyMonths + " tháng" : null; });

        simple("Tình trạng", function (product) { return product.availableQuantity > 0 ? "Còn hàng" : "Hết hàng"; });


        /* thông số kỹ thuật: tên theo thứ tự xuất hiện đầu tiên */
        const names = [];

        items.forEach(function (item) {

            item.order.forEach(function (name) {
                if (name in item.specs && names.indexOf(name) === -1) {
                    names.push(name);
                }
            });

        });

        names.forEach(function (name) {

            const texts = items.map(function (item) { return item.specs[name] || "—"; });

            rows.push({ label: name, plain: texts, values: texts.map(escapeHtml), spec: true });

        });

        rows.forEach(function (row) {

            const normal = row.plain.map(function (text) { return text.toLowerCase(); });

            row.differs = normal.some(function (text) { return text !== normal[0]; });

        });

        return rows;

    }


    function render() {

        const rows = buildRows();

        const head = items.map(function (item) {

            const product = item.product;

            const image = isSafeImageUrl(product.primaryImageUrl) ? product.primaryImageUrl.trim() : "";

            return `<th scope="col" class="compare-head">
                <button type="button" class="compare-remove" data-remove="${escapeHtml(product.id)}" aria-label="${escapeHtml("Bỏ " + product.name)}">×</button>
                <a href="${escapeHtml(getProductDetailUrl(product.id))}" class="compare-thumb">
                    ${image ? `<img src="${escapeHtml(image)}" alt="${escapeHtml(product.name)}" referrerpolicy="no-referrer">` : ""}
                </a>
                <a href="${escapeHtml(getProductDetailUrl(product.id))}" class="compare-name">${escapeHtml(product.name)}</a>
            </th>`;

        }).join("");

        const body = rows.map(function (row) {

            const hidden = onlyDiff.checked && !row.differs;

            return `<tr class="${row.differs ? "is-diff" : ""}${row.spec ? " is-spec" : ""}"${hidden ? " hidden" : ""}>
                <th scope="row">${escapeHtml(row.label)}</th>
                ${row.values.map(function (html) { return cell(html); }).join("")}
            </tr>`;

        }).join("");

        table.style.setProperty("--compare-cols", String(items.length));

        table.innerHTML = `<thead><tr><td class="compare-corner"></td>${head}</tr></thead><tbody>${body}</tbody>`;

    }


    function syncStore() {

        saveCompareList(items.map(function (item) {

            return {
                id: String(item.product.id),
                name: item.product.name,
                image: isSafeImageUrl(item.product.primaryImageUrl) ? item.product.primaryImageUrl.trim() : "",
                categoryId: String(item.product.categoryId)
            };

        }));

        syncCompareUi();

    }


    function updateUrl() {

        history.replaceState(null, "", location.pathname + "?ids=" + items.map(function (item) { return item.product.id; }).join(","));

    }


    table.addEventListener("click", function (event) {

        const remove = event.target.closest("[data-remove]");

        if (!remove) {
            return;
        }

        items = items.filter(function (item) { return String(item.product.id) !== remove.dataset.remove; });

        updateUrl();

        syncStore();

        if (items.length < 2) {
            document.getElementById("compareEmptyText").textContent = "Cần ít nhất 2 sản phẩm để so sánh. Hãy thêm sản phẩm từ danh sách.";
            show("empty");
            return;
        }

        render();

    });

    onlyDiff.addEventListener("change", render);


    const ids = idsFromUrl();

    if (ids.length < 2) {
        show(ids.length === 0 ? "empty" : "empty");
        return;
    }

    show("loading");

    items = (await Promise.all(ids.map(loadOne))).filter(Boolean);

    if (items.length < 2) {
        document.getElementById("compareEmptyText").textContent = "Một số sản phẩm không còn được bán nên không so sánh được.";
        show("empty");
        return;
    }

    syncStore();

    render();

    show("box");

});
