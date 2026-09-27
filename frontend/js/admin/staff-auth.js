/* ================= PHIÊN ĐĂNG NHẬP NỘI BỘ (admin/) ================= */

/*
 * Khoá riêng "poy_staff_auth", tách khỏi phiên khách hàng "poy_auth"
 * (js/core/api.js), nên cùng một trình duyệt vẫn đăng nhập song song được
 * một tài khoản khách và một tài khoản nhân viên.
 *
 * Hình dạng giống hệt poy_auth để Checkpoint B2 chỉ việc lưu AuthResponse
 * thật của POST /api/v1/auth/login vào đây:
 *   {
 *     accessToken, refreshToken,           (B1: null, chưa có backend)
 *     user:  { id, email, username, fullname, roles: ["STAFF"] | ["ADMIN"] },
 *     staff: { role, storeId, storeName }  (mock tới Phase 7; role là mã
 *                                           hiển thị EMPLOYEE / BRANCH_MANAGER / ADMIN)
 *   }
 *
 * Nạp sau js/core/api.js và js/admin/mock-staff-data.js.
 */

const STAFF_AUTH_KEY = "poy_staff_auth";


function getStaffAuth() {

    try {

        const raw = localStorage.getItem(STAFF_AUTH_KEY);

        const auth = raw ? JSON.parse(raw) : null;

        return auth && auth.user ? auth : null;

    } catch (error) {

        return null;

    }

}


/* staff: kết quả toStaffSummary() của mock-staff-data.js */

function saveStaffSession(staff) {

    const auth = {
        accessToken: null,
        refreshToken: null,
        user: {
            id: staff.id,
            email: staff.email,
            username: staff.email,
            fullname: staff.fullname,
            roles: [staff.role === "ADMIN" ? "ADMIN" : "STAFF"]
        },
        staff: {
            role: staff.role,
            storeId: staff.storeId,
            storeName: staff.storeName
        }
    };

    try {
        localStorage.setItem(STAFF_AUTH_KEY, JSON.stringify(auth));
    } catch (error) {
        /* localStorage bị chặn: không giữ được phiên */
    }

}


function clearStaffAuth() {

    try {
        localStorage.removeItem(STAFF_AUTH_KEY);
    } catch (error) {
        /* bỏ qua */
    }

}


function isStaffLoggedIn() {

    return getStaffAuth() !== null;

}


/*
 * Nhân viên đang đăng nhập, đọc lại từ dữ liệu mới nhất mỗi lần (giống
 * backend kiểm tra lại tài khoản ở mỗi request): bị khoá / không còn tồn
 * tại → null. Vai trò / chi nhánh đổi thì phiên được cập nhật theo.
 */

function getCurrentStaff() {

    const auth = getStaffAuth();

    if (!auth) {
        return null;
    }


    const staff = getEmployeeById(auth.user.id);

    if (!staff || staff.status !== "ACTIVE") {
        return null;
    }


    const saved = auth.staff || {};

    if (staff.role !== saved.role || staff.storeId !== saved.storeId) {
        saveStaffSession(staff);
    }

    return staff;

}


/* Có ít nhất một trong các vai trò hiển thị cho trước */

function hasStaffRole(staff, roles) {

    return Boolean(staff) && roles.indexOf(staff.role) !== -1;

}


function staffLogout() {

    clearStaffAuth();

    window.location.href = siteUrl("admin/login.html");

}


/*
 * Gọi ở đầu mỗi trang admin (trừ login.html). Chưa đăng nhập → về
 * admin/login.html?redirect=<trang hiện tại>. Có phiên nhưng tài khoản đã bị
 * khoá → xoá phiên, về trang đăng nhập với ?reason=locked.
 * Trả về nhân viên hiện tại, hoặc null nếu đã chuyển hướng.
 */

function requireStaffLogin() {

    const hadSession = isStaffLoggedIn();

    const staff = getCurrentStaff();

    if (staff) {
        return staff;
    }


    clearStaffAuth();


    const currentUrl = window.location.href.split("#")[0];

    const currentPage =
        currentUrl.startsWith(SITE_ROOT)
            ? currentUrl.slice(SITE_ROOT.length)
            : "admin/dashboard.html";

    const params = new URLSearchParams();

    params.set("redirect", currentPage || "admin/dashboard.html");

    if (hadSession) {
        params.set("reason", "locked");
    }


    window.location.replace(siteUrl("admin/login.html?" + params.toString()));

    return null;

}
