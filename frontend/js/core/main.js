document.addEventListener("DOMContentLoaded", async function () {

    setupAddToCart();


    /* Header được layout.js chèn vào sau: chờ xong mới cập nhật */

    if (typeof layoutReady !== "undefined") {
        await layoutReady;
    }

    renderAuthState();

    updateCartCount();

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


/* ================= CART ================= */

function getCart() {

    const cart = localStorage.getItem("lahy_cart");

    if (!cart) {
        return [];
    }

    return JSON.parse(cart);
}


function saveCart(cart) {

    localStorage.setItem(
        "lahy_cart",
        JSON.stringify(cart)
    );

}


function setupAddToCart() {

    const buttons = document.querySelectorAll(".add-cart");

    buttons.forEach(function (button) {

        button.addEventListener("click", function () {

            const name =
                button.dataset.name;

            const price =
                Number(button.dataset.price);

            addToCart(name, price);

        });

    });

}


function addToCart(name, price) {

    const cart = getCart();

    const existingProduct =
        cart.find(
            product => product.name === name
        );


    if (existingProduct) {

        existingProduct.quantity++;

    } else {

        cart.push({

            name: name,

            price: price,

            quantity: 1

        });

    }


    saveCart(cart);

    updateCartCount();


    alert(
        "Đã thêm " +
        name +
        " vào giỏ hàng!"
    );

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


function formatPrice(price) {

    return new Intl.NumberFormat(
        "vi-VN"
    ).format(price) + "đ";

}