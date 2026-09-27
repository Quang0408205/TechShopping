/* ================= CHI NHÁNH (admin/stores.html) ================= */

/* Chỉ ADMIN. Dữ liệu mẫu (B1), nối bảng stores thật ở Phase 7. */

document.addEventListener(
    "DOMContentLoaded",
    async function () {

        const staff = await adminLayoutReady;

        if (!staff) {
            return;
        }


        const panel = document.getElementById("createStorePanel");

        const form = document.getElementById("createStoreForm");

        const errorBox = document.getElementById("createStoreError");


        document.getElementById("toggleCreateFormBtn").addEventListener("click", function () {
            panel.hidden = !panel.hidden;
        });


        document.getElementById("cancelCreateStoreBtn").addEventListener("click", function () {

            panel.hidden = true;

            errorBox.hidden = true;

            form.reset();

        });


        form.addEventListener("submit", function (event) {

            event.preventDefault();


            const name = document.getElementById("newStoreName").value.trim();

            const address = document.getElementById("newStoreAddress").value.trim();

            const phone = document.getElementById("newStorePhone").value.trim();


            let error = "";

            if (!name || !address) {
                error = "Vui lòng nhập tên và địa chỉ chi nhánh.";
            } else if (getStores().some(function (store) {
                return store.name.toLowerCase() === name.toLowerCase();
            })) {
                error = "Đã có chi nhánh với tên này.";
            } else if (phone && !/^[0-9 +().-]{8,20}$/.test(phone)) {
                error = "Số điện thoại không hợp lệ.";
            }


            if (error) {

                errorBox.textContent = error;

                errorBox.hidden = false;

                return;

            }


            errorBox.hidden = true;

            const created = createStore({ name: name, address: address, phone: phone });

            form.reset();

            panel.hidden = true;

            showToast("Đã thêm " + created.name + ".", "success");

            render();

        });


        render();


        function render() {

            const stores = getStores();


            document.getElementById("storeRowCount").textContent =
                stores.length + " chi nhánh";


            document.getElementById("storeTableBody").innerHTML =
                stores.map(function (store) {

                    return `
                        <tr>
                            <td>${escapeHtml(store.id)}</td>
                            <td>${escapeHtml(store.name)}</td>
                            <td>${escapeHtml(store.address)}</td>
                            <td>${escapeHtml(store.phone || "—")}</td>
                            <td>${getEmployeesByStore(store.id).length} người</td>
                        </tr>
                    `;

                }).join("") || adminEmptyRow(5, "Chưa có chi nhánh nào");

        }

    }
);
