document.addEventListener("DOMContentLoaded", function () {

    const form = document.getElementById("resetForm");
    const errorBox = document.getElementById("resetError");
    const doneBox = document.getElementById("resetDone");
    const button = document.getElementById("resetButton");
    const token = new URLSearchParams(window.location.search).get("token");

    /* bỏ token khỏi thanh địa chỉ để không nằm lại trong lịch sử / ảnh chụp màn hình */
    if (token) {
        history.replaceState(null, "", window.location.pathname);
    }

    if (!token) {
        showError("Liên kết không hợp lệ. Hãy yêu cầu gửi lại liên kết đặt lại mật khẩu.");
        form.hidden = true;
        return;
    }

    form.addEventListener("submit", async function (event) {

        event.preventDefault();

        const password = document.getElementById("newPassword").value;
        const confirm = document.getElementById("confirmPassword").value;

        errorBox.hidden = true;

        if (password.length < 8) {
            showError("Mật khẩu phải có ít nhất 8 ký tự.");
            return;
        }

        if (new TextEncoder().encode(password).length > 72) {
            showError("Mật khẩu quá dài (tối đa 72 byte).");
            return;
        }

        if (password !== confirm) {
            showError("Hai mật khẩu không khớp.");
            return;
        }

        button.disabled = true;
        button.textContent = "Đang lưu...";

        try {

            await apiRequest("/auth/reset-password", {
                method: "POST",
                body: { token: token, newPassword: password }
            });

            form.hidden = true;
            doneBox.hidden = false;

        } catch (error) {

            showError(getErrorMessage(error));
            button.disabled = false;
            button.textContent = "Đổi mật khẩu";

        }

    });

    function showError(message) {
        errorBox.textContent = message;
        errorBox.hidden = false;
    }

});
