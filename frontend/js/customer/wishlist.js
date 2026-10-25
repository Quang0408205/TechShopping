/* ================= TRANG SẢN PHẨM YÊU THÍCH ================= */

/*
 * customer/wishlist.html (cần đăng nhập): GET /wishlist → thẻ sản phẩm kèm giá hiện tại; nếu giá thấp hơn lúc khách lưu
 * thì hiện "Giảm X so với lúc bạn lưu". Bỏ yêu thích: DELETE /wishlist/{id} rồi gỡ thẻ ngay. Mọi dữ liệu escape khi
 * đưa vào HTML.
 */

document.addEventListener("DOMContentLoaded", async function () {

    if (!isLoggedIn()) {
        redirectToLogin();
        return;
    }

    if (typeof layoutReady !== "undefined") {
        await layoutReady;
    }

    const loading = document.getElementById("wishLoading");

    const errorBox = document.getElementById("wishError");

    const empty = document.getElementById("wishEmpty");

    const grid = document.getElementById("wishGrid");

    let count = 0;


    function show(which) {

        loading.hidden = which !== "loading";
        errorBox.hidden = which !== "error";
        empty.hidden = which !== "empty";
        grid.hidden = which !== "grid";

    }


    function cardHtml(item) {

        const image = isSafeImageUrl(item.imageUrl) ? item.imageUrl.trim() : "";

        const url = escapeHtml(getProductDetailUrl(item.productId));

        const cheaper = item.priceAtAdd != null && Number(item.price) < Number(item.priceAtAdd);

        const saved = cheaper ? Number(item.priceAtAdd) - Number(item.price) : 0;

        const percent = cheaper ? Math.round(saved * 100 / Number(item.priceAtAdd)) : 0;

        return `<article class="wish-card" data-product-id="${escapeHtml(item.productId)}">
            <a href="${url}" class="wish-card-image">
                ${image ? `<img src="${escapeHtml(image)}" alt="${escapeHtml(item.name)}" loading="lazy" referrerpolicy="no-referrer">` : ""}
                ${cheaper ? `<span class="wish-card-badge">-${percent}%</span>` : ""}
            </a>
            <div class="wish-card-body">
                <span class="wish-card-category">${escapeHtml((item.categoryName || "").toUpperCase())}</span>
                <h3><a href="${url}">${escapeHtml(item.name)}</a></h3>
                <p class="wish-card-price">
                    <strong>${Number(item.price) > 0 ? escapeHtml(formatPrice(item.price)) : "Liên hệ"}</strong>
                    ${item.originalPrice ? `<s>${escapeHtml(formatPrice(item.originalPrice))}</s>` : ""}
                </p>
                ${cheaper ? `<p class="wish-card-drop">Giảm ${escapeHtml(formatPrice(saved))} so với lúc bạn lưu</p>` : ""}
                <p class="wish-card-stock${item.inStock ? "" : " is-out"}">${item.inStock ? "Còn hàng" : "Tạm hết hàng"}</p>
                <div class="wish-card-actions">
                    <a href="${url}" class="btn btn-dark">XEM SẢN PHẨM</a>
                    <button type="button" class="wish-remove" data-remove="${escapeHtml(item.productId)}">Bỏ yêu thích</button>
                </div>
            </div>
        </article>`;

    }


    async function load() {

        show("loading");

        let items;

        try {
            items = await apiRequest("/wishlist", { auth: true }) || [];
        } catch (error) {
            if (isLoggedIn()) {
                show("error");
            }
            return;
        }

        count = items.length;

        if (count === 0) {
            show("empty");
            return;
        }

        grid.innerHTML = items.map(cardHtml).join("");

        show("grid");

    }


    grid.addEventListener("click", async function (event) {

        const remove = event.target.closest("[data-remove]");

        if (!remove) {
            return;
        }

        remove.disabled = true;

        try {

            await apiRequest("/wishlist/" + encodeURIComponent(remove.dataset.remove), { method: "DELETE", auth: true });

            wishIds.delete(String(remove.dataset.remove));

            syncWishUi();

            const card = remove.closest(".wish-card");

            card.classList.add("is-leaving");

            setTimeout(function () {

                card.remove();

                count--;

                if (count <= 0) {
                    show("empty");
                }

            }, prefersReducedMotion() ? 0 : 240);

        } catch (error) {

            remove.disabled = false;

            showToast("Không bỏ được sản phẩm, vui lòng thử lại.", "error");

        }

    });

    document.getElementById("wishRetry").addEventListener("click", load);

    load();

});
