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

            renderProduct(images);

            loadRelatedProducts();

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


        /* Đánh giá thật trên web; chưa có thì điểm trên Thế Giới Di Động (ghi rõ nguồn); không có → ẩn */

        function renderRatingLink() {

            const link = document.getElementById("detailRating");

            const rating = productRatingSummary(product);

            link.hidden = !rating;

            if (rating) {
                link.textContent = "★ " + rating.rating.toFixed(1) +
                    " (" + rating.count.toLocaleString("vi-VN") + " đánh giá" +
                    (rating.source === "tgdd" ? " trên Thế Giới Di Động" : "") + ")";
            }

        }


        /* ================= ẢNH ================= */

        function renderImages(images, categorySlug) {

            const mainImage = document.getElementById("mainImage");

            const fallback = getFallbackImage(categorySlug);


            const urls = images
                .filter(function (image) {
                    return isSafeImageUrl(image.imageUrl);
                })
                .sort(function (a, b) {
                    if (a.isPrimary !== b.isPrimary) {
                        return a.isPrimary ? -1 : 1;
                    }
                    return (a.displayOrder || 0) - (b.displayOrder || 0);
                })
                .map(function (image) {
                    return image.imageUrl;
                });


            mainImage.alt = product.name;
            // The product CDN returns 403 when the storefront URL is sent as Referer.
            mainImage.referrerPolicy = "no-referrer";

            mainImage.addEventListener("error", function () {

                if (mainImage.src !== fallback) {
                    mainImage.src = fallback;
                }

            });

            mainImage.src = urls[0] || fallback;


            if (urls.length < 2) {
                return;
            }


            const thumbnails = document.getElementById("detailThumbnails");

            thumbnails.innerHTML = urls.map(function (url, index) {

                return `
                    <button
                        type="button"
                        class="detail-thumb ${index === 0 ? "active" : ""}"
                        data-url="${escapeHtml(url)}"
                        aria-label="Ảnh ${index + 1}"
                    >
                        <img src="${escapeHtml(url)}" alt="" referrerpolicy="no-referrer">
                    </button>
                `;

            }).join("");

            thumbnails.hidden = false;


            thumbnails.querySelectorAll(".detail-thumb").forEach(function (button) {

                /* Ảnh phụ hỏng: bỏ khỏi dải; còn dưới 2 ảnh thì ẩn cả dải */
                button.querySelector("img").addEventListener("error", function () {

                    button.remove();

                    if (thumbnails.querySelectorAll(".detail-thumb").length < 2) {
                        thumbnails.hidden = true;
                    }

                });

                button.addEventListener("click", function () {

                    mainImage.src = button.dataset.url;

                    thumbnails.querySelectorAll(".detail-thumb").forEach(function (b) {
                        b.classList.toggle("active", b === button);
                    });

                });

            });

        }


        /* ================= PHIÊN BẢN (VARIANT) ================= */

        function getVariantLabel(variant) {

            return variantLabelOf(variant);

        }


        function renderVariants() {

            if (variants.length === 0) {

                selectVariant(null);

                return;

            }


            const container = document.getElementById("variantOptions");

            container.innerHTML = variants.map(function (variant) {

                return `
                    <button
                        type="button"
                        class="variant-btn${isSoldOut(variant) ? " is-sold-out" : ""}"
                        data-variant-id="${escapeHtml(variant.id)}"
                        ${isSoldOut(variant) ? 'title="Tạm hết hàng"' : ""}
                    >
                        ${escapeHtml(getVariantLabel(variant))}
                    </button>
                `;

            }).join("");

            document.getElementById("detailVariants").hidden = false;


            container.querySelectorAll(".variant-btn").forEach(function (button) {

                button.addEventListener("click", function () {

                    const variant = variants.find(function (v) {
                        return String(v.id) === button.dataset.variantId;
                    });

                    selectVariant(variant);

                });

            });


            /* Mở trang ở phiên bản đầu tiên còn hàng (kế hoạch v3 GĐ E) */
            selectVariant(variants.find(function (variant) { return !isSoldOut(variant); }) || variants[0]);

        }


        /* Giá của phiên bản đang chọn (không có phiên bản → giá sản phẩm) */

        function getPrices() {

            return resolvePrices(product, currentVariant);

        }


        function selectVariant(variant) {

            currentVariant = variant || null;

            setQuantity(1);


            document.querySelectorAll(".variant-btn").forEach(function (button) {
                button.classList.toggle(
                    "active",
                    Boolean(variant) && button.dataset.variantId === String(variant.id)
                );
            });


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


            renderSpecs();

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


            specifications.forEach(function (spec) {
                add(spec.specName, spec.specValue);
            });


            document.getElementById("specsTable").innerHTML = rows.map(function (row) {

                return `
                    <tr>
                        <th scope="row">${escapeHtml(row[0])}</th>
                        <td>${escapeHtml(row[1])}</td>
                    </tr>
                `;

            }).join("");

        }


        /* ================= GỢI Ý AI: TƯƠNG TỰ / NÂNG CẤP / MUA KÈM ================= */

        /* Mỗi khu gọi POST /recommendations riêng; lỗi hoặc danh sách rỗng thì ẩn khu đó */

        function loadRelatedProducts() {

            try {
                localStorage.setItem(LAST_VIEWED_PRODUCT_KEY, String(product.id));
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
