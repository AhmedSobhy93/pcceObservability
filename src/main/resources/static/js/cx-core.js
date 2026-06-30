export const cx = {
    dashboardUrl(view) {
        const url = new URL("/dashboard/index.html", window.location.origin);
        if (view) {
            url.searchParams.set("view", view);
        }
        return url.toString();
    },

    wireDashboardLink(view) {
        document.querySelectorAll("[data-dashboard-view]").forEach(link => {
            link.setAttribute("href", cx.dashboardUrl(link.dataset.dashboardView || view));
        });
    },

    async api(path, options = {}) {
        const response = await fetch(path, {
            credentials: "same-origin",
            headers: {
                "Accept": "application/json",
                ...options.headers
            },
            ...options
        });
        if (!response.ok) {
            throw new Error(`${response.status} ${response.statusText}`);
        }
        return response.json();
    },

    toast(message, level = "info") {
        const container = document.querySelector("[data-toast-region]");
        if (!container) {
            return;
        }
        const item = document.createElement("div");
        item.className = `status-pill ${level}`;
        item.textContent = message;
        container.appendChild(item);
        window.setTimeout(() => item.remove(), 5000);
    }
};
