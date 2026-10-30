/* ================= KHUNG DÙNG CHUNG CHO CÁC TRANG admin/ ================= */

/*
 * Mỗi trang admin/*.html (trừ login.html) chỉ cần:
 *
 *   <body class="admin-body" data-page-title="Tổng quan" data-roles="BRANCH_MANAGER,ADMIN">
 *   <div data-include="admin-sidebar" data-active="dashboard"></div>
 *   <div data-include="admin-topbar"></div>
 *
 * data-roles: vai trò được vào trang (bỏ trống = mọi nhân viên).
 *
 * File này:
 *   1. chặn trang nếu chưa đăng nhập nội bộ / không đủ quyền (chạy ngay khi nạp);
 *   2. chờ layout.js chèn sidebar + topbar, ẩn mục menu không thuộc vai trò;
 *   3. điền tên / vai trò / chi nhánh lên topbar, gắn đăng xuất, menu mobile;
 *   4. cung cấp adminLayoutReady (Promise → nhân viên hiện tại, hoặc null nếu
 *      đã chuyển hướng) và các hàm dùng chung cho bảng / bộ lọc.
 *
 * .admin-shell bị ẩn (css/admin/admin.css) cho tới khi body có class
 * "admin-ready" (máy chủ đã xác nhận) hoặc "admin-shell-early" (vẽ sớm theo phiên đã
 * lưu, chỉ khi vai trò trong phiên được vào trang), nên người không đủ quyền không thấy trang.
 *
 * Thứ tự nạp: js/core/api.js → js/core/ui.js → js/core/layout.js →
 * js/admin/staff-auth.js →
 * (js/admin/admin-charts.js) → js/admin/admin-layout.js → script của trang.
 * Script của trang: DOMContentLoaded → const staff = await adminLayoutReady.
 */

const adminLayoutReady = initAdminLayout();


async function initAdminLayout() {

    /*
     * Chuyển trang không giật: có phiên đã lưu (đã được kiểm tra ở trang trước) và sidebar đã được
     * layout.js chèn đồng bộ → vẽ khung ngay theo phiên đó, trước lần vẽ đầu. Máy chủ vẫn kiểm tra lại
     * bên dưới; dữ liệu của trang chỉ tải sau bước đó (adminLayoutReady).
     */
    const cachedAuth = getAuth();

    const cachedStaff = cachedAuth && cachedAuth.staff;

    let shellShown = false;

    if (cachedStaff && pageAllowedFor(cachedStaff) && document.getElementById("adminSidebar")) {

        showShell(cachedStaff, "admin-shell-early");

        shellShown = true;

    }


    const staff = await requireStaffLogin();

    if (!staff) {
        return null;
    }


    if (!pageAllowedFor(staff)) {

        window.location.replace(siteUrl("admin/dashboard.html?denied=1"));

        return null;

    }


    if (typeof layoutReady !== "undefined") {
        await layoutReady;
    }


    if (shellShown) {

        /* Vai trò / tên / chi nhánh có thể vừa đổi */
        applyMenuRbac(staff);

        fillTopbarUser(staff);

        document.body.classList.add("admin-ready");

    } else {

        showShell(staff, "admin-ready");

    }


    return staff;

}


function pageAllowedFor(staff) {

    const allowedRoles = (document.body.dataset.roles || "")
        .split(",")
        .map(function (role) { return role.trim(); })
        .filter(Boolean);

    return allowedRoles.length === 0 || hasStaffRole(staff, allowedRoles);

}


/* readyClass: "admin-shell-early" (theo phiên đã lưu) hoặc "admin-ready" (máy chủ đã xác nhận) */

function showShell(staff, readyClass) {

    applyMenuRbac(staff);

    fillTopbarUser(staff);

    setupAdminLogout();

    setupSidebarToggle();

    setupPageTitle();

    document.body.classList.add(readyClass);

}


/* Ẩn hẳn các mục sidebar mà vai trò hiện tại không được thấy */

function applyMenuRbac(staff) {

    document.querySelectorAll(".admin-nav [data-roles]").forEach(function (link) {

        const roles = link.dataset.roles.split(",").map(function (role) {
            return role.trim();
        });

        link.hidden = !hasStaffRole(staff, roles);

    });


    /* Nhóm menu không còn mục nào hiển thị thì ẩn luôn nhãn nhóm */

    document.querySelectorAll(".admin-nav-label").forEach(function (label) {

        let node = label.nextElementSibling;

        let hasVisibleLink = false;

        while (node && !node.classList.contains("admin-nav-label")) {

            if (node.tagName === "A" && !node.hidden) {
                hasVisibleLink = true;
            }

            node = node.nextElementSibling;

        }

        label.hidden = !hasVisibleLink;

    });

}


function fillTopbarUser(staff) {

    const nameEl = document.getElementById("adminUserName");

    const avatarEl = document.getElementById("adminUserAvatar");

    if (avatarEl) {
        avatarEl.textContent = initialsOf(staff.fullname);
    }

    const metaEl = document.getElementById("adminUserMeta");

    if (nameEl) {
        nameEl.textContent = staff.fullname;
    }

    if (metaEl) {
        /* Không phải ADMIN thì luôn ghi chi nhánh (kể cả "Chưa gán chi nhánh") */
        metaEl.textContent = staff.role !== "ADMIN"
            ? staff.roleLabel + " · " + staff.storeName
            : staff.roleLabel;
    }

}


function setupAdminLogout() {

    const button = document.getElementById("adminLogoutBtn");

    if (button) {
        button.addEventListener("click", staffLogout);
    }

}


function setupSidebarToggle() {

    const toggle = document.getElementById("adminSidebarToggle");

    const sidebar = document.getElementById("adminSidebar");

    if (!toggle || !sidebar) {
        return;
    }


    /* Giữ vị trí cuộn của menu giữa các trang (menu dài, mục cuối không bị nhảy về đầu) */
    try {
        sidebar.scrollTop = Number(sessionStorage.getItem("poy_admin_nav_scroll")) || 0;
    } catch (error) { }

    window.addEventListener("pagehide", function () {
        try {
            sessionStorage.setItem("poy_admin_nav_scroll", String(sidebar.scrollTop));
        } catch (error) { }
    });


    /* Nền mờ phía sau menu khi menu trượt ra (màn hình dưới 1024px) */
    const backdrop = document.createElement("div");

    backdrop.className = "admin-sidebar-backdrop";

    backdrop.hidden = true;

    document.body.appendChild(backdrop);


    function setOpen(open) {

        sidebar.classList.toggle("open", open);

        backdrop.hidden = !open;

        toggle.setAttribute("aria-expanded", String(open));

    }


    toggle.addEventListener("click", function () {
        setOpen(!sidebar.classList.contains("open"));
    });

    backdrop.addEventListener("click", function () {
        setOpen(false);
    });

    document.addEventListener("keydown", function (event) {

        if (event.key === "Escape" && sidebar.classList.contains("open")) {
            setOpen(false);
            toggle.focus();
        }

    });

}


/* "Nguyễn Thị Lan" → "NL" (chữ cái đầu của họ và tên) */

function initialsOf(fullname) {

    const words = String(fullname || "").trim().split(/\s+/).filter(Boolean);

    if (words.length === 0) {
        return "?";
    }

    const first = words[0].charAt(0);

    const last = words.length > 1 ? words[words.length - 1].charAt(0) : "";

    return (first + last).toUpperCase();

}


function setupPageTitle() {

    const titleEl = document.getElementById("adminPageTitle");

    if (titleEl) {
        titleEl.textContent = document.body.dataset.pageTitle || "";
    }

}


/* ================= HÀM DÙNG CHUNG CHO CÁC TRANG ================= */

/*
 * Mọi dòng của một API phân trang (size tối đa của backend là 100).
 * path đã có "?" hoặc chưa đều được.
 */

async function fetchAllPages(path) {

    const rows = [];

    const separator = path.indexOf("?") === -1 ? "?" : "&";

    for (let page = 0; ; page++) {

        const result = await apiRequest(path + separator + "size=100&page=" + page, { auth: true });

        rows.push.apply(rows, result.content || []);

        if (page + 1 >= (result.totalPages || 0)) {
            return rows;
        }

    }

}


/* Mọi chi nhánh thật (cả chi nhánh tạm đóng), theo tên; chỉ ADMIN gọi được */

function loadAllStores() {

    return fetchAllPages("/admin/stores?sort=name");

}


/* "Chi nhánh A" hoặc "Chi nhánh A (tạm đóng)" */

function storeOptionLabel(store) {

    return store.name + (store.active === false ? " (tạm đóng)" : "");

}


/*
 * Bộ lọc chi nhánh THẬT: ADMIN chọn được mọi chi nhánh (đọc API);
 * nhân viên / quản lý chi nhánh bị khoá cứng vào chi nhánh của mình.
 * extraOptions: [{ value, label }] thêm sau danh sách (chỉ ADMIN).
 * Trả về Promise; lỗi tải danh sách → chỉ còn "Tất cả chi nhánh" (ném lại lỗi).
 */

async function setupStoreFilter(select, staff, extraOptions) {

    if (staff.role !== "ADMIN") {

        select.innerHTML =
            `<option value="${escapeHtml(staff.storeId || "")}">${escapeHtml(staff.storeName)}</option>`;

        select.disabled = true;

        return;

    }


    select.innerHTML = '<option value="">Tất cả chi nhánh</option>';

    const stores = await loadAllStores();

    fillStoreOptions(select, [{ value: "", label: "Tất cả chi nhánh" }], stores, extraOptions);

}


function fillStoreOptions(select, leadingOptions, stores, extraOptions) {

    const current = select.value;

    const options = leadingOptions
        .concat(stores.map(function (store) {
            return { value: store.id, label: storeOptionLabel(store) };
        }))
        .concat(extraOptions || []);

    select.innerHTML = options.map(function (option) {
        return `<option value="${escapeHtml(option.value)}">${escapeHtml(option.label)}</option>`;
    }).join("");

    select.value = current;

}


/* Dòng đang tải dạng khung xương cho bảng */

function adminSkeletonRow(colspan) {

    return `
        <tr>
            <td colspan="${colspan}"><span class="admin-skeleton"></span><span class="admin-skeleton"></span></td>
        </tr>
    `;

}


function adminEmptyRow(colspan, message) {

    return `
        <tr>
            <td colspan="${colspan}" class="admin-empty-cell">${escapeHtml(message)}</td>
        </tr>
    `;

}


/* Tiền gọn cho thẻ tổng quan: 1963403000 → "1,96 tỷ đ", 845200000 → "845,2 triệu đ", dưới 1 triệu giữ nguyên */

function formatMoneyShort(value) {

    const amount = Number(value) || 0;

    const size = Math.abs(amount);

    /* 999.960.000 làm tròn thành 1 tỷ chứ không thành "1.000 triệu" */
    if (size >= 999.95e6) {
        return (amount / 1e9).toLocaleString("vi-VN", { maximumFractionDigits: 2 }) + " tỷ đ";
    }

    if (size >= 1e6) {
        return (amount / 1e6).toLocaleString("vi-VN", { maximumFractionDigits: 1 }) + " triệu đ";
    }

    return formatPrice(amount);

}


/* cards: [{ label, value | money, sub }]; money = số tiền: hiện gọn, rê chuột thấy số đầy đủ */

function adminStatCardsHtml(cards) {

    return cards.map(function (card) {

        const money = card.money != null;

        const value = money ? formatMoneyShort(card.money) : card.value;

        const exact = money ? formatPrice(Number(card.money) || 0) : value;

        /* chỉ khi số đã rút gọn (từ 1 triệu); "850.000đ" đã đầy đủ */
        const full = exact !== value ? ` data-full="${escapeHtml(exact)}" tabindex="0"` : "";

        return `
            <div class="admin-stat-card">
                <div class="admin-stat-label">${escapeHtml(card.label)}</div>
                <div class="admin-stat-value"><span${full}>${escapeHtml(value)}</span></div>
                <div class="admin-stat-sub">${escapeHtml(card.sub || "")}</div>
            </div>
        `;

    }).join("");

}


/* "2026-09-18" hoặc "2026-09-18T09:30:00+07:00" → "18/9/2026" */

function formatDateVi(value) {

    const date = /^\d{4}-\d{2}-\d{2}$/.test(value)
        ? new Date(value + "T00:00:00")
        : new Date(value);

    return isNaN(date.getTime()) ? "" : date.toLocaleDateString("vi-VN");

}


function formatDateTimeVi(value) {

    const date = new Date(value);

    return isNaN(date.getTime()) ? "" : date.toLocaleString("vi-VN");

}


/* "2026-09" → "Tháng 9/2026" (short: "T9/26") */

function formatMonthLabel(month, short) {

    const parts = month.split("-");

    return short
        ? "T" + Number(parts[1]) + "/" + parts[0].slice(2)
        : "Tháng " + Number(parts[1]) + "/" + parts[0];

}


function truncateText(text, max) {

    const value = String(text || "");

    return value.length > max ? value.slice(0, max - 1) + "…" : value;

}
