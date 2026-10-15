/* ================= ĐỔI MẬT KHẨU TẠM (admin/change-password.html) ================= */

/*
 * Tài khoản mới tuyển có mustChangePassword: backend chỉ cho GET /users/me, PUT /users/me/password và
 * /auth/* cho tới khi đổi xong (PASSWORD_CHANGE_REQUIRED với mọi API khác). Đổi xong backend thu hồi mọi phiên
 * nên xoá phiên cục bộ và về trang đăng nhập.
 */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        const form = document.getElementById("changePasswordForm");

        const errorBox = document.getElementById("changePasswordError");

        const button = document.getElementById("changePasswordButton");


        document.getElementById("changePasswordLogout").addEventListener("click", function (event) {

            event.preventDefault();

            staffLogout();

        });


        if (!isLoggedIn()) {

            window.location.replace(siteUrl("admin/login.html"));

            return;

        }


        /* Chỉ tài khoản đang phải đổi mật khẩu mới ở lại trang này */

        const result = await checkStaffSession();

        if (result.staff) {

            window.location.replace(siteUrl("admin/dashboard.html"));

            return;

        }

        if (result.reason !== "password") {

            window.location.replace(siteUrl("admin/login.html" + (result.reason === "none" ? "" : "?reason=" + result.reason)));

            return;

        }


        form.addEventListener("submit", async function (event) {

            event.preventDefault();

            const currentPassword = document.getElementById("currentPassword").value;

            const newPassword = document.getElementById("newPassword").value;

            const confirmPassword = document.getElementById("confirmNewPassword").value;


            if (!currentPassword) {
                return showError("Vui lòng nhập mật khẩu tạm.");
            }

            if (newPassword.length < 8 || new TextEncoder().encode(newPassword).length > 72) {
                return showError("Mật khẩu mới phải từ 8 ký tự và tối đa 72 byte.");
            }

            if (newPassword === currentPassword) {
                return showError("Mật khẩu mới phải khác mật khẩu tạm.");
            }

            if (newPassword !== confirmPassword) {
                return showError("Hai lần nhập mật khẩu mới chưa khớp.");
            }


            errorBox.hidden = true;

            setLoading(true);


            try {

                await apiRequest("/users/me/password", {
                    method: "PUT",
                    auth: true,
                    body: { currentPassword: currentPassword, newPassword: newPassword }
                });

                clearAuth();

                window.location.replace(siteUrl("admin/login.html?reason=passwordChanged"));

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

            button.textContent = isLoading ? "Đang đổi..." : "Đổi mật khẩu";

        }

    }
);
