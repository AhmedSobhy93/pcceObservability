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

## Remaining Development Plan

- Add focused tests around reporting filters and query fallback behavior.
- Add focused tests around Finesse status/dialog parsing before changing live-agent UI behavior.
- Add focused tests around project-plan updates before improving the editable planning UI.
- Then split large JavaScript and service classes incrementally, one domain at a time.
