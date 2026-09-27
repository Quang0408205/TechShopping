/* ================= ĐĂNG NHẬP NỘI BỘ (admin/login.html) ================= */

/*
 * B1: xác thực bằng dữ liệu mẫu (authenticateStaff, js/admin/mock-staff-data.js).
 * B2 sẽ thay bằng POST /api/v1/auth/login thật và chỉ nhận tài khoản có
 * role STAFF hoặc ADMIN.
 */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        const form = document.getElementById("staffLoginForm");

        const errorBox = document.getElementById("staffLoginError");

        const button = document.getElementById("staffLoginButton");

        const params = new URLSearchParams(window.location.search);


        /* Đã đăng nhập nội bộ (và tài khoản còn hoạt động) thì vào thẳng */

        if (getCurrentStaff()) {

            window.location.replace(getRedirectTarget("admin/dashboard.html"));

            return;

        }


        if (params.get("reason") === "locked") {
            showError("Tài khoản của bạn đã bị khoá hoặc không còn tồn tại. Vui lòng liên hệ quản trị viên.");
        }


        form.addEventListener("submit", function (event) {

            event.preventDefault();


            const email = document.getElementById("staffEmail").value.trim();

            const password = document.getElementById("staffPassword").value;


            if (!email || !password) {

                showError("Vui lòng nhập email và mật khẩu.");

                return;

            }


            errorBox.hidden = true;

            setLoading(true);


            /* Độ trễ nhỏ mô phỏng gọi máy chủ */

            setTimeout(function () {

                const result = authenticateStaff(email, password);


                if (result.error === "LOCKED") {

                    showError("Tài khoản đã bị khoá. Vui lòng liên hệ quản trị viên.");

                    setLoading(false);

                    return;

                }


                if (result.error) {

                    showError("Email hoặc mật khẩu không đúng.");

                    setLoading(false);

                    return;

                }


                saveStaffSession(result.staff);

                window.location.href = getRedirectTarget("admin/dashboard.html");

            }, 300);

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
