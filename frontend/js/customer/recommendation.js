document.addEventListener(
    "DOMContentLoaded",
    function () {

        const button =
            document.getElementById(
                "recommendationButton"
            );

        const input =
            document.getElementById(
                "recommendationInput"
            );

        const result =
            document.getElementById(
                "recommendationProducts"
            );


        button.addEventListener(
            "click",
            function () {

                const text =
                    input.value.toLowerCase();


                if (!text.trim()) {

                    alert(
                        "Vui lòng nhập nhu cầu của bạn."
                    );

                    return;

                }


                let products = [];


                if (
                    text.includes("laptop") ||
                    text.includes("lập trình") ||
                    text.includes("game")
                ) {

                    products = [

                        {
                            name: "Laptop Gaming Pro",
                            price: 32990000,
                            category: "Laptop"
                        },

                        {
                            name: "Laptop Pro 14",
                            price: 24990000,
                            category: "Laptop"
                        }

                    ];

                }

                else if (
                    text.includes("điện thoại") ||
                    text.includes("phone")
                ) {

                    products = [

                        {
                            name: "Smartphone X Pro",
                            price: 21990000,
                            category: "Điện thoại"
                        },

                        {
                            name: "Smartphone Ultra",
                            price: 28990000,
                            category: "Điện thoại"
                        }

                    ];

                }

                else if (
                    text.includes("tablet") ||
                    text.includes("máy tính bảng")
                ) {

                    products = [

                        {
                            name: "Tablet Air 11",
                            price: 15490000,
                            category: "Tablet"
                        }

                    ];

                }

                else {

                    products = [

                        {
                            name: "Laptop Pro 14",
                            price: 24990000,
                            category: "Laptop"
                        },

                        {
                            name: "Smartphone X Pro",
                            price: 21990000,
                            category: "Điện thoại"
                        }

                    ];

                }


                displayRecommendations(products);

            }
        );


        function displayRecommendations(products) {

            result.innerHTML = "";


            products.forEach(function (product) {

                const card =
                    document.createElement("div");

                card.className = "product-card";


                card.innerHTML = `

                    <div class="product-image">

                        <img
                            src="../assets/images/smartphone.jpg"
                            alt="${product.name}"
                        >

                    </div>

                    <div class="product-info">

                        <span class="product-category">
                            ${product.category}
                        </span>

                        <h3>
                            ${product.name}
                        </h3>

                        <p class="product-price">
                            ${formatPrice(product.price)}
                        </p>

                        <button
                            class="add-cart"
                            data-name="${product.name}"
                            data-price="${product.price}"
                        >
                            THÊM VÀO GIỎ
                        </button>

                    </div>

                `;


                result.appendChild(card);

            });


            /*
             * Vì card được tạo bằng JavaScript
             * nên cần gắn sự kiện lại cho nút
             */

            const buttons =
                result.querySelectorAll(".add-cart");


            buttons.forEach(function (button) {

                button.addEventListener(
                    "click",
                    function () {

                        addToCart(
                            button.dataset.name,
                            Number(button.dataset.price)
                        );

                    }
                );

            });

        }

    }
);