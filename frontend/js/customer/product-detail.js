/* ================= TRANG CHI TIẾT SẢN PHẨM (Checkpoint A2) ================= */

/*
 * customer/product-detail.html?id=<productId>. Dữ liệu thật, công khai
 * (KHÔNG gửi token):
 *   GET /products/{id}                 → thông tin chính (404 → "không tìm thấy")
 *   GET /products/{id}/variants        → phiên bản (giá, màu, RAM, bộ nhớ)
 *   GET /products/{id}/images          → ảnh (ảnh chính trước)
 *   GET /products/{id}/specifications  → thông số (nếu có)
 *   GET /products?categoryId=&brandId= → "Sản phẩm tương tự"
 *
 * Dựa trên trang chi tiết của bản frontend tham chiếu, nhưng bỏ phần đánh
 * giá (mọi sản phẩm đều có rating 0, không bịa phân bố sao), bỏ các vùng
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

            const filterKey = getCategoryFilterKey(categorySlug);

            link.textContent = product.categoryName;

            link.href = siteUrl(
                filterKey
                    ? "customer/products.html?category=" + encodeURIComponent(filterKey)
                    : "customer/products.html"
            );

            link.hidden = false;

            document.getElementById("breadcrumbCategorySep").hidden = false;

        }


        /* Thương hiệu · Mã · (đánh giá chỉ khi đã có lượt đánh giá thật) */

        function buildMetaText() {

            const parts = [];

            if (product.brandName) {
                parts.push("Thương hiệu: " + product.brandName);
            }

            if (product.sku) {
                parts.push("Mã: " + product.sku);
            }

            if (product.totalReviews > 0 && Number(product.rating) > 0) {
                parts.push(
                    "★ " + Number(product.rating).toFixed(1) +
                    " (" + product.totalReviews + " đánh giá)"
                );
            }

            return parts.join(" · ");

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
                        <img src="${escapeHtml(url)}" alt="">
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
                        class="variant-btn"
                        data-variant-id="${escapeHtml(variant.id)}"
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


            selectVariant(variants[0]);

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


            /* Giá 0 (11 sản phẩm crawl) → không bán trực tuyến, mời liên hệ */

            addToCartBtn.disabled = !hasPrice;

            buyNowBtn.disabled = !hasPrice;

            document.getElementById("qtyMinus").disabled = !hasPrice;

            document.getElementById("qtyPlus").disabled = !hasPrice;

            document.getElementById("contactNote").hidden = hasPrice;


            renderSpecs();

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

                if (quantity < MAX_QUANTITY) {

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

            if (!product || prices.price <= 0) {
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
                const hasPrice = getPrices().price > 0;

                addToCartBtn.disabled = !hasPrice;

                buyNowBtn.disabled = !hasPrice;

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


        /* ================= SẢN PHẨM TƯƠNG TỰ ================= */

        /* Ưu tiên cùng danh mục + cùng hãng, thiếu thì bù bằng cùng danh mục */

        async function loadRelatedProducts() {

            const zone = document.getElementById("relatedZone");

            const grid = document.getElementById("relatedProductsGrid");

            const related = [];


            function collect(page) {

                (page.content || []).forEach(function (item) {

                    const duplicate = related.some(function (r) {
                        return r.id === item.id;
                    });

                    if (item.id !== product.id && !duplicate && related.length < RELATED_COUNT) {
                        related.push(item);
                    }

                });

            }


            const base = "/products?isActive=true&sort=id&page=0&categoryId=" + product.categoryId;

            try {

                if (product.brandId) {
                    collect(await apiRequest(base + "&brandId=" + product.brandId + "&size=" + (RELATED_COUNT + 1)));
                }

                if (related.length < RELATED_COUNT) {
                    collect(await apiRequest(base + "&size=" + (RELATED_COUNT * 2 + 1)));
                }

            } catch (error) {
                /* Không có gợi ý cũng không sao: ẩn khu vực này */
            }


            if (related.length === 0) {
                return;
            }

            zone.hidden = false;

            renderProductGrid(grid, related, categoryMaps.byId);

        }

    }
);
