document.addEventListener(
    "DOMContentLoaded",
    function () {

        /* Đã đăng nhập thì không cần ở lại trang này */

        if (isLoggedIn()) {

            window.location.href =
                getRedirectTarget("index.html");

            return;

        }


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
                loading ? "ĐANG ĐĂNG NHẬP..." : "ĐĂNG NHẬP";

        }

    }
);
