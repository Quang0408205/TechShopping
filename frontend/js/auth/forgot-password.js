document.addEventListener("DOMContentLoaded", function () {

    const form = document.getElementById("forgotForm");
    const emailInput = document.getElementById("forgotEmail");
    const errorBox = document.getElementById("forgotError");
    const doneBox = document.getElementById("forgotDone");
    const button = document.getElementById("forgotButton");

    form.addEventListener("submit", async function (event) {

        event.preventDefault();

        const email = emailInput.value.trim();

        errorBox.hidden = true;

        doneBox.hidden = true;

        if (!email || !emailInput.checkValidity()) {

            errorBox.textContent = "Vui lòng nhập một địa chỉ email hợp lệ.";

            errorBox.hidden = false;

            return;

        }

        button.disabled = true;

        button.textContent = "Đang gửi...";

        try {

            await apiRequest("/auth/forgot-password", { method: "POST", body: { email: email } });

            doneBox.hidden = false;

            /* chặn bấm lại liên tục; muốn gửi lại thì tải lại trang */
            button.textContent = "Đã gửi";

        } catch (error) {

            errorBox.textContent = getErrorMessage(error);

            errorBox.hidden = false;

            button.disabled = false;

            button.textContent = "Gửi liên kết";

        }

    });

});
