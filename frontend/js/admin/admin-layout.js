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
 * "admin-ready", nên người không đủ quyền không thấy nội dung trang.
 *
 * Thứ tự nạp: js/core/api.js → js/core/ui.js → js/core/layout.js →
 * js/admin/mock-staff-data.js → js/admin/staff-auth.js →
 * (js/admin/admin-charts.js) → js/admin/admin-layout.js → script của trang.
 * Script của trang: DOMContentLoaded → const staff = await adminLayoutReady.
 */

const adminLayoutReady = initAdminLayout();


async function initAdminLayout() {

    const staff = await requireStaffLogin();

    if (!staff) {
        return null;
    }


    const allowedRoles = (document.body.dataset.roles || "")
        .split(",")
        .map(function (role) { return role.trim(); })
        .filter(Boolean);

    if (allowedRoles.length > 0 && !hasStaffRole(staff, allowedRoles)) {

        window.location.replace(siteUrl("admin/dashboard.html?denied=1"));

        return null;

    }


    if (typeof layoutReady !== "undefined") {
        await layoutReady;
    }


    applyMenuRbac(staff);

    fillTopbarUser(staff);

    setupAdminLogout();

    setupSidebarToggle();

    setupPageTitle();

    document.body.classList.add("admin-ready");


    return staff;

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


    toggle.addEventListener("click", function () {

        const isOpen = sidebar.classList.toggle("open");

        toggle.setAttribute("aria-expanded", String(isOpen));

    });

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
 * Bộ lọc chi nhánh THẬT (Phase 7): ADMIN chọn được mọi chi nhánh (đọc API);
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


/*
 * Bộ lọc chi nhánh của các trang còn dùng DỮ LIỆU MẪU (Tổng quan, Báo cáo, Bảo
 * hành, Hỗ trợ): dùng 3 chi nhánh mẫu CN01–CN03 của mock-staff-data.js cho tới phase
 * thay các trang đó. Nhân viên thật có chi nhánh thật nên không khớp dữ liệu mẫu nào.
 */

function setupMockStoreFilter(select, staff, extraOptions) {

    if (staff.role === "ADMIN") {

        const options = [{ value: "", label: "Tất cả chi nhánh" }]
            .concat(getMockStores().map(function (store) {
                return { value: store.id, label: store.name };
            }))
            .concat(extraOptions || []);

        select.innerHTML = options.map(function (option) {
            return `<option value="${escapeHtml(option.value)}">${escapeHtml(option.label)}</option>`;
        }).join("");

        return;

    }


    select.innerHTML =
        `<option value="${escapeHtml(staff.storeId || "")}">${escapeHtml(staff.storeName)}</option>`;

    select.disabled = true;

}


/*
 * Chi nhánh dùng để lọc dữ liệu. Không phải ADMIN thì LUÔN là chi nhánh của
 * mình, kể cả khi ai đó sửa DOM (phòng thủ hai lớp).
 */

/*
 * Nhân viên chưa được gán chi nhánh nhận mã không khớp chi nhánh nào, để không
 * bao giờ thấy dữ liệu của mọi chi nhánh (các bộ lọc coi storeId rỗng là "tất cả").
 */

const NO_STORE_SCOPE = "NO_STORE";


function getScopedStoreId(staff, select) {

    if (staff.role === "ADMIN") {
        return (select && select.value) || undefined;
    }

    return staff.storeId || NO_STORE_SCOPE;

}


/* Ô chọn trạng thái trong bảng; labels: { CODE: "Nhãn" } */

function statusSelectHtml(id, currentStatus, labels, ariaLabel) {

    const options = Object.keys(labels).map(function (code) {
        return `<option value="${escapeHtml(code)}" ${code === currentStatus ? "selected" : ""}>${escapeHtml(labels[code])}</option>`;
    }).join("");


    return `
        <select
            class="admin-status-select js-status-select"
            data-id="${escapeHtml(id)}"
            data-status="${escapeHtml(currentStatus)}"
            aria-label="${escapeHtml(ariaLabel || "Trạng thái")}"
        >
            ${options}
        </select>
    `;

}


/* Gắn onChange(id, status) cho các ô trạng thái trong container */

function bindStatusSelects(container, onChange) {

    container.querySelectorAll(".js-status-select").forEach(function (select) {

        select.addEventListener("change", function () {
            onChange(select.dataset.id, select.value);
        });

    });

}


function adminEmptyRow(colspan, message) {

    return `
        <tr>
            <td colspan="${colspan}" class="admin-empty-cell">${escapeHtml(message)}</td>
        </tr>
    `;

}


/* cards: [{ label, value, sub }] */

function adminStatCardsHtml(cards) {

    return cards.map(function (card) {

        return `
            <div class="admin-stat-card">
                <div class="admin-stat-label">${escapeHtml(card.label)}</div>
                <div class="admin-stat-value">${escapeHtml(card.value)}</div>
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


/*
 * Nhãn ngắn cho trục biểu đồ: bỏ tên loại ở đầu ("Điện thoại", "Laptop"...)
 * mà gần như mọi tên sản phẩm đều có, rồi cắt ngắn. Tên đầy đủ vẫn nằm
 * trong tooltip và bảng bên dưới biểu đồ.
 */

function shortProductLabel(name) {

    const withoutType = String(name || "")
        .replace(/^(Điện thoại|Laptop|Máy tính bảng|Đồng hồ thông minh|Đồng hồ định vị trẻ em|Đồng hồ|Sạc nhanh|Ốp lưng)\s+/i, "");

    return truncateText(withoutType, 13);

}
