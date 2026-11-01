/* ================= HỆ THỐNG CỬA HÀNG (customer/stores.html) ================= */

/* API công khai: GET /stores (chi nhánh đang mở: tên, địa chỉ, quận, tỉnh, điện thoại). */

document.addEventListener("DOMContentLoaded", async function () {

    const grid = document.getElementById("storeGrid");
    const keyword = document.getElementById("storeKeyword");
    const citySelect = document.getElementById("storeCity");
    const count = document.getElementById("storeCount");

    let stores = [];

    grid.innerHTML = '<p class="store-empty">Đang tải danh sách cửa hàng…</p>';

    try {

        stores = await apiRequest("/stores") || [];

    } catch (error) {

        grid.innerHTML = `<p class="store-empty">${escapeHtml(getErrorMessage(error))}</p>`;

        return;

    }

    const cities = Array.from(new Set(stores.map(function (store) { return store.city; }).filter(Boolean))).sort(function (a, b) {
        return a.localeCompare(b, "vi");
    });

    citySelect.innerHTML = '<option value="">Mọi tỉnh / thành</option>' + cities.map(function (city) {
        return `<option value="${escapeHtml(city)}">${escapeHtml(city)}</option>`;
    }).join("");

    document.getElementById("storeFilter").addEventListener("submit", function (event) { event.preventDefault(); });

    keyword.addEventListener("input", render);

    citySelect.addEventListener("change", render);

    render();


    function render() {

        const needle = fold(keyword.value);

        const city = citySelect.value;

        const shown = stores.filter(function (store) {

            if (city && store.city !== city) {
                return false;
            }

            return !needle || fold([store.name, store.address, store.district, store.city].join(" ")).includes(needle);

        });

        count.textContent = shown.length + " / " + stores.length + " cửa hàng";

        grid.innerHTML = shown.length === 0
            ? '<p class="store-empty">Không có cửa hàng nào khớp tìm kiếm.</p>'
            : shown.map(cardHtml).join("");

    }


    function cardHtml(store) {

        const place = [store.address, store.district, store.city].filter(Boolean).join(", ");

        const map = "https://www.google.com/maps/search/?api=1&query=" + encodeURIComponent(store.name + ", " + place);

        const phone = store.phone ? String(store.phone).replace(/[^0-9+]/g, "") : "";

        return `
            <article class="store-card">
                <h2>${escapeHtml(store.name)}</h2>
                <p class="store-address">${escapeHtml(place)}</p>
                <div class="store-actions">
                    ${phone ? `<a class="btn btn-outline-dark" href="tel:${escapeHtml(phone)}">Gọi ${escapeHtml(store.phone)}</a>` : ""}
                    <a class="btn btn-dark" href="${escapeHtml(map)}" target="_blank" rel="noopener noreferrer">Chỉ đường</a>
                </div>
            </article>
        `;

    }


    /* bỏ dấu + chữ thường để tìm "quan 1" ra "Quận 1" */
    function fold(text) {

        return String(text || "").normalize("NFD").replace(/[̀-ͯ]/g, "").replace(/đ/g, "d").replace(/Đ/g, "D").toLowerCase();

    }

});
