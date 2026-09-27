/*
 * WIDGET CHATBOT NỔI (FLOATING WIDGET) — HOÀN TOÀN TÁCH BIỆT VỚI TRANG KHUYẾN NGHỊ.
 *
 * Đây là một widget hỗ trợ khách hàng dạng câu hỏi thường gặp (FAQ), KHÔNG PHẢI
 * hệ thống khuyến nghị sản phẩm và không dùng chung dữ liệu/giao diện với
 * customer/recommendation.html. Widget này tự chèn HTML của nó vào <body> và
 * hoạt động trên MỌI TRANG (được nạp cuối, sau js/core/main.js).
 *
 * Câu trả lời hiện là các câu trả lời dựng sẵn (canned response) theo từ khóa,
 * hiển thị rõ ràng là trợ lý tự động — KHÔNG giả vờ là con người hay AI thật.
 * CHỜ BACKEND: khi có dịch vụ chatbot thật, thay hàm getBotReply() bằng
 * apiRequest("/chatbot/messages", { method: "POST", body: { message } }).
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        if (typeof layoutReady !== "undefined") {
            await layoutReady;
        }

        injectChatbotWidget();

    }
);


const CHATBOT_FAQ = [
    {
        keywords: ["giao hàng", "ship", "vận chuyển", "bao lâu"],
        reply: "Đơn hàng thường được giao trong 2-4 ngày làm việc. Miễn phí vận chuyển cho đơn từ 10.000.000đ."
    },
    {
        keywords: ["đổi trả", "hoàn tiền", "trả hàng"],
        reply: "Bạn có thể đổi trả sản phẩm trong vòng 7 ngày kể từ khi nhận hàng, với điều kiện sản phẩm còn nguyên vẹn."
    },
    {
        keywords: ["bảo hành"],
        reply: "Sản phẩm được bảo hành chính hãng từ 12-24 tháng tùy loại. Bạn có thể xem chi tiết ở trang Dịch vụ."
    },
    {
        keywords: ["thanh toán", "trả góp", "cod"],
        reply: "POY hỗ trợ thanh toán khi nhận hàng (COD), chuyển khoản ngân hàng và trả góp qua thẻ tín dụng."
    },
    {
        keywords: ["giờ", "hotline", "liên hệ", "hỗ trợ"],
        reply: "Tổng đài 1900 0000 hỗ trợ từ 08:00 - 22:00 mỗi ngày. Bạn cũng có thể gửi yêu cầu ở trang Liên hệ."
    },
    {
        keywords: ["đơn hàng", "tình trạng", "theo dõi"],
        reply: "Bạn có thể theo dõi tình trạng đơn hàng tại mục \"Đơn hàng\" trong trang Tài khoản."
    }
];


function getBotReply(message) {

    const text = message.toLowerCase();

    const match = CHATBOT_FAQ.find(function (entry) {
        return entry.keywords.some(function (keyword) { return text.includes(keyword); });
    });

    if (match) {
        return match.reply;
    }

    return "Cảm ơn bạn đã nhắn tin. Đây là trợ lý hỗ trợ tự động với câu trả lời dựng sẵn — " +
        "với câu hỏi này, đội ngũ chăm sóc khách hàng sẽ cần hỗ trợ trực tiếp qua " +
        "trang Liên hệ hoặc hotline 1900 0000.";

}


const CHATBOT_QUICK_REPLIES = [
    "Thời gian giao hàng?",
    "Chính sách đổi trả?",
    "Phương thức thanh toán?"
];


function injectChatbotWidget() {

    if (document.getElementById("chatbotWidget")) {
        return;
    }


    const widget = document.createElement("div");
    widget.id = "chatbotWidget";
    widget.className = "chatbot-widget";

    widget.innerHTML = `

        <button type="button" class="chatbot-launcher" id="chatbotLauncher" aria-label="Mở hỗ trợ trực tuyến">
            💬
        </button>

        <div class="chatbot-panel" id="chatbotPanel" hidden>

            <div class="chatbot-header">
                <div>
                    <strong>Hỗ trợ trực tuyến</strong>
                    <span>Trợ lý tự động - phản hồi tức thì</span>
                </div>
                <button type="button" class="chatbot-close" id="chatbotClose" aria-label="Đóng">✕</button>
            </div>

            <div class="chatbot-messages" id="chatbotMessages"></div>

            <div class="chatbot-quick-replies" id="chatbotQuickReplies"></div>

            <form class="chatbot-input-row" id="chatbotForm">
                <input
                    type="text"
                    id="chatbotInput"
                    placeholder="Nhập câu hỏi của bạn..."
                    maxlength="300"
                    autocomplete="off"
                >
                <button type="submit" aria-label="Gửi">➤</button>
            </form>

        </div>

    `;

    document.body.appendChild(widget);


    const launcher = document.getElementById("chatbotLauncher");
    const panel = document.getElementById("chatbotPanel");
    const closeBtn = document.getElementById("chatbotClose");
    const form = document.getElementById("chatbotForm");
    const input = document.getElementById("chatbotInput");
    const messages = document.getElementById("chatbotMessages");


    addMessage(
        "Xin chào! Mình là trợ lý hỗ trợ tự động của POY. Bạn cần giúp gì hôm nay?",
        "bot"
    );

    renderQuickReplies();


    launcher.addEventListener("click", function () {

        const isOpen = !panel.hidden;

        panel.hidden = isOpen;
        launcher.classList.toggle("active", !isOpen);

        if (!isOpen) {
            input.focus();
        }

    });

    closeBtn.addEventListener("click", function () {

        panel.hidden = true;
        launcher.classList.remove("active");

    });

    form.addEventListener("submit", function (event) {

        event.preventDefault();

        const text = input.value.trim();

        if (!text) {
            return;
        }

        sendUserMessage(text);

        input.value = "";

    });


    function renderQuickReplies() {

        const container = document.getElementById("chatbotQuickReplies");

        container.innerHTML = CHATBOT_QUICK_REPLIES.map(function (text) {

            return `<button type="button" class="chatbot-chip">${text}</button>`;

        }).join("");

        container.querySelectorAll(".chatbot-chip").forEach(function (chip) {

            chip.addEventListener("click", function () {

                sendUserMessage(chip.textContent);

            });

        });

    }


    function sendUserMessage(text) {

        addMessage(text, "user");

        const typingEl = addMessage("Đang trả lời...", "bot", true);


        setTimeout(function () {

            typingEl.remove();

            addMessage(getBotReply(text), "bot");

        }, 500);

    }


    function addMessage(text, sender, isTyping) {

        const bubble = document.createElement("div");

        bubble.className = "chatbot-message chatbot-message-" + sender + (isTyping ? " chatbot-typing" : "");
        bubble.textContent = text;

        messages.appendChild(bubble);

        messages.scrollTop = messages.scrollHeight;

        return bubble;

    }

}
