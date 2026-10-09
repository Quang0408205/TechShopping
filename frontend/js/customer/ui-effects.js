/* ================= HIỆU ỨNG GIAO DIỆN KHU VỰC KHÁCH ================= */

/*
 * Chỉ thao tác giao diện, không gọi API. Nạp cuối <body>, sau main.js.
 * Header / footer do layout.js chèn nên phải chờ layoutReady.
 */

(function () {

    const MOBILE_NAV_QUERY = window.matchMedia("(max-width: 1024px)");

    const ready = typeof layoutReady !== "undefined" ? layoutReady : Promise.resolve();

    ready.then(function () {

        setupMobileNav();

        setupMegaMenu();

        setupSearchSuggest();

        setupNewsletter();

        setupBackToTop();

    });

    setupHeroCarousel();

    setupWeekCountdown();


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


    /* ---------- Nhận tin (MOCK: chưa có API) ---------- */

    function setupNewsletter() {

        document.querySelectorAll(".newsletter-form").forEach(function (form) {

            const input = form.querySelector("input[type='email']");

            form.addEventListener("submit", function (event) {

                event.preventDefault();

                const valid = input.value.trim() !== "" && input.checkValidity();

                input.setAttribute("aria-invalid", String(!valid));

                if (typeof showToast !== "function") {
                    return;
                }

                if (!valid) {
                    showToast("Vui lòng nhập email hợp lệ.", "error");
                    input.focus();
                    return;
                }

                showToast("Cảm ơn bạn! Tính năng nhận tin đang được hoàn thiện.");

                form.reset();

                input.removeAttribute("aria-invalid");

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


    /* ---------- Lên đầu trang ---------- */

    function setupBackToTop() {

        document.querySelectorAll(".back-to-top").forEach(function (button) {

            button.addEventListener("click", function () {

                window.scrollTo({ top: 0, behavior: reducedMotion() ? "auto" : "smooth" });

            });

        });

    }

})();
