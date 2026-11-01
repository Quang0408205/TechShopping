/* ================= BANNER TRANG CHỦ (admin/banners.html, chỉ ADMIN) ================= */

/*
 * API: GET / POST /admin/banners, PUT / DELETE /admin/banners/{id}, ảnh tải lên qua POST /admin/uploads/product-images.
 */

document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }

    const panel = document.getElementById("bannerFormPanel");
    const form = document.getElementById("bannerForm");
    const formError = document.getElementById("bannerFormError");
    const formTitle = document.getElementById("bannerFormTitle");
    const tbody = document.getElementById("bannerTableBody");
    const rowCount = document.getElementById("bannerRowCount");
    const image = document.getElementById("bannerImage");
    const preview = document.getElementById("bannerImagePreview");

    let banners = [];
    let editingId = null;
    let saving = false;


    document.getElementById("bannerNewBtn").addEventListener("click", function () { openForm(null); });

    document.getElementById("bannerCancelBtn").addEventListener("click", closeForm);

    form.addEventListener("submit", save);

    image.addEventListener("input", showPreview);

    document.getElementById("bannerImageFile").addEventListener("change", async function (event) {

        const file = event.target.files[0];

        event.target.value = "";

        if (!file) {
            return;
        }

        const data = new FormData();

        data.append("file", file);

        try {

            const uploaded = await apiRequest("/admin/uploads/product-images", { method: "POST", body: data, auth: true });

            image.value = uploaded.url;

            showPreview();

        } catch (error) {

            showFormError(getErrorMessage(error));

        }

    });

    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const banner = banners.find(function (item) { return String(item.id) === button.dataset.id; });

        if (!banner) {
            return;
        }

        if (button.dataset.action === "edit") {
            openForm(banner);
        } else if (button.dataset.action === "toggle") {
            toggle(banner);
        } else if (button.dataset.action === "delete") {
            openConfirmModal({
                title: "Xoá banner",
                message: "Xoá banner \"" + banner.title + "\"? Không hoàn tác được.",
                confirmLabel: "Xoá",
                onConfirm: function () { remove(banner); }
            });
        }

    });

    load();


    async function load() {

        tbody.innerHTML = adminSkeletonRow(6);

        try {

            banners = await apiRequest("/admin/banners", { auth: true }) || [];

            render();

        } catch (error) {

            handleError(error, function (message) { tbody.innerHTML = adminEmptyRow(6, message); });

        }

    }


    function render() {

        rowCount.textContent = banners.length + " banner";

        tbody.innerHTML = banners.length === 0
            ? adminEmptyRow(6, "Chưa có banner nào. Bấm \"Thêm banner\" để tạo.")
            : banners.map(rowHtml).join("");

    }


    function rowHtml(banner) {

        const id = escapeHtml(String(banner.id));

        const window_ = banner.startsAt || banner.endsAt
            ? escapeHtml((banner.startsAt ? formatDateTimeVi(banner.startsAt) : "…") + " → " + (banner.endsAt ? formatDateTimeVi(banner.endsAt) : "…"))
            : "Luôn hiện";

        return `
            <tr>
                <td><img class="admin-tax-image-preview" src="${escapeHtml(banner.imageUrl)}" alt=""></td>
                <td>
                    <strong>${escapeHtml(banner.title)}</strong>
                    ${banner.subtitle ? `<div class="admin-subtext">${escapeHtml(banner.subtitle)}</div>` : ""}
                    ${banner.linkUrl ? `<div class="admin-subtext">→ ${escapeHtml(banner.linkUrl)}</div>` : ""}
                </td>
                <td>${banner.displayOrder}</td>
                <td>${window_}</td>
                <td><span class="admin-badge ${banner.active ? "admin-badge-success" : "admin-badge-neutral"}">${banner.active ? "Đang bật" : "Đã tắt"}</span></td>
                <td>
                    <button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>
                    <button type="button" class="admin-link-btn" data-action="toggle" data-id="${id}">${banner.active ? "Tắt" : "Bật"}</button>
                    <button type="button" class="admin-link-btn admin-link-danger" data-action="delete" data-id="${id}">Xoá</button>
                </td>
            </tr>
        `;

    }


    function openForm(banner) {

        editingId = banner ? banner.id : null;

        formTitle.textContent = banner ? "Sửa banner" : "Thêm banner";

        document.getElementById("bannerTitle").value = banner ? banner.title : "";
        document.getElementById("bannerSubtitle").value = banner && banner.subtitle ? banner.subtitle : "";
        image.value = banner ? banner.imageUrl : "";
        document.getElementById("bannerLink").value = banner && banner.linkUrl ? banner.linkUrl : "";
        document.getElementById("bannerOrder").value = banner ? banner.displayOrder : 0;
        document.getElementById("bannerStarts").value = banner && banner.startsAt ? banner.startsAt.slice(0, 16) : "";
        document.getElementById("bannerEnds").value = banner && banner.endsAt ? banner.endsAt.slice(0, 16) : "";
        document.getElementById("bannerActive").checked = banner ? banner.active : true;

        formError.hidden = true;

        showPreview();

        panel.hidden = false;

        document.getElementById("bannerTitle").focus();

    }


    function closeForm() {

        panel.hidden = true;

        editingId = null;

    }


    function showPreview() {

        const url = image.value.trim();

        preview.hidden = !/^https?:\/\//i.test(url);

        if (!preview.hidden) {
            preview.src = url;
        }

    }


    function showFormError(message) {

        formError.textContent = message;

        formError.hidden = false;

    }


    async function save(event) {

        event.preventDefault();

        if (saving) {
            return;
        }

        const title = document.getElementById("bannerTitle").value.trim();
        const imageUrl = image.value.trim();
        const starts = document.getElementById("bannerStarts").value;
        const ends = document.getElementById("bannerEnds").value;

        if (!title) {
            showFormError("Vui lòng nhập tiêu đề");
            return;
        }

        if (!/^https?:\/\//i.test(imageUrl)) {
            showFormError("Vui lòng chọn ảnh banner (link http(s) hoặc tải ảnh lên)");
            return;
        }

        if (starts && ends && ends <= starts) {
            showFormError("Thời gian ngừng hiện phải sau thời gian bắt đầu");
            return;
        }

        const body = {
            title: title,
            subtitle: document.getElementById("bannerSubtitle").value.trim(),
            imageUrl: imageUrl,
            linkUrl: document.getElementById("bannerLink").value.trim(),
            displayOrder: Number(document.getElementById("bannerOrder").value) || 0,
            active: document.getElementById("bannerActive").checked,
            startsAt: starts ? starts + ":00" : null,
            endsAt: ends ? ends + ":00" : null
        };

        saving = true;

        try {

            await apiRequest(editingId === null ? "/admin/banners" : "/admin/banners/" + editingId, {
                method: editingId === null ? "POST" : "PUT",
                body: body,
                auth: true
            });

            showToast("Đã lưu banner", "success");

            closeForm();

            await load();

        } catch (error) {

            handleError(error, showFormError);

        } finally {

            saving = false;

        }

    }


    async function toggle(banner) {

        try {

            await apiRequest("/admin/banners/" + banner.id, {
                method: "PUT",
                body: {
                    title: banner.title,
                    subtitle: banner.subtitle || "",
                    imageUrl: banner.imageUrl,
                    linkUrl: banner.linkUrl || "",
                    displayOrder: banner.displayOrder,
                    active: !banner.active,
                    startsAt: banner.startsAt,
                    endsAt: banner.endsAt
                },
                auth: true
            });

            await load();

        } catch (error) {

            handleError(error, function (message) { showToast(message, "error"); });

        }

    }


    async function remove(banner) {

        try {

            await apiRequest("/admin/banners/" + banner.id, { method: "DELETE", auth: true });

            showToast("Đã xoá banner", "success");

            await load();

        } catch (error) {

            handleError(error, function (message) { showToast(message, "error"); });

        }

    }


    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
