/*
 * Nút "Đăng nhập với Google" cho trang đăng nhập và đăng ký.
 * Chỉ hiện khi backend bật Google (GET /auth/google/config có clientId). Google Identity Services trả ID token,
 * backend xác minh rồi cấp phiên như đăng nhập thường.
 */
document.addEventListener("DOMContentLoaded", function () {

    const box = document.getElementById("googleSignIn");

    if (!box) {
        return;
    }

    const errorBox = document.getElementById(box.dataset.errorTarget);

    apiRequest("/auth/google/config")
        .then(function (config) {

            if (config && config.enabled && config.clientId) {
                loadGoogleScript(config.clientId);
            }

        })
        .catch(function () {
            /* không lấy được cấu hình thì ẩn nút, đăng nhập bằng mật khẩu vẫn dùng được */
        });


    function loadGoogleScript(clientId) {

        const script = document.createElement("script");

        script.src = "https://accounts.google.com/gsi/client";

        script.async = true;

        script.onload = function () { renderButton(clientId); };

        document.head.appendChild(script);

    }


    function renderButton(clientId) {

        window.google.accounts.id.initialize({
            client_id: clientId,
            callback: onCredential
        });

        window.google.accounts.id.renderButton(
            document.getElementById("googleSignInButton"),
            { theme: "outline", size: "large", text: "continue_with", shape: "rectangular", width: 320, locale: "vi" }
        );

        box.hidden = false;

    }


    async function onCredential(response) {

        hideError();

        try {

            const auth = await apiRequest("/auth/google", {
                method: "POST",
                body: { credential: response.credential }
            });

            saveAuth(auth);

            window.location.href = await pageAfterLogin("index.html");

        } catch (error) {

            showError(getErrorMessage(error));

        }

    }


    function showError(message) {

        if (errorBox) {
            errorBox.textContent = message;
            errorBox.hidden = false;
        }

    }


    function hideError() {

        if (errorBox) {
            errorBox.textContent = "";
            errorBox.hidden = true;
        }

    }

});
