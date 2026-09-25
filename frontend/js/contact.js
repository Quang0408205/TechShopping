document.addEventListener(
    "DOMContentLoaded",
    function () {

        const form =
            document.getElementById("contactForm");


        if (!form) {
            return;
        }


        form.addEventListener(
            "submit",
            function (event) {

                event.preventDefault();


                const name =
                    document.getElementById(
                        "contactName"
                    ).value;


                alert(
                    "Cảm ơn " +
                    name +
                    "! Tin nhắn của bạn đã được ghi nhận."
                );


                form.reset();

            }
        );

    }
);