/* ================= THÔNG TIN NHẬN HÀNG (popup ở giỏ hàng) ================= */

/*
 * Khách nhập hình thức nhận hàng + người nhận trong một popup ngay ở giỏ hàng; thông tin lưu ở
 * sessionStorage theo tài khoản (đóng tab là mất), trang thanh toán đọc lại và cho sửa bằng chính
 * popup này. Quy tắc kiểm tra giống checkout.js / server. Dữ liệu từ API đều escape khi đưa vào HTML.
 * Giao tận nhà: địa chỉ; nhận tại cửa hàng: chọn 1 cửa hàng đang mở (GET /stores), cửa hàng không đủ
 * hàng cho cả giỏ (GET /cart/store-availability) bị khoá.
 */

const ORDER_INFO_KEY_PREFIX = "poy_order_info:";

const ORDER_INFO_PHONE_PATTERN = /^[0-9+\s().-]{8,20}$/;


function orderInfoKey() {

    const user = getCurrentUser() || {};

    return ORDER_INFO_KEY_PREFIX + (user.id || user.username || "me");

}


function readOrderInfo() {

    try {

        const info = JSON.parse(sessionStorage.getItem(orderInfoKey()) || "null");

        return isOrderInfoComplete(info) ? info : null;

    } catch (error) {

        return null;

    }

}


function saveOrderInfo(info) {

    try {
        sessionStorage.setItem(orderInfoKey(), JSON.stringify(info));
    } catch (error) {
        /* không lưu được: vẫn dùng được trong lần này */
    }

}


function clearOrderInfo() {

    try {
        sessionStorage.removeItem(orderInfoKey());
    } catch (error) {
        /* bỏ qua */
    }

}


function isOrderInfoComplete(info) {

    if (!info || !info.recipientName || !ORDER_INFO_PHONE_PATTERN.test(info.recipientPhone || "")) {
        return false;
    }

    return info.deliveryType === "PICKUP"
        ? Boolean(info.pickupStoreId)
        : Boolean(info.shippingAddress);

}


/* Một dòng tóm tắt cho hàng "Thông tin nhận hàng" ở giỏ hàng / thanh toán (đã escape) */

function orderInfoSummaryHtml(info) {

    if (!info) {
        return `<span class="order-info-empty">Vui lòng cung cấp thông tin nhận hàng</span>`;
    }

    let where;

    if (info.deliveryType === "PICKUP") {

        where = "Nhận tại cửa hàng" + (info.pickupStoreName ? ": " + info.pickupStoreName : "");

    } else {

        where = "Giao tận nhà: " + info.shippingAddress;

    }

    return `<strong>${escapeHtml(info.recipientName)} · ${escapeHtml(info.recipientPhone)}</strong>`
        + `<span>${escapeHtml(where)}</span>`;

}


function foldVietnamese(text) {

    return String(text || "")
        .normalize("NFD")
        .replace(/[̀-ͯ]/g, "")
        .replace(/đ/g, "d")
        .replace(/Đ/g, "D")
        .toLowerCase();

}


async function loadOrderInfoStores() {

    try {
        return await apiRequest("/stores") || [];
    } catch (error) {
        return null;
    }

}


/* storeId → { available }; lỗi → null (không khoá gì, server vẫn kiểm tra khi đặt hàng) */

async function loadOrderInfoStock() {

    try {

        const byId = {};

        (await apiRequest("/cart/store-availability", { auth: true }) || []).forEach(function (row) {
            byId[String(row.storeId)] = row;
        });

        return byId;

    } catch (error) {

        return null;

    }

}


/* Điền sẵn từ tài khoản khi chưa có thông tin đã lưu; lỗi thì để trống */

async function loadOrderInfoDefaults() {

    const user = getCurrentUser() || {};

    const defaults = { deliveryType: "HOME_DELIVERY", recipientName: user.fullname || "", recipientPhone: "", shippingAddress: "", pickupStoreId: null };

    const results = await Promise.allSettled([
        apiRequest("/users/me", { auth: true }),
        apiRequest("/users/me/profile", { auth: true })
    ]);

    if (results[0].status === "fulfilled" && results[0].value) {
        defaults.recipientName = results[0].value.fullname || defaults.recipientName;
        defaults.recipientPhone = results[0].value.phone || "";
    }

    if (results[1].status === "fulfilled" && results[1].value) {

        const profile = results[1].value;

        defaults.shippingAddress = profile.defaultShippingAddress
            || [profile.address, profile.ward, profile.district, profile.city]
                .filter(function (part) { return part && String(part).trim(); })
                .join(", ");

    }

    return defaults;

}


/*
 * openOrderInfoModal({ onSave }): hiện popup, "Xác nhận" kiểm tra → lưu → đóng → onSave(info).
 * Bấm ra ngoài / nút đóng: không lưu, không gọi onSave.
 */

async function openOrderInfoModal(options) {

    closeModal();

    const overlay = document.createElement("div");

    overlay.className = "modal-overlay";

    overlay.innerHTML = `
        <div class="modal-box order-info-modal" role="dialog" aria-modal="true" aria-labelledby="orderInfoTitle">
            <div class="order-info-head">
                <h3 id="orderInfoTitle">Thông tin nhận hàng</h3>
                <button type="button" class="order-info-close" data-action="close" aria-label="Đóng">×</button>
            </div>
            <div class="order-info-body" id="orderInfoBody">
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
        if (event.target === overlay) {
            closeModal();
        }
    });

    overlay.querySelector('[data-action="close"]').addEventListener("click", closeModal);


    const saved = readOrderInfo();

    const loaded = await Promise.all([
        saved ? Promise.resolve(saved) : loadOrderInfoDefaults(),
        loadOrderInfoStores(),
        loadOrderInfoStock()
    ]);

    /* đóng trong lúc đang tải */
    if (modalOverlayElement !== overlay) {
        return;
    }

    const info = loaded[0];

    const stores = loaded[1];

    const stock = loaded[2];

    const lacks = function (store) {
        return Boolean(stock && stock[String(store.id)]) && stock[String(store.id)].available === false;
    };

    const canPickup = Array.isArray(stores) && stores.some(function (store) { return !lacks(store); });

    let deliveryType = info.deliveryType === "PICKUP" && canPickup ? "PICKUP" : "HOME_DELIVERY";

    let pickupStoreId = info.pickupStoreId ? String(info.pickupStoreId) : "";


    const body = overlay.querySelector("#orderInfoBody");

    body.innerHTML = `
        <form id="orderInfoForm" novalidate>
            <div class="order-info-tabs" role="tablist">
                <button type="button" role="tab" data-type="HOME_DELIVERY">Giao tận nhà</button>
                <button type="button" role="tab" data-type="PICKUP" ${canPickup ? "" : "disabled"}
                    title="${escapeHtml(canPickup ? "" : "Hiện không cửa hàng nào nhận đơn tại chỗ cho giỏ này")}">Nhận tại cửa hàng</button>
            </div>

            <label class="order-info-field">
                <span>Họ và tên người nhận <b>*</b></span>
                <input type="text" id="orderInfoName" maxlength="120" value="${escapeHtml(info.recipientName)}" autocomplete="name">
                <small class="field-error" data-error="name"></small>
            </label>

            <label class="order-info-field">
                <span>Số điện thoại <b>*</b></span>
                <input type="tel" id="orderInfoPhone" maxlength="20" value="${escapeHtml(info.recipientPhone)}" autocomplete="tel">
                <small class="field-error" data-error="phone"></small>
            </label>

            <div id="orderInfoHome">
                <label class="order-info-field">
                    <span>Địa chỉ nhận hàng <b>*</b></span>
                    <textarea id="orderInfoAddress" rows="3" maxlength="500" autocomplete="street-address">${escapeHtml(info.shippingAddress)}</textarea>
                    <small class="field-error" data-error="address"></small>
                </label>
            </div>

            <div id="orderInfoPickup" hidden>
                <label class="order-info-field">
                    <span>Tìm cửa hàng theo tên, đường, quận</span>
                    <input type="search" id="orderInfoStoreSearch" placeholder="Ví dụ: Quận 1">
                </label>
                <p class="order-info-count" id="orderInfoStoreCount"></p>
                <div class="order-info-stores" id="orderInfoStores" role="radiogroup" aria-label="Cửa hàng nhận hàng"></div>
                <small class="field-error" data-error="store"></small>
            </div>

            <button type="submit" class="btn btn-dark order-info-submit">Xác nhận</button>
        </form>
    `;


    const form = body.querySelector("#orderInfoForm");

    const tabs = form.querySelectorAll(".order-info-tabs button");

    const homeBox = form.querySelector("#orderInfoHome");

    const pickupBox = form.querySelector("#orderInfoPickup");

    const storeList = form.querySelector("#orderInfoStores");

    const storeCount = form.querySelector("#orderInfoStoreCount");

    const search = form.querySelector("#orderInfoStoreSearch");


    function showType() {

        tabs.forEach(function (tab) {
            const on = tab.dataset.type === deliveryType;
            tab.classList.toggle("is-active", on);
            tab.setAttribute("aria-selected", String(on));
        });

        homeBox.hidden = deliveryType === "PICKUP";

        pickupBox.hidden = deliveryType !== "PICKUP";

    }


    function renderStores() {

        const term = foldVietnamese(search.value.trim());

        const shown = (stores || []).filter(function (store) {
            return !term || foldVietnamese([store.name, store.address, store.district, store.city].join(" ")).indexOf(term) !== -1;
        });

        storeCount.textContent = "Có " + shown.length + " cửa hàng:";

        storeList.innerHTML = shown.map(function (store) {

            const address = [store.address, store.district, store.city].filter(Boolean).join(", ");

            const out = lacks(store);

            return `
                <label class="order-info-store${out ? " is-disabled" : ""}">
                    <input type="radio" name="orderInfoStore" value="${escapeHtml(String(store.id))}"
                        ${String(store.id) === pickupStoreId ? "checked" : ""} ${out ? "disabled" : ""}>
                    <span>
                        <strong>${escapeHtml(store.name)}${out ? " (thiếu hàng)" : ""}</strong>
                        <small>${escapeHtml(address)}</small>
                    </span>
                </label>
            `;

        }).join("");

        storeList.querySelectorAll("input").forEach(function (radio) {
            radio.addEventListener("change", function () {
                pickupStoreId = radio.value;
            });
        });

    }


    tabs.forEach(function (tab) {
        tab.addEventListener("click", function () {
            if (!tab.disabled) {
                deliveryType = tab.dataset.type;
                showType();
            }
        });
    });

    search.addEventListener("input", renderStores);

    showType();

    renderStores();


    function setError(field, message) {
        form.querySelector('[data-error="' + field + '"]').textContent = message || "";
    }


    form.addEventListener("submit", function (event) {

        event.preventDefault();

        const result = {
            deliveryType: deliveryType,
            recipientName: form.querySelector("#orderInfoName").value.trim(),
            recipientPhone: form.querySelector("#orderInfoPhone").value.trim(),
            shippingAddress: form.querySelector("#orderInfoAddress").value.trim(),
            pickupStoreId: pickupStoreId ? Number(pickupStoreId) : null
        };

        const pickup = deliveryType === "PICKUP";

        const nameOk = result.recipientName !== "" && result.recipientName.length <= 120;

        const phoneOk = ORDER_INFO_PHONE_PATTERN.test(result.recipientPhone);

        const storeOk = Boolean((stores || []).find(function (store) {
            return String(store.id) === String(result.pickupStoreId) && !lacks(store);
        }));

        const addressOk = result.shippingAddress !== "";

        setError("name", nameOk ? "" : "Vui lòng nhập họ và tên người nhận (tối đa 120 ký tự).");
        setError("phone", phoneOk ? "" : "Vui lòng nhập số điện thoại hợp lệ (8–20 ký tự số).");
        setError("address", pickup || addressOk ? "" : "Vui lòng nhập địa chỉ nhận hàng đầy đủ.");
        setError("store", !pickup || storeOk ? "" : "Vui lòng chọn cửa hàng đang mở để nhận hàng.");

        if (!nameOk || !phoneOk || (pickup ? !storeOk : !addressOk)) {
            return;
        }

        /* chỉ giữ phần của hình thức đang chọn */
        const info = {
            deliveryType: deliveryType,
            recipientName: result.recipientName,
            recipientPhone: result.recipientPhone,
            shippingAddress: pickup ? "" : result.shippingAddress,
            pickupStoreId: pickup ? result.pickupStoreId : null,
            pickupStoreName: pickup ? (stores.find(function (store) { return String(store.id) === String(result.pickupStoreId); }) || {}).name || "" : ""
        };

        saveOrderInfo(info);

        closeModal();

        if (options && typeof options.onSave === "function") {
            options.onSave(info);
        }

    });

    form.querySelector("#orderInfoName").focus();

}
