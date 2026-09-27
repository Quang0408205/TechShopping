/* ================= LỊCH SỬ CHATBOT (admin/chat-history.html) ================= */

/*
 * Chỉ ADMIN, chỉ xem. Nội dung tin nhắn do khách nhập nên luôn escape.
 * Dữ liệu mẫu (B1), nối chat_sessions / chat_messages thật ở Phase 9.
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        const staff = await adminLayoutReady;

        if (!staff) {
            return;
        }


        const list = document.getElementById("chatSessionList");

        const detailPanel = document.getElementById("chatDetailPanel");

        const sessions = getChatSessions();


        list.innerHTML = sessions.map(function (session) {

            return `
                <button type="button" class="chat-session-item" data-id="${escapeHtml(session.id)}">
                    <div class="chat-session-name">${escapeHtml(session.customerName)}</div>
                    <div class="chat-session-meta">
                        ${escapeHtml(formatDateTimeVi(session.startedAt))} ·
                        ${session.resolved
                            ? '<span class="chat-status-done">Đã kết thúc</span>'
                            : '<span class="chat-status-open">Đang tiếp diễn</span>'}
                    </div>
                </button>
            `;

        }).join("") || '<p class="admin-intro admin-chat-empty">Chưa có phiên chat nào.</p>';


        list.querySelectorAll(".chat-session-item").forEach(function (button) {

            button.addEventListener("click", function () {
                selectSession(button.dataset.id);
            });

        });


        if (sessions.length > 0) {
            selectSession(sessions[0].id);
        }


        function selectSession(id) {

            list.querySelectorAll(".chat-session-item").forEach(function (button) {
                button.classList.toggle("active", button.dataset.id === id);
            });


            const session = getChatSessionById(id);

            if (!session) {

                detailPanel.innerHTML = '<p class="admin-intro">Không tìm thấy phiên chat.</p>';

                return;

            }


            detailPanel.innerHTML = `
                <div class="admin-panel-header">
                    <h2>${escapeHtml(session.customerName)} · ${escapeHtml(formatDateTimeVi(session.startedAt))}</h2>
                    <span class="admin-row-count">${session.messages.length} tin nhắn</span>
                </div>

                <div class="admin-chat-messages">
                    ${session.messages.map(function (message) {
                        return `
                            <div class="admin-chat-message ${message.sender === "user" ? "from-user" : "from-bot"}">
                                ${escapeHtml(message.text)}
                            </div>
                        `;
                    }).join("")}
                </div>
            `;

        }

    }
);
