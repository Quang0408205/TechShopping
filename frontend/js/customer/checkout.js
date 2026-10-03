/* ================= TRANG THANH TOÁN (Phase 4) ================= */

/*
 * Chỉ dành cho người đã đăng nhập.
 * - Điền sẵn họ tên / số điện thoại / địa chỉ từ API tài khoản thật
 *   (GET /users/me, GET /users/me/profile); lỗi thì bỏ qua, người dùng tự nhập.
 * - Tóm tắt lấy từ giỏ trên server (/api/v1/cart), kể cả phí ship. Có dòng
 *   ngừng bán thì không cho đặt hàng (trang giỏ hàng yêu cầu xoá trước).
 * - Đặt hàng: POST /api/v1/orders (placeOrder, js/core/order-store.js) chỉ gửi
 *   người nhận / địa chỉ / ghi chú / phương thức thanh toán; server tạo đơn từ
 *   giỏ, xoá giỏ, rồi chuyển sang trang chi tiết đơn.
 * Mọi dữ liệu đưa vào innerHTML đều escape.
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        if (typeof layoutReady !== "undefined") {
            await layoutReady;
        }

        if (!isLoggedIn()) {

            redirectToLogin();

            return;

        }


        const emptyBox = document.getElementById("checkoutEmpty");

        const content = document.getElementById("checkoutContent");

        const form = document.getElementById("checkoutForm");

        const installmentNote = document.getElementById("installmentNote");


        const FIELD_MESSAGES = {
            recipientName:
                "Vui lòng nhập họ và tên người nhận (tối đa 120 ký tự).",
            recipientPhone:
                "Vui lòng nhập số điện thoại hợp lệ (8–20 ký tự số).",
            shippingAddress:
                "Vui lòng nhập địa chỉ nhận hàng đầy đủ."
        };

        const UNAVAILABLE_MESSAGE =
            "Giỏ hàng có sản phẩm đã ngừng bán. Vui lòng quay lại giỏ hàng để xóa trước khi đặt hàng.";


        let summary;

        try {

            summary = await getCartSummary();

        } catch (error) {

            if (!isLoggedIn()) {

                redirectToLogin();

                return;

            }

            emptyBox.hidden = false;

            showToast(getErrorMessage(error), "error");

            return;

        }


        setCartCount(summary.totalQuantity);

        if (summary.items.length === 0) {

            emptyBox.hidden = false;

            return;

        }


        content.hidden = false;

        renderSummary(summary);

        prefillFromAccount();


        if (summary.hasUnavailableItems) {

            showFormError(UNAVAILABLE_MESSAGE);

            document.getElementById("checkoutSubmitBtn").disabled = true;

        }


        document.querySelectorAll('input[name="paymentMethod"]').forEach(function (radio) {

            radio.addEventListener("change", function () {
                installmentNote.hidden = radio.value !== "INSTALLMENT" || !radio.checked;
            });

        });


        form.addEventListener("submit", handleSubmit);


        /* Chỉ điền vào ô còn trống, không ghi đè thứ người dùng đã gõ */

        async function prefillFromAccount() {

            const user = getCurrentUser() || {};

            fillIfEmpty("recipientName", user.fullname);


            const results = await Promise.allSettled([
                apiRequest("/users/me", { auth: true }),
                apiRequest("/users/me/profile", { auth: true })
            ]);


            if (results[0].status === "fulfilled" && results[0].value) {

                fillIfEmpty("recipientName", results[0].value.fullname);

                fillIfEmpty("recipientPhone", results[0].value.phone);

            }


            if (results[1].status === "fulfilled" && results[1].value) {

                const profile = results[1].value;

                const address = profile.defaultShippingAddress ||
                    [profile.address, profile.ward, profile.district, profile.city]
                        .filter(function (part) { return part && String(part).trim(); })
                        .join(", ");

                fillIfEmpty("shippingAddress", address);

            }

        }


        function fillIfEmpty(id, value) {

            const input = document.getElementById(id);

            if (input && !input.value.trim() && value) {
                input.value = value;
            }

        }


        function renderSummary(cartSummary) {

            const itemsBox = document.getElementById("checkoutItems");

            itemsBox.innerHTML = cartSummary.items.map(function (item) {

                return `
                    <div class="checkout-item">
                        <img src="${escapeHtml(cartItemImageUrl(item))}" alt="${escapeHtml(item.name)}">
                        <div class="checkout-item-info">
                            <p class="checkout-item-name">${escapeHtml(item.name)}</p>
                            <p class="checkout-item-variant">${escapeHtml(item.variantLabel ? item.variantLabel + " × " : "× ")}${item.quantity}</p>
                        </div>
                        <span class="checkout-item-price">${formatPrice(item.lineTotal)}</span>
                    </div>
                `;

            }).join("");


            document.getElementById("checkoutSubtotal").textContent = formatPrice(cartSummary.subtotal);

            document.getElementById("checkoutShipping").textContent =
                cartSummary.shippingFee === 0 ? "Miễn phí" : formatPrice(cartSummary.shippingFee);

            document.getElementById("checkoutTotal").textContent = formatPrice(cartSummary.total);

        }


        async function handleSubmit(event) {

            event.preventDefault();

            clearMessages();


            const data = {
                recipientName: valueOf("recipientName").trim(),
                recipientPhone: valueOf("recipientPhone").trim(),
                shippingAddress: valueOf("shippingAddress").trim(),
                note: valueOf("orderNote").trim(),
                paymentMethod: document.querySelector('input[name="paymentMethod"]:checked').value
            };


            const invalid = [];

            if (!data.recipientName || data.recipientName.length > 120) {
                invalid.push("recipientName");
            }

            if (!/^[0-9+\s().-]{8,20}$/.test(data.recipientPhone)) {
                invalid.push("recipientPhone");
            }

            if (!data.shippingAddress) {
                invalid.push("shippingAddress");
            }


            if (invalid.length > 0) {

                invalid.forEach(function (field) {
                    showFieldError(field, FIELD_MESSAGES[field]);
                });

                showFormError("Vui lòng kiểm tra lại thông tin giao hàng.");

                return;

            }


            const button = document.getElementById("checkoutSubmitBtn");

            button.disabled = true;

            button.textContent = "ĐANG XỬ LÝ...";


            function restoreButton() {

                button.disabled = false;

                button.textContent = "ĐẶT HÀNG";

            }


            /*
             * Server đặt hàng từ giỏ của tài khoản (giá, phí ship tính lại lúc đặt)
             * và xoá giỏ. Nút đã khoá nên không gửi 2 lần; nếu vẫn trùng (2 tab),
             * lần sau nhận CART_EMPTY.
             */
            let order;

            try {

                order = await placeOrder({
                    recipientName: data.recipientName,
                    recipientPhone: data.recipientPhone,
                    shippingAddress: data.shippingAddress,
                    note: data.note || null,
                    paymentMethod: data.paymentMethod
                });

            } catch (error) {

                await handlePlaceOrderError(error);

                return;

            }


            setCartCount(0);

            window.location.href = getOrderDetailUrl(order.id, true);


            async function handlePlaceOrderError(error) {

                if (!isLoggedIn()) {

                    restoreButton();

                    showFormError("Phiên đăng nhập đã hết, vui lòng đăng nhập lại.");

                    return;

                }


                /* Sai dữ liệu: hiện lỗi ở đúng ô (backend trả tên trường) */
                if (error.code === "VALIDATION_ERROR" && error.details) {

                    Object.keys(error.details).forEach(function (field) {
                        showFieldError(field, FIELD_MESSAGES[field] || error.details[field]);
                    });

                }

                showFormError(getErrorMessage(error));


                /* Giỏ đã đổi (trống / có sản phẩm ngừng bán): vẽ lại tóm tắt từ server */
                if (error.code === "CART_EMPTY" || error.code === "PRODUCT_NOT_AVAILABLE") {

                    try {

                        const fresh = await getCartSummary();

                        setCartCount(fresh.totalQuantity);

                        if (fresh.items.length > 0) {
                            renderSummary(fresh);
                        }

                        if (fresh.hasUnavailableItems) {

                            showFormError(UNAVAILABLE_MESSAGE);

                            button.textContent = "ĐẶT HÀNG";

                            return;

                        }

                        if (fresh.items.length === 0) {

                            button.textContent = "ĐẶT HÀNG";

                            return;

                        }

                    } catch (reloadError) {
                        /* giữ thông báo lỗi đặt hàng */
                    }

                }

                restoreButton();

            }

        }


        function valueOf(id) {

            return document.getElementById(id).value;

        }


        function showFieldError(field, message) {

            const element = form.querySelector(`.field-error[data-field="${field}"]`);

            if (element) {
                element.textContent = message;
            }

        }


        function showFormError(message) {

            const box = form.querySelector('[data-role="error"]');

            box.textContent = message;

            box.hidden = false;

        }


        function clearMessages() {

            form.querySelectorAll(".field-error").forEach(function (element) {
                element.textContent = "";
            });

            const box = form.querySelector('[data-role="error"]');

            box.textContent = "";

            box.hidden = true;

        }

    }
);
