document.addEventListener("DOMContentLoaded", function () {

    renderHomeCategories();

    renderHomeProductGrids();

    setupHomeTabs();

});

/* ================= LƯỚI SẢN PHẨM TRANG CHỦ (API thật) ================= */

/*
 * Hai lưới 4 sản phẩm từ GET /products (công khai, không gửi token), vẽ bằng
 * renderProductGrid() dùng chung với trang Sản phẩm (js/core/ui.js):
 *   - "Sản phẩm nổi bật": tab Nổi bật / Mới về / Giá tốt = sort id,asc / id,desc
 *     / basePrice,asc (cùng giá trị với ô sắp xếp ở trang Sản phẩm);
 *   - "Gợi ý dành cho bạn": 4 sản phẩm mới nhất (gợi ý cá nhân hoá thật ở Phase 8).
 * Danh mục chỉ dùng để chọn ảnh thay thế, nên lỗi tải danh mục không chặn
 * việc hiển thị sản phẩm. Hai lưới dùng chung một lần tải danh mục.
 */

const HOME_PRODUCT_GRIDS = [
    { gridId: "featuredProductGrid", sort: "id,asc", retryId: "featuredRetryBtn" },
    { gridId: "personalRecommendationGrid", sort: "id,desc", retryId: "newestRetryBtn" }
];

let homeCategoriesPromise = null;


/* ================= KHỐI "MUA SẮM THEO DANH MỤC" (kế hoạch v3) ================= */

/*
 * Một thẻ bằng nhau cho mỗi danh mục ĐANG HIỂN THỊ (theo thứ tự ADMIN đặt), xếp một hàng cuộn ngang
 * (4 thẻ / khung trên máy tính) với nút ← →. Ảnh: ảnh ADMIN tải lên → ảnh cắt sẵn của danh mục gốc →
 * ảnh sản phẩm đầu tiên của danh mục. Lỗi tải danh mục → giữ 4 thẻ viết sẵn trong index.html.
 */

const HOME_CATEGORY_NOTES = {
    phone: "iPhone, Samsung Galaxy, Xiaomi, OPPO",
    laptop: "MacBook, Asus, Dell, Lenovo, HP"
};

async function renderHomeCategories() {

    const grid = document.querySelector(".category-grid");

    if (!grid) {
        return;
    }

    setupCategoryCarousel(grid);

    let categories;

    try {

        categories = (await loadShopCategories()).visible;

    } catch (error) {

        return;

    }

    if (categories.length === 0) {
        return;
    }

    grid.innerHTML = categories.map(function (category) {

        const note = HOME_CATEGORY_NOTES[getCategoryFilterKey(category.slug)];

        return `
            <a href="${escapeHtml(categoryUrlOf(category))}" class="category-card" data-category-id="${escapeHtml(String(category.id))}">
                <div class="category-text">
                    <h3>${escapeHtml(category.name)}</h3>
                    ${note ? `<p>${escapeHtml(note)}</p>` : ""}
                    <span class="category-more">Khám phá</span>
                </div>
                <img src="${escapeHtml(categoryImageOf(category))}" alt="" loading="lazy">
            </a>
        `;

    }).join("");

    grid.scrollLeft = 0;

    grid.dispatchEvent(new Event("scroll"));

    categories.filter(function (category) {
        return !hasOwnCategoryImage(category);
    }).forEach(useFirstProductImage);

}


/* Ảnh ADMIN tải lên hoặc ảnh cắt sẵn của 4 danh mục gốc */

function hasOwnCategoryImage(category) {

    return isSafeImageUrl(category.iconUrl) || Boolean(CATEGORY_CARD_IMAGES[getCategoryFilterKey(category.slug)]);

}


/* Danh mục chưa có ảnh: dùng ảnh chính của sản phẩm đầu tiên đang bán trong danh mục (lỗi thì giữ ảnh thay thế) */

async function useFirstProductImage(category) {

    try {

        const page = await apiRequest("/products?" + new URLSearchParams({
            isActive: "true", categoryId: String(category.id), size: "1", sort: "id,asc"
        }).toString());

        const product = (page.content || [])[0];

        const img = document.querySelector('.category-card[data-category-id="' + category.id + '"] img');

        if (img && product && isSafeImageUrl(product.primaryImageUrl)) {
            img.src = product.primaryImageUrl.trim();
        }

    } catch (error) {
        /* giữ ảnh thay thế */
    }

}


/* Nút ← → cuộn đúng một khung; tự khoá ở đầu / cuối, ẩn khi mọi thẻ đã vừa khung */

function setupCategoryCarousel(grid) {

    const prev = document.getElementById("categoryPrev");

    const next = document.getElementById("categoryNext");

    if (!prev || !next) {
        return;
    }

    const update = function () {

        const max = grid.scrollWidth - grid.clientWidth;

        prev.parentElement.hidden = max <= 1;

        prev.disabled = grid.scrollLeft <= 1;

        next.disabled = grid.scrollLeft >= max - 1;

    };

    const step = function (direction) {
        grid.scrollBy({ left: direction * grid.clientWidth, behavior: "smooth" });
    };

    prev.addEventListener("click", function () { step(-1); });

    next.addEventListener("click", function () { step(1); });

    grid.addEventListener("scroll", update, { passive: true });

    window.addEventListener("resize", update);

    update();

}


function getHomeCategories() {

    if (!homeCategoriesPromise) {

        homeCategoriesPromise = loadShopCategories().catch(function () {

            homeCategoriesPromise = null;

            return { byId: {}, bySlug: {} };

        });

    }

    return homeCategoriesPromise;

}


function renderHomeProductGrids() {

    HOME_PRODUCT_GRIDS.forEach(renderHomeProductGrid);

}


async function renderHomeProductGrid(config) {

    const grid = document.getElementById(config.gridId);

    if (!grid) {
        return;
    }


    const request = (config.request || 0) + 1;

    config.request = request;

    grid.innerHTML = skeletonProductGrid(4);


    try {

        const page = await apiRequest(
            "/products?isActive=true&page=0&size=4&sort=" + encodeURIComponent(config.sort)
        );

        const categories = await getHomeCategories();

        /* Đã bấm tab khác trong lúc chờ: bỏ kết quả cũ */
        if (config.request !== request) {
            return;
        }

        renderProductGrid(grid, page.content, categories.byId);

    } catch (error) {

        if (config.request !== request) {
            return;
        }

        grid.innerHTML = errorStateHtml(getErrorMessage(error), config.retryId);

        document.getElementById(config.retryId)
            .addEventListener("click", function () {
                renderHomeProductGrid(config);
            });

    }

}


/* ================= TAB LƯỚI NỔI BẬT ================= */

function setupHomeTabs() {

    const tabs = Array.from(document.querySelectorAll("[data-home-sort]"));

    const config = HOME_PRODUCT_GRIDS[0];

    tabs.forEach(function (tab, index) {

        tab.tabIndex = tab.getAttribute("aria-selected") === "true" ? 0 : -1;

        tab.addEventListener("click", function () {

            if (tab.getAttribute("aria-selected") === "true") {
                return;
            }

            tabs.forEach(function (other) {
                other.setAttribute("aria-selected", String(other === tab));
                other.tabIndex = other === tab ? 0 : -1;
            });

            config.sort = tab.dataset.homeSort;

            renderHomeProductGrid(config);

        });

        tab.addEventListener("keydown", function (event) {

            const step = event.key === "ArrowRight" ? 1 : event.key === "ArrowLeft" ? -1 : 0;

            if (step) {
                const next = tabs[(index + step + tabs.length) % tabs.length];
                next.focus();
                next.click();
            }

        });

    });

}
