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
 */

const layoutReady = loadLayout();


async function loadLayout() {

    /* Script nằm cuối <body> nên các chỗ đặt đã có sẵn; vẫn chờ cho chắc */

    if (document.readyState === "loading") {

        await new Promise(function (resolve) {

            document.addEventListener("DOMContentLoaded", resolve);

        });

    }


    const placeholders =
        document.querySelectorAll("[data-include]");


    await Promise.all(
        Array.from(placeholders).map(includePartial)
    );


    document.dispatchEvent(
        new CustomEvent("layout:ready")
    );

}


async function includePartial(placeholder) {

    const name = placeholder.dataset.include;

    try {

        const response = await fetch(
            siteUrl("partials/" + name + ".html")
        );

        if (!response.ok) {
            throw new Error("HTTP " + response.status);
        }


        const html =
            (await response.text()).replaceAll("{{ROOT}}", SITE_ROOT);

        const template =
            document.createElement("template");

        template.innerHTML = html;


        /* Bỏ qua comment đầu file, lấy phần tử <header>/<footer> */
        const element =
            template.content.firstElementChild;


        markActiveLink(element, placeholder.dataset.active);

        placeholder.replaceWith(element);

    } catch (error) {

        console.warn(
            "Không tải được partials/" + name + ".html:",
            error.message
        );

    }

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
