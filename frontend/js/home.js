document.addEventListener(
    "DOMContentLoaded",
    function () {

        const phoneImage =
            document.querySelector(".phone-image");

        const phoneContent =
            document.querySelector(".phone-content");


        if (!phoneImage) {
            return;
        }


        const observer =
            new IntersectionObserver(

                function (entries) {

                    entries.forEach(function (entry) {

                        if (entry.isIntersecting) {

                            phoneImage.classList.add("show");

                            if (phoneContent) {
                                phoneContent.classList.add("show");
                            }

                        }

                    });

                },

                {
                    threshold: 0.25
                }

            );


        observer.observe(phoneImage);

    }
);