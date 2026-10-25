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
                priceRow.classList.toggle("is-hidden", value === "DONATE");
            }
            if (lendingRow) {
                lendingRow.classList.toggle("is-hidden", value !== "LEND");
            }
        }

        var categoryChoice = document.getElementById("categoryChoice");
        var categoryInput = document.getElementById("categoryInput");

        function syncCategory(fromUser) {
            var creating = categoryChoice.value === "__new__";
            if (fromUser) {
                categoryInput.value = creating ? "" : categoryChoice.value;
            }
            categoryInput.classList.toggle("is-hidden", !creating);
            if (creating && fromUser) {
                categoryInput.focus();
            }
        }

        if (categoryChoice && categoryInput) {
            if (categoryInput.value && categoryChoice.value !== categoryInput.value) {
                categoryChoice.value = "__new__";
            }
            categoryChoice.addEventListener("change", function () {
                syncCategory(true);
            });
            syncCategory(false);
        }

        if (exchange) {
            exchange.addEventListener("change", syncExchangeFields);
            syncExchangeFields();
        }
    });
})();
