/* ================= "Bảo hành & đổi trả" ở chi tiết đơn đã giao (customer/order-detail.html) — Phase 6.5 ================= */

/*
 * GET  /orders/{id}/after-sales            hạn bảo hành từng sản phẩm, yêu cầu đang mở, số lượng còn trả được
 * POST /uploads/service-images (multipart) → { url }   ảnh JPG / PNG / WebP ≤ 5 MB
 * POST /warranty-requests | /maintenance-requests | /return-requests
 * Server kiểm tra lại mọi điều kiện (còn bảo hành, 7 ngày, số lượng, trả góp…); lỗi theo trường hiện dưới ô.
 */

const AFTER_SALES_MAX_IMAGES = 5;
const AFTER_SALES_IMAGE_MAX_BYTES = 5 * 1024 * 1024;
const AFTER_SALES_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const AFTER_SALES_TEXT_MIN = 10;
const AFTER_SALES_TEXT_MAX = 2000;


async function initOrderAfterSales(order) {

    const box = document.getElementById("orderAfterSales");

    if (!box) {
        return;
    }

    if (order.status !== "DELIVERED") {
        box.hidden = true;
        return;
    }

    box.hidden = false;

    box.innerHTML = '<p class="after-sales-hint">Đang tải thông tin bảo hành…</p>';

    let view;

    try {

        view = await apiRequest("/orders/" + encodeURIComponent(order.id) + "/after-sales", { auth: true });

    } catch (error) {

        box.innerHTML = `
            <p class="after-sales-hint">Không tải được thông tin bảo hành: ${escapeHtml(getErrorMessage(error))}</p>
            <button type="button" class="btn btn-outline-dark" data-action="retry">THỬ LẠI</button>
        `;

        box.querySelector('[data-action="retry"]').addEventListener("click", function () {
            initOrderAfterSales(order);
        });

        return;

    }

    renderAfterSales(box, order, view);

}


function renderAfterSales(box, order, view) {

    const canReturn = view.returnAvailable && view.items.some(function (item) { return item.returnableQuantity > 0; });

    const returnText = view.returnAvailable
        ? (canReturn
            ? "Trả hàng hoàn tiền đến " + formatOrderDateTime(view.returnDeadline) + " (7 ngày kể từ khi nhận hàng)."
            : "Các sản phẩm của đơn đều đã có yêu cầu trả hàng.")
        : view.returnUnavailableReason;

    box.innerHTML = `
        <div class="after-sales-head">
            <h4>Bảo hành &amp; trả hàng</h4>
            <a href="${escapeHtml(siteUrl("customer/service-requests.html"))}">Yêu cầu dịch vụ của tôi →</a>
        </div>
        <div class="after-sales-return ${canReturn ? "" : "is-unavailable"}">
            <span>${escapeHtml(returnText)}</span>
            ${canReturn ? '<button type="button" class="btn btn-outline-dark" data-action="RETURN">TRẢ HÀNG</button>' : ""}
        </div>
        <ul class="after-sales-items">
            ${view.items.map(afterSalesItemHtml).join("")}
        </ul>
    `;

    box.querySelectorAll("button[data-action]").forEach(function (button) {

        button.addEventListener("click", function () {

            const item = view.items.find(function (i) { return String(i.orderItemId) === button.dataset.line; });

            openAfterSalesForm(button.dataset.action, view, item, button, function () {
                initOrderAfterSales(order);
            });

        });

    });

}


function afterSalesItemHtml(item) {

    let warranty;

    if (item.warrantyValid) {
        warranty = `<span class="after-sales-warranty is-valid">Bảo hành đến ${escapeHtml(afterSalesDate(item.warrantyEndDate))}</span>`;
    } else if (item.warrantyEndDate) {
        warranty = `<span class="after-sales-warranty">Hết bảo hành từ ${escapeHtml(afterSalesDate(item.warrantyEndDate))}</span>`;
    } else {
        warranty = '<span class="after-sales-warranty">Sản phẩm không có bảo hành</span>';
    }

    const line = escapeHtml(String(item.orderItemId));

    const actions = [];

    if (item.openWarrantyRequestId) {
        actions.push(openRequestLink("WARRANTY", item.openWarrantyRequestId, "Đang bảo hành"));
    } else if (item.warrantyValid) {
        actions.push(`<button type="button" class="btn btn-outline-dark" data-action="WARRANTY" data-line="${line}">GỬI BẢO HÀNH</button>`);
    }

    if (item.openMaintenanceRequestId) {
        actions.push(openRequestLink("MAINTENANCE", item.openMaintenanceRequestId, "Đang bảo trì"));
    } else {
        actions.push(`<button type="button" class="btn btn-outline-dark" data-action="MAINTENANCE" data-line="${line}">GỬI BẢO TRÌ</button>`);
    }

    return `
        <li class="after-sales-item" data-line="${line}">
            <div class="after-sales-item-info">
                <strong>${escapeHtml(item.productName)}</strong>
                <span class="after-sales-warranty">${escapeHtml(item.variantName || "")} · SL ${escapeHtml(String(item.quantity))}</span>
                ${warranty}
            </div>
            <div class="after-sales-actions">${actions.join("")}</div>
        </li>
    `;

}


function openRequestLink(type, id, label) {

    const url = siteUrl("customer/service-requests.html?type=" + type + "&id=" + encodeURIComponent(id));

    return `<a class="after-sales-open-link" href="${escapeHtml(url)}">${escapeHtml(label)} →</a>`;

}


/* ================= FORM GỬI YÊU CẦU ================= */

function openAfterSalesForm(kind, view, item, opener, onDone) {

    closeModal();

    const isReturn = kind === "RETURN";

    const titles = { WARRANTY: "Gửi yêu cầu bảo hành", MAINTENANCE: "Gửi yêu cầu bảo trì", RETURN: "Trả hàng hoàn tiền" };

    const textLabels = {
        WARRANTY: "Mô tả lỗi của sản phẩm *",
        MAINTENANCE: "Bạn cần bảo trì gì? *",
        RETURN: "Mô tả lý do trả hàng *"
    };

    const returnable = isReturn ? view.items.filter(function (i) { return i.returnableQuantity > 0; }) : [];

    const state = { images: [], uploading: 0, saving: false, closed: false };


    const overlay = document.createElement("div");

    overlay.className = "modal-overlay";

    overlay.innerHTML = `
        <div class="modal-box after-sales-modal" role="dialog" aria-modal="true" aria-labelledby="afterSalesTitle">
            <h3 id="afterSalesTitle">${escapeHtml(titles[kind])}</h3>
            ${item ? `<p class="after-sales-product">${escapeHtml(item.productName)}${item.variantName ? " · " + escapeHtml(item.variantName) : ""}</p>` : ""}
            <form class="review-form" novalidate>
                ${kind === "MAINTENANCE" ? `
                    <label for="asMaintenanceType">Loại bảo trì *</label>
                    <select id="asMaintenanceType">
                        ${Object.keys(AFTER_SALES_MAINTENANCE_TYPE_LABELS).map(function (key) {
                            return `<option value="${key}">${escapeHtml(AFTER_SALES_MAINTENANCE_TYPE_LABELS[key])}</option>`;
                        }).join("")}
                    </select>
                    <p class="after-sales-hint">Bảo trì có thể mất phí; cửa hàng báo chi phí dự kiến trước khi làm, bạn trả tại cửa hàng.</p>
                ` : ""}
                ${isReturn ? `
                    <span class="review-form-label">Sản phẩm trả *</span>
                    <div class="after-sales-return-lines">
                        ${returnable.map(function (line) {
                            const id = escapeHtml(String(line.orderItemId));
                            return `
                                <div class="after-sales-return-line" data-line="${id}">
                                    <label>
                                        <input type="checkbox" value="${id}" data-unit="${escapeHtml(String(line.unitPrice))}">
                                        ${escapeHtml(line.productName)}${line.variantName ? " · " + escapeHtml(line.variantName) : ""}
                                    </label>
                                    <input type="number" min="1" max="${line.returnableQuantity}" value="${line.returnableQuantity}" aria-label="Số lượng trả">
                                    <span class="after-sales-hint">còn trả được ${line.returnableQuantity}</span>
                                </div>
                            `;
                        }).join("")}
                    </div>
                    <small class="field-error" data-field="items"></small>
                    <p class="after-sales-refund">Số tiền hoàn dự kiến: <strong data-role="refund">0đ</strong> (theo giá bạn đã trả, không gồm phí giao hàng)</p>
                    <label for="asReasonType">Lý do *</label>
                    <select id="asReasonType">
                        ${Object.keys(AFTER_SALES_RETURN_REASON_LABELS).map(function (key) {
                            return `<option value="${key}">${escapeHtml(AFTER_SALES_RETURN_REASON_LABELS[key])}</option>`;
                        }).join("")}
                    </select>
                ` : ""}
                <div class="review-form-row">
                    <label for="asText">${escapeHtml(textLabels[kind])}</label>
                    <span class="review-counter" data-role="counter">0 / ${AFTER_SALES_TEXT_MAX}</span>
                </div>
                <textarea id="asText" rows="4" maxlength="${AFTER_SALES_TEXT_MAX}" placeholder="Ít nhất ${AFTER_SALES_TEXT_MIN} ký tự"></textarea>
                <small class="field-error" data-field="text"></small>
                <span class="review-form-label">Ảnh (tối đa ${AFTER_SALES_MAX_IMAGES} ảnh, JPG / PNG / WebP ≤ 5 MB)</span>
                <div class="review-form-photos" data-role="photos"></div>
                <input type="file" accept="image/jpeg,image/png,image/webp" multiple hidden data-role="file">
                <small class="field-error" data-field="imageUrls"></small>
                <p class="form-error after-sales-form-error" data-role="error" role="alert" hidden></p>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline-dark" data-role="cancel">HỦY</button>
                    <button type="submit" class="btn btn-dark" data-role="submit">GỬI YÊU CẦU</button>
                </div>
            </form>
        </div>
    `;

    document.body.appendChild(overlay);

    modalOverlayElement = overlay;

    const formElement = overlay.querySelector("form");
    const textInput = overlay.querySelector("#asText");
    const counter = overlay.querySelector('[data-role="counter"]');
    const photosBox = overlay.querySelector('[data-role="photos"]');
    const fileInput = overlay.querySelector('[data-role="file"]');
    const errorBox = overlay.querySelector('[data-role="error"]');
    const submitButton = overlay.querySelector('[data-role="submit"]');


    function close() {

        state.closed = true;

        if (modalOverlayElement === overlay) {
            closeModal();
        }

        if (opener && document.body.contains(opener)) {
            opener.focus();
        }

    }


    function fieldError(field, message) {
        overlay.querySelector('.field-error[data-field="' + field + '"]').textContent = message || "";
    }


    function updateCounter() {
        const length = textInput.value.trim().length;
        counter.textContent = length + " / " + AFTER_SALES_TEXT_MAX;
        counter.classList.toggle("is-short", length > 0 && length < AFTER_SALES_TEXT_MIN);
    }


    function updateRefund() {

        if (!isReturn) {
            return;
        }

        let total = 0;

        overlay.querySelectorAll(".after-sales-return-line").forEach(function (row) {
            const check = row.querySelector('input[type="checkbox"]');
            const quantity = Number(row.querySelector('input[type="number"]').value) || 0;
            if (check.checked) {
                total += Number(check.dataset.unit) * quantity;
            }
        });

        overlay.querySelector('[data-role="refund"]').textContent = formatPrice(total);

    }


    function renderPhotos() {

        const tiles = state.images.map(function (url, index) {
            return `
                <figure class="review-form-photo">
                    <img src="${escapeHtml(url)}" alt="">
                    <button type="button" data-remove="${index}" aria-label="Bỏ ảnh ${index + 1}">×</button>
                </figure>
            `;
        });

        for (let i = 0; i < state.uploading; i++) {
            tiles.push('<figure class="review-form-photo is-uploading">Đang tải lên…</figure>');
        }

        if (state.images.length + state.uploading < AFTER_SALES_MAX_IMAGES) {
            tiles.push('<button type="button" class="review-form-add" data-role="add">+ THÊM ẢNH</button>');
        }

        photosBox.innerHTML = tiles.join("");

    }


    /* Kiểm tra loại / dung lượng ở trình duyệt (server kiểm tra lại theo nội dung), tải lần lượt */
    async function uploadPhotos(files) {

        const problems = [];

        const accepted = [];

        files.forEach(function (file) {
            if (AFTER_SALES_IMAGE_TYPES.indexOf(file.type) === -1) {
                problems.push(file.name + ": chỉ nhận JPG, PNG hoặc WebP");
            } else if (file.size > AFTER_SALES_IMAGE_MAX_BYTES) {
                problems.push(file.name + ": lớn hơn 5 MB");
            } else if (state.images.length + state.uploading + accepted.length >= AFTER_SALES_MAX_IMAGES) {
                problems.push(file.name + ": đã đủ " + AFTER_SALES_MAX_IMAGES + " ảnh");
            } else {
                accepted.push(file);
            }
        });

        state.uploading += accepted.length;

        renderPhotos();

        for (const file of accepted) {

            let url = null;

            try {
                const formData = new FormData();
                formData.append("file", file);
                url = (await apiRequest("/uploads/service-images", { method: "POST", body: formData, auth: true })).url;
            } catch (error) {
                problems.push(file.name + ": " + getErrorMessage(error));
            }

            if (state.closed) {
                return;
            }

            state.uploading--;

            if (url) {
                state.images.push(url);
            }

            renderPhotos();

        }

        fieldError("imageUrls", problems.join(" · "));

    }


    function collect() {

        let ok = true;

        ["items", "text", "imageUrls"].forEach(function (field) {
            if (overlay.querySelector('.field-error[data-field="' + field + '"]')) {
                fieldError(field, "");
            }
        });

        errorBox.hidden = true;

        const text = textInput.value.trim();

        if (text.length < AFTER_SALES_TEXT_MIN) {
            fieldError("text", "Vui lòng nhập ít nhất " + AFTER_SALES_TEXT_MIN + " ký tự.");
            ok = false;
        }

        if (state.uploading > 0) {
            fieldError("imageUrls", "Vui lòng chờ ảnh tải lên xong.");
            ok = false;
        }

        const body = { imageUrls: state.images.slice() };

        if (isReturn) {

            const items = [];

            overlay.querySelectorAll(".after-sales-return-line").forEach(function (row) {
                const check = row.querySelector('input[type="checkbox"]');
                const quantityInput = row.querySelector('input[type="number"]');
                if (check.checked) {
                    items.push({ orderItemId: Number(check.value), quantity: Number(quantityInput.value), max: Number(quantityInput.max) });
                }
            });

            if (items.length === 0) {
                fieldError("items", "Vui lòng chọn ít nhất một sản phẩm.");
                ok = false;
            } else if (items.some(function (i) { return !Number.isInteger(i.quantity) || i.quantity < 1 || i.quantity > i.max; })) {
                fieldError("items", "Số lượng trả phải từ 1 đến số còn trả được.");
                ok = false;
            }

            body.orderId = view.orderId;
            body.reasonType = overlay.querySelector("#asReasonType").value;
            body.reason = text;
            body.items = items.map(function (i) { return { orderItemId: i.orderItemId, quantity: i.quantity }; });

        } else {

            body.orderItemId = item.orderItemId;
            body.description = text;

            if (kind === "MAINTENANCE") {
                body.maintenanceType = overlay.querySelector("#asMaintenanceType").value;
            }

        }

        return ok ? body : null;

    }


    async function submit(event) {

        event.preventDefault();

        if (state.saving) {
            return;
        }

        const body = collect();

        if (!body) {
            return;
        }

        const paths = { WARRANTY: "/warranty-requests", MAINTENANCE: "/maintenance-requests", RETURN: "/return-requests" };

        state.saving = true;

        submitButton.disabled = true;

        try {

            const created = await apiRequest(paths[kind], { method: "POST", body: body, auth: true });

            close();

            showToast("Đã gửi yêu cầu " + created.code + ". Cửa hàng sẽ liên hệ với bạn.", "success");

            onDone();

        } catch (error) {

            if (error.status === 401) {
                redirectToLogin();
                return;
            }

            const details = error.code === "VALIDATION_ERROR" && error.details ? error.details : {};

            let shown = false;

            Object.keys(details).forEach(function (field) {
                const target = field === "description" || field === "reason" ? "text" : field;
                if (overlay.querySelector('.field-error[data-field="' + target + '"]')) {
                    fieldError(target, details[field]);
                    shown = true;
                }
            });

            if (!shown) {
                errorBox.textContent = getErrorMessage(error);
                errorBox.hidden = false;
            }

        } finally {

            state.saving = false;

            submitButton.disabled = false;

        }

    }


    textInput.addEventListener("input", function () {
        updateCounter();
        if (textInput.value.trim().length >= AFTER_SALES_TEXT_MIN) {
            fieldError("text", "");
        }
    });

    overlay.addEventListener("input", updateRefund);

    formElement.addEventListener("submit", submit);

    overlay.querySelector('[data-role="cancel"]').addEventListener("click", close);

    photosBox.addEventListener("click", function (event) {

        const remove = event.target.closest("[data-remove]");

        if (remove) {
            state.images.splice(Number(remove.dataset.remove), 1);
            renderPhotos();
            fieldError("imageUrls", "");
        } else if (event.target.closest('[data-role="add"]')) {
            fileInput.click();
        }

    });

    fileInput.addEventListener("change", function () {
        const files = Array.from(fileInput.files || []);
        fileInput.value = "";
        uploadPhotos(files);
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

    renderPhotos();

    updateRefund();

    (isReturn ? overlay.querySelector('.after-sales-return-line input[type="checkbox"]') : textInput)?.focus();

    requestAnimationFrame(function () {
        overlay.classList.add("show");
    });

}
