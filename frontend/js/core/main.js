document.addEventListener("DOMContentLoaded", async function () {

    setupAddToCart();


    /* Header được layout.js chèn vào sau: chờ xong mới cập nhật */

    if (typeof layoutReady !== "undefined") {
        await layoutReady;
    }

    renderAuthState();

    renderMegaMenuCategories();

    migrateLegacyCart();

    /* Gọi GET /cart (sau khi chuyển giỏ cũ trên trình duyệt lên server) */
    updateCartCount();

    setupHeaderSearch();

    setupSearchSuggestions();

    setupHeaderScrollEffect();

    setupScrollReveal();

});


/* ================= MEGA-MENU DANH MỤC (kế hoạch v6 B2) ================= */

/*
 * Panel "Sản phẩm" (desktop) và ngăn kéo (điện thoại) dùng chung dữ liệu: mỗi danh mục tải MỘT lần rồi giữ trong
 * bộ nhớ — Hãng GET /categories/{id}/brands, Nhu cầu GET /categories/{id}/needs, HOT = GET /products sắp theo lượt xem.
 * Khối Mức giá là các bậc cố định theo loại danh mục. Lỗi tải danh mục → panel không có hàng danh mục (chỉ còn link đáy).
 */

const MEGA_MENU_MAX_CATEGORIES = 12;

const MEGA_HOT_SIZE = 10;

const MEGA_NEW_DAYS = 30;

const MEGA_HOVER_DELAY = 110;

const MEGA_BRAND_LIMIT = 16;

/* Mốc giá (đồng) chia khoảng; khoá theo categoryArtKey() */
const MEGA_PRICE_STEPS = {
    phone: [5e6, 10e6, 20e6],
    tablet: [5e6, 10e6, 20e6],
    laptop: [15e6, 25e6, 35e6],
    monitor: [3e6, 6e6, 12e6],
    desktop: [10e6, 20e6, 35e6],
    smartwatch: [2e6, 5e6, 10e6],
    watch: [1e6, 3e6, 6e6],
    accessory: [200e3, 500e3, 1e6, 3e6],
    other: [1e6, 5e6, 20e6]
};

const megaDataCache = new Map();


function megaMoneyLabel(value) {

    return value >= 1e6
        ? (value / 1e6).toLocaleString("vi-VN") + " triệu"
        : (value / 1e3).toLocaleString("vi-VN") + " nghìn";

}

function megaPriceRanges(category) {

    const steps = MEGA_PRICE_STEPS[categoryArtKey(category.slug)] || MEGA_PRICE_STEPS.other;

    const ranges = [];

    let previous = null;

    steps.forEach(function (step) {

        ranges.push({
            min: previous,
            max: step,
            label: previous === null ? "Dưới " + megaMoneyLabel(step) : "Từ " + megaMoneyLabel(previous) + " - " + megaMoneyLabel(step)
        });

        previous = step;

    });

    ranges.push({ min: previous, max: null, label: "Trên " + megaMoneyLabel(previous) });

    return ranges;

}

function megaDataFor(category) {

    if (!megaDataCache.has(category.id)) {

        const id = encodeURIComponent(category.id);

        const quiet = function (promise) {
            return promise.catch(function () { return null; });
        };

        const promise = Promise.all([
            quiet(apiRequest("/categories/" + id + "/brands")),
            quiet(apiRequest("/products?isActive=true&categoryId=" + id + "&size=" + MEGA_HOT_SIZE
                + "&sort=viewCount,desc&sort=totalReviews,desc&sort=id,desc")),
            quiet(apiRequest("/categories/" + id + "/needs"))
        ]).then(function (results) {

            if (results.every(function (item) { return item === null; })) {

                megaDataCache.delete(category.id);

                throw new Error("mega-menu");

            }

            return {
                brands: Array.isArray(results[0]) ? results[0] : [],
                hot: results[1] && Array.isArray(results[1].content) ? results[1].content : [],
                needs: (Array.isArray(results[2]) ? results[2] : []).filter(function (need) { return need.productCount > 0; })
            };

        });

        megaDataCache.set(category.id, promise);

    }

    return megaDataCache.get(category.id);

}

/* "Hot" = đang khuyến mãi; "Mới" = thêm trong MEGA_NEW_DAYS ngày */

function megaProductTag(product) {

    if (product.activePromotionName || Number(product.basePrice) > Number(product.effectivePrice)) {
        return { text: "Hot", className: "is-hot" };
    }

    const created = Date.parse(product.createdAt);

    if (!isNaN(created) && Date.now() - created < MEGA_NEW_DAYS * 86400000) {
        return { text: "Mới", className: "is-new" };
    }

    return null;

}

function megaProductPrice(product) {

    const prices = (product.variants || [])
        .map(function (variant) { return Number(variant.effectivePrice); })
        .filter(function (price) { return price > 0; });

    return prices.length ? Math.min.apply(null, prices) : getDisplayPrice(product);

}

function megaHotHtml(category, products) {

    if (!products.length) {
        return `<p class="mega-empty">Chưa có sản phẩm.</p>`;
    }

    return `<ul class="mega-hot-list">${products.map(function (product) {

        const image = isSafeImageUrl(product.primaryImageUrl) ? product.primaryImageUrl.trim() : getFallbackImage(category.slug);

        const tag = megaProductTag(product);

        const price = megaProductPrice(product);

        return `
            <li>
                <a class="mega-hot-item" href="${escapeHtml(getProductDetailUrl(product.id))}">
                    <img src="${escapeHtml(image)}" alt="" referrerpolicy="no-referrer">
                    <span class="mega-hot-text">
                        <span class="mega-hot-name">${escapeHtml(product.name)}</span>
                        <span class="mega-hot-price">${price > 0 ? escapeHtml(formatPrice(price)) : "Liên hệ"}</span>
                    </span>
                    ${tag ? `<span class="mega-tag ${tag.className}">${tag.text}</span>` : ""}
                </a>
            </li>`;

    }).join("")}</ul>`;

}

/* Bốn khối cho một danh mục; compact = ngăn kéo điện thoại (HOT chỉ lấy 6, danh mục không cần tiêu đề lớn) */

function megaBlocksHtml(category, data, compact) {

    const base = categoryUrlOf(category);

    const brands = data.brands.slice(0, MEGA_BRAND_LIMIT).map(function (brand) {
        return `<a class="mega-chip mega-brand" href="${escapeHtml(base + "&brand=" + encodeURIComponent(brand.id))}">${escapeHtml(brand.name)}</a>`;
    }).join("");

    const prices = megaPriceRanges(category).map(function (range) {

        let query = "";

        if (range.min !== null) {
            query += "&min=" + range.min;
        }

        if (range.max !== null) {
            query += "&max=" + range.max;
        }

        return `<a class="mega-chip" href="${escapeHtml(base + query)}">${escapeHtml(range.label)}</a>`;

    }).join("");

    const needs = data.needs.map(function (need) {
        return `<a class="mega-chip" href="${escapeHtml(base + "&f=" + encodeURIComponent("need:" + need.slug))}">${escapeHtml(need.name)}</a>`;
    }).join("");

    const hot = compact ? data.hot.slice(0, 6) : data.hot;

    return `
        ${brands ? `<section class="mega-block mega-block--brands"><h4>Hãng</h4><div class="mega-chips">${brands}</div></section>` : ""}
        <section class="mega-block mega-block--hot"><h4>Sản phẩm HOT</h4>${megaHotHtml(category, hot)}</section>
        <section class="mega-block mega-block--prices"><h4>Mức giá</h4><div class="mega-chips">${prices}</div></section>
        ${needs ? `<section class="mega-block mega-block--needs"><h4>Nhu cầu</h4><div class="mega-chips">${needs}</div></section>` : ""}
    `;

}


async function renderMegaMenuCategories() {

    const menu = document.getElementById("megaMenu");

    if (!menu || typeof loadShopCategories !== "function") {
        return;
    }

    let categories;

    try {

        categories = (await loadShopCategories()).visible.slice(0, MEGA_MENU_MAX_CATEGORIES);

    } catch (error) {

        return;

    }

    if (!categories.length) {
        return;
    }

    setupMegaPanel(menu, categories);

    setupMegaDrawer(document.getElementById("navCats"), categories);

}

function setupMegaPanel(menu, categories) {

    const row = menu.querySelector(".mega-cats");

    const body = menu.querySelector(".mega-body");

    row.innerHTML = categories.map(function (category) {
        return `
            <a class="mega-cat" href="${escapeHtml(categoryUrlOf(category))}" data-category-id="${escapeHtml(category.id)}">
                <span class="mega-cat-icon">${categoryVisualHtml(category)}</span>
                <span class="mega-cat-name">${escapeHtml(category.name)}</span>
            </a>`;
    }).join("");

    const links = Array.from(row.querySelectorAll(".mega-cat"));

    let activeId = null;

    let requestId = 0;

    let hoverTimer = null;


    function activate(category) {

        if (activeId === category.id) {
            return;
        }

        activeId = category.id;

        links.forEach(function (link) {

            const active = link.dataset.categoryId === String(category.id);

            link.classList.toggle("is-active", active);

            if (active) {
                link.setAttribute("aria-current", "true");
            } else {
                link.removeAttribute("aria-current");
            }

        });

        const mine = ++requestId;

        body.classList.add("is-loading");

        megaDataFor(category).then(function (data) {

            if (mine === requestId) {

                body.innerHTML = megaBlocksHtml(category, data, false);

                body.classList.remove("is-loading");

            }

        }).catch(function () {

            if (mine === requestId) {

                activeId = null;

                body.innerHTML = `<p class="mega-empty">Chưa tải được danh mục này, thử lại sau.</p>`;

                body.classList.remove("is-loading");

            }

        });

    }

    function activateFirst() {

        if (activeId === null) {
            activate(categories[0]);
        }

    }


    links.forEach(function (link, index) {

        const category = categories[index];

        link.addEventListener("mouseenter", function () {

            clearTimeout(hoverTimer);

            /* trễ ngắn: đi chéo qua các thẻ không làm bảng nháy liên tục */
            hoverTimer = setTimeout(function () { activate(category); }, activeId === null ? 0 : MEGA_HOVER_DELAY);

        });

        link.addEventListener("mouseleave", function () {
            clearTimeout(hoverTimer);
        });

        link.addEventListener("focus", function () {
            activate(category);
        });

        link.addEventListener("keydown", function (event) {

            const step = event.key === "ArrowRight" ? 1 : event.key === "ArrowLeft" ? -1 : 0;

            if (step) {

                event.preventDefault();

                links[(index + step + links.length) % links.length].focus();

            }

        });

    });


    /* Mở panel bằng chuột / bàn phím → tải sẵn danh mục đầu; Tab từ nút mũi tên đi thẳng vào hàng danh mục */

    const trigger = document.querySelector(".header .nav-has-mega");

    const toggle = document.querySelector(".header .mega-toggle");

    [trigger, toggle, menu].forEach(function (element) {

        if (element) {

            element.addEventListener("mouseenter", activateFirst);

            element.addEventListener("focus", activateFirst);

        }

    });

    if (toggle) {

        toggle.addEventListener("click", activateFirst);

        toggle.addEventListener("keydown", function (event) {

            if (event.key === "Tab" && !event.shiftKey && !menu.hidden) {

                event.preventDefault();

                (row.querySelector(".mega-cat.is-active") || links[0]).focus();

            }

        });

    }

}

function setupMegaDrawer(container, categories) {

    if (!container) {
        return;
    }

    container.innerHTML = categories.map(function (category) {
        return `
            <div class="nav-cat" data-category-id="${escapeHtml(category.id)}">
                <button type="button" class="nav-cat-head" aria-expanded="false">
                    <span class="mega-cat-icon">${categoryVisualHtml(category)}</span>
                    <span class="nav-cat-name">${escapeHtml(category.name)}</span>
                    <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="M6 9l6 6 6-6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </button>
                <div class="nav-cat-panel" hidden></div>
            </div>`;
    }).join("");

    const items = Array.from(container.querySelectorAll(".nav-cat"));

    items.forEach(function (item, index) {

        const category = categories[index];

        const head = item.querySelector(".nav-cat-head");

        const panel = item.querySelector(".nav-cat-panel");

        head.addEventListener("click", function () {

            const open = head.getAttribute("aria-expanded") !== "true";

            items.forEach(function (other) {

                other.querySelector(".nav-cat-head").setAttribute("aria-expanded", "false");

                other.querySelector(".nav-cat-panel").hidden = true;

            });

            if (!open) {
                return;
            }

            head.setAttribute("aria-expanded", "true");

            panel.hidden = false;

            if (panel.dataset.loaded === "1") {
                return;
            }

            panel.innerHTML = `<p class="mega-empty">Đang tải...</p>`;

            megaDataFor(category).then(function (data) {

                panel.innerHTML = megaBlocksHtml(category, data, true)
                    + `<a class="nav-cat-all" href="${escapeHtml(categoryUrlOf(category))}">Xem tất cả ${escapeHtml(category.name)}</a>`;

                panel.dataset.loaded = "1";

            }).catch(function () {

                panel.innerHTML = `<p class="mega-empty">Chưa tải được danh mục này, thử lại sau.</p>`;

            });

        });

    });

}


/* ================= TÀI KHOẢN (HEADER) ================= */

/*
 * Đã đăng nhập: nút "Đăng nhập" trên header hiển thị tên người dùng,
 * kèm nút "Đăng xuất". Cần nạp js/core/api.js trước main.js.
 */

function renderAuthState() {

    if (typeof isLoggedIn !== "function" || !isLoggedIn()) {
        return;
    }


    /*
     * Đã đăng nhập: thay nút "Đăng nhập" (.login-btn, giữ nguyên cho trạng thái chưa đăng nhập) bằng MỘT nút
     * ảnh đại diện mở menu tài khoản. Gọi lại được (vd. vừa đổi ảnh / tên ở trang Tài khoản): menu cũ được thay.
     */

    const user = getCurrentUser() || {};

    document.querySelectorAll(".header .login-btn, .header .user-menu")
        .forEach(function (element) {

            const template = document.createElement("template");

            template.innerHTML = userMenuHtml(user).trim();

            const menu = template.content.firstElementChild;

            element.replaceWith(menu);

            bindUserMenu(menu);

        });

}


/* Chữ cái đầu của họ và tên: "Nguyễn Thị Lan" → "NL" */

function userInitials(name) {

    const words = String(name || "").trim().split(/\s+/).filter(Boolean);

    if (words.length === 0) {
        return "?";
    }

    return (words[0].charAt(0) + (words.length > 1 ? words[words.length - 1].charAt(0) : "")).toUpperCase();

}


function userAvatarHtml(user, className) {

    const name = user.fullname || user.username || "";

    const initials = `<span class="${className} ${className}--initials" aria-hidden="true">${escapeHtml(userInitials(name))}</span>`;

    if (user.avatarUrl && isSafeImageUrl(user.avatarUrl)) {

        /* ảnh hỏng → về chữ cái đầu (main.js gắn sự kiện error) */
        return `<img class="${className}" src="${escapeHtml(user.avatarUrl)}" alt="" data-initials="${escapeHtml(userInitials(name))}">`;

    }

    return initials;

}


function userMenuHtml(user) {

    const name = user.fullname || user.username || "Tài khoản";

    return `
        <div class="user-menu">
            <button type="button" class="user-menu-toggle" aria-haspopup="true" aria-expanded="false"
                aria-controls="userMenuPanel" aria-label="${escapeHtml("Tài khoản: " + name)}" title="${escapeHtml(user.email || name)}">
                ${userAvatarHtml(user, "user-avatar")}
            </button>
            <div class="user-menu-panel" id="userMenuPanel" hidden>
                <div class="user-menu-head">
                    ${userAvatarHtml(user, "user-avatar")}
                    <span>
                        <strong class="user-menu-name">${escapeHtml(name)}</strong>
                        <span class="user-menu-email">${escapeHtml(user.email || "")}</span>
                    </span>
                </div>
                <a href="${escapeHtml(siteUrl("customer/account.html"))}">Tài khoản của tôi</a>
                <a href="${escapeHtml(siteUrl("customer/orders.html"))}">Đơn hàng</a>
                <a href="${escapeHtml(siteUrl("customer/service-requests.html"))}">Yêu cầu dịch vụ</a>
                <button type="button" class="logout-btn">Đăng xuất</button>
            </div>
        </div>
    `;

}


function bindUserMenu(menu) {

    const toggle = menu.querySelector(".user-menu-toggle");

    const panel = menu.querySelector(".user-menu-panel");


    menu.querySelectorAll("img.user-avatar").forEach(function (img) {

        img.addEventListener("error", function () {

            const fallback = document.createElement("span");

            fallback.className = "user-avatar user-avatar--initials";

            fallback.setAttribute("aria-hidden", "true");

            fallback.textContent = img.dataset.initials || "?";

            img.replaceWith(fallback);

        });

    });


    function setOpen(open) {

        panel.hidden = !open;

        toggle.setAttribute("aria-expanded", String(open));

    }


    toggle.addEventListener("click", function (event) {

        event.stopPropagation();

        setOpen(panel.hidden);

    });

    /* bấm ra ngoài hoặc Esc thì đóng */
    document.addEventListener("click", function (event) {

        if (!panel.hidden && !menu.contains(event.target)) {
            setOpen(false);
        }

    });

    document.addEventListener("keydown", function (event) {

        if (event.key === "Escape" && !panel.hidden) {

            setOpen(false);

            toggle.focus();

        }

    });


    menu.querySelector(".logout-btn").addEventListener("click", async function () {

        await logout();

        window.location.reload();

    });

}


/* ================= TÌM KIẾM (HEADER) ================= */

/*
 * Ô tìm kiếm trên header (partials/header.html): submit sẽ chuyển tới trang
 * Sản phẩm kèm ?search=..., products.js gửi giá trị này lên API dưới dạng
 * keyword. Gọi sau khi layout.js đã chèn header.
 */

function setupHeaderSearch() {

    document.querySelectorAll(".header .search-form")
        .forEach(function (form) {

            form.addEventListener("submit", function (event) {

                event.preventDefault();

                const input = form.querySelector("input");

                const keyword = input ? input.value.trim() : "";

                rememberSearch(keyword);


                window.location.href =
                    siteUrl("customer/products.html") +
                    (keyword ? "?search=" + encodeURIComponent(keyword) : "");

            });

        });

}


/* ================= GỢI Ý DƯỚI Ô TÌM KIẾM (kế hoạch v5 GĐ3) ================= */

/*
 * Ô trống: "Tìm gần đây" (5 từ khoá cuối, localStorage poy_recent_searches) + "Tìm nhiều" (chip trong header).
 * Gõ 1 ký tự: lọc 2 nhóm chip. Gõ ≥ 2 ký tự: chờ người dùng ngừng gõ rồi hỏi GET /products/suggest
 * (công khai, không gửi token) → tối đa 6 sản phẩm có ảnh + giá + "Xem tất cả". Kết quả cũ về muộn bị bỏ.
 */

const RECENT_SEARCH_KEY = "poy_recent_searches";

const RECENT_SEARCH_MAX = 5;

const SEARCH_SUGGEST_DELAY = 250;

const SEARCH_SUGGEST_MIN_LENGTH = 2;


function readRecentSearches() {

    try {

        const list = JSON.parse(localStorage.getItem(RECENT_SEARCH_KEY) || "[]");

        return Array.isArray(list)
            ? list.filter(function (item) { return typeof item === "string" && item.trim(); }).slice(0, RECENT_SEARCH_MAX)
            : [];

    } catch (error) {

        return [];

    }

}


function rememberSearch(keyword) {

    const text = String(keyword || "").trim().slice(0, 100);

    if (!text) {
        return;
    }

    const list = readRecentSearches().filter(function (item) {
        return item.toLowerCase() !== text.toLowerCase();
    });

    list.unshift(text);

    try {
        localStorage.setItem(RECENT_SEARCH_KEY, JSON.stringify(list.slice(0, RECENT_SEARCH_MAX)));
    } catch (error) {
        /* không lưu được (chế độ riêng tư…): bỏ qua */
    }

}


function foldSearchText(text) {

    return String(text || "")
        .normalize("NFD")
        .replace(/[̀-ͯ]/g, "")
        .replace(/đ/g, "d")
        .replace(/Đ/g, "D")
        .toLowerCase()
        .trim();

}


function searchResultHtml(item) {

    const image = isSafeImageUrl(item.imageUrl) ? item.imageUrl.trim() : getFallbackImage("");

    const price = Number(item.price) || 0;

    const original = Number(item.originalPrice) || 0;

    return `
        <a class="search-result" role="option" href="${escapeHtml(getProductDetailUrl(item.id))}" data-search-result>
            <img src="${escapeHtml(image)}" alt="" referrerpolicy="no-referrer" loading="lazy">
            <span class="search-result-info">
                <span class="search-result-name">${escapeHtml(item.name)}</span>
                <span class="search-result-price">
                    ${price > 0 ? escapeHtml(formatPrice(price)) : "Liên hệ"}
                    ${original > price && price > 0 ? `<s>${escapeHtml(formatPrice(original))}</s>` : ""}
                    ${item.inStock ? "" : `<em class="search-result-out">Tạm hết hàng</em>`}
                </span>
            </span>
        </a>
    `;

}


function setupSearchSuggestions() {

    document.querySelectorAll(".header .search-form").forEach(function (form) {

        const input = form.querySelector("input");

        const panel = form.querySelector(".search-suggest");

        if (!input || !panel) {
            return;
        }

        const recentBox = panel.querySelector("[data-suggest-recent]");

        const recentList = panel.querySelector("[data-recent-list]");

        const popularBox = panel.querySelector("[data-suggest-popular]");

        const popularChips = Array.from(popularBox.querySelectorAll("[data-suggest]"));

        const results = panel.querySelector("[data-suggest-results]");

        let timer = null;

        let requestNo = 0;


        function chipsFor(term) {

            const recent = readRecentSearches().filter(function (item) {
                return !term || foldSearchText(item).indexOf(term) !== -1;
            });

            recentList.innerHTML = recent.map(function (item) {
                return `<button type="button" data-suggest="${escapeHtml(item)}">${escapeHtml(item)}</button>`;
            }).join("");

            recentBox.hidden = recent.length === 0;

            let popularShown = 0;

            popularChips.forEach(function (chip) {
                const match = !term || foldSearchText(chip.dataset.suggest).indexOf(term) !== -1;
                chip.hidden = !match;
                popularShown += match ? 1 : 0;
            });

            popularBox.hidden = popularShown === 0;

            results.hidden = true;

            panel.hidden = recent.length === 0 && popularShown === 0;

        }


        function showResultsBox(html) {

            recentBox.hidden = true;

            popularBox.hidden = true;

            results.innerHTML = html;

            results.hidden = false;

            panel.hidden = false;

        }


        async function loadResults(query) {

            const current = ++requestNo;

            showResultsBox(`<p class="search-suggest-status">Đang tìm…</p>`);

            let data;

            try {

                data = await apiRequest("/products/suggest?" + new URLSearchParams({ q: query, size: 6 }).toString());

            } catch (error) {

                if (current === requestNo) {
                    showResultsBox(`<p class="search-suggest-status">${escapeHtml(getErrorMessage(error))}</p>`);
                }

                return;

            }

            /* đã gõ tiếp / xoá ô trong lúc chờ */
            if (current !== requestNo || input.value.trim() !== query) {
                return;
            }

            const items = data && Array.isArray(data.items) ? data.items : [];

            if (items.length === 0) {

                showResultsBox(`<p class="search-suggest-status">Không tìm thấy sản phẩm nào cho “${escapeHtml(query)}”.</p>`);

                return;

            }

            const allUrl = siteUrl("customer/products.html") + "?search=" + encodeURIComponent(query);

            showResultsBox(
                `<div role="listbox" aria-label="Sản phẩm gợi ý">${items.map(searchResultHtml).join("")}</div>`
                + `<a class="search-suggest-all" href="${escapeHtml(allUrl)}" data-search-all>`
                + `Xem tất cả ${escapeHtml(String(data.total))} kết quả cho “${escapeHtml(query)}” ›</a>`
            );

        }


        function refresh() {

            clearTimeout(timer);

            const query = input.value.trim();

            if (query.length < SEARCH_SUGGEST_MIN_LENGTH) {

                requestNo += 1;

                chipsFor(foldSearchText(query));

                return;

            }

            timer = setTimeout(function () { loadResults(query); }, SEARCH_SUGGEST_DELAY);

        }


        function focusables() {

            return Array.from(panel.querySelectorAll("[data-suggest], [data-search-result], [data-search-all]"))
                .filter(function (element) { return element.offsetParent !== null; });

        }


        input.addEventListener("focus", refresh);

        input.addEventListener("input", refresh);

        input.addEventListener("keydown", function (event) {

            if (event.key === "ArrowDown" && !panel.hidden) {

                event.preventDefault();

                const first = focusables()[0];

                if (first) {
                    first.focus();
                }

            }

            if (event.key === "Escape") {
                panel.hidden = true;
            }

        });

        panel.addEventListener("keydown", function (event) {

            const items = focusables();

            const index = items.indexOf(document.activeElement);

            if (event.key === "ArrowDown" || event.key === "ArrowRight") {
                event.preventDefault();
                (items[index + 1] || items[0]).focus();
            }

            if (event.key === "ArrowUp" || event.key === "ArrowLeft") {
                event.preventDefault();
                (items[index - 1] || input).focus();
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

        panel.addEventListener("click", function (event) {

            if (event.target.closest("[data-clear-recent]")) {

                try {
                    localStorage.removeItem(RECENT_SEARCH_KEY);
                } catch (error) {
                    /* bỏ qua */
                }

                chipsFor(foldSearchText(input.value.trim()));

                input.focus();

                return;

            }

            const chip = event.target.closest("[data-suggest]");

            if (chip) {

                input.value = chip.dataset.suggest;

                panel.hidden = true;

                form.requestSubmit();

                return;

            }

            /* bấm sản phẩm / "Xem tất cả": nhớ từ khoá rồi để link tự chuyển trang */
            if (event.target.closest("[data-search-result], [data-search-all]")) {
                rememberSearch(input.value);
            }

        });

    });

}


/* ================= HIỆU ỨNG "KÍNH MỜ" KHI CUỘN (HEADER) ================= */

/*
 * Thêm class .is-scrolled cho .header khi cuộn xuống quá 24px; style.css lo
 * phần nền mờ / đổ bóng. { passive: true } vì chỉ đọc scrollY, không chặn cuộn.
 */

function setupHeaderScrollEffect() {

    const header = document.querySelector(".header");

    if (!header) {
        return;
    }


    function updateHeaderState() {

        header.classList.toggle("is-scrolled", window.scrollY > 24);

    }


    updateHeaderState();

    window.addEventListener("scroll", updateHeaderState, { passive: true });

}


/* ================= SCROLL REVEAL (TIÊU ĐỀ SECTION + LƯỚI TĨNH) ================= */

/*
 * Gắn .reveal-on-scroll (style.css) cho các khối lặp lại ở nhiều trang, rồi
 * dùng IntersectionObserver để hiện một lần khi cuộn tới. Lưới sản phẩm
 * render bằng JS đã có hiệu ứng riêng (staggerRevealCards trong ui.js).
 */

const SCROLL_REVEAL_SELECTOR =
    ".section-heading, .category-grid, .service-grid, .benefits-grid, .footer-grid, .promo-grid, .review-grid, .combo-grid";


function setupScrollReveal() {

    if (typeof prefersReducedMotion === "function" && prefersReducedMotion()) {
        return;
    }


    const targets = document.querySelectorAll(SCROLL_REVEAL_SELECTOR);

    if (targets.length === 0 || typeof IntersectionObserver === "undefined") {
        return;
    }


    const observer = new IntersectionObserver(
        function (entries) {

            entries.forEach(function (entry) {

                if (entry.isIntersecting) {

                    entry.target.classList.add("is-visible");

                    observer.unobserve(entry.target);

                }

            });

        },
        { threshold: 0.15, rootMargin: "0px 0px -60px 0px" }
    );


    targets.forEach(function (target) {

        target.classList.add("reveal-on-scroll");

        observer.observe(target);

    });

}


/* ================= CART (Phase 3) ================= */

/*
 * Lưu trữ giỏ hàng: js/core/cart-store.js (giỏ trên server, /api/v1/cart).
 * File này lo phần giao diện: nút "Thêm vào giỏ", bắt buộc đăng nhập,
 * hiệu ứng bay vào giỏ, toast.
 */

/*
 * Gắn "Thêm vào giỏ" cho các nút .add-cart trong root (mặc định: cả trang).
 * Thẻ sản phẩm render sau (products.js, home.js) gọi lại với lưới mới;
 * cờ data-cart-bound tránh gắn sự kiện 2 lần cho cùng một nút.
 * Thẻ không biết phiên bản, nên lúc bấm mới lấy GET /products/{id}/variants
 * và chọn phiên bản đầu tiên còn hàng (kế hoạch v3 GĐ E; không còn bản nào → báo hết hàng).
 */

function setupAddToCart(root) {

    const scope = root || document;

    const buttons = scope.querySelectorAll(".add-cart");

    buttons.forEach(function (button) {

        if (button.dataset.cartBound === "1") {
            return;
        }

        button.dataset.cartBound = "1";


        button.addEventListener("click", async function () {

            const card = button.closest(".product-card");

            const productId = card ? card.dataset.productId : null;

            if (!productId) {
                return;
            }


            /* Chưa đăng nhập: chuyển trang ngay, không cần gọi API */

            if (!isLoggedIn()) {

                redirectToLogin("cart");

                return;

            }


            const image = card.querySelector(".product-image img");

            button.disabled = true;


            try {

                const variants = await apiRequest(
                    "/products/" + encodeURIComponent(productId) + "/variants"
                );

                const list = Array.isArray(variants) ? variants : [];

                /* phiên bản chọn trên thẻ (nút phiên bản) nếu có, không thì bản đầu tiên còn hàng */
                const chosen = card.dataset.variantId
                    ? list.find(function (item) { return String(item.id) === card.dataset.variantId; })
                    : null;

                const variant = chosen || list.find(function (item) { return !isSoldOut(item); }) || list[0] || null;

                if (variant && isSoldOut(variant)) {

                    showToast("Sản phẩm tạm hết hàng ở mọi chi nhánh.", "error");

                    return;

                }

                const prices = variant
                    ? resolvePrices({ basePrice: button.dataset.price }, variant)
                    : { price: Number(button.dataset.price) };


                if (!(prices.price > 0)) {

                    showToast("Sản phẩm chưa có giá bán, vui lòng liên hệ để đặt hàng.", "error");

                    return;

                }


                await addToCart(
                    {
                        productId: Number(productId),
                        variantId: variant ? variant.id : null,
                        name: button.dataset.name,
                        variantLabel: variant ? variantLabelOf(variant) : "",
                        price: prices.price,
                        image: image && image.getAttribute("src") ? image.currentSrc || image.src : null
                    },
                    1,
                    image
                );

            } catch (error) {

                showToast(getErrorMessage(error), "error");

            } finally {

                button.disabled = false;

            }

        });

    });

}


/*
 * addToCart(snapshot, quantity?, sourceElement?) → Promise<true> nếu đã thêm.
 * - snapshot: { productId, variantId, name, variantLabel } — server chỉ cần
 *   variantId; tên / phiên bản dùng cho toast. Giá do server tính.
 * - Chưa đăng nhập (hoặc phiên hết hạn): không thêm, chuyển tới trang đăng nhập
 *   (?reason=cart), đăng nhập xong quay lại đúng trang đang xem → false.
 * - quantity: tuỳ chọn (trang chi tiết sản phẩm), mặc định 1.
 * - sourceElement: ảnh sản phẩm để chạy hiệu ứng bay vào giỏ (tuỳ chọn).
 */

async function addToCart(snapshot, quantity, sourceElement) {

    if (!isLoggedIn()) {

        redirectToLogin("cart");

        return false;

    }


    if (snapshot.variantId === undefined || snapshot.variantId === null) {

        showToast("Sản phẩm này hiện chưa thể đặt mua trực tuyến.", "error");

        return false;

    }


    const amount =
        Number.isInteger(quantity) && quantity > 0 ? quantity : 1;

    let summary;

    try {

        summary = await addCartItem(snapshot, amount);

    } catch (error) {

        /* refresh thất bại → api.js đã xoá phiên: mời đăng nhập lại */
        if (!isLoggedIn()) {

            redirectToLogin("cart");

            return false;

        }

        showToast(getErrorMessage(error), "error");

        return false;

    }


    const line = summary.items.find(function (item) {
        return String(item.variantId) === String(snapshot.variantId);
    });

    const lineQuantity = line ? line.quantity : amount;


    /* Số trên icon giỏ hàng tăng khi sản phẩm "bay" tới nơi */

    flyToCart(sourceElement, function () {

        setCartCount(summary.totalQuantity);

        bumpCartIcon();

    });


    /* Toast thay cho alert() (js/core/ui.js) */

    const label = snapshot.name + (snapshot.variantLabel ? " (" + snapshot.variantLabel + ")" : "");

    showToast(
        (amount > 1 ? "Đã thêm " + amount + " × " : "Đã thêm ") + label + " vào giỏ hàng!" +
        (lineQuantity === MAX_CART_LINE_QUANTITY ? " (tối đa " + MAX_CART_LINE_QUANTITY + " / sản phẩm)" : ""),
        "success"
    );

    return true;

}


/* ================= HIỆU ỨNG BAY VÀO GIỎ HÀNG ================= */

/*
 * Một bản sao ảnh sản phẩm bay theo đường cong từ thẻ / ảnh chính tới icon giỏ
 * hàng trên header, thu nhỏ dần rồi biến mất; tới nơi thì gọi onArrive (cập nhật
 * số lượng + icon "nảy"). Không có ảnh (chưa tải xong) → một chấm màu nhấn bay
 * thay. Người dùng bật "giảm chuyển động" hoặc không tìm thấy icon giỏ → bỏ qua
 * hiệu ứng, gọi onArrive ngay.
 */

const CART_FLY_DURATION = 800;


function flyToCart(sourceElement, onArrive) {

    const cartButton = document.querySelector(".header .cart-btn");

    const reducedMotion =
        typeof prefersReducedMotion === "function" && prefersReducedMotion();

    const from = sourceElement ? sourceElement.getBoundingClientRect() : null;


    if (!cartButton || reducedMotion || !from || from.width === 0 || typeof Element.prototype.animate !== "function") {

        onArrive();

        return;

    }


    const to = cartButton.getBoundingClientRect();

    const hasImage =
        sourceElement.tagName === "IMG" && sourceElement.getAttribute("src") && sourceElement.complete;


    /* Khối bay: tối đa 140px, căn giữa ảnh gốc */

    const size = Math.min(140, from.width, from.height);

    const startX = from.left + from.width / 2 - size / 2;

    const startY = from.top + from.height / 2 - size / 2;

    const dx = to.left + to.width / 2 - (startX + size / 2);

    const dy = to.top + to.height / 2 - (startY + size / 2);


    const flyer = hasImage ? document.createElement("img") : document.createElement("div");

    if (hasImage) {

        flyer.src = sourceElement.currentSrc || sourceElement.src;

        flyer.alt = "";

    }

    flyer.className = "cart-flyer" + (hasImage ? "" : " cart-flyer-dot");

    flyer.setAttribute("aria-hidden", "true");

    flyer.style.left = startX + "px";

    flyer.style.top = startY + "px";

    flyer.style.width = size + "px";

    flyer.style.height = size + "px";

    document.body.appendChild(flyer);


    /* Đường cong: bay lên một chút rồi lao vào icon giỏ hàng */

    const animation = flyer.animate(
        [
            { transform: "translate(0, 0) scale(1)", opacity: 1 },
            { transform: `translate(${dx * 0.45}px, ${dy * 0.45 - 90}px) scale(0.55)`, opacity: 1, offset: 0.5 },
            { transform: `translate(${dx}px, ${dy}px) scale(0.1)`, opacity: 0.6 }
        ],
        { duration: CART_FLY_DURATION, easing: "cubic-bezier(0.45, 0, 0.55, 1)" }
    );


    let arrived = false;

    function finish() {

        if (arrived) {
            return;
        }

        arrived = true;

        flyer.remove();

        onArrive();

    }

    animation.addEventListener("finish", finish);

    animation.addEventListener("cancel", finish);

}


/* Icon giỏ hàng "nảy" nhẹ + số lượng phóng to trong chốc lát (style.css) */

function bumpCartIcon() {

    document.querySelectorAll(".header .cart-btn").forEach(function (button) {

        button.classList.remove("cart-receive");

        void button.offsetWidth;

        button.classList.add("cart-receive");

        setTimeout(function () {
            button.classList.remove("cart-receive");
        }, 500);

    });


    document.querySelectorAll(".cart-count").forEach(function (count) {

        count.classList.add("bump");

        setTimeout(function () {
            count.classList.remove("bump");
        }, 220);

    });

}
