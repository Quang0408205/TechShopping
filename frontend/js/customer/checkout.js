/* ================= TRANG THANH TOÁN (Phase 4–5) ================= */

/*
 * Chỉ dành cho người đã đăng nhập.
 * - Điền sẵn họ tên / số điện thoại / địa chỉ từ API tài khoản thật
 *   (GET /users/me, GET /users/me/profile); lỗi thì bỏ qua, người dùng tự nhập.
 * - Tóm tắt lấy từ giỏ trên server (/api/v1/cart), kể cả phí ship. Có dòng
 *   ngừng bán thì không cho đặt hàng (trang giỏ hàng yêu cầu xoá trước).
 * - Trả góp (Phase 5): kỳ hạn, mức tối thiểu, độ dài CCCD và danh sách ngân hàng
 *   lấy từ GET /payments/installment-options; đơn dưới mức tối thiểu (hoặc không
 *   tải được) thì khoá lựa chọn trả góp. Tiền mỗi kỳ = tổng / số kỳ làm tròn
 *   xuống, kỳ cuối nhận phần lẻ (giống server).
 * - Đặt hàng: POST /api/v1/orders (placeOrder, js/core/order-store.js) gửi
 *   người nhận / địa chỉ / ghi chú / phương thức thanh toán (+ hồ sơ trả góp);
 *   server tạo đơn từ giỏ, xoá giỏ, rồi chuyển sang trang chi tiết đơn.
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

        const installmentRadio = form.querySelector('input[name="paymentMethod"][value="INSTALLMENT"]');

        const installmentMethod = document.getElementById("installmentMethod");

        const installmentHint = document.getElementById("installmentMethodHint");

        const installmentBox = document.getElementById("installmentBox");

        const monthsSelect = document.getElementById("installmentMonths");

        const citizenInput = document.getElementById("citizenId");

        const bankSelect = document.getElementById("cardBank");

        const preview = document.getElementById("installmentPreview");


        const FIELD_MESSAGES = {
            recipientName:
                "Vui lòng nhập họ và tên người nhận (tối đa 120 ký tự).",
            recipientPhone:
                "Vui lòng nhập số điện thoại hợp lệ (8–20 ký tự số).",
            shippingAddress:
                "Vui lòng nhập địa chỉ nhận hàng đầy đủ.",
            "installment.months":
                "Vui lòng chọn kỳ hạn trả góp.",
            "installment.cardBank":
                "Vui lòng chọn ngân hàng phát hành thẻ."
        };

        const UNAVAILABLE_MESSAGE =
            "Giỏ hàng có sản phẩm đã ngừng bán. Vui lòng quay lại giỏ hàng để xóa trước khi đặt hàng.";


        /* Tải song song với giỏ hàng; lỗi → null (khoá lựa chọn trả góp) */
        const optionsPromise = apiRequest("/payments/installment-options", { auth: true })
            .catch(function () { return null; });


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


        const installmentOptions = await optionsPromise;

        let currentTotal = summary.total;


        content.hidden = false;

        fillInstallmentChoices();

        renderSummary(summary);

        prefillFromAccount();


        if (summary.hasUnavailableItems) {

            showFormError(UNAVAILABLE_MESSAGE);

            document.getElementById("checkoutSubmitBtn").disabled = true;

        }


        form.querySelectorAll('input[name="paymentMethod"]').forEach(function (radio) {

            radio.addEventListener("change", updateInstallmentBox);

        });

        monthsSelect.addEventListener("change", renderPreview);

        /* CCCD chỉ gồm chữ số */
        citizenInput.addEventListener("input", function () {

            const digits = citizenInput.value.replace(/\D/g, "");

            if (digits !== citizenInput.value) {
                citizenInput.value = digits;
            }

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


            currentTotal = cartSummary.total;

            applyInstallmentEligibility();

        }


        /* ================= TRẢ GÓP ================= */

        function fillInstallmentChoices() {

            if (!installmentOptions) {
                return;
            }

            citizenInput.maxLength = installmentOptions.citizenIdLength;

            citizenInput.placeholder = installmentOptions.citizenIdLength + " chữ số";

            bankSelect.insertAdjacentHTML("beforeend", (installmentOptions.banks || []).map(function (bank) {
                return `<option value="${escapeHtml(bank.code)}">${escapeHtml(bank.name)}</option>`;
            }).join(""));

        }


        /* Khoá "Trả góp" khi không có thông tin trả góp hoặc tổng đơn dưới mức tối thiểu */

        function applyInstallmentEligibility() {

            let reason = "";

            if (!installmentOptions) {

                reason = "Hiện chưa tải được thông tin trả góp. Vui lòng chọn cách thanh toán khác hoặc tải lại trang.";

            } else if (currentTotal < Number(installmentOptions.minOrderTotal)) {

                reason = "Áp dụng cho đơn từ " + formatPrice(Number(installmentOptions.minOrderTotal))
                    + " (đơn của bạn: " + formatPrice(currentTotal) + ").";

            }


            installmentRadio.disabled = reason !== "";

            installmentMethod.classList.toggle("is-disabled", reason !== "");

            if (reason) {

                installmentHint.textContent = reason;

                if (installmentRadio.checked) {
                    form.querySelector('input[name="paymentMethod"][value="COD"]').checked = true;
                }

            } else {

                installmentHint.textContent = "Lãi suất 0%, không cần trả trước, kỳ hạn "
                    + installmentOptions.termsInMonths.join(", ") + " tháng.";

                renderMonthOptions();

            }

            updateInstallmentBox();

        }


        /* "6 tháng · 2.498.333đ/tháng"; giữ kỳ hạn đang chọn khi tổng tiền đổi */

        function renderMonthOptions() {

            const selected = monthsSelect.value;

            monthsSelect.innerHTML = `<option value="">-- Chọn kỳ hạn --</option>` +
                installmentOptions.termsInMonths.map(function (months) {
                    return `<option value="${months}">${months} tháng · ${formatPrice(monthlyPaymentOf(months))}/tháng</option>`;
                }).join("");

            monthsSelect.value = selected;

            renderPreview();

        }


        function renderPreview() {

            const months = Number(monthsSelect.value);

            if (!months) {

                preview.hidden = true;

                return;

            }

            const monthly = monthlyPaymentOf(months);

            const last = currentTotal - monthly * (months - 1);

            preview.textContent = "Trả " + formatPrice(monthly) + " mỗi tháng trong " + months + " tháng"
                + (last !== monthly ? " (kỳ cuối " + formatPrice(last) + ")" : "")
                + ". Tổng " + formatPrice(currentTotal) + ", lãi suất 0%.";

            preview.hidden = false;

        }


        function monthlyPaymentOf(months) {

            return Math.floor(currentTotal / months);

        }


        function updateInstallmentBox() {

            installmentBox.hidden = !installmentRadio.checked;

        }


        function selectedPaymentMethod() {

            return form.querySelector('input[name="paymentMethod"]:checked').value;

        }


        /* ================= ĐẶT HÀNG ================= */

        async function handleSubmit(event) {

            event.preventDefault();

            clearMessages();


            const data = {
                recipientName: valueOf("recipientName").trim(),
                recipientPhone: valueOf("recipientPhone").trim(),
                shippingAddress: valueOf("shippingAddress").trim(),
                note: valueOf("orderNote").trim(),
                paymentMethod: selectedPaymentMethod()
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


            let installment = null;

            if (data.paymentMethod === "INSTALLMENT") {

                installment = {
                    months: Number(monthsSelect.value) || null,
                    citizenId: citizenInput.value.trim(),
                    cardBank: bankSelect.value || null
                };

                if (!installment.months) {
                    invalid.push("installment.months");
                }

                const citizenIdLength = installmentOptions ? installmentOptions.citizenIdLength : 10;

                if (!new RegExp("^\\d{" + citizenIdLength + "}$").test(installment.citizenId)) {
                    FIELD_MESSAGES["installment.citizenId"] = "Số CCCD phải gồm đúng " + citizenIdLength + " chữ số.";
                    invalid.push("installment.citizenId");
                }

                if (!installment.cardBank) {
                    invalid.push("installment.cardBank");
                }

            }


            if (invalid.length > 0) {

                invalid.forEach(function (field) {
                    showFieldError(field, FIELD_MESSAGES[field]);
                });

                showFormError(invalid.some(function (field) { return field.indexOf("installment") === 0; })
                    && invalid.every(function (field) { return field.indexOf("installment") === 0; })
                    ? "Vui lòng kiểm tra lại thông tin trả góp."
                    : "Vui lòng kiểm tra lại thông tin giao hàng.");

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

                const input = {
                    recipientName: data.recipientName,
                    recipientPhone: data.recipientPhone,
                    shippingAddress: data.shippingAddress,
                    note: data.note || null,
                    paymentMethod: data.paymentMethod
                };

                if (installment) {
                    input.installment = installment;
                }

                order = await placeOrder(input);

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


                /* Sai dữ liệu: hiện lỗi ở đúng ô (backend trả tên trường, kể cả "installment.citizenId") */
                if (error.code === "VALIDATION_ERROR" && error.details) {

                    Object.keys(error.details).forEach(function (field) {
                        showFieldError(field, FIELD_MESSAGES[field] || error.details[field]);
                    });

                }

                showFormError(getErrorMessage(error));


                /*
                 * Giỏ đã đổi (trống / có sản phẩm ngừng bán / tổng tiền không còn đủ
                 * để trả góp): vẽ lại tóm tắt từ server
                 */
                if (error.code === "CART_EMPTY" || error.code === "PRODUCT_NOT_AVAILABLE"
                    || error.code === "INSTALLMENT_NOT_ELIGIBLE") {

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
