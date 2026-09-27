/* ================= GIỎ HÀNG (F2): SNAPSHOT THEO TÀI KHOẢN ================= */

/*
 * Mỗi dòng giỏ hàng là một "snapshot" lấy từ API lúc thêm:
 *   { productId, variantId, name, variantLabel, price, image, quantity }
 * Khoá dòng = productId + variantId (variantId = null khi sản phẩm không có
 * phiên bản). Không tra catalogue giả (mock-data.js) như bản frontend mới.
 *
 * Giỏ hàng gắn với TÀI KHOẢN: lưu ở localStorage "poy_cart_<userId>", nên
 * mỗi người đăng nhập trên cùng trình duyệt có giỏ riêng; chưa đăng nhập thì
 * giỏ rỗng và không thêm được (main.js chuyển tới trang đăng nhập).
 *
 * Thứ tự nạp: api.js → ui.js → cart-store.js → layout.js → main.js.
 * CHỜ BACKEND (Phase 3 – Cart): các hàm dưới đây sẽ gọi apiRequest("/cart…")
 * với cùng hình dạng dữ liệu, nên trang giỏ hàng / thanh toán không phải viết lại.
 */

/* Phí vận chuyển mô phỏng (backend sẽ quyết định thật) */
const FREE_SHIPPING_THRESHOLD = 10000000;

const DEFAULT_SHIPPING_FEE = 30000;

/* Tối đa mỗi dòng (trang chi tiết cũng cho chọn 1–10) */
const MAX_CART_LINE_QUANTITY = 10;


/* Khoá localStorage của giỏ hàng người đang đăng nhập, null nếu chưa đăng nhập */

function getCartStorageKey() {

    const user = isLoggedIn() ? getCurrentUser() : null;

    return user && user.id !== undefined && user.id !== null
        ? CART_STORAGE_KEY + "_" + user.id
        : null;

}


function getCartItems() {

    const key = getCartStorageKey();

    if (!key) {
        return [];
    }


    try {

        const items = JSON.parse(localStorage.getItem(key) || "[]");

        return Array.isArray(items) ? items.filter(isValidCartItem) : [];

    } catch (error) {

        return [];

    }

}


function isValidCartItem(item) {

    return Boolean(item) &&
        item.productId !== undefined && item.productId !== null &&
        typeof item.name === "string" &&
        Number(item.price) > 0 &&
        Number.isInteger(item.quantity) && item.quantity > 0;

}


function saveCartItems(items) {

    const key = getCartStorageKey();

    if (!key) {
        return;
    }

    localStorage.setItem(key, JSON.stringify(items));

}


function isSameCartLine(item, productId, variantId) {

    return String(item.productId) === String(productId) &&
        String(item.variantId) === String(variantId === undefined ? null : variantId);

}


/*
 * Thêm snapshot vào giỏ (gộp dòng trùng productId + variantId).
 * Trả về số lượng mới của dòng, hoặc 0 nếu không thêm được (chưa đăng nhập,
 * dữ liệu không hợp lệ).
 */

function addCartItem(snapshot, quantity) {

    if (!getCartStorageKey()) {
        return 0;
    }


    const amount = Number.isInteger(quantity) && quantity > 0 ? quantity : 1;

    const items = getCartItems();

    const existing = items.find(function (item) {
        return isSameCartLine(item, snapshot.productId, snapshot.variantId);
    });


    if (existing) {

        existing.quantity = Math.min(existing.quantity + amount, MAX_CART_LINE_QUANTITY);

        /* Cập nhật giá / ảnh theo lần thêm mới nhất */
        existing.price = snapshot.price;

        existing.image = snapshot.image || existing.image || null;

        saveCartItems(items);

        return existing.quantity;

    }


    const item = {
        productId: snapshot.productId,
        variantId: snapshot.variantId === undefined ? null : snapshot.variantId,
        name: snapshot.name,
        variantLabel: snapshot.variantLabel || "",
        price: Number(snapshot.price),
        image: isSafeImageUrl(snapshot.image) ? snapshot.image : null,
        quantity: Math.min(amount, MAX_CART_LINE_QUANTITY)
    };

    if (!isValidCartItem(item)) {
        return 0;
    }

    items.push(item);

    saveCartItems(items);

    return item.quantity;

}


/* quantity <= 0 xoá dòng; tối đa MAX_CART_LINE_QUANTITY */

function updateCartItemQuantity(productId, variantId, quantity) {

    let items = getCartItems();


    if (quantity <= 0) {

        items = items.filter(function (item) {
            return !isSameCartLine(item, productId, variantId);
        });

    } else {

        items.forEach(function (item) {

            if (isSameCartLine(item, productId, variantId)) {
                item.quantity = Math.min(quantity, MAX_CART_LINE_QUANTITY);
            }

        });

    }


    saveCartItems(items);

    updateCartCount();

}


function removeCartItem(productId, variantId) {

    updateCartItemQuantity(productId, variantId, 0);

}


function clearCart() {

    saveCartItems([]);

    updateCartCount();

}


function getCartCount() {

    return getCartItems().reduce(function (sum, item) {
        return sum + item.quantity;
    }, 0);

}


/* Dòng giỏ hàng kèm thành tiền + tổng đơn, dùng cho trang giỏ hàng / thanh toán */

function getCartSummary() {

    const items = getCartItems().map(function (item) {
        return Object.assign({}, item, { lineTotal: item.price * item.quantity });
    });

    const subtotal = items.reduce(function (sum, item) {
        return sum + item.lineTotal;
    }, 0);

    const shippingFee =
        items.length === 0 ? 0 :
            (subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : DEFAULT_SHIPPING_FEE);

    return {
        items: items,
        subtotal: subtotal,
        shippingFee: shippingFee,
        total: subtotal + shippingFee
    };

}


function updateCartCount() {

    const count = getCartCount();

    document.querySelectorAll(".cart-count").forEach(function (element) {
        element.textContent = count;
    });

}


/* Ảnh của một dòng giỏ / đơn hàng: ảnh đã lưu (http/https) hoặc ảnh mặc định */

function cartItemImageUrl(item) {

    return item && isSafeImageUrl(item.image)
        ? item.image
        : siteUrl(DEFAULT_FALLBACK_IMAGE);

}


/*
 * Giỏ hàng kiểu cũ (trước F2) lưu ở "poy_cart" theo TÊN sản phẩm, không có
 * productId nên không chuyển sang snapshot được: xoá đi và báo một lần.
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
