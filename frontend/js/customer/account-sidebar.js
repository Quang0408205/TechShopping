/* ================= THANH BÊN TRANG TÀI KHOẢN ================= */

/*
 * Dùng chung cho Tài khoản / Lịch sử mua hàng / Yêu cầu dịch vụ. Trang đánh dấu mục đang mở bằng
 * <body data-account-page="profile|loyalty|security|address|orders|service-requests"> (trang Tài khoản đổi
 * giá trị theo hash, xem account.js). Kế hoạch v5 GĐ6: mỗi mục có icon, khối người dùng có nhãn hạng thành viên
 * (GET /loyalty/me, dùng chung qua getLoyaltySummary()); điện thoại: menu thành hàng tab cuộn ngang.
 * Tên / email lấy từ phiên đăng nhập (poy_auth) nên phải escape.
 */

const ACCOUNT_NAV_ICONS = {
    profile: '<path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"/><path d="M4 20c1.5-3.5 4.5-5 8-5s6.5 1.5 8 5"/>',
    loyalty: '<path d="m12 3 2.6 5.4 5.9.8-4.3 4.1 1 5.8L12 16.4 6.8 19.1l1-5.8L3.5 9.2l5.9-.8Z"/>',
    orders: '<path d="M6 7h12l-1 13H7Z"/><path d="M9 7V5a3 3 0 0 1 6 0v2"/>',
    wishlist: '<path d="M12 20.5s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7.6a4.3 4.3 0 0 1 7.5 2.7c0 5.6-7.5 10.2-7.5 10.2Z"/>',
    "service-requests": '<path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L4 17l3 3 5.3-5.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.4-.6-.6-2.4Z"/>',
    address: '<path d="M12 21s-6-5.3-6-10a6 6 0 0 1 12 0c0 4.7-6 10-6 10Z"/><circle cx="12" cy="11" r="2.2"/>',
    security: '<rect x="5" y="10.5" width="14" height="10" rx="2"/><path d="M8 10.5V7.5a4 4 0 0 1 8 0v3"/>',
    logout: '<path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3"/><path d="M10 16l-4-4 4-4M6 12h10"/>'
};

let loyaltySummaryPromise = null;

/* GET /loyalty/me một lần cho cả trang (thanh bên + thẻ thành viên); lỗi → null */
function getLoyaltySummary() {

    if (!loyaltySummaryPromise) {
        loyaltySummaryPromise = apiRequest("/loyalty/me", { auth: true }).catch(function () { return null; });
    }

    return loyaltySummaryPromise;

}


function accountNavIcon(name) {

    return `<svg class="account-nav-icon" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ACCOUNT_NAV_ICONS[name] || ""}</svg>`;

}


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


/* Điện thoại: menu là hàng tab cuộn ngang → cuộn tab đang chọn vào giữa (không cuộn cả trang) */
function revealActiveAccountTab() {

    const nav = document.querySelector("#accountSidebarRoot .account-nav");

    const active = nav ? nav.querySelector("a.active") : null;

    if (active && nav.scrollWidth > nav.clientWidth) {
        nav.scrollLeft = active.offsetLeft - (nav.clientWidth - active.offsetWidth) / 2;
    }

}


function renderAccountSidebar() {

    const root = document.getElementById("accountSidebarRoot");

    if (!root) {
        return;
    }


    const user = getCurrentUser() || {};

    const currentPage = document.body.dataset.accountPage || "";

    const link = function (page, href, label) {
        return `<a href="${escapeHtml(siteUrl(href))}" data-account-page="${page}">${accountNavIcon(page)}<span>${escapeHtml(label)}</span></a>`;
    };


    root.innerHTML = `

        <aside class="account-sidebar">

            <div class="account-sidebar-user">
                ${userAvatarHtml(user, "user-avatar")}
                <span class="account-sidebar-text">
                    <strong>${escapeHtml(user.fullname || user.username || "Tài khoản")}</strong>
                    <span>${escapeHtml(user.email || "")}</span>
                    <em class="tier-chip" id="sidebarTierChip" hidden></em>
                </span>
            </div>

            <nav class="account-nav" aria-label="Tài khoản">

                ${link("profile", "customer/account.html#profile", "Hồ sơ")}
                ${link("loyalty", "customer/account.html#loyalty", "Hạng & điểm")}
                ${link("orders", "customer/orders.html", "Lịch sử mua hàng")}
                ${link("wishlist", "customer/wishlist.html", "Yêu thích")}
                ${link("service-requests", "customer/service-requests.html", "Yêu cầu dịch vụ")}
                ${link("address", "customer/account.html#address", "Địa chỉ")}
                ${link("security", "customer/account.html#security", "Bảo mật")}

                <a href="#" id="sidebarLogoutBtn" data-account-page="logout" class="account-nav-logout">${accountNavIcon("logout")}<span>Đăng xuất</span></a>

            </nav>

        </aside>

    `;


    root.querySelectorAll("[data-account-page]").forEach(function (item) {

        item.classList.toggle("active", item.dataset.accountPage === currentPage);

    });

    revealActiveAccountTab();


    document.getElementById("sidebarLogoutBtn")
        .addEventListener("click", async function (event) {

            event.preventDefault();

            await logout();

            window.location.href = siteUrl("index.html");

        });


    getLoyaltySummary().then(function (summary) {

        const chip = document.getElementById("sidebarTierChip");

        if (!chip || !summary || !summary.tier) {
            return;
        }

        chip.textContent = "Hạng " + summary.tier.name;

        chip.className = "tier-chip tier-chip--" + String(summary.tier.code).toLowerCase();

        chip.hidden = false;

    });

}
