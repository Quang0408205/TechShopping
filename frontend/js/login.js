document.addEventListener(
    "DOMContentLoaded",
    function () {

        const form =
            document.getElementById(
                "loginForm"
            );


        form.addEventListener(
            "submit",
            function (event) {

                event.preventDefault();


                const email =
                    document.getElementById(
                        "email"
                    ).value;


                alert(
                    "Đăng nhập demo với tài khoản: " +
                    email
                );


                /*
                 * Sau này thay bằng:
                 *
                 * fetch(
                 *   "http://localhost:8080/api/auth/login",
                 *   {...}
                 * )
                 *
                 * để gọi Spring Boot.
                 */

            }
        );

    }
);