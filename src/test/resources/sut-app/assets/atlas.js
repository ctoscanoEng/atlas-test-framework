/*
 * Atlas Outdoor — front-end of the application under test.
 *
 * Two behaviours here exist specifically to make the automation framework earn
 * its keep, and both are common in real single page applications:
 *
 *  1. VOLATILE IDS — every element marked data-volatile-id gets a random suffix
 *     appended to its id at load time, exactly like a component library that
 *     regenerates ids on each build. Locators that rely on the raw id break;
 *     locators that declare a data-testid fallback survive.
 *
 *  2. ASYNCHRONOUS RENDERING — the catalogue is fetched over HTTP and rendered
 *     after a short, variable delay. Tests that sleep are flaky; tests that wait
 *     on a condition are stable.
 *
 * window.__atlasPendingRequests is exposed so the framework can wait for the
 * network to go idle instead of guessing.
 */
(function () {
    "use strict";

    window.__atlasPendingRequests = 0;

    const nativeFetch = window.fetch.bind(window);
    window.fetch = function (...args) {
        window.__atlasPendingRequests++;
        return nativeFetch(...args).finally(() => {
            window.__atlasPendingRequests--;
        });
    };

    const TAX_RATE = 0.08;

    const Atlas = {

        // ------------------------------------------------------------ session

        session: {
            save(user) {
                sessionStorage.setItem("atlas.user", JSON.stringify(user));
            },
            current() {
                const raw = sessionStorage.getItem("atlas.user");
                return raw ? JSON.parse(raw) : null;
            },
            clear() {
                sessionStorage.removeItem("atlas.user");
                sessionStorage.removeItem("atlas.cart");
            },
            requireLogin() {
                if (!this.current()) {
                    window.location.replace("index.html?reason=session-required");
                    return false;
                }
                return true;
            }
        },

        // ------------------------------------------------------------ cart

        cart: {
            read() {
                return JSON.parse(sessionStorage.getItem("atlas.cart") || "[]");
            },
            write(lines) {
                sessionStorage.setItem("atlas.cart", JSON.stringify(lines));
                Atlas.renderCartBadge();
            },
            add(product) {
                const lines = this.read();
                const existing = lines.find(line => line.id === product.id);
                if (existing) {
                    existing.quantity++;
                } else {
                    lines.push({id: product.id, name: product.name, price: product.price, quantity: 1});
                }
                this.write(lines);
            },
            remove(id) {
                this.write(this.read().filter(line => line.id !== id));
            },
            count() {
                return this.read().reduce((total, line) => total + line.quantity, 0);
            },
            subtotal() {
                return this.read().reduce((total, line) => total + line.price * line.quantity, 0);
            },
            tax() {
                return round(this.subtotal() * TAX_RATE);
            },
            total() {
                return round(this.subtotal() + this.tax());
            }
        },

        // ------------------------------------------------------------ helpers

        money(amount) {
            return "€ " + round(amount).toFixed(2);
        },

        renderCartBadge() {
            const badge = document.querySelector("[data-testid='cart-count']");
            if (badge) {
                const count = Atlas.cart.count();
                badge.textContent = count;
                badge.style.display = count === 0 ? "none" : "inline-block";
            }
        },

        renderHeader() {
            const holder = document.querySelector("[data-testid='current-user']");
            const user = Atlas.session.current();
            if (holder && user) {
                holder.textContent = user.username;
            }
            const logout = document.querySelector("[data-testid='logout']");
            if (logout) {
                logout.addEventListener("click", event => {
                    event.preventDefault();
                    Atlas.session.clear();
                    window.location.href = "index.html";
                });
            }
            Atlas.renderCartBadge();
        },

        /** Reproduces the id churn of a component library built on every deploy. */
        scrambleVolatileIds() {
            const suffix = Math.random().toString(36).slice(2, 8);
            document.querySelectorAll("[data-volatile-id]").forEach(element => {
                if (element.id) {
                    element.id = element.id + "-" + suffix;
                }
            });
        },

        showError(message) {
            const banner = document.querySelector("[data-testid='error-message']");
            if (!banner) {
                return;
            }
            banner.textContent = message;
            banner.style.display = message ? "block" : "none";
        }
    };

    function round(value) {
        return Math.round(value * 100) / 100;
    }

    document.addEventListener("DOMContentLoaded", () => {
        Atlas.scrambleVolatileIds();
        Atlas.renderHeader();
    });

    window.Atlas = Atlas;
})();
