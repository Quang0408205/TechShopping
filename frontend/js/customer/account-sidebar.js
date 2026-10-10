/* ================= THANH BÊN TRANG TÀI KHOẢN ================= */

/*
 * Lấy từ bản frontend mới. Trang đánh dấu mục đang mở bằng
 * <body data-account-page="profile|security|address|orders|service-requests"> (trang Tài khoản đổi giá trị theo
 * hash #profile / #security / #address, xem account.js). "Yêu thích" / "Đã xem" bỏ khỏi menu tới khi có API.
 * Tên / email lấy từ phiên đăng nhập (poy_auth) nên phải escape.
 * Mục "Đơn hàng" trỏ tới customer/orders.html (F2).
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        /*
         * Toàn bộ trang tài khoản chỉ dành cho người đã đăng nhập. Kiểm tra
         * NGAY lúc tải trang: nếu đợi layout xong mới kiểm tra, account.js có
         * thể đã xoá phiên (tài khoản bị khoá → 403) và trang sẽ bị chuyển đi
         * thay vì hiện thông báo "Tài khoản đã bị khóa".
         */

        if (!isLoggedIn()) {

            redirectToLogin();

            return;

        }


        if (typeof layoutReady !== "undefined") {
            await layoutReady;
        }

        /* Phiên bị xoá trong lúc chờ: trang tự hiển thị lý do, không vẽ thanh bên */
        if (!isLoggedIn()) {
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
                ${userAvatarHtml(user, "user-avatar")}
                <span class="account-sidebar-text">
                    <strong>${escapeHtml(user.fullname || user.username || "Tài khoản")}</strong>
                    <span>${escapeHtml(user.email || "")}</span>
                </span>
            </div>

            <nav class="account-nav">

                <a href="${escapeHtml(siteUrl("customer/account.html#profile"))}" data-account-page="profile">
                    Hồ sơ
                </a>

                <a href="${escapeHtml(siteUrl("customer/account.html#security"))}" data-account-page="security">
                    Bảo mật
                </a>

                <a href="${escapeHtml(siteUrl("customer/account.html#address"))}" data-account-page="address">
                    Địa chỉ
                </a>

                <a href="${escapeHtml(siteUrl("customer/orders.html"))}" data-account-page="orders">
                    Đơn hàng
                </a>

                <a href="${escapeHtml(siteUrl("customer/service-requests.html"))}" data-account-page="service-requests">
                    Yêu cầu dịch vụ
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
