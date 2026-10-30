/* ================= YÊU THÍCH + SO SÁNH SẢN PHẨM (dùng chung mọi trang khách hàng) ================= */

/*
 *.
 * - Yêu thích: lưu trên server theo tài khoản (GET /wishlist/ids, POST / DELETE /wishlist/{id}) để server gửi email khi
 *   giá giảm. Chưa đăng nhập bấm tim → sang trang đăng nhập rồi quay lại. Số trên biểu tượng tim ở đầu trang = số đã lưu.
 * - So sánh: lưu trên trình duyệt (localStorage poy_compare), tối đa 4 sản phẩm CÙNG danh mục (thông số mới so được với
 *   nhau); thanh nổi ở đáy màn hình hiện ảnh các sản phẩm đã chọn + nút "So sánh ngay" (customer/compare.html?ids=…).
 * Nút nằm trong thẻ sản phẩm / trang chi tiết: [data-wish] và [data-compare] kèm data-product-id…; thẻ vẽ sau (lọc, tải
 * thêm) được cập nhật trạng thái nhờ MutationObserver. Dữ liệu hiển thị đều escape.
 */

const COMPARE_KEY = "poy_compare";

const COMPARE_MAX = 4;

const wishIds = new Set();

let wishLoaded = false;


/* ---------- So sánh: lưu trong trình duyệt ---------- */

function readCompareList() {

    try {

        const list = JSON.parse(localStorage.getItem(COMPARE_KEY) || "[]");

        return Array.isArray(list)
            ? list.filter(function (item) { return item && /^\d{1,18}$/.test(String(item.id)); }).slice(0, COMPARE_MAX)
            : [];

    } catch (error) {

        return [];

    }

}


function saveCompareList(list) {

    try {
        localStorage.setItem(COMPARE_KEY, JSON.stringify(list));
    } catch (error) {
        /* không lưu được: bỏ qua, thanh so sánh chỉ mất khi tải lại */
    }

}


function toggleCompare(button) {

    const id = String(button.dataset.productId);

    const list = readCompareList();

    const at = list.findIndex(function (item) { return String(item.id) === id; });

    if (at !== -1) {

        list.splice(at, 1);

    } else {

        if (list.length >= COMPARE_MAX) {
            showToast("Chỉ so sánh tối đa " + COMPARE_MAX + " sản phẩm cùng lúc.", "error");
            return;
        }

        if (list.length > 0 && String(list[0].categoryId) !== String(button.dataset.categoryId)) {
            showToast("Chỉ so sánh được các sản phẩm cùng danh mục. Hãy xoá bớt hoặc bắt đầu lại.", "error");
            return;
        }

        list.push({
            id: id,
            name: button.dataset.name || "",
            image: button.dataset.image || "",
            categoryId: button.dataset.categoryId || ""
        });

    }

    saveCompareList(list);

    syncCompareUi();

}


function removeFromCompare(id) {

    saveCompareList(readCompareList().filter(function (item) { return String(item.id) !== String(id); }));

    syncCompareUi();

}


function compareBarElement() {

    let bar = document.getElementById("compareBar");

    if (!bar) {

        bar = document.createElement("div");

        bar.id = "compareBar";

        bar.className = "compare-bar";

        bar.hidden = true;

        bar.setAttribute("role", "region");

        bar.setAttribute("aria-label", "So sánh sản phẩm");

        bar.addEventListener("click", function (event) {

            const remove = event.target.closest("[data-compare-remove]");

            if (remove) {
                removeFromCompare(remove.dataset.compareRemove);
                return;
            }

            if (event.target.closest("[data-compare-clear]")) {
                saveCompareList([]);
                syncCompareUi();
            }

        });

        document.body.appendChild(bar);

    }

    return bar;

}


function syncCompareUi() {

    const list = readCompareList();

    const ids = list.map(function (item) { return String(item.id); });

    document.querySelectorAll("[data-compare]").forEach(function (button) {

        const on = ids.indexOf(String(button.dataset.productId)) !== -1;

        button.classList.toggle("is-on", on);

        button.setAttribute("aria-pressed", String(on));

    });

    const bar = compareBarElement();

    if (list.length === 0) {
        bar.hidden = true;
        document.body.classList.remove("has-compare-bar");
        return;
    }

    /* cùng danh sách thì giữ nguyên HTML (không kích hoạt lại MutationObserver) */
    const signature = JSON.stringify(list);

    if (bar.dataset.signature === signature) {
        bar.hidden = false;
        document.body.classList.add("has-compare-bar");
        return;
    }

    bar.dataset.signature = signature;

    const href = siteUrl("customer/compare.html?ids=" + ids.join(","));

    const slots = [];

    for (let index = 0; index < COMPARE_MAX; index++) {

        const item = list[index];

        slots.push(item
            ? `<span class="compare-slot is-filled" title="${escapeHtml(item.name)}">
                    ${isSafeImageUrl(item.image) ? `<img src="${escapeHtml(item.image)}" alt="" referrerpolicy="no-referrer">` : ""}
                    <button type="button" data-compare-remove="${escapeHtml(item.id)}" aria-label="${escapeHtml("Bỏ " + item.name + " khỏi so sánh")}">×</button>
                </span>`
            : `<span class="compare-slot" aria-hidden="true">+</span>`);

    }

    bar.innerHTML = `
        <div class="container compare-bar-inner">
            <div class="compare-slots">${slots.join("")}</div>
            <p class="compare-hint">${list.length < 2 ? "Chọn thêm ít nhất 1 sản phẩm cùng danh mục để so sánh" : "Đã chọn " + list.length + " / " + COMPARE_MAX + " sản phẩm"}</p>
            <button type="button" class="compare-clear" data-compare-clear>Xoá hết</button>
            <a href="${escapeHtml(href)}" class="btn btn-dark compare-go${list.length < 2 ? " is-disabled" : ""}"${list.length < 2 ? ' aria-disabled="true" tabindex="-1"' : ""}>So sánh ngay</a>
        </div>
    `;

    bar.hidden = false;

    document.body.classList.add("has-compare-bar");

}


/* ---------- Yêu thích: lưu trên server ---------- */

function syncWishUi() {

    document.querySelectorAll("[data-wish]").forEach(function (button) {

        const on = wishIds.has(String(button.dataset.productId));

        button.classList.toggle("is-on", on);

        button.setAttribute("aria-pressed", String(on));

        button.setAttribute("title", on ? "Bỏ khỏi yêu thích" : "Thêm vào yêu thích");

    });

    /* chỉ ghi khi đổi: MutationObserver bên dưới theo dõi cả trang, ghi lặp sẽ tự kích hoạt lại */
    document.querySelectorAll(".wishlist-count").forEach(function (badge) {

        const text = String(wishIds.size);

        if (badge.textContent !== text) {
            badge.textContent = text;
        }

        badge.hidden = wishIds.size === 0;

    });

}


async function loadWishIds() {

    if (!isLoggedIn()) {
        wishIds.clear();
        wishLoaded = true;
        syncWishUi();
        return;
    }

    try {

        (await apiRequest("/wishlist/ids", { auth: true }) || []).forEach(function (id) {
            wishIds.add(String(id));
        });

    } catch (error) {
        /* lỗi mạng / phiên hết hạn: trái tim hiện như chưa lưu */
    }

    wishLoaded = true;

    syncWishUi();

}


async function toggleWish(button) {

    if (!isLoggedIn()) {
        showToast("Vui lòng đăng nhập để lưu sản phẩm yêu thích.", "info");
        setTimeout(function () { redirectToLogin(); }, 700);
        return;
    }

    const id = String(button.dataset.productId);

    const adding = !wishIds.has(id);

    button.disabled = true;

    try {

        await apiRequest("/wishlist/" + encodeURIComponent(id), { method: adding ? "POST" : "DELETE", auth: true });

        if (adding) {
            wishIds.add(id);
            showToast("Đã thêm vào yêu thích. POY sẽ gửi email khi sản phẩm giảm giá.", "success");
        } else {
            wishIds.delete(id);
            showToast("Đã bỏ khỏi yêu thích.", "success");
        }

        syncWishUi();

    } catch (error) {

        if (error && error.status === 404) {
            showToast("Sản phẩm này không còn được bán.", "error");
        } else if (!error || error.status !== 401) {
            showToast("Không cập nhật được danh sách yêu thích, vui lòng thử lại.", "error");
        }

    } finally {

        button.disabled = false;

    }

}


/* ---------- Khởi tạo ---------- */

document.addEventListener("click", function (event) {

    const wish = event.target.closest("[data-wish]");

    if (wish) {
        event.preventDefault();
        toggleWish(wish);
        return;
    }

    const compare = event.target.closest("[data-compare]");

    if (compare) {
        event.preventDefault();
        toggleCompare(compare);
        return;
    }

    const go = event.target.closest(".compare-go.is-disabled");

    if (go) {
        event.preventDefault();
    }

});


document.addEventListener("DOMContentLoaded", async function () {

    syncCompareUi();

    if (typeof layoutReady !== "undefined") {
        await layoutReady;
    }

    await loadWishIds();

    /* thẻ vẽ sau (lọc, phân trang, khối trang chủ): cập nhật trạng thái nút, gom lại mỗi khung hình */
    let queued = false;

    new MutationObserver(function () {

        if (queued) {
            return;
        }

        queued = true;

        requestAnimationFrame(function () {
            queued = false;
            syncCompareUi();
            syncWishUi();
        });

    }).observe(document.body, { childList: true, subtree: true });

});


/* Các tab khác đổi danh sách so sánh → thanh này cập nhật theo */
window.addEventListener("storage", function (event) {

    if (event.key === COMPARE_KEY) {
        syncCompareUi();
    }

});
