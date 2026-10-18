/* ================= ĐỊA CHỈ 2 CẤP: TỈNH / THÀNH → PHƯỜNG / XÃ ================= */

/*
 * Theo đơn vị hành chính mới từ 01/07/2025 (34 tỉnh / thành, không còn quận / huyện).
 * Dữ liệu tĩnh assets/data/vn-units.json: [{ n: tên tỉnh, w: [tên phường / xã] }].
 * setupVnAddress(provinceInput, wardInput) biến 2 ô chữ thành ô chọn có tìm kiếm (gõ không dấu được);
 * chỉ tên chọn đúng trong danh sách mới được coi là hợp lệ.
 */

let vnUnitsPromise = null;


function loadVnUnits() {

    if (!vnUnitsPromise) {

        vnUnitsPromise = fetch(siteUrl("assets/data/vn-units.json"))
            .then(function (response) {

                if (!response.ok) {
                    throw new Error("vn-units " + response.status);
                }

                return response.json();

            })
            .catch(function (error) {

                vnUnitsPromise = null;

                throw error;

            });

    }

    return vnUnitsPromise;

}


function vnFold(text) {

    return String(text || "")
        .normalize("NFD")
        .replace(/[̀-ͯ]/g, "")
        .replace(/đ/g, "d")
        .replace(/Đ/g, "D")
        .toLowerCase()
        .replace(/\s+/g, " ")
        .trim();

}


function vnBareName(folded) {

    return folded
        .replace(/^(thanh pho|tinh|tp\.?|phuong|xa|dac khu)\s+/, "")
        .replace(/^tp\.?\s*/, "")
        .trim();

}


/* "Số nhà, đường, Phường / Xã, Tỉnh / Thành" — chuỗi địa chỉ đầy đủ gửi lên server */

function joinVnAddress(street, ward, province) {

    return [street, ward, province]
        .map(function (part) { return String(part || "").trim(); })
        .filter(Boolean)
        .join(", ");

}


const VN_COMBO_MAX_ITEMS = 60;


/* Một ô chọn có tìm kiếm; getOptions() trả về danh sách tên hiện có */

function createVnCombo(input, getOptions, onPick) {

    const list = document.createElement("ul");

    list.className = "vn-combo-list";

    list.setAttribute("role", "listbox");

    list.id = input.id + "List";

    list.hidden = true;

    input.parentNode.classList.add("vn-combo");

    input.insertAdjacentElement("afterend", list);

    input.setAttribute("role", "combobox");

    input.setAttribute("aria-autocomplete", "list");

    input.setAttribute("aria-controls", list.id);

    input.setAttribute("aria-expanded", "false");

    input.setAttribute("autocomplete", "off");

    let shown = [];

    let activeIndex = -1;


    /* đúng tên; không có thì bỏ chữ đầu "Thành phố / Tỉnh / TP. / Phường / Xã" ở hai bên rồi so lại */
    function matchExact(text) {

        const folded = vnFold(text);

        if (!folded) {
            return null;
        }

        const options = getOptions();

        const exact = options.find(function (name) { return vnFold(name) === folded; });

        if (exact) {
            return exact;
        }

        const bare = vnBareName(folded);

        const loose = options.filter(function (name) { return vnBareName(vnFold(name)) === bare; });

        return loose.length === 1 ? loose[0] : null;

    }


    function render() {

        const term = vnFold(input.value);

        const all = getOptions();

        /* đang hiện đúng tên đã chọn: mở ra thì cho xem cả danh sách */
        const filtered = !term || matchExact(input.value)
            ? all
            : all.filter(function (name) { return vnFold(name).indexOf(term) !== -1; });

        shown = filtered.slice(0, VN_COMBO_MAX_ITEMS);

        activeIndex = -1;

        list.innerHTML = shown.length
            ? shown.map(function (name, i) {
                return `<li role="option" data-index="${i}">${escapeHtml(name)}</li>`;
            }).join("")
            : `<li class="vn-combo-empty">Không tìm thấy</li>`;

        if (filtered.length > shown.length) {
            list.insertAdjacentHTML("beforeend", `<li class="vn-combo-empty">Gõ thêm để thu hẹp (${filtered.length} kết quả)</li>`);
        }

    }


    function open() {

        if (input.disabled) {
            return;
        }

        render();

        list.hidden = false;

        input.setAttribute("aria-expanded", "true");

    }


    function close() {

        list.hidden = true;

        input.setAttribute("aria-expanded", "false");

    }


    function pick(name) {

        input.value = name;

        close();

        onPick(name);

    }


    function highlight(index) {

        const items = list.querySelectorAll('[role="option"]');

        if (!items.length) {
            return;
        }

        activeIndex = (index + items.length) % items.length;

        items.forEach(function (item, i) {
            item.classList.toggle("is-active", i === activeIndex);
        });

        items[activeIndex].scrollIntoView({ block: "nearest" });

    }


    input.addEventListener("focus", open);

    input.addEventListener("input", function () {

        open();

        onPick(matchExact(input.value));

    });

    input.addEventListener("keydown", function (event) {

        if (event.key === "ArrowDown" || event.key === "ArrowUp") {

            event.preventDefault();

            if (list.hidden) {
                open();
            }

            highlight(activeIndex + (event.key === "ArrowDown" ? 1 : -1));

        } else if (event.key === "Enter" && !list.hidden && activeIndex >= 0) {

            event.preventDefault();

            pick(shown[activeIndex]);

        } else if (event.key === "Escape" || event.key === "Tab") {

            close();

        }

    });

    /* mousedown để chọn trước khi ô mất focus */
    list.addEventListener("mousedown", function (event) {

        const item = event.target.closest('[role="option"]');

        event.preventDefault();

        if (item) {
            pick(shown[Number(item.dataset.index)]);
        }

    });

    input.addEventListener("blur", function () {

        close();

        const exact = matchExact(input.value);

        if (exact) {
            input.value = exact;
        }

        onPick(exact);

    });

    return { matchExact: matchExact, close: close };

}


/*
 * Trả về { ready, getValue(), setValue(province, ward) }.
 * getValue() → { province, ward } chỉ gồm tên hợp lệ (sai / chưa chọn → "").
 * Không tải được dữ liệu: 2 ô bị khoá, getValue() trả rỗng.
 */

function setupVnAddress(provinceInput, wardInput) {

    let units = [];

    let province = "";

    let ward = "";


    function wardsOf(name) {

        const unit = units.find(function (item) { return item.n === name; });

        return unit ? unit.w : [];

    }


    function syncWardState() {

        wardInput.disabled = !province;

        wardInput.placeholder = province ? "Gõ để tìm phường / xã" : "Chọn tỉnh / thành phố trước";

    }


    const provinceCombo = createVnCombo(provinceInput, function () {
        return units.map(function (item) { return item.n; });
    }, function (name) {

        const changed = (name || "") !== province;

        province = name || "";

        if (changed) {

            ward = "";

            wardInput.value = "";

        }

        syncWardState();

    });

    const wardCombo = createVnCombo(wardInput, function () {
        return wardsOf(province);
    }, function (name) {

        ward = name || "";

    });


    provinceInput.placeholder = "Gõ để tìm tỉnh / thành phố";

    provinceInput.disabled = true;

    syncWardState();


    const ready = loadVnUnits()
        .then(function (data) {

            units = Array.isArray(data) ? data : [];

            provinceInput.disabled = false;

            /* giá trị đặt trước khi tải xong */
            setValue(provinceInput.value, wardInput.value);

        })
        .catch(function () {

            provinceInput.placeholder = "Không tải được danh sách tỉnh / thành";

        });


    function setValue(provinceName, wardName) {

        provinceInput.value = provinceName || "";

        wardInput.value = wardName || "";

        if (!units.length) {
            return;
        }

        province = provinceCombo.matchExact(provinceName) || "";

        provinceInput.value = province || provinceInput.value;

        ward = province ? wardCombo.matchExact(wardName) || "" : "";

        wardInput.value = ward || (province ? wardInput.value : "");

        syncWardState();

    }


    return {
        ready: ready,
        getValue: function () {
            return { province: province, ward: ward };
        },
        setValue: setValue
    };

}
