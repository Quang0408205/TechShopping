/* ================= KẾT NỐI SPRING BOOT API ================= */

/*
 * Dùng chung cho mọi trang. Phải được nạp ĐẦU TIÊN,
 * trước layout.js, main.js và các file JS của từng trang.
 *
 * Mọi response của backend có dạng:
 *   { success, timestamp, data, error: { code, message, details } }
 */

const API_BASE_URL = "http://localhost:8080/api/v1";

/*
 * Đường dẫn gốc của website (thư mục chứa index.html), tính từ vị trí file này
 * (js/core/api.js). Đúng cho mọi trang dù nằm ở gốc, auth/ hay customer/,
 * và cả khi Live Server phục vụ website dưới một thư mục con (vd. /frontend/).
 */

const SITE_ROOT =
    new URL("../../", document.currentScript.src).href;


/* siteUrl("auth/login.html") → địa chỉ đầy đủ của trang tính từ gốc website */

function siteUrl(path) {

    return new URL(path, SITE_ROOT).href;

}


/*
 * Khu nội bộ (admin/) dùng phiên riêng, nên cùng một trình duyệt vẫn đăng
 * nhập song song được một tài khoản khách và một tài khoản nhân viên (B2).
 */

const IS_STAFF_AREA =
    window.location.href.startsWith(SITE_ROOT + "admin/");


/* Khoá localStorage mang tiền tố "poy_" (đổi từ "lahy_" ngày 2026-09-26) */

const CUSTOMER_AUTH_STORAGE_KEY = "poy_auth";

const STAFF_AUTH_STORAGE_KEY = "poy_staff_auth";

/* Phiên dùng cho apiRequest(..., { auth: true }) của trang hiện tại */
const AUTH_STORAGE_KEY =
    IS_STAFF_AREA ? STAFF_AUTH_STORAGE_KEY : CUSTOMER_AUTH_STORAGE_KEY;

/*
 * Giỏ hàng nằm trên server từ Phase 3. Hai khoá cũ chỉ còn để dọn một lần
 * (js/core/cart-store.js): "poy_cart_<userId>" (F2, chuyển lên server) và
 * "poy_cart" (kiểu cũ theo tên sản phẩm, bỏ đi).
 */
const CART_STORAGE_KEY = "poy_cart";


/*
 * Chuyển dữ liệu của khoá cũ "lahy_*" sang khoá mới đúng một lần, để người
 * đang đăng nhập / đang có giỏ hàng không bị mất khi đổi tên. api.js được
 * nạp đầu tiên trên mọi trang nên chỉ cần làm ở đây.
 */

function migrateStorageKey(oldKey, newKey) {

    try {

        const oldValue = localStorage.getItem(oldKey);

        if (oldValue === null) {
            return;
        }

        if (localStorage.getItem(newKey) === null) {
            localStorage.setItem(newKey, oldValue);
        }

        localStorage.removeItem(oldKey);

    } catch (error) {
        /* localStorage bị chặn: bỏ qua */
    }

}


migrateStorageKey("lahy_auth", CUSTOMER_AUTH_STORAGE_KEY);

migrateStorageKey("lahy_cart", CART_STORAGE_KEY);


/*
 * Đang chạy trên máy lập trình (Live Server, static-server…). Khi đó CORS dev
 * cho mọi cổng localhost, nên không gọi được API gần như chắc chắn là backend
 * chưa chạy: báo rõ để người phát triển biết cần làm gì.
 */

const IS_LOCAL_DEV =
    location.hostname === "localhost" || location.hostname === "127.0.0.1";


/* Thông báo tiếng Việt theo mã lỗi của backend */

const API_ERROR_MESSAGES = {

    NETWORK_ERROR: IS_LOCAL_DEV
        ? "Không thể kết nối tới máy chủ (localhost:8080). Hãy kiểm tra backend đã chạy chưa."
        : "Không thể kết nối tới máy chủ. Vui lòng thử lại sau.",

    VALIDATION_ERROR:
        "Dữ liệu chưa hợp lệ. Vui lòng kiểm tra lại.",

    INVALID_CREDENTIALS:
        "Email/tên đăng nhập hoặc mật khẩu không đúng.",

    ACCOUNT_DISABLED:
        "Tài khoản đã bị khóa. Vui lòng liên hệ hỗ trợ.",

    DUPLICATE_EMAIL:
        "Email này đã được đăng ký.",

    DUPLICATE_USERNAME:
        "Tên đăng nhập đã có người sử dụng.",

    UNAUTHORIZED:
        "Vui lòng đăng nhập để tiếp tục.",

    INVALID_TOKEN:
        "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",

    ACCESS_DENIED:
        "Bạn không có quyền thực hiện thao tác này.",

    INVALID_PASSWORD:
        "Mật khẩu hiện tại không đúng.",

    INTERNAL_ERROR:
        "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.",

    /* Khu nội bộ (B2): quản lý người dùng */

    USER_NOT_FOUND:
        "Không tìm thấy người dùng.",

    ROLE_NOT_FOUND:
        "Vai trò không tồn tại.",

    CANNOT_MODIFY_OWN_ACCOUNT:
        "Không thể tự khoá, tự xoá hoặc tự bỏ quyền quản trị của chính mình.",

    LAST_ADMIN:
        "Không thể thực hiện: hệ thống phải còn ít nhất một quản trị viên đang hoạt động.",

    USER_DELETED:
        "Tài khoản này đã bị xoá, không thể thay đổi.",

    /* Khu nội bộ (B2): quản lý sản phẩm */

    PRODUCT_NOT_FOUND:
        "Không tìm thấy sản phẩm (có thể đã bị xoá).",

    DUPLICATE_PRODUCT:
        "Slug hoặc mã SKU đã được dùng cho sản phẩm khác.",

    INVALID_PRODUCT_DATA:
        "Dữ liệu sản phẩm chưa hợp lệ (ví dụ giá khuyến mãi lớn hơn giá gốc).",

    CATEGORY_NOT_FOUND:
        "Danh mục không tồn tại.",

    BRAND_NOT_FOUND:
        "Thương hiệu không tồn tại.",

    RESOURCE_IN_USE:
        "Dữ liệu đang được sử dụng, không thể xoá.",

    /* Ảnh sản phẩm (IMG) */

    PRODUCT_IMAGE_NOT_FOUND:
        "Không tìm thấy ảnh (có thể đã bị xoá).",

    PRODUCT_IMAGE_LIMIT_EXCEEDED:
        "Mỗi sản phẩm có tối đa 10 ảnh.",

    LAST_PRODUCT_IMAGE:
        "Không thể xoá ảnh cuối cùng của sản phẩm. Hãy thêm ảnh khác trước.",

    PRIMARY_IMAGE_REQUIRED:
        "Sản phẩm phải có ảnh chính. Hãy đặt một ảnh khác làm ảnh chính.",

    INVALID_IMAGE_FILE:
        "Chỉ nhận ảnh JPG, PNG hoặc WebP.",

    IMAGE_TOO_LARGE:
        "Ảnh quá lớn (tối đa 5 MB).",

    /* Giỏ hàng (Phase 3) */

    PRODUCT_VARIANT_NOT_FOUND:
        "Phiên bản sản phẩm không tồn tại hoặc đã ngừng kinh doanh.",

    PRODUCT_NOT_AVAILABLE:
        "Sản phẩm này hiện không còn bán.",

    CART_ITEM_NOT_FOUND:
        "Sản phẩm không còn trong giỏ hàng (có thể đã được xóa ở tab khác).",

    CART_LIMIT_EXCEEDED:
        "Giỏ hàng đã đủ 50 sản phẩm khác nhau. Vui lòng xóa bớt trước khi thêm mới.",

    /* Đơn hàng (Phase 4) */

    ORDER_NOT_FOUND:
        "Không tìm thấy đơn hàng.",

    CART_EMPTY:
        "Giỏ hàng của bạn đang trống (đơn hàng có thể đã được đặt ở tab khác).",

    INVALID_ORDER_STATUS:
        "Đơn hàng đã được xử lý nên không thể thực hiện thao tác này. Vui lòng tải lại trang."

};


/* Lỗi trả về từ API, giữ nguyên mã lỗi và chi tiết từng trường */

class ApiError extends Error {

    constructor(status, code, message, details) {

        super(message);

        this.name = "ApiError";
        this.status = status;
        this.code = code;
        this.details = details || null;

    }

}


function getErrorMessage(error) {

    if (error && API_ERROR_MESSAGES[error.code]) {
        return API_ERROR_MESSAGES[error.code];
    }

    if (error && error.message) {
        return error.message;
    }

    return "Đã có lỗi xảy ra. Vui lòng thử lại.";

}


/* ================= LƯU PHIÊN ĐĂNG NHẬP ================= */

function getAuth() {

    try {

        const auth = localStorage.getItem(AUTH_STORAGE_KEY);

        return auth ? JSON.parse(auth) : null;

    } catch (error) {

        return null;

    }

}


/*
 * authResponse = data của /auth/login, /auth/register, /auth/refresh.
 * Phần "staff" của phiên nội bộ (js/admin/staff-auth.js) được giữ lại khi
 * làm mới token của CÙNG một tài khoản.
 */

function saveAuth(authResponse) {

    const previous = getAuth();

    const sameUser =
        previous && previous.user && authResponse.user &&
        previous.user.id === authResponse.user.id;


    localStorage.setItem(
        AUTH_STORAGE_KEY,
        JSON.stringify({
            accessToken: authResponse.accessToken,
            refreshToken: authResponse.refreshToken,
            user: authResponse.user,
            staff: sameUser && previous.staff ? previous.staff : undefined
        })
    );

}


function clearAuth() {

    localStorage.removeItem(AUTH_STORAGE_KEY);

}


function isLoggedIn() {

    const auth = getAuth();

    return Boolean(auth && auth.accessToken && auth.refreshToken);

}


function getCurrentUser() {

    const auth = getAuth();

    return auth ? auth.user : null;

}


/* ================= GỌI API ================= */

/*
 * apiRequest("/products?size=12")
 * apiRequest("/auth/login", { method: "POST", body: {...} })
 * apiRequest("/users/me", { auth: true })
 * apiRequest("/admin/uploads/product-images", { method: "POST", body: formData, auth: true })
 *   (body là FormData thì gửi nguyên dạng multipart, không chuyển JSON)
 *
 * Trả về phần "data" khi thành công, ném ApiError khi thất bại.
 * Chỉ gửi token khi auth = true: backend từ chối token hỏng
 * ngay cả ở API công khai.
 */

async function apiRequest(path, options) {

    const settings = options || {};

    const method = settings.method || "GET";

    const useAuth = settings.auth === true;


    let response =
        await sendRequest(path, method, settings.body, useAuth);


    /* Access token hết hạn: làm mới 1 lần rồi gửi lại */

    if (response.status === 401 && useAuth) {

        const refreshed = await refreshTokens();

        if (refreshed) {

            response =
                await sendRequest(path, method, settings.body, useAuth);

        }

    }


    return readApiResult(response);

}


async function sendRequest(path, method, body, useAuth) {

    const headers = {};

    /* FormData (upload file): trình duyệt tự đặt Content-Type multipart kèm boundary */
    const isFormData = typeof FormData !== "undefined" && body instanceof FormData;

    if (body !== undefined && !isFormData) {
        headers["Content-Type"] = "application/json";
    }

    if (useAuth) {

        const auth = getAuth();

        if (auth && auth.accessToken) {
            headers["Authorization"] = "Bearer " + auth.accessToken;
        }

    }


    try {

        return await fetch(
            API_BASE_URL + path,
            {
                method: method,
                headers: headers,
                body: body === undefined ? undefined : (isFormData ? body : JSON.stringify(body))
            }
        );

    } catch (error) {

        throw new ApiError(
            0,
            "NETWORK_ERROR",
            API_ERROR_MESSAGES.NETWORK_ERROR,
            null
        );

    }

}


async function readApiResult(response) {

    let result = null;

    try {

        /* 204 No Content không có body */
        result = await response.json();

    } catch (error) {

        result = null;

    }


    if (response.ok) {
        return result ? result.data : null;
    }


    const error = result && result.error ? result.error : {};

    throw new ApiError(
        response.status,
        error.code || "HTTP_" + response.status,
        error.message || "Request failed with status " + response.status,
        error.details
    );

}


/* Nhiều request cùng gặp 401 chỉ dùng chung một lần làm mới */

let refreshInProgress = null;


async function refreshTokens() {

    const auth = getAuth();

    if (!auth || !auth.refreshToken) {

        clearAuth();

        return false;

    }


    if (!refreshInProgress) {

        refreshInProgress = (async function () {

            try {

                const response = await sendRequest(
                    "/auth/refresh",
                    "POST",
                    { refreshToken: auth.refreshToken },
                    false
                );

                saveAuth(await readApiResult(response));

                return true;

            } catch (error) {

                /* Refresh token hết hạn hoặc đã bị thu hồi: đăng xuất */
                if (error.status === 401 || error.status === 403) {
                    clearAuth();
                }

                return false;

            } finally {

                refreshInProgress = null;

            }

        })();

    }


    return refreshInProgress;

}


/* ================= ĐĂNG XUẤT / CHUYỂN TRANG ================= */

async function logout() {

    const auth = getAuth();

    /* Xóa phiên ở trình duyệt trước, kể cả khi máy chủ không phản hồi */
    clearAuth();


    if (auth && auth.refreshToken) {

        try {

            await apiRequest(
                "/auth/logout",
                {
                    method: "POST",
                    body: { refreshToken: auth.refreshToken }
                }
            );

        } catch (error) {

            /* Không cần xử lý: phiên ở trình duyệt đã bị xóa */

        }

    }

}


/*
 * Trang cần quay lại sau khi đăng nhập, lấy từ ?redirect=...
 * redirect là đường dẫn tính từ gốc website, vd. "customer/cart.html".
 * Chỉ chấp nhận trang .html trong website (tối đa 1 thư mục)
 * để tránh chuyển hướng ra ngoài. Trả về địa chỉ đầy đủ.
 */

function getRedirectTarget(defaultPage) {

    const target =
        new URLSearchParams(window.location.search).get("redirect");

    if (target && /^([a-z0-9_-]+\/)?[a-z0-9_-]+\.html(\?[^\/\\]*)?$/i.test(target)) {
        return siteUrl(target);
    }

    return siteUrl(defaultPage);

}


/*
 * Chuyển tới trang đăng nhập, sau khi đăng nhập sẽ quay lại trang hiện tại.
 * reason (tuỳ chọn, vd. "cart"): trang đăng nhập hiện câu giải thích tương ứng
 * (LOGIN_REASON_MESSAGES trong js/auth/login.js).
 */

function redirectToLogin(reason) {

    const currentUrl =
        window.location.href.split("#")[0];

    const currentPage =
        currentUrl.startsWith(SITE_ROOT)
            ? currentUrl.slice(SITE_ROOT.length) || "index.html"
            : "index.html";

    /* Khu nội bộ có trang đăng nhập riêng */
    const loginPage = IS_STAFF_AREA ? "admin/login.html" : "auth/login.html";

    window.location.href =
        siteUrl(
            loginPage + "?redirect=" + encodeURIComponent(currentPage) +
            (reason ? "&reason=" + encodeURIComponent(reason) : "")
        );

}
