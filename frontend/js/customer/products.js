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
         * Giá trị radio → slug danh mục trong DB. tab dựng từ các danh mục ĐANG HIỂN THỊ
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
            onSale: false,
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


        /* ================= DẢI DANH MỤC ================= */

        const categoryStrip = document.querySelector(".category-strip");

        const ALL_CATEGORIES_ICON = '<path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z"/>';

        function categoryTabIconHtml(paths) {

            return `<span class="category-tab-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" `
                + `stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${paths}</svg></span>`;

        }

        function updateStripNav() {

            if (!categoryStrip) {
                return;
            }

            const max = categoryTabs.scrollWidth - categoryTabs.clientWidth;

            categoryStrip.classList.toggle("has-prev", categoryTabs.scrollLeft > 1);

            categoryStrip.classList.toggle("has-next", categoryTabs.scrollLeft < max - 1);

        }

        /* Thẻ đang chọn cuộn vào giữa dải (chỉ cuộn dải, không cuộn cả trang) */
        function revealActiveCategoryTab() {

            const active = categoryTabs.querySelector("input:checked");

            if (!active) {
                return;
            }

            const card = active.closest(".category-tab");

            categoryTabs.scrollTo({
                left: card.offsetLeft - (categoryTabs.clientWidth - card.offsetWidth) / 2,
                behavior: "smooth"
            });

        }

        /* Số sản phẩm đang bán của từng danh mục: một lần gọi GET /categories/product-counts */
        async function loadCategoryCounts(categories) {

            let rows;

            try {
                rows = await apiRequest("/categories/product-counts");
            } catch (error) {
                return;
            }

            const byId = {};

            let total = 0;

            (Array.isArray(rows) ? rows : []).forEach(function (row) {
                byId[row.categoryId] = Number(row.count) || 0;
            });

            categories.forEach(function (category) {

                const count = byId[category.id] || 0;

                total += count;

                const radio = categoryTabs.querySelector('input[value="' + categoryKeyOf(category.slug) + '"]');

                if (radio) {
                    radio.nextElementSibling.querySelector(".category-tab-name").dataset.count = count.toLocaleString("vi-VN");
                }

            });

            const all = categoryTabs.querySelector('input[value=""]');

            if (all) {
                all.nextElementSibling.querySelector(".category-tab-name").dataset.count = total.toLocaleString("vi-VN");
            }

            updateStripNav();

        }

        function setupCategoryStrip() {

            if (!categoryStrip) {
                return;
            }

            categoryTabs.addEventListener("scroll", updateStripNav);

            window.addEventListener("resize", updateStripNav);

            categoryStrip.querySelector(".category-strip-nav.is-prev").addEventListener("click", function () {
                categoryTabs.scrollBy({ left: -categoryTabs.clientWidth * 0.7, behavior: "smooth" });
            });

            categoryStrip.querySelector(".category-strip-nav.is-next").addEventListener("click", function () {
                categoryTabs.scrollBy({ left: categoryTabs.clientWidth * 0.7, behavior: "smooth" });
            });

            /* kéo bằng chuột (cảm ứng đã tự vuốt được); kéo xong thì không tính là bấm vào thẻ */
            let dragStartX = null;

            let dragStartScroll = 0;

            let dragged = false;

            categoryTabs.addEventListener("pointerdown", function (event) {

                if (event.pointerType !== "mouse" || event.button !== 0) {
                    return;
                }

                dragStartX = event.clientX;

                dragStartScroll = categoryTabs.scrollLeft;

                dragged = false;

            });

            window.addEventListener("pointermove", function (event) {

                if (dragStartX === null) {
                    return;
                }

                const dx = event.clientX - dragStartX;

                if (!dragged && Math.abs(dx) > 5) {
                    dragged = true;
                    categoryTabs.classList.add("is-dragging");
                }

                if (dragged) {
                    categoryTabs.scrollLeft = dragStartScroll - dx;
                }

            });

            window.addEventListener("pointerup", function () {

                dragStartX = null;

                categoryTabs.classList.remove("is-dragging");

            });

            categoryTabs.addEventListener("click", function (event) {

                if (dragged) {
                    event.preventDefault();
                    event.stopPropagation();
                    dragged = false;
                }

            }, true);

            /* dải dính dưới header → thu gọn (mốc = một thẻ 1px đặt ngay trên dải) */
            const sentinel = document.querySelector(".category-strip-sentinel");

            if (sentinel && typeof IntersectionObserver !== "undefined") {

                let observer = null;

                const observe = function () {

                    if (observer) {
                        observer.disconnect();
                    }

                    const top = parseFloat(getComputedStyle(categoryStrip).top) || 0;

                    observer = new IntersectionObserver(function (entries) {

                        categoryStrip.classList.toggle("is-stuck",
                            !entries[0].isIntersecting && entries[0].boundingClientRect.top < top);

                    }, { rootMargin: "-" + Math.round(top) + "px 0px 0px 0px", threshold: [0, 1] });

                    observer.observe(sentinel);

                };

                observe();

                window.addEventListener("resize", observe);

            }

        }

        setupCategoryStrip();


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
                        <span class="category-tab-card">
                            ${categoryTabIconHtml(CATEGORY_ART_PATHS[categoryArtKey(category.slug)])}
                            <span class="category-tab-name" data-count="">${escapeHtml(category.name)}</span>
                        </span>
                    </label>
                `;

            }).join("");

            categoryTabs.innerHTML = `
                <label class="category-tab">
                    <input type="radio" name="category" value="">
                    <span class="category-tab-card">
                        ${categoryTabIconHtml(ALL_CATEGORIES_ICON)}
                        <span class="category-tab-name" data-count="">Tất cả</span>
                    </span>
                </label>
            ` + tabs;

            updateStripNav();

            loadCategoryCounts(categories);

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

            /* Từ mega-menu: ?brand=<id>&min=&max= (chỉ nhận số hợp lệ) */
            const brand = params.get("brand");

            if (category && brand && /^\d{1,9}$/.test(brand)) {
                state.brandId = brand;
            }

            if (params.get("sale") === "1") {
                state.onSale = true;
                document.getElementById("saleOnlyInput").checked = true;
            }

            const min = params.get("min");

            const max = params.get("max");

            if (min && /^\d{1,12}$/.test(min)) {
                state.minPrice = Number(min);
                minPriceInput.value = min;
            }

            if (max && /^\d{1,12}$/.test(max)) {
                state.maxPrice = Number(max);
                maxPriceInput.value = max;
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

            Object.keys(state.specFilters).forEach(function (group) {
                state.specFilters[group].forEach(function (value) {
                    params.append("f", group + ":" + value);
                });
            });

            if (state.category && state.brandId) {
                params.set("brand", state.brandId);
            }

            if (state.minPrice !== null) {
                params.set("min", String(state.minPrice));
            }

            if (state.maxPrice !== null) {
                params.set("max", String(state.maxPrice));
            }

            if (state.onSale) {
                params.set("sale", "1");
            }


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

                revealActiveCategoryTab();
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

                        syncUrl();

                        loadProducts();

                    });

                });

        }


        /* ================= NHU CẦU + THÔNG SỐ ================= */

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

            if (state.onSale) {
                params.set("onSale", "true");
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

            renderCategoryHero();


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

            insertGridPromo(page.content.length);

            renderPagination(page.page, page.totalPages);

        }


        /* ================= ĐẦU TRANG THEO DANH MỤC + Ô XEN LƯỚI ================= */

        const heroBox = document.getElementById("pageHero");

        const HERO_DEFAULT = {
            title: document.getElementById("pageHeroTitle").textContent,
            text: document.getElementById("pageHeroText").textContent
        };

        function renderCategoryHero() {

            const category = state.category && categoryMaps
                ? categoryMaps.visible.find(function (item) { return item.id === getCategoryId(state.category); })
                : null;

            const title = document.getElementById("pageHeroTitle");

            const text = document.getElementById("pageHeroText");

            const meta = document.getElementById("pageHeroMeta");

            heroBox.classList.toggle("has-category", Boolean(category));

            heroBox.querySelector(".page-hero-art")?.remove();

            if (!category) {

                title.textContent = HERO_DEFAULT.title;

                text.textContent = HERO_DEFAULT.text;

                meta.hidden = true;

                return;

            }

            title.textContent = category.name;

            text.textContent = category.description && category.description.trim()
                ? category.description.trim()
                : "Hàng chính hãng, giá niêm yết rõ ràng, nhận tại cửa hàng hoặc giao tận nơi.";

            meta.innerHTML = ["Trả góp 0% cho đơn từ 3.000.000đ", "Trả hàng trong 7 ngày", "Nhận tại cửa hàng"]
                .map(function (item) { return "<span>" + item + "</span>"; }).join("");

            meta.hidden = false;

            if (isSafeImageUrl(category.iconUrl)) {

                const art = document.createElement("img");

                art.className = "page-hero-art";
                art.alt = "";
                art.src = category.iconUrl.trim();

                heroBox.appendChild(art);

            }

        }

        const GRID_PROMOS = [
            { label: "Trả góp 0%", title: "Mua trước, trả dần mỗi tháng", text: "Áp dụng cho đơn từ 3.000.000đ, kỳ hạn 12 tháng.", link: "services.html", cta: "Xem chính sách" },
            { label: "Nhận tại cửa hàng", title: "Đặt online, ghé lấy máy", text: "Chọn cửa hàng còn hàng ngay ở bước đặt hàng.", link: "services.html", cta: "Xem dịch vụ" }
        ];

        /* Mỗi trang một ô quảng cáo chiếm trọn một hàng, sau thẻ thứ 6 (chỉ khi lưới còn tiếp tục sau đó) */
        function insertGridPromo(count) {

            const cards = grid.querySelectorAll(":scope > .product-card");

            if (count < 8 || cards.length < 7 || grid.classList.contains("is-list")) {
                return;
            }

            const promo = GRID_PROMOS[state.page % GRID_PROMOS.length];

            const tile = document.createElement("aside");

            tile.className = "grid-promo";

            tile.innerHTML = '<div><span class="grid-promo-label">' + promo.label + '</span><strong class="grid-promo-title">' + promo.title + '</strong><p>' + promo.text +
                '</p></div><a class="btn btn-outline-dark" href="' + promo.link + '">' + promo.cta + '</a>';

            cards[5].after(tile);

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


            document.getElementById("saleOnlyInput").addEventListener("change", function (event) {

                state.onSale = event.target.checked;

                state.page = 0;

                syncUrl();

                loadProducts();

            });


            [minPriceInput, maxPriceInput].forEach(function (input) {

                input.addEventListener("change", function () {

                    if (!readPriceInputs()) {
                        return;
                    }

                    state.page = 0;

                    syncUrl();

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

                    state.onSale = false;

                    document.getElementById("saleOnlyInput").checked = false;

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
