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
