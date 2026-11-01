/* ================= TRANG TĨNH (customer/page.html?slug=) ================= */

/*
 * API công khai: GET /pages/{slug} (nội dung HTML đã được backend lọc), GET /pages (danh sách bên cạnh).
 * Liên kết nội bộ trong nội dung viết dạng "/customer/contact.html" được gắn vào gốc của site.
 */

document.addEventListener("DOMContentLoaded", async function () {

    const slug = new URLSearchParams(window.location.search).get("slug") || "";

    const title = document.getElementById("pageTitle");
    const crumb = document.getElementById("pageCrumb");
    const body = document.getElementById("pageBody");
    const list = document.getElementById("pageList");


    apiRequest("/pages").then(function (pages) {

        list.innerHTML = (pages || []).map(function (page) {
            return `<li><a href="${escapeHtml(pageHref(page.slug))}"${page.slug === slug ? ' aria-current="page"' : ""}>${escapeHtml(page.title)}</a></li>`;
        }).join("");

    }).catch(function () {
        list.closest("aside").hidden = true;
    });


    if (!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(slug)) {
        showMissing();
        return;
    }

    try {

        const page = await apiRequest("/pages/" + encodeURIComponent(slug));

        document.title = page.title + " - POY";

        setMeta("description", plainText(page.body).slice(0, 160));

        title.textContent = page.title;

        crumb.textContent = page.title;

        body.innerHTML = page.body;

        body.querySelectorAll("a[href^='/']").forEach(function (link) {
            link.setAttribute("href", siteUrl(link.getAttribute("href").slice(1)));
        });

    } catch (error) {

        if (error.status === 404) {
            showMissing();
        } else {
            title.textContent = "Không tải được trang";
            body.innerHTML = `<p>${escapeHtml(getErrorMessage(error))}</p>`;
        }

    }


    function showMissing() {

        document.title = "Không tìm thấy trang - POY";

        title.textContent = "Không tìm thấy trang";

        crumb.textContent = "Không tìm thấy";

        body.innerHTML = '<p>Trang bạn mở không tồn tại hoặc đã được gỡ. Hãy chọn một trang ở bên cạnh hoặc <a href="' + escapeHtml(siteUrl("index.html")) + '">quay về trang chủ</a>.</p>';

    }

});


function pageHref(slug) {

    return siteUrl("customer/page.html?slug=" + encodeURIComponent(slug));

}


function plainText(html) {

    return String(html).replace(/<[^>]*>/g, " ").replace(/&nbsp;/g, " ").replace(/\s+/g, " ").trim();

}


function setMeta(name, content) {

    let element = document.querySelector('meta[name="' + name + '"]');

    if (!element) {
        element = document.createElement("meta");
        element.setAttribute("name", name);
        document.head.appendChild(element);
    }

    element.setAttribute("content", content);

}
