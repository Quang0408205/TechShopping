/* ================= ĐÁNH GIÁ SẢN PHẨM (admin/reviews.html) — R5 ================= */

/*
 * API thật (chỉ ADMIN, backend kiểm tra lại quyền ở mọi request):
 *   GET   /admin/reviews?keyword=&rating=&hidden=&productId=&page=&size=   mọi đánh giá (kể cả đã ẩn), mới nhất trước
 *   PATCH /admin/reviews/{id}/visibility { hidden, reason }               ẩn (bắt buộc lý do) / hiện lại
 * Ẩn / hiện lại đều tính lại điểm sản phẩm ở backend. Lý do ẩn được hiện cho người viết.
 * Lọc theo một sản phẩm: nút "Lọc SP này" trong bảng hoặc link admin/reviews.html?productId=.
 * Mọi chữ qua escapeHtml, mọi ảnh qua isSafeImageUrl.
 */

const REVIEW_PAGE_SIZE = 20;

/* Khớp ReviewVisibilityRequest.reason (≤ 500 ký tự) */
const REVIEW_HIDE_REASON_MAX_LENGTH = 500;

const REVIEW_COMMENT_PREVIEW_LENGTH = 120;

/* Lý do chọn nhanh; "Khác" bắt buộc tự ghi */
const REVIEW_HIDE_REASONS = [
    "Ngôn từ không phù hợp",
    "Spam, quảng cáo",
    "Không liên quan đến sản phẩm",
    "Chứa thông tin cá nhân",
    "Khác"
];

const REVIEW_HIDE_REASON_OTHER = "Khác";


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const form = document.getElementById("reviewFilterForm");

    const tbody = document.getElementById("reviewTableBody");

    const pagination = document.getElementById("reviewPagination");

    const productChip = document.getElementById("reviewProductChip");


    let currentPage = 0;

    let currentReviews = [];

    let requestCounter = 0;

    const expandedIds = new Set();

    /* { id, name } khi đang lọc theo một sản phẩm; name null = chưa biết tên (link ?productId=) */
    let productFilter = null;


    const initialProductId = Number(new URLSearchParams(window.location.search).get("productId"));

    if (Number.isInteger(initialProductId) && initialProductId > 0) {
        productFilter = { id: initialProductId, name: null };
    }


    form.addEventListener("submit", function (event) {

        event.preventDefault();

        reload();

    });

    document.getElementById("reviewRatingFilter").addEventListener("change", reload);

    document.getElementById("reviewHiddenFilter").addEventListener("change", reload);

    document.getElementById("reviewProductChipClear").addEventListener("click", function () {
        setProductFilter(null);
    });


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const review = currentReviews.find(function (r) {
            return String(r.id) === button.dataset.id;
        });

        if (!review) {
            return;
        }


        if (button.dataset.action === "toggle") {
            toggleDetail(review.id);
        } else if (button.dataset.action === "product") {
            setProductFilter({ id: review.productId, name: review.productName });
        } else if (button.dataset.action === "hide") {
            openHideModal(review, button);
        } else if (button.dataset.action === "show") {
            confirmShow(review);
        }

    });


    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        loadReviews();

    });


    renderProductChip();

    loadReviews();


    /* ================= BỘ LỌC ================= */

    function reload() {

        currentPage = 0;

        expandedIds.clear();

        loadReviews();

    }


    function setProductFilter(filter) {

        productFilter = filter;

        const url = new URL(window.location.href);

        if (filter) {
            url.searchParams.set("productId", String(filter.id));
        } else {
            url.searchParams.delete("productId");
        }

        history.replaceState(null, "", url);

        renderProductChip();

        reload();

    }


    function renderProductChip() {

        productChip.hidden = !productFilter;

        if (productFilter) {
            document.getElementById("reviewProductChipText").textContent =
                "Sản phẩm: " + (productFilter.name || "#" + productFilter.id);
        }

    }


    /* ================= TẢI DANH SÁCH ================= */

    async function loadReviews() {

        const requestId = ++requestCounter;

        const params = new URLSearchParams({
            page: String(currentPage),
            size: String(REVIEW_PAGE_SIZE)
        });

        const keyword = document.getElementById("reviewKeyword").value.trim();

        const rating = document.getElementById("reviewRatingFilter").value;

        const hidden = document.getElementById("reviewHiddenFilter").value;

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (rating) {
            params.set("rating", rating);
        }

        if (hidden) {
            params.set("hidden", hidden);
        }

        if (productFilter) {
            params.set("productId", String(productFilter.id));
        }


        tbody.innerHTML = adminEmptyRow(7, "Đang tải…");


        try {

            const page = await apiRequest("/admin/reviews?" + params.toString(), { auth: true });

            if (requestId !== requestCounter) {
                return;
            }

            currentReviews = page.content || [];

            /* Link ?productId= chỉ có mã: lấy tên từ dòng đầu tiên */
            if (productFilter && !productFilter.name && currentReviews.length > 0) {
                productFilter.name = currentReviews[0].productName;
                renderProductChip();
            }

            renderTable(page);

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(7, message);
                });
            }

        }

    }


    /* ================= HIỂN THỊ ================= */

    function renderTable(page) {

        document.getElementById("reviewRowCount").textContent = page.totalElements + " đánh giá";

        tbody.innerHTML = currentReviews.map(renderReviewRows).join("") ||
            adminEmptyRow(7, "Không có đánh giá nào phù hợp bộ lọc");

        renderPagination(page);

    }


    function renderReviewRows(review) {

        const id = escapeHtml(String(review.id));

        const expanded = expandedIds.has(review.id);

        const photoCount = (review.imageUrls || []).filter(isSafeImageUrl).length;

        const filteringThisProduct = productFilter && productFilter.id === review.productId;


        return `
            <tr data-review-id="${id}" class="${review.hidden ? "admin-review-row--hidden" : ""}">
                <td>
                    <button
                        type="button"
                        class="admin-row-toggle"
                        data-action="toggle"
                        data-id="${id}"
                        aria-label="Xem đầy đủ đánh giá #${id}"
                        aria-expanded="${expanded}"
                    >${expanded ? "▾" : "▸"}</button>
                </td>
                <td class="admin-review-cell">
                    ${starsHtml(review.rating)}
                    <span class="admin-review-excerpt">${escapeHtml(truncateText(review.comment, REVIEW_COMMENT_PREVIEW_LENGTH))}</span>
                    <span class="admin-subtext">
                        ${photoCount > 0 ? escapeHtml(photoCount + " ảnh") : "Không có ảnh"}${review.edited ? " · (đã chỉnh sửa)" : ""}
                    </span>
                </td>
                <td>
                    ${escapeHtml(review.productName)}
                    <span class="admin-subtext">#${escapeHtml(String(review.productId))}</span>
                    ${filteringThisProduct ? "" : `<button type="button" class="admin-link-btn" data-action="product" data-id="${id}">Lọc SP này</button>`}
                </td>
                <td>
                    ${escapeHtml(review.authorName)}
                    ${review.verifiedPurchase ? ' <span class="admin-badge admin-badge-success">✓ Đã mua hàng</span>' : ""}
                    <span class="admin-subtext">@${escapeHtml(review.authorUsername || "")}</span>
                    <span class="admin-subtext">${escapeHtml(review.authorEmail || "")}</span>
                </td>
                <td>${escapeHtml(formatDateTimeVi(review.createdAt))}</td>
                <td>
                    <span class="admin-badge ${review.hidden ? "admin-badge-danger" : "admin-badge-success"}">
                        ${review.hidden ? "Đã ẩn" : "Đang hiện"}
                    </span>
                </td>
                <td class="admin-actions-cell">
                    ${review.hidden
                        ? `<button type="button" class="admin-link-btn" data-action="show" data-id="${id}">Hiện lại</button>`
                        : `<button type="button" class="admin-link-btn admin-link-danger" data-action="hide" data-id="${id}">Ẩn</button>`}
                </td>
            </tr>
            <tr class="js-review-detail" data-review-id="${id}" ${expanded ? "" : "hidden"}>
                <td></td>
                <td colspan="6">${reviewDetailHtml(review)}</td>
            </tr>
        `;

    }


    function reviewDetailHtml(review) {

        const images = (review.imageUrls || []).filter(isSafeImageUrl);

        const facts = [
            ["Số sao", escapeHtml(review.rating + " / 5")],
            ["Viết lúc", escapeHtml(formatDateTimeVi(review.createdAt))]
        ];

        if (review.edited && review.updatedAt) {
            facts.push(["Sửa lần cuối", escapeHtml(formatDateTimeVi(review.updatedAt))]);
        }

        facts.push([
            "Sản phẩm",
            `<a href="${escapeHtml(getProductDetailUrl(review.productId))}" target="_blank" rel="noopener">Xem trang sản phẩm ↗</a>`
        ]);

        if (review.hidden) {
            facts.push(["Lý do ẩn", escapeHtml(review.hiddenReason || "—")]);
            facts.push(["Ẩn bởi", escapeHtml(review.hiddenByName || "—")]);
            facts.push(["Ẩn lúc", review.hiddenAt ? escapeHtml(formatDateTimeVi(review.hiddenAt)) : "—"]);
        }


        const photosHtml = images.length === 0 ? "" : `
            <div class="admin-review-photos">
                ${images.map(function (url, index) {
                    return `<a href="${escapeHtml(url.trim())}" target="_blank" rel="noopener" aria-label="Mở ảnh ${index + 1} trong tab mới"><img src="${escapeHtml(url.trim())}" alt="" loading="lazy"></a>`;
                }).join("")}
            </div>
        `;


        return `
            <div class="admin-order-detail">
                <p class="admin-review-comment">${escapeHtml(review.comment)}</p>
                ${photosHtml}
                <dl class="admin-order-facts">
                    ${facts.map(function (fact) {
                        return `<div><dt>${fact[0]}</dt><dd>${fact[1]}</dd></div>`;
                    }).join("")}
                </dl>
            </div>
        `;

    }


    function starsHtml(value) {

        const rating = Math.max(0, Math.min(5, Number(value) || 0));

        return `<span class="admin-review-stars" role="img" aria-label="${rating} trên 5 sao">`
            + "★".repeat(rating) + `<span class="admin-review-stars-off">${"★".repeat(5 - rating)}</span></span>`;

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


    function toggleDetail(reviewId) {

        const detailRow = tbody.querySelector('.js-review-detail[data-review-id="' + reviewId + '"]');

        const button = tbody.querySelector('button[data-action="toggle"][data-id="' + reviewId + '"]');

        if (!detailRow || !button) {
            return;
        }

        detailRow.hidden = !detailRow.hidden;

        if (detailRow.hidden) {
            expandedIds.delete(reviewId);
        } else {
            expandedIds.add(reviewId);
        }

        button.textContent = detailRow.hidden ? "▸" : "▾";

        button.setAttribute("aria-expanded", String(!detailRow.hidden));

    }


    /* ================= ẨN / HIỆN LẠI ================= */

    /*
     * Hộp thoại riêng (openConfirmModal chỉ có một ô và luôn đóng khi xác nhận): chọn lý do + ghi thêm,
     * "Khác" bắt buộc ghi; lỗi (kể cả từ server) hiện ngay trong hộp, hộp chỉ đóng khi ẩn xong.
     * Lý do gửi đi: "Lý do chọn: ghi thêm", hoặc chỉ phần ghi thêm khi chọn "Khác".
     */

    function openHideModal(review, opener) {

        closeModal();

        const overlay = document.createElement("div");

        overlay.className = "modal-overlay";

        overlay.innerHTML = `
            <div class="modal-box admin-review-hide-box" role="dialog" aria-modal="true" aria-labelledby="reviewHideTitle">
                <h3 id="reviewHideTitle">Ẩn đánh giá?</h3>
                <p>
                    Đánh giá ${escapeHtml(review.rating + "★")} của ${escapeHtml(review.authorName)} về
                    "${escapeHtml(truncateText(review.productName, 60))}" sẽ không còn hiện trên trang sản phẩm và không
                    tính vào điểm. Người viết sẽ thấy lý do bên dưới.
                </p>
                <label class="modal-field">
                    <span>Lý do *</span>
                    <select id="reviewHidePreset">
                        ${REVIEW_HIDE_REASONS.map(function (reason) {
                            return `<option value="${escapeHtml(reason)}">${escapeHtml(reason)}</option>`;
                        }).join("")}
                    </select>
                </label>
                <label class="modal-field">
                    <span id="reviewHideNoteLabel">Ghi thêm (không bắt buộc)</span>
                    <input type="text" id="reviewHideNote" placeholder="VD: có số điện thoại trong nội dung">
                </label>
                <p class="form-error" id="reviewHideError" role="alert" hidden></p>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline-dark" data-action="cancel">Quay lại</button>
                    <button type="button" class="btn btn-dark" data-action="confirm">ẨN ĐÁNH GIÁ</button>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        modalOverlayElement = overlay;


        const preset = overlay.querySelector("#reviewHidePreset");

        const note = overlay.querySelector("#reviewHideNote");

        const noteLabel = overlay.querySelector("#reviewHideNoteLabel");

        const errorBox = overlay.querySelector("#reviewHideError");

        const confirmButton = overlay.querySelector('[data-action="confirm"]');

        let saving = false;


        function syncPreset() {

            const isOther = preset.value === REVIEW_HIDE_REASON_OTHER;

            noteLabel.textContent = isOther ? "Lý do cụ thể *" : "Ghi thêm (không bắt buộc)";

            /* Cả chuỗi gửi đi không quá 500 ký tự */
            note.maxLength = isOther
                ? REVIEW_HIDE_REASON_MAX_LENGTH
                : REVIEW_HIDE_REASON_MAX_LENGTH - preset.value.length - 2;

        }


        function showError(message) {

            errorBox.textContent = message;

            errorBox.hidden = false;

        }


        function close() {

            if (modalOverlayElement === overlay) {
                closeModal();
            }

            if (opener && document.body.contains(opener)) {
                opener.focus();
            }

        }


        async function submit() {

            if (saving) {
                return;
            }

            const extra = note.value.trim();

            const isOther = preset.value === REVIEW_HIDE_REASON_OTHER;

            if (isOther && !extra) {

                showError("Vui lòng ghi lý do cụ thể khi chọn \"Khác\".");

                note.focus();

                return;

            }

            const reason = isOther ? extra : preset.value + (extra ? ": " + extra : "");


            saving = true;

            confirmButton.disabled = true;

            errorBox.hidden = true;


            try {

                await apiRequest("/admin/reviews/" + review.id + "/visibility", {
                    method: "PATCH",
                    body: { hidden: true, reason: reason },
                    auth: true
                });

                close();

                showToast("Đã ẩn đánh giá của " + review.authorName + ".", "success");

                loadReviews();

            } catch (error) {

                handleError(error, function (message) {
                    const details = error.details || {};
                    showError(details.reason || message);
                });

                if (error.status === 404) {
                    loadReviews();
                }

            } finally {

                saving = false;

                confirmButton.disabled = false;

            }

        }


        syncPreset();

        preset.addEventListener("change", function () {
            syncPreset();
            errorBox.hidden = true;
        });

        note.addEventListener("keydown", function (event) {

            if (event.key === "Enter") {
                event.preventDefault();
                submit();
            }

        });

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

        overlay.querySelector('[data-action="cancel"]').addEventListener("click", close);

        confirmButton.addEventListener("click", submit);

        preset.focus();

        requestAnimationFrame(function () {
            overlay.classList.add("show");
        });

    }


    function confirmShow(review) {

        openConfirmModal({
            title: "Hiện lại đánh giá?",
            message: "Đánh giá của " + review.authorName + " sẽ hiện lại trên trang sản phẩm và được tính vào điểm. Lý do ẩn cũ sẽ bị xoá.",
            confirmLabel: "HIỆN LẠI",
            cancelLabel: "Quay lại",
            onConfirm: async function () {

                try {

                    await apiRequest("/admin/reviews/" + review.id + "/visibility", {
                        method: "PATCH",
                        body: { hidden: false },
                        auth: true
                    });

                    showToast("Đã hiện lại đánh giá của " + review.authorName + ".", "success");

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

                loadReviews();

            }
        });

    }


    /*
     * Hết phiên (401, kể cả sau khi đã thử làm mới token) → kiểm tra lại phiên
     * và về trang đăng nhập; lỗi khác (403, 404…) → hiển thị bằng show(message).
     */

    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
