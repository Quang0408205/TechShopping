/* ================= TRANG SẢN PHẨM (Checkpoint 2.8) ================= */

/*
 * Dữ liệu thật từ backend (công khai, KHÔNG gửi token):
 *   GET /categories?size=100                 → map slug ↔ id
 *   GET /products?isActive=true&categoryId=&brandId=&keyword=&minPrice=
 *       &maxPrice=&page=&size=12&sort=       → lưới sản phẩm + phân trang
 *   ảnh thẻ: primaryImageUrl có sẵn trong danh sách (IMG-3, js/core/ui.js)
 *
 * Bộ lọc thương hiệu theo danh mục: GET /categories/{id}/brands (máy chủ
 * trả sẵn hãng có sản phẩm đang bán, có cache), lưu tạm trong bộ nhớ theo
 * categoryId.
 */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        /*
         * Giá trị radio → slug danh mục trong DB. Kế hoạch v3: tab dựng từ các danh mục ĐANG HIỂN THỊ
         * (GET /categories), giá trị = khoá cũ (laptop, phone…) nếu có, không thì slug (categoryKeyOf, ui.js).
         */

        const CATEGORY_SLUGS = {};

        const categoryTabs = document.querySelector(".category-tabs");

        const PAGE_SIZE = 12;

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
            page: 0,
            /* Nhu cầu + thông số: { nhóm: [giá trị, …] }, nhóm "need" là slug nhu cầu */
            specFilters: {}
        };

        const specFiltersBox = document.getElementById("specFilters");

        const filterGroupCache = {};

        let specRequestId = 0;


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

                categoryMaps = await loadShopCategories();

                renderCategoryTabs(categoryMaps.visible);

            } catch (error) {

                resultCount.textContent = "";

                showGridError(getErrorMessage(error), start);

                return;

            }


            /* ?category= không khớp danh mục đang hiển thị nào → coi như "Tất cả" */

            if (state.category && !getCategoryId(state.category)) {

                state.category = "";

                syncUrl();

            }

            checkCategoryRadio(state.category);


            buildBrandFilters();

            buildSpecFilters();

            loadProducts();

        }


        /* Tab "Tất cả" + một tab cho mỗi danh mục đang hiển thị (thay các tab viết sẵn trong HTML) */

        function renderCategoryTabs(categories) {

            Object.keys(CATEGORY_SLUGS).forEach(function (key) {
                delete CATEGORY_SLUGS[key];
            });

            const tabs = categories.map(function (category) {

                const key = categoryKeyOf(category.slug);

                CATEGORY_SLUGS[key] = category.slug;

                return `
                    <label class="category-tab">
                        <input type="radio" name="category" value="${escapeHtml(key)}">
                        <span>${escapeHtml(category.name)}</span>
                    </label>
                `;

            }).join("");

            categoryTabs.innerHTML = `
                <label class="category-tab">
                    <input type="radio" name="category" value="">
                    <span>Tất cả</span>
                </label>
            ` + tabs;

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


            /* Kiểm tra sau khi tải danh mục (start); tab viết sẵn trong HTML chỉ để hiện trước */
            if (category) {

                state.category = category;

                checkCategoryRadio(category);

            }


            if (search) {
                state.search = search.slice(0, MAX_KEYWORD_LENGTH);
            }

            params.getAll("f").forEach(function (item) {

                const colon = item.indexOf(":");

                if (colon > 0 && colon < item.length - 1) {
                    addSpecFilter(item.slice(0, colon), item.slice(colon + 1));
                }

            });

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

            Object.keys(state.specFilters).forEach(function (group) {
                state.specFilters[group].forEach(function (value) {
                    params.append("f", group + ":" + value);
                });
            });


            const query = params.toString();

            window.history.replaceState(
                null,
                "",
                window.location.pathname + (query ? "?" + query : "")
            );

        }


        function checkCategoryRadio(value) {

            const radio = Array.from(document.querySelectorAll('input[name="category"]')).find(function (input) {
                return input.value === value;
            });

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


        /* ================= NHU CẦU + THÔNG SỐ (kế hoạch v5 GĐ9) ================= */

        function addSpecFilter(group, value) {

            if (!state.specFilters[group]) {
                state.specFilters[group] = [];
            }

            if (state.specFilters[group].indexOf(value) === -1) {
                state.specFilters[group].push(value);
            }

        }

        function removeSpecFilter(group, value) {

            state.specFilters[group] = (state.specFilters[group] || []).filter(function (item) { return item !== value; });

            if (state.specFilters[group].length === 0) {
                delete state.specFilters[group];
            }

        }

        function loadSpecFilterGroups(categoryId) {

            if (!filterGroupCache[categoryId]) {

                filterGroupCache[categoryId] = apiRequest("/categories/" + categoryId + "/filters").catch(function (error) {

                    delete filterGroupCache[categoryId];

                    throw error;

                });

            }

            return filterGroupCache[categoryId];

        }

        /* Mỗi nhóm một khung gập; chỉ hiện giá trị có sản phẩm (máy chủ đã lọc); chọn nhiều ô được */

        async function buildSpecFilters() {

            const requestId = ++specRequestId;

            const categoryId = getCategoryId(state.category);

            if (!categoryId) {

                specFiltersBox.innerHTML = "";

                state.specFilters = {};

                return;

            }

            let groups;

            try {

                groups = await loadSpecFilterGroups(categoryId);

            } catch (error) {

                if (requestId === specRequestId) {
                    specFiltersBox.innerHTML = "";
                }

                return;

            }

            if (requestId !== specRequestId) {
                return;
            }

            /* Bỏ giá trị trên địa chỉ không còn tồn tại */
            Object.keys(state.specFilters).forEach(function (group) {

                const known = groups.find(function (item) { return item.key === group; });

                (state.specFilters[group] || []).slice().forEach(function (value) {

                    if (!known || !known.values.some(function (item) { return item.value === value; })) {
                        removeSpecFilter(group, value);
                    }

                });

            });

            specFiltersBox.innerHTML = groups.map(function (group, index) {

                const selected = state.specFilters[group.key] || [];

                const open = index < 3 || selected.length > 0;

                return `
                    <details class="filter-group spec-filter-group" data-group="${escapeHtml(group.key)}" ${open ? "open" : ""}>
                        <summary><h4>${escapeHtml(group.title)}</h4></summary>
                        <div class="spec-filter-options">
                            ${group.values.map(function (item) {

                                const label = item.label || item.value;

                                return `
                                    <label>
                                        <input type="checkbox" data-group="${escapeHtml(group.key)}" value="${escapeHtml(item.value)}" ${selected.indexOf(item.value) !== -1 ? "checked" : ""}>
                                        <span>${escapeHtml(label)}</span>
                                        <small>${escapeHtml(String(item.count))}</small>
                                    </label>
                                `;

                            }).join("")}
                        </div>
                    </details>
                `;

            }).join("");

            specFiltersBox.querySelectorAll('input[type="checkbox"]').forEach(function (box) {

                box.addEventListener("change", function () {

                    if (box.checked) {
                        addSpecFilter(box.dataset.group, box.value);
                    } else {
                        removeSpecFilter(box.dataset.group, box.value);
                    }

                    state.page = 0;

                    syncUrl();

                    loadProducts();

                });

            });

        }

        function getCategoryLabel(categoryKey) {

            const radio = Array.from(document.querySelectorAll('input[name="category"]')).find(function (input) {
                return input.value === categoryKey;
            });

            return radio ? radio.parentElement.textContent.trim() : "";

        }


        /*
         * Hãng có sản phẩm trong danh mục, sắp xếp theo tên. Lưu Promise vào
         * cache để 2 lần chọn liên tiếp không tải trùng; lỗi thì xóa khỏi cache.
         */

        function loadBrandsForCategory(categoryId) {

            if (!brandCache[categoryId]) {

                brandCache[categoryId] = fetchBrands(categoryId)
                    .catch(function (error) {

                        delete brandCache[categoryId];

                        throw error;

                    });

            }

            return brandCache[categoryId];

        }


        async function fetchBrands(categoryId) {

            /* Máy chủ trả sẵn thương hiệu có sản phẩm đang bán trong danh mục (có cache) */
            const brands = await apiRequest("/categories/" + categoryId + "/brands");

            return (brands || []).map(function (brand) {
                return { id: brand.id, name: brand.name || "" };
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

            if (categoryId) {

                Object.keys(state.specFilters).forEach(function (group) {
                    state.specFilters[group].forEach(function (value) {
                        params.append(group === "need" ? "need" : "filter", group === "need" ? value : group + ":" + value);
                    });
                });

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

            /* Tab dựng lại sau khi tải danh mục → nghe sự kiện ở khung tab */
            [categoryTabs]
                .forEach(function (tabs) {

                    tabs.addEventListener("change", function (event) {

                        const radio = event.target;

                        if (radio.name !== "category") {
                            return;
                        }

                        state.category = radio.value;

                        state.brandId = "";

                        state.specFilters = {};

                        state.page = 0;

                        syncUrl();

                        buildBrandFilters();

                        buildSpecFilters();

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

                    state.specFilters = {};

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

                    buildSpecFilters();

                    loadProducts();

                });


            toggleFiltersBtn.addEventListener("click", function () {

                const isOpen = sidebar.classList.toggle("open");

                toggleFiltersBtn.setAttribute("aria-expanded", String(isOpen));

            });

        }

    }
);
