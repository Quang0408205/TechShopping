/* ================= KẾT NỐI SPRING BOOT API ================= */

/*
 * Dùng chung cho mọi trang. Phải được nạp ĐẦU TIÊN,
 * trước layout.js, main.js và các file JS của từng trang.
 *
 * Mọi response của backend có dạng:
 *   { success, timestamp, data, error: { code, message, details } }
 */

const API_BASE_URL = "http://localhost:8080/api/v1";

const AUTH_STORAGE_KEY = "lahy_auth";


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


/* Thông báo tiếng Việt theo mã lỗi của backend */

const API_ERROR_MESSAGES = {

    NETWORK_ERROR:
        "Không thể kết nối tới máy chủ. Vui lòng thử lại sau.",

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
        "Hệ thống đang gặp sự cố. Vui lòng thử lại sau."

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


/* authResponse = data của /auth/login, /auth/register, /auth/refresh */

function saveAuth(authResponse) {

    localStorage.setItem(
        AUTH_STORAGE_KEY,
        JSON.stringify({
            accessToken: authResponse.accessToken,
            refreshToken: authResponse.refreshToken,
            user: authResponse.user
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

    if (body !== undefined) {
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
                body: body !== undefined ? JSON.stringify(body) : undefined
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


/* Chuyển tới trang đăng nhập, sau khi đăng nhập sẽ quay lại trang hiện tại */

function redirectToLogin() {

    const currentUrl =
        window.location.href.split("#")[0];

    const currentPage =
        currentUrl.startsWith(SITE_ROOT)
            ? currentUrl.slice(SITE_ROOT.length) || "index.html"
            : "index.html";

    window.location.href =
        siteUrl(
            "auth/login.html?redirect=" + encodeURIComponent(currentPage)
        );

}
