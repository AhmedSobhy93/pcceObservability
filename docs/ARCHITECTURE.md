# CX Observability Platform - Architecture Guide

## Package Layout

- `com.cisco.cx.observability` - root Spring Boot application package.
- `config/` - Spring configuration beans and configuration properties.
- `entity/` - JPA entities for local application-owned data.
- `model/` - API/domain records used by controllers and services.
- `repository/` - Spring Data repositories and future JdbcTemplate repositories.
- `security/` - authentication, authorization, rate limiting, and security helpers.
- `service/` - business logic, external integration clients, reporting aggregation, and operations workflows.
- `web/` - MVC and REST controllers, filters, and exception handling.

## Target Package Direction

The current codebase has been renamed to the production base package. Future feature work should move gradually toward domain packages:

- `domain/agent`, `domain/call`, `domain/component`, `domain/cvp`, `domain/finesse`, `domain/operations`, `domain/project`
- `service/{domain}`
- `web/{domain}`
- `web/{domain}/dto`
- `infrastructure/http`, `infrastructure/db`, `infrastructure/security`, `infrastructure/web`

Do not do a large file split and behavior change in the same commit. Move classes first, compile, then extract logic.

## Naming Conventions

| Layer | Convention | Example |
|---|---|---|
| Base package | `com.cisco.cx.observability` | `com.cisco.cx.observability.service` |
| Application class | Product name + `Application` | `CxObservabilityApplication` |
| Domain model | Plain noun | `CallMetric`, `AgentStat` |
| Request DTO | `NounUpdateRequest` / `NounCreateRequest` | `ProjectTaskUpdateRequest` |
| Enum constants | `SCREAMING_SNAKE_CASE` | `ON_CALL` |
| Business service | `NounService` | `ProjectPlanService` |
| Integration monitor | `NounMonitoringService` | `CvpApiMonitoringService` |
| Controller | `NounController` | `ReportingController` |
| REST paths | Stable `/api/v1/...` paths | `/api/v1/agents/stats` |
| CSS variables | `--cx-category-variant` | `--cx-accent-teal` |
| HTML IDs | `cx-view-element` for new UI | `cx-kpi-offered` |
| JS render function | `render{View}` | `renderAgents` |
| JS load function | `load{Data}` | `loadDashboard` |

## Integration Rules

1. Keep existing public REST paths unless a replacement alias is added.
2. Use Skill Groups only. Do not add Precision Queue features unless explicitly requested.
3. Do not add `trustAll` certificate bypasses. Use `HttpClientConfig` and `PCCE_SSL_TRUST_STORE`.
4. Keep AW/HDS SQL Server queries and CVP Informix queries separate.
5. Protect mutating browser actions with CSRF.
6. Keep live data and Finesse calls cached or bounded by timeout so UI refresh does not block.
7. Prefer app-owned DB tables for users, project plan, audit, and runtime configuration; keep PCCE DB read-only.

## Adding a New Integration

1. Add config under `application.yml` with environment variable overrides.
2. Add a focused service/client class under the relevant domain package.
3. Add records for API responses in `model/` or the target `domain/` package.
4. Add a controller under `web/` or `web/{domain}` with stable `/api/v1/...` paths.
5. Add Admin diagnostics readiness so operations can see missing configuration.
6. Add logging with snake_case keys.
7. Add tests or at least a compile check before merging.

## Current Refactor Status

- Maven coordinates have been renamed to `com.cisco.cx:cx-observability:1.0.0`.
- Java base package has been renamed to `com.cisco.cx.observability`.
- Application class has been renamed to `CxObservabilityApplication`.
- The legacy IntelliJ launcher `com.example.pcceobservability.PcceObservabilityApplication` remains as a compatibility shim only.
- Stable dashboard aliases now route to the SPA shell with view selection, for example `/business`, `/agents`, `/calls`, `/system`, `/pcce`, `/cvp`, `/alerts`, `/spog`, `/admin`, and `/app`.
- Shared dashboard CSS now exposes `--cx-*` variables while keeping older variable aliases intact.
- The requested target folders now exist as tracked migration scaffolding:
  - `controller/`, `domain/*`, `infrastructure/*`, and `poller/`
  - `templates/layout`, `templates/auth`, `templates/core`, `templates/operations`, `templates/workforce`, `templates/technical`, and `templates/executive`
  - `static/css/dashboard.css`, `static/js/charts.js`, `static/js/wallboard.js`, and `static/js/sidebar.js`
- Domain package splitting remains planned and should be done incrementally.
