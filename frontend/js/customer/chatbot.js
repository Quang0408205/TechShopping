/*
 * TRỢ LÝ MUA SẮM AI NỔI Ở GÓC TRANG (báo cáo mục 3.6): tự chèn HTML vào <body> trên mọi trang khách.
 *
 * POST /chat/stream {sessionToken, message} → Backend → AI_Service (Claude + RAG + Function Calling); câu trả lời
 * hiện dần theo luồng (Server-Sent Events), thao tác đề xuất (thêm giỏ, huỷ đơn) hiện thành nút Xác nhận.
 * Gửi kèm token khi đã đăng nhập để trợ lý xem được đơn hàng, giỏ hàng, yêu cầu bảo hành của chính khách.
 * sessionToken lưu ở localStorage để giữ cuộc trò chuyện khi chuyển trang (GET /chat/{token}/messages).
 *
 * Dự phòng khi AI_Service ngừng (503) hoặc mất mạng: trả lời theo từ khoá như bản cũ
 *   - chính sách: viết theo đúng backend (ShippingPolicy, AfterSalesServiceImpl 7 ngày, InstallmentPolicy);
 *   - đơn hàng: link "Đơn hàng của tôi" (chưa đăng nhập → đăng nhập rồi quay lại);
 *   - còn lại: tìm sản phẩm thật GET /products?isActive=true&keyword=…&size=3.
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

const CHATBOT_SESSION_KEY = "poy_chat_session";

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
                    "cho đơn từ 3.000.000đ: bạn chọn đối tác (thẻ tín dụng hoặc công ty tài chính), kỳ hạn của đối tác đó và nhập CCCD. " +
                    "Đối tác duyệt hồ sơ và thu tiền hằng tháng, cửa hàng xác nhận đơn sau khi đối tác chấp thuận.",
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
    "Điện thoại chụp ảnh đẹp dưới 10 triệu?",
    "Trả hàng thế nào?",
    "Mua trả góp cần gì?",
    "Đơn hàng của tôi tới đâu rồi?"
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

        <button type="button" class="chatbot-launcher" id="chatbotLauncher" aria-label="Mở trợ lý mua sắm AI">
            💬
        </button>

        <div class="chatbot-panel" id="chatbotPanel" role="dialog" aria-label="Trợ lý mua sắm AI" hidden>

            <div class="chatbot-header">
                <div>
                    <strong>Trợ lý mua sắm POY</strong>
                    <span>Trợ lý AI · có thể nhầm, hãy kiểm tra lại giá ở trang sản phẩm</span>
                </div>
                <button type="button" class="chatbot-close" id="chatbotClose" aria-label="Đóng">✕</button>
            </div>

            <div class="chatbot-messages" id="chatbotMessages" aria-live="polite"></div>

            <div class="chatbot-quick-replies" id="chatbotQuickReplies"></div>

            <form class="chatbot-input-row" id="chatbotForm">
                <input
                    type="text"
                    id="chatbotInput"
                    placeholder="Hỏi về sản phẩm, đơn hàng, chính sách..."
                    aria-label="Câu hỏi cho trợ lý"
                    maxlength="1000"
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
        "Xin chào! Mình là trợ lý mua sắm AI của POY. Bạn có thể nhờ mình tìm sản phẩm theo nhu cầu, so sánh, " +
        "gợi ý phụ kiện, tra cứu đơn hàng, hoặc hỏi về giao hàng, trả góp, bảo hành, trả hàng.",
        "bot"
    );

    renderQuickReplies();

    let historyLoaded = false;

    let waiting = false;


    launcher.addEventListener("click", function () {

        const isOpen = !panel.hidden;

        panel.hidden = isOpen;
        launcher.classList.toggle("active", !isOpen);

        if (!isOpen) {
            input.focus();
            loadHistory();
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


    function getSessionToken() {

        try {
            return localStorage.getItem(CHATBOT_SESSION_KEY);
        } catch (error) {
            return null;
        }

    }


    function setSessionToken(token) {

        try {
            if (token) {
                localStorage.setItem(CHATBOT_SESSION_KEY, token);
            } else {
                localStorage.removeItem(CHATBOT_SESSION_KEY);
            }
        } catch (error) {
            /* bỏ qua */
        }

    }


    /* Mở lại khung chat ở trang khác: hiện lại cuộc trò chuyện đang có */

    async function loadHistory() {

        const token = getSessionToken();

        if (historyLoaded || !token) {
            return;
        }

        historyLoaded = true;

        try {

            const history = await apiRequest("/chat/" + encodeURIComponent(token) + "/messages", { auth: isLoggedIn() });

            history.forEach(function (message) {
                addMessage(message.content, message.sender === "USER" ? "user" : "bot");
            });

        } catch (error) {
            setSessionToken(null);
        }

    }


    async function sendUserMessage(text) {

        if (waiting) {
            return;
        }

        historyLoaded = true;

        addMessage(text, "user");

        waiting = true;

        form.querySelector("button").disabled = true;

        /* Bong bóng trả lời: chữ được thêm dần theo luồng; dòng trạng thái khi trợ lý đang tra cứu */
        const bubble = addMessage("Trợ lý đang trả lời...", "bot", { typing: true });
        const paragraph = bubble.querySelector("p");
        const status = document.createElement("p");
        status.className = "chatbot-status";
        let streamed = "";

        try {

            const answer = await streamAssistant(text, true, function (event) {

                if (event.type === "delta") {
                    if (!streamed) {
                        bubble.classList.remove("chatbot-typing");
                    }
                    streamed += event.text;
                    paragraph.textContent = streamed.replace(/\*\*(.+?)\*\*/g, "$1");
                    status.remove();
                } else if (event.type === "reset") {
                    /* trợ lý bỏ đoạn vừa viết (vd. "để mình tra cứu...") và trả lời lại */
                    streamed = "";
                    paragraph.textContent = "";
                } else if (event.type === "status") {
                    status.textContent = event.text;
                    bubble.appendChild(status);
                }

                messages.scrollTop = messages.scrollHeight;

            });

            bubble.remove();

            addMessage(answer.reply, "bot", { products: answer.products, actions: answer.actions });

        } catch (error) {

            bubble.remove();

            if (error.status === 429) {
                addMessage(getErrorMessage(error), "bot");
            } else {
                await keywordReply(text);
            }

        } finally {

            waiting = false;

            form.querySelector("button").disabled = false;

        }

    }


    /*
     * POST /chat/stream, đọc Server-Sent Events: session → delta / status ... → done (hoặc error).
     * Lỗi trước khi luồng bắt đầu (429, 404 phiên cũ, 503...) là JSON ApiResult như mọi API khác.
     * Phiên cũ không còn (vd. đã đăng xuất) → bắt đầu phiên mới một lần.
     */

    async function streamAssistant(text, retry, onEvent) {

        const send = function () {

            const headers = { "Content-Type": "application/json", "Accept": "text/event-stream" };
            const auth = getAuth();

            if (isLoggedIn() && auth && auth.accessToken) {
                headers["Authorization"] = "Bearer " + auth.accessToken;
            }

            return fetch(API_BASE_URL + "/chat/stream", {
                method: "POST",
                headers: headers,
                body: JSON.stringify({ sessionToken: getSessionToken(), message: text })
            });

        };

        let response;

        try {

            response = await send();

            if (response.status === 401 && isLoggedIn() && await refreshTokens()) {
                response = await send();
            }

        } catch (error) {
            throw new ApiError(0, "NETWORK_ERROR", API_ERROR_MESSAGES.NETWORK_ERROR, null);
        }


        if (!response.ok) {

            try {
                await readApiResult(response);
            } catch (error) {
                if (retry && error.status === 404 && getSessionToken()) {
                    setSessionToken(null);
                    return streamAssistant(text, false, onEvent);
                }
                throw error;
            }

        }


        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        while (true) {

            const chunk = await reader.read();

            if (chunk.done) {
                break;
            }

            buffer += decoder.decode(chunk.value, { stream: true });

            let boundary = buffer.indexOf("\n\n");

            while (boundary !== -1) {

                const line = buffer.slice(0, boundary).trim();
                buffer = buffer.slice(boundary + 2);
                boundary = buffer.indexOf("\n\n");

                if (!line.startsWith("data:")) {
                    continue;
                }

                const event = JSON.parse(line.slice(5).trim());

                if (event.type === "session") {
                    setSessionToken(event.sessionToken);
                } else if (event.type === "done") {
                    setSessionToken(event.sessionToken);
                    return event;
                } else if (event.type === "error") {
                    throw new ApiError(503, "AI_SERVICE_UNAVAILABLE", event.message, null);
                } else {
                    onEvent(event);
                }

            }

        }

        throw new ApiError(503, "AI_SERVICE_UNAVAILABLE", "Trợ lý AI ngắt kết nối", null);

    }


    /*
     * Nút "Xác nhận" cho thao tác trợ lý đề xuất. Trợ lý không tự thực hiện: khi khách bấm, trang gọi đúng API
     * thường của khách (thêm giỏ / huỷ đơn) với quyền của chính khách.
     */

    function renderActions(bubble, actions) {

        const row = document.createElement("div");

        row.className = "chatbot-actions";

        actions.forEach(function (action) {

            const button = document.createElement("button");

            button.type = "button";
            button.className = "chatbot-action";
            button.textContent = "✓ Xác nhận: " + action.label;

            button.addEventListener("click", async function () {

                button.disabled = true;

                try {

                    if (action.type === "ADD_TO_CART") {

                        await addCartItem({ variantId: action.variantId }, action.quantity);

                        if (typeof updateCartCount === "function") {
                            updateCartCount();
                        }

                        addMessage("Đã thêm vào giỏ hàng.", "bot", { links: [["Xem giỏ hàng", "customer/cart.html"]] });

                    } else if (action.type === "CANCEL_ORDER") {

                        await apiRequest("/orders/" + encodeURIComponent(action.orderId) + "/cancel",
                            { method: "POST", auth: true });

                        addMessage("Đã huỷ đơn hàng.", "bot",
                            { links: [["Xem đơn hàng", "customer/order-detail.html?id=" + action.orderId]] });

                    }

                    button.textContent = "✓ Đã thực hiện: " + action.label;

                } catch (error) {

                    button.disabled = false;

                    addMessage(getErrorMessage(error), "bot");

                }

            });

            row.appendChild(button);

        });

        bubble.appendChild(row);

    }


    /* ===== Dự phòng khi trợ lý AI không phản hồi: trả lời theo từ khoá ===== */

    async function keywordReply(text) {

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
                    "Trợ lý AI đang tạm ngưng nên mình chỉ tìm được theo từ khoá, và không thấy sản phẩm khớp \"" +
                    keyword + "\". Bạn thử từ khoá khác (ví dụ \"iPhone 16\", \"laptop Dell\"), " +
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

        /* Trợ lý AI có thể dùng **in đậm** kiểu markdown: bỏ dấu, giữ xuống dòng (CSS white-space: pre-line) */
        paragraph.textContent = sender === "bot" ? text.replace(/\*\*(.+?)\*\*/g, "$1") : text;

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


        if (options.actions && options.actions.length) {
            renderActions(bubble, options.actions);
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
