(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        var toggle = document.querySelector("[data-nav-toggle]");
        var nav = document.querySelector("[data-nav]");
        if (toggle && nav) {
            toggle.addEventListener("click", function () {
                var open = nav.classList.toggle("nav-open");
                toggle.setAttribute("aria-expanded", String(open));
            });
        }

        document.querySelectorAll("form[data-confirm]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                var message = form.getAttribute("data-confirm") || "Are you sure?";
                if (!window.confirm(message)) {
                    event.preventDefault();
                }
            });
        });

        var exchange = document.getElementById("exchangeType");
        var priceRow = document.getElementById("priceRow");
        var lendingRow = document.getElementById("lendingRow");

        function syncExchangeFields() {
            if (!exchange) {
                return;
            }
            var value = exchange.value;
            if (priceRow) {
                priceRow.classList.toggle("is-hidden", value !== "SELL");
            }
            if (lendingRow) {
                lendingRow.classList.toggle("is-hidden", value !== "LEND");
            }
        }

        if (exchange) {
            exchange.addEventListener("change", syncExchangeFields);
            syncExchangeFields();
        }
    });
})();
