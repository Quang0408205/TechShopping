/* ================= TRANG GIỎ HÀNG ================= */

/*
 * Giao diện lấy từ bản frontend mới; dữ liệu là giỏ trên server
 * (js/core/cart-store.js → /api/v1/cart). Chưa đăng nhập → mời đăng nhập.
 * Dòng đã ngừng bán: không tính tiền, chỉ còn nút xoá, và chặn thanh toán
 * cho tới khi xoá hết. Mọi dữ liệu đưa vào innerHTML đều escape.
 * Có đơn chuyển khoản / thẻ còn chờ tiền → khung nhắc phía trên giỏ
 * (awaiting-payment.js); "Huỷ đơn & lấy lại giỏ" xong thì tải lại giỏ.
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        if (typeof layoutReady !== "undefined") {
            await layoutReady;
        }

        loadCart();

        showAwaitingPaymentNotice(document.getElementById("awaitingPayment"), function () {

            document.getElementById("awaitingPayment").hidden = true;

            loadCart();

        });

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
                Vui lòng đăng nhập
            </h2>
            <p>
                Giỏ hàng được lưu theo tài khoản của bạn.
            </p>
            <br>
            <button type="button" class="btn btn-dark" id="cartLoginBtn">
                Đăng nhập
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
                    Giỏ hàng đang trống
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
                    Khám phá sản phẩm
                </a>
            </div>
        `;

        return;

    }


    const rows = summary.items.map(function (item) {

        const detailUrl = escapeHtml(getProductDetailUrl(item.productId));

        const variantAttribute = `data-variant-id="${escapeHtml(String(item.variantId))}"`;

        /* Thiếu hàng ≠ ngừng bán; dòng thiếu hàng vẫn giảm được số lượng */
        const stockShort = !item.available && item.availableQuantity !== null && item.availableQuantity < item.quantity;

        const unavailableTag = !stockShort
            ? "Ngừng bán"
            : item.availableQuantity === 0 ? "Hết hàng" : "Chỉ còn " + item.availableQuantity;

        const unitPrice = item.available
            ? formatPrice(item.price) +
                (item.oldPrice ? `<span class="cart-old-price">${formatPrice(item.oldPrice)}</span>` : "")
            : `<span class="cart-unavailable-tag">${escapeHtml(unavailableTag)}</span>`;

        const canIncrease = item.available && item.quantity < MAX_CART_LINE_QUANTITY
            && (item.availableQuantity === null || item.quantity < item.availableQuantity);

        return `
            <tr class="${item.available ? "" : "cart-row-unavailable"}">
                <td class="cart-product-cell">
                    <img
                        src="${escapeHtml(cartItemImageUrl(item))}"
                        alt="${escapeHtml(item.name)}"
                        referrerpolicy="no-referrer"
                        class="cart-thumb"
                    >
                    <div>
                        <a href="${detailUrl}" class="cart-product">
                            ${escapeHtml(item.name)}
                        </a>
                        <span class="cart-variant">${escapeHtml(item.variantLabel)}</span>
                        ${item.available && item.comboDiscount > 0 ? comboTagHtml(item) : ""}
                    </div>
                </td>

                <td>
                    <div class="quantity-control">
                        <button type="button" data-action="decrease" ${variantAttribute} aria-label="Giảm số lượng"
                            ${item.available || (stockShort && item.quantity > 1) ? "" : "disabled"}>
                            −
                        </button>
                        <span>
                            ${item.quantity}
                        </span>
                        <button type="button" data-action="increase" ${variantAttribute} aria-label="Tăng số lượng"
                            ${canIncrease ? "" : "disabled"}>
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
                Giỏ hàng có sản phẩm đã ngừng bán hoặc không đủ hàng. Vui lòng xóa hoặc giảm số lượng các sản phẩm này trước khi thanh toán.
            </p>
            <button type="button" class="btn btn-dark" id="checkoutButton" disabled>
                Đặt mua
            </button>
        `
        : `
            <button type="button" class="order-info-row" id="orderInfoRow" aria-label="Thông tin nhận hàng">
                <span>${orderInfoSummaryHtml(readOrderInfo())}</span>
                <span aria-hidden="true">›</span>
            </button>
            <a
                href="${escapeHtml(siteUrl("customer/checkout.html"))}"
                class="btn btn-dark"
                id="checkoutButton"
            >
                Đặt mua
            </a>
        `;


    container.innerHTML = `
        <table class="cart-table">
            <thead>
                <tr>
                    <th>
                        Sản phẩm
                    </th>
                    <th>
                        Số lượng
                    </th>
                    <th>
                        Đơn giá
                    </th>
                    <th>
                        Thành tiền
                    </th>
                    <th><span class="visually-hidden">Xoá</span></th>
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
                ${summary.comboDiscount > 0 ? `
                <div class="cart-total-row cart-total-combo">
                    <span>
                        Giảm combo
                    </span>
                    <strong>
                        −${formatPrice(summary.comboDiscount)}
                    </strong>
                </div>` : ""}
                <div class="cart-total-row">
                    <span>
                        Phí vận chuyển
                    </span>
                    <strong>
                        ${summary.shippingFee === 0 ? "Miễn phí" : formatPrice(summary.shippingFee)}
                    </strong>
                </div>
                <div class="cart-total-row cart-total-final">
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


/* Nhãn "Combo" dưới tên món mua kèm được giảm */
function comboTagHtml(item) {

    return `<span class="combo-tag" title="${escapeHtml(item.comboNames.join(", "))}">Combo −${escapeHtml(formatPrice(item.comboDiscount))}</span>`;

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


    /*
     * Thông tin nhận hàng: bấm dòng thông tin để mở / sửa popup; "Đặt mua" chỉ chuyển sang bước thanh toán
     * khi đã có thông tin hợp lệ, chưa có thì mở popup trước rồi mới chuyển.
     */

    const infoRow = container.querySelector("#orderInfoRow");

    const checkoutButton = container.querySelector("#checkoutButton");

    if (infoRow) {

        infoRow.addEventListener("click", function () {

            openOrderInfoModal({
                onSave: function (info) {
                    infoRow.firstElementChild.innerHTML = orderInfoSummaryHtml(info);
                }
            });

        });

    }

    if (checkoutButton && checkoutButton.tagName === "A") {

        checkoutButton.addEventListener("click", function (event) {

            if (readOrderInfo()) {
                return;
            }

            event.preventDefault();

            openOrderInfoModal({
                onSave: function () {
                    window.location.href = checkoutButton.href;
                }
            });

        });

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
