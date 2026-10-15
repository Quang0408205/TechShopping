document.addEventListener(
    "DOMContentLoaded",
    function () {

        /* Đã đăng nhập thì không cần ở lại trang này */

        if (isLoggedIn()) {

            window.location.href =
                getRedirectTarget("index.html");

            return;

        }


        showLoginReason();


        const form =
            document.getElementById(
                "loginForm"
            );

        const errorBox =
            document.getElementById(
                "loginError"
            );

        const button =
            document.getElementById(
                "loginButton"
            );


        form.addEventListener(
            "submit",
            async function (event) {

                event.preventDefault();


                const identifier =
                    document.getElementById(
                        "identifier"
                    ).value.trim();

                const password =
                    document.getElementById(
                        "password"
                    ).value;


                if (!identifier || !password) {

                    showError(
                        "Vui lòng nhập email/tên đăng nhập và mật khẩu."
                    );

                    return;

                }


                hideError();

                setLoading(true);


                try {

                    /*
                     * POST /api/v1/auth/login
                     * identifier: email hoặc tên đăng nhập
                     */

                    const auth = await apiRequest(
                        "/auth/login",
                        {
                            method: "POST",
                            body: {
                                identifier: identifier,
                                password: password
                            }
                        }
                    );


                    /*
                     * Tài khoản nội bộ (nhân viên, quản lý chi nhánh, quản trị viên) không mua hàng ở
                     * trang khách: không lưu phiên, thu hồi refresh token vừa cấp và chỉ đường sang
                     * trang quản trị.
                     */

                    if ((auth.user.roles || []).indexOf("CUSTOMER") === -1) {

                        try {

                            await apiRequest(
                                "/auth/logout",
                                { method: "POST", body: { refreshToken: auth.refreshToken } }
                            );

                        } catch (logoutError) {

                            /* không thu hồi được thì phiên vẫn không được lưu ở trình duyệt này */

                        }

                        showInternalAccountNotice();

                        setLoading(false);

                        return;

                    }


                    saveAuth(auth);

                    window.location.href =
                        getRedirectTarget("index.html");

                } catch (error) {

                    showError(
                        getErrorMessage(error)
                    );

                    setLoading(false);

                }

            }
        );


        function showError(message) {

            errorBox.textContent = message;

            errorBox.hidden = false;

        }


        function showInternalAccountNotice() {

            errorBox.textContent = "Tài khoản nội bộ vui lòng đăng nhập tại trang quản trị. ";

            const link = document.createElement("a");

            link.href = siteUrl("admin/login.html");

            link.id = "internalLoginLink";

            link.textContent = "Mở trang đăng nhập quản trị";

            errorBox.appendChild(link);

            errorBox.hidden = false;

        }


        function hideError() {

            errorBox.textContent = "";

            errorBox.hidden = true;

        }


        function setLoading(loading) {

            button.disabled = loading;

            button.textContent =
                loading ? "Đang đăng nhập..." : "Đăng nhập";

        }

    }
);


/* ================= LÝ DO CHUYỂN TỚI TRANG ĐĂNG NHẬP ================= */

/*
 * ?reason=cart (redirectToLogin("cart") trong main.js): giải thích vì sao
 * người dùng bị chuyển tới đây. Link "Đăng ký" giữ nguyên ?redirect để đăng
 * ký xong cũng quay lại đúng trang (register.js dùng getRedirectTarget).
 */

const LOGIN_REASON_MESSAGES = {
    cart: "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng.",
    review: "Vui lòng đăng nhập để viết đánh giá sản phẩm."
};


function showLoginReason() {

    const params = new URLSearchParams(window.location.search);

    const notice = document.getElementById("loginNotice");

    const message = LOGIN_REASON_MESSAGES[params.get("reason")];


    if (notice && message) {

        notice.textContent = message;

        notice.hidden = false;

    }


    const registerLink = document.getElementById("registerLink");

    const redirect = params.get("redirect");

    if (registerLink && redirect) {

        registerLink.href = "register.html?redirect=" + encodeURIComponent(redirect);

    }

}
