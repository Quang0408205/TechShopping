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

    /* Số Zalo nhận tin nhắn của khách (chỉ chữ số, vd "0901234567"); để trống thì nút liên hệ không có mục Chat Zalo */
    const ZALO_PHONE = "0877276504";

    /* Số gọi nhanh ở nút liên hệ (cùng số ở chân trang) */
    const HOTLINE = "1900 0000";

    const ready = typeof layoutReady !== "undefined" ? layoutReady : Promise.resolve();

    ready.then(function () {

        setupMobileNav();

        setupMegaMenu();

        setupBackToTop();

        trackStickyHeader();

    });

    setupHeroCarousel();

    setupProductsView();

    setupActiveFilters();

    setupFilterSidebar();

    setupBuyBar();

    setupPasswordToggles();

    setupContactHub();

    setupPageProgress();


    /*
     * Nút liên hệ gộp: một nút tròn ở góc phải, bấm xòe ra Chat Zalo / Trợ lý AI / Gọi hotline.
     * Thay bong bóng Zalo riêng; nút chatbot cũ vẫn có trong trang (chatbot.js) nhưng được ẩn, "Trợ lý AI" bấm hộ nó.
     */

    function setupContactHub() {

        if (document.getElementById("contactHub")) {
            return;
        }

        const phone = ZALO_PHONE.replace(/\D/g, "");

        const hotlineDigits = HOTLINE.replace(/\D/g, "");

        const hub = document.createElement("div");

        hub.id = "contactHub";

        hub.className = "contact-hub";

        hub.innerHTML = `
            <div class="contact-hub-menu" id="contactHubMenu" hidden>
                ${phone ? `<a class="contact-hub-item is-zalo" id="zaloBubble" href="https://zalo.me/${phone}" target="_blank" rel="noopener noreferrer">
                    <span class="contact-hub-icon" aria-hidden="true">Zalo</span><span class="contact-hub-label">Chat Zalo</span></a>` : ""}
                <button type="button" class="contact-hub-item is-ai" data-hub="ai">
                    <span class="contact-hub-icon" aria-hidden="true"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l1.8 4.7L18.5 9.5l-4.7 1.8L12 16l-1.8-4.7L5.5 9.5l4.7-1.8zM18 15l.8 2.2L21 18l-2.2.8L18 21l-.8-2.2L15 18l2.2-.8z"/></svg></span>
                    <span class="contact-hub-label">Trợ lý AI</span></button>
                <a class="contact-hub-item is-phone" href="tel:${hotlineDigits}">
                    <span class="contact-hub-icon" aria-hidden="true"><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z"/></svg></span>
                    <span class="contact-hub-label">Gọi ${HOTLINE}</span></a>
            </div>
            <button type="button" class="contact-hub-toggle" id="contactHubToggle" aria-expanded="false" aria-controls="contactHubMenu" aria-label="Liên hệ với POY">
                <svg class="is-open" viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 5h16v11H9l-5 4z"/><path d="M8.5 10h.01M12 10h.01M15.5 10h.01"/></svg>
                <svg class="is-close" viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
        `;

        document.body.appendChild(hub);

        document.body.classList.add("has-contact-hub");

        const toggle = document.getElementById("contactHubToggle");

        const menu = document.getElementById("contactHubMenu");

        function setOpen(open) {

            menu.hidden = !open;

            hub.classList.toggle("is-open", open);

            toggle.setAttribute("aria-expanded", String(open));

        }

        toggle.addEventListener("click", function () {
            /* trang không có chatbot (đăng nhập…) → không có mục Trợ lý AI */
            hub.querySelector('[data-hub="ai"]').hidden = !document.getElementById("chatbotLauncher");
            setOpen(menu.hidden);
        });

        hub.querySelector('[data-hub="ai"]').addEventListener("click", function () {

            const launcher = document.getElementById("chatbotLauncher");

            setOpen(false);

            if (launcher) {
                launcher.click();
            }

        });

        hub.addEventListener("click", function (event) {

            if (event.target.closest("a.contact-hub-item")) {
                setOpen(false);
            }

        });

        document.addEventListener("click", function (event) {

            if (!menu.hidden && !hub.contains(event.target)) {
                setOpen(false);
            }

        });

        document.addEventListener("keydown", function (event) {

            if (event.key === "Escape" && !menu.hidden) {
                setOpen(false);
                toggle.focus();
            }

        });

        /* link "Liên hệ" còn trong nội dung trang / chatbot (kể cả link vẽ sau) → mở Zalo khi đã có số */
        if (phone) {

            document.addEventListener("click", function (event) {

                const target = event.target.closest('a[href$="contact.html"]');

                if (!target || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey) {
                    return;
                }

                event.preventDefault();

                window.open("https://zalo.me/" + phone, "_blank", "noopener");

            });

        }

    }


    /*
     * Thanh tiến độ khi chuyển trang: bấm link sang trang khác của web (hoặc chuyển trang bằng JS / gửi form) → thanh chạy dần
     * tới ~90%; trang mới mở ra thì chạy nốt tới 100% rồi mờ đi (đánh dấu qua sessionStorage). Quay lại bằng nút Back
     * (trang lấy từ bộ nhớ đệm) thì tắt ngay.
     */

    function setupPageProgress() {

        const KEY = "poy_nav_progress";

        let bar = null;

        let timer = null;

        let safety = null;

        let value = 0;

        function element() {

            if (!bar) {

                bar = document.createElement("div");

                bar.className = "page-progress";

                bar.setAttribute("aria-hidden", "true");

                bar.appendChild(document.createElement("i"));

                document.body.appendChild(bar);

            }

            return bar;

        }

        function set(next) {

            value = next;

            element().firstChild.style.transform = "scaleX(" + next.toFixed(3) + ")";

        }

        function start() {

            if (timer) {
                return;
            }

            element().classList.remove("is-done");

            bar.classList.add("is-active");

            set(0.12);

            /* nhích dần, chậm lại khi gần 90% */
            timer = setInterval(function () {
                set(value + (0.9 - value) * 0.09);
            }, 180);

            /* trang không chuyển (vd. huỷ hộp thoại rời trang): không để thanh chạy mãi */
            safety = setTimeout(finish, 12000);

            try {
                sessionStorage.setItem(KEY, String(Date.now()));
            } catch (error) {
                /* bỏ qua */
            }

        }

        function finish() {

            clearTimeout(safety);

            clearInterval(timer);

            timer = null;

            element().classList.add("is-active");

            set(1);

            setTimeout(function () { bar.classList.add("is-done"); }, 280);

            setTimeout(function () {
                bar.classList.remove("is-active", "is-done");
                set(0);
            }, 700);

        }

        function shouldTrack(link, event) {

            if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
                return false;
            }

            if (link.target && link.target !== "_self" || link.hasAttribute("download")) {
                return false;
            }

            const url = new URL(link.href, location.href);

            if (!/^https?:$/.test(url.protocol) || url.origin !== location.origin) {
                return false;
            }

            /* chỉ đổi #… trên cùng trang: không tải trang mới */
            return !(url.pathname === location.pathname && url.search === location.search && (url.hash || link.getAttribute("href").startsWith("#")));

        }

        document.addEventListener("click", function (event) {

            const link = event.target.closest && event.target.closest("a[href]");

            if (!link || !shouldTrack(link, event)) {
                return;
            }

            /* chờ các xử lý khác của cú bấm (vd. link "Liên hệ" mở Zalo) quyết định có chặn không */
            setTimeout(function () {

                if (!event.defaultPrevented) {
                    start();
                }

            }, 0);

        });

        /* chuyển trang bằng JS (location.href = …) hoặc gửi form */
        window.addEventListener("beforeunload", start);

        window.addEventListener("pageshow", function (event) {

            if (event.persisted) {

                clearInterval(timer);

                timer = null;

                if (bar) {
                    bar.classList.remove("is-active", "is-done");
                    set(0);
                }

            }

        });

        /* vừa mở từ một lần chuyển trang: chạy nốt */
        let startedAt = 0;

        try {
            startedAt = Number(sessionStorage.getItem(KEY)) || 0;
            sessionStorage.removeItem(KEY);
        } catch (error) {
            startedAt = 0;
        }

        if (startedAt && Date.now() - startedAt < 15000) {

            element().classList.add("is-active");

            set(0.7);

            if (document.readyState === "complete") {
                setTimeout(finish, 60);
            } else {
                window.addEventListener("load", function () { setTimeout(finish, 60); }, { once: true });
            }

        }

    }


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


    /* ---------- So khớp không dấu (ô tìm hãng của bộ lọc) ---------- */

    function normalize(text) {

        return text.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().replace(/đ/g, "d").trim();

    }


    /* ---------- Carousel hero (trang chủ) ---------- */

    /*
     * Slide mới quét vào bằng màn che (slide cũ giữ .is-leaving tới khi quét xong), bộ đếm "01 / 03",
     * mỗi chấm là một thanh thời gian chạy đúng INTERVAL. Dừng khi rê chuột / focus thì thời gian còn lại được giữ.
     */

    function setupHeroCarousel() {

        const hero = document.querySelector(".hero[aria-roledescription='carousel']");

        if (!hero) {
            return;
        }

        const slides = Array.from(hero.querySelectorAll(".hero-slide"));

        const dots = Array.from(hero.querySelectorAll("[data-hero-dot]"));

        const counter = hero.querySelector("[data-hero-current]");

        const INTERVAL = 6000;

        const WIPE_MS = 950;

        hero.style.setProperty("--hero-interval", INTERVAL + "ms");

        let current = 0;

        let timer = null;

        let leaveTimer = null;

        let paused = false;

        let remaining = INTERVAL;

        let startedAt = 0;


        function show(index) {

            const previous = current;

            current = (index + slides.length) % slides.length;

            clearTimeout(leaveTimer);

            slides.forEach(function (slide, i) {

                const active = i === current;

                slide.classList.toggle("is-active", active);

                slide.classList.toggle("is-leaving", !active && i === previous && previous !== current);

                slide.setAttribute("aria-hidden", String(!active));

                slide.querySelectorAll("a, button").forEach(function (control) {
                    control.tabIndex = active ? 0 : -1;
                });

            });

            leaveTimer = setTimeout(function () {
                slides.forEach(function (slide) { slide.classList.remove("is-leaving"); });
            }, WIPE_MS);

            dots.forEach(function (dot, i) {

                if (i === current) {
                    dot.setAttribute("aria-current", "true");
                } else {
                    dot.removeAttribute("aria-current");
                }

                /* chạy lại thanh thời gian của chấm đang chọn */
                dot.classList.remove("is-running");

            });

            if (counter) {
                counter.textContent = String(current + 1).padStart(2, "0");
            }

            void hero.offsetWidth;

            if (dots[current]) {
                dots[current].classList.add("is-running");
            }

            remaining = INTERVAL;

        }

        function schedule() {

            clearTimeout(timer);

            hero.classList.toggle("is-paused", paused || reducedMotion());

            if (!paused && !reducedMotion() && !document.hidden) {

                startedAt = Date.now();

                timer = setTimeout(function () {
                    show(current + 1);
                    schedule();
                }, remaining);

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

            if (!paused && timer) {
                remaining = Math.max(400, remaining - (Date.now() - startedAt));
            }

            paused = true;

            schedule();

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

        /* màn hình mở đầu đang che: chờ nó xong mới bắt đầu tính giờ */
        if (document.documentElement.classList.contains("has-intro")) {

            paused = true;

            schedule();

            document.addEventListener("poy:intro-done", resume, { once: true });

        } else {

            schedule();

        }

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

            /* Nhu cầu + thông số đã tích: mỗi ô một chip, bấm để bỏ ô đó */
            scope.querySelectorAll('#specFilters input[type="checkbox"]:checked').forEach(function (box) {

                chips.push({
                    label: labelOf(box).replace(/\s+\d+$/, ""),
                    clear: function () {
                        box.checked = false;
                        box.dispatchEvent(new Event("change", { bubbles: true }));
                    }
                });

            });

            const saleBox = document.getElementById("saleOnlyInput");

            if (saleBox && saleBox.checked) {
                chips.push({
                    label: "Đang giảm giá",
                    clear: function () {
                        saleBox.checked = false;
                        saleBox.dispatchEvent(new Event("change", { bubbles: true }));
                    }
                });
            }

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
