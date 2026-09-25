document.addEventListener(
    "DOMContentLoaded",
    function () {

        if (isLoggedIn()) {

            window.location.href =
                getRedirectTarget("index.html");

            return;

        }


        const form =
            document.getElementById(
                "registerForm"
            );

        const errorBox =
            document.getElementById(
                "registerError"
            );

        const button =
            document.getElementById(
                "registerButton"
            );


        /*
         * Cùng ràng buộc với RegisterRequest ở backend,
         * dùng để hiển thị lỗi tiếng Việt theo từng ô.
         */

        const FIELD_MESSAGES = {

            fullname:
                "Vui lòng nhập họ và tên (tối đa 120 ký tự).",

            email:
                "Email không hợp lệ.",

            username:
                "Tên đăng nhập gồm 3-50 ký tự: chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.",

            phone:
                "Số điện thoại tối đa 20 ký tự.",

            password:
                "Mật khẩu phải từ 8 đến 72 ký tự.",

            confirmPassword:
                "Mật khẩu nhập lại không khớp."

        };


        form.addEventListener(
            "submit",
            async function (event) {

                event.preventDefault();


                const data = {

                    fullname:
                        valueOf("fullname").trim(),

                    email:
                        valueOf("email").trim(),

                    username:
                        valueOf("username").trim(),

                    phone:
                        valueOf("phone").trim(),

                    password:
                        valueOf("password"),

                    confirmPassword:
                        valueOf("confirmPassword")

                };


                clearErrors();


                const invalidFields = validate(data);

                if (invalidFields.length > 0) {

                    invalidFields.forEach(function (field) {

                        showFieldError(field, FIELD_MESSAGES[field]);

                    });

                    showError(
                        API_ERROR_MESSAGES.VALIDATION_ERROR
                    );

                    return;

                }


                setLoading(true);


                try {

                    /* POST /api/v1/auth/register: tài khoản khách hàng, đăng nhập luôn */

                    const auth = await apiRequest(
                        "/auth/register",
                        {
                            method: "POST",
                            body: {
                                fullname: data.fullname,
                                email: data.email,
                                username: data.username,
                                phone: data.phone || null,
                                password: data.password
                            }
                        }
                    );


                    saveAuth(auth);

                    window.location.href =
                        getRedirectTarget("index.html");

                } catch (error) {

                    showApiError(error);

                    setLoading(false);

                }

            }
        );


        function validate(data) {

            const invalid = [];

            if (!data.fullname || data.fullname.length > 120) {
                invalid.push("fullname");
            }

            if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email) || data.email.length > 120) {
                invalid.push("email");
            }

            if (!/^[A-Za-z0-9._-]{3,50}$/.test(data.username)) {
                invalid.push("username");
            }

            if (data.phone.length > 20) {
                invalid.push("phone");
            }

            /* BCrypt chỉ nhận tối đa 72 byte (chữ có dấu chiếm nhiều byte) */
            const passwordBytes =
                new TextEncoder().encode(data.password).length;

            if (data.password.length < 8 || passwordBytes > 72) {
                invalid.push("password");
            }

            if (data.confirmPassword !== data.password) {
                invalid.push("confirmPassword");
            }

            return invalid;

        }


        function showApiError(error) {

            if (error.code === "DUPLICATE_EMAIL") {
                showFieldError("email", getErrorMessage(error));
            }

            if (error.code === "DUPLICATE_USERNAME") {
                showFieldError("username", getErrorMessage(error));
            }

            /* VALIDATION_ERROR: details = { tênTrường: thông báo } */
            if (error.details) {

                Object.keys(error.details).forEach(function (field) {

                    showFieldError(
                        field,
                        FIELD_MESSAGES[field] || error.details[field]
                    );

                });

            }

            showError(
                getErrorMessage(error)
            );

        }


        function valueOf(id) {

            return document.getElementById(id).value;

        }


        function showFieldError(field, message) {

            const element =
                form.querySelector(
                    `.field-error[data-field="${field}"]`
                );

            if (element) {
                element.textContent = message;
            }

        }


        function clearErrors() {

            form.querySelectorAll(".field-error")
                .forEach(function (element) {

                    element.textContent = "";

                });

            errorBox.textContent = "";

            errorBox.hidden = true;

        }


        function showError(message) {

            errorBox.textContent = message;

            errorBox.hidden = false;

        }


        function setLoading(loading) {

            button.disabled = loading;

            button.textContent =
                loading ? "ĐANG ĐĂNG KÝ..." : "ĐĂNG KÝ";

        }

    }
);
