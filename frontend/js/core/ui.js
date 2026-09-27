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
 * phẩm) hoặc tải ảnh lỗi. ProductResponse không có URL ảnh nên mỗi thẻ tải
 * ảnh riêng qua GET /products/{id}/images (xem loadProductImages).
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


/* Giá hiển thị: giá khuyến mãi nếu có, không thì giá gốc */

function getDisplayPrice(product) {

    const price =
        product.discountPrice !== null && product.discountPrice !== undefined
            ? product.discountPrice
            : product.basePrice;

    return Number(price) || 0;

}


/*
 * productCardHtml(product, categorySlug): product là một phần tử của
 * data.content từ GET /products. Giữ đúng markup thẻ cũ (.product-card >
 * .product-image + .product-info); ảnh và tên link sang trang chi tiết
 * (A2). Nút .add-cart vẫn mang data-name /
 * data-price vì giỏ hàng còn lưu theo tên cho tới Phase 3.
 * Sản phẩm giá 0 (11 sản phẩm trong dữ liệu crawl) hiển thị "Liên hệ" và
 * không cho thêm vào giỏ.
 */

function productCardHtml(product, categorySlug) {

    const price = getDisplayPrice(product);

    const hasPrice = price > 0;

    const name = escapeHtml(product.name);

    const detailUrl = escapeHtml(getProductDetailUrl(product.id));


    return `
        <div class="product-card" data-product-id="${escapeHtml(product.id)}">

            <a href="${detailUrl}" class="product-image">
                <img
                    data-fallback="${escapeHtml(getFallbackImage(categorySlug))}"
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

                <p class="product-price">
                    ${hasPrice ? formatPrice(price) : "Liên hệ"}
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
 * vẽ lưới thẻ, gắn "Thêm vào giỏ" cho các thẻ mới, rồi tải ảnh thật.
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

    loadProductImages(container);

}


/* ================= ẢNH SẢN PHẨM ================= */

/* Chỉ nhận URL http/https (ảnh do ADMIN nhập, không tin tuyệt đối) */

function isSafeImageUrl(url) {

    return typeof url === "string" && /^https?:\/\//i.test(url.trim());

}


function pickProductImage(images) {

    if (!Array.isArray(images) || images.length === 0) {
        return null;
    }

    const primary = images.find(function (image) {
        return image.isPrimary && isSafeImageUrl(image.imageUrl);
    });

    if (primary) {
        return primary.imageUrl;
    }

    const first = images.find(function (image) {
        return isSafeImageUrl(image.imageUrl);
    });

    return first ? first.imageUrl : null;

}


/*
 * Với mỗi thẻ trong container: GET /products/{id}/images (công khai, không
 * gửi token), lấy ảnh chính hoặc ảnh đầu tiên. Không có ảnh / request lỗi /
 * ảnh hỏng → ảnh thay thế theo danh mục. Thẻ mới render chưa có src (ô ảnh
 * nền xám) để không hiện nhầm ảnh thay thế trong lúc chờ API.
 */

function loadProductImages(container) {

    container.querySelectorAll(".product-card[data-product-id]")
        .forEach(function (card) {

            const img = card.querySelector(".product-image img");

            if (!img) {
                return;
            }


            img.addEventListener("error", function () {

                if (img.src !== img.dataset.fallback) {
                    img.src = img.dataset.fallback;
                }

            });


            apiRequest(
                "/products/" + encodeURIComponent(card.dataset.productId) + "/images"
            )
                .then(function (images) {

                    img.src = pickProductImage(images) || img.dataset.fallback;

                })
                .catch(function () {

                    img.src = img.dataset.fallback;

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
