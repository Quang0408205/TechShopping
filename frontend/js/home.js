document.addEventListener("DOMContentLoaded", function () {

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


function getHomeCategories() {

    if (!homeCategoriesPromise) {

        homeCategoriesPromise = fetchCategoryMaps().catch(function () {

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
