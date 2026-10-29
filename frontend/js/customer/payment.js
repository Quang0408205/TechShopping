/* ================= TRANG THANH TOÁN (CHUYỂN KHOẢN / THẺ QUỐC TẾ) ================= */

/*
 * customer/payment.html?id=<id đơn>: hiện ngay sau khi đặt đơn chuyển khoản hoặc thẻ.
 * Dữ liệu: GET /api/v1/orders/{id} (fetchMyOrder). Cứ 5 giây hỏi lại một lần; khi
 * tiền về (payment.status = PAID) trang tự chuyển sang "Đã nhận được thanh toán".
 * - Chuyển khoản: thông tin trên (có nút sao chép STK / số tiền / nội dung), QR dưới;
 *   webhook ngân hàng xác nhận.
 * - Thẻ quốc tế (kế hoạch v5 GĐ5, Stripe chế độ Test): nút mở Stripe Checkout; &start=card
 *   (vừa đặt đơn) → tự mở. Stripe đưa khách về &card=paid (hỏi lại Stripe qua
 *   POST …/payment/card-refresh, chạy cả khi không có webhook) hoặc &card=back (chưa trả).
 * - "Huỷ thanh toán": huỷ đơn, trả lượt voucher, đưa đúng sản phẩm về giỏ → trang giỏ hàng.
 * Đơn không còn chờ tiền (đã huỷ, đổi cách trả...) → khối thông báo. Mọi dữ liệu đưa vào
 * innerHTML đều escape.
 */

const PAYMENT_POLL_MS = 5000;

const PAYMENT_AUTO_OPEN_MS = 4000;

/* Chế độ demo (PAYMENT_SIMULATOR_ENABLED): QR hiện đủ 1 phút là tính đã chuyển khoản */
const PAYMENT_DEMO_AFTER_MS = 60000;

/* Quay về từ Stripe (&card=paid): webhook có thể tới chậm vài giây → hỏi Stripe thêm vài lần */
const CARD_REFRESH_TRIES = 6;

/* Logo ngân hàng theo mã BIN VietQR (api.vietqr.io/img/<mã>.png); không có → chỉ hiện tên */
const BANK_LOGO_CODES = {
    "970436": "VCB", "970422": "MB", "970407": "TCB", "970415": "ICB", "970418": "BIDV",
    "970432": "VPB", "970423": "TPB", "970405": "VBA", "970416": "ACB", "970403": "STB",
    "970441": "VIB", "970443": "SHB", "970437": "HDB", "970426": "MSB", "970448": "OCB"
};

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


        const params = new URLSearchParams(window.location.search);

        const orderId = params.get("id");

        const cardReturn = params.get("card");

        let autoStartCard = params.get("start") === "card";

        let cardRefreshLeft = cardReturn === "paid" ? CARD_REFRESH_TRIES : 0;

        const loading = document.getElementById("payLoading");

        const errorBox = document.getElementById("payError");

        const cardButton = document.getElementById("payCardBtn");

        let currentOrder = null;

        let timer = null;

        let countdown = null;

        let demoTimer = null;

        let sandboxTimer = null;


        if (!orderId || !/^\d{1,18}$/.test(orderId)) {
            showOnly("payNotFound");
            return;
        }

        /* Bỏ start / card khỏi địa chỉ: tải lại hoặc Back từ Stripe không tự mở Stripe lần nữa */
        if (autoStartCard || cardReturn) {
            history.replaceState(null, "", window.location.pathname + "?id=" + encodeURIComponent(orderId));
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

        cardButton.addEventListener("click", openStripe);

        document.getElementById("paySandboxBtn").addEventListener("click", confirmDemoPayment);

        document.getElementById("payCancelBtn").addEventListener("click", function () {

            if (!currentOrder) {
                return;
            }

            confirmCancelPayment(currentOrder, function () {

                stop();

                window.location.replace("cart.html");

            });

        });


        await load(true);


        async function load(first) {

            let order;

            try {

                order = cardRefreshLeft > 0
                    ? await refreshCardPayment(orderId)
                    : await fetchMyOrder(orderId);

            } catch (error) {

                if (!isLoggedIn()) {
                    redirectToLogin();
                    return;
                }

                if (error.status === 404) {
                    showOnly("payNotFound");
                    return;
                }

                /* Hỏi Stripe lỗi: lần sau đọc đơn như thường (webhook vẫn có thể xác nhận) */
                if (cardRefreshLeft > 0) {
                    cardRefreshLeft = 0;
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

            if (cardRefreshLeft > 0) {
                cardRefreshLeft--;
            }

            currentOrder = order;

            render(order);

        }


        function render(order) {

            const payment = order.payment;

            const transfer = payment ? payment.bankTransfer : null;

            const card = payment ? payment.card : null;

            const online = order.paymentMethod === "BANK_TRANSFER" || order.paymentMethod === "CARD";

            document.getElementById("pageTitle").textContent = "Thanh toán đơn " + order.code + " - POY";


            if (online && payment && payment.status === "PAID") {

                stop();

                document.getElementById("payDoneText").textContent =
                    "Đơn " + order.code + " (" + formatPrice(order.total) + ") đã được thanh toán"
                    + (order.paymentMethod === "CARD" ? " bằng thẻ" : "") + ". "
                    + "Cửa hàng sẽ xác nhận và giao hàng cho bạn. Đang chuyển tới đơn hàng…";

                showOnly("payDone");

                setTimeout(function () {
                    window.location.href = orderUrl;
                }, PAYMENT_AUTO_OPEN_MS);

                return;

            }


            if ((!transfer && !card) || order.status === "CANCELLED") {

                stop();

                const cancelled = order.status === "CANCELLED";

                document.getElementById("payClosedTitle").textContent = cancelled
                    ? "Đơn hàng đã được huỷ"
                    : "Đơn hàng không cần thanh toán trước";

                document.getElementById("payClosedText").textContent = cancelled
                    ? "Đơn " + order.code + " đã huỷ nên không còn chờ thanh toán."
                    : "Đơn " + order.code + " không ở trạng thái chờ thanh toán.";

                showOnly("payClosed");

                return;

            }


            renderHead(order, transfer || card);

            if (card) {
                renderCard(card);
            } else {
                renderTransfer(transfer);
            }

            showOnly("payWaiting");

            if (autoStartCard && card) {

                autoStartCard = false;

                openStripe();

                return;

            }

            schedule();

        }


        function renderHead(order, pending) {

            document.getElementById("payOrderCode").textContent = order.code;

            document.getElementById("payAmount").textContent = formatPrice(order.payment.amount || order.total);

            document.getElementById("payMethodLine").textContent = order.paymentMethod === "CARD"
                ? "Thẻ quốc tế (Visa / Mastercard)"
                : "Chuyển khoản ngân hàng";

            startCountdown(pending.payBefore, order.paymentMethod === "CARD");

        }


        function renderTransfer(transfer) {

            document.getElementById("payTransferBody").hidden = false;

            document.getElementById("payCardBody").hidden = true;

            document.getElementById("payWaitingText").textContent =
                "Đang chờ nhận tiền… Trang tự cập nhật khi cửa hàng nhận được tiền.";


            const qr = document.getElementById("payQr");

            document.getElementById("payQrError").hidden = true;

            qr.hidden = !transfer.qrImageUrl;

            if (transfer.qrImageUrl && qr.getAttribute("src") !== transfer.qrImageUrl) {
                qr.src = transfer.qrImageUrl;
            }


            const logoCode = BANK_LOGO_CODES[transfer.bankBin];

            const bankHtml = (logoCode
                ? `<img class="pay-bank-logo" src="https://api.vietqr.io/img/${logoCode}.png" alt="">`
                : "") + escapeHtml(transfer.bankName);

            document.getElementById("payFacts").innerHTML = [
                row("Ngân hàng", bankHtml),
                row("Chủ tài khoản", escapeHtml(transfer.accountName)),
                row("Số tài khoản", escapeHtml(transfer.accountNumber), transfer.accountNumber, "số tài khoản"),
                row("Số tiền", escapeHtml(formatPrice(transfer.amount)), String(Math.ceil(transfer.amount)), "số tiền", true),
                row("Nội dung", escapeHtml(transfer.transferContent), transfer.transferContent, "nội dung chuyển khoản", true)
            ].join("");

            document.querySelectorAll("#payFacts .pay-bank-logo").forEach(function (logo) {
                logo.addEventListener("error", function () { logo.remove(); });
            });


            /* Chế độ thử nghiệm: nút giả lập ngân hàng báo tiền; không bấm thì QR hiện đủ 1 phút là tính đã nhận tiền (rời trang trước đó thì chưa tính) */
            document.getElementById("paySandbox").hidden = !transfer.demoAutoPay;

            if (transfer.demoAutoPay && !demoTimer) {
                demoTimer = setTimeout(confirmDemoPayment, PAYMENT_DEMO_AFTER_MS);
                startSandboxCountdown();
            }

        }


        function renderCard(card) {

            document.getElementById("payTransferBody").hidden = true;

            document.getElementById("payCardBody").hidden = false;

            document.getElementById("payCardTest").hidden = !card.testMode;

            document.getElementById("payWaitingText").textContent = cardRefreshLeft > 0
                ? "Đang xác nhận thanh toán với Stripe…"
                : "Chưa nhận được thanh toán thẻ. Bấm nút trên để thanh toán.";

            const notice = document.getElementById("payCardNotice");

            if (cardReturn === "back" || (cardReturn === "paid" && cardRefreshLeft === 0)) {

                notice.textContent = cardReturn === "back"
                    ? "Bạn chưa hoàn tất thanh toán thẻ. Bấm \"Thanh toán bằng thẻ\" để thử lại, hoặc huỷ thanh toán để lấy lại giỏ hàng."
                    : "Chưa nhận được xác nhận từ Stripe. Nếu bạn đã thanh toán, trang sẽ tự cập nhật trong giây lát.";

                notice.hidden = false;

            }

        }


        /* POST …/payment/card-checkout → chuyển sang trang Stripe (hoặc đã trả rồi → tải lại) */
        async function openStripe() {

            cardButton.disabled = true;

            cardButton.querySelector("span").textContent = "Đang mở Stripe…";

            let result;

            try {

                result = await startCardCheckout(orderId, window.location.origin + window.location.pathname);

            } catch (error) {

                cardButton.disabled = false;

                cardButton.querySelector("span").textContent = "Thanh toán bằng thẻ";

                showToast(getErrorMessage(error), "error");

                schedule();

                return;

            }

            if (result && result.checkoutUrl) {

                stop();

                window.location.href = result.checkoutUrl;

                return;

            }

            cardButton.disabled = false;

            cardButton.querySelector("span").textContent = "Thanh toán bằng thẻ";

            load(false);

        }


        function startSandboxCountdown() {

            const end = Date.now() + PAYMENT_DEMO_AFTER_MS;

            const left = document.getElementById("paySandboxLeft");

            const tick = function () {
                left.textContent = "(" + Math.max(0, Math.ceil((end - Date.now()) / 1000)) + "s)";
            };

            tick();

            clearInterval(sandboxTimer);

            sandboxTimer = setInterval(tick, 1000);

        }


        async function confirmDemoPayment() {

            clearTimeout(demoTimer);

            clearInterval(sandboxTimer);

            const button = document.getElementById("paySandboxBtn");

            button.disabled = true;

            button.textContent = "Đang xử lý…";

            try {

                await apiRequest("/orders/" + encodeURIComponent(orderId) + "/payment/demo-confirm", { method: "POST", auth: true });

            } catch (error) {
                /* lỗi thì lần hỏi lại kế tiếp vẫn hiện đúng trạng thái */
            }

            clearTimeout(timer);

            load(false);

        }


        function startCountdown(payBefore, card) {

            clearInterval(countdown);

            const line = document.getElementById("payDeadline");

            const end = payBefore ? new Date(payBefore).getTime() : NaN;

            if (Number.isNaN(end)) {
                line.textContent = "";
                return;
            }

            const verb = card ? "Thanh toán" : "Chuyển khoản";

            const tick = function () {

                const left = end - Date.now();

                if (left <= 0) {
                    line.textContent = "Đã quá hạn thanh toán (" + formatOrderDateTime(payBefore) + "). Liên hệ cửa hàng nếu bạn đã trả tiền.";
                    line.classList.add("is-late");
                    clearInterval(countdown);
                    return;
                }

                const minutes = Math.floor(left / 60000);

                const text = minutes >= 60
                    ? Math.floor(minutes / 60) + " giờ " + (minutes % 60) + " phút"
                    : minutes + " phút " + String(Math.floor((left % 60000) / 1000)).padStart(2, "0") + " giây";

                line.textContent = verb + " trong " + text + " (trước " + formatOrderDateTime(payBefore) + ")";

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
            }, cardRefreshLeft > 0 ? 2000 : PAYMENT_POLL_MS);

        }


        function stop() {

            clearTimeout(timer);

            clearTimeout(demoTimer);

            clearInterval(sandboxTimer);

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

                button.textContent = "Đã chép";

                button.classList.add("is-copied");

                setTimeout(function () {
                    button.textContent = "Sao chép";
                    button.classList.remove("is-copied");
                }, 1600);

            } catch (error) {

                showToast("Trình duyệt không cho sao chép, vui lòng chép thủ công.", "error");

            }

        }

    }
);
