/* ================= TRANG SẢN PHẨM (Checkpoint 2.8) ================= */

/*
 * Dữ liệu thật từ backend (công khai, KHÔNG gửi token):
 *   GET /categories?size=100                 → map slug ↔ id
 *   GET /products?isActive=true&categoryId=&brandId=&keyword=&minPrice=
 *       &maxPrice=&page=&size=12&sort=       → lưới sản phẩm + phân trang
 *   GET /products/{id}/images                → ảnh từng thẻ (js/core/ui.js)
 *
 * Bộ lọc thương hiệu theo danh mục: backend không có endpoint "hãng theo
 * danh mục" (và 2.8 không sửa backend), nên gom brandId/brandName từ các
 * sản phẩm của danh mục đó (size=100, Laptop 431 sản phẩm = 5 request),
 * rồi lưu tạm trong bộ nhớ theo categoryId.
 */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        /* Giá trị radio (giữ đúng data-category cũ) → slug danh mục trong DB (js/core/ui.js) */

        const CATEGORY_SLUGS = PRODUCT_CATEGORY_FILTERS;

        const PAGE_SIZE = 12;

        const BRAND_SCAN_PAGE_SIZE = 100;

        const MAX_KEYWORD_LENGTH = 100;


        const grid = document.getElementById("productGrid");

        const pagination = document.getElementById("pagination");

        const resultCount = document.getElementById("resultCount");

        const brandOptionsList = document.getElementById("brandOptionsList");

        const brandFilterTitle = document.getElementById("brandFilterTitle");

        const sortSelect = document.getElementById("sortSelect");

        const minPriceInput = document.getElementById("minPriceInput");

        const maxPriceInput = document.getElementById("maxPriceInput");

        const priceError = document.getElementById("priceError");

        const toggleFiltersBtn = document.getElementById("toggleFiltersBtn");

        const sidebar = document.getElementById("productsSidebar");


        /* Trạng thái bộ lọc; page tính từ 0 như API */

        const state = {
            category: "",
            brandId: "",
            minPrice: null,
            maxPrice: null,
            search: "",
            sort: sortSelect.value,
            page: 0
        };


        let categoryMaps = null;

        const brandCache = {};

        /* Bộ đếm request: bỏ qua response cũ khi người dùng đổi bộ lọc liên tục */

        let productRequestId = 0;

        let brandRequestId = 0;


        readStateFromUrl();

        prefillHeaderSearch();

        bindEvents();

        start();


        /* ================= KHỞI ĐỘNG ================= */

        async function start() {

            grid.innerHTML = skeletonProductGrid(PAGE_SIZE);

            resultCount.textContent = "Đang tải sản phẩm...";


            try {

                categoryMaps = await fetchCategoryMaps();

            } catch (error) {

                resultCount.textContent = "";

                showGridError(getErrorMessage(error), start);

                return;

            }


            /* ?category= không khớp danh mục nào trong DB → coi như "Tất cả" */

            if (state.category && !getCategoryId(state.category)) {

                state.category = "";

                checkCategoryRadio("");

                syncUrl();

            }


            buildBrandFilters();

            loadProducts();

        }


        function getCategoryId(categoryKey) {

            if (!categoryMaps || !CATEGORY_SLUGS[categoryKey]) {
                return null;
            }

            return categoryMaps.bySlug[CATEGORY_SLUGS[categoryKey]] || null;

        }


        /* ================= URL: ?category= và ?search= ================= */

        function readStateFromUrl() {

            const params = new URLSearchParams(window.location.search);

            const category = params.get("category");

            const search = (params.get("search") || "").trim();


            if (category && CATEGORY_SLUGS[category]) {

                state.category = category;

                checkCategoryRadio(category);

            }


            if (search) {
                state.search = search.slice(0, MAX_KEYWORD_LENGTH);
            }

        }


        /* Giữ ?category= / ?search= trên URL khớp với bộ lọc (tải lại trang vẫn đúng) */

        function syncUrl() {

            const params = new URLSearchParams();

            if (state.category) {
                params.set("category", state.category);
            }

            if (state.search) {
                params.set("search", state.search);
            }


            const query = params.toString();

            window.history.replaceState(
                null,
                "",
                window.location.pathname + (query ? "?" + query : "")
            );

        }


        function checkCategoryRadio(value) {

            const radio = document.querySelector(
                'input[name="category"][value="' + value + '"]'
            );

            if (radio) {
                radio.checked = true;
            }

        }


        /* Ô tìm kiếm trên header hiện lại từ khóa đang tìm */

        function prefillHeaderSearch() {

            if (!state.search || typeof layoutReady === "undefined") {
                return;
            }

            layoutReady.then(function () {

                const input = document.querySelector(".header .search-form input");

                if (input) {
                    input.value = state.search;
                }

            });

        }


        /* ================= THƯƠNG HIỆU THEO DANH MỤC ================= */

        async function buildBrandFilters() {

            if (!categoryMaps) {
                return;
            }


            const requestId = ++brandRequestId;


            if (!state.category) {

                brandFilterTitle.textContent = "Thương hiệu";

                brandOptionsList.innerHTML =
                    '<p class="filter-hint">Chọn một danh mục để lọc theo thương hiệu.</p>';

                return;

            }


            brandFilterTitle.textContent =
                "Thương hiệu (" + getCategoryLabel(state.category) + ")";

            brandOptionsList.innerHTML =
                '<p class="filter-hint">Đang tải thương hiệu...</p>';


            let brands;

            try {

                brands = await loadBrandsForCategory(getCategoryId(state.category));

            } catch (error) {

                if (requestId === brandRequestId) {

                    brandOptionsList.innerHTML =
                        '<p class="filter-hint">Không tải được danh sách thương hiệu.</p>';

                }

                return;

            }


            if (requestId !== brandRequestId) {
                return;
            }


            let html = `
                <label>
                    <input type="radio" name="brand" value="" ${state.brandId ? "" : "checked"}>
                    Tất cả
                </label>
            `;

            brands.forEach(function (brand) {

                html += `
                    <label>
                        <input
                            type="radio"
                            name="brand"
                            value="${escapeHtml(brand.id)}"
                            ${String(brand.id) === state.brandId ? "checked" : ""}
                        >
                        ${escapeHtml(brand.name)}
                    </label>
                `;

            });

            brandOptionsList.innerHTML = html;


            /* Radio được tạo lại mỗi lần đổi danh mục nên phải gắn sự kiện lại */

            brandOptionsList.querySelectorAll('input[name="brand"]')
                .forEach(function (radio) {

                    radio.addEventListener("change", function () {

                        state.brandId = radio.value;

                        state.page = 0;

                        loadProducts();

                    });

                });

        }


        function getCategoryLabel(categoryKey) {

            const radio = document.querySelector(
                'input[name="category"][value="' + categoryKey + '"]'
            );

            return radio ? radio.parentElement.textContent.trim() : "";

        }


        /*
         * Hãng có sản phẩm trong danh mục, sắp xếp theo tên. Lưu Promise vào
         * cache để 2 lần chọn liên tiếp không tải trùng; lỗi thì xóa khỏi cache.
         */

        function loadBrandsForCategory(categoryId) {

            if (!brandCache[categoryId]) {

                brandCache[categoryId] = scanBrands(categoryId)
                    .catch(function (error) {

                        delete brandCache[categoryId];

                        throw error;

                    });

            }

            return brandCache[categoryId];

        }


        async function scanBrands(categoryId) {

            function pagePath(page) {

                return "/products?isActive=true&categoryId=" + categoryId +
                    "&size=" + BRAND_SCAN_PAGE_SIZE + "&sort=id&page=" + page;

            }


            const firstPage = await apiRequest(pagePath(0));

            const pages = [firstPage];


            if (firstPage.totalPages > 1) {

                const requests = [];

                for (let page = 1; page < firstPage.totalPages; page++) {
                    requests.push(apiRequest(pagePath(page)));
                }

                pages.push.apply(pages, await Promise.all(requests));

            }


            const brandsById = new Map();

            pages.forEach(function (page) {

                (page.content || []).forEach(function (product) {

                    if (product.brandId && !brandsById.has(product.brandId)) {
                        brandsById.set(product.brandId, product.brandName || "");
                    }

                });

            });


            return Array.from(brandsById, function (entry) {
                return { id: entry[0], name: entry[1] };
            }).sort(function (a, b) {
                return a.name.localeCompare(b.name, "vi");
            });

        }


        /* ================= DANH SÁCH SẢN PHẨM ================= */

        function buildProductQuery() {

            const params = new URLSearchParams();

            params.set("isActive", "true");

            params.set("page", String(state.page));

            params.set("size", String(PAGE_SIZE));

            params.set("sort", state.sort);


            const categoryId = getCategoryId(state.category);

            if (categoryId) {
                params.set("categoryId", String(categoryId));
            }

            if (categoryId && state.brandId) {
                params.set("brandId", state.brandId);
            }

            if (state.search) {
                params.set("keyword", state.search);
            }

            if (state.minPrice !== null) {
                params.set("minPrice", String(state.minPrice));
            }

            if (state.maxPrice !== null) {
                params.set("maxPrice", String(state.maxPrice));
            }


            return params.toString();

        }


        async function loadProducts() {

            /* Danh mục chưa tải xong: start() sẽ tự gọi lại với state mới nhất */

            if (!categoryMaps) {
                return;
            }


            const requestId = ++productRequestId;


            grid.innerHTML = skeletonProductGrid(PAGE_SIZE);

            resultCount.textContent = "Đang tải sản phẩm...";

            pagination.innerHTML = "";


            let page;

            try {

                page = await apiRequest("/products?" + buildProductQuery());

            } catch (error) {

                if (requestId !== productRequestId) {
                    return;
                }

                resultCount.textContent = "";

                showGridError(getErrorMessage(error), loadProducts);

                return;

            }


            if (requestId !== productRequestId) {
                return;
            }


            /* Trang hiện tại vượt quá số trang mới (hiếm) → về trang cuối */

            if (page.totalPages > 0 && state.page >= page.totalPages) {

                state.page = page.totalPages - 1;

                loadProducts();

                return;

            }


            renderResultCount(page.totalElements);


            if (!page.content || page.content.length === 0) {

                grid.innerHTML = emptyStateHtml(
                    "Không tìm thấy sản phẩm phù hợp",
                    state.search
                        ? "Không có sản phẩm nào khớp với \"" + state.search +
                        "\". Hãy thử từ khóa khác hoặc xóa bộ lọc."
                        : "Vui lòng thử lại với bộ lọc khác."
                );

                return;

            }


            renderProductGrid(grid, page.content, categoryMaps.byId);

            renderPagination(page.page, page.totalPages);

        }


        function renderResultCount(total) {

            if (!total) {

                resultCount.textContent = "";

                return;

            }


            resultCount.textContent =
                "Tìm thấy " + new Intl.NumberFormat("vi-VN").format(total) +
                " sản phẩm" +
                (state.search ? " cho \"" + state.search + "\"" : "");

        }


        function showGridError(message, retry) {

            grid.innerHTML = errorStateHtml(message, "productsRetryBtn");

            document.getElementById("productsRetryBtn")
                .addEventListener("click", retry);

        }


        /* ================= PHÂN TRANG (rút gọn: 1 … 4 5 6 … 73) ================= */

        function getPageItems(current, total) {

            const wanted = [0, total - 1, current - 1, current, current + 1]
                .filter(function (page) {
                    return page >= 0 && page < total;
                });

            const pages = Array.from(new Set(wanted)).sort(function (a, b) {
                return a - b;
            });


            const items = [];

            pages.forEach(function (page, index) {

                const previous = pages[index - 1];

                if (index > 0 && page - previous === 2) {

                    /* Chỉ thiếu đúng 1 trang: hiện luôn trang đó thay cho "…" */
                    items.push(previous + 1);

                } else if (index > 0 && page - previous > 2) {

                    items.push("…");

                }

                items.push(page);

            });


            return items;

        }


        function renderPagination(current, total) {

            if (total <= 1) {

                pagination.innerHTML = "";

                return;

            }


            let html = `
                <button
                    type="button"
                    class="page-btn"
                    data-page="${current - 1}"
                    aria-label="Trang trước"
                    ${current === 0 ? "disabled" : ""}
                >‹</button>
            `;

            getPageItems(current, total).forEach(function (item) {

                if (item === "…") {

                    html += '<span class="page-ellipsis">…</span>';

                    return;

                }

                html += `
                    <button
                        type="button"
                        class="page-btn ${item === current ? "active" : ""}"
                        data-page="${item}"
                        ${item === current ? 'aria-current="page"' : ""}
                    >${item + 1}</button>
                `;

            });

            html += `
                <button
                    type="button"
                    class="page-btn"
                    data-page="${current + 1}"
                    aria-label="Trang sau"
                    ${current === total - 1 ? "disabled" : ""}
                >›</button>
            `;

            pagination.innerHTML = html;


            pagination.querySelectorAll(".page-btn").forEach(function (button) {

                button.addEventListener("click", function () {

                    state.page = Number(button.dataset.page);

                    loadProducts();

                    scrollToProducts();

                });

            });

        }


        function scrollToProducts() {

            const top =
                document.querySelector(".products-layout").getBoundingClientRect().top +
                window.scrollY - 110;

            window.scrollTo({ top: top, behavior: "smooth" });

        }


        /* ================= KHOẢNG GIÁ ================= */

        /* Trả về true nếu hợp lệ (và đã ghi vào state) */

        function readPriceInputs() {

            const min = minPriceInput.value === "" ? null : Number(minPriceInput.value);

            const max = maxPriceInput.value === "" ? null : Number(maxPriceInput.value);


            if ((min !== null && (isNaN(min) || min < 0)) ||
                (max !== null && (isNaN(max) || max < 0))) {

                priceError.textContent = "Giá phải là số không âm.";

                return false;

            }


            if (min !== null && max !== null && min > max) {

                priceError.textContent = "Giá \"Từ\" phải nhỏ hơn hoặc bằng giá \"Đến\".";

                return false;

            }


            priceError.textContent = "";

            state.minPrice = min;

            state.maxPrice = max;

            return true;

        }


        /* ================= SỰ KIỆN ================= */

        function bindEvents() {

            document.querySelectorAll('input[name="category"]')
                .forEach(function (radio) {

                    radio.addEventListener("change", function () {

                        state.category = radio.value;

                        state.brandId = "";

                        state.page = 0;

                        syncUrl();

                        buildBrandFilters();

                        loadProducts();

                    });

                });


            [minPriceInput, maxPriceInput].forEach(function (input) {

                input.addEventListener("change", function () {

                    if (!readPriceInputs()) {
                        return;
                    }

                    state.page = 0;

                    loadProducts();

                });

            });


            sortSelect.addEventListener("change", function () {

                state.sort = sortSelect.value;

                state.page = 0;

                loadProducts();

            });


            document.getElementById("clearFiltersBtn")
                .addEventListener("click", function () {

                    state.category = "";

                    state.brandId = "";

                    state.minPrice = null;

                    state.maxPrice = null;

                    state.search = "";

                    state.sort = sortSelect.options[0].value;

                    state.page = 0;


                    checkCategoryRadio("");

                    minPriceInput.value = "";

                    maxPriceInput.value = "";

                    priceError.textContent = "";

                    sortSelect.value = state.sort;


                    const headerInput =
                        document.querySelector(".header .search-form input");

                    if (headerInput) {
                        headerInput.value = "";
                    }


                    syncUrl();

                    buildBrandFilters();

                    loadProducts();

                });


            toggleFiltersBtn.addEventListener("click", function () {

                const isOpen = sidebar.classList.toggle("open");

                toggleFiltersBtn.setAttribute("aria-expanded", String(isOpen));

            });

        }

    }
);
