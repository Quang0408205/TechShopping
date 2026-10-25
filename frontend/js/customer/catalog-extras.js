/* ================= THẺ SẢN PHẨM SỐNG ĐỘNG (kế hoạch v8 bước 3): XEM NHANH, nghiêng nhẹ ================= */

/*
 * Gắn bằng uỷ quyền sự kiện nên dùng được cho mọi lưới thẻ (trang chủ, Sản phẩm, chi tiết).
 * - "Xem nhanh": cửa sổ chọn phiên bản + màu, thêm giỏ qua addToCart (cùng đường với trang chi tiết).
 * - Nghiêng 3D nhẹ theo con trỏ: chỉ khi có chuột, không bật khi người dùng giảm chuyển động.
 */

(function () {

    const finePointer = window.matchMedia && window.matchMedia("(hover: hover) and (pointer: fine)").matches;

    const reduced = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    /* ================= NGHIÊNG ================= */

    if (finePointer && !reduced) {

        document.addEventListener("pointermove", function (event) {

            const card = event.target.closest && event.target.closest(".product-card");

            if (!card || card.classList.contains("is-flat")) {
                return;
            }

            const box = card.getBoundingClientRect();

            const x = (event.clientX - box.left) / box.width - 0.5;

            const y = (event.clientY - box.top) / box.height - 0.5;

            card.style.setProperty("--tilt-x", (-y * 3).toFixed(2) + "deg");

            card.style.setProperty("--tilt-y", (x * 3).toFixed(2) + "deg");

            card.classList.add("is-tilting");

        });

        document.addEventListener("pointerout", function (event) {

            const card = event.target.closest && event.target.closest(".product-card");

            if (card && !card.contains(event.relatedTarget)) {
                card.classList.remove("is-tilting");
                card.style.removeProperty("--tilt-x");
                card.style.removeProperty("--tilt-y");
            }

        });

    }


    /* ================= XEM NHANH ================= */

    let quick = null;

    document.addEventListener("click", function (event) {

        const button = event.target.closest && event.target.closest("[data-quickview]");

        if (!button) {
            return;
        }

        event.preventDefault();

        event.stopPropagation();

        openQuickView(button.closest(".product-card").dataset.productId, button);

    }, true);


    function closeQuickView() {

        if (!quick) {
            return;
        }

        quick.root.remove();

        document.removeEventListener("keydown", quick.onKey);

        document.body.classList.remove("quickview-open");

        quick.opener.focus();

        quick = null;

    }


    async function openQuickView(productId, opener) {

        closeQuickView();

        const root = document.createElement("div");

        root.className = "quickview";

        root.innerHTML = `<div class="quickview-backdrop" data-qv-close></div>
            <div class="quickview-dialog" role="dialog" aria-modal="true" aria-label="Xem nhanh sản phẩm">
                <button type="button" class="quickview-close" data-qv-close aria-label="Đóng">&times;</button>
                <p class="quickview-loading">Đang tải…</p>
            </div>`;

        document.body.appendChild(root);

        document.body.classList.add("quickview-open");

        const onKey = function (e) { if (e.key === "Escape") { closeQuickView(); } };

        document.addEventListener("keydown", onKey);

        quick = { root: root, opener: opener, onKey: onKey };

        root.addEventListener("click", function (e) {

            if (e.target.closest("[data-qv-close]")) {
                closeQuickView();
            }

        });

        let product, variants, images;

        try {

            const results = await Promise.all([
                apiRequest("/products/" + encodeURIComponent(productId)),
                apiRequest("/products/" + encodeURIComponent(productId) + "/variants").catch(function () { return []; }),
                apiRequest("/products/" + encodeURIComponent(productId) + "/images").catch(function () { return []; })
            ]);

            product = results[0];
            variants = (results[1] || []).filter(function (item) { return item.isActive !== false; });
            images = results[2] || [];

        } catch (error) {

            if (quick && quick.root === root) {
                root.querySelector(".quickview-loading").textContent = "Không tải được sản phẩm. Vui lòng thử lại.";
            }

            return;

        }

        if (!quick || quick.root !== root) {
            return;
        }

        renderQuickView(root, product, variants, images);

    }


    function renderQuickView(root, product, variants, images) {

        let current = variants.find(function (item) { return !isSoldOut(item); }) || variants[0] || null;

        const dialog = root.querySelector(".quickview-dialog");

        const picture = isSafeImageUrl(product.primaryImageUrl) ? product.primaryImageUrl.trim()
            : (images.map(function (item) { return item.imageUrl || item.url; }).find(isSafeImageUrl) || "");

        function draw() {

            const prices = resolvePrices(product, current);

            const canBuy = prices.price > 0 && !(current && isSoldOut(current));

            const versions = variants.map(function (item, index) {
                return `<button type="button" class="quickview-option${item === current ? " active" : ""}${isSoldOut(item) ? " is-out" : ""}" data-qv-variant="${index}" aria-pressed="${item === current}">${escapeHtml(variantLabelOf(item) || "Mặc định")}</button>`;
            }).join("");

            dialog.innerHTML = `<button type="button" class="quickview-close" data-qv-close aria-label="Đóng">&times;</button>
                <div class="quickview-body">
                    <div class="quickview-image">${picture ? `<img src="${escapeHtml(picture)}" alt="${escapeHtml(product.name)}" referrerpolicy="no-referrer">` : ""}</div>
                    <div class="quickview-info">
                        <span class="product-category">${escapeHtml((product.categoryName || "").toUpperCase())}</span>
                        <h2>${escapeHtml(product.name)}</h2>
                        <p class="quickview-price"><strong>${prices.price > 0 ? escapeHtml(formatPrice(prices.price)) : "Liên hệ"}</strong>${prices.oldPrice ? `<s>${escapeHtml(formatPrice(prices.oldPrice))}</s>` : ""}</p>
                        ${versions ? `<div class="quickview-options" role="group" aria-label="Phiên bản">${versions}</div>` : ""}
                        <div class="quickview-actions">
                            <button type="button" class="btn btn-dark" data-qv-add ${canBuy ? "" : "disabled"}>${canBuy ? "Thêm vào giỏ" : (prices.price > 0 ? "Tạm hết hàng" : "Liên hệ")}</button>
                            <a class="btn btn-outline-dark" href="${escapeHtml(getProductDetailUrl(product.id))}">Xem chi tiết</a>
                        </div>
                    </div>
                </div>`;

            const focus = dialog.querySelector("[data-qv-add]");

            if (focus && !focus.disabled) {
                focus.focus();
            }

        }

        dialog.addEventListener("click", async function (event) {

            const pick = event.target.closest("[data-qv-variant]");

            if (pick) {
                current = variants[Number(pick.dataset.qvVariant)];
                draw();
                return;
            }

            const add = event.target.closest("[data-qv-add]");

            if (add && !add.disabled) {

                add.disabled = true;

                const prices = resolvePrices(product, current);

                const done = await addToCart({
                    productId: product.id,
                    variantId: current ? current.id : null,
                    name: product.name,
                    variantLabel: current ? variantLabelOf(current) : "",
                    price: prices.price,
                    image: picture || null
                }, 1, dialog.querySelector(".quickview-image img"));

                if (done) {
                    closeQuickView();
                } else {
                    add.disabled = false;
                }

            }

        });

        draw();

    }

})();
