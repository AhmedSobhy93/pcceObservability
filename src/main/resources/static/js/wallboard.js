window.cxWallboard = {
    start(refreshFn, intervalMs = 30000) {
        if (typeof refreshFn === "function") {
            refreshFn();
            return window.setInterval(refreshFn, intervalMs);
        }
        return null;
    }
};
