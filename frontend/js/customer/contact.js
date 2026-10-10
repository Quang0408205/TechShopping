/* ================= FORM LIÊN HỆ (customer/contact.html) ================= */

/*
 * POST /api/v1/contact-requests (công khai; đã đăng nhập thì gửi kèm token để gắn tài khoản).
 * Chỉ báo thành công khi API trả 201. Lỗi theo ô (details) hiện dưới ô đó; 429 = gửi quá nhiều lần.
 */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        const form = document.getElementById("contactForm");

        if (!form) {
            return;
        }


        const MESSAGE_MIN = 10;

        const MESSAGE_MAX = 2000;

        const PHONE_PATTERN = /^(0|\+84)[0-9]{9,10}$/;

        const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;


        const successBox = document.getElementById("contactSuccess");

        const errorBox = document.getElementById("contactError");

        const button = form.querySelector("button[type=submit]");

        const counter = document.getElementById("contactMessageCount");

        const fields = {
            fullName: document.getElementById("contactName"),
            email: document.getElementById("contactEmail"),
            phone: document.getElementById("contactPhone"),
            topic: document.getElementById("contactTopic"),
            message: document.getElementById("contactMessage")
        };


        prefillFromAccount();

        updateCounter();

        fields.message.addEventListener("input", updateCounter);

        Object.keys(fields).forEach(function (name) {

            fields[name].addEventListener("input", function () {
                showFieldError(name, "");
            });

        });


        form.addEventListener("submit", async function (event) {

            event.preventDefault();

            successBox.hidden = true;

            errorBox.hidden = true;


            const body = {
                fullName: fields.fullName.value.trim(),
                email: fields.email.value.trim(),
                phone: fields.phone.value.trim() || null,
                topic: fields.topic.value || null,
                message: fields.message.value.trim()
            };

            const errors = validate(body);

            Object.keys(fields).forEach(function (name) {
                showFieldError(name, errors[name] || "");
            });

            const firstInvalid = Object.keys(fields).find(function (name) { return errors[name]; });

            if (firstInvalid) {

                fields[firstInvalid].focus();

                return;

            }


            button.disabled = true;

            button.textContent = "Đang gửi...";

            try {

                const created = await send(body);

                successBox.textContent =
                    "Cảm ơn " + body.fullName + "! POY đã nhận tin nhắn của bạn (mã #" + created.id + "). " +
                    "Nhân viên sẽ liên hệ lại qua email" + (body.phone ? " hoặc số điện thoại" : "") + " bạn đã để lại.";

                successBox.hidden = false;

                form.reset();

                prefillFromAccount();

                updateCounter();

            } catch (error) {

                const details = error && error.details ? error.details : null;

                if (error && error.status === 400 && details) {

                    Object.keys(fields).forEach(function (name) {
                        showFieldError(name, details[name] || "");
                    });

                }

                errorBox.textContent = error && error.status === 400 && details
                    ? "Vui lòng kiểm tra lại các ô được đánh dấu."
                    : getErrorMessage(error);

                errorBox.hidden = false;

            } finally {

                button.disabled = false;

                button.textContent = "Gửi liên hệ";

            }

        });


        /* Token hỏng / hết hạn mà không làm mới được: gửi lại như khách chưa đăng nhập */

        async function send(body) {

            const options = { method: "POST", body: body, auth: isLoggedIn() };

            try {

                return await apiRequest("/contact-requests", options);

            } catch (error) {

                if (options.auth && error && error.status === 401) {
                    return apiRequest("/contact-requests", { method: "POST", body: body });
                }

                throw error;

            }

        }


        function validate(body) {

            const errors = {};

            if (body.fullName.length < 2) {
                errors.fullName = "Vui lòng nhập họ tên (ít nhất 2 ký tự).";
            }

            if (!body.email) {
                errors.email = "Vui lòng nhập email.";
            } else if (!EMAIL_PATTERN.test(body.email)) {
                errors.email = "Email chưa hợp lệ.";
            }

            if (body.phone && !PHONE_PATTERN.test(body.phone.replace(/[\s.-]/g, ""))) {
                errors.phone = "Số điện thoại chưa hợp lệ (ví dụ 0912345678).";
            } else if (body.phone) {
                body.phone = body.phone.replace(/[\s.-]/g, "");
            }

            if (!body.topic) {
                errors.topic = "Vui lòng chọn chủ đề.";
            }

            if (body.message.length < MESSAGE_MIN || body.message.length > MESSAGE_MAX) {
                errors.message = "Nội dung từ " + MESSAGE_MIN + " đến " + MESSAGE_MAX + " ký tự.";
            }

            return errors;

        }


        function showFieldError(name, message) {

            const slot = form.querySelector('.field-error[data-field="' + name + '"]');

            if (slot) {
                slot.textContent = message;
            }

            if (message) {
                fields[name].setAttribute("aria-invalid", "true");
            } else {
                fields[name].removeAttribute("aria-invalid");
            }

        }


        function updateCounter() {

            counter.textContent = fields.message.value.length + " / " + MESSAGE_MAX;

        }


        /* Đã đăng nhập: điền sẵn họ tên + email (vẫn sửa được) */

        function prefillFromAccount() {

            const user = typeof getCurrentUser === "function" && isLoggedIn() ? getCurrentUser() : null;

            if (!user) {
                return;
            }

            if (!fields.fullName.value && user.fullname) {
                fields.fullName.value = user.fullname;
            }

            if (!fields.email.value && user.email) {
                fields.email.value = user.email;
            }

        }

    }
);
