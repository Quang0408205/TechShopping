document.addEventListener(
    "DOMContentLoaded",
    function () {

        renderFeaturedProducts();


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

/* ================= SẢN PHẨM NỔI BẬT (Checkpoint 2.8) ================= */

/*
 * 4 sản phẩm đầu tiên từ GET /products (công khai, không gửi token), vẽ
 * bằng renderProductGrid() dùng chung với trang Sản phẩm (js/core/ui.js).
 * Danh mục chỉ dùng để chọn ảnh thay thế, nên lỗi tải danh mục không chặn
 * việc hiển thị sản phẩm.
 */

async function renderFeaturedProducts() {

    const grid = document.getElementById("featuredProductGrid");

    if (!grid) {
        return;
    }


    grid.innerHTML = skeletonProductGrid(4);


    const categoriesPromise = fetchCategoryMaps().catch(function () {
        return { byId: {}, bySlug: {} };
    });


    try {

        const page = await apiRequest("/products?isActive=true&page=0&size=4&sort=id");

        const categories = await categoriesPromise;

        renderProductGrid(grid, page.content, categories.byId);

    } catch (error) {

        grid.innerHTML = errorStateHtml(getErrorMessage(error), "featuredRetryBtn");

        document.getElementById("featuredRetryBtn")
            .addEventListener("click", renderFeaturedProducts);

    }

}
