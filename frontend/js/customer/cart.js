/* ================= TRANG GIỎ HÀNG (Phase 3) ================= */

/*
 * Giao diện lấy từ bản frontend mới; dữ liệu là giỏ trên server
 * (js/core/cart-store.js → /api/v1/cart). Chưa đăng nhập → mời đăng nhập.
 * Dòng đã ngừng bán: không tính tiền, chỉ còn nút xoá, và chặn thanh toán
 * cho tới khi xoá hết. Mọi dữ liệu đưa vào innerHTML đều escape.
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        if (typeof layoutReady !== "undefined") {
            await layoutReady;
        }

        loadCart();

    }
);


function getCartContainer() {

    return document.getElementById("cartContainer");

}


async function loadCart() {

    const container = getCartContainer();


    if (!isLoggedIn()) {

        renderLoginPrompt();

        return;

    }


    container.innerHTML = `
        <div class="state-box">
            <p>Đang tải giỏ hàng...</p>
        </div>
    `;


    let summary;

    try {

        summary = await getCartSummary();

    } catch (error) {

        /* refresh thất bại → api.js đã xoá phiên */
        if (!isLoggedIn()) {

            renderLoginPrompt();

            return;

        }

        container.innerHTML = errorStateHtml(getErrorMessage(error), "cartRetryBtn");

        document.getElementById("cartRetryBtn").addEventListener("click", loadCart);

        return;

    }


    setCartCount(summary.totalQuantity);

    renderCart(summary);

}


function renderLoginPrompt() {

    getCartContainer().innerHTML = `
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

}


function renderCart(summary) {

    const container = getCartContainer();


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

        const variantAttribute = `data-variant-id="${escapeHtml(String(item.variantId))}"`;

        const unitPrice = item.available
            ? formatPrice(item.price) +
                (item.oldPrice ? `<span class="cart-old-price">${formatPrice(item.oldPrice)}</span>` : "")
            : `<span class="cart-unavailable-tag">Ngừng bán</span>`;

        return `
            <tr class="${item.available ? "" : "cart-row-unavailable"}">
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
                        <button type="button" data-action="decrease" ${variantAttribute} aria-label="Giảm số lượng"
                            ${item.available ? "" : "disabled"}>
                            −
                        </button>
                        <span>
                            ${item.quantity}
                        </span>
                        <button type="button" data-action="increase" ${variantAttribute} aria-label="Tăng số lượng"
                            ${!item.available || item.quantity >= MAX_CART_LINE_QUANTITY ? "disabled" : ""}>
                            +
                        </button>
                    </div>
                </td>

                <td>
                    ${unitPrice}
                </td>

                <td>
                    ${item.available ? formatPrice(item.lineTotal) : "—"}
                </td>

                <td>
                    <button type="button" class="remove-btn" data-action="remove" ${variantAttribute}>
                        Xóa
                    </button>
                </td>
            </tr>
        `;

    }).join("");


    const checkoutAction = summary.hasUnavailableItems
        ? `
            <p class="cart-unavailable-note" id="cartUnavailableNote">
                Giỏ hàng có sản phẩm đã ngừng bán. Vui lòng xóa các sản phẩm này trước khi thanh toán.
            </p>
            <button type="button" class="btn btn-dark" id="checkoutButton" disabled>
                TIẾN HÀNH THANH TOÁN
            </button>
        `
        : `
            <a
                href="${escapeHtml(siteUrl("customer/checkout.html"))}"
                class="btn btn-dark"
                id="checkoutButton"
            >
                TIẾN HÀNH THANH TOÁN
            </a>
        `;


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
                ${checkoutAction}
            </div>
        </div>
    `;


    bindCartEvents(summary.items);

}


function bindCartEvents(items) {

    const container = getCartContainer();


    function findItem(button) {

        return items.find(function (item) {
            return String(item.variantId) === button.dataset.variantId;
        });

    }


    /*
     * Gọi API rồi vẽ lại bằng giỏ server trả về. Trong lúc chờ khoá mọi nút để
     * không bấm trùng; lỗi (vd. dòng đã bị xoá ở tab khác) → toast + tải lại giỏ.
     */

    async function runAction(request, successMessage) {

        container.querySelectorAll("[data-action]").forEach(function (button) {
            button.disabled = true;
        });


        try {

            renderCart(await request());

            if (successMessage) {
                showToast(successMessage);
            }

        } catch (error) {

            if (isLoggedIn()) {
                showToast(getErrorMessage(error), "error");
            }

            loadCart();

        }

    }


    container.querySelectorAll('[data-action="decrease"]').forEach(function (button) {

        button.addEventListener("click", function () {

            const item = findItem(button);

            if (item) {

                runAction(function () {
                    return updateCartItemQuantity(item.productId, item.variantId, item.quantity - 1);
                });

            }

        });

    });


    container.querySelectorAll('[data-action="increase"]').forEach(function (button) {

        button.addEventListener("click", function () {

            const item = findItem(button);

            if (item) {

                runAction(function () {
                    return updateCartItemQuantity(item.productId, item.variantId, item.quantity + 1);
                });

            }

        });

    });


    container.querySelectorAll('[data-action="remove"]').forEach(function (button) {

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

                    runAction(function () {
                        return removeCartItem(item.productId, item.variantId);
                    }, "Đã xóa sản phẩm khỏi giỏ hàng.");

                }
            });

        });

    });

}
