/* ================= HEADER / FOOTER DÙNG CHUNG ================= */

/*
 * Trang chỉ cần đặt chỗ:
 *
 *   <div data-include="header" data-active="products"></div>
 *   ...
 *   <div data-include="footer"></div>
 *
 * layout.js tải partials/header.html, partials/footer.html và thay chỗ đặt
 * bằng nội dung đó (thay hẳn thẻ div để header "sticky" vẫn hoạt động).
 *
 * Cần nạp sau js/core/api.js (dùng SITE_ROOT) và trước main.js.
 * Cần chạy website qua HTTP (Live Server); mở file trực tiếp (file://) sẽ không tải được.
 *
 * Chuyển trang không giật: partial đã tải được lưu ở sessionStorage và chèn ĐỒNG BỘ ngay khi
 * script chạy (trước lần vẽ đầu); bản mới vẫn được tải lại ngầm cho lần chuyển trang sau.
 */

const PARTIAL_CACHE_PREFIX = "poy_partial:";

const layoutReady = loadLayout();


async function loadLayout() {

    /* Script nằm cuối <body> nên các chỗ đặt thường đã có sẵn */

    if (document.readyState === "loading" && !document.querySelector("[data-include]")) {

        await new Promise(function (resolve) {

            document.addEventListener("DOMContentLoaded", resolve);

        });

    }


    const placeholders =
        Array.from(document.querySelectorAll("[data-include]"));

    const missing = [];

    placeholders.forEach(function (placeholder) {

        const cached = readPartialCache(placeholder.dataset.include);

        if (cached === null) {
            missing.push(placeholder);
        } else {
            insertPartial(placeholder, cached);
        }

    });


    await Promise.all(missing.map(includePartial));


    /* Làm mới bản lưu cho lần sau (không đổi trang đang xem) */
    placeholders.filter(function (placeholder) {
        return missing.indexOf(placeholder) === -1;
    }).forEach(function (placeholder) {
        fetchPartial(placeholder.dataset.include).catch(function () { });
    });


    document.dispatchEvent(
        new CustomEvent("layout:ready")
    );

}


async function includePartial(placeholder) {

    const name = placeholder.dataset.include;

    try {

        insertPartial(placeholder, await fetchPartial(name));

    } catch (error) {

        console.warn(
            "Không tải được partials/" + name + ".html:",
            error.message
        );

    }

}


async function fetchPartial(name) {

    const response = await fetch(
        siteUrl("partials/" + name + ".html")
    );

    if (!response.ok) {
        throw new Error("HTTP " + response.status);
    }

    const html = await response.text();

    try {
        sessionStorage.setItem(PARTIAL_CACHE_PREFIX + name, html);
    } catch (error) {
        /* sessionStorage bị chặn / đầy: chỉ mất phần chèn nhanh */
    }

    return html;

}


function readPartialCache(name) {

    try {
        return sessionStorage.getItem(PARTIAL_CACHE_PREFIX + name);
    } catch (error) {
        return null;
    }

}


function insertPartial(placeholder, rawHtml) {

    const template =
        document.createElement("template");

    template.innerHTML = rawHtml.replaceAll("{{ROOT}}", SITE_ROOT);


    /* Bỏ qua comment đầu file, lấy phần tử <header>/<footer> */
    const element =
        template.content.firstElementChild;


    markActiveLink(element, placeholder.dataset.active);

    placeholder.replaceWith(element);

}


/* Tô đậm mục menu của trang hiện tại (data-page trùng data-active) */

function markActiveLink(element, activePage) {

    if (!activePage) {
        return;
    }

    element.querySelectorAll("[data-page]")
        .forEach(function (link) {

            link.classList.toggle(
                "active",
                link.dataset.page === activePage
            );

        });

}
