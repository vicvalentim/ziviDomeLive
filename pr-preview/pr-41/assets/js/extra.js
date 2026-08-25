(function () {
    "use strict";

    const copy = {
        en: {
            title: "Cookie preferences",
            description: "This documentation stores essential interface and consent preferences in your browser. Optional GitHub repository data is loaded only if you enable it. You can change this choice at any time from the Cookies tab.",
            github: "GitHub repository data",
            tab: "Cookies",
            tabLabel: "Manage cookie preferences",
            accept: "Accept",
            reject: "Reject",
            manage: "Manage settings"
        },
        pt: {
            title: "Preferências de cookies",
            description: "Esta documentação armazena no navegador preferências essenciais de interface e consentimento. Dados opcionais do repositório no GitHub só são carregados se você permitir. Altere essa escolha a qualquer momento pela aba Cookies.",
            github: "Dados do repositório no GitHub",
            tab: "Cookies",
            tabLabel: "Gerenciar preferências de cookies",
            accept: "Aceitar",
            reject: "Rejeitar",
            manage: "Gerenciar configurações"
        }
    };

    let activeConsent = null;
    let activeTab = null;
    let escapeHandlerBound = false;

    function closeCookiePreferences() {
        if (!activeConsent || activeConsent.hidden) {
            return;
        }
        activeConsent.hidden = true;
        activeTab?.setAttribute("aria-expanded", "false");
        activeTab?.focus({ preventScroll: true });
    }

    function bindGlobalEscapeHandler() {
        if (escapeHandlerBound) {
            return;
        }
        escapeHandlerBound = true;
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeCookiePreferences();
            }
        });
    }

    function localizeConsent(consent, language) {
        const strings = copy[language];
        const heading = consent.querySelector(".md-consent__form h4");
        const description = consent.querySelector(".md-consent__form > p");
        const githubInput = consent.querySelector('input[name="github"]');
        const githubLabel = githubInput?.closest("label");
        const accept = consent.querySelector('.md-consent__controls button:not([type="reset"])');
        const reject = consent.querySelector('.md-consent__controls button[type="reset"]');
        const manage = consent.querySelector('.md-consent__controls label[for="__settings"]');

        if (heading) {
            heading.id = "zd-cookie-title";
            heading.tabIndex = -1;
            heading.textContent = strings.title;
        }
        if (description) {
            description.textContent = strings.description;
        }
        if (githubLabel?.lastChild?.nodeType === Node.TEXT_NODE) {
            githubLabel.lastChild.nodeValue = ` ${strings.github}`;
        }
        if (accept) {
            accept.textContent = strings.accept;
        }
        if (reject) {
            reject.textContent = strings.reject;
        }
        if (manage) {
            manage.textContent = strings.manage;
        }

        const dialog = consent.querySelector(".md-consent__inner");
        dialog?.setAttribute("role", "dialog");
        dialog?.setAttribute("aria-modal", "true");
        dialog?.setAttribute("aria-labelledby", "zd-cookie-title");
    }

    function createCookieTab(consent, language) {
        let tab = document.querySelector("[data-zd-cookie-settings]");
        if (!tab) {
            tab = document.createElement("button");
            tab.type = "button";
            tab.className = "zd-cookie-tab";
            tab.dataset.zdCookieSettings = "";
            tab.setAttribute("aria-controls", "__consent");
            document.body.appendChild(tab);
        }

        tab.textContent = copy[language].tab;
        tab.setAttribute("aria-label", copy[language].tabLabel);
        tab.setAttribute("aria-expanded", String(!consent.hidden));

        if (!tab.dataset.zdCookieBound) {
            tab.dataset.zdCookieBound = "true";
            tab.addEventListener("click", () => {
                const settings = consent.querySelector("#__settings");
                consent.hidden = false;
                if (settings) {
                    settings.checked = true;
                }
                tab.setAttribute("aria-expanded", "true");
                window.requestAnimationFrame(() => {
                    consent.querySelector(".md-consent__form h4")?.focus({ preventScroll: true });
                });
            });
        }
        return tab;
    }

    function initializeCookiePreferences() {
        const consent = document.querySelector('[data-md-component="consent"]');
        if (!consent) {
            return;
        }

        const language = document.documentElement.lang.toLowerCase().startsWith("pt") ? "pt" : "en";
        activeConsent = consent;
        localizeConsent(consent, language);
        activeTab = createCookieTab(consent, language);

        const overlay = consent.querySelector(".md-consent__overlay");
        if (overlay && !overlay.dataset.zdCookieBound) {
            overlay.dataset.zdCookieBound = "true";
            overlay.addEventListener("click", closeCookiePreferences);
        }

        if (!consent.dataset.zdCookieObserved && "MutationObserver" in window) {
            consent.dataset.zdCookieObserved = "true";
            new MutationObserver(() => {
                activeTab?.setAttribute("aria-expanded", String(!consent.hidden));
            }).observe(consent, { attributes: true, attributeFilter: ["hidden"] });
        }
        bindGlobalEscapeHandler();
    }

    function initializeDocumentation() {
        document.documentElement.classList.add("docs-ready");
        initializeCookiePreferences();
    }

    if (typeof document$ !== "undefined") {
        document$.subscribe(initializeDocumentation);
    } else if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeDocumentation, { once: true });
    } else {
        initializeDocumentation();
    }
})();
