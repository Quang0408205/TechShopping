/* ================= LỊCH SỬ CHATBOT (admin/chat-history.html) ================= */

/*
 * Chỉ ADMIN, chỉ xem: theo dõi chất lượng câu trả lời của trợ lý AI.
 *   GET /admin/chat-sessions?page=&size=        → danh sách phiên, hoạt động gần nhất trước
 *   GET /admin/chat-sessions/{id}/messages      → tin nhắn của một phiên
 * Nội dung tin nhắn do khách nhập / AI sinh ra nên luôn escape.
 */

const CHAT_SESSION_PAGE_SIZE = 30;


document.addEventListener(
    "DOMContentLoaded",
    async function () {

        const staff = await adminLayoutReady;

        if (!staff) {
            return;
        }


        const list = document.getElementById("chatSessionList");

        const detailPanel = document.getElementById("chatDetailPanel");

        let sessions = [];


        try {

            const page = await apiRequest(
                "/admin/chat-sessions?page=0&size=" + CHAT_SESSION_PAGE_SIZE, { auth: true }
            );

            sessions = page.content;

        } catch (error) {

            list.innerHTML = '<p class="admin-intro admin-chat-empty">' + escapeHtml(getErrorMessage(error)) + "</p>";

            return;

        }


        list.innerHTML = sessions.map(function (session) {

            return `
                <button type="button" class="chat-session-item" data-id="${escapeHtml(session.id)}">
                    <div class="chat-session-name">${escapeHtml(session.customerName)}</div>
                    <div class="chat-session-meta">
                        ${escapeHtml(formatDateTimeVi(session.lastMessageAt || session.createdAt))} ·
                        ${escapeHtml(session.messageCount)} tin nhắn
                    </div>
                    <div class="chat-session-meta">${escapeHtml(session.topic || "")}</div>
                </button>
            `;

        }).join("") || '<p class="admin-intro admin-chat-empty">Chưa có phiên chat nào.</p>';


        list.querySelectorAll(".chat-session-item").forEach(function (button) {

            button.addEventListener("click", function () {
                selectSession(Number(button.dataset.id));
            });

        });


        if (sessions.length > 0) {
            selectSession(sessions[0].id);
        }


        async function selectSession(id) {

            list.querySelectorAll(".chat-session-item").forEach(function (button) {
                button.classList.toggle("active", Number(button.dataset.id) === id);
            });


            const session = sessions.find(function (item) {
                return item.id === id;
            });

            detailPanel.innerHTML = '<p class="admin-intro">Đang tải...</p>';


            let messages;

            try {

                messages = await apiRequest("/admin/chat-sessions/" + id + "/messages", { auth: true });

            } catch (error) {

                detailPanel.innerHTML = '<p class="admin-intro">' + escapeHtml(getErrorMessage(error)) + "</p>";

                return;

            }


            const who = session.customerEmail
                ? session.customerName + " (" + session.customerEmail + ")"
                : session.customerName;

            detailPanel.innerHTML = `
                <div class="admin-panel-header">
                    <h2>${escapeHtml(who)} · ${escapeHtml(formatDateTimeVi(session.createdAt))}</h2>
                    <span class="admin-row-count">${messages.length} tin nhắn</span>
                </div>

                <div class="admin-chat-messages">
                    ${messages.map(function (message) {
                        /* nội dung sát thẻ div: CSS white-space: pre-line giữ cả khoảng trắng thụt lề */
                        return `<div class="admin-chat-message ${message.sender === "USER" ? "from-user" : "from-bot"}">${
                            escapeHtml(message.content)}</div>`;
                    }).join("")}
                </div>
            `;

        }

    }
);
