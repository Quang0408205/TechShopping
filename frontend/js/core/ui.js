/* ================= TIỆN ÍCH GIAO DIỆN DÙNG CHUNG ================= */

/*
 * Toast thông báo (thay cho alert()), khung xương (skeleton) khi đang tải,
 * trạng thái rỗng / lỗi và thẻ sản phẩm dùng chung cho trang chủ + trang
 * Sản phẩm. Lấy chọn lọc từ bản frontend tham chiếu, viết lại theo đúng
 * ProductResponse của backend (GET /api/v1/products).
 *
 * Thứ tự nạp: js/core/api.js → js/core/ui.js → js/core/layout.js →
 * js/core/main.js → script của trang. setupAddToCart() nằm trong main.js,
 * chỉ được gọi lúc chạy nên không phụ thuộc thứ tự nạp. Trang admin/ nạp
 * ui.js nhưng không nạp main.js.
 */


/* ================= ĐỊNH DẠNG GIÁ ================= */

/* formatPrice(41990000) → "41.990.000đ" (chuyển từ main.js sang đây để trang admin/ dùng được) */

function formatPrice(price) {

    return new Intl.NumberFormat(
        "vi-VN"
    ).format(price) + "đ";

}


/*
 * Điểm hiển thị của một sản phẩm (ProductResponse): đánh giá thật trên web
 * (rating / totalReviews, chỉ tính đánh giá đang hiện); chưa có thì điểm crawl
 * từ Thế Giới Di Động (tgddRating / tgddReviewCount); không có cả hai → null.
 * → { rating, count, source: "site" | "tgdd" }
 */

function productRatingSummary(product) {

    if (product && product.totalReviews > 0 && Number(product.rating) > 0) {
        return { rating: Number(product.rating), count: Number(product.totalReviews), source: "site" };
    }

    if (product && product.tgddReviewCount > 0 && Number(product.tgddRating) > 0) {
        return { rating: Number(product.tgddRating), count: Number(product.tgddReviewCount), source: "tgdd" };
    }

    return null;

}


/* ================= CHỐNG XSS ================= */

/*
 * Mọi dữ liệu lấy từ API (tên sản phẩm, tên hãng, URL ảnh...) phải đi qua
 * hàm này trước khi ghép vào innerHTML hoặc thuộc tính HTML.
 */

function escapeHtml(value) {

    if (value === null || value === undefined) {
        return "";
    }

    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");

}


/* ================= TOAST ================= */

let toastContainerElement = null;


function ensureToastContainer() {

    if (!toastContainerElement) {

        toastContainerElement = document.createElement("div");

        toastContainerElement.className = "toast-container";

        toastContainerElement.setAttribute("role", "status");

        toastContainerElement.setAttribute("aria-live", "polite");

        document.body.appendChild(toastContainerElement);

    }

    return toastContainerElement;

}


/* showToast(message, type): type = "success" | "error" | bỏ trống (mặc định) */

function showToast(message, type) {

    const container = ensureToastContainer();

    const toast = document.createElement("div");

    toast.className = "toast" + (type ? " toast-" + type : "");

    toast.textContent = message;

    container.appendChild(toast);


    requestAnimationFrame(function () {
        toast.classList.add("show");
    });


    setTimeout(function () {

        toast.classList.remove("show");

        setTimeout(function () {
            toast.remove();
        }, 300);

    }, 2800);

}


/* ================= SKELETON / RỖNG / LỖI ================= */

function skeletonProductGrid(count) {

    let html = "";

    for (let i = 0; i < (count || 8); i++) {

        html += `
            <div class="product-card skeleton-card" aria-hidden="true">
                <div class="skeleton-block skeleton-image"></div>
                <div class="product-info">
                    <div class="skeleton-block skeleton-line" style="width: 40%"></div>
                    <div class="skeleton-block skeleton-line" style="width: 85%; margin-top: 12px"></div>
                    <div class="skeleton-block skeleton-line" style="width: 50%; margin-top: 12px"></div>
                </div>
            </div>
        `;

    }

    return html;

}


function emptyStateHtml(title, message) {

    return `
        <div class="state-box empty-state">
            <h3>${escapeHtml(title)}</h3>
            <p>${escapeHtml(message)}</p>
        </div>
    `;

}


function errorStateHtml(message, retryButtonId) {

    return `
        <div class="state-box error-state">
            <h3>Đã có lỗi xảy ra</h3>
            <p>${escapeHtml(message)}</p>
            <button type="button" class="btn btn-outline-dark" id="${escapeHtml(retryButtonId)}">
                THỬ LẠI
            </button>
        </div>
    `;

}


/* ================= THẺ SẢN PHẨM ================= */

/*
 * Khoá danh mục dùng trên URL (?category=, các thẻ danh mục ở trang chủ)
 * → slug danh mục trong DB. Dùng chung cho trang Sản phẩm và trang chi tiết.
 */

const PRODUCT_CATEGORY_FILTERS = {
    laptop: "laptop",
    phone: "dien-thoai",
    tablet: "may-tinh-bang",
    accessory: "phu-kien"
};


/* Khoá bộ lọc (vd. "phone") của một slug DB, hoặc null nếu không có nút lọc */

function getCategoryFilterKey(slug) {

    const key = Object.keys(PRODUCT_CATEGORY_FILTERS).find(function (k) {
        return PRODUCT_CATEGORY_FILTERS[k] === slug;
    });

    return key || null;

}


function getProductDetailUrl(productId) {

    return siteUrl(
        "customer/product-detail.html?id=" + encodeURIComponent(productId)
    );

}


/*
 * Ảnh thay thế theo slug danh mục, dùng khi sản phẩm không có ảnh (116 sản
 * phẩm dữ liệu crawl) hoặc tải ảnh lỗi. Ảnh của thẻ lấy thẳng từ
 * ProductResponse.primaryImageUrl (IMG-3), không gọi API riêng cho từng thẻ.
 */

const CATEGORY_FALLBACK_IMAGES = {
    "laptop": "assets/images/laptop.png",
    "dien-thoai": "assets/images/phone.png",
    "may-tinh-bang": "assets/images/tablet.png",
    "phu-kien": "assets/images/banphim.png"
};

const DEFAULT_FALLBACK_IMAGE = "assets/images/phone.png";


function getFallbackImage(categorySlug) {

    return siteUrl(
        CATEGORY_FALLBACK_IMAGES[categorySlug] || DEFAULT_FALLBACK_IMAGE
    );

}


/*
 * Nhãn phiên bản: variantName, nếu trống thì ghép dung lượng - RAM - màu.
 * Dùng chung cho trang chi tiết, giỏ hàng và đơn hàng.
 */

function variantLabelOf(variant) {

    if (!variant) {
        return "";
    }

    if (variant.variantName && variant.variantName.trim()) {
        return variant.variantName.trim();
    }

    const parts = [variant.storage, variant.ram, variant.color]
        .filter(function (part) {
            return part && String(part).trim();
        });

    return parts.length > 0 ? parts.join(" - ") : "Mặc định";

}


/*
 * Giá đang bán do backend tính (effectivePrice): giá thấp hơn giữa giá giảm thủ công (discountPrice)
 * và giá của chương trình khuyến mãi đang diễn ra — đúng giá giỏ hàng / thanh toán sẽ tính.
 * Không có effectivePrice (dữ liệu cũ) → dùng discountPrice như trước.
 */

function salePriceOf(source) {

    if (source.effectivePrice !== null && source.effectivePrice !== undefined) {
        return Number(source.effectivePrice);
    }

    return source.discountPrice !== null && source.discountPrice !== undefined
        ? Number(source.discountPrice)
        : null;

}


/*
 * Giá bán của một phiên bản (hoặc của sản phẩm khi không có phiên bản):
 * { price, oldPrice, promotionName } — oldPrice chỉ có khi giá bán thấp hơn giá gốc,
 * promotionName khi giá đó đến từ một chương trình khuyến mãi.
 */

function resolvePrices(product, variant) {

    const source = variant || product;

    const original = Number(variant ? variant.price : product.basePrice) || 0;

    const sale = salePriceOf(source);

    const discounted = sale !== null && sale < original;

    return {
        price: discounted ? sale : original,
        oldPrice: discounted ? original : null,
        promotionName: discounted && source.activePromotionName ? source.activePromotionName : null
    };

}


/* Giá hiển thị trên thẻ / danh sách: giá đang bán nếu có, không thì giá gốc */

function getDisplayPrice(product) {

    const sale = salePriceOf(product);

    return (sale !== null ? sale : Number(product.basePrice)) || 0;

}


/*
 * productCardHtml(product, categorySlug): product là một phần tử của
 * data.content từ GET /products. Giữ đúng markup thẻ cũ (.product-card >
 * .product-image + .product-info); ảnh và tên link sang trang chi tiết
 * (A2). Nút .add-cart vẫn mang data-name /
 * data-price vì giỏ hàng còn lưu theo tên cho tới Phase 3.
 * Sản phẩm giá 0 (11 sản phẩm trong dữ liệu crawl) hiển thị "Liên hệ" và
 * không cho thêm vào giỏ.
 * Ảnh: primaryImageUrl (ảnh chính, không có thì ảnh đầu tiên) nếu là
 * http(s), không thì ảnh thay thế theo danh mục; ảnh hỏng cũng đổi sang ảnh
 * thay thế (bindProductImageFallbacks).
 */

/*
 * "★ 4.5 (12)" từ đánh giá thật; chưa có thì điểm crawl ghi rõ là tham khảo
 * "★ 4.6 · 1.234 đánh giá tham khảo" (kiemthu GĐ6); không có cả hai → rỗng
 */

function productCardRatingHtml(product) {

    const rating = productRatingSummary(product);

    if (!rating) {
        return "";
    }

    const score = escapeHtml(rating.rating.toFixed(1));

    const count = escapeHtml(rating.count.toLocaleString("vi-VN"));

    if (rating.source === "tgdd") {

        return `
            <p class="product-rating product-rating--tgdd" title="Nguồn: dữ liệu tham khảo bên ngoài">
                ★ ${score} · ${count} đánh giá tham khảo
            </p>
        `;

    }

    return `
        <p class="product-rating" title="${escapeHtml(rating.count + " đánh giá trên POY")}">
            ★ ${score}
            <span>(${count})</span>
        </p>
    `;

}


function productCardHtml(product, categorySlug) {

    const price = getDisplayPrice(product);

    const fallbackImage = getFallbackImage(categorySlug);

    const imageUrl = isSafeImageUrl(product.primaryImageUrl)
        ? product.primaryImageUrl.trim()
        : fallbackImage;

    const hasPrice = price > 0;

    const name = escapeHtml(product.name);

    const detailUrl = escapeHtml(getProductDetailUrl(product.id));

    const basePrice = Number(product.basePrice) || 0;

    /* Có giảm giá thật (giá bán < basePrice): nhãn -x% + giá gốc gạch ngang; di chuột lên nhãn → tên chương trình */
    const hasDiscount = hasPrice && basePrice > price;

    const promotionTitle = hasDiscount && product.activePromotionName
        ? ` title="${escapeHtml("Khuyến mãi: " + product.activePromotionName)}"`
        : "";


    return `
        <div class="product-card" data-product-id="${escapeHtml(product.id)}">

            <a href="${detailUrl}" class="product-image">
                ${hasDiscount ? `<span class="product-badge"${promotionTitle}>-${Math.round(100 - (price / basePrice) * 100)}%</span>` : ""}
                <img
                    src="${escapeHtml(imageUrl)}"
                    data-fallback="${escapeHtml(fallbackImage)}"
                    alt="${name}"
                    loading="lazy"
                >
            </a>

            <div class="product-info">

                <span class="product-category">
                    ${escapeHtml((product.categoryName || "").toUpperCase())}
                </span>

                <h3 title="${name}">
                    <a href="${detailUrl}">${name}</a>
                </h3>

                ${productCardRatingHtml(product)}

                <p class="product-price">
                    ${hasPrice ? formatPrice(price) : "Liên hệ"}
                    ${hasDiscount ? `<span class="product-price-old">${formatPrice(basePrice)}</span>` : ""}
                </p>

                <button
                    type="button"
                    class="add-cart"
                    data-name="${name}"
                    data-price="${price}"
                    ${hasPrice ? "" : "disabled"}
                >
                    ${hasPrice ? "THÊM VÀO GIỎ" : "LIÊN HỆ ĐỂ ĐẶT HÀNG"}
                </button>

            </div>

        </div>
    `;

}


/*
 * renderProductGrid(container, products, categorySlugById):
 * vẽ lưới thẻ (ảnh từ primaryImageUrl), gắn "Thêm vào giỏ" cho các thẻ mới
 * và ảnh thay thế cho ảnh hỏng.
 * categorySlugById: { [categoryId]: slug }, dùng để chọn ảnh thay thế.
 */

function renderProductGrid(container, products, categorySlugById) {

    if (!container) {
        return;
    }


    if (!products || products.length === 0) {

        container.innerHTML = emptyStateHtml(
            "Chưa có sản phẩm phù hợp",
            "Vui lòng thử lại với bộ lọc khác."
        );

        return;

    }


    const slugs = categorySlugById || {};

    container.innerHTML = products.map(function (product) {
        return productCardHtml(product, slugs[product.categoryId]);
    }).join("");


    setupAddToCart(container);

    bindProductImageFallbacks(container);

    staggerRevealCards(container);

}


/* ================= HIỆU ỨNG XUẤT HIỆN ================= */

function prefersReducedMotion() {

    return Boolean(
        window.matchMedia &&
        window.matchMedia("(prefers-reduced-motion: reduce)").matches
    );

}


/*
 * Các thẻ .product-card mới render xuất hiện lần lượt (stagger fade-in).
 * Tự dọn class / transition-delay khi xong để không ảnh hưởng hiệu ứng
 * hover bình thường của thẻ.
 */

function staggerRevealCards(container) {

    if (prefersReducedMotion()) {
        return;
    }


    const cards = Array.from(container.querySelectorAll(".product-card"));

    cards.forEach(function (card, index) {

        card.classList.add("card-enter");

        card.style.transitionDelay = Math.min(index * 45, 360) + "ms";

    });


    requestAnimationFrame(function () {

        requestAnimationFrame(function () {

            cards.forEach(function (card) {

                card.classList.add("card-enter-active");

                card.addEventListener("transitionend", function onEnd(event) {

                    if (event.propertyName !== "opacity") {
                        return;
                    }

                    card.classList.remove("card-enter", "card-enter-active");

                    card.style.transitionDelay = "";

                    card.removeEventListener("transitionend", onEnd);

                });

            });

        });

    });

}


/* ================= MODAL (HỘP THOẠI XÁC NHẬN) ================= */

let modalOverlayElement = null;


/*
 * openConfirmModal({ title, message, confirmLabel, cancelLabel, onConfirm, input }):
 * thay cho confirm() / alert() khi cần người dùng xác nhận (vd. xoá khỏi giỏ).
 * input (tuỳ chọn) = { label, value, placeholder, maxLength }: thêm một ô nhập,
 * onConfirm nhận giá trị đã trim; Enter trong ô = xác nhận.
 * select (tuỳ chọn, thay cho input) = { label, value, options: [{ value, label }] }:
 * thêm một ô chọn, onConfirm nhận giá trị đang chọn.
 * Mọi chuỗi đều được escape.
 */

function openConfirmModal(options) {

    closeModal();


    const input = options.input;

    const select = options.select;

    const overlay = document.createElement("div");

    overlay.className = "modal-overlay";

    overlay.innerHTML = `
        <div class="modal-box" role="dialog" aria-modal="true">
            <h3>${escapeHtml(options.title || "Xác nhận")}</h3>
            <p>${escapeHtml(options.message || "")}</p>
            ${input ? `
                <label class="modal-field">
                    <span>${escapeHtml(input.label || "")}</span>
                    <input
                        type="text"
                        value="${escapeHtml(input.value || "")}"
                        placeholder="${escapeHtml(input.placeholder || "")}"
                        ${input.maxLength ? `maxlength="${Number(input.maxLength)}"` : ""}
                    >
                </label>
            ` : ""}
            ${!input && select ? `
                <label class="modal-field">
                    <span>${escapeHtml(select.label || "")}</span>
                    <select>
                        ${(select.options || []).map(function (option) {
                            return `<option value="${escapeHtml(option.value)}" ${String(option.value) === String(select.value) ? "selected" : ""}>${escapeHtml(option.label)}</option>`;
                        }).join("")}
                    </select>
                </label>
            ` : ""}
            <div class="modal-actions">
                <button type="button" class="btn btn-outline-dark" data-action="cancel">
                    ${escapeHtml(options.cancelLabel || "Hủy")}
                </button>
                <button type="button" class="btn btn-dark" data-action="confirm">
                    ${escapeHtml(options.confirmLabel || "Xác nhận")}
                </button>
            </div>
        </div>
    `;

    document.body.appendChild(overlay);

    modalOverlayElement = overlay;

    const inputElement = overlay.querySelector(".modal-field input");

    const selectElement = overlay.querySelector(".modal-field select");


    function confirm() {

        const value = inputElement
            ? inputElement.value.trim()
            : (selectElement ? selectElement.value : undefined);

        closeModal();

        if (typeof options.onConfirm === "function") {
            options.onConfirm(value);
        }

    }


    overlay.addEventListener("click", function (event) {

        if (event.target === overlay) {
            closeModal();
        }

    });

    overlay.querySelector('[data-action="cancel"]').addEventListener("click", closeModal);

    overlay.querySelector('[data-action="confirm"]').addEventListener("click", confirm);

    if (inputElement) {

        inputElement.addEventListener("keydown", function (event) {

            if (event.key === "Enter") {
                event.preventDefault();
                confirm();
            }

        });

        inputElement.focus();

    } else if (selectElement) {

        selectElement.focus();

    }


    requestAnimationFrame(function () {
        overlay.classList.add("show");
    });

}


function closeModal() {

    if (modalOverlayElement) {

        modalOverlayElement.remove();

        modalOverlayElement = null;

    }

}


/*
 * Hộp thoại hiện MỘT LẦN một giá trị bí mật (mật khẩu tạm của nhân viên mới), có nút sao chép.
 * Cố ý không đóng khi bấm ra ngoài: đóng rồi thì không xem lại được.
 * options: { title, message, label, secret, doneLabel, onClose }
 */

function openSecretModal(options) {

    closeModal();


    const overlay = document.createElement("div");

    overlay.className = "modal-overlay";

    overlay.innerHTML = `
        <div class="modal-box modal-secret" role="dialog" aria-modal="true">
            <h3>${escapeHtml(options.title || "Thông tin quan trọng")}</h3>
            <p>${escapeHtml(options.message || "")}</p>
            <div class="modal-secret-row">
                <span class="modal-secret-label">${escapeHtml(options.label || "")}</span>
                <code class="modal-secret-value" id="secretValue">${escapeHtml(options.secret || "")}</code>
                <button type="button" class="btn btn-outline-dark" id="secretCopyBtn">SAO CHÉP</button>
            </div>
            <p class="modal-secret-warning">Mật khẩu này chỉ hiện một lần. Đóng hộp thoại là không xem lại được.</p>
            <div class="modal-actions">
                <button type="button" class="btn btn-dark" id="secretDoneBtn">${escapeHtml(options.doneLabel || "Đã ghi lại")}</button>
            </div>
        </div>
    `;

    document.body.appendChild(overlay);

    modalOverlayElement = overlay;


    const copyButton = overlay.querySelector("#secretCopyBtn");

    copyButton.addEventListener("click", async function () {

        try {

            await navigator.clipboard.writeText(options.secret || "");

            copyButton.textContent = "ĐÃ SAO CHÉP";

        } catch (error) {

            /* Không có quyền clipboard: chọn sẵn chữ để người dùng tự Ctrl+C */
            const range = document.createRange();

            range.selectNodeContents(overlay.querySelector("#secretValue"));

            const selection = window.getSelection();

            selection.removeAllRanges();

            selection.addRange(range);

            copyButton.textContent = "ĐÃ CHỌN, NHẤN CTRL+C";

        }

    });

    overlay.querySelector("#secretDoneBtn").addEventListener("click", function () {

        closeModal();

        if (typeof options.onClose === "function") {
            options.onClose();
        }

    });

    overlay.querySelector("#secretDoneBtn").focus();

    requestAnimationFrame(function () {
        overlay.classList.add("show");
    });

}


/* ================= ẢNH SẢN PHẨM ================= */

/* Chỉ nhận URL http/https (ảnh do ADMIN nhập, không tin tuyệt đối) */

function isSafeImageUrl(url) {

    return typeof url === "string" && /^https?:\/\//i.test(url.trim());

}


/*
 * Ảnh thẻ hỏng (link sai, CDN lỗi, ảnh đã xoá) → ảnh thay thế theo danh mục
 * (data-fallback). Đổi một lần, không lặp nếu chính ảnh thay thế cũng lỗi.
 */

function bindProductImageFallbacks(container) {

    container.querySelectorAll(".product-card .product-image img[data-fallback]")
        .forEach(function (img) {

            img.addEventListener("error", function () {

                if (img.getAttribute("src") !== img.dataset.fallback) {
                    img.src = img.dataset.fallback;
                }

            });

        });

}


/* ================= DANH MỤC ================= */

/*
 * GET /categories?size=100 (công khai). Trả về
 * { byId: { [id]: slug }, bySlug: { [slug]: id } }.
 */

async function fetchCategoryMaps() {

    const page = await apiRequest("/categories?size=100");

    const byId = {};

    const bySlug = {};


    (page.content || []).forEach(function (category) {

        byId[category.id] = category.slug;

        bySlug[category.slug] = category.id;

    });


    return { byId: byId, bySlug: bySlug };

}
