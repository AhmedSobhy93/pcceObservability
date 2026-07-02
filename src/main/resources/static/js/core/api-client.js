import { cx } from "./app.js";

export const apiClient = {
    get(path) {
        return cx.api(path);
    },
    send(path, options = {}) {
        return cx.api(path, options);
    }
};
