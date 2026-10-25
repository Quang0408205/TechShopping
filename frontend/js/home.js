document.addEventListener("DOMContentLoaded", function () {

    setupIntro();

    setupHeadingMasks();

    setupHeroParallax();

    renderBrandMarquee();

    renderFlashSale();

    renderHomeCategories();

    renderShowcase();

    renderHomeProductGrids();

    setupHomeTabs();

    renderFeaturedCombos();

    renderHomeStats();

    renderHomeNeeds();

    renderHomeReviews();

    renderRecentViewed();

});


/* ================= MÀN HÌNH MỞ ĐẦU: LOGO BAY VÀO HEADER (kế hoạch v6 B5) ================= */

/*
 * index.html thêm lớp has-intro lần đầu mỗi phiên (sessionStorage poy_intro_seen, không hiện khi giảm chuyển động).
 * CSS vẽ nét chữ POY → đổ màu nâu ánh kim + vệt sáng → dòng chữ gõ dần. Ở đây: tới INTRO_FLY_AT thì logo bay vào đúng
 * chỗ logo trên header (FLIP), rồi hai nửa màn đen tách ra; xong thì báo "poy:intro-done" (carousel hero bắt đầu chạy).
 */

const INTRO_FLY_AT = 1850;

const INTRO_OPEN_AT = 2050;

const INTRO_DONE_AT = 2800;

function setupIntro() {

    const intro = document.getElementById("intro");

    const root = document.documentElement;

    if (!intro) {
        return;
    }

    if (!root.classList.contains("has-intro")) {
        intro.remove();
        return;
    }

    const timers = [];

    let finished = false;

    function fly() {

        const mark = intro.querySelector(".intro-mark");

        const word = intro.querySelector(".intro-word--fill");

        const target = document.querySelector(".header .logo-main");

        if (!mark || !word) {
            return;
        }

        /* khớp khung chữ POY (không phải cả khung SVG) với chữ POY của logo header */
        const from = word.getBoundingClientRect();

        const to = target ? target.getBoundingClientRect() : { left: 24, top: 52, width: 62, height: 28 };

        const box = mark.getBoundingClientRect();

        const scale = Math.max(0.05, to.width / from.width);

        /* SVG thu nhỏ quanh tâm của nó, nên bù độ lệch giữa tâm chữ và tâm khung */
        const cx = box.left + box.width / 2;

        const cy = box.top + box.height / 2;

        const dx = (to.left + to.width / 2) - cx - ((from.left + from.width / 2) - cx) * scale;

        const dy = (to.top + to.height / 2) - cy - ((from.top + from.height / 2) - cy) * scale;

        mark.style.transform = `translate(${dx}px, ${dy}px) scale(${scale})`;

        intro.classList.add("is-flying");

    }

    function open() {

        intro.classList.add("is-opening");

        root.classList.add("intro-opening");

    }

    function done() {

        if (finished) {
            return;
        }

        finished = true;

        timers.forEach(clearTimeout);

        root.classList.remove("has-intro", "intro-opening");

        intro.remove();

        document.dispatchEvent(new CustomEvent("poy:intro-done"));

    }

    function skip() {

        if (finished) {
            return;
        }

        timers.forEach(clearTimeout);

        intro.classList.add("is-skipping");

        open();

        timers.push(setTimeout(done, 420));

    }

    /* tính từ lúc script đầu trang bật intro, để khớp với hoạt hình CSS (chạy từ lần vẽ đầu) */
    const elapsed = window.POY_INTRO_T0 ? performance.now() - window.POY_INTRO_T0 : 0;

    timers.push(setTimeout(fly, Math.max(0, INTRO_FLY_AT - elapsed)));

    timers.push(setTimeout(open, Math.max(0, INTRO_OPEN_AT - elapsed)));

    timers.push(setTimeout(done, Math.max(0, INTRO_DONE_AT - elapsed)));

    document.getElementById("introSkip").addEventListener("click", skip);

    document.addEventListener("keydown", function (event) {

        if (event.key === "Escape") {
            skip();
        }

    });

}


/* ================= TIÊU ĐỀ HIỆN THEO DÒNG TRONG KHUNG CHE ================= */

/* Bọc chữ của mỗi h2 tiêu đề khối; CSS trượt chữ lên khi .section-heading được main.js đánh dấu is-visible */

function setupHeadingMasks() {

    document.querySelectorAll("main .section-heading h2").forEach(function (heading) {

        if (heading.querySelector(".line-mask")) {
            return;
        }

        const inner = document.createElement("span");

        inner.textContent = heading.textContent.trim();

        const mask = document.createElement("span");

        mask.className = "line-mask";

        mask.appendChild(inner);

        heading.textContent = "";

        heading.appendChild(mask);

    });

}


/* ================= HERO: DỊCH CHUYỂN THEO CHUỘT ================= */

function setupHeroParallax() {

    const hero = document.querySelector(".hero");

    if (!hero || !window.matchMedia("(hover: hover) and (pointer: fine)").matches
        || (typeof prefersReducedMotion === "function" && prefersReducedMotion())) {
        return;
    }

    hero.addEventListener("mousemove", function (event) {

        const box = hero.getBoundingClientRect();

        hero.style.setProperty("--px", (((event.clientX - box.left) / box.width) * 2 - 1).toFixed(3));

        hero.style.setProperty("--py", (((event.clientY - box.top) / box.height) * 2 - 1).toFixed(3));

    });

    hero.addEventListener("mouseleave", function () {

        hero.style.removeProperty("--px");

        hero.style.removeProperty("--py");

    });

}


/* ================= DẢI THƯƠNG HIỆU CHẠY NGANG ================= */

/* Tên hãng thật (GET /brands, đang hiển thị); danh sách được nhân đôi để vòng chạy liền mạch. Lỗi / rỗng → ẩn */

let homeBrandsPromise = null;

function loadHomeBrands() {

    if (!homeBrandsPromise) {

        homeBrandsPromise = apiRequest("/brands?size=200&sort=name,asc")
            .then(function (page) {
                return (page && page.content ? page.content : []).filter(function (brand) { return brand.isActive !== false; });
            })
            .catch(function () { return null; });

    }

    return homeBrandsPromise;

}

async function renderBrandMarquee() {

    const section = document.getElementById("brandMarquee");

    const track = document.getElementById("brandMarqueeTrack");

    if (!section || !track) {
        return;
    }

    /* kế hoạch v8: chỉ các hãng có nhiều sản phẩm đang bán nhất (GET /brands/top), kèm số sản phẩm */
    let brands = null;

    try {
        brands = await apiRequest("/brands/top?size=24");
    } catch (error) {
        brands = null;
    }

    if (!brands || !brands.length) {
        return;
    }

    const items = brands.map(function (brand) {
        return `<a class="brand-marquee-item" href="${escapeHtml(siteUrl("customer/products.html?search=" + encodeURIComponent(brand.name)))}">${escapeHtml(brand.name)}<sup>${escapeHtml(String(brand.productCount))}</sup></a>`;
    }).join(`<span class="brand-marquee-dot" aria-hidden="true">✦</span>`);

    /* bản thứ hai chỉ để nối vòng, trình đọc màn hình bỏ qua */
    track.innerHTML = `<div class="brand-marquee-group">${items}<span class="brand-marquee-dot" aria-hidden="true">✦</span></div>`
        + `<div class="brand-marquee-group" aria-hidden="true">${items.replace(/<a /g, '<a tabindex="-1" ')}<span class="brand-marquee-dot">✦</span></div>`;

    section.style.setProperty("--marquee-duration", Math.max(30, brands.length * 2.6) + "s");

    section.hidden = false;

}


/* ================= FLASH SALE ================= */

/*
 * GET /promotions/flash-sale: tối đa 8 sản phẩm đang khuyến mãi + giờ kết thúc sớm nhất. Không có dữ liệu (hoặc lỗi) → khối ẩn.
 * Hết giờ → hỏi lại một lần (còn khuyến mãi khác thì đếm tiếp), vẫn hết thì ẩn. Số đổi thì "lật" nhẹ.
 */

let flashTimer = null;

let flashSalePromise = null;

function loadFlashSale() {

    flashSalePromise = apiRequest("/promotions/flash-sale").catch(function () { return null; });

    return flashSalePromise;

}

async function renderFlashSale() {

    const section = document.getElementById("flashSale");

    const grid = document.getElementById("flashSaleGrid");

    if (!section || !grid) {
        return;
    }

    clearInterval(flashTimer);

    const sale = await loadFlashSale();

    const endsAt = sale && sale.endsAt ? Date.parse(sale.endsAt) : NaN;

    if (!sale || !Array.isArray(sale.items) || !sale.items.length || isNaN(endsAt) || endsAt <= Date.now()) {

        section.hidden = true;

        updatePromoFlash(null);

        return;

    }

    const categories = await getHomeCategories();

    renderProductGrid(grid, sale.items, categories.byId);

    section.hidden = false;

    updatePromoFlash(sale);

    setupFlashNav(grid);

    const units = {};

    document.querySelectorAll("#flashCountdown [data-unit]").forEach(function (element) {
        units[element.dataset.unit] = element;
    });

    function put(unit, value) {

        if (units[unit].textContent !== value) {

            units[unit].textContent = value;

            units[unit].classList.remove("is-tick");

            void units[unit].offsetWidth;

            units[unit].classList.add("is-tick");

        }

    }

    function tick() {

        const left = Math.max(0, endsAt - Date.now());

        if (left <= 0) {

            clearInterval(flashTimer);

            section.hidden = true;

            setTimeout(renderFlashSale, 1000);

            return;

        }

        const total = Math.floor(left / 1000);

        put("d", String(Math.floor(total / 86400)));

        put("h", String(Math.floor((total % 86400) / 3600)).padStart(2, "0"));

        put("m", String(Math.floor((total % 3600) / 60)).padStart(2, "0"));

        put("s", String(total % 60).padStart(2, "0"));

    }

    tick();

    flashTimer = setInterval(tick, 1000);

}

function setupFlashNav(track) {

    const nav = document.querySelector(".flash-nav");

    /* ít sản phẩm, vừa một khung: không cần nút */
    const sync = function () {

        if (nav) {
            nav.hidden = track.scrollWidth <= track.clientWidth + 2;
        }

    };

    sync();

    window.addEventListener("resize", sync);

    document.querySelectorAll("[data-flash-step]").forEach(function (button) {

        if (button.dataset.bound) {
            return;
        }

        button.dataset.bound = "1";

        button.addEventListener("click", function () {

            const card = track.querySelector(".product-card");

            const step = card ? card.getBoundingClientRect().width + 16 : 260;

            track.scrollBy({ left: Number(button.dataset.flashStep) * step * 2, behavior: "smooth" });

        });

    });

}


/* Banner đôi: thẻ tối là lối tắt tới Flash sale khi có khuyến mãi thật, không thì giữ chữ tĩnh */

function updatePromoFlash(sale) {

    const card = document.getElementById("promoFlash");

    if (!card) {
        return;
    }

    if (!sale) {
        return;
    }

    const deepest = sale.items.reduce(function (best, item) {

        const base = Number(item.basePrice);

        const percent = base > 0 ? Math.round(100 - (Number(item.effectivePrice) / base) * 100) : 0;

        return Math.max(best, percent);

    }, 0);

    card.setAttribute("href", "#flashSale");

    document.getElementById("promoFlashTitle").textContent = "Flash sale đang diễn ra";

    document.getElementById("promoFlashText").textContent = sale.items.length + " sản phẩm đang giảm"
        + (deepest > 0 ? ", sâu nhất " + deepest + "%." : ".");

    document.getElementById("promoFlashLink").textContent = "Tới Flash sale";

}


/* ================= DANH MỤC DẠNG BENTO ================= */

/*
 * Ô đầu tiên to (2 x 2), ô thứ hai rộng (2 x 1), còn lại 1 x 1; thứ tự theo "Thứ tự hiển thị" ADMIN đặt.
 * Số sản phẩm đang bán lấy từ GET /categories/product-counts (lỗi → chỉ không hiện số).
 * Lỗi tải danh mục → giữ 4 thẻ viết sẵn trong index.html.
 */

let homeCountsPromise = null;

function loadCategoryCounts() {

    if (!homeCountsPromise) {

        homeCountsPromise = apiRequest("/categories/product-counts")
            .then(function (rows) {

                const counts = {};

                (rows || []).forEach(function (row) { counts[row.categoryId] = row.count; });

                return counts;

            })
            .catch(function () { return null; });

    }

    return homeCountsPromise;

}

async function renderHomeCategories() {

    const grid = document.querySelector(".category-grid");

    if (!grid) {
        return;
    }

    let categories;

    try {

        categories = (await loadShopCategories()).visible;

    } catch (error) {

        return;

    }

    if (categories.length === 0) {
        return;
    }

    const counts = await loadCategoryCounts();

    grid.innerHTML = categories.map(function (category, index) {

        const count = counts && counts[category.id] !== undefined ? counts[category.id] : null;

        return `
            <a href="${escapeHtml(categoryUrlOf(category))}" class="category-card" data-category-id="${escapeHtml(String(category.id))}">
                <div class="category-visual">${categoryVisualHtml(category)}</div>
                <div class="category-text">
                    <h3>${escapeHtml(category.name)}</h3>
                    ${count !== null ? `<p class="category-count">${escapeHtml(count.toLocaleString("vi-VN"))} sản phẩm</p>` : ""}
                </div>
            </a>
        `;

    }).join("");

}


/* ================= KHU TRƯNG BÀY ================= */

/*
 * 8 sản phẩm được xem nhiều nhất (GET /products sắp theo lượt xem → số đánh giá → mới nhất), một dải cuộn ngang:
 * nút ← →, kéo bằng chuột, vuốt trên cảm ứng; thanh mảnh phía trên chạy theo vị trí cuộn. Không giữ cuộn trang.
 */

const SHOWCASE_SIZE = 8;

async function renderShowcase() {

    const section = document.getElementById("showcase");

    const track = document.getElementById("showcaseTrack");

    if (!section || !track) {
        return;
    }

    let page;

    try {

        page = await apiRequest("/products?isActive=true&page=0&size=" + SHOWCASE_SIZE
            + "&sort=viewCount,desc&sort=totalReviews,desc&sort=id,desc");

    } catch (error) {

        return;

    }

    const products = page && Array.isArray(page.content) ? page.content : [];

    if (products.length < 3) {
        return;
    }

    const categories = await getHomeCategories();

    track.innerHTML = products.map(function (product, index) {

        const slug = categories.byId ? categories.byId[product.categoryId] : "";

        const image = isSafeImageUrl(product.primaryImageUrl) ? product.primaryImageUrl.trim() : getFallbackImage(slug);

        const prices = (product.variants || [])
            .map(function (variant) { return Number(variant.effectivePrice); })
            .filter(function (price) { return price > 0; });

        const price = prices.length ? Math.min.apply(null, prices) : getDisplayPrice(product);

        return `
            <a class="showcase-card" href="${escapeHtml(getProductDetailUrl(product.id))}">
                <span class="showcase-index">${String(index + 1).padStart(2, "0")}</span>
                <span class="showcase-image"><img src="${escapeHtml(image)}" alt="" loading="lazy" referrerpolicy="no-referrer"></span>
                <span class="showcase-category">${escapeHtml(product.categoryName || "")}</span>
                <span class="showcase-name">${escapeHtml(product.name)}</span>
                <span class="showcase-price">${price > 0 ? (prices.length > 1 ? "Từ " : "") + escapeHtml(formatPrice(price)) : "Liên hệ"}</span>
                <span class="showcase-more">Xem chi tiết</span>
            </a>`;

    }).join("");

    bindProductImageFallbacks(track);

    section.hidden = false;

    setupShowcaseScroller(section);

}

function setupShowcaseScroller(section) {

    const viewport = section.querySelector(".showcase-viewport");

    const progress = document.getElementById("showcaseProgress");

    const tools = section.querySelector(".showcase-tools");

    function update() {

        const max = viewport.scrollWidth - viewport.clientWidth;

        if (tools) {
            tools.hidden = max <= 2;
        }

        if (progress) {
            progress.style.transform = `scaleX(${max > 0 ? Math.max(0.08, viewport.scrollLeft / max) : 1})`;
        }

        section.querySelectorAll("[data-showcase-step]").forEach(function (button) {
            button.disabled = Number(button.dataset.showcaseStep) < 0 ? viewport.scrollLeft <= 2 : viewport.scrollLeft >= max - 2;
        });

    }

    section.querySelectorAll("[data-showcase-step]").forEach(function (button) {

        button.addEventListener("click", function () {

            const card = viewport.querySelector(".showcase-card");

            const step = card ? card.getBoundingClientRect().width + 20 : 360;

            viewport.scrollBy({ left: Number(button.dataset.showcaseStep) * step * 2, behavior: "smooth" });

        });

    });

    /* kéo bằng chuột (cảm ứng đã vuốt được sẵn); kéo xong thì không tính là bấm vào thẻ */
    let dragX = null;

    let startLeft = 0;

    let dragged = false;

    viewport.addEventListener("pointerdown", function (event) {

        if (event.pointerType !== "mouse" || event.button !== 0) {
            return;
        }

        dragX = event.clientX;

        startLeft = viewport.scrollLeft;

        dragged = false;

    });

    window.addEventListener("pointermove", function (event) {

        if (dragX === null) {
            return;
        }

        const moved = event.clientX - dragX;

        if (Math.abs(moved) > 6) {

            dragged = true;

            viewport.classList.add("is-dragging");

            viewport.scrollLeft = startLeft - moved;

        }

    });

    window.addEventListener("pointerup", function () {

        dragX = null;

        viewport.classList.remove("is-dragging");

    });

    viewport.addEventListener("click", function (event) {

        if (dragged) {
            event.preventDefault();
            dragged = false;
        }

    }, true);

    viewport.addEventListener("dragstart", function (event) {
        event.preventDefault();
    });

    viewport.addEventListener("scroll", update, { passive: true });

    window.addEventListener("resize", update);

    update();

}


/* ================= SỐ LIỆU ĐẾM TĂNG ================= */

/* Số thật: tổng sản phẩm đang bán (product-counts), thương hiệu (GET /brands), chi nhánh (GET /stores), danh mục đang hiển thị */

async function renderHomeStats() {

    const section = document.getElementById("homeStats");

    if (!section) {
        return;
    }

    const results = await Promise.all([
        loadCategoryCounts(),
        loadHomeBrands(),
        apiRequest("/stores").catch(function () { return null; }),
        getHomeCategories()
    ]);

    const values = {
        products: results[0] ? Object.values(results[0]).reduce(function (sum, count) { return sum + Number(count || 0); }, 0) : 0,
        brands: results[1] ? results[1].length : 0,
        stores: Array.isArray(results[2]) ? results[2].length : (results[2] && Array.isArray(results[2].content) ? results[2].content.length : 0),
        categories: results[3] && results[3].visible ? results[3].visible.length : 0
    };

    let shown = 0;

    section.querySelectorAll("[data-stat]").forEach(function (item) {

        const value = values[item.dataset.stat];

        if (value > 0) {

            item.hidden = false;

            item.querySelector("[data-count]").dataset.target = String(value);

            shown++;

        }

    });

    if (!shown) {
        return;
    }

    section.hidden = false;

    const numbers = Array.from(section.querySelectorAll(".stat:not([hidden]) [data-count]"));

    const reduced = typeof prefersReducedMotion === "function" && prefersReducedMotion();

    function run() {

        numbers.forEach(function (number) {

            const target = Number(number.dataset.target);

            if (reduced) {
                number.textContent = target.toLocaleString("vi-VN");
                return;
            }

            const start = performance.now();

            const duration = 1400;

            function step(now) {

                const t = Math.min(1, (now - start) / duration);

                const eased = 1 - Math.pow(1 - t, 3);

                number.textContent = Math.round(target * eased).toLocaleString("vi-VN");

                if (t < 1) {
                    requestAnimationFrame(step);
                }

            }

            requestAnimationFrame(step);

        });

    }

    if (typeof IntersectionObserver === "undefined" || reduced) {
        run();
        return;
    }

    const observer = new IntersectionObserver(function (entries) {

        if (entries.some(function (entry) { return entry.isIntersecting; })) {
            observer.disconnect();
            run();
        }

    }, { threshold: 0.4 });

    observer.observe(section);

}


/* ================= ĐÁNH GIÁ THẬT ================= */

/* GET /reviews/highlights: tối đa 6 đánh giá 4–5 sao mới nhất đang hiển thị; rỗng / lỗi → khối ẩn */

async function renderHomeReviews() {

    const section = document.getElementById("homeReviews");

    const grid = document.getElementById("homeReviewGrid");

    if (!section || !grid) {
        return;
    }

    let reviews;

    try {

        reviews = await apiRequest("/reviews/highlights");

    } catch (error) {

        return;

    }

    if (!Array.isArray(reviews) || !reviews.length) {
        return;
    }

    grid.innerHTML = reviews.map(function (review) {

        const rating = Math.max(0, Math.min(5, Number(review.rating) || 0));

        const image = isSafeImageUrl(review.productImageUrl) ? review.productImageUrl.trim() : getFallbackImage("");

        const date = new Date(review.createdAt);

        return `
            <figure class="review-card">
                <div class="review-stars" role="img" aria-label="${rating} trên 5 sao">${"★".repeat(rating)}<span aria-hidden="true">${"★".repeat(5 - rating)}</span></div>
                <blockquote>${escapeHtml(review.comment)}</blockquote>
                <a class="review-product" href="${escapeHtml(getProductDetailUrl(review.productId))}">
                    <img src="${escapeHtml(image)}" alt="" loading="lazy" referrerpolicy="no-referrer">
                    <span>${escapeHtml(review.productName)}${review.variantLabel ? ` · ${escapeHtml(review.variantLabel)}` : ""}</span>
                </a>
                <figcaption>
                    <strong>${escapeHtml(review.authorName)}</strong>
                    <span>${review.verifiedPurchase ? "Đã mua hàng · " : ""}${isNaN(date.getTime()) ? "" : escapeHtml(date.toLocaleDateString("vi-VN"))}</span>
                </figcaption>
            </figure>`;

    }).join("");

    section.hidden = false;

}


/* ================= COMBO NỔI BẬT (kế hoạch v6 B4) ================= */

/* GET /combos/featured: tối đa 3 combo đang chạy; không có (hoặc lỗi) → khối ẩn */

async function renderFeaturedCombos() {

    const section = document.getElementById("featuredCombos");

    const grid = document.getElementById("comboGrid");

    if (!section || !grid) {
        return;
    }

    let combos;

    try {

        combos = await apiRequest("/combos/featured");

    } catch (error) {

        return;

    }

    if (!Array.isArray(combos) || !combos.length) {
        return;
    }

    const picture = function (name, url) {

        const src = isSafeImageUrl(url) ? url.trim() : getFallbackImage("");

        return `<img src="${escapeHtml(src)}" alt="${escapeHtml(name)}" loading="lazy" referrerpolicy="no-referrer">`;

    };

    grid.innerHTML = combos.map(function (combo) {

        const parts = [combo.main.name].concat(combo.items.map(function (item) { return item.name; }));

        const ends = new Date(combo.endsAt);

        return `
            <a class="combo-card" href="${escapeHtml(getProductDetailUrl(combo.main.productId) + "#comboZone")}">
                <div class="combo-pics">
                    ${picture(combo.main.name, combo.main.imageUrl)}
                    ${combo.items.slice(0, 3).map(function (item) {
                        return `<span class="combo-plus" aria-hidden="true">+</span>${picture(item.name, item.imageUrl)}`;
                    }).join("")}
                </div>
                <h3 class="combo-name">${escapeHtml(combo.name)}</h3>
                <p class="combo-parts">${escapeHtml(parts.join(" + "))}</p>
                <div class="combo-price">
                    <b>${escapeHtml(formatPrice(combo.comboTotal))}</b>
                    <s>${escapeHtml(formatPrice(combo.originalTotal))}</s>
                </div>
                <span class="combo-save">Tiết kiệm ${escapeHtml(formatPrice(combo.saved))}</span>
                ${isNaN(ends.getTime()) ? "" : `<span class="combo-until">Đến hết ${escapeHtml(ends.toLocaleDateString("vi-VN"))}</span>`}
            </a>`;

    }).join("");

    section.hidden = false;

}


/* ================= SẢN PHẨM ĐÃ XEM (kế hoạch v6 B3) ================= */

/*
 * Id lưu trên trình duyệt (rememberViewedProduct, js/core/ui.js); giá / tên lấy mới từ GET /products/{id}.
 * Sản phẩm không còn bán (404 / ngừng bán) bị bỏ và xoá khỏi danh sách; không còn gì thì ẩn cả khối.
 */

async function renderRecentViewed() {

    const section = document.getElementById("recentViewed");

    const list = document.getElementById("recentList");

    const clear = document.getElementById("recentClear");

    if (!section || !list || !clear) {
        return;
    }

    const ids = readRecentViewed();

    if (!ids.length) {
        return;
    }

    const loaded = await Promise.all(ids.map(function (id) {

        return apiRequest("/products/" + id)
            .then(function (product) { return product && product.isActive !== false ? product : null; })
            .catch(function (error) { return error && error.status === 404 ? null : undefined; });

    }));

    /* undefined = lỗi mạng: giữ id, chưa vẽ; null = không còn bán: bỏ */
    const kept = ids.filter(function (id, index) { return loaded[index] !== null; });

    if (kept.length !== ids.length) {
        writeRecentViewed(kept);
    }

    const products = loaded.filter(function (product) { return product; });

    if (!products.length) {
        return;
    }

    list.innerHTML = products.map(function (product) {

        const image = isSafeImageUrl(product.primaryImageUrl) ? product.primaryImageUrl.trim() : getFallbackImage("");

        const prices = (product.variants || [])
            .map(function (variant) { return Number(variant.effectivePrice); })
            .filter(function (price) { return price > 0; });

        const price = prices.length ? Math.min.apply(null, prices) : getDisplayPrice(product);

        return `
            <div class="recent-item" data-product-id="${escapeHtml(product.id)}">
                <a class="recent-link" href="${escapeHtml(getProductDetailUrl(product.id))}">
                    <img src="${escapeHtml(image)}" alt="" loading="lazy" referrerpolicy="no-referrer">
                    <span class="recent-name">${escapeHtml(product.name)}</span>
                    <span class="recent-price">${price > 0 ? escapeHtml(formatPrice(price)) : "Liên hệ"}</span>
                </a>
                <button type="button" class="recent-remove" aria-label="Bỏ khỏi danh sách đã xem: ${escapeHtml(product.name)}">✕</button>
            </div>`;

    }).join("");

    section.hidden = false;

    list.addEventListener("click", function (event) {

        const button = event.target.closest(".recent-remove");

        if (!button) {
            return;
        }

        const item = button.closest(".recent-item");

        writeRecentViewed(readRecentViewed().filter(function (id) { return id !== Number(item.dataset.productId); }));

        item.remove();

        section.hidden = !list.children.length;

    });

    clear.addEventListener("click", function () {

        writeRecentViewed([]);

        list.innerHTML = "";

        section.hidden = true;

    });

}


/* ================= LƯỚI SẢN PHẨM TRANG CHỦ (API thật) ================= */

/*
 * Hai lưới 4 sản phẩm từ GET /products (công khai, không gửi token), vẽ bằng
 * renderProductGrid() dùng chung với trang Sản phẩm (js/core/ui.js):
 *   - "Lựa chọn hàng đầu": tab Nổi bật / Mới về / Giá tốt = sort id,asc / id,desc / basePrice,asc;
 *   - "Gợi ý dành cho bạn": 4 sản phẩm mới nhất.
 * Danh mục chỉ dùng để chọn ảnh thay thế, nên lỗi tải danh mục không chặn việc hiển thị sản phẩm.
 */

const HOME_PRODUCT_GRIDS = [
    { gridId: "featuredProductGrid", sort: "id,asc", retryId: "featuredRetryBtn" },
    { gridId: "personalRecommendationGrid", sort: "id,desc", retryId: "newestRetryBtn" }
];

let homeCategoriesPromise = null;

function getHomeCategories() {

    if (!homeCategoriesPromise) {

        homeCategoriesPromise = loadShopCategories().catch(function () {

            homeCategoriesPromise = null;

            return { byId: {}, bySlug: {}, visible: [] };

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


/* ================= MUA THEO NHU CẦU ================= */

/* Mỗi thẻ là một bộ lọc "nhu cầu" có thật của danh mục (GET /categories/{id}/filters); nhu cầu không còn sản phẩm thì không hiện */

const HOME_NEEDS = [
    { slug: "dien-thoai", need: "chup-anh-quay-phim", icon: "<path d=\"M4 8h3l2-3h6l2 3h3v11H4Z\"/><circle cx=\"12\" cy=\"13\" r=\"3.500\"/>" },
    { slug: "dien-thoai", need: "choi-game", icon: "<rect x=\"2\" y=\"7\" width=\"20\" height=\"11\" rx=\"4\"/><path d=\"M7 10v5M4.500 12.500h5M16 11.500h.01M18.500 13.500h.01\"/>" },
    { slug: "dien-thoai", need: "pin-khung", icon: "<rect x=\"3\" y=\"7\" width=\"16\" height=\"10\" rx=\"2\"/><path d=\"M21 11v2M11 9l-2 3h4l-2 3\"/>" },
    { slug: "laptop", need: "hoc-tap-van-phong", icon: "<rect x=\"4\" y=\"5\" width=\"16\" height=\"11\" rx=\"1.500\"/><path d=\"M2 20h20\"/>" },
    { slug: "laptop", need: "gaming", icon: "<rect x=\"2\" y=\"7\" width=\"20\" height=\"11\" rx=\"4\"/><path d=\"M7 10v5M4.500 12.500h5M16 11.500h.01M18.500 13.500h.01\"/>" },
    { slug: "laptop", need: "do-hoa", icon: "<path d=\"M4 20l4-1L19 8a2.100 2.100 0 0 0-3-3L5 16Z\"/>" }
];

async function renderHomeNeeds() {

    const section = document.getElementById("homeNeeds");

    if (!section) {
        return;
    }

    const categories = await getHomeCategories();

    const filters = {};

    await Promise.all(Array.from(new Set(HOME_NEEDS.map(function (item) { return item.slug; }))).map(async function (slug) {

        const category = categories.visible.find(function (item) { return item.slug === slug; });

        if (!category) {
            return;
        }

        try {
            filters[slug] = { category: category, groups: await apiRequest("/categories/" + category.id + "/filters") || [] };
        } catch (error) {
            /* thiếu dữ liệu → bỏ các thẻ của danh mục này */
        }

    }));

    const cards = HOME_NEEDS.map(function (item) {

        const entry = filters[item.slug];

        const group = entry && entry.groups.find(function (g) { return g.key === "need"; });

        const value = group && group.values.find(function (v) { return v.value === item.need; });

        return value && value.count > 0 ? { item: item, entry: entry, value: value } : null;

    }).filter(Boolean);

    if (cards.length < 3) {
        return;
    }

    document.getElementById("needsGrid").innerHTML = cards.map(function (card) {

        const href = "customer/products.html?category=" + encodeURIComponent(categoryKeyOf(card.item.slug)) +
            "&f=" + encodeURIComponent("need:" + card.item.need);

        return `<a class="need-card" href="${escapeHtml(href)}">
            <svg class="need-icon" viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${card.item.icon}</svg>
            <strong>${escapeHtml(card.value.label)}</strong>
            <span>${escapeHtml(card.entry.category.name)} · ${card.value.count.toLocaleString("vi-VN")} sản phẩm</span>
        </a>`;

    }).join("");

    section.hidden = false;

}
