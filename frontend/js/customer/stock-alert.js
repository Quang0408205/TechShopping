/* ================= "BÁO TÔI KHI CÓ HÀNG" (trang chi tiết sản phẩm) ================= */

/*
 * updateStockAlert(variantId, soldOut) — product-detail.js gọi mỗi khi đổi phiên bản hoặc tồn kho.
 * Phiên bản hết hàng ở mọi chi nhánh đang mở → hiện nút; bấm khi chưa đăng nhập → trang đăng nhập.
 * API: GET /stock-subscriptions (id phiên bản đang chờ), POST /stock-subscriptions { variantId },
 *      DELETE /stock-subscriptions/{variantId}. Hàng về thì cửa hàng gửi một email rồi xoá đăng ký.
 */

let stockAlertVariantId = null;
let stockAlertWaiting = new Set();
let stockAlertLoaded = false;


async function updateStockAlert(variantId, soldOut) {

    const box = document.getElementById("stockAlert");

    if (!box) {
        return;
    }

    stockAlertVariantId = variantId;

    if (!variantId || !soldOut) {
        box.hidden = true;
        return;
    }

    if (isLoggedIn() && !stockAlertLoaded) {

        stockAlertLoaded = true;

        try {
            stockAlertWaiting = new Set(await apiRequest("/stock-subscriptions", { auth: true }) || []);
        } catch (error) {
            stockAlertWaiting = new Set();
        }

        if (stockAlertVariantId !== variantId) {
            return;
        }

    }

    renderStockAlert(box);

}


function renderStockAlert(box) {

    const waiting = stockAlertWaiting.has(stockAlertVariantId);

    box.hidden = false;

    box.innerHTML = waiting
        ? `<p class="stock-alert-text">Bạn sẽ nhận email khi sản phẩm này có hàng lại.</p>
           <button type="button" class="btn btn-outline-dark stock-alert-btn" data-stock-alert="off">BỎ THEO DÕI</button>`
        : `<p class="stock-alert-text">Sản phẩm tạm hết hàng. Để lại email của tài khoản, POY báo ngay khi có hàng.</p>
           <button type="button" class="btn btn-dark stock-alert-btn" data-stock-alert="on">BÁO TÔI KHI CÓ HÀNG</button>`;

    box.querySelector("button").addEventListener("click", async function (event) {

        if (!isLoggedIn()) {
            redirectToLogin("stock");
            return;
        }

        const button = event.currentTarget;

        const variantId = stockAlertVariantId;

        button.disabled = true;

        try {

            if (button.dataset.stockAlert === "on") {

                await apiRequest("/stock-subscriptions", { method: "POST", body: { variantId: variantId }, auth: true });

                stockAlertWaiting.add(variantId);

                showToast("Đã đăng ký. POY sẽ gửi email khi có hàng.", "success");

            } else {

                await apiRequest("/stock-subscriptions/" + variantId, { method: "DELETE", auth: true });

                stockAlertWaiting.delete(variantId);

            }

        } catch (error) {

            showToast(getErrorMessage(error), "error");

        }

        if (stockAlertVariantId === variantId) {
            renderStockAlert(box);
        }

    });

}
