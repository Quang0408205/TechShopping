/* ================= TRANG THANH TOÁN (F2, mô phỏng) ================= */

/*
 * Giao diện lấy từ bản frontend mới. Chỉ dành cho người đã đăng nhập.
 * - Điền sẵn họ tên / số điện thoại / địa chỉ từ API tài khoản thật
 *   (GET /users/me, GET /users/me/profile); lỗi thì bỏ qua, người dùng tự nhập.
 * - Đặt hàng: chưa có backend Order (Phase 4) → lưu đơn theo tài khoản bằng
 *   createLocalOrder() (js/core/order-store.js), rồi xoá giỏ hàng.
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


        const summary = getCartSummary();

        if (summary.items.length === 0) {

            emptyBox.hidden = false;

            return;

        }


        content.hidden = false;

        renderSummary(summary);

        prefillFromAccount();


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


        function handleSubmit(event) {

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


            const cartSummary = getCartSummary();

            if (cartSummary.items.length === 0) {

                showFormError("Giỏ hàng của bạn đang trống.");

                return;

            }


            const button = document.getElementById("checkoutSubmitBtn");

            button.disabled = true;

            button.textContent = "ĐANG XỬ LÝ...";


            /* Độ trễ giả lập thời gian xử lý đơn ở backend */

            setTimeout(function () {

                const order = createLocalOrder({
                    recipientName: data.recipientName,
                    recipientPhone: data.recipientPhone,
                    shippingAddress: data.shippingAddress,
                    note: data.note,
                    paymentMethod: data.paymentMethod,
                    items: cartSummary.items.map(function (item) {
                        return {
                            productId: item.productId,
                            variantId: item.variantId,
                            name: item.name,
                            variantLabel: item.variantLabel,
                            image: item.image,
                            price: item.price,
                            quantity: item.quantity
                        };
                    }),
                    subtotal: cartSummary.subtotal,
                    shippingFee: cartSummary.shippingFee,
                    total: cartSummary.total
                });


                if (!order) {

                    button.disabled = false;

                    button.textContent = "ĐẶT HÀNG";

                    showFormError("Phiên đăng nhập đã hết, vui lòng đăng nhập lại.");

                    return;

                }


                clearCart();

                window.location.href =
                    siteUrl("customer/order-detail.html?id=" + encodeURIComponent(order.id) + "&justPlaced=1");

            }, 700);

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
