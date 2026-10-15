/* ================= HIỆU ỨNG GIAO DIỆN KHU VỰC KHÁCH ================= */

/*
 * Chỉ thao tác giao diện, không gọi API. Nạp cuối <body>, sau main.js.
 * Header / footer do layout.js chèn nên phải chờ layoutReady.
 */

(function () {

    const MOBILE_NAV_QUERY = window.matchMedia("(max-width: 1024px)");

    /* Khai báo trước các lời gọi setup bên dưới (const chưa khởi tạo → ReferenceError) */

    const VIEW_STORAGE_KEY = "poy_products_view";

    const BRAND_VISIBLE_COUNT = 6;

    const BRAND_SEARCH_MIN = 8;

    const FILTER_DRAWER_QUERY = window.matchMedia("(max-width: 899px)");

    const ready = typeof layoutReady !== "undefined" ? layoutReady : Promise.resolve();

    ready.then(function () {

        setupMobileNav();

        setupMegaMenu();

        setupSearchSuggest();

        setupBackToTop();

        trackStickyHeader();

    });

    setupHeroCarousel();

    setupWeekCountdown();

    setupProductsView();

    setupActiveFilters();

    setupFilterSidebar();

    setupBuyBar();

    setupPasswordToggles();


    function reducedMotion() {

        return typeof prefersReducedMotion === "function" && prefersReducedMotion();

    }


    /* ---------- Menu mobile ---------- */

    function setupMobileNav() {

        const toggle = document.querySelector(".header .menu-toggle");

        const nav = document.querySelector(".header .nav");

        if (!toggle || !nav) {
            return;
        }

        const closeButton = nav.querySelector(".nav-close");


        function setOpen(open) {

            nav.classList.toggle("is-open", open);

            document.body.classList.toggle("nav-open", open);

            toggle.setAttribute("aria-expanded", String(open));

            if (open) {
                (closeButton || nav).focus();
            }

        }


        toggle.addEventListener("click", function () {
            setOpen(!nav.classList.contains("is-open"));
        });

        if (closeButton) {

            closeButton.addEventListener("click", function () {
                setOpen(false);
                toggle.focus();
            });

        }

        document.addEventListener("click", function (event) {

            if (nav.classList.contains("is-open") && !nav.contains(event.target) && !toggle.contains(event.target)) {
                setOpen(false);
            }

        });

        document.addEventListener("keydown", function (event) {

            if (event.key === "Escape" && nav.classList.contains("is-open")) {
                setOpen(false);
                toggle.focus();
            }

        });

        MOBILE_NAV_QUERY.addEventListener("change", function () {
            setOpen(false);
        });

    }


    /* ---------- Mega-menu danh mục (desktop) ---------- */

    function setupMegaMenu() {

        const trigger = document.querySelector(".header .nav-has-mega");

        const toggle = document.querySelector(".header .mega-toggle");

        const menu = document.getElementById("megaMenu");

        if (!trigger || !toggle || !menu) {
            return;
        }

        let closeTimer = null;


        function setOpen(open) {

            clearTimeout(closeTimer);

            menu.hidden = !open;

            toggle.setAttribute("aria-expanded", String(open));

        }

        function openOnHover() {

            if (!MOBILE_NAV_QUERY.matches) {
                setOpen(true);
            }

        }

        function closeSoon() {

            clearTimeout(closeTimer);

            closeTimer = setTimeout(function () {
                setOpen(false);
            }, 160);

        }


        [trigger, toggle, menu].forEach(function (element) {

            element.addEventListener("mouseenter", openOnHover);

            element.addEventListener("mouseleave", closeSoon);

        });

        toggle.addEventListener("click", function () {
            setOpen(menu.hidden);
        });

        menu.addEventListener("focusout", function (event) {

            if (!menu.contains(event.relatedTarget) && event.relatedTarget !== toggle) {
                setOpen(false);
            }

        });

        document.addEventListener("keydown", function (event) {

            if (event.key === "Escape" && !menu.hidden) {
                setOpen(false);
                toggle.focus();
            }

        });

    }


    /* ---------- Gợi ý tìm kiếm (dữ liệu tĩnh) ---------- */

    function normalize(text) {

        return text.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().replace(/đ/g, "d").trim();

    }


    function setupSearchSuggest() {

        document.querySelectorAll(".header .search-form").forEach(function (form) {

            const input = form.querySelector("input");

            const panel = form.querySelector(".search-suggest");

            if (!input || !panel) {
                return;
            }

            const options = Array.from(panel.querySelectorAll("[data-suggest]"));


            function refresh() {

                const query = normalize(input.value);

                let visible = 0;

                options.forEach(function (option) {

                    const match = !query || normalize(option.dataset.suggest).includes(query);

                    option.hidden = !match;

                    visible += match ? 1 : 0;

                });

                panel.hidden = visible === 0;

            }


            input.addEventListener("focus", refresh);

            input.addEventListener("input", refresh);

            input.addEventListener("keydown", function (event) {

                if (event.key === "ArrowDown" && !panel.hidden) {

                    event.preventDefault();

                    const first = options.find(function (option) { return !option.hidden; });

                    if (first) {
                        first.focus();
                    }

                }

                if (event.key === "Escape") {
                    panel.hidden = true;
                }

            });

            panel.addEventListener("keydown", function (event) {

                const visible = options.filter(function (option) { return !option.hidden; });

                const index = visible.indexOf(document.activeElement);

                if (event.key === "ArrowRight" || event.key === "ArrowDown") {
                    event.preventDefault();
                    (visible[index + 1] || visible[0]).focus();
                }

                if (event.key === "ArrowLeft" || event.key === "ArrowUp") {
                    event.preventDefault();
                    (visible[index - 1] || input).focus();
                }

                if (event.key === "Escape") {
                    panel.hidden = true;
                    input.focus();
                }

            });

            form.addEventListener("focusout", function (event) {

                if (!form.contains(event.relatedTarget)) {
                    panel.hidden = true;
                }

            });

            options.forEach(function (option) {

                option.addEventListener("click", function () {

                    input.value = option.dataset.suggest;

                    panel.hidden = true;

                    form.requestSubmit();

                });

            });

        });

    }


    /* ---------- Carousel hero (trang chủ) ---------- */

    function setupHeroCarousel() {

        const hero = document.querySelector(".hero[aria-roledescription='carousel']");

        if (!hero) {
            return;
        }

        const slides = Array.from(hero.querySelectorAll(".hero-slide"));

        const dots = Array.from(hero.querySelectorAll("[data-hero-dot]"));

        const INTERVAL = 6500;

        let current = 0;

        let timer = null;

        let paused = false;


        function show(index) {

            current = (index + slides.length) % slides.length;

            slides.forEach(function (slide, i) {

                const active = i === current;

                slide.classList.toggle("is-active", active);

                slide.setAttribute("aria-hidden", String(!active));

                slide.querySelectorAll("a, button").forEach(function (control) {
                    control.tabIndex = active ? 0 : -1;
                });

            });

            dots.forEach(function (dot, i) {

                if (i === current) {
                    dot.setAttribute("aria-current", "true");
                } else {
                    dot.removeAttribute("aria-current");
                }

            });

        }

        function schedule() {

            clearTimeout(timer);

            if (!paused && !reducedMotion() && !document.hidden) {

                timer = setTimeout(function () {
                    show(current + 1);
                    schedule();
                }, INTERVAL);

            }

        }

        function go(index) {

            show(index);

            schedule();

        }


        hero.querySelector("[data-hero-prev]").addEventListener("click", function () { go(current - 1); });

        hero.querySelector("[data-hero-next]").addEventListener("click", function () { go(current + 1); });

        dots.forEach(function (dot) {

            dot.addEventListener("click", function () {
                go(Number(dot.dataset.heroDot));
            });

        });

        hero.addEventListener("keydown", function (event) {

            if (event.key === "ArrowLeft") {
                go(current - 1);
            }

            if (event.key === "ArrowRight") {
                go(current + 1);
            }

        });


        function pause() {
            paused = true;
            clearTimeout(timer);
        }

        function resume() {
            paused = false;
            schedule();
        }

        hero.addEventListener("mouseenter", pause);

        hero.addEventListener("mouseleave", resume);

        hero.addEventListener("focusin", pause);

        hero.addEventListener("focusout", function (event) {

            if (!hero.contains(event.relatedTarget)) {
                resume();
            }

        });

        document.addEventListener("visibilitychange", schedule);


        let startX = null;

        hero.addEventListener("pointerdown", function (event) {

            if (event.pointerType !== "mouse") {
                startX = event.clientX;
            }

        });

        hero.addEventListener("pointerup", function (event) {

            if (startX === null) {
                return;
            }

            const distance = event.clientX - startX;

            startX = null;

            if (Math.abs(distance) > 40) {
                go(current + (distance < 0 ? 1 : -1));
            }

        });


        show(0);

        schedule();

    }


    /* ---------- Đếm ngược tới 23:59:59 Chủ nhật (MOCK: chưa có API ưu đãi) ---------- */

    function setupWeekCountdown() {

        const box = document.querySelector("[data-countdown-week]");

        if (!box) {
            return;
        }

        const units = {};

        box.querySelectorAll("[data-unit]").forEach(function (element) {
            units[element.dataset.unit] = element;
        });


        function endOfWeek() {

            const end = new Date();

            end.setDate(end.getDate() + ((7 - end.getDay()) % 7));

            end.setHours(23, 59, 59, 999);

            return end;

        }

        function pad(value) {
            return String(value).padStart(2, "0");
        }

        function tick() {

            const seconds = Math.max(0, Math.floor((endOfWeek() - Date.now()) / 1000));

            units.d.textContent = Math.floor(seconds / 86400);

            units.h.textContent = pad(Math.floor(seconds / 3600) % 24);

            units.m.textContent = pad(Math.floor(seconds / 60) % 60);

            units.s.textContent = pad(seconds % 60);

        }


        tick();

        setInterval(tick, 1000);

    }


    /* ---------- Trang Sản phẩm: lưới / danh sách ---------- */

    function setupProductsView() {

        const grid = document.getElementById("productGrid");

        const buttons = Array.from(document.querySelectorAll("[data-view]"));

        if (!grid || buttons.length === 0) {
            return;
        }


        function apply(view) {

            grid.classList.toggle("is-list", view === "list");

            buttons.forEach(function (button) {
                button.setAttribute("aria-pressed", String(button.dataset.view === view));
            });

        }


        let saved = "grid";

        try {
            saved = localStorage.getItem(VIEW_STORAGE_KEY) || "grid";
        } catch (error) {
            saved = "grid";
        }

        apply(saved);


        buttons.forEach(function (button) {

            button.addEventListener("click", function () {

                apply(button.dataset.view);

                try {
                    localStorage.setItem(VIEW_STORAGE_KEY, button.dataset.view);
                } catch (error) {
                    /* Trình duyệt chặn lưu trữ: chỉ không nhớ lựa chọn */
                }

            });

        });

    }


    /* ---------- Trang Sản phẩm: chip bộ lọc đang áp dụng ---------- */

    /*
     * Đọc trạng thái từ chính các ô lọc của products.js; bấm × thì đặt lại ô đó
     * và phát sự kiện "change" để products.js tải lại như khi người dùng chọn.
     * Vẽ lại mỗi khi #resultCount đổi (products.js cập nhật sau mỗi lần tải).
     */

    function setupActiveFilters() {

        const box = document.getElementById("activeFilters");

        /* Radio danh mục nằm ở thanh tab, radio thương hiệu ở sidebar */
        const scope = document.querySelector(".products-layout");

        const resultCount = document.getElementById("resultCount");

        if (!box || !scope || !resultCount) {
            return;
        }

        const minInput = document.getElementById("minPriceInput");

        const maxInput = document.getElementById("maxPriceInput");

        const priceError = document.getElementById("priceError");


        function resetRadio(name) {

            const radio = scope.querySelector('input[name="' + name + '"][value=""]');

            if (radio) {
                radio.checked = true;
                radio.dispatchEvent(new Event("change", { bubbles: true }));
            }

        }

        function labelOf(input) {
            return input.parentElement ? input.parentElement.textContent.trim() : input.value;
        }

        function money(value) {
            return typeof formatPrice === "function" ? formatPrice(Number(value)) : value;
        }


        function collect() {

            const chips = [];

            ["category", "brand"].forEach(function (name) {

                const checked = scope.querySelector('input[name="' + name + '"]:checked');

                if (checked && checked.value) {
                    chips.push({ label: labelOf(checked), clear: function () { resetRadio(name); } });
                }

            });

            if ((minInput.value || maxInput.value) && !priceError.textContent) {

                const label = minInput.value && maxInput.value
                    ? money(minInput.value) + " - " + money(maxInput.value)
                    : minInput.value ? "Từ " + money(minInput.value) : "Đến " + money(maxInput.value);

                chips.push({
                    label: label,
                    clear: function () {
                        minInput.value = "";
                        maxInput.value = "";
                        minInput.dispatchEvent(new Event("change", { bubbles: true }));
                    }
                });

            }

            const params = new URLSearchParams(window.location.search);

            const search = params.get("search");

            if (search) {

                chips.push({
                    label: "Từ khoá: " + search,
                    clear: function () {
                        params.delete("search");
                        const query = params.toString();
                        window.location.href = window.location.pathname + (query ? "?" + query : "");
                    }
                });

            }

            return chips;

        }


        function render() {

            const chips = collect();

            box.replaceChildren();

            box.hidden = chips.length === 0;

            chips.forEach(function (chip) {

                const button = document.createElement("button");

                button.type = "button";

                button.className = "active-filter";

                button.textContent = chip.label;

                button.setAttribute("aria-label", "Bỏ lọc " + chip.label);

                button.addEventListener("click", chip.clear);

                box.appendChild(button);

            });

            if (chips.length > 0) {

                const clearAll = document.createElement("button");

                clearAll.type = "button";

                clearAll.className = "active-filters-clear";

                clearAll.textContent = "Xoá tất cả";

                clearAll.addEventListener("click", function () {
                    document.getElementById("clearFiltersBtn").click();
                });

                box.appendChild(clearAll);

            }

        }


        new MutationObserver(render).observe(resultCount, { childList: true, characterData: true, subtree: true });

        render();

    }


    /* ---------- Trang Sản phẩm: chiều cao header còn dính khi cuộn ---------- */

    /* Header dính với top âm (thanh thông báo cuộn mất) → phần còn thấy = cao header + top */

    function trackStickyHeader() {

        const header = document.querySelector(".header");

        if (!header || !document.querySelector(".products-page")) {
            return;
        }


        function update() {

            const top = parseFloat(getComputedStyle(header).top) || 0;

            const visible = Math.max(0, Math.round(header.offsetHeight + Math.min(top, 0)));

            document.documentElement.style.setProperty("--header-sticky-h", visible + "px");

        }

        update();

        if (typeof ResizeObserver !== "undefined") {
            new ResizeObserver(update).observe(header);
        }

    }


    /* ---------- Trang Sản phẩm: sidebar bộ lọc ---------- */

    /*
     * Chỉ đổi cách hiển thị các ô lọc gốc của products.js (thu gọn / tìm hãng,
     * mức giá chọn nhanh, ngăn kéo mobile); mọi thay đổi bộ lọc vẫn đi qua sự
     * kiện "change" hoặc nút toggleFiltersBtn mà products.js đang nghe.
     */

    function setupFilterSidebar() {

        const sidebar = document.getElementById("productsSidebar");

        const brandList = document.getElementById("brandOptionsList");

        if (!sidebar || !brandList) {
            return;
        }

        setupBrandList(sidebar, brandList);

        setupPricePresets(sidebar);

        setupFilterDrawer(sidebar);

    }


    function setupBrandList(sidebar, list) {

        const search = sidebar.querySelector(".brand-search");

        const more = sidebar.querySelector(".brand-more");

        if (!search || !more) {
            return;
        }

        const empty = document.createElement("p");

        empty.className = "filter-hint";

        empty.textContent = "Không có thương hiệu phù hợp.";

        empty.hidden = true;

        list.after(empty);


        let expanded = false;


        /* Bỏ dòng "Tất cả" (value rỗng): luôn hiện */

        function brandLabels() {

            return Array.from(list.querySelectorAll("label")).filter(function (label) {

                const input = label.querySelector('input[name="brand"]');

                return input && input.value;

            });

        }


        function apply() {

            const labels = brandLabels();

            search.hidden = labels.length <= BRAND_SEARCH_MIN;

            const query = search.hidden ? "" : normalize(search.value);

            let collapsed = 0;

            let matches = 0;


            labels.forEach(function (label, index) {

                const checked = label.querySelector("input").checked;

                const show = query
                    ? normalize(label.textContent).includes(query)
                    : expanded || checked || index < BRAND_VISIBLE_COUNT;

                label.classList.toggle("is-collapsed", !show);

                if (show) {
                    matches++;
                } else if (!query) {
                    collapsed++;
                }

            });


            empty.hidden = !query || matches > 0;

            more.hidden = Boolean(query) || labels.length <= BRAND_VISIBLE_COUNT;

            more.textContent = expanded ? "Thu gọn" : "Xem thêm (" + collapsed + ")";

            more.setAttribute("aria-expanded", String(expanded));

        }


        /* products.js dựng lại danh sách khi đổi danh mục → về trạng thái thu gọn */

        new MutationObserver(function () {

            expanded = false;

            search.value = "";

            apply();

        }).observe(list, { childList: true });


        more.addEventListener("click", function () {

            expanded = !expanded;

            apply();

        });

        search.addEventListener("input", apply);

        list.addEventListener("change", apply);

        apply();

    }


    function setupPricePresets(sidebar) {

        const minInput = document.getElementById("minPriceInput");

        const maxInput = document.getElementById("maxPriceInput");

        const applyButton = sidebar.querySelector(".price-apply");

        const presets = Array.from(sidebar.querySelectorAll("[data-price-min]"));

        if (!minInput || !maxInput || !applyButton) {
            return;
        }


        /* Giá trị products.js đã nhận lần cuối: "Áp dụng" không gửi lại khi không đổi */

        let applied = { min: minInput.value, max: maxInput.value };


        function markPresets() {

            presets.forEach(function (button) {

                const active = button.dataset.priceMin === minInput.value &&
                    button.dataset.priceMax === maxInput.value;

                button.setAttribute("aria-pressed", String(active));

            });

        }


        function submit() {

            minInput.dispatchEvent(new Event("change", { bubbles: true }));

        }


        [minInput, maxInput].forEach(function (input) {

            input.addEventListener("change", function () {

                applied = { min: minInput.value, max: maxInput.value };

                markPresets();

            });

            input.addEventListener("input", markPresets);

            input.addEventListener("keydown", function (event) {

                if (event.key === "Enter") {
                    applyButton.click();
                }

            });

        });


        presets.forEach(function (button) {

            button.addEventListener("click", function () {

                /* Bấm lại mức đang chọn = bỏ lọc giá */
                const active = button.getAttribute("aria-pressed") === "true";

                minInput.value = active ? "" : button.dataset.priceMin;

                maxInput.value = active ? "" : button.dataset.priceMax;

                submit();

            });

        });


        applyButton.addEventListener("click", function () {

            if (minInput.value !== applied.min || maxInput.value !== applied.max) {
                submit();
            }

        });


        /* "Xoá bộ lọc" của products.js đặt lại 2 ô mà không phát sự kiện; listener này chạy trước nó */

        const clearButton = document.getElementById("clearFiltersBtn");

        if (clearButton) {

            clearButton.addEventListener("click", function () {

                setTimeout(function () {

                    applied = { min: minInput.value, max: maxInput.value };

                    markPresets();

                }, 0);

            });

        }

        markPresets();

    }


    /*
     * Dưới 900px: products.js bật / tắt class "open" của sidebar khi bấm
     * toggleFiltersBtn; ở đây thêm nền mờ, khoá cuộn, Esc, giữ focus trong
     * ngăn kéo và nút "Xem N kết quả". Đóng = bấm lại toggleFiltersBtn để
     * products.js cập nhật aria-expanded.
     */

    function setupFilterDrawer(sidebar) {

        const toggle = document.getElementById("toggleFiltersBtn");

        const backdrop = document.querySelector(".filters-backdrop");

        const closeButton = sidebar.querySelector(".filters-close");

        const showButton = sidebar.querySelector(".filters-show-results");

        const resultCount = document.getElementById("resultCount");

        if (!toggle || !backdrop || !closeButton || !showButton || !resultCount) {
            return;
        }


        function isOpen() {
            return sidebar.classList.contains("open") && FILTER_DRAWER_QUERY.matches;
        }

        function close() {

            if (sidebar.classList.contains("open")) {
                toggle.click();
            }

        }

        function sync() {

            const open = isOpen();

            backdrop.hidden = !open;

            document.body.classList.toggle("filters-open", open);

            if (open) {
                sidebar.setAttribute("role", "dialog");
                sidebar.setAttribute("aria-modal", "true");
            } else {
                sidebar.removeAttribute("role");
                sidebar.removeAttribute("aria-modal");
            }

        }


        let wasOpen = false;

        new MutationObserver(function () {

            const open = isOpen();

            sync();

            if (open !== wasOpen) {
                (open ? closeButton : toggle).focus();
            }

            wasOpen = open;

        }).observe(sidebar, { attributes: true, attributeFilter: ["class"] });

        FILTER_DRAWER_QUERY.addEventListener("change", function () {

            sync();

            wasOpen = isOpen();

        });


        closeButton.addEventListener("click", close);

        backdrop.addEventListener("click", close);

        showButton.addEventListener("click", function () {

            close();

            const main = document.querySelector(".products-main");

            if (main && main.getBoundingClientRect().top < 0) {
                main.scrollIntoView({ behavior: reducedMotion() ? "auto" : "smooth" });
            }

        });


        document.addEventListener("keydown", function (event) {

            if (!isOpen()) {
                return;
            }

            if (event.key === "Escape") {

                close();

                return;

            }

            if (event.key !== "Tab") {
                return;
            }

            const focusable = Array.from(
                sidebar.querySelectorAll("button, input, select, summary")
            ).filter(function (element) {
                return !element.disabled && element.getClientRects().length > 0;
            });

            const first = focusable[0];

            const last = focusable[focusable.length - 1];

            if (event.shiftKey && document.activeElement === first) {

                event.preventDefault();

                last.focus();

            } else if (!event.shiftKey && document.activeElement === last) {

                event.preventDefault();

                first.focus();

            }

        });


        /* "Xem N kết quả": số lấy từ #resultCount mà products.js cập nhật sau mỗi lần tải */

        function updateShowLabel() {

            const text = resultCount.textContent;

            const match = text.match(/Tìm thấy ([\d.]+) sản phẩm/);

            showButton.textContent = text.indexOf("Đang tải") === 0
                ? "Đang tải..."
                : match ? "Xem " + match[1] + " kết quả" : "Xem kết quả";

        }

        new MutationObserver(updateShowLabel)
            .observe(resultCount, { childList: true, characterData: true, subtree: true });

        updateShowLabel();

    }


    /* ---------- Trang chi tiết: thanh mua hàng dính ---------- */

    function setupBuyBar() {

        const bar = document.getElementById("buyBar");

        const actions = document.querySelector(".detail-actions");

        const addButton = document.getElementById("addToCartBtn");

        const buyButton = document.getElementById("buyNowBtn");

        if (!bar || !actions || !addButton || !buyButton || typeof IntersectionObserver === "undefined") {
            return;
        }

        const nameSource = document.getElementById("detailName");

        const priceSource = document.getElementById("detailPrice");

        const imageSource = document.getElementById("mainImage");

        const barImage = bar.querySelector("[data-buy-bar-image]");

        const barCart = bar.querySelector('[data-buy-bar="cart"]');

        const barBuy = bar.querySelector('[data-buy-bar="buy"]');


        function sync() {

            bar.querySelector("[data-buy-bar-name]").textContent = nameSource.textContent;

            bar.querySelector("[data-buy-bar-price]").textContent = priceSource.textContent;

            if (imageSource.getAttribute("src")) {
                barImage.src = imageSource.currentSrc || imageSource.src;
            }

            barCart.disabled = addButton.disabled;

            barBuy.disabled = buyButton.disabled;

        }


        const observer = new MutationObserver(sync);

        observer.observe(nameSource, { childList: true, characterData: true, subtree: true });

        observer.observe(priceSource, { childList: true, characterData: true, subtree: true });

        observer.observe(imageSource, { attributes: true, attributeFilter: ["src"] });

        observer.observe(addButton, { attributes: true, attributeFilter: ["disabled"] });

        observer.observe(buyButton, { attributes: true, attributeFilter: ["disabled"] });

        sync();


        new IntersectionObserver(function (entries) {

            const entry = entries[0];

            const show = !entry.isIntersecting && entry.boundingClientRect.top < 0;

            bar.classList.toggle("is-visible", show);

            document.body.classList.toggle("buy-bar-open", show);

        }).observe(actions);


        barCart.addEventListener("click", function () {
            addButton.click();
        });

        barBuy.addEventListener("click", function () {
            buyButton.click();
        });

    }


    /* ---------- Hiện / ẩn mật khẩu (trang đăng nhập, đăng ký) ---------- */

    function setupPasswordToggles() {

        document.querySelectorAll(".password-field").forEach(function (field) {

            const input = field.querySelector("input");

            const button = field.querySelector(".password-toggle");

            if (!input || !button) {
                return;
            }

            button.addEventListener("click", function () {

                const show = input.type === "password";

                input.type = show ? "text" : "password";

                button.setAttribute("aria-pressed", String(show));

                button.setAttribute("aria-label", show ? "Ẩn mật khẩu" : "Hiện mật khẩu");

                input.focus();

            });

        });

    }


    /* ---------- Lên đầu trang ---------- */

    function setupBackToTop() {

        document.querySelectorAll(".back-to-top").forEach(function (button) {

            button.addEventListener("click", function () {

                window.scrollTo({ top: 0, behavior: reducedMotion() ? "auto" : "smooth" });

            });

        });

    }

})();
