/* ================= BẢO HÀNH / BẢO TRÌ / ĐỔI TRẢ (admin/service-requests.html) — Phase 6.4 ================= */

/*
 * API thật (STAFF chỉ chi nhánh mình, ADMIN tất cả; backend kiểm tra lại ở mọi request):
 *   GET   /admin/service-requests?type=&status=&storeId=&keyword=&fromDate=&toDate=&page=&size=
 *   PATCH /admin/service-requests/{TYPE}/{id}  { status?, rejectionReason, notes, estimatedCompletionDate,
 *                                                estimatedCost, actualCost, restockOrderItemIds }
 * Chỉ hiện nút cho bước kế tiếp hợp lệ (giống canMoveTo ở backend); 409 → báo và tải lại.
 */

const REQUEST_PAGE_SIZE = 20;

const REQUEST_NOTE_MAX_LENGTH = 2000;
const REQUEST_REASON_MAX_LENGTH = 1000;


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const form = document.getElementById("requestFilterForm");
    const keywordInput = document.getElementById("requestKeyword");
    const typeFilter = document.getElementById("typeFilter");
    const statusFilter = document.getElementById("statusFilter");
    const storeFilter = document.getElementById("storeFilter");
    const fromDateInput = document.getElementById("requestFromDate");
    const toDateInput = document.getElementById("requestToDate");
    const filterError = document.getElementById("requestFilterError");
    const tbody = document.getElementById("requestTableBody");
    const pagination = document.getElementById("requestPagination");
    const isAdmin = staff.role === "ADMIN";

    let currentPage = 0;
    let currentRequests = [];
    let requestCounter = 0;
    const expandedKeys = new Set();


    /* admin/service-requests.html?keyword=DT000012 */
    keywordInput.value = new URLSearchParams(window.location.search).get("keyword") || "";

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        reloadFromFirstPage();
    });

    [typeFilter, statusFilter, storeFilter, fromDateInput, toDateInput].forEach(function (control) {
        control.addEventListener("change", reloadFromFirstPage);
    });

    setupStoreFilter(storeFilter, staff).catch(function (error) {
        handleError(error, function (message) {
            showToast("Không tải được danh sách chi nhánh: " + message, "error");
        });
    });


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const request = button && findRequest(button.dataset.key);

        if (!request) {
            return;
        }

        const action = button.dataset.action;

        if (action === "toggle") {
            toggleDetail(button.dataset.key);
        } else if (action === "reject") {
            rejectRequest(request, button);
        } else if (action === "edit") {
            editRequest(request, button);
        } else {
            runAction(request, action, button);
        }

    });


    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        loadRequests();

    });


    loadRequests();


    /* ================= TẢI DANH SÁCH ================= */

    function reloadFromFirstPage() {

        currentPage = 0;

        expandedKeys.clear();

        loadRequests();

    }


    async function loadRequests() {

        const requestId = ++requestCounter;

        filterError.hidden = true;

        if (fromDateInput.value && toDateInput.value && fromDateInput.value > toDateInput.value) {

            filterError.textContent = "Ngày bắt đầu phải trước ngày kết thúc.";

            filterError.hidden = false;

            return;

        }


        const params = new URLSearchParams({ page: String(currentPage), size: String(REQUEST_PAGE_SIZE) });

        [
            ["keyword", keywordInput.value.trim()],
            ["type", typeFilter.value],
            ["status", statusFilter.value],
            ["storeId", isAdmin ? storeFilter.value : ""],
            ["fromDate", fromDateInput.value],
            ["toDate", toDateInput.value]
        ].forEach(function (pair) {
            if (pair[1]) {
                params.set(pair[0], pair[1]);
            }
        });


        tbody.innerHTML = adminEmptyRow(9, "Đang tải…");

        try {

            const page = await apiRequest("/admin/service-requests?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            currentRequests = page.content || [];

            renderTable(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    document.getElementById("requestRowCount").textContent = "";
                    tbody.innerHTML = adminEmptyRow(9, message);
                    pagination.innerHTML = "";
                });
            }

        }

    }


    /* ================= HIỂN THỊ ================= */

    function keyOf(request) {
        return request.type + "-" + request.id;
    }


    function findRequest(key) {
        return currentRequests.find(function (request) {
            return keyOf(request) === key;
        });
    }


    function renderTable(page) {

        document.getElementById("requestRowCount").textContent = page.totalElements + " yêu cầu";

        tbody.innerHTML = currentRequests.map(renderRows).join("") ||
            adminEmptyRow(9, "Không có yêu cầu nào phù hợp bộ lọc");

        renderPagination(page);

    }


    function renderRows(request) {

        const key = escapeHtml(keyOf(request));

        const expanded = expandedKeys.has(keyOf(request));

        const tone = afterSalesStatusTone(request.status);


        return `
            <tr data-request-key="${key}">
                <td>
                    <button
                        type="button"
                        class="admin-row-toggle"
                        data-action="toggle"
                        data-key="${key}"
                        aria-label="Xem chi tiết yêu cầu ${escapeHtml(request.code)}"
                        aria-expanded="${expanded}"
                    >${expanded ? "▾" : "▸"}</button>
                </td>
                <td>
                    <strong>${escapeHtml(request.code)}</strong>
                    <span class="admin-subtext">${escapeHtml(AFTER_SALES_TYPE_LABELS[request.type] || request.type)}</span>
                </td>
                <td>
                    ${escapeHtml(afterSalesItemsSummary(request))}
                    <span class="admin-subtext">${escapeHtml(request.orderCode)}</span>
                </td>
                <td>
                    ${escapeHtml(request.customerName)}
                    <span class="admin-subtext">${escapeHtml(request.recipientPhone || "")}</span>
                </td>
                <td>${escapeHtml(request.storeName || "Chưa có chi nhánh")}</td>
                <td>${escapeHtml(request.handlerName || "Chưa có")}</td>
                <td>${escapeHtml(formatDateTimeVi(request.createdAt))}</td>
                <td><span class="admin-badge admin-badge-${tone}">${escapeHtml(afterSalesStatusLabel(request.type, request.status))}</span></td>
                <td class="admin-actions-cell">${actionButtonsHtml(request)}</td>
            </tr>
            <tr class="js-request-detail" data-request-key="${key}" ${expanded ? "" : "hidden"}>
                <td></td>
                <td colspan="8">${detailHtml(request)}</td>
            </tr>
        `;

    }


    /* Nút cho bước kế tiếp hợp lệ của từng loại */

    function actionButtonsHtml(request) {

        const actions = nextActions(request);

        if (actions.length === 0) {
            return '<span class="admin-subtext">—</span>';
        }

        return actions.map(function (action) {
            return `<button type="button" class="admin-link-btn${action.danger ? " admin-link-danger" : ""}" data-action="${action.id}" data-key="${escapeHtml(keyOf(request))}">${escapeHtml(action.label)}</button>`;
        }).join(" ");

    }


    function nextActions(request) {

        const reject = { id: "reject", label: "Từ chối", danger: true };

        if (request.type === "RETURN") {
            return {
                PENDING: [{ id: "approve", label: "Duyệt" }, reject],
                APPROVED: [{ id: "receive-return", label: "Đã nhận hàng" }, reject],
                RECEIVED: [{ id: "refund", label: "Đã hoàn tiền" }]
            }[request.status] || [];
        }

        const edit = { id: "edit", label: "Cập nhật" };

        return {
            PENDING: [{ id: "receive", label: "Tiếp nhận" }, reject],
            RECEIVED: [{ id: "process", label: "Bắt đầu xử lý" }, edit, reject],
            PROCESSING: [{ id: "complete", label: "Hoàn tất" }, edit, reject]
        }[request.status] || [];

    }


    function detailHtml(request) {

        const facts = [
            ["Đơn hàng", `<a href="${escapeHtml(siteUrl("admin/orders.html?keyword=" + encodeURIComponent(request.orderCode)))}">${escapeHtml(request.orderCode)}</a>`],
            ["Khách hàng", escapeHtml(request.customerName) + (request.customerEmail ? `<span class="admin-subtext">${escapeHtml(request.customerEmail)}</span>` : "")],
            ["SĐT người nhận", escapeHtml(request.recipientPhone || "—")]
        ];

        if (request.maintenanceType) {
            facts.push(["Loại bảo trì", escapeHtml(AFTER_SALES_MAINTENANCE_TYPE_LABELS[request.maintenanceType] || request.maintenanceType)]);
        }

        if (request.reasonType) {
            facts.push(["Lý do trả", escapeHtml(AFTER_SALES_RETURN_REASON_LABELS[request.reasonType] || request.reasonType)]);
        }

        if (request.warrantyEndDate) {
            facts.push(["Bảo hành đến", escapeHtml(formatDateVi(request.warrantyEndDate))]);
        }

        if (request.estimatedCompletionDate) {
            facts.push(["Dự kiến xong", escapeHtml(formatDateVi(request.estimatedCompletionDate))]);
        }

        if (request.estimatedCost != null) {
            facts.push(["Chi phí dự kiến", escapeHtml(formatPrice(Number(request.estimatedCost)))]);
        }

        if (request.actualCost != null) {
            facts.push(["Chi phí thực tế", escapeHtml(formatPrice(Number(request.actualCost)))]);
        }

        if (request.refundAmount != null) {
            facts.push(["Số tiền hoàn", escapeHtml(formatPrice(Number(request.refundAmount)))]);
        }

        if (request.notes) {
            facts.push(["Ghi chú xử lý", escapeHtml(request.notes)]);
        }

        if (request.rejectionReason) {
            facts.push(["Lý do từ chối", escapeHtml(request.rejectionReason)]);
        }

        [
            ["Gửi lúc", request.createdAt],
            ["Duyệt lúc", request.approvedAt],
            [request.type === "RETURN" ? "Nhận hàng lúc" : "Nhận máy lúc", request.receivedAt],
            [request.type === "RETURN" ? "Hoàn tiền lúc" : "Hoàn tất", request.completedAt],
            ["Khách huỷ lúc", request.cancelledAt]
        ].forEach(function (moment) {
            if (moment[1]) {
                facts.push([moment[0], escapeHtml(request.type === "MAINTENANCE" && moment[1] === request.completedAt
                    ? formatDateVi(moment[1].slice(0, 10))
                    : formatDateTimeVi(moment[1]))]);
            }
        });


        const photos = (request.imageUrls || []).filter(isSafeImageUrl);

        const photosHtml = photos.length === 0 ? "" : `
            <div class="admin-review-photos">
                ${photos.map(function (url, index) {
                    return `<a href="${escapeHtml(url.trim())}" target="_blank" rel="noopener" aria-label="Mở ảnh ${index + 1} trong tab mới"><img src="${escapeHtml(url.trim())}" alt="" loading="lazy"></a>`;
                }).join("")}
            </div>
        `;


        const itemsHtml = request.type !== "RETURN" ? "" : `
            <table class="admin-table">
                <thead>
                    <tr><th>Sản phẩm</th><th>Số lượng</th><th>Tiền hoàn</th><th>Cộng lại kho</th></tr>
                </thead>
                <tbody>
                    ${(request.items || []).map(function (item) {
                        const restocked = item.restocked == null ? "—" : (item.restocked ? "Có" : "Không");
                        return `
                            <tr>
                                <td>${escapeHtml(item.productName)}<span class="admin-subtext">${escapeHtml(item.variantName || "")}</span></td>
                                <td>${escapeHtml(String(item.quantity))}</td>
                                <td>${escapeHtml(formatPrice(Number(item.refundAmount)))}</td>
                                <td>${escapeHtml(restocked)}</td>
                            </tr>
                        `;
                    }).join("")}
                </tbody>
            </table>
        `;


        return `
            <div class="admin-order-detail">
                <p class="admin-review-comment">${escapeHtml(request.description)}</p>
                ${photosHtml}
                <dl class="admin-order-facts">
                    ${facts.map(function (fact) {
                        return `<div><dt>${fact[0]}</dt><dd>${fact[1]}</dd></div>`;
                    }).join("")}
                </dl>
                ${itemsHtml}
            </div>
        `;

    }


    function renderPagination(page) {

        if (page.totalPages <= 1) {

            pagination.innerHTML = "";

            return;

        }

        pagination.innerHTML = `
            <button type="button" class="btn btn-outline-dark" data-page="${page.page - 1}" ${page.page <= 0 ? "disabled" : ""}>‹ TRƯỚC</button>
            <span>Trang ${page.page + 1} / ${page.totalPages}</span>
            <button type="button" class="btn btn-outline-dark" data-page="${page.page + 1}" ${page.page + 1 >= page.totalPages ? "disabled" : ""}>SAU ›</button>
        `;

    }


    function toggleDetail(key) {

        const detailRow = tbody.querySelector('.js-request-detail[data-request-key="' + key + '"]');

        const button = tbody.querySelector('button[data-action="toggle"][data-key="' + key + '"]');

        if (!detailRow || !button) {
            return;
        }

        detailRow.hidden = !detailRow.hidden;

        if (detailRow.hidden) {
            expandedKeys.delete(key);
        } else {
            expandedKeys.add(key);
        }

        button.textContent = detailRow.hidden ? "▸" : "▾";

        button.setAttribute("aria-expanded", String(!detailRow.hidden));

    }


    /* ================= THAO TÁC ================= */

    function runAction(request, action, opener) {

        const isMaintenance = request.type === "MAINTENANCE";

        if (action === "receive") {

            openActionModal(opener, {
                title: "Tiếp nhận " + request.code,
                message: "Khách sẽ thấy yêu cầu chuyển sang \"Đã nhận máy\" cùng ngày dự kiến và ghi chú bên dưới.",
                confirmLabel: "TIẾP NHẬN",
                fields: [
                    { name: "estimatedCompletionDate", type: "date", label: "Ngày dự kiến xong", value: request.estimatedCompletionDate },
                    isMaintenance ? { name: "estimatedCost", type: "money", label: "Chi phí dự kiến (đ)", value: request.estimatedCost } : null,
                    { name: "notes", type: "textarea", label: "Ghi chú xử lý (khách thấy)", value: request.notes, maxLength: REQUEST_NOTE_MAX_LENGTH }
                ],
                submit: function (values) {
                    return patch(request, Object.assign({ status: "RECEIVED" }, values), "Đã tiếp nhận " + request.code + ".");
                }
            });

        } else if (action === "process") {

            openConfirmModal({
                title: "Bắt đầu xử lý " + request.code + "?",
                message: "Yêu cầu chuyển sang \"Đang xử lý\".",
                confirmLabel: "BẮT ĐẦU",
                cancelLabel: "Quay lại",
                onConfirm: function () {
                    patchFromConfirm(request, { status: "PROCESSING" }, request.code + " đang được xử lý.");
                }
            });

        } else if (action === "complete") {

            openActionModal(opener, {
                title: "Hoàn tất " + request.code,
                message: isMaintenance
                    ? "Nhập chi phí thực tế (khách trả tại cửa hàng). Không thể hoàn tác."
                    : "Bảo hành miễn phí; khách sẽ thấy yêu cầu đã hoàn tất. Không thể hoàn tác.",
                confirmLabel: "HOÀN TẤT",
                fields: [
                    isMaintenance ? { name: "actualCost", type: "money", label: "Chi phí thực tế (đ)", value: request.actualCost != null ? request.actualCost : request.estimatedCost } : null,
                    { name: "notes", type: "textarea", label: "Ghi chú xử lý (khách thấy)", value: request.notes, maxLength: REQUEST_NOTE_MAX_LENGTH }
                ],
                submit: function (values) {
                    return patch(request, Object.assign({ status: "COMPLETED" }, values), "Đã hoàn tất " + request.code + ".");
                }
            });

        } else if (action === "approve") {

            openConfirmModal({
                title: "Duyệt trả hàng " + request.code + "?",
                message: "Khách sẽ được báo mang / gửi hàng về chi nhánh " + (request.storeName || "") + ". Số tiền hoàn dự kiến "
                    + formatPrice(Number(request.refundAmount)) + ".",
                confirmLabel: "DUYỆT",
                cancelLabel: "Quay lại",
                onConfirm: function () {
                    patchFromConfirm(request, { status: "APPROVED" }, "Đã duyệt " + request.code + ".");
                }
            });

        } else if (action === "receive-return") {

            openActionModal(opener, {
                title: "Đã nhận hàng " + request.code,
                message: "Chọn những sản phẩm còn bán được: chúng sẽ được cộng lại vào tồn kho chi nhánh " + (request.storeName || "") + ".",
                confirmLabel: "ĐÃ NHẬN HÀNG",
                fields: [
                    {
                        name: "restockOrderItemIds",
                        type: "checks",
                        label: "Còn bán được — cộng lại kho",
                        options: (request.items || []).map(function (item) {
                            return { value: item.orderItemId, label: item.productName + (item.variantName ? " (" + item.variantName + ")" : "") + " × " + item.quantity };
                        })
                    }
                ],
                submit: function (values) {
                    return patch(request, { status: "RECEIVED", restockOrderItemIds: values.restockOrderItemIds },
                        "Đã nhận hàng " + request.code + (values.restockOrderItemIds.length ? ", đã cộng lại kho." : "."));
                }
            });

        } else if (action === "refund") {

            openConfirmModal({
                title: "Đã hoàn tiền " + request.code + "?",
                message: "Xác nhận đã chuyển " + formatPrice(Number(request.refundAmount)) + " cho khách. Số tiền này được trừ khỏi tổng chi tiêu của khách. Không thể hoàn tác.",
                confirmLabel: "ĐÃ HOÀN TIỀN",
                cancelLabel: "Quay lại",
                onConfirm: function () {
                    patchFromConfirm(request, { status: "REFUNDED" }, "Đã hoàn tiền " + request.code + ".");
                }
            });

        }

    }


    function rejectRequest(request, opener) {

        openActionModal(opener, {
            title: "Từ chối " + request.code,
            message: "Khách sẽ thấy lý do bên dưới. Không thể hoàn tác.",
            confirmLabel: "TỪ CHỐI",
            fields: [
                { name: "rejectionReason", type: "textarea", label: "Lý do từ chối *", required: true, maxLength: REQUEST_REASON_MAX_LENGTH }
            ],
            submit: function (values) {
                return patch(request, { status: "REJECTED", rejectionReason: values.rejectionReason }, "Đã từ chối " + request.code + ".");
            }
        });

    }


    function editRequest(request, opener) {

        openActionModal(opener, {
            title: "Cập nhật " + request.code,
            message: "Khách thấy ngày dự kiến, ghi chú" + (request.type === "MAINTENANCE" ? " và chi phí dự kiến." : "."),
            confirmLabel: "LƯU",
            fields: [
                { name: "estimatedCompletionDate", type: "date", label: "Ngày dự kiến xong", value: request.estimatedCompletionDate },
                request.type === "MAINTENANCE" ? { name: "estimatedCost", type: "money", label: "Chi phí dự kiến (đ)", value: request.estimatedCost } : null,
                { name: "notes", type: "textarea", label: "Ghi chú xử lý (khách thấy; để trống để xoá)", value: request.notes, maxLength: REQUEST_NOTE_MAX_LENGTH, keepEmpty: true }
            ],
            submit: function (values) {
                return patch(request, values, "Đã cập nhật " + request.code + ".");
            }
        });

    }


    /*
     * Gọi PATCH; dùng cho cả hộp thoại riêng (trả về promise để hộp hiện lỗi) lẫn openConfirmModal.
     * 409 (người khác vừa đổi / khách vừa huỷ) → báo và tải lại; lỗi theo trường → ném lại cho hộp thoại.
     */
    async function patch(request, body, successMessage) {

        try {

            await apiRequest("/admin/service-requests/" + request.type + "/" + request.id, {
                method: "PATCH",
                body: body,
                auth: true
            });

            showToast(successMessage, "success");

            loadRequests();

        } catch (error) {

            if (error.status === 409 || error.status === 404) {
                showToast(getErrorMessage(error) + " Danh sách đã được tải lại.", "error");
                loadRequests();
                return;
            }

            if (error.status === 400) {
                throw error;
            }

            handleError(error, function (message) {
                showToast(message, "error");
            });

        }

    }


    function patchFromConfirm(request, body, successMessage) {
        patch(request, body, successMessage).catch(function (error) {
            showToast(getErrorMessage(error), "error");
        });
    }


    /*
     * Hộp thoại nhiều ô (openConfirmModal chỉ có 1 ô): date / money / textarea / checks. Lỗi (kể cả từ
     * server) hiện ngay trong hộp; hộp chỉ đóng khi lưu xong. Ô trống không gửi (trừ keepEmpty).
     */
    function openActionModal(opener, options) {

        closeModal();

        const fields = options.fields.filter(Boolean);

        const overlay = document.createElement("div");

        overlay.className = "modal-overlay";

        overlay.innerHTML = `
            <div class="modal-box admin-action-modal" role="dialog" aria-modal="true" aria-labelledby="actionModalTitle">
                <h3 id="actionModalTitle">${escapeHtml(options.title)}</h3>
                <p>${escapeHtml(options.message || "")}</p>
                ${fields.map(fieldHtml).join("")}
                <p class="form-error" data-role="error" role="alert" hidden></p>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline-dark" data-role="cancel">Quay lại</button>
                    <button type="button" class="btn btn-dark" data-role="confirm">${escapeHtml(options.confirmLabel)}</button>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        modalOverlayElement = overlay;

        const errorBox = overlay.querySelector('[data-role="error"]');

        const confirmButton = overlay.querySelector('[data-role="confirm"]');

        let saving = false;


        function close() {

            if (modalOverlayElement === overlay) {
                closeModal();
            }

            if (opener && document.body.contains(opener)) {
                opener.focus();
            }

        }


        function showError(message) {

            errorBox.textContent = message;

            errorBox.hidden = false;

        }


        function collect() {

            const values = {};

            for (const field of fields) {

                if (field.type === "checks") {
                    values[field.name] = Array.from(overlay.querySelectorAll('input[name="' + field.name + '"]:checked'))
                        .map(function (input) { return Number(input.value); });
                    continue;
                }

                const input = overlay.querySelector('[name="' + field.name + '"]');

                const raw = input.value.trim();

                if (field.required && !raw) {
                    showError("Vui lòng nhập " + field.label.replace(" *", "").toLowerCase() + ".");
                    input.focus();
                    return null;
                }

                if (field.type === "money" && raw && !(Number(raw) >= 0)) {
                    showError(field.label + " phải là số không âm.");
                    input.focus();
                    return null;
                }

                if (raw) {
                    values[field.name] = field.type === "money" ? Number(raw) : raw;
                } else if (field.keepEmpty) {
                    values[field.name] = "";
                }

            }

            return values;

        }


        async function submit() {

            if (saving) {
                return;
            }

            errorBox.hidden = true;

            const values = collect();

            if (!values) {
                return;
            }

            saving = true;

            confirmButton.disabled = true;

            try {

                await options.submit(values);

                close();

            } catch (error) {

                const details = error.details || {};

                const fieldMessages = Object.keys(details).map(function (key) { return details[key]; });

                showError(fieldMessages.length ? fieldMessages.join(" · ") : getErrorMessage(error));

            } finally {

                saving = false;

                confirmButton.disabled = false;

            }

        }


        overlay.addEventListener("keydown", function (event) {

            if (event.key === "Escape") {
                close();
            }

        });

        overlay.addEventListener("click", function (event) {

            if (event.target === overlay) {
                close();
            }

        });

        overlay.querySelector('[data-role="cancel"]').addEventListener("click", close);

        confirmButton.addEventListener("click", submit);

        const first = overlay.querySelector("input, textarea");

        if (first) {
            first.focus();
        }

        requestAnimationFrame(function () {
            overlay.classList.add("show");
        });

    }


    function fieldHtml(field) {

        if (field.type === "checks") {

            const options = field.options.length === 0
                ? '<span class="admin-subtext">Không có sản phẩm</span>'
                : field.options.map(function (option) {
                    return `
                        <label class="admin-check-filter admin-modal-check">
                            <input type="checkbox" name="${escapeHtml(field.name)}" value="${escapeHtml(String(option.value))}" checked>
                            ${escapeHtml(option.label)}
                        </label>
                    `;
                }).join("");

            return `<fieldset class="modal-field admin-modal-checks"><legend>${escapeHtml(field.label)}</legend>${options}</fieldset>`;

        }

        const value = field.value == null ? "" : String(field.value);

        const control = field.type === "textarea"
            ? `<textarea name="${escapeHtml(field.name)}" rows="3" ${field.maxLength ? `maxlength="${field.maxLength}"` : ""}>${escapeHtml(value)}</textarea>`
            : `<input type="${field.type === "money" ? "number" : "date"}" name="${escapeHtml(field.name)}" value="${escapeHtml(value)}" ${field.type === "money" ? 'min="0" step="1000"' : ""}>`;

        return `<label class="modal-field"><span>${escapeHtml(field.label)}</span>${control}</label>`;

    }


    /*
     * Hết phiên (401, kể cả sau khi đã thử làm mới token) → kiểm tra lại phiên
     * và về trang đăng nhập; lỗi khác (403, 409…) → hiển thị bằng show(message).
     */
    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
