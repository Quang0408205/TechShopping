/* ================= CHI NHÁNH (admin/stores.html) ================= */

/*
 * Chỉ ADMIN, dữ liệu thật (Phase 7):
 *   GET    /admin/stores?keyword&active&page&size   danh sách (theo tên)
 *   POST   /admin/stores                            thêm (201)
 *   PUT    /admin/stores/{id}                       sửa toàn bộ; active=false = tạm đóng
 *   DELETE /admin/stores/{id}                       xoá (409 STORE_IN_USE khi đã có nhân viên / tồn kho / đơn)
 * Cột "Nhân viên" đếm nhân viên đang làm được gán vào chi nhánh (GET /admin/employees).
 */

const STORE_PAGE_SIZE = 20;


document.addEventListener("DOMContentLoaded", async function () {

    const staff = await adminLayoutReady;

    if (!staff) {
        return;
    }


    const filterForm = document.getElementById("storeFilterForm");

    const keywordInput = document.getElementById("storeKeyword");

    const statusFilter = document.getElementById("storeStatusFilter");

    const panel = document.getElementById("storeFormPanel");

    const form = document.getElementById("storeForm");

    const formTitle = document.getElementById("storeFormTitle");

    const formError = document.getElementById("storeFormError");

    const saveButton = document.getElementById("storeSaveBtn");

    const tbody = document.getElementById("storeTableBody");

    const pagination = document.getElementById("storePagination");


    let currentPage = 0;

    let currentStores = [];

    let editingStore = null;

    let requestCounter = 0;


    filterForm.addEventListener("submit", function (event) {
        event.preventDefault();
        reloadFromFirstPage();
    });

    statusFilter.addEventListener("change", reloadFromFirstPage);

    document.getElementById("newStoreBtn").addEventListener("click", function () {
        openForm(null);
    });

    document.getElementById("storeCancelBtn").addEventListener("click", closeForm);

    form.addEventListener("submit", saveStore);


    tbody.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-action]");

        const store = button && findStore(button.dataset.id);

        if (!store) {
            return;
        }

        if (button.dataset.action === "edit") {
            openForm(store);
        } else if (button.dataset.action === "toggle") {
            confirmToggle(store);
        } else if (button.dataset.action === "delete") {
            confirmDelete(store);
        }

    });


    pagination.addEventListener("click", function (event) {

        const button = event.target.closest("button[data-page]");

        if (!button || button.disabled) {
            return;
        }

        currentPage = Number(button.dataset.page);

        loadStores();

    });


    loadStores();


    /* ================= DANH SÁCH ================= */

    function reloadFromFirstPage() {

        currentPage = 0;

        loadStores();

    }


    async function loadStores() {

        const requestId = ++requestCounter;

        const params = new URLSearchParams({ page: String(currentPage), size: String(STORE_PAGE_SIZE) });

        const keyword = keywordInput.value.trim();

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (statusFilter.value) {
            params.set("active", statusFilter.value);
        }


        tbody.innerHTML = adminEmptyRow(6, "Đang tải…");


        try {

            const results = await Promise.all([
                apiRequest("/admin/stores?" + params.toString(), { auth: true }),
                fetchAllPages("/admin/employees?active=true")
            ]);

            if (requestId !== requestCounter) {
                return;
            }

            const page = results[0];

            /* Dòng cuối của trang cuối vừa bị xoá → lùi một trang */
            if ((page.content || []).length === 0 && currentPage > 0 && page.totalPages > 0) {

                currentPage = page.totalPages - 1;

                loadStores();

                return;

            }

            currentStores = page.content || [];

            renderTable(page, countEmployees(results[1]));

        } catch (error) {

            if (requestId === requestCounter) {
                handleError(error, function (message) {
                    tbody.innerHTML = adminEmptyRow(6, message);
                    pagination.innerHTML = "";
                });
            }

        }

    }


    /* { storeId: số nhân viên đang làm được gán vào chi nhánh } */

    function countEmployees(employees) {

        const counts = {};

        employees.forEach(function (employee) {

            if (employee.assignment) {
                counts[employee.assignment.storeId] = (counts[employee.assignment.storeId] || 0) + 1;
            }

        });

        return counts;

    }


    function findStore(id) {

        return currentStores.find(function (store) {
            return String(store.id) === String(id);
        });

    }


    function renderTable(page, employeeCounts) {

        document.getElementById("storeRowCount").textContent = page.totalElements + " chi nhánh";

        tbody.innerHTML = currentStores.map(function (store) {
            return renderRow(store, employeeCounts[store.id] || 0);
        }).join("") || adminEmptyRow(6, "Chưa có chi nhánh nào phù hợp bộ lọc");

        renderPagination(page);

    }


    function renderRow(store, employeeCount) {

        const id = escapeHtml(String(store.id));

        const open = store.active !== false;

        const area = [store.district, store.city].filter(Boolean).join(", ");

        const coordinates = store.latitude != null && store.longitude != null
            ? `<span class="admin-subtext">${escapeHtml(store.latitude + ", " + store.longitude)}</span>`
            : "";


        return `
            <tr data-store-id="${id}">
                <td>
                    <strong>${escapeHtml(store.name)}</strong>
                    <span class="admin-subtext">#${id}</span>
                </td>
                <td>
                    ${escapeHtml(store.address)}
                    <span class="admin-subtext">${escapeHtml(area)}</span>
                    ${coordinates}
                </td>
                <td>
                    ${escapeHtml(store.phone || "—")}
                    ${store.email ? `<span class="admin-subtext">${escapeHtml(store.email)}</span>` : ""}
                </td>
                <td>${employeeCount} người</td>
                <td>
                    <span class="admin-badge ${open ? "admin-badge-success" : "admin-badge-neutral"}">
                        ${open ? "Đang mở" : "Tạm đóng"}
                    </span>
                </td>
                <td class="admin-actions-cell">
                    <button type="button" class="admin-link-btn" data-action="edit" data-id="${id}">Sửa</button>
                    <button type="button" class="admin-link-btn" data-action="toggle" data-id="${id}">${open ? "Tạm đóng" : "Mở lại"}</button>
                    <button type="button" class="admin-link-btn admin-link-danger" data-action="delete" data-id="${id}">Xoá</button>
                </td>
            </tr>
        `;

    }


    function renderPagination(page) {

        if (page.totalPages <= 1) {

            pagination.innerHTML = "";

            return;

        }

        pagination.innerHTML = `
            <button type="button" class="btn btn-outline-dark" data-page="${page.page - 1}" ${page.page <= 0 ? "disabled" : ""}>‹ TRƯỚC</button>
            <span>Trang ${page.page + 1} / ${page.totalPages}</span>
            <button type="button" class="btn btn-outline-dark" data-page="${page.page + 1}" ${page.page + 1 >= page.totalPages ? "disabled" : ""}>SAU ›</button>
        `;

    }


    /* ================= THÊM / SỬA ================= */

    function field(id) {

        return document.getElementById(id);

    }


    function openForm(store) {

        editingStore = store;

        formTitle.textContent = store ? "Sửa chi nhánh " + store.name : "Thêm chi nhánh mới";

        field("stName").value = store ? store.name : "";

        field("stAddress").value = store ? store.address : "";

        field("stDistrict").value = store ? store.district || "" : "";

        field("stCity").value = store ? store.city || "" : "";

        field("stPhone").value = store ? store.phone || "" : "";

        field("stEmail").value = store ? store.email || "" : "";

        field("stLatitude").value = store && store.latitude != null ? store.latitude : "";

        field("stLongitude").value = store && store.longitude != null ? store.longitude : "";

        field("stActive").checked = store ? store.active !== false : true;

        formError.hidden = true;

        panel.hidden = false;

        field("stName").focus();

    }


    function closeForm() {

        editingStore = null;

        form.reset();

        formError.hidden = true;

        panel.hidden = true;

    }


    function textOrNull(id) {

        const value = field(id).value.trim();

        return value === "" ? null : value;

    }


    /* StoreRequest đầy đủ; trả về { body } hoặc { error } */

    function readForm() {

        const body = {
            name: textOrNull("stName"),
            address: textOrNull("stAddress"),
            district: textOrNull("stDistrict"),
            city: textOrNull("stCity"),
            phone: textOrNull("stPhone"),
            email: textOrNull("stEmail"),
            latitude: textOrNull("stLatitude"),
            longitude: textOrNull("stLongitude"),
            active: field("stActive").checked
        };

        if (!body.name || !body.address || !body.district || !body.city) {
            return { error: "Vui lòng nhập tên, địa chỉ, quận / huyện và tỉnh / thành phố." };
        }

        if ((body.latitude === null) !== (body.longitude === null)) {
            return { error: "Vĩ độ và kinh độ phải nhập cả hai hoặc bỏ trống cả hai." };
        }

        if (field("stLatitude").validity.badInput || field("stLongitude").validity.badInput) {
            return { error: "Vĩ độ / kinh độ phải là số." };
        }

        body.latitude = body.latitude === null ? null : Number(body.latitude);

        body.longitude = body.longitude === null ? null : Number(body.longitude);

        return { body: body };

    }


    async function saveStore(event) {

        event.preventDefault();


        const result = readForm();

        if (result.error) {

            formError.textContent = result.error;

            formError.hidden = false;

            return;

        }


        saveButton.disabled = true;

        try {

            const saved = editingStore
                ? await apiRequest("/admin/stores/" + editingStore.id, { method: "PUT", body: result.body, auth: true })
                : await apiRequest("/admin/stores", { method: "POST", body: result.body, auth: true });

            showToast((editingStore ? "Đã lưu " : "Đã thêm ") + saved.name + ".", "success");

            closeForm();

            loadStores();

        } catch (error) {

            handleError(error, function (message) {
                formError.textContent = message + detailsText(error);
                formError.hidden = false;
            });

        } finally {

            saveButton.disabled = false;

        }

    }


    /* " (Tỉnh / thành: …; Email: …)" từ error.details của VALIDATION_ERROR */

    function detailsText(error) {

        if (!error.details || typeof error.details !== "object") {
            return "";
        }

        const parts = Object.keys(error.details).map(function (key) {
            return error.details[key];
        });

        return parts.length > 0 ? " (" + parts.join("; ") + ")" : "";

    }


    /* ================= TẠM ĐÓNG / MỞ LẠI / XOÁ ================= */

    function confirmToggle(store) {

        const closing = store.active !== false;

        openConfirmModal({
            title: (closing ? "Tạm đóng " : "Mở lại ") + store.name + "?",
            message: closing
                ? "Chi nhánh không nhận đơn mới (cả giao tận nhà lẫn nhận tại cửa hàng). Đơn đã gán vẫn xử lý bình thường."
                : "Chi nhánh lại được gán đơn giao tận nhà và hiện ở ô \"Nhận tại cửa hàng\".",
            confirmLabel: closing ? "TẠM ĐÓNG" : "MỞ LẠI",
            cancelLabel: "Quay lại",
            onConfirm: async function () {

                try {

                    await apiRequest("/admin/stores/" + store.id, {
                        method: "PUT",
                        auth: true,
                        body: {
                            name: store.name,
                            address: store.address,
                            district: store.district,
                            city: store.city,
                            phone: store.phone,
                            email: store.email,
                            latitude: store.latitude,
                            longitude: store.longitude,
                            active: !closing
                        }
                    });

                    showToast((closing ? "Đã tạm đóng " : "Đã mở lại ") + store.name + ".", "success");

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

                loadStores();

            }
        });

    }


    function confirmDelete(store) {

        openConfirmModal({
            title: "Xoá chi nhánh " + store.name + "?",
            message: "Chỉ xoá được chi nhánh chưa có nhân viên, tồn kho hay đơn hàng nào; nếu đã có, hãy tạm đóng. Không thể hoàn tác.",
            confirmLabel: "XOÁ",
            cancelLabel: "Quay lại",
            onConfirm: async function () {

                try {

                    await apiRequest("/admin/stores/" + store.id, { method: "DELETE", auth: true });

                    showToast("Đã xoá " + store.name + ".", "success");

                    if (editingStore && editingStore.id === store.id) {
                        closeForm();
                    }

                } catch (error) {

                    handleError(error, function (message) {
                        showToast(message, "error");
                    });

                }

                loadStores();

            }
        });

    }


    /*
     * Hết phiên (401, kể cả sau khi đã thử làm mới token) → kiểm tra lại phiên
     * và về trang đăng nhập; lỗi khác (403, 404, 409…) → hiển thị bằng show(message).
     */

    function handleError(error, show) {

        if (error.status === 401) {
            requireStaffLogin();
        }

        show(getErrorMessage(error));

    }

});
