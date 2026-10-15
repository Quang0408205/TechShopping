/* ================= YÊU CẦU DỊCH VỤ CỦA TÔI (customer/service-requests.html) — Phase 6.5 ================= */

/*
 * GET  /service-requests/mine?type=&status=&page=&size=   bảo hành / bảo trì / trả hàng, mới nhất trước
 * GET  /service-requests/{TYPE}/{id}                     một yêu cầu (link ?type=&id= từ trang đơn hàng)
 * POST /service-requests/{TYPE}/{id}/cancel              huỷ khi còn chờ
 * Chưa đăng nhập: account-sidebar.js đã chuyển tới trang đăng nhập. Mọi chữ qua escapeHtml, ảnh qua isSafeImageUrl.
 */

const SERVICE_PAGE_SIZE = 10;


document.addEventListener(
    "DOMContentLoaded",
    async function () {

        if (typeof layoutReady !== "undefined") {
            await layoutReady;
        }

        if (!isLoggedIn()) {
            return;
        }


        const container = document.getElementById("serviceRequestList");
        const typeFilter = document.getElementById("serviceTypeFilter");
        const statusFilter = document.getElementById("serviceStatusFilter");
        const params = new URLSearchParams(window.location.search);
        const focusType = params.get("type");
        const focusId = params.get("id");

        let currentPage = 0;
        let requestCounter = 0;
        let shown = [];


        [typeFilter, statusFilter].forEach(function (select) {
            select.addEventListener("change", function () {
                currentPage = 0;
                loadRequests();
            });
        });

        container.addEventListener("click", function (event) {

            const pageButton = event.target.closest("button[data-page]");

            if (pageButton && !pageButton.disabled) {
                currentPage = Number(pageButton.dataset.page);
                loadRequests();
                container.scrollIntoView({ behavior: "smooth", block: "start" });
                return;
            }

            if (event.target.closest("#serviceRetryBtn")) {
                loadRequests();
                return;
            }

            const cancelButton = event.target.closest('button[data-action="cancel"]');

            if (cancelButton) {
                confirmCancel(shown.find(function (r) { return keyOf(r) === cancelButton.dataset.key; }), cancelButton);
            }

        });


        loadRequests();


        async function loadRequests() {

            const requestId = ++requestCounter;

            const query = new URLSearchParams({ page: String(currentPage), size: String(SERVICE_PAGE_SIZE) });

            if (typeFilter.value) {
                query.set("type", typeFilter.value);
            }

            if (statusFilter.value) {
                query.set("status", statusFilter.value);
            }

            container.innerHTML = '<div class="state-box"><p>Đang tải yêu cầu…</p></div>';

            let page;

            let focused = null;

            try {

                page = await apiRequest("/service-requests/mine?" + query.toString(), { auth: true });

                // link từ trang đơn hàng: yêu cầu đó luôn hiện, kể cả khi không nằm ở trang đầu
                if (focusType && focusId && !(page.content || []).some(function (r) { return r.type === focusType && String(r.id) === focusId; })
                    && currentPage === 0 && !typeFilter.value && !statusFilter.value) {
                    focused = await apiRequest("/service-requests/" + encodeURIComponent(focusType) + "/" + encodeURIComponent(focusId),
                        { auth: true }).catch(function () { return null; });
                }

            } catch (error) {

                if (requestId !== requestCounter) {
                    return;
                }

                if (!isLoggedIn()) {
                    redirectToLogin();
                    return;
                }

                container.innerHTML = errorStateHtml(getErrorMessage(error), "serviceRetryBtn");

                return;

            }

            if (requestId !== requestCounter) {
                return;
            }

            shown = (focused ? [focused] : []).concat(page.content || []);

            render(page);

        }


        function render(page) {

            if (shown.length === 0) {

                const filtered = typeFilter.value || statusFilter.value;

                container.innerHTML = `
                    <div class="state-box empty-state">
                        <h3>${filtered ? "Không có yêu cầu nào phù hợp" : "Bạn chưa gửi yêu cầu nào"}</h3>
                        <p>Gửi bảo hành, bảo trì hoặc trả hàng từ trang chi tiết đơn hàng đã giao.</p>
                        <a href="${escapeHtml(siteUrl("customer/orders.html"))}" class="btn btn-dark">ĐƠN HÀNG CỦA TÔI</a>
                    </div>
                `;

                return;

            }

            container.innerHTML = shown.map(cardHtml).join("") + paginationHtml(page);

            if (focusType && focusId) {
                const card = document.getElementById("request-" + focusType + "-" + focusId);
                if (card) {
                    card.classList.add("is-focused");
                    card.scrollIntoView({ block: "center" });
                }
            }

        }


        function keyOf(request) {
            return request.type + "-" + request.id;
        }


        function cardHtml(request) {

            const facts = [["Đơn hàng", `<a href="${escapeHtml(siteUrl("customer/order-detail.html?id=" + encodeURIComponent(request.orderId)))}">${escapeHtml(request.orderCode)}</a>`],
                ["Gửi lúc", escapeHtml(formatOrderDateTime(request.createdAt))]];

            if (request.storeName) {
                facts.push(["Chi nhánh xử lý", escapeHtml(request.storeName)]);
            }

            if (request.maintenanceType) {
                facts.push(["Loại bảo trì", escapeHtml(AFTER_SALES_MAINTENANCE_TYPE_LABELS[request.maintenanceType] || request.maintenanceType)]);
            }

            if (request.reasonType) {
                facts.push(["Lý do", escapeHtml(AFTER_SALES_RETURN_REASON_LABELS[request.reasonType] || request.reasonType)]);
            }

            if (request.warrantyEndDate) {
                facts.push(["Bảo hành đến", escapeHtml(afterSalesDate(request.warrantyEndDate))]);
            }

            if (request.estimatedCompletionDate) {
                facts.push(["Dự kiến xong", escapeHtml(afterSalesDate(request.estimatedCompletionDate))]);
            }

            if (request.estimatedCost != null) {
                facts.push(["Chi phí dự kiến", escapeHtml(formatPrice(Number(request.estimatedCost)))]);
            }

            if (request.actualCost != null) {
                facts.push(["Chi phí thực tế", escapeHtml(formatPrice(Number(request.actualCost)))]);
            }

            if (request.refundAmount != null) {
                facts.push([request.status === "REFUNDED" ? "Đã hoàn" : "Số tiền hoàn", escapeHtml(formatPrice(Number(request.refundAmount)))]);
            }

            if (request.handlerName) {
                facts.push(["Nhân viên phụ trách", escapeHtml(request.handlerName)]);
            }

            [
                ["Duyệt lúc", request.approvedAt],
                [request.type === "RETURN" ? "Cửa hàng nhận hàng" : "Cửa hàng nhận máy", request.receivedAt],
                [request.type === "RETURN" ? "Hoàn tiền lúc" : "Hoàn tất", request.type === "MAINTENANCE" ? null : request.completedAt],
                ["Đã huỷ lúc", request.cancelledAt]
            ].forEach(function (moment) {
                if (moment[1]) {
                    facts.push([moment[0], escapeHtml(formatOrderDateTime(moment[1]))]);
                }
            });

            if (request.type === "MAINTENANCE" && request.completedAt) {
                facts.push(["Hoàn tất", escapeHtml(afterSalesDate(request.completedAt.slice(0, 10)))]);
            }

            const photos = (request.imageUrls || []).filter(isSafeImageUrl);

            const items = request.type === "RETURN" ? `
                <ul class="service-card-items">
                    ${(request.items || []).map(function (item) {
                        return `<li>${escapeHtml(item.productName)}${item.variantName ? " · " + escapeHtml(item.variantName) : ""} × ${escapeHtml(String(item.quantity))} — ${escapeHtml(formatPrice(Number(item.refundAmount)))}</li>`;
                    }).join("")}
                </ul>
            ` : "";

            const key = escapeHtml(keyOf(request));

            return `
                <article class="service-card" id="request-${key}" data-key="${key}">
                    <div class="service-card-head">
                        <div>
                            <strong>${escapeHtml(request.code)}</strong>
                            <span class="service-card-type">${escapeHtml(AFTER_SALES_TYPE_LABELS[request.type] || request.type)}</span>
                        </div>
                        <span class="service-badge service-badge--${afterSalesStatusTone(request.status)}">${escapeHtml(afterSalesStatusLabel(request.type, request.status))}</span>
                    </div>
                    <p class="service-card-product">${escapeHtml(afterSalesItemsSummary(request))}</p>
                    ${items}
                    <p class="service-card-text">${escapeHtml(request.description)}</p>
                    ${photos.length ? `<div class="service-card-photos">${photos.map(function (url, index) {
                        return `<a href="${escapeHtml(url.trim())}" target="_blank" rel="noopener" aria-label="Mở ảnh ${index + 1}"><img src="${escapeHtml(url.trim())}" alt="" loading="lazy"></a>`;
                    }).join("")}</div>` : ""}
                    ${request.notes ? `<p class="service-card-note is-info">Cửa hàng ghi chú: ${escapeHtml(request.notes)}</p>` : ""}
                    ${request.rejectionReason ? `<p class="service-card-note">Lý do từ chối: ${escapeHtml(request.rejectionReason)}</p>` : ""}
                    <dl class="service-card-facts">
                        ${facts.map(function (fact) {
                            return `<div><dt>${fact[0]}</dt><dd>${fact[1]}</dd></div>`;
                        }).join("")}
                    </dl>
                    ${request.cancellable ? `
                        <div class="service-card-actions">
                            <button type="button" class="btn btn-outline-dark" data-action="cancel" data-key="${key}">HUỶ YÊU CẦU</button>
                        </div>
                    ` : ""}
                </article>
            `;

        }


        function paginationHtml(page) {

            if (page.totalPages <= 1) {
                return "";
            }

            return `
                <div class="orders-pagination">
                    <button type="button" class="btn btn-outline-dark" data-page="${page.page - 1}" ${page.page <= 0 ? "disabled" : ""}>‹ TRƯỚC</button>
                    <span>Trang ${page.page + 1} / ${page.totalPages}</span>
                    <button type="button" class="btn btn-outline-dark" data-page="${page.page + 1}" ${page.page + 1 >= page.totalPages ? "disabled" : ""}>SAU ›</button>
                </div>
            `;

        }


        function confirmCancel(request, button) {

            if (!request) {
                return;
            }

            openConfirmModal({
                title: "Huỷ yêu cầu " + request.code + "?",
                message: "Cửa hàng sẽ không xử lý yêu cầu này nữa. Bạn có thể gửi yêu cầu mới sau.",
                confirmLabel: "HUỶ YÊU CẦU",
                cancelLabel: "Quay lại",
                onConfirm: async function () {

                    button.disabled = true;

                    try {

                        await apiRequest("/service-requests/" + request.type + "/" + request.id + "/cancel", { method: "POST", auth: true });

                        showToast("Đã huỷ yêu cầu " + request.code + ".", "success");

                    } catch (error) {

                        if (error.status === 401) {
                            redirectToLogin();
                            return;
                        }

                        showToast(getErrorMessage(error), "error");

                    }

                    loadRequests();

                }
            });

        }

    }
);
