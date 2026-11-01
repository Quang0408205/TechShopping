/* ================= HỎI ĐÁP SẢN PHẨM (tab "Hỏi đáp" ở trang chi tiết) ================= */

/*
 * initProductQuestions(product) — gọi từ product-detail.js khi đã có sản phẩm.
 * API: GET /products/{id}/questions?page&size (công khai, chỉ câu đã có trả lời),
 *      POST /product-questions { productId, question } (cần đăng nhập; 10–500 ký tự, tối đa 5 câu / 24 giờ).
 * Câu hỏi chỉ hiện công khai sau khi cửa hàng trả lời, nên sau khi gửi khách được báo như vậy.
 */

function initProductQuestions(product) {

    const PAGE_SIZE = 5;

    const countEl = document.getElementById("qaTabCount");
    const list = document.getElementById("qaList");
    const moreBtn = document.getElementById("qaMoreBtn");
    const form = document.getElementById("qaForm");
    const input = document.getElementById("qaInput");
    const counter = document.getElementById("qaCounter");
    const error = document.getElementById("qaError");
    const note = document.getElementById("qaNote");
    const submit = document.getElementById("qaSubmit");

    if (!list || !form) {
        return;
    }

    let page = 0;
    let busy = false;


    load(true);

    input.addEventListener("input", function () {
        counter.textContent = input.value.length + " / 500";
        error.hidden = true;
    });

    moreBtn.addEventListener("click", function () { load(false); });

    form.addEventListener("submit", async function (event) {

        event.preventDefault();

        if (!isLoggedIn()) {
            redirectToLogin("question");
            return;
        }

        const question = input.value.trim().replace(/\s+/g, " ");

        if (question.length < 10) {
            showError("Câu hỏi cần ít nhất 10 ký tự.");
            return;
        }

        submit.disabled = true;

        try {

            await apiRequest("/product-questions", { method: "POST", body: { productId: product.id, question: question }, auth: true });

            input.value = "";

            counter.textContent = "0 / 500";

            note.hidden = false;

            note.textContent = "Cảm ơn bạn! Câu hỏi sẽ hiện ở đây sau khi POY trả lời.";

        } catch (err) {

            showError(getErrorMessage(err));

        } finally {

            submit.disabled = false;

        }

    });

    document.getElementById("qaLoginHint").hidden = isLoggedIn();


    async function load(reset) {

        if (busy) {
            return;
        }

        busy = true;

        if (reset) {
            page = 0;
        }

        try {

            const result = await apiRequest("/products/" + encodeURIComponent(product.id) + "/questions?page=" + page + "&size=" + PAGE_SIZE);

            countEl.textContent = String(result.totalElements);

            const html = result.content.map(itemHtml).join("");

            if (reset) {
                list.innerHTML = html || '<p class="qa-empty">Chưa có câu hỏi nào được trả lời. Hãy là người đầu tiên đặt câu hỏi.</p>';
            } else {
                list.insertAdjacentHTML("beforeend", html);
            }

            moreBtn.hidden = result.page + 1 >= result.totalPages;

            page = result.page + 1;

        } catch (err) {

            list.innerHTML = `<p class="qa-empty">${escapeHtml(getErrorMessage(err))}</p>`;

        } finally {

            busy = false;

        }

    }


    function itemHtml(item) {

        return `
            <article class="qa-item">
                <p class="qa-question"><strong>${escapeHtml(item.askerName)}</strong> hỏi: ${escapeHtml(item.question)}</p>
                <p class="qa-answer"><strong>POY</strong> trả lời: ${escapeHtml(item.answer)}</p>
                <time class="qa-time">${escapeHtml(formatQaDate(item.answeredAt))}</time>
            </article>
        `;

    }


    function showError(message) {

        error.textContent = message;

        error.hidden = false;

    }

}


function formatQaDate(value) {

    const date = new Date(value);

    return isNaN(date) ? "" : date.toLocaleDateString("vi-VN");

}
