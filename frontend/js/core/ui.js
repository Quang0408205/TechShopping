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
 * Điểm của một sản phẩm (ProductResponse): chỉ đánh giá thật trên web
 * (rating / totalReviews, tính các đánh giá đang hiện); chưa có → null.
 * → { rating, count }
 */

function productRatingSummary(product) {

    if (product && product.totalReviews > 0 && Number(product.rating) > 0) {
        return { rating: Number(product.rating), count: Number(product.totalReviews) };
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
 * không cho thêm vào giỏ. availableQuantity = 0 (không chi nhánh đang mở nào
 * còn hàng, kế hoạch v3 GĐ E) → nhãn "Hết hàng", nút bị khoá.
 * Thiếu trường này (API cũ) thì coi như còn hàng.
 * Ảnh: primaryImageUrl (ảnh chính, không có thì ảnh đầu tiên) nếu là
 * http(s), không thì ảnh thay thế theo danh mục; ảnh hỏng cũng đổi sang ảnh
 * thay thế (bindProductImageFallbacks).
 */

/* "★ 4.5 (12)" từ đánh giá thật; chưa có đánh giá nào → "Chưa có đánh giá" */

function productCardRatingHtml(product) {

    const rating = productRatingSummary(product);

    if (!rating) {
        return '<p class="product-rating product-rating--none">Chưa có đánh giá</p>';
    }

    const score = escapeHtml(rating.rating.toFixed(1));

    const count = escapeHtml(rating.count.toLocaleString("vi-VN"));

    return `
        <p class="product-rating" title="${escapeHtml(rating.count + " đánh giá trên POY")}">
            ★ ${score}
            <span>(${count})</span>
        </p>
    `;

}


/* Kế hoạch v3 GĐ E: sản phẩm / phiên bản không còn ở chi nhánh đang mở nào (thiếu trường = API cũ → còn hàng) */

function isSoldOut(item) {

    return item && item.availableQuantity === 0;

}


/* ================= MÀU + PHIÊN BẢN (kế hoạch v5 GĐ2) ================= */

/*
 * Dữ liệu chỉ có tên màu ("Titan sa mạc", "Đen - Đỏ"…): đổi sang mã màu theo từ khoá (không dấu, khớp
 * nguyên cụm từ, cụm dài đứng trước). Màu 2 tông "A - B" → nửa A nửa B; không nhận ra → null (chấm xám).
 */

const VARIANT_COLOR_HEX = [
    ["titan sa mac", "#c8a98a"], ["titan tu nhien", "#b8b1a6"], ["titan den", "#3c3b39"], ["titan xam", "#7d7f83"],
    ["titan xanh", "#4b5e72"], ["titan tim", "#7b6a8c"], ["titan vang", "#d9c08f"], ["titan trang", "#e9e7e2"],
    ["xanh bac ha", "#a8dcc6"], ["xanh mint", "#a8dcc6"], ["xanh navy", "#1f3a68"], ["xanh nuoc bien", "#1f5fa0"],
    ["xanh luu ly", "#3b5ba5"], ["xanh duong dam", "#1f4e8c"], ["xanh duong nhat", "#a9c8ec"],
    ["xanh da troi nhat", "#a9c8ec"], ["xanh lam nhat", "#a9c8ec"], ["xanh da troi", "#6aa6e0"],
    ["xanh duong", "#2f6fbf"], ["xanh la xo thom", "#9caf88"], ["xanh la dam", "#2e6b45"], ["xanh la", "#3e9a5c"],
    ["xanh reu", "#5b6b3a"], ["xanh oliu", "#708238"], ["xanh ngoc", "#2bb3a6"], ["xanh tim", "#5a5ba8"],
    ["xanh den", "#1e2b3a"], ["xanh dam", "#1f3f6e"], ["xanh", "#4a7fb5"],
    ["tim bac", "#b9a9cc"], ["tim nhat", "#c7b3e0"], ["tim", "#7e5bb5"],
    ["vang hong", "#e8b4a0"], ["hong nhat", "#f6c6d2"], ["hong", "#ef8fa8"],
    ["vang dong", "#c9a26b"], ["vang nhat", "#f1dc98"], ["vang", "#e5bf4c"],
    ["do burgundy", "#7a1f2b"], ["do", "#c62f2f"],
    ["cam vu tru", "#d9652b"], ["cam nhat", "#f4b183"], ["cam", "#e67e22"],
    ["nau nhat", "#b08968"], ["nau", "#7b4b2a"],
    ["trang starlight", "#f3efe6"], ["trang anh sao", "#f3efe6"], ["trang", "#f7f7f7"],
    ["mau be", "#efe4cc"], ["kem", "#efe4cc"], ["be", "#efe4cc"],
    ["ghi den", "#3a3b3d"], ["ghi", "#6d6f73"], ["bac", "#c9ccd1"], ["xam nhat", "#c3c5c9"], ["xam", "#85888e"],
    ["den bong", "#111111"], ["den", "#1f1f1f"], ["dong", "#b87333"]
];


function foldVariantText(text) {

    return String(text || "")
        .normalize("NFD")
        .replace(/[̀-ͯ]/g, "")
        .replace(/đ/g, "d")
        .replace(/Đ/g, "D")
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, " ")
        .trim();

}


function variantColorHex(name) {

    const folded = " " + foldVariantText(name) + " ";

    const found = VARIANT_COLOR_HEX.find(function (entry) {
        return folded.indexOf(" " + entry[0] + " ") !== -1;
    });

    return found ? found[1] : null;

}


/* Giá trị CSS background cho chấm màu (đã an toàn: chỉ từ bảng mã màu ở trên) */

function variantColorBackground(name) {

    const tones = String(name || "").split(/\s+-\s+/).map(variantColorHex).filter(Boolean);

    if (tones.length >= 2) {
        return "linear-gradient(135deg, " + tones[0] + " 50%, " + tones[1] + " 50%)";
    }

    return tones[0] || variantColorHex(name) || "";

}


function variantColorDotHtml(name, extraClass) {

    const background = variantColorBackground(name);

    return `<span class="color-dot${background ? "" : " is-unknown"}${extraClass ? " " + extraClass : ""}"`
        + (background ? ` style="background:${background}"` : "")
        + ` aria-hidden="true"></span>`;

}


/* "12 GB" → "12GB" */

function compactCapacity(value) {

    return String(value || "").replace(/\s+/g, "").toUpperCase();

}


/* Dung lượng ra GB để sắp xếp ("1 TB" → 1024); không đọc được → 0 */

function capacityInGb(value) {

    const match = String(value || "").match(/([\d.,]+)\s*(TB|GB|MB)/i);

    if (!match) {
        return 0;
    }

    const number = Number(match[1].replace(",", "."));

    const unit = match[2].toUpperCase();

    return unit === "TB" ? number * 1024 : unit === "MB" ? number / 1024 : number;

}


/*
 * Nhãn "phiên bản" = RAM + bộ nhớ: "8GB - 256GB", chỉ bộ nhớ → "256GB", laptop → "RAM 16GB - SSD 512GB".
 * Không có cả hai → tên phiên bản bỏ phần màu (hoặc "" nếu chỉ khác nhau về màu).
 */

function variantVersionLabel(variant, isLaptop) {

    const ram = compactCapacity(variant.ram);

    const storage = compactCapacity(variant.storage);

    if (ram && storage) {
        return isLaptop ? "RAM " + ram + " - SSD " + storage : ram + " - " + storage;
    }

    if (storage || ram) {
        return isLaptop && ram ? "RAM " + ram : storage || ram;
    }

    const name = String(variant.variantName || "").trim();

    if (!name || !variant.color) {
        return name;
    }

    /* "Bản thường - Đen" → "Bản thường"; chỉ có màu → "" */
    return name.split(/\s+-\s+/).filter(function (part) {
        return foldVariantText(part) !== foldVariantText(variant.color);
    }).join(" - ");

}


/*
 * Nhóm phiên bản theo nhãn RAM + bộ nhớ, nhóm sắp theo bộ nhớ rồi RAM tăng dần:
 * [{ label, variants: [...] }]. Thứ tự màu trong nhóm giữ như API.
 */

function groupVariantsByVersion(variants, isLaptop) {

    const groups = [];

    (variants || []).forEach(function (variant) {

        const label = variantVersionLabel(variant, isLaptop);

        let group = groups.find(function (item) { return item.label === label; });

        if (!group) {
            group = { label: label, variants: [], storageGb: capacityInGb(variant.storage), ramGb: capacityInGb(variant.ram) };
            groups.push(group);
        }

        group.variants.push(variant);

    });

    return groups.sort(function (a, b) {
        return (a.storageGb - b.storageGb) || (a.ramGb - b.ramGb);
    });

}


/* Dãy chấm màu nhỏ trên thẻ sản phẩm: các màu khác nhau của sản phẩm (tối đa 5, còn lại "+n") */

const CARD_COLOR_DOTS_MAX = 5;

function cardColorDotsHtml(product) {

    const names = [];

    (Array.isArray(product.variants) ? product.variants : []).forEach(function (option) {

        const color = String(option.color || "").trim();

        if (color && !names.some(function (name) { return foldVariantText(name) === foldVariantText(color); })) {
            names.push(color);
        }

    });

    if (names.length === 0) {
        return "";
    }

    const dots = names.slice(0, CARD_COLOR_DOTS_MAX).map(function (name) {
        return `<span class="product-color" title="${escapeHtml(name)}">${variantColorDotHtml(name)}</span>`;
    }).join("");

    const more = names.length > CARD_COLOR_DOTS_MAX
        ? `<span class="product-color-more">+${names.length - CARD_COLOR_DOTS_MAX}</span>`
        : "";

    return `<div class="product-colors" aria-label="${escapeHtml(names.length + " màu: " + names.join(", "))}">${dots}${more}</div>`;

}


/*
 * Phiên bản trên thẻ (API GET /products trả product.variants, rẻ nhất trước): gộp theo nhãn
 * (dung lượng / RAM), mỗi nút đại diện phiên bản rẻ nhất còn hàng của nhãn đó. Chọn nút →
 * đổi giá, nhãn giảm giá, trạng thái hết hàng và phiên bản sẽ thêm vào giỏ.
 * Thiếu product.variants (gợi ý AI, API cũ) → không hiện nút; chỉ 1 phiên bản vẫn hiện 1 nút (đang chọn).
 */

const CARD_VARIANT_CHIPS_MAX = 4;

function cardVariantChoices(product) {

    const options = Array.isArray(product.variants) ? product.variants : [];

    const groups = [];

    options.forEach(function (option) {

        const label = option.label || "";

        const current = groups.find(function (group) { return group.label === label; });

        if (!current) {
            groups.push(option);
            return;
        }

        /* cùng nhãn: ưu tiên bản còn hàng, rồi bản rẻ hơn */
        const better = (isSoldOut(current) && !isSoldOut(option))
            || (isSoldOut(current) === isSoldOut(option)
                && Number(option.effectivePrice) < Number(current.effectivePrice));

        if (better) {
            groups[groups.indexOf(current)] = option;
        }

    });

    return groups;

}


function cardVariantsHtml(choices, selectedId) {

    if (choices.length < 1) {
        return "";
    }

    const chips = choices.slice(0, CARD_VARIANT_CHIPS_MAX).map(function (choice) {

        const selected = choice.id === selectedId;

        return `
            <button
                type="button"
                class="variant-chip${selected ? " is-selected" : ""}${isSoldOut(choice) ? " is-sold-out" : ""}"
                data-variant-id="${escapeHtml(choice.id)}"
                aria-pressed="${selected}"
            >${escapeHtml(choice.label)}</button>
        `;

    }).join("");

    const more = choices.length > CARD_VARIANT_CHIPS_MAX
        ? `<a class="variant-chip variant-chip--more" href="#" data-more-variants>+${choices.length - CARD_VARIANT_CHIPS_MAX}</a>`
        : "";

    return `<div class="product-variants" role="group" aria-label="Chọn phiên bản">${chips}${more}</div>`;

}


/* Khối giá của thẻ; dùng cả lúc vẽ lần đầu lẫn khi đổi phiên bản */

function cardPriceHtml(price, basePrice) {

    if (!(price > 0)) {
        return "Liên hệ";
    }

    return formatPrice(price)
        + (basePrice > price ? `<span class="product-price-old">${formatPrice(basePrice)}</span>` : "");

}


function bindCardVariantChips(container) {

    container.querySelectorAll(".product-card").forEach(function (card) {

        const chips = card.querySelectorAll(".variant-chip[data-variant-id]");

        if (chips.length === 0) {
            return;
        }

        const choices = JSON.parse(card.dataset.variants || "[]");

        chips.forEach(function (chip) {

            chip.addEventListener("click", function () {

                const choice = choices.find(function (item) { return String(item.id) === chip.dataset.variantId; });

                if (!choice) {
                    return;
                }

                chips.forEach(function (other) {
                    const on = other === chip;
                    other.classList.toggle("is-selected", on);
                    other.setAttribute("aria-pressed", String(on));
                });

                const price = Number(choice.effectivePrice) || 0;
                const listPrice = Number(choice.price) || 0;
                const soldOut = price > 0 && isSoldOut(choice);
                const hasDiscount = price > 0 && listPrice > price;

                card.dataset.variantId = String(choice.id);

                card.querySelector(".product-price").innerHTML = cardPriceHtml(price, listPrice);

                const image = card.querySelector(".product-image");
                const badge = image.querySelector(".product-badge");

                if (hasDiscount) {
                    const text = "-" + Math.round(100 - (price / listPrice) * 100) + "%";
                    if (badge) {
                        badge.textContent = text;
                    } else {
                        image.insertAdjacentHTML("afterbegin", `<span class="product-badge">${text}</span>`);
                    }
                } else if (badge) {
                    badge.remove();
                }

                const stockBadge = image.querySelector(".product-stock-badge");

                if (soldOut && !stockBadge) {
                    image.insertAdjacentHTML("afterbegin", `<span class="product-stock-badge">Hết hàng</span>`);
                } else if (!soldOut && stockBadge) {
                    stockBadge.remove();
                }

                const button = card.querySelector(".add-cart");

                button.dataset.price = String(price);
                button.disabled = !(price > 0) || soldOut;
                button.textContent = !(price > 0) ? "LIÊN HỆ ĐỂ ĐẶT HÀNG" : soldOut ? "TẠM HẾT HÀNG" : "THÊM VÀO GIỎ";

            });

        });

        /* "+N": xem đủ phiên bản ở trang chi tiết */
        const more = card.querySelector("[data-more-variants]");

        if (more) {
            more.setAttribute("href", card.querySelector(".product-image").getAttribute("href"));
        }

    });

}


function productCardHtml(product, categorySlug) {

    const choices = cardVariantChoices(product);

    /* mặc định: phiên bản rẻ nhất còn hàng; thẻ giữ giá sản phẩm khi chưa có phiên bản nào */
    const firstChoice = choices.find(function (choice) { return !isSoldOut(choice); }) || choices[0] || null;

    const shown = choices.length >= 1 && firstChoice
        ? Object.assign({}, product, {
            effectivePrice: firstChoice.effectivePrice,
            basePrice: firstChoice.price,
            discountPrice: null,
            availableQuantity: firstChoice.availableQuantity
        })
        : product;

    const price = getDisplayPrice(shown);

    const fallbackImage = getFallbackImage(categorySlug);

    const imageUrl = isSafeImageUrl(product.primaryImageUrl)
        ? product.primaryImageUrl.trim()
        : fallbackImage;

    const hasPrice = price > 0;

    const soldOut = hasPrice && isSoldOut(shown);

    const name = escapeHtml(product.name);

    const detailUrl = escapeHtml(getProductDetailUrl(product.id));

    const basePrice = Number(shown.basePrice) || 0;

    const variantsAttrs = choices.length >= 1 && firstChoice
        ? ` data-variant-id="${escapeHtml(firstChoice.id)}" data-variants="${escapeHtml(JSON.stringify(choices))}"`
        : "";

    /* Có giảm giá thật (giá bán < basePrice): nhãn -x% + giá gốc gạch ngang; di chuột lên nhãn → tên chương trình */
    const hasDiscount = hasPrice && basePrice > price;

    const promotionTitle = hasDiscount && product.activePromotionName
        ? ` title="${escapeHtml("Khuyến mãi: " + product.activePromotionName)}"`
        : "";


    return `
        <div class="product-card" data-product-id="${escapeHtml(product.id)}"${variantsAttrs}>

            <a href="${detailUrl}" class="product-image">
                ${hasDiscount ? `<span class="product-badge"${promotionTitle}>-${Math.round(100 - (price / basePrice) * 100)}%</span>` : ""}
                ${soldOut ? `<span class="product-stock-badge">Hết hàng</span>` : ""}
                <img
                    src="${escapeHtml(imageUrl)}"
                    data-fallback="${escapeHtml(fallbackImage)}"
                    referrerpolicy="no-referrer"
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

                ${cardVariantsHtml(choices, firstChoice ? firstChoice.id : null)}

                ${cardColorDotsHtml(product)}

                <p class="product-price">${cardPriceHtml(price, basePrice)}</p>

                <button
                    type="button"
                    class="add-cart"
                    data-name="${name}"
                    data-price="${price}"
                    ${hasPrice && !soldOut ? "" : "disabled"}
                >
                    ${!hasPrice ? "LIÊN HỆ ĐỂ ĐẶT HÀNG" : soldOut ? "TẠM HẾT HÀNG" : "THÊM VÀO GIỎ"}
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


    bindCardVariantChips(container);

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

    /* Kế hoạch v3: danh mục ADMIN ẩn → không hiện ở trang khách, sản phẩm của nó cũng không bán */
    const hiddenIds = {};


    (page.content || []).forEach(function (category) {

        byId[category.id] = category.slug;

        bySlug[category.slug] = category.id;

        if (category.isActive === false) {
            hiddenIds[category.id] = true;
        }

    });


    /* Danh mục đang hiển thị, theo "Thứ tự hiển thị" ADMIN đặt rồi theo tên */
    const visible = (page.content || [])
        .filter(function (category) { return category.isActive !== false; })
        .sort(function (a, b) {
            return (a.displayOrder ?? 9999) - (b.displayOrder ?? 9999) || a.name.localeCompare(b.name, "vi");
        });


    return { byId: byId, bySlug: bySlug, hiddenIds: hiddenIds, visible: visible };

}


/* Một lần tải cho cả trang (header + nội dung trang); lỗi thì lần sau tải lại */

let shopCategoriesPromise = null;

function loadShopCategories() {

    if (!shopCategoriesPromise) {

        shopCategoriesPromise = fetchCategoryMaps().catch(function (error) {

            shopCategoriesPromise = null;

            throw error;

        });

    }

    return shopCategoriesPromise;

}


/* Khoá trên URL ?category=: khoá cũ (phone, laptop…) nếu có, không thì chính slug */

function categoryKeyOf(slug) {

    return getCategoryFilterKey(slug) || slug;

}


function categoryUrlOf(category) {

    return siteUrl("customer/products.html?category=" + encodeURIComponent(categoryKeyOf(category.slug)));

}


/* Ảnh của danh mục: chỉ ảnh ADMIN tải lên ở trang Danh mục (không có thì dùng bộ biểu tượng nét mảnh bên dưới) */

function categoryImageOf(category) {

    return isSafeImageUrl(category.iconUrl) ? category.iconUrl.trim() : null;

}


/*
 * Danh mục chưa có ảnh ADMIN tải lên: biểu tượng nét mảnh cùng một bộ (không dùng ảnh poster của sản phẩm,
 * vì mỗi sản phẩm một kiểu nền nên khó nhìn và dễ lệch màu). Khớp theo slug.
 */

const CATEGORY_ART_PATHS = {
    phone: '<rect x="7" y="2" width="10" height="20" rx="2.5"/><path d="M11 18.5h2"/>',
    laptop: '<rect x="4" y="5" width="16" height="11" rx="1.5"/><path d="M2 19h20"/>',
    tablet: '<rect x="5" y="2.5" width="14" height="19" rx="2"/><path d="M11.5 18.5h1"/>',
    smartwatch: '<rect x="7" y="6" width="10" height="12" rx="3"/><path d="M9 6l.8-3h4.4l.8 3M9 18l.8 3h4.4l.8-3M12 9.5v2.7l1.6 1"/>',
    watch: '<circle cx="12" cy="12" r="5"/><path d="M12 9.5V12l1.8 1.2M9.5 7.2L10 3h4l.5 4.2M9.5 16.8L10 21h4l.5-4.2"/>',
    monitor: '<rect x="3" y="4" width="18" height="12" rx="1.5"/><path d="M8 20h8M12 16v4"/>',
    desktop: '<rect x="7" y="2.5" width="10" height="19" rx="1.5"/><circle cx="12" cy="7" r="1.2"/><path d="M10 17h4"/>',
    accessory: '<path d="M4 14v-2a8 8 0 0 1 16 0v2"/><rect x="3" y="14" width="4" height="6" rx="1.5"/><rect x="17" y="14" width="4" height="6" rx="1.5"/>',
    other: '<path d="M12 3l8 4.5v9L12 21l-8-4.5v-9z"/><path d="M4 7.5l8 4.5 8-4.5M12 12v9"/>'
};

function categoryArtKey(slug) {

    const value = String(slug || "");

    if (/dong-ho-thong-minh|smartwatch|vong-deo/.test(value)) {
        return "smartwatch";
    }

    if (/dong-ho/.test(value)) {
        return "watch";
    }

    if (/man-hinh/.test(value)) {
        return "monitor";
    }

    if (/may-tinh-de-ban|pc|desktop/.test(value)) {
        return "desktop";
    }

    if (/may-tinh-bang|tablet/.test(value)) {
        return "tablet";
    }

    if (/laptop/.test(value)) {
        return "laptop";
    }

    if (/dien-thoai|phone/.test(value)) {
        return "phone";
    }

    if (/phu-kien|accessory/.test(value)) {
        return "accessory";
    }

    return "other";

}

function categoryHasPhoto(category) {

    return isSafeImageUrl(category.iconUrl);

}

/* <img> khi có ảnh riêng, không thì biểu tượng (cả hai dùng chung ô hiển thị của header và trang chủ) */

function categoryVisualHtml(category) {

    if (categoryHasPhoto(category)) {
        return `<img src="${escapeHtml(categoryImageOf(category))}" alt="" loading="lazy">`;
    }

    return `<span class="category-art" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" `
        + `stroke-linecap="round" stroke-linejoin="round">${CATEGORY_ART_PATHS[categoryArtKey(category.slug)]}</svg></span>`;

}


/* ================= KHUYẾN NGHỊ SẢN PHẨM (AI) ================= */

/* Sản phẩm khách xem gần nhất: "neo" cho các khu tương tự / mua kèm / nâng cấp ở trang Khuyến nghị */
const LAST_VIEWED_PRODUCT_KEY = "poy_last_viewed_product";

/* Sản phẩm đã xem (kế hoạch v6 B3): mảng id trong localStorage, mới nhất đứng đầu, tối đa 10 */
const RECENT_VIEWED_KEY = "poy_recent_viewed";

const RECENT_VIEWED_MAX = 10;

function readRecentViewed() {

    try {

        const list = JSON.parse(localStorage.getItem(RECENT_VIEWED_KEY) || "[]");

        return Array.isArray(list)
            ? list.filter(function (id) { return Number.isInteger(id) && id > 0; }).slice(0, RECENT_VIEWED_MAX)
            : [];

    } catch (error) {

        return [];

    }

}

function writeRecentViewed(ids) {

    try {

        if (ids.length) {
            localStorage.setItem(RECENT_VIEWED_KEY, JSON.stringify(ids.slice(0, RECENT_VIEWED_MAX)));
        } else {
            localStorage.removeItem(RECENT_VIEWED_KEY);
        }

    } catch (error) {
        /* bỏ qua: trình duyệt chặn lưu trữ */
    }

}

function rememberViewedProduct(productId) {

    const id = Number(productId);

    if (Number.isInteger(id) && id > 0) {

        writeRecentViewed([id].concat(readRecentViewed().filter(function (other) { return other !== id; })));

    }

}


/*
 * POST /recommendations {type, productId?, size} → {type, source: "AI" | "FALLBACK", items: [{product, score, reason}]}.
 * type: PERSONAL | SIMILAR | UPSELL | CROSS_SELL. Gửi token khi đã đăng nhập để danh sách được cá nhân hoá;
 * options.guest = true: luôn hỏi như khách (danh sách phổ biến).
 */

function fetchRecommendations(type, productId, size, options) {

    const guest = options && options.guest === true;

    return apiRequest("/recommendations", {
        method: "POST",
        auth: !guest && isLoggedIn(),
        body: { type: type, productId: productId || null, size: size }
    });

}


/* Lưới thẻ sản phẩm như renderProductGrid, thêm dòng "lý do gợi ý" dưới tên sản phẩm */

function renderRecommendationGrid(container, items, categorySlugById) {

    renderProductGrid(
        container,
        items.map(function (item) { return item.product; }),
        categorySlugById
    );

    items.forEach(function (item) {

        if (!item.reason) {
            return;
        }

        const card = container.querySelector(
            '.product-card[data-product-id="' + CSS.escape(String(item.product.id)) + '"] .product-info h3'
        );

        if (card) {
            card.insertAdjacentHTML(
                "afterend",
                '<p class="product-reason">' + escapeHtml(item.reason) + "</p>"
            );
        }

    });

}
