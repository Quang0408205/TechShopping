/* ================= GIỎ HÀNG (Phase 3): GIỎ TRÊN SERVER ================= */

/*
 * Giỏ hàng lưu ở backend theo tài khoản: /api/v1/cart (cần đăng nhập).
 * Mỗi dòng xác định bằng variantId; giá, tên, ảnh luôn lấy từ server lúc đọc
 * (không tin giá do trình duyệt gửi lên). Mọi hàm đều async và trả về giỏ đã
 * đổi sang dạng dùng cho trang giỏ / thanh toán:
 *   { items: [{ productId, variantId, name, variantLabel, price, oldPrice,
 *               image, quantity, lineTotal, available }],
 *     totalQuantity, subtotal, shippingFee, total, hasUnavailableItems }
 * Dòng available = false: sản phẩm đã ngừng bán (bị xoá / ẩn / chưa có giá),
 * không tính vào tạm tính, chỉ còn nút xoá.
 *
 * Giỏ cũ lưu trên trình duyệt ("poy_cart_<userId>", F2) được chuyển lên
 * server một lần (migrateLocalCart); lỗi API thì hàm ném ApiError (api.js).
 *
 * Thứ tự nạp: api.js → ui.js → cart-store.js → layout.js → main.js.
 */

/* Phí vận chuyển mô phỏng: backend chưa tính phí ship (Phase 4 – Order) */
const FREE_SHIPPING_THRESHOLD = 10000000;

const DEFAULT_SHIPPING_FEE = 30000;

/* Tối đa mỗi dòng, giống CartService.MAX_LINE_QUANTITY ở backend */
const MAX_CART_LINE_QUANTITY = 10;


function emptyCartSummary() {

    return toCartSummary(null);

}


/* CartResponse của API → dạng dùng ở giao diện (+ phí ship mô phỏng) */

function toCartSummary(cart) {

    const items = (cart && Array.isArray(cart.items) ? cart.items : []).map(function (item) {

        return {
            productId: item.productId,
            variantId: item.variantId,
            name: item.productName,
            variantLabel: item.variantName || "",
            price: Number(item.unitPrice) || 0,
            oldPrice: item.originalPrice !== null && item.originalPrice !== undefined
                ? Number(item.originalPrice)
                : null,
            image: isSafeImageUrl(item.imageUrl) ? item.imageUrl : null,
            quantity: item.quantity,
            lineTotal: Number(item.lineTotal) || 0,
            available: item.available === true
        };

    });


    const subtotal = cart ? Number(cart.subtotal) || 0 : 0;

    const hasPayableItems = items.some(function (item) {
        return item.available;
    });

    const shippingFee =
        !hasPayableItems ? 0 :
            (subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : DEFAULT_SHIPPING_FEE);


    return {
        items: items,
        totalQuantity: cart ? cart.totalQuantity || 0 : 0,
        subtotal: subtotal,
        shippingFee: shippingFee,
        total: subtotal + shippingFee,
        hasUnavailableItems: Boolean(cart && cart.hasUnavailableItems)
    };

}


/* Giỏ hàng hiện tại; chưa đăng nhập → giỏ rỗng (không gọi API) */

async function getCartSummary() {

    if (!isLoggedIn()) {
        return emptyCartSummary();
    }

    await migrateLocalCart();

    return toCartSummary(await apiRequest("/cart", { auth: true }));

}


/*
 * Thêm một phiên bản vào giỏ (cộng dồn vào dòng cũ, tối đa 10 / dòng).
 * snapshot chỉ cần variantId; các trường khác (tên, giá…) do server trả về.
 */

async function addCartItem(snapshot, quantity) {

    const amount = Number.isInteger(quantity) && quantity > 0 ? quantity : 1;

    await migrateLocalCart();

    const cart = await apiRequest("/cart/items", {
        method: "POST",
        auth: true,
        body: { variantId: snapshot.variantId, quantity: Math.min(amount, MAX_CART_LINE_QUANTITY) }
    });

    return toCartSummary(cart);

}


/* quantity <= 0 xoá dòng; tối đa MAX_CART_LINE_QUANTITY. productId giữ lại cho tương thích F2. */

async function updateCartItemQuantity(productId, variantId, quantity) {

    if (quantity <= 0) {
        return removeCartItem(productId, variantId);
    }

    const cart = await apiRequest("/cart/items/" + encodeURIComponent(variantId), {
        method: "PUT",
        auth: true,
        body: { quantity: Math.min(quantity, MAX_CART_LINE_QUANTITY) }
    });

    const summary = toCartSummary(cart);

    setCartCount(summary.totalQuantity);

    return summary;

}


async function removeCartItem(productId, variantId) {

    const cart = await apiRequest("/cart/items/" + encodeURIComponent(variantId), {
        method: "DELETE",
        auth: true
    });

    const summary = toCartSummary(cart);

    setCartCount(summary.totalQuantity);

    return summary;

}


async function clearCart() {

    const summary = toCartSummary(await apiRequest("/cart", { method: "DELETE", auth: true }));

    setCartCount(summary.totalQuantity);

    return summary;

}


async function getCartCount() {

    return (await getCartSummary()).totalQuantity;

}


/* Số trên icon giỏ hàng (header) */

function setCartCount(count) {

    document.querySelectorAll(".cart-count").forEach(function (element) {
        element.textContent = count;
    });

}


/* Đọc lại giỏ từ server rồi cập nhật icon; lỗi (mất mạng, hết phiên) → hiện 0 */

async function updateCartCount() {

    try {

        setCartCount(await getCartCount());

    } catch (error) {

        setCartCount(0);

    }

}


/* Ảnh của một dòng giỏ / đơn hàng: ảnh đã lưu (http/https) hoặc ảnh mặc định */

function cartItemImageUrl(item) {

    return item && isSafeImageUrl(item.image)
        ? item.image
        : siteUrl(DEFAULT_FALLBACK_IMAGE);

}


/* ================= CHUYỂN GIỎ CŨ TRÊN TRÌNH DUYỆT LÊN SERVER ================= */

/*
 * F2 lưu giỏ ở localStorage "poy_cart_<userId>". Lần đầu đọc / ghi giỏ sau khi
 * đăng nhập: gửi từng dòng lên POST /cart/items (server cộng dồn, tối đa 10),
 * xoá dòng đã chuyển khỏi localStorage, rồi báo bằng toast. Dòng không có
 * phiên bản hoặc sản phẩm đã ngừng bán thì bỏ qua. Mất mạng / hết phiên → dừng,
 * giữ các dòng còn lại để lần sau chuyển tiếp. Chỉ chạy một lần mỗi trang.
 */

let localCartMigration = null;


function migrateLocalCart() {

    if (!localCartMigration) {
        localCartMigration = runLocalCartMigration();
    }

    return localCartMigration;

}


async function runLocalCartMigration() {

    const user = isLoggedIn() ? getCurrentUser() : null;

    if (!user || user.id === undefined || user.id === null) {
        return;
    }


    const key = CART_STORAGE_KEY + "_" + user.id;

    let items;

    try {

        items = JSON.parse(localStorage.getItem(key) || "null");

    } catch (error) {

        items = [];

    }

    if (items === null) {
        return;
    }


    let remaining = Array.isArray(items) ? items.slice() : [];

    let moved = 0;

    let skipped = 0;


    while (remaining.length > 0) {

        const item = remaining[0];

        const quantity = item && Number.isInteger(item.quantity) && item.quantity > 0 ? item.quantity : 0;


        if (item && item.variantId !== undefined && item.variantId !== null && quantity > 0) {

            try {

                await apiRequest("/cart/items", {
                    method: "POST",
                    auth: true,
                    body: { variantId: item.variantId, quantity: Math.min(quantity, MAX_CART_LINE_QUANTITY) }
                });

                moved += 1;

            } catch (error) {

                /* Mất mạng / hết phiên / tài khoản bị khoá: giữ phần còn lại cho lần sau */
                if (error.status === 0 || error.status === 401 || error.status === 403) {
                    break;
                }

                skipped += 1;

            }

        } else {

            skipped += 1;

        }


        remaining = remaining.slice(1);

        try {
            localStorage.setItem(key, JSON.stringify(remaining));
        } catch (error) {
            /* localStorage bị chặn: bỏ qua */
        }

    }


    if (remaining.length === 0) {

        try {
            localStorage.removeItem(key);
        } catch (error) {
            /* localStorage bị chặn: bỏ qua */
        }

    }


    if ((moved > 0 || skipped > 0) && typeof showToast === "function") {

        showToast((
            (moved > 0 ? "Đã chuyển " + moved + " sản phẩm từ giỏ hàng cũ lên tài khoản của bạn. " : "") +
            (skipped > 0 ? skipped + " sản phẩm không còn bán nên đã được bỏ qua." : "")
        ).trim());

    }

}


/*
 * Giỏ hàng kiểu cũ (trước F2) lưu ở "poy_cart" theo TÊN sản phẩm, không có
 * phiên bản nên không chuyển lên server được: xoá đi và báo một lần.
 */

function migrateLegacyCart() {

    try {

        const legacy = localStorage.getItem(CART_STORAGE_KEY);

        if (legacy === null) {
            return;
        }

        localStorage.removeItem(CART_STORAGE_KEY);


        const items = JSON.parse(legacy);

        if (Array.isArray(items) && items.length > 0 && typeof showToast === "function") {

            showToast(
                "Giỏ hàng đã được nâng cấp để gắn với tài khoản của bạn. " +
                "Vui lòng thêm lại các sản phẩm trước đó."
            );

        }

    } catch (error) {
        /* dữ liệu cũ hỏng hoặc localStorage bị chặn: bỏ qua */
    }

}
