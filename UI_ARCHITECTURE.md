# UI Architecture

## Template Tree
- `templates/layout/base.html`
- `templates/layout/fragments/{sidebar,topbar,filters,pagination,status-strip,modals}.html`
- `templates/layout/error/{403,404,500}.html`
- `templates/auth/login.html`
- `templates/feature/reporting/{overview,business,agents,calls,wallboard,executive,cost}.html`
- `templates/feature/components/{system,servers}.html`
- `templates/feature/components/fragments/status-list.html`
- `templates/feature/operations/{alerts,spog,supervisor,sla-trends,ivr,forecast,wallboard}.html`
- `templates/feature/monitoring/{infra,cti,capacity}.html`
- `templates/feature/workforce/{config,adherence,quality}.html`
- `templates/feature/integration/{integration,cvp,advanced}.html`
- `templates/feature/usermgmt/admin.html`
- `templates/feature/projectplan/project.html`

## Static Resource Tree
- `static/js/vendor/htmx.min.js`
- `static/js/vendor/alpine.min.js`
- `static/js/core/{app,api-client,csrf,common-ui,sidebar,charts-global}.js`
- `static/js/feature/reporting/*`
- `static/js/feature/components/*`
- `static/js/feature/operations/*`
- `static/js/feature/workforce/*`
- `static/js/feature/projectplan/*`
- `static/js/feature/usermgmt/*`
- `static/js/feature/integration/*`
- `static/dashboard/js/*` remains the operational dashboard runtime.

## Page Mapping
- `DashboardController`: routes dashboard aliases to `templates/feature/*`.
- `ProjectPlanController`: `/project` to `feature/projectplan/project`.
- `ComponentFragmentController`: `/fragments/components/status-list` to `feature/components/fragments/status-list :: statusList`.

## Shared JavaScript
- `core/app.js`: dashboard links, fetch wrapper, toast helper, CSRF helper, HTMX CSRF hook.
- `core/api-client.js`: small API wrapper around `cx.api`.
- `core/csrf.js`: CSRF access helper.
- `core/common-ui.js`: empty chart helper.

## HTMX and Alpine
- HTMX version: 2.0.4.
- HTMX local path: `src/main/resources/static/js/vendor/htmx.min.js`.
- HTMX source: `https://unpkg.com/htmx.org@2.0.4/dist/htmx.min.js`.
- HTMX license: BSD-2-Clause per htmx.org package.
- HTMX SHA-256: `E209DDA5C8235479F3166DEFC7750E1DBCD5A5C1808B7792FC2E6733768FB447`.
- Alpine.js version: 3.14.8.
- Alpine local path: `src/main/resources/static/js/vendor/alpine.min.js`.
- Alpine source: `https://unpkg.com/alpinejs@3.14.8/dist/cdn.min.js`.
- Alpine license: MIT.
- Alpine SHA-256: `B600E363D99D95444DB54ACBFB2DEFFEC9AE792AA99A09229BCDA078E5B55643`.

## CSRF Handling
- Base layout renders Spring CSRF meta tags.
- `core/app.js` adds the CSRF header to shared fetch calls and HTMX requests.
- No CSRF token is stored in browser storage.

## Manual UI Validation Checklist
1. Login at `/login` and `/profile/login`.
2. Open `/overview`, `/business`, `/agents`, `/calls`, `/system`, `/project`.
3. Confirm `/system` loads the HTMX component fragment and refreshes without console errors.
4. Confirm unauthorized users cannot access `/fragments/components/status-list`.
5. Confirm `/dashboard/index.html` still loads the existing dashboard SPA.
6. Confirm project plan inline edit, add, save, export, and delete still work.

## Deferred UX Improvements
- Split the large `/dashboard/js/dashboard.js` by page after browser regression coverage exists.
- Extract project plan inline JavaScript after preserving Thymeleaf data bindings through safe page config.
- Add reusable table/filter/pagination fragments to high-traffic pages after SIT.
