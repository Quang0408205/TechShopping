/* ================= PHIÊN ĐĂNG NHẬP NỘI BỘ (admin/) — B2 ================= */

/*
 * Đăng nhập nội bộ dùng tài khoản THẬT: POST /api/v1/auth/login, chỉ nhận
 * tài khoản có role STAFF hoặc ADMIN. Phiên lưu ở "poy_staff_auth"
 * (api.js tự chọn khoá này trên các trang admin/), tách khỏi phiên khách
 * "poy_auth", nên một trình duyệt vẫn đăng nhập song song được cả hai.
 *
 *   {
 *     accessToken, refreshToken, user,   (AuthResponse thật)
 *     staff: { id, fullname, email, role, roleLabel, storeId, storeName }
 *   }
 *
 * staff.role là mã hiển thị EMPLOYEE / BRANCH_MANAGER / ADMIN:
 *   - role ADMIN của backend → ADMIN (toàn hệ thống);
 *   - role STAFF → vai trò + chi nhánh lấy từ dữ liệu mẫu theo email
 *     (getEmployeeByEmail, mock-staff-data.js) cho tới Phase 7 (employees /
 *     employee_assignments); không có trong dữ liệu mẫu → Nhân viên, chưa gán
 *     chi nhánh (các trang theo chi nhánh sẽ không có dữ liệu).
 *
 * Nạp sau js/core/api.js và js/admin/mock-staff-data.js.
 */

const STAFF_BACKEND_ROLES = ["STAFF", "ADMIN"];


/* Tài khoản (user của AuthResponse / UserResponse) có quyền vào khu nội bộ */

function hasStaffAccess(user) {

    const roles = user && Array.isArray(user.roles) ? user.roles : [];

    return roles.some(function (role) {
        return STAFF_BACKEND_ROLES.indexOf(role) !== -1;
    });

}


/* Hồ sơ hiển thị của nhân viên (vai trò / chi nhánh) từ tài khoản thật */

function buildStaffProfile(user) {

    const isAdmin = (user.roles || []).indexOf("ADMIN") !== -1;

    const employee = isAdmin ? null : getEmployeeByEmail(user.email);

    const role = isAdmin
        ? "ADMIN"
        : (employee && employee.role !== "ADMIN" ? employee.role : "EMPLOYEE");

    const storeId = role === "ADMIN" ? null : (employee ? employee.storeId : null);

    const store = storeId ? getStoreById(storeId) : null;


    return {
        id: user.id,
        fullname: user.fullname || user.username || user.email,
        email: user.email,
        role: role,
        roleLabel: getStaffRoleLabel(role),
        storeId: storeId,
        storeName: role === "ADMIN"
            ? "Toàn hệ thống"
            : (store ? store.name : "Chưa gán chi nhánh")
    };

}


/* Lưu AuthResponse của /auth/login kèm hồ sơ nhân viên */

function saveStaffSession(authResponse) {

    saveAuth(authResponse);

    const auth = getAuth();

    auth.staff = buildStaffProfile(authResponse.user);

    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));

}


/*
 * Nhân viên đang đăng nhập, KIỂM TRA LẠI với máy chủ mỗi lần mở trang
 * (GET /users/me): tài khoản bị khoá / xoá / hết phiên / mất quyền STAFF
 * và ADMIN → xoá phiên. Trả về:
 *   { staff }                      hợp lệ (phiên được cập nhật tên / vai trò mới)
 *   { reason: "none" }             chưa đăng nhập
 *   { reason: "expired" | "locked" | "denied" }  phiên đã bị xoá
 *   { reason: "network" }          không gọi được máy chủ (giữ nguyên phiên)
 */

async function checkStaffSession() {

    if (!isLoggedIn()) {
        return { reason: "none" };
    }


    let me;

    try {

        me = await apiRequest("/users/me", { auth: true });

    } catch (error) {

        if (error.code === "NETWORK_ERROR") {
            return { reason: "network" };
        }

        clearAuth();

        return { reason: error.code === "ACCOUNT_DISABLED" ? "locked" : "expired" };

    }


    if (!hasStaffAccess(me)) {

        await logout();

        return { reason: "denied" };

    }


    /*
     * Vai trò trong DB khác vai trò trong phiên (vừa được cấp / bỏ quyền):
     * làm mới token để access token mang đúng vai trò mới, nếu không API
     * admin sẽ trả 403 cho tới khi token cũ hết hạn (≤ 30 phút).
     */

    const current = getAuth();

    const sessionRoles = current && current.user ? (current.user.roles || []).slice().sort().join() : "";

    if (sessionRoles !== (me.roles || []).slice().sort().join()) {
        await refreshTokens();
    }


    const auth = getAuth();

    if (!auth) {
        return { reason: "expired" };
    }

    auth.user = {
        id: me.id,
        email: me.email,
        username: me.username,
        fullname: me.fullname,
        roles: me.roles
    };

    auth.staff = buildStaffProfile(auth.user);

    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));


    return { staff: auth.staff };

}


/* Có ít nhất một trong các vai trò hiển thị cho trước */

function hasStaffRole(staff, roles) {

    return Boolean(staff) && roles.indexOf(staff.role) !== -1;

}


async function staffLogout() {

    await logout();

    window.location.href = siteUrl("admin/login.html");

}


/*
 * Gọi ở đầu mỗi trang admin (trừ login.html). Không có phiên hợp lệ → về
 * admin/login.html?redirect=<trang hiện tại>[&reason=...].
 * Trả về nhân viên hiện tại, hoặc null nếu đã chuyển hướng.
 */

async function requireStaffLogin() {

    const result = await checkStaffSession();

    if (result.staff) {
        return result.staff;
    }


    const currentUrl = window.location.href.split("#")[0];

    const currentPage =
        currentUrl.startsWith(SITE_ROOT)
            ? currentUrl.slice(SITE_ROOT.length)
            : "admin/dashboard.html";

    const params = new URLSearchParams();

    params.set("redirect", currentPage || "admin/dashboard.html");

    if (result.reason !== "none") {
        params.set("reason", result.reason);
    }

    window.location.replace(siteUrl("admin/login.html?" + params.toString()));

    return null;

}
