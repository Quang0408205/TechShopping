document.addEventListener(
    "DOMContentLoaded",
    function () {

        renderHomeProductGrids();


        const phoneImage =
            document.querySelector(".phone-image");

        const phoneContent =
            document.querySelector(".phone-content");


        if (!phoneImage) {
            return;
        }


        const observer =
            new IntersectionObserver(

                function (entries) {

                    entries.forEach(function (entry) {

                        if (entry.isIntersecting) {

                            phoneImage.classList.add("show");

                            if (phoneContent) {
                                phoneContent.classList.add("show");
                            }

                        }

                    });

                },

                {
                    threshold: 0.25
                }

            );


        observer.observe(phoneImage);

    }
);

/* ================= LƯỚI SẢN PHẨM TRANG CHỦ (API thật) ================= */

/*
 * Hai lưới 4 sản phẩm từ GET /products (công khai, không gửi token), vẽ bằng
 * renderProductGrid() dùng chung với trang Sản phẩm (js/core/ui.js):
 *   - "Sản phẩm nổi bật": 4 sản phẩm đầu tiên (chưa có dữ liệu bán chạy);
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


    grid.innerHTML = skeletonProductGrid(4);


    try {

        const page = await apiRequest(
            "/products?isActive=true&page=0&size=4&sort=" + encodeURIComponent(config.sort)
        );

        const categories = await getHomeCategories();

        renderProductGrid(grid, page.content, categories.byId);

    } catch (error) {

        grid.innerHTML = errorStateHtml(getErrorMessage(error), config.retryId);

        document.getElementById(config.retryId)
            .addEventListener("click", function () {
                renderHomeProductGrid(config);
            });

    }

}
