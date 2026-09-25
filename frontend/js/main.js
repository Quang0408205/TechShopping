document.addEventListener("DOMContentLoaded", function () {

    updateCartCount();

    setupAddToCart();

});


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