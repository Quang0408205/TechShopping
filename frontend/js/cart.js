document.addEventListener(
    "DOMContentLoaded",
    function () {

        renderCart();

    }
);


function renderCart() {

    const container =
        document.getElementById(
            "cartContainer"
        );


    const cart = getCart();


    if (cart.length === 0) {

        container.innerHTML = `

            <div class="empty-cart">

                <h2>
                    GIỎ HÀNG ĐANG TRỐNG
                </h2>

                <p>
                    Hãy khám phá các sản phẩm
                    của LAHY.
                </p>

                <br>

                <a
                    href="products.html"
                    class="btn btn-dark"
                >
                    KHÁM PHÁ SẢN PHẨM
                </a>

            </div>

        `;

        return;

    }


    let total = 0;


    let rows = "";


    cart.forEach(function (product, index) {

        const productTotal =
            product.price *
            product.quantity;


        total += productTotal;


        rows += `

            <tr>

                <td class="cart-product">
                    ${product.name}
                </td>

                <td>

                    <div class="quantity-control">

                        <button
                            onclick="changeQuantity(${index}, -1)"
                        >
                            −
                        </button>

                        <span>
                            ${product.quantity}
                        </span>

                        <button
                            onclick="changeQuantity(${index}, 1)"
                        >
                            +
                        </button>

                    </div>

                </td>

                <td>
                    ${formatPrice(product.price)}
                </td>

                <td>
                    ${formatPrice(productTotal)}
                </td>

                <td>

                    <button
                        class="remove-btn"
                        onclick="removeProduct(${index})"
                    >
                        Xóa
                    </button>

                </td>

            </tr>

        `;

    });


    container.innerHTML = `

        <table class="cart-table">

            <thead>

                <tr>

                    <th>
                        SẢN PHẨM
                    </th>

                    <th>
                        SỐ LƯỢNG
                    </th>

                    <th>
                        ĐƠN GIÁ
                    </th>

                    <th>
                        THÀNH TIỀN
                    </th>

                    <th></th>

                </tr>

            </thead>

            <tbody>

                ${rows}

            </tbody>

        </table>


        <div class="cart-summary">

            <div class="cart-total">

                <div class="cart-total-row">

                    <span>
                        Tạm tính
                    </span>

                    <strong>
                        ${formatPrice(total)}
                    </strong>

                </div>


                <div class="cart-total-row">

                    <span>
                        Tổng cộng
                    </span>

                    <span class="cart-total-price">
                        ${formatPrice(total)}
                    </span>

                </div>


                <button
                    class="btn btn-dark"
                    onclick="checkout()"
                >
                    TIẾN HÀNH THANH TOÁN
                </button>

            </div>

        </div>

    `;

}


function changeQuantity(index, amount) {

    const cart = getCart();


    cart[index].quantity += amount;


    if (cart[index].quantity <= 0) {

        cart.splice(index, 1);

    }


    saveCart(cart);

    updateCartCount();

    renderCart();

}


function removeProduct(index) {

    const cart = getCart();

    cart.splice(index, 1);

    saveCart(cart);

    updateCartCount();

    renderCart();

}


function checkout() {

    const cart = getCart();


    if (cart.length === 0) {

        alert(
            "Giỏ hàng đang trống."
        );

        return;

    }


    alert(
        "Chức năng thanh toán sẽ được kết nối với Spring Boot."
    );

}