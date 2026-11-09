/*
 * Trang "Sở thích mua sắm": bảng hỏi để gợi ý cho khách chưa xem / mua gì (thay cho danh sách phổ biến).
 * Câu 1 (loại sản phẩm) bắt buộc; tầm giá, hãng, nhu cầu có thể bỏ trống. Lựa chọn lấy từ
 * GET /recommendations/preference-options (danh mục đang bán, hãng có hàng, nhu cầu có sản phẩm).
 * Đã đăng nhập: đọc / lưu GET, PUT /users/me/preferences. Chưa đăng nhập: lưu ở trình duyệt (api.js), gửi kèm khi
 * lấy gợi ý và chép vào tài khoản khi đăng nhập. Lưu xong → trang ?redirect (mặc định trang Khuyến nghị).
 */

const PRICE_LEVEL_CHOICES = [
    { value: "LOW", label: "Phổ thông" },
    { value: "MID", label: "Tầm trung" },
    { value: "HIGH", label: "Cao cấp" }
];

/* Hãng của các loại sản phẩm đã chọn: hiện trước các hãng nhiều sản phẩm nhất, còn lại sau nút "Xem thêm" */
const BRANDS_SHOWN = 12;


document.addEventListener(
    "DOMContentLoaded",
    function () {

        const form = document.getElementById("preferenceForm");

        const errorBox = document.getElementById("preferenceError");

        const submitButton = document.getElementById("preferenceSubmit");

        const chosen = {
            categoryIds: new Set(),
            priceLevels: new Set(),
            brandIds: new Set(),
            needIds: new Set()
        };

        let options = [];

        let showAllBrands = false;


        load();


        async function load() {

            const categoryBox = document.getElementById("categoryOptions");

            categoryBox.innerHTML = '<p class="pref-hint">Đang tải...</p>';

            try {

                const [choices, saved] = await Promise.all([
                    apiRequest("/recommendations/preference-options"),
                    isLoggedIn()
                        ? apiRequest("/users/me/preferences", { auth: true })
                        : Promise.resolve(getGuestPreferences())
                ]);

                options = choices || [];

                if (saved) {
                    (saved.categoryIds || []).forEach(function (id) { chosen.categoryIds.add(Number(id)); });
                    (saved.priceLevels || []).forEach(function (level) { chosen.priceLevels.add(level); });
                    (saved.brandIds || []).forEach(function (id) { chosen.brandIds.add(Number(id)); });
                    (saved.needIds || []).forEach(function (id) { chosen.needIds.add(Number(id)); });
                }

                categoryBox.removeAttribute("aria-busy");

                renderAll();

            } catch (error) {

                categoryBox.innerHTML = errorStateHtml(getErrorMessage(error), "preferenceRetryBtn");

                document.getElementById("preferenceRetryBtn").addEventListener("click", load);

            }

        }


        function renderAll() {

            document.getElementById("categoryOptions").innerHTML = options
                .map(function (category) {
                    return chipHtml("categoryIds", category.id, category.name, chosen.categoryIds.has(category.id));
                })
                .join("");

            document.getElementById("priceOptions").innerHTML = PRICE_LEVEL_CHOICES
                .map(function (level) {
                    return chipHtml("priceLevels", level.value, level.label, chosen.priceLevels.has(level.value));
                })
                .join("");

            renderBrands();

            renderNeeds();

        }


        function chosenCategories() {

            return options.filter(function (category) { return chosen.categoryIds.has(category.id); });

        }


        /* Hãng của các loại đã chọn, gộp số sản phẩm của cùng một hãng ở nhiều loại */

        function renderBrands() {

            const box = document.getElementById("brandOptions");

            const categories = chosenCategories();

            if (!categories.length) {
                box.innerHTML = '<p class="pref-hint">Chọn loại sản phẩm ở câu 1 để xem các hãng.</p>';
                return;
            }

            const byId = new Map();

            categories.forEach(function (category) {
                category.brands.forEach(function (brand) {
                    const entry = byId.get(brand.id) || { id: brand.id, name: brand.name, productCount: 0 };
                    entry.productCount += brand.productCount;
                    byId.set(brand.id, entry);
                });
            });

            const brands = Array.from(byId.values()).sort(function (a, b) {
                return b.productCount - a.productCount || a.name.localeCompare(b.name, "vi");
            });

            /* hãng đã chọn luôn hiện, kể cả khi nằm ngoài nhóm đầu */
            const shown = showAllBrands
                ? brands
                : brands.filter(function (brand, index) {
                    return index < BRANDS_SHOWN || chosen.brandIds.has(brand.id);
                });

            const hidden = brands.length - shown.length;

            box.innerHTML =
                '<div class="pref-chips">' +
                shown.map(function (brand) {
                    return chipHtml("brandIds", brand.id, brand.name, chosen.brandIds.has(brand.id));
                }).join("") +
                "</div>" +
                (hidden > 0
                    ? '<button type="button" class="pref-more" id="moreBrandsBtn">Xem thêm ' + hidden + " hãng</button>"
                    : "");

            const more = document.getElementById("moreBrandsBtn");

            if (more) {
                more.addEventListener("click", function () {
                    showAllBrands = true;
                    renderBrands();
                });
            }

        }


        /* Nhu cầu theo từng loại đã chọn (chỉ điện thoại, laptop, máy tính bảng có nhu cầu) */

        function renderNeeds() {

            const step = document.getElementById("needStep");

            const groups = chosenCategories().filter(function (category) { return category.needs.length; });

            step.hidden = groups.length === 0;

            document.getElementById("needOptions").innerHTML = groups
                .map(function (category) {
                    return '<div class="pref-group">' +
                        '<p class="pref-group-title">' + escapeHtml(category.name) + "</p>" +
                        '<div class="pref-chips">' +
                        category.needs.map(function (need) {
                            return chipHtml("needIds", need.id, need.name, chosen.needIds.has(need.id));
                        }).join("") +
                        "</div></div>";
                })
                .join("");

        }


        form.addEventListener("change", function (event) {

            const input = event.target;

            if (!input.matches('input[type="checkbox"]') || !chosen[input.name]) {
                return;
            }

            const value = input.name === "priceLevels" ? input.value : Number(input.value);

            if (input.checked) {
                chosen[input.name].add(value);
            } else {
                chosen[input.name].delete(value);
            }

            if (input.name === "categoryIds") {
                errorBox.hidden = true;
                renderBrands();
                renderNeeds();
            }

        });


        form.addEventListener("submit", async function (event) {

            event.preventDefault();

            errorBox.hidden = true;

            const answers = collectAnswers();

            if (!answers.categoryIds.length) {
                errorBox.textContent = "Vui lòng chọn ít nhất một loại sản phẩm ở câu 1.";
                errorBox.hidden = false;
                document.getElementById("categoryOptions").scrollIntoView({ behavior: "smooth", block: "center" });
                return;
            }

            submitButton.disabled = true;

            try {

                if (isLoggedIn()) {
                    await apiRequest("/users/me/preferences", { method: "PUT", auth: true, body: answers });
                    clearGuestPreferences();
                } else {
                    saveGuestPreferences(answers);
                }

                window.location.href = getRedirectTarget("customer/recommendation.html");

            } catch (error) {

                errorBox.textContent = getErrorMessage(error);
                errorBox.hidden = false;
                submitButton.disabled = false;

            }

        });


        /* Chỉ gửi hãng / nhu cầu thuộc các loại đang chọn (bỏ loại thì bỏ luôn lựa chọn con của nó) */

        function collectAnswers() {

            const categories = chosenCategories();

            const brandIds = new Set();

            const needIds = new Set();

            categories.forEach(function (category) {
                category.brands.forEach(function (brand) { brandIds.add(brand.id); });
                category.needs.forEach(function (need) { needIds.add(need.id); });
            });

            return {
                categoryIds: categories.map(function (category) { return category.id; }),
                priceLevels: PRICE_LEVEL_CHOICES
                    .map(function (level) { return level.value; })
                    .filter(function (level) { return chosen.priceLevels.has(level); }),
                brandIds: Array.from(chosen.brandIds).filter(function (id) { return brandIds.has(id); }),
                needIds: Array.from(chosen.needIds).filter(function (id) { return needIds.has(id); })
            };

        }

    }
);


function chipHtml(name, value, label, checked) {

    return '<label class="pref-chip">' +
        '<input type="checkbox" name="' + name + '" value="' + escapeHtml(String(value)) + '"' +
        (checked ? " checked" : "") + ">" +
        "<span>" + escapeHtml(label) + "</span>" +
        "</label>";

}
