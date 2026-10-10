/* ================= PHIÊN ĐĂNG NHẬP NỘI BỘ (admin/) — B2 ================= */

/*
 * Đăng nhập nội bộ dùng tài khoản THẬT: POST /api/v1/auth/login, chỉ nhận
 * tài khoản có role STAFF, BRANCH_MANAGER hoặc ADMIN. Phiên lưu ở "poy_staff_auth"
 * (api.js tự chọn khoá này trên các trang admin/), tách khỏi phiên khách
 * "poy_auth", nên một trình duyệt vẫn đăng nhập song song được cả hai.
 *
 *   {
 *     accessToken, refreshToken, user,   (AuthResponse thật)
 *     staff: { id, fullname, email, role, roleLabel, storeId, storeName }
 *   }
 *
 * staff.role là mã hiển thị EMPLOYEE / BRANCH_MANAGER / ADMIN, lấy từ ROLE của
 * backend (không còn suy ra từ chức danh tại chi nhánh):
 *   - ADMIN → ADMIN (toàn hệ thống);
 *   - BRANCH_MANAGER → BRANCH_MANAGER; STAFF → EMPLOYEE;
 *   - chi nhánh = phân công đang hiệu lực trong hồ sơ nhân viên thật
 *     (GET /employees/me); chưa có hồ sơ / chưa được gán → "Chưa gán chi nhánh"
 *     (backend từ chối mọi thao tác theo chi nhánh của tài khoản này).
 *
 * Nạp sau js/core/api.js.
 */

const STAFF_BACKEND_ROLES = ["STAFF", "BRANCH_MANAGER", "ADMIN"];


const STAFF_ROLE_LABELS = {
    EMPLOYEE: "Nhân viên",
    BRANCH_MANAGER: "Quản lý chi nhánh",
    ADMIN: "Quản trị viên"
};


function getStaffRoleLabel(role) {

    return STAFF_ROLE_LABELS[role] || role;

}


/*
 * Chức danh tại chi nhánh (employee_assignments.position_at_store) chỉ để HIỂN THỊ.
 * "Quản lý chi nhánh" gắn với role BRANCH_MANAGER nên không có trong danh sách chọn.
 */

const BRANCH_MANAGER_POSITION = "Quản lý chi nhánh";

const STORE_POSITIONS = ["Nhân viên bán hàng", "Nhân viên kỹ thuật", "Thu ngân"];


/* Tài khoản (user của AuthResponse / UserResponse) có quyền vào khu nội bộ */

function hasStaffAccess(user) {

    const roles = user && Array.isArray(user.roles) ? user.roles : [];

    return roles.some(function (role) {
        return STAFF_BACKEND_ROLES.indexOf(role) !== -1;
    });

}


/*
 * Hồ sơ hiển thị của nhân viên. employee: EmployeeResponse của GET /employees/me
 * (null = chưa có hồ sơ). Chi nhánh = phân công đang hiệu lực.
 */

function buildStaffProfile(user, employee) {

    const isAdmin = (user.roles || []).indexOf("ADMIN") !== -1;

    const assignment = !isAdmin && employee && employee.active ? employee.assignment : null;

    const role = isAdmin
        ? "ADMIN"
        : ((user.roles || []).indexOf("BRANCH_MANAGER") !== -1 ? "BRANCH_MANAGER" : "EMPLOYEE");


    return {
        id: user.id,
        fullname: user.fullname || user.username || user.email,
        email: user.email,
        role: role,
        roleLabel: getStaffRoleLabel(role),
        storeId: assignment ? assignment.storeId : null,
        storeName: isAdmin
            ? "Toàn hệ thống"
            : (assignment ? assignment.storeName : "Chưa gán chi nhánh")
    };

}


/*
 * Đọc hồ sơ nhân viên thật cho tài khoản STAFF (ADMIN không cần). 404 = chưa có hồ sơ.
 * Lỗi mạng → giữ hồ sơ đã lưu trong phiên (previous) nếu có.
 */

async function loadStaffProfile(user, previous) {

    if ((user.roles || []).indexOf("ADMIN") !== -1) {
        return buildStaffProfile(user, null);
    }


    try {

        return buildStaffProfile(user, await apiRequest("/employees/me", { auth: true }));

    } catch (error) {

        if (error.code === "NETWORK_ERROR" && previous) {
            return previous;
        }

        return buildStaffProfile(user, null);

    }

}


/* Lưu AuthResponse của /auth/login kèm hồ sơ nhân viên */

async function saveStaffSession(authResponse) {

    saveAuth(authResponse);

    const staff = await loadStaffProfile(authResponse.user, null);

    const auth = getAuth();

    if (auth) {

        auth.staff = staff;

        localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));

    }

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


    const user = {
        id: me.id,
        email: me.email,
        username: me.username,
        fullname: me.fullname,
        roles: me.roles
    };

    const previous = getAuth();

    const staff = await loadStaffProfile(user, previous && previous.staff);

    /* Đọc lại sau lời gọi mạng: phiên có thể vừa được làm mới / xoá */
    const auth = getAuth();

    if (!auth) {
        return { reason: "expired" };
    }

    auth.user = user;

    auth.staff = staff;

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
