/*
 * "TRỢ LÝ TRA CỨU NHANH" NỔI Ở GÓC TRANG — KHÔNG PHẢI AI (kiemthu GĐ7).
 *
 * Tách biệt với trang khuyến nghị; tự chèn HTML vào <body> trên mọi trang khách (nạp cuối, sau main.js).
 * Trả lời theo từ khoá:
 *   - chính sách: viết theo đúng backend (ShippingPolicy, AfterSalesServiceImpl 7 ngày, InstallmentPolicy);
 *   - đơn hàng: link "Đơn hàng của tôi" (chưa đăng nhập → đăng nhập rồi quay lại);
 *   - còn lại: tìm sản phẩm thật GET /products?isActive=true&keyword=…&size=3.
 * Chatbot AI thật làm ở Phase 9 (AI_Service).
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


const CHATBOT_SEARCH_SIZE = 3;

const CHATBOT_KEYWORD_MAX = 100;


/* Từ khoá so khớp sau khi bỏ dấu + chữ thường (chatbotNormalize) */

const CHATBOT_TOPICS = [
    {
        keywords: ["giao hang", "ship", "van chuyen", "phi giao", "nhan tai cua hang"],
        reply: function () {
            return {
                text: "Giao tận nhà: phí 30.000đ, miễn phí cho đơn từ 10.000.000đ. " +
                    "Nhận tại cửa hàng: miễn phí, chọn cửa hàng ở bước thanh toán.",
                links: [["Xem cửa hàng ở trang Dịch vụ", "customer/services.html"]]
            };
        }
    },
    {
        keywords: ["tra hang", "doi tra", "hoan tien", "doi hang", "doi san pham"],
        reply: function () {
            return {
                text: "POY nhận trả hàng hoàn tiền trong 7 ngày kể từ khi nhận hàng, với mọi lý do; " +
                    "hoàn theo giá bạn đã trả, không gồm phí giao hàng. Muốn đổi sản phẩm khác: trả hàng rồi đặt đơn mới. " +
                    "Đơn trả góp vui lòng liên hệ cửa hàng. Gửi yêu cầu ở trang chi tiết đơn đã giao.",
                links: [["Chính sách trả hàng", "customer/services.html#chinh-sach-doi-tra"]]
            };
        }
    },
    {
        keywords: ["bao hanh", "bao tri", "sua chua", "loi may"],
        reply: function () {
            return {
                text: "Thời hạn bảo hành ghi ở trang chi tiết từng sản phẩm. Còn hạn: gửi yêu cầu bảo hành ở chi tiết đơn đã giao, " +
                    "sửa miễn phí tại chi nhánh bán hàng. Hết hạn vẫn gửi được yêu cầu bảo trì (có thể mất phí).",
                links: [["Yêu cầu dịch vụ của tôi", "customer/service-requests.html"]]
            };
        }
    },
    {
        keywords: ["tra gop", "thanh toan", "cod", "chuyen khoan", "the tin dung", "qr"],
        reply: function () {
            return {
                text: "POY nhận thanh toán khi nhận hàng (COD), chuyển khoản (mã QR hiện ở trang đơn hàng) và trả góp 0% " +
                    "kỳ hạn 3, 6, 9 hoặc 12 tháng cho đơn từ 3.000.000đ (nhập CCCD và ngân hàng phát hành thẻ, cửa hàng duyệt hồ sơ).",
                links: [["Hỏi đáp thanh toán", "customer/services.html"]]
            };
        }
    },
    {
        keywords: ["don hang", "don cua toi", "tinh trang", "theo doi", "tra cuu don", "huy don"],
        reply: function () {
            const loggedIn = typeof isLoggedIn === "function" && isLoggedIn();
            return loggedIn
                ? { text: "Xem tình trạng, huỷ đơn chờ xác nhận hoặc gửi bảo hành / trả hàng ở Đơn hàng của tôi.", links: [["Đơn hàng của tôi", "customer/orders.html"]] }
                : { text: "Bạn đăng nhập để xem đơn hàng của mình nhé.", links: [["Đăng nhập", "auth/login.html?redirect=" + encodeURIComponent("customer/orders.html")]] };
        }
    },
    {
        keywords: ["lien he", "hotline", "nhan vien", "tu van", "ho tro", "khieu nai"],
        reply: function () {
            return {
                text: "Bạn gửi câu hỏi ở trang Liên hệ, nhân viên POY sẽ trả lời qua email hoặc điện thoại bạn để lại.",
                links: [["Gửi tin nhắn cho POY", "customer/contact.html"]]
            };
        }
    }
];


const CHATBOT_QUICK_REPLIES = [
    "Phí giao hàng?",
    "Trả hàng thế nào?",
    "Mua trả góp?",
    "Tra cứu đơn hàng"
];


function chatbotNormalize(text) {

    return text.normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/đ/g, "d").replace(/Đ/g, "D")
        .toLowerCase().replace(/\s+/g, " ").trim();

}


function findChatbotTopic(message) {

    const text = chatbotNormalize(message);

    return CHATBOT_TOPICS.find(function (topic) {
        return topic.keywords.some(function (keyword) { return text.includes(keyword); });
    }) || null;

}


function injectChatbotWidget() {

    if (document.getElementById("chatbotWidget")) {
        return;
    }


    const widget = document.createElement("div");
    widget.id = "chatbotWidget";
    widget.className = "chatbot-widget";

    widget.innerHTML = `

        <button type="button" class="chatbot-launcher" id="chatbotLauncher" aria-label="Mở trợ lý tra cứu nhanh">
            💬
        </button>

        <div class="chatbot-panel" id="chatbotPanel" role="dialog" aria-label="Trợ lý tra cứu nhanh" hidden>

            <div class="chatbot-header">
                <div>
                    <strong>Trợ lý tra cứu nhanh</strong>
                    <span>Tra cứu theo từ khoá · không phải AI</span>
                </div>
                <button type="button" class="chatbot-close" id="chatbotClose" aria-label="Đóng">✕</button>
            </div>

            <div class="chatbot-messages" id="chatbotMessages" aria-live="polite"></div>

            <div class="chatbot-quick-replies" id="chatbotQuickReplies"></div>

            <form class="chatbot-input-row" id="chatbotForm">
                <input
                    type="text"
                    id="chatbotInput"
                    placeholder="Hỏi chính sách hoặc gõ tên sản phẩm..."
                    aria-label="Câu hỏi hoặc tên sản phẩm"
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
        "Xin chào! Đây là trợ lý tra cứu nhanh của POY, trả lời theo từ khoá (không phải AI). " +
        "Bạn có thể hỏi về giao hàng, trả hàng, bảo hành, trả góp, đơn hàng, hoặc gõ tên sản phẩm để tìm.",
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
        launcher.focus();

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

        CHATBOT_QUICK_REPLIES.forEach(function (text) {

            const chip = document.createElement("button");

            chip.type = "button";
            chip.className = "chatbot-chip";
            chip.textContent = text;

            chip.addEventListener("click", function () {
                sendUserMessage(text);
            });

            container.appendChild(chip);

        });

    }


    async function sendUserMessage(text) {

        addMessage(text, "user");

        const topic = findChatbotTopic(text);

        if (topic) {

            const answer = topic.reply();

            addMessage(answer.text, "bot", { links: answer.links });

            return;

        }

        const typingEl = addMessage("Đang tìm sản phẩm...", "bot", { typing: true });

        const keyword = text.slice(0, CHATBOT_KEYWORD_MAX);

        try {

            const page = await apiRequest("/products?" + new URLSearchParams({
                isActive: "true", keyword: keyword, size: String(CHATBOT_SEARCH_SIZE), page: "0"
            }).toString());

            typingEl.remove();

            const products = page.content || [];

            if (products.length === 0) {

                addMessage(
                    "Không tìm thấy sản phẩm khớp \"" + keyword + "\". Bạn thử từ khoá khác (ví dụ \"iPhone 16\", \"laptop Dell\"), " +
                    "hoặc gửi câu hỏi cho nhân viên.",
                    "bot",
                    { links: [["Gửi tin nhắn cho POY", "customer/contact.html"]] }
                );

                return;

            }

            addMessage(
                "Tìm thấy " + page.totalElements.toLocaleString("vi-VN") + " sản phẩm khớp \"" + keyword + "\"" +
                (page.totalElements > products.length ? ", đây là " + products.length + " sản phẩm đầu:" : ":"),
                "bot",
                {
                    products: products,
                    links: page.totalElements > products.length
                        ? [["Xem tất cả kết quả", "customer/products.html?search=" + encodeURIComponent(keyword)]]
                        : []
                }
            );

        } catch (error) {

            typingEl.remove();

            addMessage("Không tra cứu được lúc này. Bạn thử lại sau, hoặc tìm ở trang Sản phẩm.", "bot",
                { links: [["Trang Sản phẩm", "customer/products.html"]] });

        }

    }


    /* Mọi nội dung đưa vào bằng textContent / thuộc tính, không dùng innerHTML với dữ liệu */

    function addMessage(text, sender, extra) {

        const options = extra || {};

        const bubble = document.createElement("div");

        bubble.className = "chatbot-message chatbot-message-" + sender + (options.typing ? " chatbot-typing" : "");

        const paragraph = document.createElement("p");

        paragraph.textContent = text;

        bubble.appendChild(paragraph);


        if (options.products && options.products.length) {

            const list = document.createElement("ul");

            list.className = "chatbot-products";

            options.products.forEach(function (product) {

                const item = document.createElement("li");

                const link = document.createElement("a");

                link.href = getProductDetailUrl(product.id);

                link.textContent = product.name;

                const price = document.createElement("span");

                const value = getDisplayPrice(product);

                price.textContent = value > 0 ? formatPrice(value) : "Liên hệ";

                item.appendChild(link);

                item.appendChild(price);

                list.appendChild(item);

            });

            bubble.appendChild(list);

        }


        if (options.links && options.links.length) {

            const links = document.createElement("p");

            links.className = "chatbot-links";

            options.links.forEach(function (pair) {

                const link = document.createElement("a");

                link.href = siteUrl(pair[1]);

                link.textContent = pair[0] + " →";

                links.appendChild(link);

            });

            bubble.appendChild(links);

        }


        messages.appendChild(bubble);

        messages.scrollTop = messages.scrollHeight;

        return bubble;

    }

}
