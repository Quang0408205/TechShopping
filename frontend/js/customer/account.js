document.addEventListener(
    "DOMContentLoaded",
    async function () {

        /* Trang chỉ dành cho người đã đăng nhập */

        if (!isLoggedIn()) {

            redirectToLogin();

            return;

        }


        const accountForm =
            document.getElementById("accountForm");

        const profileForm =
            document.getElementById("profileForm");

        const passwordForm =
            document.getElementById("passwordForm");

        const statusBox =
            document.getElementById("accountStatus");


        /* Cùng ràng buộc với UserUpdateRequest, CustomerProfileUpdateRequest, ChangePasswordRequest */

        const FIELD_MESSAGES = {

            fullname:
                "Vui lòng nhập họ và tên (tối đa 120 ký tự).",

            phone:
                "Số điện thoại tối đa 20 ký tự.",

            avatarUrl:
                "Đường dẫn ảnh tối đa 2048 ký tự.",

            dateOfBirth:
                "Ngày sinh phải là một ngày trong quá khứ.",

            gender:
                "Vui lòng chọn Nam, Nữ hoặc Khác.",

            address:
                "Địa chỉ tối đa 255 ký tự.",

            ward:
                "Phường / Xã tối đa 100 ký tự.",

            district:
                "Quận / Huyện tối đa 100 ký tự.",

            city:
                "Tỉnh / Thành phố tối đa 100 ký tự.",

            postalCode:
                "Mã bưu chính tối đa 20 ký tự.",

            defaultShippingAddress:
                "Địa chỉ giao hàng tối đa 1000 ký tự.",

            currentPassword:
                "Vui lòng nhập mật khẩu hiện tại.",

            newPassword:
                "Mật khẩu mới phải từ 8 đến 72 ký tự và khác mật khẩu hiện tại.",

            confirmNewPassword:
                "Mật khẩu nhập lại không khớp."

        };


        accountForm.addEventListener("submit", saveAccount);

        profileForm.addEventListener("submit", saveProfile);

        passwordForm.addEventListener("submit", changePassword);

        document.getElementById("logoutButton")
            .addEventListener(
                "click",
                async function () {

                    await logout();

                    window.location.href = siteUrl("index.html");

                }
            );


        await loadAccount();


        /* ================= TẢI DỮ LIỆU ================= */

        async function loadAccount() {

            try {

                const results = await Promise.all([
                    apiRequest("/users/me", { auth: true }),
                    loadProfile()
                ]);

                fillAccount(results[0]);

                fillProfile(results[1]);

                document.getElementById("accountSummary").hidden = false;

                document.getElementById("accountContent").hidden = false;

            } catch (error) {

                if (!handleSessionError(error)) {

                    showStatus(getErrorMessage(error));

                }

            }

        }


        /* Tài khoản chưa có hồ sơ (vd. tài khoản quản trị): trả về null */

        async function loadProfile() {

            try {

                return await apiRequest("/users/me/profile", { auth: true });

            } catch (error) {

                if (error.code === "CUSTOMER_PROFILE_NOT_FOUND") {
                    return null;
                }

                throw error;

            }

        }


        function fillAccount(user) {

            setValue("email", user.email);
            setValue("username", user.username);
            setValue("fullname", user.fullname);
            setValue("phone", user.phone);
            setValue("avatarUrl", user.avatarUrl);

            document.getElementById("summaryUsername").textContent =
                user.username;

            document.getElementById("summaryJoined").textContent =
                user.createdAt
                    ? new Date(user.createdAt).toLocaleDateString("vi-VN")
                    : "-";

        }


        function fillProfile(profile) {

            const data = profile || {};

            setValue("dateOfBirth", data.dateOfBirth);
            setValue("gender", data.gender);
            setValue("address", data.address);
            setValue("ward", data.ward);
            setValue("district", data.district);
            setValue("city", data.city);
            setValue("postalCode", data.postalCode);
            setValue("defaultShippingAddress", data.defaultShippingAddress);

            document.getElementById("profileEmptyHint").hidden =
                profile !== null;

            document.getElementById("summaryPoints").textContent =
                new Intl.NumberFormat("vi-VN").format(data.loyaltyPoints || 0);

            document.getElementById("summarySpent").textContent =
                formatPrice(Number(data.totalSpent || 0));

        }


        /* ================= THÔNG TIN TÀI KHOẢN ================= */

        async function saveAccount(event) {

            event.preventDefault();

            clearMessages(accountForm);


            const data = {
                fullname: valueOf("fullname").trim(),
                phone: valueOf("phone").trim(),
                avatarUrl: valueOf("avatarUrl").trim()
            };


            const invalid = [];

            if (!data.fullname || data.fullname.length > 120) {
                invalid.push("fullname");
            }

            if (data.phone.length > 20) {
                invalid.push("phone");
            }

            if (data.avatarUrl.length > 2048) {
                invalid.push("avatarUrl");
            }

            if (showInvalid(accountForm, invalid)) {
                return;
            }


            await submit(accountForm, async function () {

                /* PUT /api/v1/users/me: email và tên đăng nhập không đổi được */

                const user = await apiRequest(
                    "/users/me",
                    {
                        method: "PUT",
                        auth: true,
                        body: {
                            fullname: data.fullname,
                            phone: data.phone || null,
                            avatarUrl: data.avatarUrl || null
                        }
                    }
                );

                fillAccount(user);

                updateHeaderName(user);

                showSuccess(accountForm, "Đã lưu thông tin tài khoản.");

            });

        }


        /* Tên mới hiển thị ngay trên header và được nhớ trong phiên đăng nhập */

        function updateHeaderName(user) {

            const auth = getAuth();

            if (auth) {

                auth.user = Object.assign({}, auth.user, { fullname: user.fullname });

                saveAuth(auth);

            }


            document.querySelectorAll(".header .login-btn.user-name")
                .forEach(function (button) {

                    button.textContent = user.fullname || user.username;

                });

        }


        /* ================= HỒ SƠ KHÁCH HÀNG ================= */

        async function saveProfile(event) {

            event.preventDefault();

            clearMessages(profileForm);


            const data = {
                dateOfBirth: valueOf("dateOfBirth"),
                gender: valueOf("gender"),
                address: valueOf("address").trim(),
                ward: valueOf("ward").trim(),
                district: valueOf("district").trim(),
                city: valueOf("city").trim(),
                postalCode: valueOf("postalCode").trim(),
                defaultShippingAddress: valueOf("defaultShippingAddress").trim()
            };


            const invalid = [];

            /* Chuỗi "yyyy-mm-dd" so sánh được trực tiếp */
            if (data.dateOfBirth && data.dateOfBirth >= todayIso()) {
                invalid.push("dateOfBirth");
            }

            const maxLengths = {
                address: 255, ward: 100, district: 100, city: 100,
                postalCode: 20, defaultShippingAddress: 1000
            };

            Object.keys(maxLengths).forEach(function (field) {

                if (data[field].length > maxLengths[field]) {
                    invalid.push(field);
                }

            });

            if (showInvalid(profileForm, invalid)) {
                return;
            }


            /* Ô để trống gửi null (backend không nhận chuỗi rỗng cho giới tính, ngày sinh) */

            const body = {};

            Object.keys(data).forEach(function (field) {

                body[field] = data[field] || null;

            });


            await submit(profileForm, async function () {

                /* PUT /api/v1/users/me/profile: tạo mới nếu chưa có hồ sơ */

                const profile = await apiRequest(
                    "/users/me/profile",
                    {
                        method: "PUT",
                        auth: true,
                        body: body
                    }
                );

                fillProfile(profile);

                showSuccess(profileForm, "Đã lưu hồ sơ khách hàng.");

            });

        }


        /* ================= ĐỔI MẬT KHẨU ================= */

        async function changePassword(event) {

            event.preventDefault();

            clearMessages(passwordForm);


            const currentPassword = valueOf("currentPassword");

            const newPassword = valueOf("newPassword");

            const confirmNewPassword = valueOf("confirmNewPassword");


            const invalid = [];

            if (!currentPassword) {
                invalid.push("currentPassword");
            }

            /* BCrypt chỉ nhận tối đa 72 byte (chữ có dấu chiếm nhiều byte) */
            const newPasswordBytes =
                new TextEncoder().encode(newPassword).length;

            if (newPassword.length < 8 || newPasswordBytes > 72 || newPassword === currentPassword) {
                invalid.push("newPassword");
            }

            if (confirmNewPassword !== newPassword) {
                invalid.push("confirmNewPassword");
            }

            if (showInvalid(passwordForm, invalid)) {
                return;
            }


            await submit(passwordForm, async function () {

                /* PUT /api/v1/users/me/password: backend thu hồi mọi phiên đăng nhập */

                await apiRequest(
                    "/users/me/password",
                    {
                        method: "PUT",
                        auth: true,
                        body: {
                            currentPassword: currentPassword,
                            newPassword: newPassword
                        }
                    }
                );

                passwordForm.reset();

                showSuccess(
                    passwordForm,
                    "Đổi mật khẩu thành công. Vui lòng đăng nhập lại."
                );

                clearAuth();

                setTimeout(function () {

                    window.location.href = siteUrl("auth/login.html");

                }, 1500);

            }, true);

        }


        /* ================= DÙNG CHUNG ================= */

        /* Gửi form: khóa nút trong lúc chờ, hiển thị lỗi của backend */

        async function submit(form, action, keepDisabled) {

            const button = form.querySelector("button[type=submit]");

            button.disabled = true;

            let succeeded = false;

            try {

                await action();

                succeeded = true;

            } catch (error) {

                if (!handleSessionError(error)) {

                    showApiError(form, error);

                }

            } finally {

                if (!(succeeded && keepDisabled)) {
                    button.disabled = false;
                }

            }

        }


        /* Phiên hết hạn hoặc tài khoản bị khóa: không thể tiếp tục ở trang này */

        function handleSessionError(error) {

            if (error.status === 401) {

                clearAuth();

                redirectToLogin();

                return true;

            }

            if (error.code === "ACCOUNT_DISABLED") {

                clearAuth();

                document.getElementById("accountSummary").hidden = true;

                document.getElementById("accountContent").hidden = true;

                showStatus(getErrorMessage(error));

                return true;

            }

            return false;

        }


        function showApiError(form, error) {

            if (error.code === "INVALID_PASSWORD") {
                showFieldError(form, "currentPassword", getErrorMessage(error));
            }

            /* VALIDATION_ERROR: details = { tênTrường: thông báo } */
            if (error.details) {

                Object.keys(error.details).forEach(function (field) {

                    showFieldError(
                        form,
                        field,
                        FIELD_MESSAGES[field] || error.details[field]
                    );

                });

            }

            showFormMessage(form, "error", getErrorMessage(error));

        }


        function showInvalid(form, fields) {

            if (fields.length === 0) {
                return false;
            }

            fields.forEach(function (field) {

                showFieldError(form, field, FIELD_MESSAGES[field]);

            });

            showFormMessage(form, "error", API_ERROR_MESSAGES.VALIDATION_ERROR);

            return true;

        }


        function showFieldError(form, field, message) {

            const element =
                form.querySelector(`.field-error[data-field="${field}"]`);

            if (element) {
                element.textContent = message;
            }

        }


        function showSuccess(form, message) {

            showFormMessage(form, "success", message);

        }


        function showFormMessage(form, role, message) {

            const box = form.querySelector(`[data-role="${role}"]`);

            box.textContent = message;

            box.hidden = false;

        }


        function clearMessages(form) {

            form.querySelectorAll(".field-error")
                .forEach(function (element) {

                    element.textContent = "";

                });

            form.querySelectorAll("[data-role]")
                .forEach(function (box) {

                    box.textContent = "";

                    box.hidden = true;

                });

        }


        function showStatus(message) {

            statusBox.textContent = message;

            statusBox.hidden = false;

        }


        function valueOf(id) {

            return document.getElementById(id).value;

        }


        function setValue(id, value) {

            document.getElementById(id).value = value || "";

        }


        function todayIso() {

            const now = new Date();

            now.setMinutes(now.getMinutes() - now.getTimezoneOffset());

            return now.toISOString().slice(0, 10);

        }

    }
);
