# PCCE Observability API

Spring Boot API for Cisco PCCE 12.6.2 reporting across AW/HDS/CVP Reporting databases, component status, and dropped-call metrics.

## What It Exposes

- `GET /api/v1/metrics/calls?from=2026-06-01&to=2026-06-15&skillGroup=Sales`
- `GET /api/v1/agents/stats?from=2026-06-01&to=2026-06-15&agentId=jsmith`
- `GET /api/v1/calls/dropped?from=2026-06-01&to=2026-06-15`
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
- `GET /api/v1/admin/components`
- `PUT /api/v1/admin/components/{name}`
- `GET /api/v1/admin/diagnostics/hds/tables?namePattern=%25Skill%25`
- `GET /api/v1/admin/diagnostics/hds/tables/t_Skill_Group_Interval/columns`
- `GET /api/v1/admin/diagnostics/cvp-reporting/tables?namePattern=%25call%25`

Runtime admin updates take effect immediately. Store final production configuration in environment variables, `application.yml`, or a dedicated configuration database so changes survive restarts.

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

## Important PCCE Notes

The default SQL targets SQL Server AW/HDS tables such as `t_Skill_Group_Interval`, `t_Agent_Interval`, `t_Termination_Call_Detail`, `t_Skill_Group`, `t_Agent`, and `t_Person`. CVP Reporting is configured separately as IBM Informix using `jdbc:informix-sqli`.

For production, validate the default query mappings against your AW/HDS SQL Server schema and CVP Reporting Informix schema. Override any query under `pcce.queries.*` in `application.yml` if your table/column names differ.

The app includes the IBM Informix JDBC dependency `com.ibm.informix:jdbc`. If your bank requires a certified internal driver package instead, install that driver into your internal Maven repository and update the version in `pom.xml`.

Dropped calls are currently classified with `t_Termination_Call_Detail.CallDisposition IN (3, 13, 26, 27, 28, 29, 30)`. Confirm these disposition codes with your Cisco reporting dictionary and business definition of "dropped call" before operational use.

Fields such as `first_call_resolution`, `csat_score`, `adherence_pct`, and some IVR containment logic usually require WFM/QA/survey integrations or customer-specific CVP application events. They are included in the API contract and default to `null` where the Cisco historical tables do not directly provide them.

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
