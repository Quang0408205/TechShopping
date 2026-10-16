/* ================= TRANG THANH TOÁN CHUYỂN KHOẢN ================= */

/*
 * customer/payment.html?id=<id đơn>: hiện ngay sau khi đặt đơn chuyển khoản.
 * Dữ liệu: GET /api/v1/orders/{id} (fetchMyOrder). Cứ 5 giây hỏi lại một lần; khi
 * webhook ngân hàng xác nhận (payment.status = PAID) trang tự chuyển sang
 * "Đã nhận được thanh toán". Đơn không còn chờ tiền (đã hủy, đổi cách trả...) →
 * khối thông báo. Mọi dữ liệu đưa vào innerHTML đều escape.
 */

const PAYMENT_POLL_MS = 5000;

const PAYMENT_AUTO_OPEN_MS = 4000;

/* Chế độ demo (PAYMENT_SIMULATOR_ENABLED): QR hiện đủ 1 phút là tính đã chuyển khoản */
const PAYMENT_DEMO_AFTER_MS = 60000;

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


        const orderId = new URLSearchParams(window.location.search).get("id");

        const loading = document.getElementById("payLoading");

        const errorBox = document.getElementById("payError");

        let timer = null;

        let countdown = null;

        let demoTimer = null;


        if (!orderId || !/^\d{1,18}$/.test(orderId)) {
            showOnly("payNotFound");
            return;
        }

        const orderUrl = getOrderDetailUrl(orderId);

        ["payViewOrder", "payDoneLink", "payClosedLink"].forEach(function (id) {
            document.getElementById(id).href = orderUrl;
        });

        document.getElementById("payWaiting").addEventListener("click", copyValue);

        document.getElementById("payQr").addEventListener("error", function () {
            this.hidden = true;
            document.getElementById("payQrError").hidden = false;
        });

        errorBox.addEventListener("click", function (event) {
            if (event.target.closest("#payRetryBtn")) {
                load(true);
            }
        });


        await load(true);


        async function load(first) {

            let order;

            try {

                order = await fetchMyOrder(orderId);

            } catch (error) {

                if (!isLoggedIn()) {
                    redirectToLogin();
                    return;
                }

                /* Lỗi khi đang chờ: bỏ qua, lần hỏi sau thử lại */
                if (first) {
                    showOnly("payError");
                    errorBox.innerHTML = errorStateHtml(getErrorMessage(error), "payRetryBtn");
                } else {
                    schedule();
                }

                return;

            }

            if (!order) {
                showOnly("payNotFound");
                return;
            }

            render(order);

        }


        function render(order) {

            const transfer = order.payment ? order.payment.bankTransfer : null;

            document.getElementById("pageTitle").textContent = "Thanh toán đơn " + order.code + " - POY";


            if (order.paymentMethod === "BANK_TRANSFER" && order.payment && order.payment.status === "PAID") {

                stop();

                document.getElementById("payDoneText").textContent =
                    "Đơn " + order.code + " (" + formatPrice(order.total) + ") đã được thanh toán. "
                    + "Cửa hàng sẽ xác nhận và giao hàng cho bạn. Đang chuyển tới đơn hàng…";

                showOnly("payDone");

                setTimeout(function () {
                    window.location.href = orderUrl;
                }, PAYMENT_AUTO_OPEN_MS);

                return;

            }


            if (!transfer || order.status === "CANCELLED") {

                stop();

                const cancelled = order.status === "CANCELLED";

                document.getElementById("payClosedTitle").textContent = cancelled
                    ? "Đơn hàng đã được hủy"
                    : "Đơn hàng không cần chuyển khoản";

                document.getElementById("payClosedText").textContent = cancelled
                    ? "Đơn " + order.code + " đã hủy nên không còn chờ thanh toán."
                    : "Đơn " + order.code + " không ở trạng thái chờ chuyển khoản.";

                showOnly("payClosed");

                return;

            }


            renderWaiting(order, transfer);

            showOnly("payWaiting");

            schedule();

        }


        function renderWaiting(order, transfer) {

            document.getElementById("payOrderCode").textContent = order.code;

            document.getElementById("payAmount").textContent = formatPrice(transfer.amount);


            const qr = document.getElementById("payQr");

            document.getElementById("payQrError").hidden = true;

            qr.hidden = !transfer.qrImageUrl;

            if (transfer.qrImageUrl && qr.getAttribute("src") !== transfer.qrImageUrl) {
                qr.src = transfer.qrImageUrl;
            }


            document.getElementById("payFacts").innerHTML = [
                row("Ngân hàng", escapeHtml(transfer.bankName)),
                row("Chủ tài khoản", escapeHtml(transfer.accountName)),
                row("Số tài khoản", escapeHtml(transfer.accountNumber), transfer.accountNumber, "số tài khoản"),
                row("Số tiền", escapeHtml(formatPrice(transfer.amount)), null, null, true),
                row("Nội dung", escapeHtml(transfer.transferContent), null, null, true)
            ].join("");


            startCountdown(transfer.payBefore);

            /* Demo: QR đã hiện 1 phút trên trang → coi như đã nhận tiền; rời trang trước đó thì chưa tính */
            if (transfer.demoAutoPay && !demoTimer) {
                demoTimer = setTimeout(confirmDemoPayment, PAYMENT_DEMO_AFTER_MS);
            }

        }


        async function confirmDemoPayment() {

            try {

                await apiRequest("/orders/" + encodeURIComponent(orderId) + "/payment/demo-confirm", { method: "POST", auth: true });

            } catch (error) {
                /* lỗi thì lần hỏi lại kế tiếp vẫn hiện đúng trạng thái */
            }

            clearTimeout(timer);

            load(false);

        }


        function startCountdown(payBefore) {

            clearInterval(countdown);

            const line = document.getElementById("payDeadline");

            const end = payBefore ? new Date(payBefore).getTime() : NaN;

            if (Number.isNaN(end)) {
                line.textContent = "";
                return;
            }

            const tick = function () {

                const left = end - Date.now();

                if (left <= 0) {
                    line.textContent = "Đã quá hạn chuyển khoản (" + formatOrderDateTime(payBefore) + "). Liên hệ cửa hàng nếu bạn đã chuyển.";
                    clearInterval(countdown);
                    return;
                }

                const minutes = Math.floor(left / 60000);

                const text = minutes >= 60
                    ? Math.floor(minutes / 60) + " giờ " + (minutes % 60) + " phút"
                    : minutes + " phút " + String(Math.floor((left % 60000) / 1000)).padStart(2, "0") + " giây";

                line.textContent = "Chuyển khoản trong " + text + " (trước " + formatOrderDateTime(payBefore) + ")";

            };

            tick();

            countdown = setInterval(tick, 1000);

        }


        function schedule() {

            clearTimeout(timer);

            if (document.hidden) {
                document.addEventListener("visibilitychange", function once() {
                    document.removeEventListener("visibilitychange", once);
                    schedule();
                });
                return;
            }

            timer = setTimeout(function () {
                load(false);
            }, PAYMENT_POLL_MS);

        }


        function stop() {

            clearTimeout(timer);

            clearTimeout(demoTimer);

            clearInterval(countdown);

        }


        function showOnly(id) {

            loading.hidden = true;

            ["payError", "payNotFound", "payWaiting", "payDone", "payClosed"].forEach(function (name) {
                document.getElementById(name).hidden = name !== id;
            });

        }


        function row(label, valueHtml, copyValue, copyLabel, strong) {

            const button = copyValue
                ? `<button type="button" class="pay-copy" data-copy="${escapeHtml(copyValue)}" data-copy-label="${escapeHtml(copyLabel)}">Sao chép</button>`
                : "";

            return `<div class="pay-fact${strong ? " pay-fact--strong" : ""}">`
                + `<dt>${escapeHtml(label)}</dt><dd>${valueHtml}</dd>${button}</div>`;

        }


        async function copyValue(event) {

            const button = event.target.closest("button[data-copy]");

            if (!button) {
                return;
            }

            try {

                await navigator.clipboard.writeText(button.dataset.copy);

                showToast("Đã sao chép " + button.dataset.copyLabel + ".", "success");

            } catch (error) {

                showToast("Trình duyệt không cho sao chép, vui lòng chép thủ công.", "error");

            }

        }

    }
);
