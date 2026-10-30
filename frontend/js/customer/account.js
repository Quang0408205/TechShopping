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
                "Ảnh đại diện phải được tải lên từ trang này.",

            dateOfBirth:
                "Ngày sinh phải là một ngày trong quá khứ.",

            gender:
                "Vui lòng chọn Nam, Nữ hoặc Khác.",

            address:
                "Địa chỉ tối đa 255 ký tự.",

            ward:
                "Vui lòng chọn phường / xã trong danh sách.",

            city:
                "Vui lòng chọn tỉnh / thành phố trong danh sách.",

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


        const AVATAR_TYPES = ["image/jpeg", "image/png", "image/webp"];

        const AVATAR_MAX_BYTES = 2 * 1024 * 1024;

        const TABS = ["profile", "loyalty", "security", "address"];

        /* Lịch sử điểm: trang đang xem, 8 dòng / lần */
        const HISTORY_SIZE = 8;

        let historyPage = 0;

        const TX_LABELS = {
            EARN: "Tích điểm",
            REDEEM: "Dùng điểm",
            REDEEM_BACK: "Hoàn điểm",
            REVOKE: "Trừ điểm",
            ADJUST: "Điều chỉnh"
        };

        /* Tài khoản đang hiển thị (GET / PUT /users/me), để lưu tên / SĐT mà không đụng tới ảnh và ngược lại */
        let currentUser = null;

        /* Hồ sơ đang hiển thị: giữ lại các trường không còn ô nhập (địa chỉ 1 dòng kiểu cũ) */
        let currentProfile = null;


        setupTabs();

        setupAvatar();

        const area = setupVnAddress(document.getElementById("city"), document.getElementById("ward"));

        accountForm.addEventListener("submit", saveAccount);

        profileForm.addEventListener("submit", saveProfile);

        passwordForm.addEventListener("submit", changePassword);

        document.getElementById("loyaltyMoreBtn").addEventListener("click", function () {
            loadHistory(historyPage + 1);
        });


        await loadAccount();

        loadLoyalty();


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

            currentUser = user;

            renderAvatar(user);

            updateSessionUser(user);

            document.getElementById("summaryUsername").textContent =
                user.username;

            document.getElementById("memberName").textContent = user.fullname || user.username;

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
            area.setValue(data.city, data.ward);
            setValue("postalCode", data.postalCode);

            currentProfile = profile;

            document.getElementById("profileEmptyHint").hidden =
                profile !== null;

            document.getElementById("summaryPoints").textContent =
                new Intl.NumberFormat("vi-VN").format(data.loyaltyPoints || 0);

            document.getElementById("summarySpent").textContent =
                formatPrice(Number(data.totalSpent || 0));

        }


        /* ================= HẠNG & ĐIỂM ================= */

        /* Thẻ thành viên + ô thống kê + bậc hạng + cách tính điểm; lỗi thì giữ số từ hồ sơ */
        async function loadLoyalty() {

            const results = await Promise.all([
                typeof getLoyaltySummary === "function" ? getLoyaltySummary() : apiRequest("/loyalty/me", { auth: true }).catch(function () { return null; }),
                apiRequest("/orders/summary", { auth: true }).catch(function () { return null; })
            ]);

            const summary = results[0];

            const counts = results[1];

            if (counts) {
                document.getElementById("statProcessing").textContent = formatCount(counts.pending + counts.inProgress);
                document.getElementById("statDelivered").textContent = formatCount(counts.delivered);
            }

            if (!summary) {
                document.getElementById("memberProgressText").textContent = "Chưa tải được hạng thành viên.";
                return;
            }

            renderMemberCard(summary);

            renderTierLadder(summary);

            renderRules(summary.rules);

            loadHistory(0);

        }


        function renderMemberCard(summary) {

            const tier = summary.tier;

            const card = document.getElementById("memberCard");

            card.className = "member-card member-card--" + String(tier.code).toLowerCase();

            document.getElementById("memberTierName").textContent = tier.name;

            document.getElementById("summaryPoints").textContent = formatCount(summary.points);

            document.getElementById("memberPointsValue").textContent = "Dùng như " + formatPrice(Number(summary.pointsValue)) + " khi mua hàng";

            document.getElementById("summarySpent").textContent = formatPrice(Number(summary.totalSpent));

            document.getElementById("statMultiplier").textContent = "x" + multiplierText(tier.pointMultiplier);

            const next = summary.nextTier;

            document.getElementById("memberProgressText").textContent = next
                ? "Còn " + formatPrice(Number(summary.amountToNextTier)) + " để lên hạng " + next.name
                : "Bạn đang ở hạng cao nhất";

            document.getElementById("memberProgressPercent").textContent = next ? summary.progressPercent + "%" : "";

            document.getElementById("memberProgressFill").style.width = (next ? summary.progressPercent : 100) + "%";

            document.getElementById("memberProgressBar").setAttribute("aria-valuenow", String(next ? summary.progressPercent : 100));

        }


        function renderTierLadder(summary) {

            const current = summary.tier;

            document.getElementById("tierLadder").innerHTML = summary.tiers.map(function (tier) {

                const reached = tier.sortOrder <= current.sortOrder;

                const mine = tier.code === current.code;

                return `
                    <div class="tier-step tier-step--${escapeHtml(String(tier.code).toLowerCase())}${mine ? " is-current" : ""}${reached ? " is-reached" : ""}" data-tier="${escapeHtml(tier.code)}">
                        <span class="tier-step-medal" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="m12 2.5 2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.1 1.2-6.5-4.8-4.6 6.6-.9Z"/></svg>
                        </span>
                        <strong>${escapeHtml(tier.name)}</strong>
                        <span class="tier-step-min">${Number(tier.minSpent) > 0 ? "Từ " + escapeHtml(formatPrice(Number(tier.minSpent))) : "Khi đăng ký"}</span>
                        <span class="tier-step-rate">Tích điểm x${escapeHtml(multiplierText(tier.pointMultiplier))}</span>
                        ${mine ? `<em class="tier-step-you">Hạng của bạn</em>` : ""}
                    </div>
                `;

            }).join("");

        }


        function renderRules(rules) {

            const earn = formatPrice(Number(rules.vndPerPointEarned));

            const spend = formatPrice(Number(rules.vndPerPointSpent));

            const items = [
                ["Tích điểm", "Mỗi " + earn + " tiền hàng thực trả (không tính phí giao hàng) = 1 điểm, nhân hệ số của hạng. Điểm được cộng khi đơn đã giao."],
                ["Dùng điểm", "1 điểm = " + spend + ". Mỗi đơn dùng tối đa " + rules.maxRedeemPercent + "% giá trị hàng (sau voucher), dùng cùng voucher được."],
                ["Huỷ / trả hàng", "Đơn huỷ được hoàn lại điểm đã dùng. Trả hàng: hoàn điểm đã dùng cho phần trả và trừ điểm đã tích tương ứng."],
                ["Lên hạng", "Hạng tính theo tổng chi tiêu các đơn đã giao. Hạng đã đạt được giữ nguyên, không bị hạ."]
            ];

            document.getElementById("loyaltyRules").innerHTML = items.map(function (item, index) {
                return `<li><span class="loyalty-rule-no">${index + 1}</span><div><strong>${escapeHtml(item[0])}</strong><p>${escapeHtml(item[1])}</p></div></li>`;
            }).join("");

        }


        async function loadHistory(page) {

            const box = document.getElementById("loyaltyHistory");

            const more = document.getElementById("loyaltyMoreBtn");

            let result;

            try {

                result = await apiRequest("/loyalty/me/history?page=" + page + "&size=" + HISTORY_SIZE, { auth: true });

            } catch (error) {

                if (page === 0) {
                    box.innerHTML = `<div class="state-box"><p>${escapeHtml(getErrorMessage(error))}</p></div>`;
                }

                return;

            }

            historyPage = page;

            const rows = (result.content || []).map(function (tx) {

                const plus = tx.points > 0;

                const order = tx.orderId
                    ? `<a href="${escapeHtml(getOrderDetailUrl(tx.orderId))}">${escapeHtml(tx.orderCode)}</a>`
                    : "";

                return `
                    <li class="loyalty-tx">
                        <span class="loyalty-tx-icon loyalty-tx-icon--${plus ? "plus" : "minus"}" aria-hidden="true">${plus ? "+" : "−"}</span>
                        <div class="loyalty-tx-main">
                            <strong>${escapeHtml(TX_LABELS[tx.type] || tx.type)}</strong>
                            <span>${escapeHtml(tx.note || "")}${order ? " · " + order : ""}</span>
                            <small>${escapeHtml(formatOrderDateTime(tx.createdAt))}</small>
                        </div>
                        <div class="loyalty-tx-points">
                            <strong class="${plus ? "is-plus" : "is-minus"}">${plus ? "+" : "−"}${escapeHtml(formatCount(Math.abs(tx.points)))}</strong>
                            <small>Còn ${escapeHtml(formatCount(tx.balanceAfter))}</small>
                        </div>
                    </li>
                `;

            }).join("");

            if (page === 0) {

                box.innerHTML = rows
                    ? `<ul class="loyalty-tx-list">${rows}</ul>`
                    : `<div class="loyalty-empty"><strong>Chưa có điểm nào</strong><p>Điểm được cộng khi đơn hàng của bạn đã giao.</p>
                        <a class="btn btn-dark" href="${escapeHtml(siteUrl("customer/products.html"))}">Mua sắm ngay</a></div>`;

            } else {

                box.querySelector(".loyalty-tx-list").insertAdjacentHTML("beforeend", rows);

            }

            more.hidden = result.page + 1 >= result.totalPages;

        }


        function formatCount(value) {

            return new Intl.NumberFormat("vi-VN").format(Number(value) || 0);

        }


        function multiplierText(value) {

            return String(Number(value)).replace(".", ",");

        }


        /* ================= THÔNG TIN TÀI KHOẢN ================= */

        async function saveAccount(event) {

            event.preventDefault();

            clearMessages(accountForm);


            const data = {
                fullname: valueOf("fullname").trim(),
                phone: valueOf("phone").trim()
            };


            const invalid = [];

            if (!data.fullname || data.fullname.length > 120) {
                invalid.push("fullname");
            }

            if (data.phone.length > 20) {
                invalid.push("phone");
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
                            avatarUrl: currentUser ? currentUser.avatarUrl || null : null
                        }
                    }
                );

                fillAccount(user);

                showSuccess(accountForm, "Đã lưu thông tin tài khoản.");

            });

        }


        /*
         * Tên / ảnh mới được nhớ trong phiên đăng nhập (poy_auth.user) và hiện ngay trên header + thanh bên,
         * không cần tải lại trang.
         */

        function updateSessionUser(user) {

            const auth = getAuth();

            if (!auth || !auth.user) {
                return;
            }

            if (auth.user.fullname === user.fullname && (auth.user.avatarUrl || null) === (user.avatarUrl || null)) {
                return;
            }

            auth.user = Object.assign({}, auth.user, { fullname: user.fullname, avatarUrl: user.avatarUrl || null });

            saveAuth(auth);

            renderAuthState();

            if (typeof renderAccountSidebar === "function" && document.querySelector("#accountSidebarRoot .account-sidebar")) {
                renderAccountSidebar();
                markActiveTab(currentTab());
            }

        }


        /* ================= KHUNG THEO HASH: #profile / #security / #address ================= */

        function currentTab() {

            const hash = window.location.hash.replace("#", "");

            return TABS.indexOf(hash) !== -1 ? hash : "profile";

        }


        function setupTabs() {

            showTab(currentTab());

            window.addEventListener("hashchange", function () {
                showTab(currentTab());
            });

        }


        function showTab(tab) {

            document.querySelectorAll("[data-account-panel]").forEach(function (panel) {
                panel.hidden = panel.dataset.accountPanel !== tab;
            });

            markActiveTab(tab);

        }


        function markActiveTab(tab) {

            document.body.dataset.accountPage = tab;

            document.querySelectorAll("#accountSidebarRoot [data-account-page]").forEach(function (link) {
                link.classList.toggle("active", link.dataset.accountPage === tab);
                if (link.dataset.accountPage === tab) {
                    link.setAttribute("aria-current", "page");
                } else {
                    link.removeAttribute("aria-current");
                }
            });

            if (typeof revealActiveAccountTab === "function") {
                revealActiveAccountTab();
            }

        }


        /* ================= ẢNH ĐẠI DIỆN ================= */

        function renderAvatar(user, previewUrl) {

            const box = document.getElementById("accountAvatar");

            const url = previewUrl || user.avatarUrl;

            box.innerHTML = url && (previewUrl || isSafeImageUrl(url))
                ? `<img src="${escapeHtml(url)}" alt="">`
                : `<span>${escapeHtml(userInitials(user.fullname || user.username))}</span>`;

            const img = box.querySelector("img");

            if (img) {
                img.addEventListener("error", function () {
                    box.innerHTML = `<span>${escapeHtml(userInitials(user.fullname || user.username))}</span>`;
                });
            }

            document.getElementById("avatarName").textContent = user.fullname || user.username || "";

            /* thẻ thành viên dùng chung ảnh vừa chọn / ảnh đã lưu */
            document.getElementById("memberAvatar").innerHTML = box.innerHTML;

            document.getElementById("avatarRemoveBtn").hidden = !user.avatarUrl;

        }


        function setupAvatar() {

            const input = document.getElementById("avatarFile");

            document.getElementById("avatarChangeBtn").addEventListener("click", function () {
                input.click();
            });

            input.addEventListener("change", function () {

                const file = input.files && input.files[0];

                input.value = "";

                if (file) {
                    uploadAvatar(file);
                }

            });

            document.getElementById("avatarRemoveBtn").addEventListener("click", function () {
                saveAvatar(null, "Đã xoá ảnh đại diện.");
            });

        }


        function avatarMessage(error, success) {

            const errorBox = document.getElementById("avatarError");

            const successBox = document.getElementById("avatarSuccess");

            errorBox.textContent = error || "";

            errorBox.hidden = !error;

            successBox.textContent = success || "";

            successBox.hidden = !success;

        }


        async function uploadAvatar(file) {

            avatarMessage(null, null);

            if (AVATAR_TYPES.indexOf(file.type) === -1) {
                avatarMessage("Chỉ nhận ảnh JPG, PNG hoặc WebP.");
                return;
            }

            if (file.size > AVATAR_MAX_BYTES) {
                avatarMessage("Ảnh đại diện tối đa 2 MB (ảnh này " + (file.size / 1024 / 1024).toFixed(1).replace(".", ",") + " MB).");
                return;
            }


            const preview = URL.createObjectURL(file);

            renderAvatar(currentUser || {}, preview);

            const button = document.getElementById("avatarChangeBtn");

            button.disabled = true;

            button.textContent = "Đang tải ảnh…";


            try {

                const form = new FormData();

                form.append("file", file);

                const uploaded = await apiRequest("/uploads/avatar", { method: "POST", body: form, auth: true });

                await saveAvatar(uploaded.url, "Đã đổi ảnh đại diện.");

            } catch (error) {

                renderAvatar(currentUser || {});

                if (!handleSessionError(error)) {
                    avatarMessage(error.code === "IMAGE_TOO_LARGE" ? "Ảnh đại diện tối đa 2 MB." : getErrorMessage(error));
                }

            } finally {

                URL.revokeObjectURL(preview);

                button.disabled = false;

                button.textContent = "Đổi ảnh";

            }

        }


        /* PUT /users/me với tên / SĐT đang lưu (không lấy chữ đang gõ dở trong form) và ảnh mới */

        async function saveAvatar(url, message) {

            if (!currentUser) {
                return;
            }

            try {

                const user = await apiRequest("/users/me", {
                    method: "PUT",
                    auth: true,
                    body: { fullname: currentUser.fullname, phone: currentUser.phone || null, avatarUrl: url }
                });

                currentUser = user;

                renderAvatar(user);

                updateSessionUser(user);

                avatarMessage(null, message);

            } catch (error) {

                renderAvatar(currentUser);

                if (!handleSessionError(error)) {
                    avatarMessage(getErrorMessage(error));
                }

            }

        }


        /* ================= HỒ SƠ KHÁCH HÀNG ================= */

        async function saveProfile(event) {

            event.preventDefault();

            clearMessages(profileForm);


            const picked = area.getValue();

            /* địa chỉ 2 cấp: không còn quận / huyện; địa chỉ 1 dòng kiểu cũ giữ nguyên giá trị đang có */
            const data = {
                dateOfBirth: valueOf("dateOfBirth"),
                gender: valueOf("gender"),
                address: valueOf("address").trim(),
                ward: picked.ward,
                district: "",
                city: picked.province,
                postalCode: valueOf("postalCode").trim(),
                defaultShippingAddress: (currentProfile && currentProfile.defaultShippingAddress) || ""
            };


            const invalid = [];

            /* Chuỗi "yyyy-mm-dd" so sánh được trực tiếp */
            if (data.dateOfBirth && data.dateOfBirth >= todayIso()) {
                invalid.push("dateOfBirth");
            }

            /* đã gõ nhưng không chọn đúng tên trong danh sách */
            if (valueOf("city").trim() && !data.city) {
                invalid.push("city");
            }

            if (valueOf("ward").trim() && !data.ward) {
                invalid.push("ward");
            }

            const maxLengths = {
                address: 255, postalCode: 20
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
