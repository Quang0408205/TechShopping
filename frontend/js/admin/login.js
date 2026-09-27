/* ================= ĐĂNG NHẬP NỘI BỘ (admin/login.html) — B2 ================= */

/*
 * POST /api/v1/auth/login thật (email hoặc tên đăng nhập). Chỉ tài khoản có
 * role STAFF hoặc ADMIN được vào: tài khoản khách hàng đăng nhập đúng mật
 * khẩu vẫn bị từ chối, và refresh token vừa cấp được thu hồi ngay.
 * ?reason=… (staff-auth.js requireStaffLogin) giải thích vì sao bị đưa về đây.
 */

const STAFF_LOGIN_REASONS = {
    expired: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
    locked: "Tài khoản của bạn đã bị khoá hoặc không còn tồn tại. Vui lòng liên hệ quản trị viên.",
    denied: "Tài khoản này không có quyền truy cập khu nội bộ.",
    network: API_ERROR_MESSAGES.NETWORK_ERROR
};


document.addEventListener(
    "DOMContentLoaded",
    async function () {

        const form = document.getElementById("staffLoginForm");

        const errorBox = document.getElementById("staffLoginError");

        const button = document.getElementById("staffLoginButton");

        const params = new URLSearchParams(window.location.search);


        /* Đã có phiên nội bộ hợp lệ thì vào thẳng */

        if (isLoggedIn()) {

            const result = await checkStaffSession();

            if (result.staff) {

                window.location.replace(getRedirectTarget("admin/dashboard.html"));

                return;

            }

        }


        if (STAFF_LOGIN_REASONS[params.get("reason")]) {
            showError(STAFF_LOGIN_REASONS[params.get("reason")]);
        }


        form.addEventListener("submit", async function (event) {

            event.preventDefault();

            const identifier = document.getElementById("staffEmail").value.trim();

            const password = document.getElementById("staffPassword").value;


            if (!identifier || !password) {

                showError("Vui lòng nhập email (hoặc tên đăng nhập) và mật khẩu.");

                return;

            }


            errorBox.hidden = true;

            setLoading(true);


            try {

                const authResponse = await apiRequest("/auth/login", {
                    method: "POST",
                    body: { identifier: identifier, password: password }
                });


                if (!hasStaffAccess(authResponse.user)) {

                    /* Thu hồi phiên vừa cấp, không lưu gì */
                    apiRequest("/auth/logout", {
                        method: "POST",
                        body: { refreshToken: authResponse.refreshToken }
                    }).catch(function () { });

                    showError(STAFF_LOGIN_REASONS.denied);

                    setLoading(false);

                    return;

                }


                saveStaffSession(authResponse);

                window.location.href = getRedirectTarget("admin/dashboard.html");

            } catch (error) {

                showError(getErrorMessage(error));

                setLoading(false);

            }

        });


        function showError(message) {

            errorBox.textContent = message;

            errorBox.hidden = false;

        }


        function setLoading(isLoading) {

            button.disabled = isLoading;

            button.textContent = isLoading ? "ĐANG ĐĂNG NHẬP..." : "ĐĂNG NHẬP";

        }

    }
);
