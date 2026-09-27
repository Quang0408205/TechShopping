document.addEventListener(
    "DOMContentLoaded",
    function () {

        const form =
            document.getElementById("contactForm");


        if (!form) {
            return;
        }


        const successBox =
            document.getElementById("contactSuccess");

        const errorBox =
            document.getElementById("contactError");

        const button =
            form.querySelector("button[type=submit]");


        form.addEventListener(
            "submit",
            function (event) {

                event.preventDefault();


                const name =
                    document.getElementById(
                        "contactName"
                    ).value.trim();

                const email =
                    document.getElementById(
                        "contactEmail"
                    ).value.trim();

                const message =
                    document.getElementById(
                        "contactMessage"
                    ).value.trim();


                /* Form có novalidate: tự kiểm tra, báo lỗi tiếng Việt ngay trên form */

                const validationError =
                    !name || !email || !message
                        ? "Vui lòng nhập đầy đủ họ tên, email và nội dung."
                        : !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
                            ? "Email chưa hợp lệ."
                            : "";

                errorBox.textContent = validationError;

                errorBox.hidden = !validationError;

                if (validationError) {

                    successBox.hidden = true;

                    return;

                }



                /*
                 * CHỜ BACKEND: hiện chỉ hiển thị thông báo đã ghi nhận, chưa gửi đi đâu.
                 * Khi có API thật, thay bằng
                 * apiRequest("/contact-requests", { method: "POST", body: {...} }).
                 */

                button.disabled = true;

                successBox.textContent =
                    "Cảm ơn " + name + "! Tin nhắn của bạn đã được ghi nhận, " +
                    "đội ngũ POY sẽ phản hồi trong thời gian sớm nhất.";

                successBox.hidden = false;


                form.reset();


                setTimeout(function () {

                    button.disabled = false;
                    successBox.hidden = true;

                }, 4000);

            }
        );

    }
);
