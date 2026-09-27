/* ================= TRANG GIỎ HÀNG (F2) ================= */

/*
 * Giao diện lấy từ bản frontend mới; dữ liệu là snapshot trong
 * js/core/cart-store.js (giỏ hàng theo tài khoản). Chưa đăng nhập → mời
 * đăng nhập. Mọi dữ liệu đưa vào innerHTML đều escape.
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        if (typeof layoutReady !== "undefined") {
            await layoutReady;
        }

        renderCart();

    }
);


function cartLineAttributes(item) {

    /* String(): variantId null → "null", khớp với cách so sánh ở findItem / cart-store */
    return `data-product-id="${escapeHtml(String(item.productId))}" data-variant-id="${escapeHtml(String(item.variantId))}"`;

}


function renderCart() {

    const container =
        document.getElementById(
            "cartContainer"
        );


    if (!isLoggedIn()) {

        container.innerHTML = `
            <div class="empty-cart">
                <h2>
                    VUI LÒNG ĐĂNG NHẬP
                </h2>
                <p>
                    Giỏ hàng được lưu theo tài khoản của bạn.
                </p>
                <br>
                <button type="button" class="btn btn-dark" id="cartLoginBtn">
                    ĐĂNG NHẬP
                </button>
            </div>
        `;

        document.getElementById("cartLoginBtn")
            .addEventListener("click", function () {
                redirectToLogin();
            });

        return;

    }


    const summary = getCartSummary();


    if (summary.items.length === 0) {

        container.innerHTML = `
            <div class="empty-cart">
                <h2>
                    GIỎ HÀNG ĐANG TRỐNG
                </h2>
                <p>
                    Hãy khám phá các sản phẩm
                    của POY.
                </p>
                <br>
                <a
                    href="${escapeHtml(siteUrl("customer/products.html"))}"
                    class="btn btn-dark"
                >
                    KHÁM PHÁ SẢN PHẨM
                </a>
            </div>
        `;

        return;

    }


    const rows = summary.items.map(function (item) {

        const detailUrl = escapeHtml(getProductDetailUrl(item.productId));

        return `
            <tr>
                <td class="cart-product-cell">
                    <img
                        src="${escapeHtml(cartItemImageUrl(item))}"
                        alt="${escapeHtml(item.name)}"
                        class="cart-thumb"
                    >
                    <div>
                        <a href="${detailUrl}" class="cart-product">
                            ${escapeHtml(item.name)}
                        </a>
                        <span class="cart-variant">${escapeHtml(item.variantLabel)}</span>
                    </div>
                </td>

                <td>
                    <div class="quantity-control">
                        <button type="button" data-action="decrease" ${cartLineAttributes(item)} aria-label="Giảm số lượng">
                            −
                        </button>
                        <span>
                            ${item.quantity}
                        </span>
                        <button type="button" data-action="increase" ${cartLineAttributes(item)} aria-label="Tăng số lượng"
                            ${item.quantity >= MAX_CART_LINE_QUANTITY ? "disabled" : ""}>
                            +
                        </button>
                    </div>
                </td>

                <td>
                    ${formatPrice(item.price)}
                </td>

                <td>
                    ${formatPrice(item.lineTotal)}
                </td>

                <td>
                    <button type="button" class="remove-btn" data-action="remove" ${cartLineAttributes(item)}
                        data-name="${escapeHtml(item.name)}">
                        Xóa
                    </button>
                </td>
            </tr>
        `;

    }).join("");


    container.innerHTML = `
        <table class="cart-table">
            <thead>
                <tr>
                    <th>
                        SẢN PHẨM
                    </th>
                    <th>
                        SỐ LƯỢNG
                    </th>
                    <th>
                        ĐƠN GIÁ
                    </th>
                    <th>
                        THÀNH TIỀN
                    </th>
                    <th></th>
                </tr>
            </thead>
            <tbody>
                ${rows}
            </tbody>
        </table>

        <div class="cart-summary">
            <div class="cart-total">
                <div class="cart-total-row">
                    <span>
                        Tạm tính
                    </span>
                    <strong>
                        ${formatPrice(summary.subtotal)}
                    </strong>
                </div>
                <div class="cart-total-row">
                    <span>
                        Phí vận chuyển
                    </span>
                    <strong>
                        ${summary.shippingFee === 0 ? "Miễn phí" : formatPrice(summary.shippingFee)}
                    </strong>
                </div>
                <div class="cart-total-row">
                    <span>
                        Tổng cộng
                    </span>
                    <span class="cart-total-price">
                        ${formatPrice(summary.total)}
                    </span>
                </div>
                <a
                    href="${escapeHtml(siteUrl("customer/checkout.html"))}"
                    class="btn btn-dark"
                    id="checkoutButton"
                >
                    TIẾN HÀNH THANH TOÁN
                </a>
            </div>
        </div>
    `;


    bindCartEvents(summary.items);

}


function bindCartEvents(items) {

    function findItem(button) {

        return items.find(function (item) {
            return String(item.productId) === button.dataset.productId &&
                String(item.variantId) === button.dataset.variantId;
        });

    }


    document.querySelectorAll('[data-action="decrease"]').forEach(function (button) {

        button.addEventListener("click", function () {

            const item = findItem(button);

            if (item) {

                updateCartItemQuantity(item.productId, item.variantId, item.quantity - 1);

                renderCart();

            }

        });

    });


    document.querySelectorAll('[data-action="increase"]').forEach(function (button) {

        button.addEventListener("click", function () {

            const item = findItem(button);

            if (item) {

                updateCartItemQuantity(item.productId, item.variantId, item.quantity + 1);

                renderCart();

            }

        });

    });


    document.querySelectorAll('[data-action="remove"]').forEach(function (button) {

        button.addEventListener("click", function () {

            const item = findItem(button);

            if (!item) {
                return;
            }

            openConfirmModal({
                title: "Xóa sản phẩm khỏi giỏ hàng?",
                message: "\"" + item.name + "\" sẽ được xóa khỏi giỏ hàng của bạn.",
                confirmLabel: "XÓA",
                onConfirm: function () {

                    removeCartItem(item.productId, item.variantId);

                    showToast("Đã xóa sản phẩm khỏi giỏ hàng.");

                    renderCart();

                }
            });

        });

    });

}
