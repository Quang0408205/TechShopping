document.addEventListener("DOMContentLoaded", async function () {

    setupAddToCart();


    /* Header được layout.js chèn vào sau: chờ xong mới cập nhật */

    if (typeof layoutReady !== "undefined") {
        await layoutReady;
    }

    renderAuthState();

    migrateLegacyCart();

    /* Gọi GET /cart (sau khi chuyển giỏ cũ trên trình duyệt lên server) */
    updateCartCount();

    setupHeaderSearch();

    setupHeaderScrollEffect();

    setupScrollReveal();

});


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


                window.location.href =
                    siteUrl("customer/products.html") +
                    (keyword ? "?search=" + encodeURIComponent(keyword) : "");

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
    ".section-heading, .category-grid, .service-grid, .benefits-grid, .footer-grid, .promo-grid, .review-grid";


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
 * và chọn phiên bản đầu tiên (giống phiên bản mặc định ở trang chi tiết).
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

                const variant = Array.isArray(variants) && variants.length > 0 ? variants[0] : null;

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
