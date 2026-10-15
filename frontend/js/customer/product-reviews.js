/* ================= ĐÁNH GIÁ SẢN PHẨM (tab "ĐÁNH GIÁ" ở customer/product-detail.html) ================= */

/*
 * API thật:
 *   GET    /products/{id}/reviews?rating=&withImages=&page=&size=   công khai, chỉ đánh giá đang hiện, mới nhất trước
 *   GET    /products/{id}/reviews/summary                           trung bình, tổng, số đánh giá từng mức 5→1, có ảnh
 *   GET    /reviews/mine?productId=                                 đánh giá của tôi (kể cả bị ẩn, kèm lý do)
 *   POST   /reviews {productId, rating, comment, imageUrls}         viết (mỗi tài khoản 1 đánh giá / sản phẩm)
 *   PUT    /reviews/{id}, DELETE /reviews/{id}                      sửa / xoá đánh giá của mình
 *   POST   /uploads/review-images (multipart "file") → { url }      ảnh đánh giá (JPG / PNG / WebP ≤ 5 MB)
 * Chỉ hiển thị số thật (không bịa phân bố sao). Điểm Thế Giới Di Động (product.tgddRating) hiện riêng, ghi rõ
 * nguồn, không tính vào điểm trên POY. Mọi chữ qua escapeHtml, mọi ảnh qua isSafeImageUrl.
 *
 * initProductReviews(product, { onSummaryChange(summary) }) — gọi từ product-detail.js khi đã có sản phẩm.
 */

const REVIEW_PAGE_SIZE = 5;

const REVIEW_COMMENT_MIN = 10;

const REVIEW_COMMENT_MAX = 2000;

const REVIEW_MAX_IMAGES = 5;

const REVIEW_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

const REVIEW_IMAGE_MAX_BYTES = 5 * 1024 * 1024;

const REVIEW_STAR_LABELS = { 1: "Rất tệ", 2: "Tệ", 3: "Bình thường", 4: "Tốt", 5: "Rất tốt" };


function initProductReviews(product, options) {

    const onSummaryChange = (options && options.onSummaryChange) || function () {};

    const mineBox = document.getElementById("reviewMine");

    const filtersBox = document.getElementById("reviewFilters");

    const listBox = document.getElementById("reviewList");

    const moreButton = document.getElementById("reviewMoreBtn");

    const base = "/products/" + product.id + "/reviews";


    let summary = null;

    let filter = { rating: null, withImages: false };

    let items = [];

    let page = -1;

    let totalPages = 0;

    let listCounter = 0;

    let myReview = null;

    let editing = false;

    /* Form đang mở: { rating, images: [url], uploading: số ảnh đang tải, session } */
    let form = null;

    let formSession = 0;


    renderTgddLine();

    filtersBox.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-filter]");

        if (!button) {
            return;
        }

        filter = button.dataset.filter === "images"
            ? { rating: null, withImages: true }
            : { rating: button.dataset.filter === "all" ? null : Number(button.dataset.filter), withImages: false };

        renderFilters();

        loadList(false);

    });

    moreButton.addEventListener("click", function () {
        loadList(true);
    });

    listBox.addEventListener("click", openViewerFromClick);

    mineBox.addEventListener("click", handleMineClick);

    mineBox.addEventListener("input", handleMineInput);

    mineBox.addEventListener("change", handleMineChange);

    mineBox.addEventListener("submit", submitForm);


    refreshAll();


    /* ================= TẢI DỮ LIỆU ================= */

    async function refreshAll() {

        await Promise.all([loadSummary(), loadMine()]);

        loadList(false);

    }


    async function loadSummary() {

        try {

            summary = await apiRequest(base + "/summary");

        } catch (error) {

            summary = null;

        }

        renderSummary();

        renderFilters();

        if (summary) {
            onSummaryChange(summary);
        }

    }


    async function loadMine() {

        myReview = null;

        if (isLoggedIn()) {

            try {

                const mine = await apiRequest("/reviews/mine?productId=" + encodeURIComponent(product.id), { auth: true });

                myReview = (mine || [])[0] || null;

            } catch (error) {

                /* Hết phiên: coi như chưa đăng nhập (ô "Đăng nhập để đánh giá") */
                myReview = null;

            }

        }

        editing = false;

        renderMine();

    }


    async function loadList(append) {

        const requestId = ++listCounter;

        const nextPage = append ? page + 1 : 0;

        const params = new URLSearchParams({ page: String(nextPage), size: String(REVIEW_PAGE_SIZE) });

        if (filter.rating) {
            params.set("rating", String(filter.rating));
        }

        if (filter.withImages) {
            params.set("withImages", "true");
        }

        moreButton.disabled = true;

        if (!append) {
            listBox.innerHTML = '<p class="review-empty">Đang tải đánh giá…</p>';
        }


        try {

            const result = await apiRequest(base + "?" + params.toString());

            if (requestId !== listCounter) {
                return;
            }

            items = append ? items.concat(result.content || []) : (result.content || []);

            page = result.page;

            totalPages = result.totalPages;

            renderList();

        } catch (error) {

            if (requestId === listCounter) {
                listBox.innerHTML = `<p class="review-empty">${escapeHtml(getErrorMessage(error))}</p>`;
                moreButton.hidden = true;
            }

        } finally {

            moreButton.disabled = false;

        }

    }


    /* ================= TỔNG QUAN ================= */

    function renderSummary() {

        const total = summary ? summary.totalReviews : 0;

        const average = summary ? Number(summary.averageRating) : 0;

        document.getElementById("reviewsTabCount").textContent = String(total);

        document.getElementById("reviewAverage").textContent = average.toFixed(1);

        document.getElementById("reviewAverageStars").innerHTML = starsHtml(average);

        document.getElementById("reviewTotal").textContent = total > 0
            ? total.toLocaleString("vi-VN") + " đánh giá trên POY"
            : "Chưa có đánh giá nào trên POY";

        const stars = summary ? summary.stars : [5, 4, 3, 2, 1].map(function (s) { return { stars: s, count: 0 }; });

        document.getElementById("reviewBars").innerHTML = stars.map(function (row) {

            const percent = total > 0 ? Math.round((row.count / total) * 100) : 0;

            return `
                <li>
                    <span class="review-bar-label">${row.stars} ★</span>
                    <span class="review-bar-track"><span class="review-bar-fill" style="width: ${percent}%"></span></span>
                    <span class="review-bar-count">${row.count.toLocaleString("vi-VN")}</span>
                </li>
            `;

        }).join("");

    }


    function renderTgddLine() {

        const line = document.getElementById("reviewTgdd");

        const hasTgdd = product.tgddReviewCount > 0 && Number(product.tgddRating) > 0;

        line.hidden = !hasTgdd;

        if (hasTgdd) {
            line.textContent = "Trên Thế Giới Di Động: ★ " + Number(product.tgddRating).toFixed(1) + " ("
                + Number(product.tgddReviewCount).toLocaleString("vi-VN")
                + " đánh giá) — số liệu tham khảo, không tính vào điểm trên POY.";
        }

    }


    function renderFilters() {

        const counts = {};

        (summary ? summary.stars : []).forEach(function (row) {
            counts[row.stars] = row.count;
        });

        const buttons = [{ key: "all", label: "Tất cả", count: summary ? summary.totalReviews : 0 }]
            .concat([5, 4, 3, 2, 1].map(function (s) { return { key: String(s), label: s + " ★", count: counts[s] || 0 }; }))
            .concat([{ key: "images", label: "Có ảnh", count: summary ? summary.withImages : 0 }]);

        const active = filter.withImages ? "images" : (filter.rating ? String(filter.rating) : "all");

        filtersBox.innerHTML = buttons.map(function (button) {
            return `<button type="button" class="review-filter${button.key === active ? " active" : ""}" data-filter="${button.key}" aria-pressed="${button.key === active}">${escapeHtml(button.label)} (${button.count})</button>`;
        }).join("");

    }


    /* ================= DANH SÁCH ================= */

    function renderList() {

        if (items.length === 0) {

            const filtered = filter.rating || filter.withImages;

            listBox.innerHTML = `<p class="review-empty">${filtered
                ? "Không có đánh giá nào phù hợp bộ lọc."
                : "Chưa có đánh giá nào. Hãy là người đầu tiên đánh giá sản phẩm này!"}</p>`;

        } else {

            listBox.innerHTML = items.map(reviewHtml).join("");

        }

        moreButton.hidden = page + 1 >= totalPages;

    }


    function reviewHtml(review, ownCard) {

        const images = (review.imageUrls || []).filter(isSafeImageUrl);

        return `
            <article class="review-item${ownCard === true ? " review-item--mine" : ""}" data-review-id="${escapeHtml(String(review.id))}">
                <header class="review-item-head">
                    <strong class="review-author">${escapeHtml(review.authorName)}</strong>
                    ${review.verifiedPurchase ? '<span class="review-badge">✓ Đã mua hàng</span>' : ""}
                </header>
                <div class="review-item-meta">
                    <span class="review-stars" aria-label="${review.rating} trên 5 sao">${starsHtml(review.rating)}</span>
                    <span>${escapeHtml(formatReviewDate(review.createdAt))}</span>
                    ${review.edited ? '<span class="review-edited">(đã chỉnh sửa)</span>' : ""}
                </div>
                <p class="review-comment">${escapeHtml(review.comment)}</p>
                ${images.length ? `
                    <div class="review-photos">
                        ${images.map(function (url, index) {
                            return `<button type="button" class="review-photo" data-viewer="${index}" aria-label="Xem ảnh ${index + 1} của ${escapeHtml(review.authorName)}"><img src="${escapeHtml(url.trim())}" alt="" loading="lazy"></button>`;
                        }).join("")}
                    </div>
                ` : ""}
            </article>
        `;

    }


    /* ================= ĐÁNH GIÁ CỦA BẠN ================= */

    function renderMine() {

        if (!isLoggedIn()) {

            form = null;

            mineBox.innerHTML = `
                <div class="review-login">
                    <p>Bạn đã dùng sản phẩm này? Hãy chia sẻ cảm nhận với mọi người.</p>
                    <button type="button" class="btn btn-dark" data-action="login">ĐĂNG NHẬP ĐỂ ĐÁNH GIÁ</button>
                </div>
            `;

            return;

        }

        if (myReview && !editing) {

            form = null;

            mineBox.innerHTML = `
                <h3 class="review-mine-title">Đánh giá của bạn</h3>
                ${myReview.hidden ? `<p class="review-hidden-note" role="status">Đánh giá của bạn đang bị ẩn khỏi trang sản phẩm. Lý do: ${escapeHtml(myReview.hiddenReason || "")}</p>` : ""}
                ${reviewHtml(myReview, true)}
                <div class="review-mine-actions">
                    <button type="button" class="btn btn-outline-dark" data-action="edit">SỬA ĐÁNH GIÁ</button>
                    <button type="button" class="btn btn-outline-dark review-delete" data-action="delete">XOÁ</button>
                </div>
            `;

            return;

        }

        openForm(editing ? myReview : null);

    }


    function openForm(review) {

        formSession++;

        form = {
            rating: review ? review.rating : 0,
            images: review ? (review.imageUrls || []).slice() : [],
            uploading: 0,
            session: formSession
        };

        mineBox.innerHTML = `
            <form class="review-form" id="reviewForm" novalidate>
                <h3 class="review-mine-title">${review ? "Sửa đánh giá của bạn" : "Viết đánh giá"}</h3>
                <p class="form-error" data-role="review-error" role="alert" hidden></p>

                <div class="review-star-picker" role="radiogroup" aria-label="Chọn số sao">
                    ${[1, 2, 3, 4, 5].map(function (s) {
                        return `<button type="button" class="review-star-btn" data-star="${s}" role="radio" aria-checked="false" aria-label="${s} sao: ${REVIEW_STAR_LABELS[s]}">★</button>`;
                    }).join("")}
                    <span class="review-star-text" data-role="star-text">Chọn số sao</span>
                </div>
                <small class="field-error" data-field="rating"></small>

                <label for="reviewComment">Nhận xét của bạn</label>
                <textarea id="reviewComment" rows="4" maxlength="${REVIEW_COMMENT_MAX}" placeholder="Chất lượng, hiệu năng, đóng gói, giao hàng… (tối thiểu ${REVIEW_COMMENT_MIN} ký tự)">${escapeHtml(review ? review.comment : "")}</textarea>
                <div class="review-form-row">
                    <small class="field-error" data-field="comment"></small>
                    <small class="review-counter" data-role="counter"></small>
                </div>

                <span class="review-form-label">Ảnh thực tế (tối đa ${REVIEW_MAX_IMAGES} ảnh, JPG / PNG / WebP ≤ 5 MB)</span>
                <div class="review-form-photos" data-role="photos"></div>
                <input type="file" id="reviewImageInput" accept="${REVIEW_IMAGE_TYPES.join(",")}" multiple hidden>
                <small class="field-error" data-field="imageUrls"></small>

                <div class="review-mine-actions">
                    <button type="submit" class="btn btn-dark" data-role="submit">${review ? "LƯU THAY ĐỔI" : "GỬI ĐÁNH GIÁ"}</button>
                    ${review ? '<button type="button" class="btn btn-outline-dark" data-action="cancel-edit">HỦY</button>' : ""}
                </div>
            </form>
        `;

        renderStars();

        renderCounter();

        renderFormPhotos();

    }


    function renderStars() {

        mineBox.querySelectorAll(".review-star-btn").forEach(function (button) {

            const star = Number(button.dataset.star);

            button.classList.toggle("is-on", star <= form.rating);

            button.setAttribute("aria-checked", String(star === form.rating));

        });

        mineBox.querySelector('[data-role="star-text"]').textContent = form.rating
            ? form.rating + " sao · " + REVIEW_STAR_LABELS[form.rating]
            : "Chọn số sao";

    }


    function renderCounter() {

        const length = document.getElementById("reviewComment").value.trim().length;

        const counter = mineBox.querySelector('[data-role="counter"]');

        counter.textContent = length + " / " + REVIEW_COMMENT_MAX;

        counter.classList.toggle("is-short", length > 0 && length < REVIEW_COMMENT_MIN);

    }


    function renderFormPhotos() {

        const box = mineBox.querySelector('[data-role="photos"]');

        const tiles = form.images.map(function (url, index) {
            return `
                <figure class="review-form-photo">
                    <img src="${escapeHtml(url)}" alt="">
                    <button type="button" data-action="remove-photo" data-index="${index}" aria-label="Bỏ ảnh ${index + 1}">×</button>
                </figure>
            `;
        });

        for (let i = 0; i < form.uploading; i++) {
            tiles.push('<figure class="review-form-photo is-uploading"><span>Đang tải lên…</span></figure>');
        }

        if (form.images.length + form.uploading < REVIEW_MAX_IMAGES) {
            tiles.push('<button type="button" class="review-form-add" data-action="add-photo">+ Thêm ảnh</button>');
        }

        box.innerHTML = tiles.join("");

    }


    function handleMineClick(event) {

        const target = event.target.closest("[data-action], .review-star-btn, .review-photo");

        if (!target) {
            return;
        }

        if (target.classList.contains("review-photo")) {
            openViewer(myReview ? myReview.imageUrls : [], Number(target.dataset.viewer));
            return;
        }

        if (target.classList.contains("review-star-btn")) {
            form.rating = Number(target.dataset.star);
            renderStars();
            mineBox.querySelector('.field-error[data-field="rating"]').textContent = "";
            return;
        }

        const action = target.dataset.action;

        if (action === "login") {
            redirectToLogin("review");
        } else if (action === "edit") {
            editing = true;
            renderMine();
            document.getElementById("reviewComment").focus();
        } else if (action === "cancel-edit") {
            editing = false;
            renderMine();
        } else if (action === "delete") {
            confirmDelete();
        } else if (action === "add-photo") {
            document.getElementById("reviewImageInput").click();
        } else if (action === "remove-photo") {
            form.images.splice(Number(target.dataset.index), 1);
            renderFormPhotos();
        }

    }


    function handleMineInput(event) {

        if (event.target.id === "reviewComment") {
            renderCounter();
        }

    }


    function handleMineChange(event) {

        if (event.target.id === "reviewImageInput") {

            const files = Array.from(event.target.files || []);

            event.target.value = "";

            uploadPhotos(files);

        }

    }


    /* Kiểm tra loại / dung lượng ở trình duyệt (server kiểm tra lại theo nội dung), tải lần lượt */

    async function uploadPhotos(files) {

        const problems = [];

        const accepted = [];

        files.forEach(function (file) {

            if (REVIEW_IMAGE_TYPES.indexOf(file.type) === -1) {
                problems.push(file.name + ": chỉ nhận JPG, PNG hoặc WebP");
            } else if (file.size > REVIEW_IMAGE_MAX_BYTES) {
                problems.push(file.name + ": lớn hơn 5 MB");
            } else if (form.images.length + form.uploading + accepted.length >= REVIEW_MAX_IMAGES) {
                problems.push(file.name + ": đã đủ " + REVIEW_MAX_IMAGES + " ảnh");
            } else {
                accepted.push(file);
            }

        });

        const current = form;

        current.uploading += accepted.length;

        renderFormPhotos();


        for (const file of accepted) {

            let url = null;

            try {

                const formData = new FormData();

                formData.append("file", file);

                url = (await apiRequest("/uploads/review-images", { method: "POST", body: formData, auth: true })).url;

            } catch (error) {

                problems.push(file.name + ": " + getErrorMessage(error));

            }

            /* Form đã đóng / mở lại trong lúc tải: bỏ kết quả */
            if (form !== current || current.session !== formSession) {
                return;
            }

            current.uploading--;

            if (url) {
                current.images.push(url);
            }

            renderFormPhotos();

        }

        mineBox.querySelector('.field-error[data-field="imageUrls"]').textContent = problems.join(" · ");

    }


    async function submitForm(event) {

        event.preventDefault();

        if (!form) {
            return;
        }

        const comment = document.getElementById("reviewComment").value.trim();

        const errors = {};

        if (!form.rating) {
            errors.rating = "Vui lòng chọn số sao.";
        }

        if (comment.length < REVIEW_COMMENT_MIN || comment.length > REVIEW_COMMENT_MAX) {
            errors.comment = "Nhận xét từ " + REVIEW_COMMENT_MIN + " đến " + REVIEW_COMMENT_MAX + " ký tự (hiện có " + comment.length + ").";
        }

        if (form.uploading > 0) {
            errors.imageUrls = "Vui lòng chờ ảnh tải lên xong.";
        }

        showFormErrors(errors, Object.keys(errors).length ? "Vui lòng kiểm tra lại đánh giá." : "");

        if (Object.keys(errors).length) {
            return;
        }


        const submit = mineBox.querySelector('[data-role="submit"]');

        submit.disabled = true;

        const wasEditing = Boolean(editing && myReview);

        try {

            const body = { productId: product.id, rating: form.rating, comment: comment, imageUrls: form.images };

            if (wasEditing) {
                await apiRequest("/reviews/" + myReview.id, { method: "PUT", body: body, auth: true });
            } else {
                await apiRequest("/reviews", { method: "POST", body: body, auth: true });
            }

            showToast(wasEditing ? "Đã lưu đánh giá của bạn." : "Cảm ơn bạn đã đánh giá sản phẩm!", "success");

            refreshAll();

        } catch (error) {

            submit.disabled = false;

            if (!isLoggedIn()) {
                renderMine();
                showToast("Phiên đăng nhập đã hết, vui lòng đăng nhập lại.", "error");
                return;
            }

            /* Đã có đánh giá (vd. viết ở tab khác) / đánh giá vừa bị xoá: tải lại khối của tôi */
            if (error.code === "ALREADY_REVIEWED" || error.code === "REVIEW_NOT_FOUND") {
                showToast(getErrorMessage(error), "error");
                loadMine();
                return;
            }

            showFormErrors(error.code === "VALIDATION_ERROR" && error.details ? error.details : {}, getErrorMessage(error));

        }

    }


    function showFormErrors(fieldErrors, message) {

        mineBox.querySelectorAll(".review-form .field-error").forEach(function (element) {
            element.textContent = fieldErrors[element.dataset.field] || "";
        });

        const box = mineBox.querySelector('[data-role="review-error"]');

        box.textContent = message;

        box.hidden = !message;

    }


    function confirmDelete() {

        openConfirmModal({
            title: "Xoá đánh giá của bạn?",
            message: "Đánh giá và ảnh kèm theo sẽ bị xoá; bạn có thể viết đánh giá mới sau.",
            confirmLabel: "XOÁ",
            cancelLabel: "Quay lại",
            onConfirm: async function () {

                try {

                    await apiRequest("/reviews/" + myReview.id, { method: "DELETE", auth: true });

                    showToast("Đã xoá đánh giá của bạn.", "success");

                } catch (error) {

                    showToast(getErrorMessage(error), "error");

                }

                refreshAll();

            }
        });

    }


    /* ================= XEM ẢNH LỚN ================= */

    function openViewerFromClick(event) {

        const button = event.target.closest(".review-photo");

        const card = button && button.closest("[data-review-id]");

        const review = card && items.find(function (item) {
            return String(item.id) === card.dataset.reviewId;
        });

        if (review) {
            openViewer(review.imageUrls, Number(button.dataset.viewer));
        }

    }


    function openViewer(urls, startIndex) {

        const images = (urls || []).filter(isSafeImageUrl);

        if (images.length === 0) {
            return;
        }

        let index = Math.min(Math.max(startIndex, 0), images.length - 1);

        const opener = document.activeElement;

        const overlay = document.createElement("div");

        overlay.className = "review-viewer";

        overlay.setAttribute("role", "dialog");

        overlay.setAttribute("aria-modal", "true");

        overlay.setAttribute("aria-label", "Ảnh đánh giá");

        overlay.innerHTML = `
            <button type="button" class="review-viewer-close" data-viewer-action="close" aria-label="Đóng">×</button>
            ${images.length > 1 ? '<button type="button" class="review-viewer-nav prev" data-viewer-action="prev" aria-label="Ảnh trước">‹</button>' : ""}
            <img alt="">
            ${images.length > 1 ? '<button type="button" class="review-viewer-nav next" data-viewer-action="next" aria-label="Ảnh sau">›</button>' : ""}
            <span class="review-viewer-count"></span>
        `;

        const img = overlay.querySelector("img");

        function show() {
            img.src = images[index].trim();
            overlay.querySelector(".review-viewer-count").textContent = images.length > 1 ? (index + 1) + " / " + images.length : "";
        }

        function close() {
            overlay.remove();
            document.removeEventListener("keydown", onKey);
            if (opener && opener.focus) {
                opener.focus();
            }
        }

        function step(delta) {
            index = (index + delta + images.length) % images.length;
            show();
        }

        function onKey(event) {
            if (event.key === "Escape") {
                close();
            } else if (event.key === "ArrowLeft") {
                step(-1);
            } else if (event.key === "ArrowRight") {
                step(1);
            }
        }

        overlay.addEventListener("click", function (event) {

            const action = event.target.closest("[data-viewer-action]");

            if (event.target === overlay || (action && action.dataset.viewerAction === "close")) {
                close();
            } else if (action) {
                step(action.dataset.viewerAction === "prev" ? -1 : 1);
            }

        });

        document.addEventListener("keydown", onKey);

        document.body.appendChild(overlay);

        show();

        overlay.querySelector(".review-viewer-close").focus();

    }


    /* ================= TIỆN ÍCH ================= */

    function starsHtml(value) {

        const rounded = Math.round(Number(value) || 0);

        let html = "";

        for (let s = 1; s <= 5; s++) {
            html += `<span class="${s <= rounded ? "on" : "off"}">★</span>`;
        }

        return html;

    }


    function formatReviewDate(value) {

        const date = value ? new Date(value) : null;

        return date && !isNaN(date.getTime()) ? date.toLocaleDateString("vi-VN") : "";

    }

}
