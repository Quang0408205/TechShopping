/* ================= TRANG CHI TIẾT: ĐIỂM NỔI BẬT, CÒN HÀNG TẠI CỬA HÀNG, SO SÁNH VỚI… (kế hoạch v8 bước 2) ================= */

/*
 * Dữ liệu thật cả: điểm nổi bật rút từ bảng thông số, tồn kho từ GET /products/{id}/store-stock,
 * gợi ý so sánh từ GET /products (cùng danh mục, giá trong khoảng ±35%). Thiếu dữ liệu thì khối tự ẩn.
 * product-detail.js gọi: renderProductHighlights(rows), loadStoreStock(productId), renderStoreStock(variantId),
 * loadCompareSuggestions(product, price).
 */

/* rows = [[tên, giá trị], ...] như renderSpecs; chọn tối đa 4 ô có dữ liệu, ít hơn 3 ô thì ẩn cả khối */
function renderProductHighlights(rows) {

    const box = document.getElementById("detailHighlights");

    if (!box) {
        return;
    }

    const tiles = pickHighlights(rows);

    if (tiles.length < 3) {
        box.hidden = true;
        return;
    }

    document.getElementById("detailHighlightGrid").innerHTML = tiles.map(function (item) {
        return `<li class="highlight-tile">
            <span class="highlight-icon"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${item.tile.icon}</svg></span>
            <span class="highlight-label">${escapeHtml(item.tile.label)}</span>
            <strong class="highlight-value">${escapeHtml(item.value)}</strong>
        </li>`;
    }).join("");

    box.hidden = false;

}


/* ================= CÒN HÀNG TẠI CỬA HÀNG ================= */

let storeStockRows = null;

const STORE_STOCK_PREVIEW = 3;

const STORE_STOCK_SEARCH_FROM = 8;

let storeStockExpanded = false;

let storeStockQuery = "";

let storeStockVariantId = null;


async function loadStoreStock(productId) {

    try {
        storeStockRows = await apiRequest("/products/" + encodeURIComponent(productId) + "/store-stock") || [];
    } catch (error) {
        storeStockRows = null;
    }

}


function renderStoreStock(variantId) {

    storeStockVariantId = variantId;

    drawStoreStock();

}


/* Mặc định chỉ vài dòng đầu; nhiều cửa hàng thì "Xem thêm" mở danh sách cuộn, từ 8 cửa hàng có ô tìm */
function drawStoreStock() {

    const variantId = storeStockVariantId;

    const box = document.getElementById("detailStoreStock");

    if (!box) {
        return;
    }

    if (storeStockRows === null) {
        box.hidden = true;
        return;
    }

    const all = storeStockRows.filter(function (row) {
        return variantId === null || row.variantId === variantId;
    });

    const query = foldText(storeStockQuery);

    const rows = query === "" ? all : all.filter(function (row) {
        return foldText([row.storeName, row.address, row.district, row.city].join(" ")).indexOf(query) !== -1;
    });

    const list = document.getElementById("storeStockList");

    const search = document.getElementById("storeStockSearch");

    const more = document.getElementById("storeStockMore");

    const collapsible = all.length > STORE_STOCK_PREVIEW;

    const shown = !collapsible || storeStockExpanded ? rows : rows.slice(0, STORE_STOCK_PREVIEW);

    search.hidden = !(storeStockExpanded && all.length >= STORE_STOCK_SEARCH_FROM);

    more.hidden = !collapsible;

    more.textContent = storeStockExpanded ? "Thu gọn" : "Xem thêm " + (all.length - STORE_STOCK_PREVIEW) + " cửa hàng";

    more.setAttribute("aria-expanded", String(storeStockExpanded));

    list.classList.toggle("is-scroll", storeStockExpanded);

    document.getElementById("storeStockSummary").textContent = all.length === 0
        ? "Hiện chưa cửa hàng nào còn phiên bản này"
        : "Còn tại " + all.length + " cửa hàng";

    list.innerHTML = shown.length === 0 && all.length > 0 ? `<li class="store-stock-empty">Không có cửa hàng nào khớp "${escapeHtml(storeStockQuery)}"</li>` : shown.map(function (row) {

        const place = [row.district, row.city].filter(Boolean).join(", ");

        const level = row.quantity <= 3 ? `<span class="store-stock-qty is-low">Sắp hết (${row.quantity})</span>`
            : `<span class="store-stock-qty">Còn hàng</span>`;

        return `<li>
            <div><strong>${escapeHtml(row.storeName)}</strong><span>${escapeHtml([row.address, place].filter(Boolean).join(" · "))}</span></div>
            ${level}
        </li>`;

    }).join("");

    box.hidden = false;

}


function foldText(text) {

    return String(text || "").toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/đ/g, "d").trim();

}


document.addEventListener("click", function (event) {

    if (event.target.id === "storeStockMore") {

        storeStockExpanded = !storeStockExpanded;

        if (!storeStockExpanded) {
            storeStockQuery = "";
            document.getElementById("storeStockSearch").value = "";
        }

        drawStoreStock();

    }

});

document.addEventListener("input", function (event) {

    if (event.target.id === "storeStockSearch") {
        storeStockQuery = event.target.value;
        drawStoreStock();
    }

});


/* ================= SO SÁNH VỚI… ================= */

async function loadCompareSuggestions(product, price) {

    const zone = document.getElementById("compareZone");

    const grid = document.getElementById("compareSuggestGrid");

    if (!zone || !(price > 0)) {
        return;
    }

    let page;

    try {

        const params = new URLSearchParams({
            categoryId: String(product.categoryId),
            minPrice: String(Math.round(price * 0.65)),
            maxPrice: String(Math.round(price * 1.35)),
            size: "12",
            sort: "viewCount,desc"
        });

        page = await apiRequest("/products?" + params.toString());

    } catch (error) {
        return;
    }

    const picks = ((page && page.content) || []).filter(function (item) {
        return item.id !== product.id && isSafeImageUrl(item.primaryImageUrl) && getDisplayPrice(item) > 0
            && !isSoldOut(item);
    }).slice(0, 3);

    if (picks.length === 0) {
        return;
    }

    grid.innerHTML = picks.map(function (item) {

        const other = getDisplayPrice(item);

        const diff = other - price;

        const tag = Math.abs(diff) < 100000 ? "Giá tương đương"
            : diff < 0 ? "Rẻ hơn " + formatPrice(-diff) : "Đắt hơn " + formatPrice(diff);

        return `<article class="compare-pick">
            <a class="compare-pick-image" href="${escapeHtml(getProductDetailUrl(item.id))}"><img src="${escapeHtml(item.primaryImageUrl.trim())}" alt="" loading="lazy" referrerpolicy="no-referrer"></a>
            <div class="compare-pick-body">
                <a class="compare-pick-name" href="${escapeHtml(getProductDetailUrl(item.id))}">${escapeHtml(item.name)}</a>
                <strong>${escapeHtml(formatPrice(other))}</strong>
                <span class="compare-pick-diff${diff < 0 ? " is-cheaper" : ""}">${escapeHtml(tag)}</span>
                <a class="btn btn-outline-dark compare-pick-btn" href="compare.html?ids=${product.id},${item.id}">So sánh chi tiết</a>
            </div>
        </article>`;

    }).join("");

    zone.hidden = false;

}
