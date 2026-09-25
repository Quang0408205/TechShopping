document.addEventListener(
    "DOMContentLoaded",
    function () {

        const buttons =
            document.querySelectorAll(".filter-btn");

        const products =
            document.querySelectorAll(".product-card");


        buttons.forEach(function (button) {

            button.addEventListener(
                "click",
                function () {

                    buttons.forEach(function (btn) {

                        btn.classList.remove("active");

                    });


                    button.classList.add("active");


                    const category =
                        button.dataset.category;


                    products.forEach(function (product) {

                        if (
                            category === "all" ||
                            product.dataset.category === category
                        ) {

                            product.classList.remove("hidden");

                        } else {

                            product.classList.add("hidden");

                        }

                    });

                }
            );

        });


        /* Đọc category từ URL */

        const params =
            new URLSearchParams(
                window.location.search
            );

        const category =
            params.get("category");


        if (category) {

            const button =
                document.querySelector(
                    `.filter-btn[data-category="${category}"]`
                );

            if (button) {
                button.click();
            }

        }

    }
);