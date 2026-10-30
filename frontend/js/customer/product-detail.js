/* ================= TRANG CHI TIẾT SẢN PHẨM (Checkpoint A2) ================= */

/*
 * customer/product-detail.html?id=<productId>. Dữ liệu thật, công khai
 * (KHÔNG gửi token):
 *   GET /products/{id}                 → thông tin chính (404 → "không tìm thấy")
 *   GET /products/{id}/variants        → phiên bản (giá, màu, RAM, bộ nhớ)
 *   GET /products/{id}/images          → ảnh (ảnh chính trước)
 *   GET /products/{id}/specifications  → thông số (nếu có)
 *   POST /recommendations (SIMILAR, UPSELL, CROSS_SELL) → "Sản phẩm tương tự", "Có thể nâng cấp", "Mua kèm"
 *
 * Dựa trên trang chi tiết của bản frontend tham chiếu, nhưng không bịa phân
 * bố sao (dòng ★ = đánh giá thật, chưa có thì điểm Thế Giới Di Động), bỏ các vùng
 * gợi ý mua kèm / nâng cấp (Phase 8) và không dùng catalogue mock.
 * Giỏ hàng: snapshot {productId, variantId, ...} theo tài khoản (js/core/cart-store.js, F2).
 */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        const MAX_QUANTITY = 10;

        const RELATED_COUNT = 4;


        const productId = new URLSearchParams(window.location.search).get("id");

        const skeleton = document.getElementById("detailSkeleton");

        const notFoundBox = document.getElementById("detailNotFound");

        const errorBox = document.getElementById("detailError");

        const content = document.getElementById("detailContent");

        const qtyValue = document.getElementById("qtyValue");

        const addToCartBtn = document.getElementById("addToCartBtn");

        const buyNowBtn = document.getElementById("buyNowBtn");


        let product = null;

        let variants = [];

        let specifications = [];

        let currentVariant = null;

        let quantity = 1;

        let categoryMaps = { byId: {}, bySlug: {} };


        setupTabs();

        /* Bấm dòng ★ → mở tab Đánh giá */
        document.getElementById("detailRating").addEventListener("click", function () {

            const tab = document.querySelector('.tab-btn[data-tab="reviews"]');

            tab.click();

            tab.scrollIntoView({ behavior: "smooth", block: "start" });

        });

        setupQuantityControls();

        setupActions();


        /* id phải là số nguyên dương; sai định dạng thì không cần gọi API */

        if (!productId || !/^\d+$/.test(productId)) {

            showNotFound();

        } else {

            loadProduct();

        }


        /* ================= TẢI DỮ LIỆU ================= */

        async function loadProduct() {

            skeleton.hidden = false;

            errorBox.hidden = true;

            const base = "/products/" + productId;


            /* Ảnh / phiên bản / thông số / danh mục lỗi thì vẫn hiện trang */

            const extras = Promise.allSettled([
                apiRequest(base + "/variants"),
                apiRequest(base + "/images"),
                apiRequest(base + "/specifications"),
                fetchCategoryMaps()
            ]);


            try {

                product = await apiRequest(base);

            } catch (error) {

                skeleton.hidden = true;

                if (error.status === 404) {
                    showNotFound();
                } else {
                    showError(getErrorMessage(error));
                }

                return;

            }


            /* Sản phẩm đã ngừng bán: xử lý như không tìm thấy */

            if (product.isActive === false) {

                skeleton.hidden = true;

                showNotFound();

                return;

            }


            trackInteraction(product.id, "VIEW");


            const results = await extras;

            variants = valueOrEmpty(results[0]);

            const images = valueOrEmpty(results[1]);

            specifications = valueOrEmpty(results[2])
                .slice()
                .sort(function (a, b) {
                    return (a.specOrder || 0) - (b.specOrder || 0);
                });

            if (results[3].status === "fulfilled") {
                categoryMaps = results[3].value;
            }


            /* Kế hoạch v3: danh mục bị ẩn → sản phẩm không bán, xử lý như ngừng bán */

            if (categoryMaps && categoryMaps.hiddenIds && categoryMaps.hiddenIds[product.categoryId]) {

                skeleton.hidden = true;

                showNotFound();

                return;

            }


            skeleton.hidden = true;

            await loadStoreStock(product.id);

            renderProduct(images);

            loadCompareSuggestions(product, resolvePrices(product, null).price);

            loadRelatedProducts();

            loadCombos();

        }


        function valueOrEmpty(result) {

            return result.status === "fulfilled" && Array.isArray(result.value)
                ? result.value
                : [];

        }


        function showNotFound() {

            skeleton.hidden = true;

            content.hidden = true;

            notFoundBox.hidden = false;

            document.getElementById("breadcrumbCurrent").textContent =
                "Không tìm thấy";

        }


        function showError(message) {

            errorBox.innerHTML = errorStateHtml(message, "detailRetryBtn");

            errorBox.hidden = false;

            document.getElementById("detailRetryBtn")
                .addEventListener("click", loadProduct);

        }


        /* ================= HIỂN THỊ ================= */

        function renderProduct(images) {

            const categorySlug = categoryMaps.byId[product.categoryId];


            document.title = product.name + " - POY";

            document.getElementById("breadcrumbCurrent").textContent = product.name;

            renderBreadcrumbCategory(categorySlug);


            document.getElementById("detailCategory").textContent =
                (product.categoryName || "").toUpperCase();

            document.getElementById("detailName").textContent = product.name;

            document.getElementById("detailMeta").textContent = buildMetaText();

            renderRatingLink();

            /* Tab "ĐÁNH GIÁ" (js/customer/product-reviews.js); viết / sửa / xoá đánh giá → cập nhật dòng ★ */
            initProductReviews(product, {
                onSummaryChange: function (summary) {
                    product.rating = summary.averageRating;
                    product.totalReviews = summary.totalReviews;
                    renderRatingLink();
                }
            });


            renderImages(images, categorySlug);

            /* nút yêu thích / so sánh (compare-wishlist.js đọc data-*; trạng thái bật tắt do script đó cập nhật) */
            ["detailWishBtn", "detailCompareBtn"].forEach(function (id) {

                const tool = document.getElementById(id);

                tool.dataset.productId = String(product.id);
                tool.dataset.categoryId = String(product.categoryId);
                tool.dataset.name = product.name;
                tool.dataset.image = isSafeImageUrl(product.primaryImageUrl) ? product.primaryImageUrl.trim() : "";

                tool.hidden = false;

            });

            renderVariants();


            document.getElementById("detailDescription").textContent =
                product.description && product.description.trim()
                    ? product.description
                    : "Mô tả chi tiết của sản phẩm đang được cập nhật.";


            const warranty = document.getElementById("detailWarranty");

            if (product.warrantyMonths > 0) {

                warranty.textContent =
                    "Bảo hành chính hãng " + product.warrantyMonths + " tháng";

                warranty.hidden = false;

            }


            content.hidden = false;

        }


        /* Đường dẫn: Trang chủ / Sản phẩm / <Danh mục> / <Tên> */

        function renderBreadcrumbCategory(categorySlug) {

            if (!product.categoryName) {
                return;
            }

            const link = document.getElementById("breadcrumbCategory");

            link.textContent = product.categoryName;

            link.href = siteUrl(
                categorySlug
                    ? "customer/products.html?category=" + encodeURIComponent(categoryKeyOf(categorySlug))
                    : "customer/products.html"
            );

            link.hidden = false;

            document.getElementById("breadcrumbCategorySep").hidden = false;

        }


        /* Thương hiệu · Mã (điểm đánh giá là nút riêng #detailRating, bấm → tab Đánh giá) */

        function buildMetaText() {

            const parts = [];

            if (product.brandName) {
                parts.push("Thương hiệu: " + product.brandName);
            }

            if (product.sku) {
                parts.push("Mã: " + product.sku);
            }

            return parts.join(" · ");

        }


        /* Đánh giá thật trên web; chưa có → "Chưa có đánh giá" (bấm vẫn mở tab Đánh giá để viết) */

        function renderRatingLink() {

            const link = document.getElementById("detailRating");

            const rating = productRatingSummary(product);

            link.hidden = false;

            link.textContent = rating
                ? "★ " + rating.rating.toFixed(1) + " (" + rating.count.toLocaleString("vi-VN") + " đánh giá)"
                : "Chưa có đánh giá";

        }


        /* ================= ẢNH ================= */

        let galleryImages = [];

        let galleryColor;

        let galleryFallback = "";

        function renderImages(images, categorySlug) {

            const mainImage = document.getElementById("mainImage");

            galleryFallback = getFallbackImage(categorySlug);

            galleryImages = images
                .filter(function (image) {
                    return isSafeImageUrl(image.imageUrl);
                })
                .sort(function (a, b) {
                    if (a.isPrimary !== b.isPrimary) {
                        return a.isPrimary ? -1 : 1;
                    }
                    return (a.displayOrder || 0) - (b.displayOrder || 0);
                });


            mainImage.alt = product.name;
            // The product CDN returns 403 when the storefront URL is sent as Referer.
            mainImage.referrerPolicy = "no-referrer";

            mainImage.addEventListener("error", function () {

                if (mainImage.src !== galleryFallback) {
                    mainImage.src = galleryFallback;
                }

            });

            showGallery(null, true);

        }


        /* Ảnh theo màu: màu có ảnh riêng → chỉ ảnh của màu đó; không có → ảnh chung (color NULL); sản phẩm không gắn màu → tất cả */

        function colorKey(value) {

            return String(value || "").trim().toLowerCase();

        }

        function imagesForColor(color) {

            const key = colorKey(color);

            const own = key ? galleryImages.filter(function (image) { return colorKey(image.color) === key; }) : [];

            if (own.length) {
                return own;
            }

            const common = galleryImages.filter(function (image) { return !colorKey(image.color); });

            return common.length ? common : galleryImages;

        }

        /*
         * Khung ảnh (kế hoạch v6 B1): mũi tên ‹ › nằm trên ảnh chính, bấm → ảnh kế tiếp thay ảnh chính (vòng lại),
         * dải ảnh nhỏ bên dưới đổi viền đang chọn và tự cuộn theo; vuốt trên điện thoại, phím ← →, số "3 / 12",
         * bấm ảnh chính → xem toàn màn hình (lightbox).
         */

        let galleryUrls = [];

        let galleryIndex = 0;

        const stage = document.getElementById("detailStage");

        const lightbox = createLightbox();

        function setMainImage(direction) {

            const mainImage = document.getElementById("mainImage");

            const url = galleryUrls[galleryIndex] || galleryFallback;

            if (direction) {
                mainImage.classList.remove("is-sliding");
                mainImage.style.setProperty("--slide-from", direction > 0 ? "36px" : "-36px");
            }

            mainImage.src = url;

            if (direction) {
                void mainImage.offsetWidth;
                mainImage.classList.add("is-sliding");
            }

            const many = galleryUrls.length > 1;

            document.getElementById("mainPrev").hidden = !many;

            document.getElementById("mainNext").hidden = !many;

            const counter = document.getElementById("imageCounter");

            counter.hidden = !many;

            counter.textContent = (galleryIndex + 1) + " / " + galleryUrls.length;

            stage.classList.toggle("has-gallery", galleryUrls.length > 0);

            const thumbnails = document.getElementById("detailThumbnails");

            thumbnails.querySelectorAll(".detail-thumb").forEach(function (button, index) {

                const on = index === galleryIndex;

                button.classList.toggle("active", on);

                if (on) {
                    thumbnails.scrollTo({
                        left: button.offsetLeft - (thumbnails.clientWidth - button.offsetWidth) / 2,
                        behavior: "smooth"
                    });
                }

            });

            lightbox.sync();

        }

        function goToImage(index, direction) {

            if (galleryUrls.length === 0) {
                return;
            }

            galleryIndex = (index + galleryUrls.length) % galleryUrls.length;

            setMainImage(direction);

        }

        function stepImage(step) {

            goToImage(galleryIndex + step, step);

        }

        function setupGalleryControls() {

            document.getElementById("mainPrev").addEventListener("click", function (event) {
                event.stopPropagation();
                stepImage(-1);
            });

            document.getElementById("mainNext").addEventListener("click", function (event) {
                event.stopPropagation();
                stepImage(1);
            });

            stage.addEventListener("click", function () {

                if (galleryUrls.length > 0) {
                    lightbox.open();
                }

            });

            stage.addEventListener("keydown", function (event) {

                if (event.key === "ArrowLeft") {
                    stepImage(-1);
                } else if (event.key === "ArrowRight") {
                    stepImage(1);
                } else if ((event.key === "Enter" || event.key === " ") && galleryUrls.length > 0) {
                    event.preventDefault();
                    lightbox.open();
                }

            });

            bindSwipe(stage, stepImage);

        }

        /* Vuốt ngang: sang trái → ảnh sau, sang phải → ảnh trước (bỏ qua khi vuốt dọc để cuộn trang) */
        function bindSwipe(element, onStep) {

            let startX = null;

            let startY = null;

            element.addEventListener("touchstart", function (event) {
                startX = event.touches[0].clientX;
                startY = event.touches[0].clientY;
            }, { passive: true });

            element.addEventListener("touchend", function (event) {

                if (startX === null) {
                    return;
                }

                const dx = event.changedTouches[0].clientX - startX;

                const dy = event.changedTouches[0].clientY - startY;

                startX = null;

                if (Math.abs(dx) > 40 && Math.abs(dx) > Math.abs(dy) * 1.5) {
                    onStep(dx < 0 ? 1 : -1);
                }

            }, { passive: true });

        }

        function createLightbox() {

            const root = document.createElement("div");

            root.className = "lightbox";

            root.hidden = true;

            root.setAttribute("role", "dialog");

            root.setAttribute("aria-modal", "true");

            root.setAttribute("aria-label", "Xem ảnh sản phẩm");

            root.innerHTML = `
                <button type="button" class="lightbox-close" aria-label="Đóng">&times;</button>
                <button type="button" class="lightbox-arrow is-prev" aria-label="Ảnh trước">&#8249;</button>
                <img class="lightbox-image" alt="" referrerpolicy="no-referrer">
                <button type="button" class="lightbox-arrow is-next" aria-label="Ảnh sau">&#8250;</button>
                <span class="lightbox-counter"></span>
            `;

            document.body.appendChild(root);

            const image = root.querySelector(".lightbox-image");

            let opener = null;

            function close() {

                root.hidden = true;

                document.body.classList.remove("lightbox-open");

                document.removeEventListener("keydown", onKey);

                if (opener) {
                    opener.focus({ preventScroll: true });
                }

            }

            function onKey(event) {

                if (event.key === "Escape") {
                    close();
                } else if (event.key === "ArrowLeft") {
                    stepImage(-1);
                } else if (event.key === "ArrowRight") {
                    stepImage(1);
                }

            }

            root.querySelector(".lightbox-close").addEventListener("click", close);

            root.querySelector(".is-prev").addEventListener("click", function () { stepImage(-1); });

            root.querySelector(".is-next").addEventListener("click", function () { stepImage(1); });

            root.addEventListener("click", function (event) {

                if (event.target === root) {
                    close();
                }

            });

            bindSwipe(root, stepImage);

            return {

                open: function () {
                    opener = document.activeElement;
                    root.hidden = false;
                    document.body.classList.add("lightbox-open");
                    document.addEventListener("keydown", onKey);
                    this.sync();
                    root.querySelector(".lightbox-close").focus({ preventScroll: true });
                },

                sync: function () {

                    if (root.hidden) {
                        return;
                    }

                    image.src = galleryUrls[galleryIndex] || galleryFallback;

                    root.querySelector(".lightbox-counter").textContent = (galleryIndex + 1) + " / " + galleryUrls.length;

                    root.classList.toggle("is-single", galleryUrls.length < 2);

                }

            };

        }

        setupGalleryControls();


        function renderThumbnails() {

            const thumbnails = document.getElementById("detailThumbnails");

            if (galleryUrls.length < 2) {
                thumbnails.hidden = true;
                thumbnails.innerHTML = "";
                return;
            }

            thumbnails.innerHTML = galleryUrls.map(function (url, index) {

                return `
                    <button
                        type="button"
                        class="detail-thumb ${index === galleryIndex ? "active" : ""}"
                        data-url="${escapeHtml(url)}"
                        aria-label="Ảnh ${index + 1}"
                    >
                        <img src="${escapeHtml(url)}" alt="" referrerpolicy="no-referrer">
                    </button>
                `;

            }).join("");

            thumbnails.hidden = false;

            thumbnails.scrollLeft = 0;

            thumbnails.querySelectorAll(".detail-thumb").forEach(function (button, index) {

                /* Ảnh phụ hỏng: bỏ khỏi bộ ảnh rồi dựng lại dải (còn dưới 2 ảnh thì ẩn cả dải) */
                button.querySelector("img").addEventListener("error", function () {

                    galleryUrls.splice(index, 1);

                    galleryIndex = Math.min(galleryIndex, Math.max(0, galleryUrls.length - 1));

                    renderThumbnails();

                    setMainImage(0);

                });

                button.addEventListener("click", function () {
                    goToImage(index, index > galleryIndex ? 1 : -1);
                });

            });

        }


        function showGallery(color, force) {

            const key = colorKey(color);

            if (!force && key === galleryColor) {
                return;
            }

            galleryColor = key;

            galleryUrls = imagesForColor(color).map(function (image) {
                return image.imageUrl;
            });

            galleryIndex = 0;

            renderThumbnails();

            setMainImage(0);

        }


        /* ================= PHIÊN BẢN (VARIANT) ================= */

        function getVariantLabel(variant) {

            return variantLabelOf(variant);

        }


        /*
         * Kế hoạch v5 GĐ2: 2 cột — trái "Phiên bản" (RAM + bộ nhớ), phải "Màu sắc" của phiên bản đang chọn.
         * Màu không thuộc phiên bản bị ẩn, màu hết hàng làm mờ (bấm vẫn xem được giá, nút mua bị khoá).
         * Chỉ khác nhau về màu → chỉ cột màu; không có màu → chỉ cột phiên bản.
         */

        let versionGroups = [];

        function renderVariants() {

            if (variants.length === 0) {

                selectVariant(null);

                return;

            }


            const hasColors = variants.some(function (variant) { return variant.color && String(variant.color).trim(); });

            versionGroups = groupVariantsByVersion(variants);

            versionGroups.forEach(function (group) {
                if (!group.label && !hasColors) {
                    group.label = getVariantLabel(group.variants[0]);
                }
            });

            const showVersions = versionGroups.some(function (group) { return group.label; });

            document.getElementById("versionColumn").hidden = !showVersions;

            document.getElementById("colorColumn").hidden = !hasColors;

            document.getElementById("variantPicker").classList.toggle("is-single", !showVersions || !hasColors);


            const container = document.getElementById("variantOptions");

            container.innerHTML = versionGroups.map(function (group, index) {

                const soldOut = group.variants.every(isSoldOut);

                return `
                    <button
                        type="button"
                        class="variant-btn${soldOut ? " is-sold-out" : ""}"
                        data-version-index="${index}"
                        aria-pressed="false"
                        ${soldOut ? 'title="Tạm hết hàng"' : ""}
                    >
                        <span class="variant-btn-name">${escapeHtml(group.label)}</span>
                    </button>
                `;

            }).join("");

            document.getElementById("detailVariants").hidden = false;


            container.querySelectorAll(".variant-btn").forEach(function (button) {

                button.addEventListener("click", function () {

                    const group = versionGroups[Number(button.dataset.versionIndex)];

                    /* giữ màu đang chọn nếu phiên bản mới có màu đó và còn hàng */
                    const sameColor = currentVariant && group.variants.find(function (variant) {
                        return foldVariantText(variant.color) === foldVariantText(currentVariant.color) && !isSoldOut(variant);
                    });

                    selectVariant(sameColor
                        || group.variants.find(function (variant) { return !isSoldOut(variant); })
                        || group.variants[0]);

                });

            });


            /* Mở trang ở phiên bản đầu tiên còn hàng (kế hoạch v3 GĐ E), theo thứ tự hiển thị */
            const ordered = versionGroups.reduce(function (all, group) { return all.concat(group.variants); }, []);

            selectVariant(ordered.find(function (variant) { return !isSoldOut(variant); }) || ordered[0]);

        }


        function renderColors(group) {

            const container = document.getElementById("colorOptions");

            container.innerHTML = (group ? group.variants : []).map(function (variant) {

                const name = String(variant.color || "").trim() || "Mặc định";

                const soldOut = isSoldOut(variant);

                const selected = currentVariant && currentVariant.id === variant.id;

                return `
                    <button
                        type="button"
                        class="color-btn${selected ? " active" : ""}${soldOut ? " is-sold-out" : ""}"
                        data-variant-id="${escapeHtml(variant.id)}"
                        aria-pressed="${Boolean(selected)}"
                        title="${escapeHtml(soldOut ? name + " — tạm hết hàng" : name)}"
                    >
                        ${variantColorDotHtml(name)}
                        <span>${escapeHtml(name)}</span>
                    </button>
                `;

            }).join("");

            container.querySelectorAll(".color-btn").forEach(function (button) {

                button.addEventListener("click", function () {

                    selectVariant(variants.find(function (variant) {
                        return String(variant.id) === button.dataset.variantId;
                    }));

                });

            });

            document.getElementById("selectedColorName").textContent =
                currentVariant && currentVariant.color ? ": " + currentVariant.color : "";

        }


        /* Giá của phiên bản đang chọn (không có phiên bản → giá sản phẩm) */

        function getPrices() {

            return resolvePrices(product, currentVariant);

        }


        function selectVariant(variant) {

            currentVariant = variant || null;

            setQuantity(1);

            showGallery(variant ? variant.color : null);


            const group = variant
                ? versionGroups.find(function (item) { return item.variants.indexOf(variant) !== -1; })
                : null;

            document.querySelectorAll(".variant-btn").forEach(function (button) {

                const on = Boolean(group) && versionGroups[Number(button.dataset.versionIndex)] === group;

                button.classList.toggle("active", on);

                button.setAttribute("aria-pressed", String(on));

            });

            renderColors(group);


            const prices = getPrices();

            const hasPrice = prices.price > 0;

            document.getElementById("detailPrice").textContent =
                hasPrice ? formatPrice(prices.price) : "Liên hệ";


            const oldPriceEl = document.getElementById("detailPriceOld");

            if (prices.oldPrice) {

                oldPriceEl.textContent = formatPrice(prices.oldPrice);

                oldPriceEl.hidden = false;

            } else {

                oldPriceEl.hidden = true;

            }


            /* Giá đến từ chương trình khuyến mãi → ghi tên chương trình (textContent: tên do admin nhập) */

            const promoEl = document.getElementById("detailPromo");

            promoEl.textContent = prices.promotionName ? "Khuyến mãi: " + prices.promotionName : "";

            promoEl.hidden = !prices.promotionName;


            /* Giá 0 (11 sản phẩm crawl) → không bán trực tuyến, mời liên hệ; hết hàng → khoá mua (v3 GĐ E) */

            const canBuy = canBuyCurrent();

            addToCartBtn.disabled = !canBuy;

            buyNowBtn.disabled = !canBuy;

            document.getElementById("qtyMinus").disabled = !canBuy;

            document.getElementById("qtyPlus").disabled = !canBuy;

            document.getElementById("contactNote").hidden = hasPrice;

            renderStock(hasPrice);

            renderStoreStock(currentVariant ? currentVariant.id : null);


            renderSpecs();

            refreshComboTotals();

        }


        /* Có giá và (nếu API trả số lượng) còn hàng ở chi nhánh đang mở */

        function canBuyCurrent() {

            return getPrices().price > 0 && !isSoldOut(currentVariant);

        }


        function availableUnits() {

            return currentVariant && Number.isInteger(currentVariant.availableQuantity)
                ? currentVariant.availableQuantity
                : null;

        }


        function renderStock(hasPrice) {

            const stockEl = document.getElementById("detailStock");

            const units = availableUnits();

            if (!hasPrice || units === null) {

                stockEl.hidden = true;

                return;

            }

            stockEl.hidden = false;

            stockEl.classList.toggle("is-out", units === 0);

            stockEl.textContent = units === 0
                ? "Tạm hết hàng ở tất cả chi nhánh"
                : units <= 5 ? "Chỉ còn " + units + " sản phẩm" : "Còn hàng";

        }


        /* ================= SỐ LƯỢNG ================= */

        function setQuantity(value) {

            quantity = value;

            qtyValue.textContent = String(value);

        }


        function setupQuantityControls() {

            document.getElementById("qtyMinus").addEventListener("click", function () {

                if (quantity > 1) {
                    setQuantity(quantity - 1);
                }

            });


            document.getElementById("qtyPlus").addEventListener("click", function () {

                const units = availableUnits();

                if (units !== null && quantity >= units) {

                    showToast("Chỉ còn " + units + " sản phẩm cho phiên bản này.", "error");

                } else if (quantity < MAX_QUANTITY) {

                    setQuantity(quantity + 1);

                } else {

                    showToast(
                        "Mỗi lần chỉ thêm tối đa " + MAX_QUANTITY + " sản phẩm.",
                        "error"
                    );

                }

            });

        }


        /* ================= THÊM VÀO GIỎ / MUA NGAY ================= */

        /*
         * Phase 3: giỏ hàng trên server, mỗi dòng là một variantId (cart-store.js),
         * nên cùng một phiên bản thêm từ trang danh sách hay trang chi tiết
         * luôn gộp chung một dòng. Giá do server tính.
         */

        async function addCurrentToCart() {

            const prices = getPrices();

            if (!product || prices.price <= 0 || isSoldOut(currentVariant)) {
                return false;
            }


            const mainImage = document.getElementById("mainImage");

            /* false khi chưa đăng nhập (addToCart đã chuyển tới trang đăng nhập) hoặc API báo lỗi */
            return addToCart(
                {
                    productId: product.id,
                    variantId: currentVariant ? currentVariant.id : null,
                    name: product.name,
                    variantLabel: currentVariant ? getVariantLabel(currentVariant) : "",
                    price: prices.price,
                    image: mainImage ? mainImage.currentSrc || mainImage.src : null
                },
                quantity,
                mainImage
            );

        }


        function setupActions() {

            addToCartBtn.addEventListener("click", function () {
                runCartAction(false);
            });


            buyNowBtn.addEventListener("click", function () {
                runCartAction(true);
            });

        }


        /* Khoá 2 nút trong lúc chờ API để không bấm trùng; "Mua ngay" xong thì sang giỏ hàng */

        async function runCartAction(goToCart) {

            addToCartBtn.disabled = true;

            buyNowBtn.disabled = true;


            let added = false;

            try {

                added = await addCurrentToCart();

            } finally {

                /* Người dùng có thể đã đổi sang phiên bản khác trong lúc chờ */
                const canBuy = canBuyCurrent();

                addToCartBtn.disabled = !canBuy;

                buyNowBtn.disabled = !canBuy;

            }


            if (added && goToCart) {
                window.location.href = siteUrl("customer/cart.html");
            }

        }


        /* ================= TABS ================= */

        function setupTabs() {

            const buttons = document.querySelectorAll(".tab-btn");

            buttons.forEach(function (button) {

                button.addEventListener("click", function () {

                    buttons.forEach(function (b) {
                        b.classList.toggle("active", b === button);
                        b.setAttribute("aria-selected", String(b === button));
                    });

                    document.querySelectorAll(".tab-panel").forEach(function (panel) {
                        panel.classList.toggle(
                            "active",
                            panel.dataset.tabPanel === button.dataset.tab
                        );
                    });

                });

            });

        }


        /* ================= THÔNG SỐ ================= */

        /*
         * Dữ liệu crawl chưa có bảng thông số riêng, nên bảng ghép từ các
         * trường thật của sản phẩm + phiên bản đang chọn, rồi nối thêm
         * product_specifications nếu có.
         */

        function renderSpecs() {

            const rows = [];

            function add(label, value) {

                if (value !== null && value !== undefined && String(value).trim() !== "") {
                    rows.push([label, String(value).trim()]);
                }

            }


            add("Thương hiệu", product.brandName);

            add("Danh mục", product.categoryName);

            add("Mã sản phẩm", (currentVariant && currentVariant.skuVariant) || product.sku);


            if (currentVariant) {

                add("Phiên bản", getVariantLabel(currentVariant));

                add("RAM", currentVariant.ram);

                add("Bộ nhớ trong", currentVariant.storage);

                add("Màu sắc", currentVariant.color);


                /* Thuộc tính khác của phiên bản (bỏ những cái đã hiện ở trên) */

                const shown = ["ram", "bộ nhớ trong", "màu sắc"];

                (currentVariant.attributeValues || []).forEach(function (value) {

                    const name = (value.attributeName || "").trim();

                    if (name && shown.indexOf(name.toLowerCase()) === -1) {
                        add(name, value.value);
                    }

                });

            }


            if (product.warrantyMonths > 0) {
                add("Bảo hành", product.warrantyMonths + " tháng");
            }

            if (product.weight) {
                add("Khối lượng", product.weight + " kg");
            }


            /* Thông số chung + thông số riêng của phiên bản đang chọn (dòng riêng ghi đè dòng chung cùng tên) */
            const specRows = [];

            specifications.forEach(function (spec) {

                if (spec.variantId && (!currentVariant || spec.variantId !== currentVariant.id)) {
                    return;
                }

                const key = String(spec.specName || '').trim().toLowerCase();

                const at = spec.variantId ? specRows.findIndex(function (row) { return row.key === key; }) : -1;

                if (at >= 0) {
                    specRows[at] = { key: key, spec: spec };
                } else {
                    specRows.push({ key: key, spec: spec });
                }

            });

            specRows.forEach(function (row) {
                add(row.spec.specName, row.spec.specValue);
            });


            renderSpecGroups(document.getElementById("specsTable"), rows);

            renderProductHighlights(rows);

        }


        /* ================= GỢI Ý AI: TƯƠNG TỰ / NÂNG CẤP / MUA KÈM ================= */

        /* Mỗi khu gọi POST /recommendations riêng; lỗi hoặc danh sách rỗng thì ẩn khu đó */

        function loadRelatedProducts() {

            try {
                localStorage.setItem(LAST_VIEWED_PRODUCT_KEY, String(product.id));
                rememberViewedProduct(product.id);
            } catch (error) {
                /* bỏ qua */
            }

            [
                ["SIMILAR", "relatedZone", "relatedProductsGrid"],
                ["UPSELL", "upsellZone", "upsellProductsGrid"],
                ["CROSS_SELL", "crossSellZone", "crossSellProductsGrid"]
            ].forEach(function (zone) {
                loadRecommendationZone(zone[0], zone[1], zone[2]);
            });

        }


        /* ================= MUA KÈM TIẾT KIỆM (kế hoạch v5 GĐ10) ================= */

        /*
         * GET /products/{id}/combos (công khai): mỗi combo là danh sách món kèm + giá sau giảm của từng phiên bản.
         * Khách tick món, chọn phiên bản, bấm "Thêm combo vào giỏ" → thêm sản phẩm chính (phiên bản đang chọn) và
         * các món đã tick, mỗi thứ 1 cái. Giảm giá do server tự áp khi giỏ đủ món (kể cả thêm riêng lẻ).
         */

        let combos = [];

        async function loadCombos() {

            try {
                combos = await apiRequest("/products/" + product.id + "/combos");
            } catch (error) {
                return;
            }

            if (!Array.isArray(combos) || combos.length === 0) {
                combos = [];
                return;
            }

            document.getElementById("comboList").innerHTML = combos.map(comboHtml).join("");

            document.getElementById("comboZone").hidden = false;

            bindComboEvents();

            refreshComboTotals();

        }


        function comboVariantLabel(variant) {

            return variant.label + (variant.color ? " · " + variant.color : "");

        }


        /* Phiên bản còn hàng trước; món hết sạch hàng vẫn hiện nhưng không tick được */
        function comboVariantsOf(item) {

            const buyable = item.variants.filter(function (variant) { return variant.availableQuantity > 0; });

            return buyable.length ? buyable : item.variants;

        }


        function comboHtml(combo, comboIndex) {

            const ends = combo.endsAt ? new Date(combo.endsAt) : null;

            const endsText = ends && !isNaN(ends) ? "Đến " + ends.toLocaleDateString("vi-VN") : "";

            const rows = combo.items.map(function (item, itemIndex) {

                const variants = comboVariantsOf(item);

                const inStock = variants.some(function (variant) { return variant.availableQuantity > 0; });

                const discountText = item.discountType === "PERCENT"
                    ? "−" + Number(item.discountValue).toLocaleString("vi-VN", { maximumFractionDigits: 2 }) + "%"
                    : "−" + formatPrice(Number(item.discountValue));

                const options = variants.map(function (variant, index) {
                    return `<option value="${index}">${escapeHtml(comboVariantLabel(variant))}</option>`;
                }).join("");

                const image = isSafeImageUrl(item.imageUrl)
                    ? `<img src="${escapeHtml(item.imageUrl.trim())}" alt="" loading="lazy" referrerpolicy="no-referrer">`
                    : `<span class="combo-noimg"></span>`;

                return `
                    <label class="combo-item" data-combo="${comboIndex}" data-item="${itemIndex}">
                        <input type="checkbox" ${inStock ? "checked" : "disabled"}>
                        ${image}
                        <span class="combo-item-info">
                            <a href="${escapeHtml(getProductDetailUrl(item.productId))}">${escapeHtml(item.name)}</a>
                            ${inStock ? "" : `<em>Hết hàng</em>`}
                            <select aria-label="Phiên bản của ${escapeHtml(item.name)}" ${variants.length > 1 ? "" : "hidden"}>${options}</select>
                        </span>
                        <span class="combo-item-price">
                            <strong data-combo-price></strong>
                            <s data-combo-old></s>
                            <small>${escapeHtml(discountText)}</small>
                        </span>
                    </label>
                `;

            }).join("");

            return `
                <div class="combo-card" data-combo-card="${comboIndex}">
                    <div class="combo-card-head">
                        <strong>${escapeHtml(combo.name)}</strong>
                        <span>${escapeHtml(endsText)}</span>
                    </div>
                    ${combo.description ? `<p class="combo-card-desc">${escapeHtml(combo.description)}</p>` : ""}
                    <div class="combo-items">${rows}</div>
                    <div class="combo-card-foot">
                        <div class="combo-card-total">
                            <span>Tổng cộng (kèm sản phẩm đang xem)</span>
                            <strong data-combo-total></strong>
                            <em data-combo-saving></em>
                        </div>
                        <button type="button" class="btn btn-dark" data-combo-add>Thêm combo vào giỏ</button>
                    </div>
                </div>
            `;

        }


        function comboChosenVariants(comboIndex) {

            const combo = combos[comboIndex];

            return Array.from(document.querySelectorAll('[data-combo="' + comboIndex + '"]')).map(function (row) {

                const item = combo.items[Number(row.dataset.item)];

                const checkbox = row.querySelector("input");

                return {
                    row: row,
                    variant: comboVariantsOf(item)[Number(row.querySelector("select").value) || 0],
                    checked: checkbox.checked && !checkbox.disabled
                };

            });

        }


        function refreshComboTotals() {

            combos.forEach(function (combo, comboIndex) {

                const card = document.querySelector('[data-combo-card="' + comboIndex + '"]');

                if (!card) {
                    return;
                }

                const mainPrice = getPrices().price;

                let total = mainPrice;

                let saving = 0;

                comboChosenVariants(comboIndex).forEach(function (entry) {

                    entry.row.querySelector("[data-combo-price]").textContent = formatPrice(Number(entry.variant.comboPrice));

                    entry.row.querySelector("[data-combo-old]").textContent = formatPrice(Number(entry.variant.price));

                    entry.row.classList.toggle("is-off", !entry.checked);

                    if (entry.checked) {
                        total += Number(entry.variant.comboPrice);
                        saving += Number(entry.variant.price) - Number(entry.variant.comboPrice);
                    }

                });

                card.querySelector("[data-combo-total]").textContent = mainPrice > 0 ? formatPrice(total) : "—";

                card.querySelector("[data-combo-saving]").textContent = saving > 0 ? "Tiết kiệm " + formatPrice(saving) : "";

                card.querySelector("[data-combo-add]").disabled = !canBuyCurrent();

            });

        }


        function bindComboEvents() {

            const list = document.getElementById("comboList");

            list.addEventListener("change", refreshComboTotals);

            list.addEventListener("click", function (event) {

                const button = event.target.closest("[data-combo-add]");

                if (button) {
                    addComboToCart(Number(button.closest("[data-combo-card]").dataset.comboCard), button);
                }

            });

        }


        async function addComboToCart(comboIndex, button) {

            if (!isLoggedIn()) {
                redirectToLogin("cart");
                return;
            }

            if (!canBuyCurrent() || !currentVariant) {
                showToast("Sản phẩm đang xem hiện chưa thể đặt mua.", "error");
                return;
            }

            const picked = comboChosenVariants(comboIndex).filter(function (entry) { return entry.checked; });

            const variantIds = [currentVariant.id].concat(picked.map(function (entry) { return entry.variant.id; }));

            button.disabled = true;

            try {

                let summary = null;

                for (const variantId of variantIds) {
                    summary = await addCartItem({ variantId: variantId }, 1);
                }

                setCartCount(summary.totalQuantity);

                bumpCartIcon();

                showToast("Đã thêm " + variantIds.length + " sản phẩm vào giỏ" +
                    (summary.comboDiscount > 0 ? " — giảm combo " + formatPrice(summary.comboDiscount) : "") + ".", "success");

            } catch (error) {

                if (!isLoggedIn()) {
                    redirectToLogin("cart");
                } else {
                    showToast(getErrorMessage(error), "error");
                }

            } finally {

                button.disabled = !canBuyCurrent();

            }

        }


        async function loadRecommendationZone(type, zoneId, gridId) {

            let result;

            try {
                result = await fetchRecommendations(type, product.id, RELATED_COUNT);
            } catch (error) {
                return;
            }

            if (!result || result.items.length === 0) {
                return;
            }

            document.getElementById(zoneId).hidden = false;

            renderRecommendationGrid(document.getElementById(gridId), result.items, categoryMaps.byId);

        }

    }
);
