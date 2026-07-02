import { cx } from "./app.js";

export const csrf = {
    token() {
        return cx.csrf()?.token || "";
    },
    headerName() {
        return cx.csrf()?.header || "";
    },
    headers() {
        return cx.csrfHeaders();
    }
};
