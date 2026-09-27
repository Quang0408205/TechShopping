document.addEventListener("DOMContentLoaded", async function () {

    setupAddToCart();


    /* Header được layout.js chèn vào sau: chờ xong mới cập nhật */

    if (typeof layoutReady !== "undefined") {
        await layoutReady;
    }

    renderAuthState();

    updateCartCount();

    setupHeaderSearch();

});


/* ================= TÀI KHOẢN (HEADER) ================= */

/*
 * Đã đăng nhập: nút "Đăng nhập" trên header hiển thị tên người dùng,
 * kèm nút "Đăng xuất". Cần nạp js/core/api.js trước main.js.
 */

function renderAuthState() {

    if (typeof isLoggedIn !== "function" || !isLoggedIn()) {
        return;
    }


    const user = getCurrentUser() || {};

    const displayName =
        user.fullname || user.username || "Tài khoản";


    document.querySelectorAll(".header .login-btn")
        .forEach(function (loginButton) {

            loginButton.textContent = displayName;

            loginButton.title = user.email || displayName;

            loginButton.classList.add("user-name");

            loginButton.href = siteUrl("customer/account.html");


            const logoutButton =
                document.createElement("a");

            logoutButton.href = "#";

            logoutButton.className = "login-btn logout-btn";

            logoutButton.textContent = "Đăng xuất";


            logoutButton.addEventListener(
                "click",
                async function (event) {

                    event.preventDefault();

                    await logout();

                    window.location.reload();

                }
            );


            loginButton.insertAdjacentElement(
                "afterend",
                logoutButton
            );

        });

}


/* ================= TÌM KIẾM (HEADER) ================= */

/*
 * Ô tìm kiếm trên header (partials/header.html): submit sẽ chuyển tới trang
 * Sản phẩm kèm ?search=..., products.js gửi giá trị này lên API dưới dạng
 * keyword. Gọi sau khi layout.js đã chèn header.
 */

function setupHeaderSearch() {

    document.querySelectorAll(".header .search-form")
        .forEach(function (form) {

            form.addEventListener("submit", function (event) {

                event.preventDefault();

                const input = form.querySelector("input");

                const keyword = input ? input.value.trim() : "";


                window.location.href =
                    siteUrl("customer/products.html") +
                    (keyword ? "?search=" + encodeURIComponent(keyword) : "");

            });

        });

}


/* ================= CART ================= */

function getCart() {

    const cart = localStorage.getItem(CART_STORAGE_KEY);

    if (!cart) {
        return [];
    }

    return JSON.parse(cart);
}


function saveCart(cart) {

    localStorage.setItem(
        CART_STORAGE_KEY,
        JSON.stringify(cart)
    );

}


/*
 * Gắn "Thêm vào giỏ" cho các nút .add-cart trong root (mặc định: cả trang).
 * Thẻ sản phẩm render sau (products.js, home.js) gọi lại với lưới mới;
 * cờ data-cart-bound tránh gắn sự kiện 2 lần cho cùng một nút.
 */

function setupAddToCart(root) {

    const scope = root || document;

    const buttons = scope.querySelectorAll(".add-cart");

    buttons.forEach(function (button) {

        if (button.dataset.cartBound === "1") {
            return;
        }

        button.dataset.cartBound = "1";


        button.addEventListener("click", function () {

            const name =
                button.dataset.name;

            const price =
                Number(button.dataset.price);

            addToCart(name, price);

        });

    });

}


/* quantity: tuỳ chọn (trang chi tiết sản phẩm), mặc định 1 */

function addToCart(name, price, quantity) {

    const amount =
        Number.isInteger(quantity) && quantity > 0 ? quantity : 1;

    const cart = getCart();

    const existingProduct =
        cart.find(
            product => product.name === name
        );


    if (existingProduct) {

        existingProduct.quantity += amount;

    } else {

        cart.push({

            name: name,

            price: price,

            quantity: amount

        });

    }


    saveCart(cart);

    updateCartCount();


    /* Toast thay cho alert() (js/core/ui.js) */

    if (typeof showToast === "function") {

        showToast(
            (amount > 1 ? "Đã thêm " + amount + " × " : "Đã thêm ") + name + " vào giỏ hàng!",
            "success"
        );

    }

}


function updateCartCount() {

    const cart = getCart();

    let count = 0;

    cart.forEach(function (product) {

        count += product.quantity;

    });


    const elements =
        document.querySelectorAll(".cart-count");


    elements.forEach(function (element) {

        element.textContent = count;

    });

}
