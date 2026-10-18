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
 * - Hình thức nhận hàng (Phase 7): giao tận nhà (cần địa chỉ; server tự chọn
 *   chi nhánh gần địa chỉ) hoặc nhận tại cửa hàng (chọn 1 cửa hàng đang mở từ
 *   GET /stores, không cần địa chỉ, miễn phí vận chuyển giống server — GET /cart
 *   luôn tính phí giao tận nhà nên tóm tắt tự bỏ phí khi chọn nhận tại cửa hàng).
 *   Không tải được / chưa có cửa hàng nào → khoá lựa chọn nhận tại cửa hàng.
 * - Kế hoạch v3 GĐ E: GET /cart/store-availability cho biết cửa hàng nào có đủ cả giỏ;
 *   cửa hàng thiếu hàng hiện "(thiếu hàng)" và không chọn được. Không cửa hàng nào đủ
 *   → báo trước (giao tận nhà cũng sẽ bị 409 OUT_OF_STOCK). Gặp OUT_OF_STOCK lúc đặt
 *   → tải lại giỏ + tình trạng hàng.
 * - Đặt hàng: POST /api/v1/orders (placeOrder, js/core/order-store.js) gửi
 *   người nhận / hình thức nhận (+ địa chỉ hoặc cửa hàng) / ghi chú / phương thức
 *   thanh toán (+ hồ sơ trả góp); server tạo đơn từ giỏ, xoá giỏ, rồi chuyển sang
 *   trang chi tiết đơn.
 * - Kế hoạch v5 GĐ5: "Thẻ quốc tế" (Stripe, chế độ Test) chỉ hiện khi GET /payments/card-options
 *   bật; đơn vượt hạn mức thẻ thì khoá. Đặt xong (chuyển khoản / thẻ) → trang thanh toán; thẻ thì
 *   trang đó tự mở Stripe. Có đơn còn chờ tiền (khách bấm Back) → khung nhắc phía trên.
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

        const pickupRadio = form.querySelector('input[name="deliveryType"][value="PICKUP"]');

        const pickupMethod = document.getElementById("pickupMethod");

        const pickupHint = document.getElementById("pickupMethodHint");

        const homeDeliveryFields = document.getElementById("homeDeliveryFields");

        const pickupFields = document.getElementById("pickupFields");

        const pickupSelect = document.getElementById("pickupStore");

        const pickupInfo = document.getElementById("pickupStoreInfo");

        const PICKUP_HINT = "Miễn phí vận chuyển. Chọn cửa hàng gần bạn để đến nhận.";

        const area = setupVnAddress(document.getElementById("shippingProvince"), document.getElementById("shippingWard"));


        const FIELD_MESSAGES = {
            recipientName:
                "Vui lòng nhập họ và tên người nhận (tối đa 120 ký tự).",
            recipientPhone:
                "Vui lòng nhập số điện thoại hợp lệ (8–20 ký tự số).",
            shippingAddress:
                "Vui lòng nhập địa chỉ nhận hàng đầy đủ.",
            shippingProvince:
                "Vui lòng chọn tỉnh / thành phố trong danh sách.",
            shippingWard:
                "Vui lòng chọn phường / xã trong danh sách.",
            shippingStreet:
                "Vui lòng nhập số nhà, tên đường.",
            pickupStoreId:
                "Vui lòng chọn cửa hàng đang mở để nhận hàng.",
            "installment.months":
                "Vui lòng chọn kỳ hạn trả góp.",
            "installment.cardBank":
                "Vui lòng chọn ngân hàng phát hành thẻ."
        };

        const UNAVAILABLE_MESSAGE =
            "Giỏ hàng có sản phẩm đã ngừng bán hoặc không đủ hàng. Vui lòng quay lại giỏ hàng để xóa hoặc giảm số lượng trước khi đặt hàng.";

        const NO_STORE_HOLDS_CART_MESSAGE =
            "Hiện không chi nhánh nào có đủ hàng cho cả giỏ của bạn. Hãy giảm số lượng hoặc đặt riêng từng sản phẩm.";


        const cardRadio = form.querySelector('input[name="paymentMethod"][value="CARD"]');

        const cardMethod = document.getElementById("cardMethod");

        const cardHint = document.getElementById("cardMethodHint");

        const CARD_HINT = "Thanh toán ngay trên trang bảo mật của Stripe.";


        /* Khách bấm Back từ trang thanh toán: nhắc đơn đang chờ tiền; huỷ xong → về giỏ (đã có lại sản phẩm) */
        showAwaitingPaymentNotice(document.getElementById("awaitingPayment"), function () {
            window.location.href = "cart.html";
        });

        /* Kế hoạch v5 GĐ6: điểm tích luỹ của khách; lỗi → null (ẩn ô dùng điểm) */
        const loyaltyPromise = apiRequest("/loyalty/me", { auth: true }).catch(function () { return null; });

        /* Thẻ quốc tế: lỗi → null (ẩn lựa chọn) */
        const cardOptionsPromise = apiRequest("/payments/card-options", { auth: true })
            .catch(function () { return null; });

        /* Tải song song với giỏ hàng; lỗi → null (khoá lựa chọn trả góp) */
        const optionsPromise = apiRequest("/payments/installment-options", { auth: true })
            .catch(function () { return null; });

        /* Cửa hàng đang mở (công khai); lỗi → null (khoá "Nhận tại cửa hàng") */
        const storesPromise = loadPickupStores();

        /* Cửa hàng nào có đủ cả giỏ (v3 GĐ E); lỗi → null (không khoá gì, server vẫn kiểm tra) */
        const stockPromise = loadStoreStock();


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

        const cardOptions = await cardOptionsPromise;

        const loyalty = await loyaltyPromise;

        /* số điểm khách đang có (giảm khi server báo số dư mới) */
        let pointsBalance = loyalty ? Number(loyalty.points) || 0 : 0;

        /* số điểm sẽ dùng cho đơn này (0 khi không tick) */
        let pointsToUse = 0;

        document.getElementById("usePoints").addEventListener("change", renderTotals);

        let pickupStores = await storesPromise;

        let storeStock = await stockPromise;

        let currentTotal = summary.total;

        /* Giỏ trên server gần nhất (phí ship trong đó luôn là phí giao tận nhà) */
        let lastCart = summary;

        /* Voucher đang áp (VoucherOptionResponse của POST /vouchers/apply) hoặc null */
        let appliedVoucher = null;


        content.hidden = false;

        fillInstallmentChoices();

        setupCardMethod();

        fillPickupStores();

        renderSummary(summary);

        prefillFromAccount();

        applyStoredOrderInfo();


        if (summary.hasUnavailableItems) {

            showFormError(UNAVAILABLE_MESSAGE);

            document.getElementById("checkoutSubmitBtn").disabled = true;

        } else if (noStoreHoldsCart()) {

            showFormError(NO_STORE_HOLDS_CART_MESSAGE);

        }


        form.querySelectorAll('input[name="paymentMethod"]').forEach(function (radio) {

            radio.addEventListener("change", updateInstallmentBox);

        });

        form.querySelectorAll('input[name="deliveryType"]').forEach(function (radio) {

            radio.addEventListener("change", updateDeliveryFields);

        });

        pickupSelect.addEventListener("change", renderPickupInfo);

        monthsSelect.addEventListener("change", renderPreview);

        /* CCCD chỉ gồm chữ số */
        citizenInput.addEventListener("input", function () {

            const digits = citizenInput.value.replace(/\D/g, "");

            if (digits !== citizenInput.value) {
                citizenInput.value = digits;
            }

        });


        form.addEventListener("submit", handleSubmit);


        document.getElementById("voucherApplyBtn").addEventListener("click", function () {
            applyVoucherCode(document.getElementById("voucherCodeInput").value, false);
        });

        /* Enter trong ô mã: áp dụng, không gửi form đặt hàng */
        document.getElementById("voucherCodeInput").addEventListener("keydown", function (event) {
            if (event.key === "Enter") {
                event.preventDefault();
                applyVoucherCode(document.getElementById("voucherCodeInput").value, false);
            }
        });

        document.getElementById("voucherRemoveBtn").addEventListener("click", removeVoucher);

        document.getElementById("voucherPickBtn").addEventListener("click", openVoucherModal);

        loadVoucherCount();


        /*
         * Thông tin nhận hàng đã nhập ở popup trong giỏ hàng (order-info.js): đổ vào các ô của form
         * (vẫn là nguồn khi đặt hàng) rồi chỉ hiện bản tóm tắt + nút Sửa. Cửa hàng đã lưu mà nay đóng /
         * thiếu hàng thì không dùng bản lưu: hiện form để khách chọn lại.
         */

        function applyStoredOrderInfo() {

            const stored = readOrderInfo();

            const card = document.getElementById("orderInfoCard");

            const fields = document.getElementById("orderInfoFields");

            if (!stored) {
                return;
            }

            if (stored.deliveryType === "PICKUP") {

                const store = findPickupStore(stored.pickupStoreId);

                if (pickupRadio.disabled || !store || storeLacksItems(store)) {
                    return;
                }

            }

            document.getElementById("recipientName").value = stored.recipientName;

            document.getElementById("recipientPhone").value = stored.recipientPhone;

            document.getElementById("shippingStreet").value = stored.shippingStreet || "";

            area.setValue(stored.shippingProvince, stored.shippingWard);

            form.querySelector('input[name="deliveryType"][value="' + stored.deliveryType + '"]').checked = true;

            if (stored.deliveryType === "PICKUP") {
                pickupSelect.value = String(stored.pickupStoreId);
            }

            updateDeliveryFields();

            renderPickupInfo();

            document.getElementById("orderInfoCardText").innerHTML = orderInfoSummaryHtml(stored);

            card.hidden = false;

            fields.hidden = true;

        }


        document.getElementById("orderInfoEdit").addEventListener("click", function () {

            openOrderInfoModal({
                onSave: function () {

                    applyStoredOrderInfo();

                    clearMessages();

                }
            });

        });


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

                fillIfEmpty("shippingStreet", profile.address);

                if (!document.getElementById("shippingProvince").value.trim()) {
                    area.setValue(profile.city, profile.ward);
                }

            }

        }


        function fillIfEmpty(id, value) {

            const input = document.getElementById(id);

            if (input && !input.value.trim() && value) {
                input.value = value;
            }

        }


        function renderSummary(cartSummary) {

            lastCart = cartSummary;

            const itemsBox = document.getElementById("checkoutItems");

            itemsBox.innerHTML = cartSummary.items.map(function (item) {

                return `
                    <div class="checkout-item">
                        <img src="${escapeHtml(cartItemImageUrl(item))}" alt="${escapeHtml(item.name)}" referrerpolicy="no-referrer">
                        <div class="checkout-item-info">
                            <p class="checkout-item-name">${escapeHtml(item.name)}</p>
                            <p class="checkout-item-variant">${escapeHtml(item.variantLabel ? item.variantLabel + " × " : "× ")}${item.quantity}</p>
                            ${item.comboDiscount > 0 ? `<span class="combo-tag" title="${escapeHtml(item.comboNames.join(", "))}">Combo −${escapeHtml(formatPrice(item.comboDiscount))}</span>` : ""}
                        </div>
                        <span class="checkout-item-price">${formatPrice(item.lineTotal)}</span>
                    </div>
                `;

            }).join("");


            document.getElementById("checkoutSubtotal").textContent = formatPrice(cartSummary.subtotal);

            document.getElementById("checkoutComboRow").hidden = !(cartSummary.comboDiscount > 0);

            document.getElementById("checkoutComboDiscount").textContent = "−" + formatPrice(cartSummary.comboDiscount);

            renderTotals();

        }


        /* Phí ship + tổng theo hình thức nhận: nhận tại cửa hàng miễn phí (giống ShippingPolicy ở server) */

        function renderTotals() {

            const shippingFee = selectedDeliveryType() === "PICKUP" ? 0 : lastCart.shippingFee;

            /* voucher chỉ giảm tiền hàng, không giảm phí ship (giống server) */
            const voucherDiscount = appliedVoucher ? Number(appliedVoucher.discount) || 0 : 0;

            /* điểm: tối đa 10% tiền hàng sau voucher (giống LoyaltyPolicy ở server) */
            const goodsAfterVoucher = lastCart.total - lastCart.shippingFee - voucherDiscount;

            const usable = Math.max(0, Math.min(pointsBalance, Math.floor(goodsAfterVoucher * 10 / 100 / 1000)));

            renderPointsBox(usable);

            pointsToUse = document.getElementById("usePoints").checked ? usable : 0;

            currentTotal = lastCart.total - lastCart.shippingFee + shippingFee - voucherDiscount - pointsToUse * 1000;

            document.getElementById("checkoutPointsRow").hidden = pointsToUse <= 0;

            document.getElementById("checkoutPointsDiscount").textContent = "−" + formatPrice(pointsToUse * 1000);

            document.getElementById("checkoutShipping").textContent =
                shippingFee === 0 ? "Miễn phí" : formatPrice(shippingFee);

            document.getElementById("checkoutVoucherRow").hidden = voucherDiscount <= 0;

            document.getElementById("checkoutVoucherCode").textContent = appliedVoucher ? appliedVoucher.code : "";

            document.getElementById("checkoutVoucherDiscount").textContent = "−" + formatPrice(voucherDiscount);

            document.getElementById("checkoutTotal").textContent = formatPrice(currentTotal);

            applyInstallmentEligibility();

            applyCardEligibility();

        }


        /* Ô "Dùng điểm": ẩn khi chưa có điểm; dùng được 0 điểm (đơn quá nhỏ) → khoá kèm lý do */
        function renderPointsBox(usable) {

            const box = document.getElementById("checkoutPoints");

            const checkbox = document.getElementById("usePoints");

            box.hidden = !loyalty || pointsBalance <= 0;

            if (box.hidden) {
                checkbox.checked = false;
                return;
            }

            checkbox.disabled = usable <= 0;

            if (usable <= 0) {
                checkbox.checked = false;
            }

            box.classList.toggle("is-disabled", usable <= 0);

            document.getElementById("pointsUsable").textContent = new Intl.NumberFormat("vi-VN").format(usable);

            document.getElementById("pointsHint").textContent = usable > 0
                ? "Bạn có " + new Intl.NumberFormat("vi-VN").format(pointsBalance) + " điểm · dùng tối đa 10% giá trị hàng"
                : "Bạn có " + new Intl.NumberFormat("vi-VN").format(pointsBalance) + " điểm · đơn này chưa đủ giá trị để dùng điểm";

            document.getElementById("pointsValue").textContent = usable > 0 ? "−" + formatPrice(usable * 1000) : "";

        }


        /* ================= VOUCHER (kế hoạch v5 GĐ4) ================= */

        function showVoucherMessage(text, kind) {

            const box = document.getElementById("voucherMessage");

            box.textContent = text || "";

            box.className = "checkout-voucher-message" + (kind ? " is-" + kind : "");

            box.hidden = !text;

        }


        function renderVoucherState() {

            document.getElementById("voucherApplied").hidden = !appliedVoucher;

            document.getElementById("voucherInputRow").hidden = Boolean(appliedVoucher);

            if (appliedVoucher) {

                document.getElementById("voucherAppliedCode").textContent = appliedVoucher.code;

                document.getElementById("voucherAppliedName").textContent =
                    appliedVoucher.name + " · −" + formatPrice(Number(appliedVoucher.discount));

            }

            renderTotals();

        }


        /* POST /vouchers/apply: server tính trên giỏ hiện tại; quiet = kiểm tra lại sau khi giỏ đổi */

        async function applyVoucherCode(code, quiet) {

            const value = String(code || "").trim().toUpperCase();

            if (!value) {
                showVoucherMessage("Vui lòng nhập mã voucher.", "error");
                return false;
            }

            const applyButton = document.getElementById("voucherApplyBtn");

            applyButton.disabled = true;

            try {

                const option = await apiRequest("/vouchers/apply", { method: "POST", body: { code: value }, auth: true });

                appliedVoucher = option;

                document.getElementById("voucherCodeInput").value = "";

                showVoucherMessage(quiet ? "" : "Đã áp dụng voucher, giảm " + formatPrice(Number(option.discount)) + ".", quiet ? "" : "success");

                renderVoucherState();

                return true;

            } catch (error) {

                appliedVoucher = null;

                renderVoucherState();

                showVoucherMessage(error.code === "VOUCHER_NOT_FOUND" || error.code === "VOUCHER_NOT_APPLICABLE"
                    ? error.message
                    : getErrorMessage(error), "error");

                return false;

            } finally {

                applyButton.disabled = false;

            }

        }


        function removeVoucher() {

            appliedVoucher = null;

            showVoucherMessage("", "");

            renderVoucherState();

        }


        function voucherConditionText(option) {

            const parts = ["Giảm " + Number(option.discountPercent).toLocaleString("vi-VN", { maximumFractionDigits: 2 }) + "%"];

            if (option.maxDiscount) {
                parts.push("tối đa " + formatPrice(Number(option.maxDiscount)));
            }

            if (Number(option.minOrderAmount) > 0) {
                parts.push("đơn từ " + formatPrice(Number(option.minOrderAmount)));
            }

            return parts.join(" · ");

        }


        async function loadVoucherCount() {

            try {

                const list = await apiRequest("/vouchers/available", { auth: true }) || [];

                const usable = list.filter(function (option) { return option.applicable; }).length;

                document.getElementById("voucherPickCount").textContent = list.length
                    ? usable + "/" + list.length + " mã dùng được ›"
                    : "Chưa có mã nào ›";

            } catch (error) {

                document.getElementById("voucherPickCount").textContent = "›";

            }

        }


        /* Danh sách voucher đang chạy: dùng được → nút "Dùng"; chưa đủ điều kiện → mờ + lý do (vd "Mua thêm …") */

        async function openVoucherModal() {

            closeModal();

            const overlay = document.createElement("div");

            overlay.className = "modal-overlay";

            overlay.innerHTML = `
                <div class="modal-box voucher-modal" role="dialog" aria-modal="true" aria-labelledby="voucherModalTitle">
                    <div class="order-info-head">
                        <h3 id="voucherModalTitle">Chọn voucher</h3>
                        <button type="button" class="order-info-close" data-action="close" aria-label="Đóng">×</button>
                    </div>
                    <div class="voucher-modal-body" id="voucherModalBody">
                        <p class="order-info-loading">Đang tải...</p>
                    </div>
                </div>
            `;

            document.body.appendChild(overlay);

            modalOverlayElement = overlay;

            requestAnimationFrame(function () {
                overlay.classList.add("show");
            });

            overlay.addEventListener("click", function (event) {

                if (event.target === overlay || event.target.closest('[data-action="close"]')) {
                    closeModal();
                    return;
                }

                const use = event.target.closest("[data-use-voucher]");

                if (use) {

                    closeModal();

                    applyVoucherCode(use.dataset.useVoucher, false);

                }

            });

            const body = overlay.querySelector("#voucherModalBody");

            let list;

            try {

                list = await apiRequest("/vouchers/available", { auth: true }) || [];

            } catch (error) {

                body.innerHTML = `<p class="voucher-modal-empty">${escapeHtml(getErrorMessage(error))}</p>`;

                return;

            }

            if (modalOverlayElement !== overlay) {
                return;
            }

            if (list.length === 0) {

                body.innerHTML = `<p class="voucher-modal-empty">Hiện chưa có voucher nào đang áp dụng.</p>`;

                return;

            }

            body.innerHTML = list.map(function (option) {

                const scope = option.allCategories ? "Mọi sản phẩm" : "Áp dụng: " + (option.categoryNames || []).join(", ");

                const current = appliedVoucher && appliedVoucher.code === option.code;

                const action = option.applicable
                    ? `<button type="button" class="btn btn-dark" data-use-voucher="${escapeHtml(option.code)}" ${current ? "disabled" : ""}>${current ? "ĐANG DÙNG" : "DÙNG"}</button>`
                    : "";

                return `
                    <div class="voucher-card${option.applicable ? "" : " is-disabled"}">
                        <div class="voucher-card-badge">
                            <strong>${escapeHtml(Number(option.discountPercent).toLocaleString("vi-VN", { maximumFractionDigits: 2 }))}%</strong>
                            <span>GIẢM</span>
                        </div>
                        <div class="voucher-card-info">
                            <strong>${escapeHtml(option.name)}</strong>
                            <span class="voucher-card-code">${escapeHtml(option.code)}</span>
                            <span>${escapeHtml(voucherConditionText(option))}</span>
                            <span class="voucher-card-muted">${escapeHtml(scope)} · HSD ${escapeHtml(new Date(option.endsAt).toLocaleDateString("vi-VN"))}</span>
                            ${option.applicable
                                ? `<span class="voucher-card-good">Giảm ${escapeHtml(formatPrice(Number(option.discount)))} cho giỏ này</span>`
                                : `<span class="voucher-card-why">${escapeHtml(option.message || "Chưa dùng được")}</span>`}
                        </div>
                        ${action}
                    </div>
                `;

            }).join("");

        }


        /* ================= HÌNH THỨC NHẬN HÀNG ================= */

        async function loadPickupStores() {

            try {

                return await apiRequest("/stores") || [];

            } catch (error) {

                return null;

            }

        }


        /* storeId → { available, missing } của giỏ hiện tại; null khi không tải được */

        async function loadStoreStock() {

            try {

                const rows = await apiRequest("/cart/store-availability", { auth: true }) || [];

                const byId = {};

                rows.forEach(function (row) {
                    byId[String(row.storeId)] = row;
                });

                return byId;

            } catch (error) {

                return null;

            }

        }


        function storeLacksItems(store) {

            const entry = storeStock && store ? storeStock[String(store.id)] : null;

            return Boolean(entry) && entry.available === false;

        }


        function noStoreHoldsCart() {

            return Boolean(storeStock) && (pickupStores || []).length > 0
                && pickupStores.every(storeLacksItems);

        }


        async function refreshStoresAndStock() {

            const results = await Promise.all([loadPickupStores(), loadStoreStock()]);

            pickupStores = results[0];

            storeStock = results[1];

            fillPickupStores();

        }


        /* Điền ô chọn cửa hàng (giữ cửa hàng đang chọn nếu vẫn mở); không có cửa hàng nào → khoá lựa chọn */

        function fillPickupStores() {

            const selected = pickupSelect.value;

            pickupSelect.innerHTML = `<option value="">-- Chọn cửa hàng --</option>` +
                (pickupStores || []).map(function (store) {

                    const area = [store.district, store.city].filter(Boolean).join(", ");

                    const lacks = storeLacksItems(store);

                    return `<option value="${escapeHtml(String(store.id))}" ${lacks ? "disabled" : ""}>`
                        + `${escapeHtml(store.name + (area ? " — " + area : "") + (lacks ? " (thiếu hàng)" : ""))}</option>`;

                }).join("");

            const selectedStore = findPickupStore(selected);

            if (selectedStore && !storeLacksItems(selectedStore)) {
                pickupSelect.value = selected;
            }


            let reason = "";

            if (pickupStores === null) {
                reason = "Hiện chưa tải được danh sách cửa hàng. Vui lòng chọn giao tận nhà hoặc tải lại trang.";
            } else if (pickupStores.length === 0) {
                reason = "Hiện chưa có cửa hàng nào nhận đơn tại chỗ.";
            } else if (noStoreHoldsCart()) {
                reason = "Không cửa hàng nào có đủ hàng cho cả giỏ của bạn.";
            }

            pickupRadio.disabled = reason !== "";

            pickupMethod.classList.toggle("is-disabled", reason !== "");

            pickupHint.textContent = reason || PICKUP_HINT;

            if (reason && pickupRadio.checked) {
                form.querySelector('input[name="deliveryType"][value="HOME_DELIVERY"]').checked = true;
            }

            renderPickupInfo();

            updateDeliveryFields();

        }


        function findPickupStore(id) {

            return (pickupStores || []).find(function (store) {
                return String(store.id) === String(id);
            });

        }


        function renderPickupInfo() {

            const store = findPickupStore(pickupSelect.value);

            if (!store) {

                pickupInfo.hidden = true;

                pickupInfo.innerHTML = "";

                return;

            }

            const address = [store.address, store.district, store.city].filter(Boolean).join(", ");

            pickupInfo.innerHTML = `<strong>${escapeHtml(store.name)}</strong>`
                + escapeHtml(address)
                + (store.phone ? `<br>Điện thoại: ${escapeHtml(store.phone)}` : "");

            pickupInfo.hidden = false;

        }


        /* Giao tận nhà → ô địa chỉ; nhận tại cửa hàng → ô chọn cửa hàng. Tổng tiền đổi theo phí ship */

        function updateDeliveryFields() {

            const pickup = selectedDeliveryType() === "PICKUP";

            homeDeliveryFields.hidden = pickup;

            pickupFields.hidden = !pickup;

            renderTotals();

        }


        function selectedDeliveryType() {

            return form.querySelector('input[name="deliveryType"]:checked').value;

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


        /* Hiện "Thẻ quốc tế" khi server bật Stripe; chế độ Test → nhãn + gợi ý thẻ thử */
        function setupCardMethod() {

            if (!cardOptions || !cardOptions.enabled) {
                return;
            }

            cardMethod.hidden = false;

            document.getElementById("cardTestBadge").hidden = !cardOptions.testMode;

            applyCardEligibility();

        }


        /* Stripe nhận tối đa 99.999.999đ / lần: vượt thì khoá lựa chọn (server cũng kiểm tra) */
        function applyCardEligibility() {

            if (!cardOptions || !cardOptions.enabled) {
                return;
            }

            const tooLarge = currentTotal > Number(cardOptions.maxAmount);

            cardRadio.disabled = tooLarge;

            cardMethod.classList.toggle("is-disabled", tooLarge);

            cardHint.textContent = tooLarge
                ? "Đơn trên " + formatPrice(Number(cardOptions.maxAmount)) + " không thanh toán bằng thẻ được."
                : (cardOptions.testMode ? CARD_HINT + " Chế độ thử: dùng thẻ 4242 4242 4242 4242, không trừ tiền thật." : CARD_HINT);

            if (tooLarge && cardRadio.checked) {
                form.querySelector('input[name="paymentMethod"][value="COD"]').checked = true;
            }

        }


        function selectedPaymentMethod() {

            return form.querySelector('input[name="paymentMethod"]:checked').value;

        }


        /* ================= ĐẶT HÀNG ================= */

        async function handleSubmit(event) {

            event.preventDefault();

            clearMessages();


            const picked = area.getValue();

            const data = {
                recipientName: valueOf("recipientName").trim(),
                recipientPhone: valueOf("recipientPhone").trim(),
                deliveryType: selectedDeliveryType(),
                shippingProvince: picked.province,
                shippingWard: picked.ward,
                shippingStreet: valueOf("shippingStreet").trim(),
                pickupStoreId: pickupSelect.value ? Number(pickupSelect.value) : null,
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

            if (data.deliveryType === "PICKUP") {

                if (!findPickupStore(data.pickupStoreId) || storeLacksItems(findPickupStore(data.pickupStoreId))) {
                    invalid.push("pickupStoreId");
                }

            } else {

                ["shippingProvince", "shippingWard", "shippingStreet"].forEach(function (field) {
                    if (!data[field]) {
                        invalid.push(field);
                    }
                });

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
                    : "Vui lòng kiểm tra lại thông tin nhận hàng.");

                return;

            }


            const button = document.getElementById("checkoutSubmitBtn");

            button.disabled = true;

            button.textContent = "Đang xử lý...";


            function restoreButton() {

                button.disabled = false;

                button.textContent = "Đặt hàng";

            }


            /*
             * Server đặt hàng từ giỏ của tài khoản (giá, phí ship tính lại lúc đặt)
             * và xoá giỏ. Nút đã khoá nên không gửi 2 lần; nếu vẫn trùng (2 tab),
             * lần sau nhận CART_EMPTY.
             */
            let order;

            try {

                /* Nhận tại cửa hàng: không gửi địa chỉ (server ghi địa chỉ cửa hàng vào đơn) */
                const pickup = data.deliveryType === "PICKUP";

                const input = {
                    recipientName: data.recipientName,
                    recipientPhone: data.recipientPhone,
                    deliveryType: data.deliveryType,
                    shippingAddress: pickup ? null : joinVnAddress(data.shippingStreet, data.shippingWard, data.shippingProvince),
                    shippingProvince: pickup ? null : data.shippingProvince,
                    shippingWard: pickup ? null : data.shippingWard,
                    pickupStoreId: pickup ? data.pickupStoreId : null,
                    note: data.note || null,
                    paymentMethod: data.paymentMethod,
                    voucherCode: appliedVoucher ? appliedVoucher.code : null,
                    pointsToUse: pointsToUse > 0 ? pointsToUse : null
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

            /* Chuyển khoản / thẻ đang chờ tiền → trang thanh toán riêng (thẻ: trang đó tự mở Stripe); còn lại → chi tiết đơn */
            window.location.href = order.payment && order.payment.card
                ? getPaymentUrl(order.id) + "&start=card"
                : order.payment && order.payment.bankTransfer
                    ? getPaymentUrl(order.id)
                    : getOrderDetailUrl(order.id, true);


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

                /* Điểm vừa đổi (đơn khác vừa dùng / tổng đổi): tải lại số dư, bỏ tick, báo lý do */
                if (error.code === "VALIDATION_ERROR" && error.details && error.details.pointsToUse) {

                    const fresh = await apiRequest("/loyalty/me", { auth: true }).catch(function () { return null; });

                    if (fresh) {
                        pointsBalance = Number(fresh.points) || 0;
                    }

                    document.getElementById("usePoints").checked = false;

                    renderTotals();

                    restoreButton();

                    showFormError("Không dùng được điểm: " + error.details.pointsToUse + ". Vui lòng kiểm tra lại tổng tiền rồi đặt hàng.");

                    return;

                }

                /* Voucher vừa hết lượt / hết hạn / không còn đủ điều kiện: bỏ voucher, báo lý do của server */
                if (error.code === "VOUCHER_NOT_APPLICABLE" || error.code === "VOUCHER_NOT_FOUND") {

                    appliedVoucher = null;

                    renderVoucherState();

                    showVoucherMessage(error.message, "error");

                    restoreButton();

                    showFormError("Voucher không còn dùng được: " + error.message + " Vui lòng kiểm tra lại tổng tiền rồi đặt hàng.");

                    loadVoucherCount();

                    return;

                }

                showFormError(getErrorMessage(error));


                /* Cửa hàng vừa tạm đóng / tồn kho vừa đổi: tải lại cửa hàng + tình trạng hàng */
                if ((error.details && error.details.pickupStoreId) || error.code === "OUT_OF_STOCK") {

                    await refreshStoresAndStock();

                }


                /*
                 * Giỏ đã đổi (trống / có sản phẩm ngừng bán / tổng tiền không còn đủ
                 * để trả góp): vẽ lại tóm tắt từ server
                 */
                if (error.code === "CART_EMPTY" || error.code === "PRODUCT_NOT_AVAILABLE"
                    || error.code === "INSTALLMENT_NOT_ELIGIBLE" || error.code === "OUT_OF_STOCK") {

                    try {

                        const fresh = await getCartSummary();

                        setCartCount(fresh.totalQuantity);

                        if (fresh.items.length > 0) {

                            renderSummary(fresh);

                            /* giỏ đổi → số tiền giảm của voucher có thể đổi theo */
                            if (appliedVoucher) {
                                await applyVoucherCode(appliedVoucher.code, true);
                            }

                        }

                        if (fresh.hasUnavailableItems) {

                            showFormError(UNAVAILABLE_MESSAGE);

                            button.textContent = "Đặt hàng";

                            return;

                        }

                        if (fresh.items.length === 0) {

                            button.textContent = "Đặt hàng";

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
