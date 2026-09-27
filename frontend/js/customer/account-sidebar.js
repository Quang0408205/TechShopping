/* ================= THANH BÊN TRANG TÀI KHOẢN ================= */

/*
 * Lấy từ bản frontend mới. Trang đánh dấu mục đang mở bằng
 * <body data-account-page="profile|address|orders|wishlist|viewed">.
 * Tên / email lấy từ phiên đăng nhập (poy_auth) nên phải escape.
 * Mục "Đơn hàng" trỏ tới customer/orders.html (F2).
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        /* Toàn bộ trang tài khoản chỉ dành cho người đã đăng nhập */

        if (typeof layoutReady !== "undefined") {
            await layoutReady;
        }

        if (!isLoggedIn()) {

            redirectToLogin();

            return;

        }

        renderAccountSidebar();

    }
);


function renderAccountSidebar() {

    const root = document.getElementById("accountSidebarRoot");

    if (!root) {
        return;
    }


    const user = getCurrentUser() || {};

    const currentPage = document.body.dataset.accountPage || "";


    root.innerHTML = `

        <aside class="account-sidebar">

            <div class="account-sidebar-user">
                <strong>${escapeHtml(user.fullname || user.username || "Tài khoản")}</strong>
                <span>${escapeHtml(user.email || "")}</span>
            </div>

            <nav class="account-nav">

                <a href="${escapeHtml(siteUrl("customer/account.html"))}" data-account-page="profile">
                    Thông tin cá nhân
                </a>

                <a href="${escapeHtml(siteUrl("customer/account.html#address"))}" data-account-page="address">
                    Địa chỉ
                </a>

                <a href="${escapeHtml(siteUrl("customer/orders.html"))}" data-account-page="orders">
                    Đơn hàng
                </a>

                <a href="${escapeHtml(siteUrl("customer/account.html#wishlist"))}" data-account-page="wishlist">
                    Yêu thích
                </a>

                <a href="${escapeHtml(siteUrl("customer/account.html#viewed"))}" data-account-page="viewed">
                    Đã xem
                </a>

                <a href="#" id="sidebarLogoutBtn" data-account-page="logout" class="account-nav-logout">
                    Đăng xuất
                </a>

            </nav>

        </aside>

    `;


    root.querySelectorAll("[data-account-page]").forEach(function (link) {

        link.classList.toggle("active", link.dataset.accountPage === currentPage);

    });


    document.getElementById("sidebarLogoutBtn")
        .addEventListener("click", async function (event) {

            event.preventDefault();

            await logout();

            window.location.href = siteUrl("index.html");

        });

}
