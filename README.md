# PCCE Observability API

Spring Boot API for Cisco PCCE 12.6.2 reporting across AW/HDS/CVP Reporting databases, component status, and dropped-call metrics.

## What It Exposes

- `GET /api/v1/metrics/calls?from=2026-06-01&to=2026-06-15&skillGroup=Sales`
- `GET /api/v1/metrics/call-types?from=2026-06-01&to=2026-06-15`
- `GET /api/v1/agents/stats?from=2026-06-01&to=2026-06-15&agentId=jsmith`
- `GET /api/v1/calls/dropped?from=2026-06-01&to=2026-06-15`
- `GET /api/v1/calls/disposition-breakdown?from=2026-06-01&to=2026-06-15`
- `GET /api/v1/metrics/ivr-containment?from=2026-06-01&to=2026-06-15`
- `GET /api/v1/components/status`
- `GET /api/v1/summary?from=2026-06-01&to=2026-06-15`
- OpenAPI UI: `/swagger-ui/index.html`
- Dashboard UI: `/dashboard`
- Login UI: `/login` and `/profile/login`

JSON uses snake_case, matching the metric names you provided.

## Authentication And Permissions

All API endpoints require HTTP Basic authentication except `/actuator/health`.

Default users are configured in `src/main/resources/application.yml` under `pcce.security.users`:

- `ADMIN`: full access, including `/api/v1/admin/**` and Swagger/OpenAPI.
- `WORKFORCE_MANAGER`: reads contact center metrics and agent stats for configured `allowed-teams`.
- `SUPERVISOR`: reads metrics, dropped calls, component status, and configured team agent stats.
- `AGENT`: reads only their own `/api/v1/agents/stats` using configured `agent-id`.
- `VIEWER`: read-only contact center visibility without solution administration.

Example:

```powershell
curl.exe -u admin:change-me "http://localhost:8080/api/v1/auth/me"
curl.exe -u supervisor.sales:change-me "http://localhost:8080/api/v1/agents/stats?from=2026-06-01&to=2026-06-15&team=Sales"
curl.exe -u agent.1001:change-me "http://localhost:8080/api/v1/agents/stats?from=2026-06-01&to=2026-06-15"
```

Use encoded passwords in production, for example `{bcrypt}...`, and set them through environment variables or a secrets manager. The `{noop}change-me` defaults are only placeholders.

Runtime integration secrets are environment-variable only. `application.yml` intentionally keeps password defaults blank for database, SMTP, Finesse, PCCE API, CVP API, CUCM AXL, SMS, and trust-store credentials.

Admin endpoints:

- `GET /api/v1/admin/users`
- `PUT /api/v1/admin/users/{username}`
- `GET /api/v1/admin/roles`
- `PUT /api/v1/admin/roles/{role}`
- `GET /api/v1/admin/components`
- `PUT /api/v1/admin/components/{name}`
- `GET /api/v1/admin/diagnostics/hds/tables?namePattern=%25Skill%25`
- `GET /api/v1/admin/diagnostics/hds/tables/t_Skill_Group_Interval/columns`
- `GET /api/v1/admin/diagnostics/hds/columns?tablePattern=t_Skill_Group_Interval&columnPattern=%25Hold%25`
- `GET /api/v1/admin/diagnostics/cvp-reporting/tables?namePattern=%25call%25`

Runtime admin updates take effect immediately. Store final production configuration in environment variables, `application.yml`, or a dedicated configuration database so changes survive restarts.

Role permissions are configurable under `pcce.security.role-permissions`. Use this to define exactly what `ADMIN`, `WORKFORCE_MANAGER`, `SUPERVISOR`, `AGENT`, and `VIEWER` can access in production.

Monitoring endpoints:

- `GET /api/v1/monitoring/query-performance?limit=50`
- `GET /api/v1/monitoring/logs?limit=80`
- `GET /api/v1/components/history?limit=100`
- `GET /api/v1/components/{name}/history?limit=100`
- `GET /api/v1/pcce-api/capabilities`
- `GET /api/v1/pcce-api/functions`
- `GET /api/v1/pcce-api/actions`
- `POST /api/v1/pcce-api/actions/{id}/execute`
- `GET /api/v1/pcce-api/rtmt-capabilities`
- `GET /api/v1/pcce-api/spog-capabilities`
- `GET /api/v1/pcce-api/status`

The dashboard consumes these endpoints from the Spring Boot App tab to show slow/failed database queries and recent application log lines.

## Configuration

Set database credentials with environment variables:

```powershell
$env:PCCE_AW_JDBC_URL="jdbc:sqlserver://aw-host:1433;databaseName=awdb;encrypt=true;trustServerCertificate=true"
$env:PCCE_AW_USERNAME="pcce_reader"
$env:PCCE_AW_PASSWORD="..."
$env:PCCE_HDS_JDBC_URL="jdbc:sqlserver://hds-host:1433;databaseName=ucce_hds;encrypt=true;trustServerCertificate=true"
$env:PCCE_HDS_USERNAME="pcce_reader"
$env:PCCE_HDS_PASSWORD="..."
$env:CVP_REPORTING_JDBC_URL="jdbc:informix-sqli://cvp-reporting-host:1526/cvp_data:INFORMIXSERVER=cvp_report;DELIMIDENT=y;"
$env:CVP_REPORTING_USERNAME="cvp_reader"
$env:CVP_REPORTING_PASSWORD="..."
$env:FINESSE_PASSWORD="..."
$env:PCCE_API_PASSWORD="..."
$env:CVP_API_PASSWORD="..."
$env:CUCM_AXL_PASSWORD="..."
$env:PCCE_SMTP_PASSWORD="..."
$env:PCCE_SMS_AUTHORIZATION="Basic ..."
```

Then edit `src/main/resources/application.yml` to enable component probes and set the real hostnames, ports, and URLs.

If the dashboard still shows an old probe URL after editing YAML, check IntelliJ active profile and environment overrides, then restart the Spring Boot run configuration. You can verify the effective runtime component config with:

```powershell
curl.exe -u admin:change-me "http://localhost:8080/api/v1/admin/components"
```

For PCCE REST API surveillance, set:

```powershell
$env:PCCE_API_BASE_URL="https://your-pcce-aw-or-admin-host"
$env:PCCE_API_USERNAME="pcce_api_reader"
$env:PCCE_API_PASSWORD="..."
```

Then enable `pcce.pcce-api.enabled` and the required `pcce.pcce-api.monitors[*].enabled` entries. Keep these checks read-only and use a least-privilege PCCE API account.

The PCCE Integration dashboard includes a controlled API console. Enable only approved `pcce.pcce-api.actions[*]` entries. GET actions are available to operations users; mutating actions require solution administrator permission.

## Important PCCE Notes

The default SQL targets SQL Server AW/HDS tables such as `t_Skill_Group_Interval`, `t_Agent_Interval`, `t_Termination_Call_Detail`, `t_Skill_Group`, `t_Agent`, and `t_Person`. CVP Reporting is configured separately as IBM Informix using `jdbc:informix-sqli`.

For production, validate the default query mappings against your AW/HDS SQL Server schema and CVP Reporting Informix schema. Override any query under `pcce.queries.*` in `application.yml` if your table/column names differ.

The app includes the IBM Informix JDBC dependency `com.ibm.informix:jdbc`. If your bank requires a certified internal driver package instead, install that driver into your internal Maven repository and update the version in `pom.xml`.

## Cisco References

- Cisco Packaged Contact Center Enterprise support and 12.6 documentation: https://www.cisco.com/c/en/us/support/customer-collaboration/packaged-contact-center-enterprise/series.html
- Cisco Packaged Contact Center Enterprise DevNet REST API overview: https://developer.cisco.com/docs/packaged-contact-center/
- Cisco Unified Customer Voice Portal support and reporting documentation: https://www.cisco.com/c/en/us/support/customer-collaboration/unified-customer-voice-portal/series.html
- Practical call disposition code reference used for initial labels: https://comstice.com/blog/post/cisco-ucce-pcce-call-disposition-codes

## Current SIT Host Map

- `vswsitrgr01` / `10.10.90.191`: Rogger
- `vswsitpg01` / `10.10.90.192`: PG
- `vswsitaw01` / `10.10.90.193`: AW/HDS SQL Server
- `vswsitcvp01` / `10.10.90.194`: CVP Call Server
- `vssitfin01` / `10.10.90.195`: Finesse
- `vssitcuic01` / `10.10.90.196`: CUIC
- `vssitvvb01` / `10.10.90.197`: VVB
- `vswsitcvpr01` / `10.10.90.198`: CVP Reporting Informix

If a TCP probe times out, verify the exact listener port from Cisco service configuration and Windows firewall. Do not mark Router/PG/CTI ports as failed until the port is confirmed to be open from the app host.

Router and PG default to `HOST` probe in SIT so the dashboard distinguishes server reachability from Cisco service-port reachability. Add separate TCP checks only after confirming the exact PCCE listener ports from your deployment.

Dropped-call counting is disabled by default to avoid misleading numbers. Use `/api/v1/calls/disposition-breakdown` to see your live HDS distribution, then enable `pcce.queries.dropped-calls-enabled` and configure the approved dropped-call SQL after the bank confirms which call disposition codes count as dropped calls.

Call KPIs read `t_Skill_Group_Interval` first. If that interval table is empty for the selected dates, the app automatically falls back to `t_Termination_Call_Detail.DateTime`, which matches CUIC agent/call-detail style data better in some PCCE environments.

Fields such as `first_call_resolution`, `csat_score`, `adherence_pct`, and some IVR containment logic usually require WFM/QA/survey integrations or customer-specific CVP application events. They are included in the API contract and default to `null` where the Cisco historical tables do not directly provide them.

The default `pcce.queries.ivr-containment` is intentionally a safe no-data Informix query. After confirming your CVP Reporting schema, override it with the bank-approved query. Useful discovery calls:

```powershell
curl.exe -u admin:change-me "http://localhost:8080/api/v1/admin/diagnostics/cvp-reporting/tables?namePattern=%25call%25"
curl.exe -u admin:change-me "http://localhost:8080/api/v1/admin/diagnostics/cvp-reporting/columns?tablePattern=%25call%25&columnPattern=%25start%25"
curl.exe -u admin:change-me "http://localhost:8080/api/v1/admin/diagnostics/cvp-reporting/columns?tablePattern=%25call%25&columnPattern=%25end%25"
```

## Run

Install JDK 17+ and Maven, then run:

```powershell
mvn spring-boot:run
```

For first local run without real PCCE database connectivity:

```powershell
.\scripts\run-local.ps1
```

Then verify:

```powershell
curl.exe -u admin:change-me "http://localhost:8080/api/v1/auth/me"
curl.exe -u admin:change-me "http://localhost:8080/api/v1/components/status"
```

Build:

```powershell
mvn clean package
java -jar target/pcce-observability-0.0.1-SNAPSHOT.jar
```

For on-prem production deployment, see `ON_PREM_RUNBOOK.md`.

For dashboard/report alignment with Cisco stock reports, see `CISCO_STOCK_REPORT_MAPPING.md`.

## Phase 1 Baseline

Phase 1 keeps the existing business logic intact and focuses on repository hygiene, startup confidence, and a clear boundary for later refactoring.

- Main application package: `com.cisco.cx.observability`.
- Legacy IntelliJ launcher remains as a compatibility shim only.
- Generated Maven output belongs under ignored `target/` and should not be tracked.
- Baseline validation command: `mvn clean test`.
- Next development phases should split large services and dashboard scripts incrementally, with tests added before behavior changes.

## Phase 2 Baseline

Phase 2 adds focused regression tests before further integration work. The first protected area is the PCCE/CVP API action catalog used by the support console.

- PCCE read actions such as `users.list` and `machineInventory.list` must stay enabled and read-only.
- CVP operations read actions such as `cvp.syslog.get` and `cvp.snmp.get` must stay enabled and read-only.
- Built-in safe read actions must override stale disabled config entries for the same action ID.
- No external PCCE/CVP connectivity is required for these unit tests.

## Phase 3 Baseline

Phase 3 extends the API support-console safety net without changing runtime behavior.

- Actions with path placeholders must fail fast when required path parameters are missing.
- Missing path parameters are validated before any outbound PCCE/CVP network call is attempted.
- This protects actions such as PCCE `agent.get` and CVP `cvp.app.get` from malformed Cisco API requests.

## Phase 4 Baseline

Phase 4 covers default-deny behavior for mutating Cisco API actions.

- Disabled mutating PCCE actions, such as `skill.create`, must be rejected before any outbound API call.
- Disabled mutating CVP actions, such as `cvp.syslog.update`, must be rejected before any outbound API call.
- Read-only support-console actions remain the only safe defaults.

## Phase 5 Baseline

Phase 5 adds controller-level permission coverage for the API support console.

- Operations users can execute read-only support-console actions.
- Mutating or admin-only PCCE/CVP actions require `PERM_SOLUTION_ADMIN`.
- Controllers must reject unauthorized mutating actions before calling the integration service.

## Phase 6 Baseline

Phase 6 protects support-console request mapping.

- Controller request DTO fields for body, path parameters, and query parameters must be passed unchanged to the integration service.
- This keeps UI-driven PCCE/CVP API execution predictable when operators provide endpoint parameters.

## UI Rebuild Phase

The dashboard UI keeps the existing `/dashboard/index.html` shell and API contracts while rebuilding in small slices.

- Shared visual overrides live in `src/main/resources/static/dashboard/css/ui-rebuild.css`.
- Page filters use searchable multi-select controls with visible removable chips.
- Pagination controls stay page-local and default to 10 rows/cards where supported.
- Thymeleaf page shells now use the requested folders: `templates/analytics`, `templates/operations`, `templates/configuration`, `templates/integrations`, `templates/admin`, and `templates/project`.
- New ES module entry points live under `src/main/resources/static/js`: `cx-core.js`, `cx-charts.js`, and `views/cx-*.js`.

## Remaining Development Plan

Use this plan for parallel work with colleagues. Keep every phase small: add tests first, preserve public REST paths, and avoid mixing package moves with behavior changes.

### Delivery Principles

- Keep PCCE AW/HDS and CVP Reporting databases read-only.
- Prefer app-owned DB tables for users, project plan, audit, runtime settings, and feature toggles.
- Use Live Data or bounded realtime snapshots for live dashboards; keep HDS/CVP historical queries paginated and cached.
- Treat Cisco API mutating actions as disabled by default until admin permissions, audit, and dry-run behavior are tested.
- Every implementation phase must end with `mvn clean test` and a short note of changed files.
- Any UI redesign must preserve existing API contracts until backend tests cover the replacement.

### Workstreams And Owners

| Workstream | Primary focus | Suggested owner | Can run in parallel with |
|---|---|---|---|
| Reporting | HDS/AW/CVP SQL, filters, CUIC alignment | Backend/reporting engineer | Finesse, Project Plan |
| Realtime | Live Data, Finesse, running calls | Finesse/API engineer | Reporting once shared filters are agreed |
| Operations | System health, SNMP/JMX/AppDynamics/logs | Infrastructure engineer | Admin/config hardening |
| Admin/Security | Users, roles, LDAP, secrets, audit | Security/backend engineer | Project Plan |
| UI | Page filters, charts, pagination, dashboard split | Frontend engineer | After contracts/tests exist |
| Architecture | Package split, module boundaries, cleanup | Senior backend reviewer | After each domain is tested |

### Dependency Map

```text
Phase 7 Reporting Filters
  -> Phase 8 Finesse/Live Agent shared filters
  -> Phase 9 CVP Journey filter and trace scoping
  -> Phase 13 UI page-local filters

Phase 10 Project Plan
  -> Phase 11 Admin roles and sharing permissions

Phase 11 Admin/Config
  -> Phase 12 Monitoring collectors and production readiness

Phases 7-12 tested contracts
  -> Phase 13 UI modularization
  -> Phase 14 architecture split
```

### Release Milestones

| Milestone | Included phases | Outcome |
|---|---|---|
| M1 Stabilize Data | 7, 8, 9 | Business, agent, and CVP data behavior is predictable and tested. |
| M2 Operationalize | 10, 11, 12 | Shared planning, admin/security, and monitoring readiness are production-shaped. |
| M3 Maintainability | 13, 14 | UI and backend structure are easier to maintain without changing behavior. |

### Phase 7 - Reporting And Filter Safety

Goal: make business pages reliable before changing dashboards.

Tasks:
- Add focused tests for reporting date presets, skill-group filters, call-type filters, and agent/team filters.
- Add tests for `t_Skill_Group_Interval` to `t_Termination_Call_Detail` fallback behavior.
- Verify empty filter values do not become `NULL`/`UNKNOWN` filters accidentally.
- Document which metrics are HDS-derived, AW realtime-derived, CVP-derived, or externally derived.
- Add a small matrix for each metric: source table/API, filter support, expected null behavior, and fallback.
- Add regression coverage for service level, AHT, ASA, abandoned, dropped, first-call resolution placeholder, and IVR containment placeholder.

Dependencies:
- Phases 1-6 complete.
- Confirm approved CUIC/HDS SQL mappings in `CISCO_STOCK_REPORT_MAPPING.md`.

Can be done by:
- Backend/reporting owner.
- Business validation colleague with CUIC report samples.

Exit criteria:
- `mvn clean test` passes.
- Filters affect only their own page/query.
- No production query behavior changes without a matching test.
- Business owner signs off against at least one CUIC sample export.

### Phase 8 - Finesse And Live-Agent Safety

Goal: protect live agent state and running-call rendering before UI changes.

Tasks:
- Add parser tests for Finesse `Users`, `User`, `Dialogs`, `Teams`, and `SystemInfo` XML responses.
- Add tests for fallback from configured user IDs to discovered users and AW login IDs.
- Add tests proving Finesse status overlays historical HDS agent rows without duplicating agents.
- Define how running calls are displayed: agent, extension, caller, dialog state, queue/skill, and start time.
- Add stale-data rules: when Finesse is unavailable, show last known status age instead of `offline`.
- Define agent identity matching priority: PCCE agent ID, login name, extension, then display name.

Dependencies:
- Phase 7 for shared agent/team filter contracts.
- Stable Finesse sample XML captured from SIT.

Can be done by:
- Finesse/API owner.
- UI colleague can prepare mock screen states in parallel.

Exit criteria:
- `mvn clean test` passes.
- No live Finesse endpoint is required for unit tests.
- Agent row count and live status behavior are deterministic.
- Running-call cards are based on Finesse dialog data, not historical HDS data.

### Phase 9 - CVP IVR Journey And Containment

Goal: make CVP Reporting data explainable and safe for customer-journey views.

Tasks:
- Add Informix SQL mapping tests around CVP node/session DTO mapping.
- Add service tests for IVR containment fallback when node-level data is unavailable.
- Define journey ordering rules so routing nodes appear in customer-event order.
- Add call trace filtering tests: selected call GUID returns only that journey.
- Document disposition mapping for customer abandon, agent/network disconnect, normal completion, and CVP errors.
- Add pagination and date-window tests for CVP IVR calls.
- Define journey grouping key: call GUID plus call start date where required by CVP Reporting.

Dependencies:
- Phase 7 metric filter contracts.
- Approved CVP Reporting query from the bank.

Can be done by:
- CVP/reporting owner.
- Business colleague validates journey wording and disposition labels.

Exit criteria:
- `mvn clean test` passes.
- CVP unavailable state is explicit, not silently empty.
- Trace output is scoped to selected call GUID.
- IVR containment calculation states its numerator, denominator, and excluded dispositions.

### Phase 10 - Project Plan Management

Goal: make the planning page safe for shared editing before UI polish.

Tasks:
- Add service/controller tests for create/update/delete project tasks.
- Add tests for resource dropdowns, status/priority values, date/duration updates, and dependency fields.
- Add audit expectations for task changes.
- Define share modes: viewer, editor, manager/admin.
- Only after tests, improve one-row task editing UI and resource assignment UX.
- Add task dependency model: blocked-by, parent milestone, and external dependency.
- Add import/export path for stakeholder sharing if required: CSV first, Excel/PDF later.

Dependencies:
- App-owned DB migrations are stable.
- Role/permission model from admin screens is confirmed.

Can be done by:
- App-owned DB owner.
- Project manager/business colleague validates fields and workflow.

Exit criteria:
- `mvn clean test` passes.
- Project-plan edits persist in app DB.
- Viewer/editor/admin behavior is explicit.
- Plan page supports team review without exposing operational admin screens.

### Phase 11 - Admin, Config, And Secrets Hardening

Goal: make production configuration safe for banking deployment.

Tasks:
- Add startup validation for `prod` profile: no `{noop}change-me`, no blank required secrets, and no `trustAll` unless explicitly allowed for SIT.
- Add tests for role permission updates and user enable/disable behavior.
- Confirm LDAP/local-auth precedence and fallback behavior.
- Document environment-specific config: local, SIT, UAT, production, DR.
- Add readiness checks for SMTP, SMS, PCCE API, CVP API, Finesse, Live Data, JMX, AppDynamics, and Grafana.
- Add audit coverage for user, role, component, alert, and integration-setting changes.
- Add dry-run preview for dangerous operations where possible.

Dependencies:
- Phases 7-10 identify which integrations are required per environment.
- Security team confirms secret-store approach.

Can be done by:
- Security/config owner.
- Infrastructure colleague supplies environment values.

Exit criteria:
- `mvn clean test` passes.
- Production startup fails fast on unsafe defaults.
- README/environment docs show required variables by environment.
- Security owner approves local-user, LDAP, and emergency-admin behavior.

### Phase 12 - Observability, Logs, JMX, SNMP, And AppDynamics

Goal: separate app monitoring from Cisco component monitoring and make gaps visible.

Tasks:
- Add tests for server metric readiness models: configured, unavailable, disabled, and not configured.
- Define collector strategy per component: SNMP, WMI/WinRM, exporter, JMX, AppDynamics, or external SIEM.
- Add log-target validation for Router, Logger/AW/HDS, PG/CTI, CVP, VVB, Finesse, CUIC, CUCM, gateway, and Eleveo.
- Document CVP JMX prerequisites and AppDynamics dashboard embedding rules.
- Keep remote probes bounded by timeout and cache slow calls.
- Add health-state taxonomy: UP, DEGRADED, DOWN, DISABLED, NOT_CONFIGURED, STALE.
- Add alert routing matrix for SMTP, SMS, webhook/SIEM, and dashboard-only notices.

Dependencies:
- Phase 11 config validation.
- Infrastructure team confirms allowed monitoring protocols.

Can be done by:
- Infrastructure/monitoring owner.
- App owner adds readiness/test coverage.

Exit criteria:
- `mvn clean test` passes.
- System Health distinguishes "service down" from "collector not configured".
- No UI refresh depends on long-running monitoring calls.
- Alert thresholds are configurable and audit-visible.

### Phase 13 - UI Modularization And Page-Specific Filters

Goal: improve maintainability after behavior is protected by tests.

Tasks:
- Split large dashboard JavaScript by domain: business, agents, calls, system, integrations, project, admin.
- Keep current routes and DOM IDs stable unless aliases are added.
- Make filters page-local and multi-select/searchable where needed.
- Standardize pagination controls with default page size 10.
- Replace duplicated rendering helpers with shared utilities only after tests exist.
- Add frontend smoke checklist for dashboard, business, agents, calls, system, integrations, alerts, admin, and project plan.
- Keep chart empty states explicit: no data, unavailable source, permission denied, or filter has no matches.

Dependencies:
- Phases 7-12 provide stable contracts.
- No unresolved high-risk backend behavior gaps.

Can be done by:
- Frontend/UI owner.
- Backend owner reviews API contract impact.

Exit criteria:
- `mvn clean test` passes.
- Browser smoke test confirms main tabs render.
- No page filter changes another page's state.
- Static assets are grouped by page/domain and have cache-busting strategy.

### Phase 14 - Incremental Architecture Split

Goal: move toward the target architecture without rewriting the app.

Tasks:
- Move one domain at a time toward `domain/*`, `service/{domain}`, and `controller/{domain}` or the agreed package convention.
- Start with low-risk domains already covered by tests, such as PCCE/CVP API support console.
- Keep public `/api/v1/...` paths unchanged.
- Compile and test after each move.
- Update `docs/ARCHITECTURE.md` after each completed domain move.
- Remove compatibility scaffolding only after IntelliJ run configs and docs point to the final application class.
- Keep one pull request per domain move where possible.

Dependencies:
- Relevant domain tests are in place.
- Team agrees package naming before moves.

Can be done by:
- Senior backend owner.
- One reviewer checks import-only moves separately from behavior changes.

Exit criteria:
- `mvn clean test` passes after every small move.
- No mixed refactor/feature commits.
- Architecture docs match the codebase.
- Old package/class names are either removed or documented as intentional compatibility shims.

### Backlog Triage Rules

- P0: login, app startup, security bypass, data corruption, or production outage risk.
- P1: incorrect business KPIs, broken filters, failed PCCE/CVP/Finesse connectivity, or blocked admin workflow.
- P2: missing visualization, weak empty states, slow but bounded screens, or incomplete project-plan UX.
- P3: cosmetic improvements, package cleanup, comments, or nonblocking documentation.

### Team Handoff Checklist

- Link the phase and exact task in the ticket.
- State affected endpoints, pages, SQL/API sources, and roles.
- Include sample input/output or a CUIC/Finesse/CVP sample where relevant.
- Add or update tests before changing behavior.
- Run `mvn clean test`.
- Summarize changed files and any remaining risk.
