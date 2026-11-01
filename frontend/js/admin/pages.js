/* ================= TRANG NỘI DUNG (admin/pages.html, chỉ ADMIN) ================= */

/*
 * API: GET /admin/pages, GET / PUT / DELETE /admin/pages/{slug}. PUT tạo trang nếu đường dẫn chưa có;
 * backend lọc HTML trước khi lưu, nên sau khi lưu ô nội dung được nạp lại bằng bản đã lọc.
 */

document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }

    const panel = document.getElementById("pageFormPanel");
    const form = document.getElementById("pageForm");
    const formError = document.getElementById("pageFormError");
    const formTitle = document.getElementById("pageFormTitle");
    const tbody = document.getElementById("pageTableBody");
    const rowCount = document.getElementById("pageRowCount");
    const titleInput = document.getElementById("pageTitleInput");
    const slugInput = document.getElementById("pageSlug");
    const bodyInput = document.getElementById("pageBody");
    const published = document.getElementById("pagePublished");

    let pages = [];
    let editingSlug = null;
    let saving = false;


    document.getElementById("pageNewBtn").addEventListener("click", function () { openForm(null); });

    document.getElementById("pageCancelBtn").addEventListener("click", closeForm);

    form.addEventListener("submit", save);

    document.getElementById("pageTools").addEventListener("click", function (event) {

        const button = event.target.closest("button[data-wrap]");

        if (button) {
            wrapSelection(button.dataset.wrap);
        }

    });

    tbody.addEventListener("click", async function (event) {

        const button = event.target.closest("button[data-action]");

        if (!button) {
            return;
        }

        const slug = button.dataset.slug;

        if (button.dataset.action === "edit") {

            try {
                openForm(await apiRequest("/admin/pages/" + encodeURIComponent(slug), { auth: true }));
            } catch (error) {
                handleError(error, function (message) { showToast(message, "error"); });
            }

        } else if (button.dataset.action === "delete") {

            openConfirmModal({
                title: "Xoá trang",
                message: "Xoá trang \"" + slug + "\"? Các liên kết tới trang này sẽ báo không tìm thấy.",
                confirmLabel: "Xoá",
                onConfirm: function () { remove(slug); }
            });

        }

    });

    load();


    async function load() {

        tbody.innerHTML = adminSkeletonRow(5);

        try {

            pages = await apiRequest("/admin/pages", { auth: true }) || [];

            rowCount.textContent = pages.length + " trang";

            tbody.innerHTML = pages.length === 0
                ? adminEmptyRow(5, "Chưa có trang nào.")
                : pages.map(rowHtml).join("");

        } catch (error) {

            handleError(error, function (message) { tbody.innerHTML = adminEmptyRow(5, message); });

        }

    }


    function rowHtml(page) {

        const slug = escapeHtml(page.slug);

        return `
            <tr>
                <td><strong>${escapeHtml(page.title)}</strong></td>
                <td><code>${slug}</code></td>
                <td>${escapeHtml(formatDateTimeVi(page.updatedAt))}</td>
                <td><span class="admin-badge ${page.published ? "admin-badge-success" : "admin-badge-neutral"}">${page.published ? "Đang hiện" : "Đang ẩn"}</span></td>
                <td>
                    <a class="admin-link-btn" href="${escapeHtml(siteUrl("customer/page.html?slug=" + encodeURIComponent(page.slug)))}" target="_blank" rel="noopener">Xem</a>
                    <button type="button" class="admin-link-btn" data-action="edit" data-slug="${slug}">Sửa</button>
                    <button type="button" class="admin-link-btn admin-link-danger" data-action="delete" data-slug="${slug}">Xoá</button>
                </td>
            </tr>
        `;

    }


    function openForm(page) {

        editingSlug = page ? page.slug : null;

        formTitle.textContent = page ? "Sửa trang" : "Thêm trang";

        titleInput.value = page ? page.title : "";
        slugInput.value = page ? page.slug : "";
        slugInput.disabled = Boolean(page);
        bodyInput.value = page ? page.body : "";
        published.checked = page ? page.published : true;

        formError.hidden = true;

        panel.hidden = false;

        titleInput.focus();

    }


    function closeForm() {

        panel.hidden = true;

        editingSlug = null;

    }


    /* Bọc phần đang chọn trong ô nội dung bằng một thẻ (danh sách: mỗi dòng thành một mục) */
    function wrapSelection(tag) {

        const start = bodyInput.selectionStart;
        const end = bodyInput.selectionEnd;
        const selected = bodyInput.value.slice(start, end) || "nội dung";

        let html;

        if (tag === "ul") {
            html = "<ul>" + selected.split("\n").filter(Boolean).map(function (line) { return "<li>" + line + "</li>"; }).join("") + "</ul>";
        } else if (tag === "a") {
            html = '<a href="https://">' + selected + "</a>";
        } else {
            html = "<" + tag + ">" + selected + "</" + tag + ">";
        }

        bodyInput.setRangeText(html, start, end, "select");

        bodyInput.focus();

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

        const title = titleInput.value.trim();
        const slug = (editingSlug || slugInput.value.trim());

        if (!title) {
            showFormError("Vui lòng nhập tiêu đề trang");
            return;
        }

        if (!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(slug)) {
            showFormError("Đường dẫn chỉ gồm chữ thường không dấu, số và dấu gạch ngang");
            return;
        }

        if (!bodyInput.value.trim()) {
            showFormError("Vui lòng nhập nội dung trang");
            return;
        }

        saving = true;

        try {

            const saved = await apiRequest("/admin/pages/" + encodeURIComponent(slug), {
                method: "PUT",
                body: { title: title, body: bodyInput.value, published: published.checked },
                auth: true
            });

            showToast("Đã lưu trang", "success");

            editingSlug = saved.slug;

            slugInput.value = saved.slug;

            slugInput.disabled = true;

            bodyInput.value = saved.body;

            await load();

        } catch (error) {

            handleError(error, showFormError);

        } finally {

            saving = false;

        }

    }


    async function remove(slug) {

        try {

            await apiRequest("/admin/pages/" + encodeURIComponent(slug), { method: "DELETE", auth: true });

            showToast("Đã xoá trang", "success");

            if (editingSlug === slug) {
                closeForm();
            }

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
