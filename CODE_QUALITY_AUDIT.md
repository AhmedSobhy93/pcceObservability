# Code Quality Audit

## Baseline
- Branch: `master`
- Initial build result: `mvn clean test` passed after Maven network access was allowed; the first sandboxed run failed on dependency resolution.
- Test result after code changes: 23 tests passed, 0 failures.
- Source size: 129 main Java source files; 163 Java type declarations.
- Test count: 9 test files; 23 test cases after Phase 10.
- `git diff --check`: clean at baseline.

## Class Assessment Matrix
| Class | Module | Main Responsibility | Risk | Decision | Reason |
|---|---|---|---|---|---|
| `PcceProperties` | config | Legacy compatibility facade for all `pcce.*` configuration | High | SPLIT BY RESPONSIBILITY, PARTIAL ONLY | Broadly used across security, reporting, monitoring, integration, and admin modules; full split would risk deployment compatibility. |
| `PcceReportingProperties` | feature/reporting | Reporting query configuration bound to `pcce.queries` | Low | EXTRACT REUSABLE COMPONENT | Natural prefix boundary; used by reporting service without changing property keys. |
| `PerformanceProperties` | platform/config | Performance thresholds bound to `pcce.performance` | Low | EXTRACT REUSABLE COMPONENT | Cross-cutting thresholds are not reporting-specific. |
| `PcceApiProperties` | feature/integration/pcceapi | PCCE API configuration bound to `pcce.pcce-api` | Low | EXTRACT REUSABLE COMPONENT | Natural API integration boundary and already covered by tests. |
| `CvpApiProperties` | feature/integration/cvpapi | CVP API configuration bound to `pcce.cvp-api` | Low | EXTRACT REUSABLE COMPONENT | Natural CVP API boundary and already covered by tests. |
| `ReportingService` | feature/reporting | HDS/AW/CVP reporting aggregation | High | SPLIT BY RESPONSIBILITY, DEFERRED | It mixes business metrics, agent stats, call flow, CVP journey, references, and query timing. Behavior depends on live Cisco schemas. |
| `PcceApiMonitoringService` | feature/integration/pcceapi | PCCE API catalog, action execution, monitor probes | Medium | SMALL CLEANUP | Switched to scoped properties; deeper client/action split should wait for API regression tests. |
| `CvpApiMonitoringService` | feature/integration/cvpapi | CVP API catalog, action execution, monitor probes | Medium | SMALL CLEANUP | Switched to scoped properties; deeper client/action split should wait for API regression tests. |
| `AgentSkillManagementService` | feature/workforce | PCCE and CUCM AXL agent/skill provisioning orchestration | High | DO NOT CHANGE YET - HIGH RISK | Mutating external-system workflows need explicit integration tests and operator approval. |
| `FinesseIntegrationService` | feature/integration/finesse | Finesse API live status and dialog retrieval | High | SPLIT BY RESPONSIBILITY, DEFERRED | API client, mapper, and aggregation responsibilities are mixed but live behavior is sensitive. |
| `ComponentStatusService` | feature/components | Component probe execution and state aggregation | Medium | SPLIT BY RESPONSIBILITY, DEFERRED | Probe runner and status aggregation can be separated after probe tests exist. |
| `OperationsService` | feature/operations | Readiness and operational alert assessment | Medium | REQUIRES TEST COVERAGE | Contains business alert logic with limited focused tests. |
| `AlertNotificationService` | feature/notifications | Alert dispatch to SMTP/SMS/webhook | Medium | SPLIT BY RESPONSIBILITY, DEFERRED | Sender implementations and throttling should be tested before extracting. |
| `AdminController` | feature/usermgmt | Runtime admin APIs for users, roles, components, notifications | Medium | SPLIT BY RESPONSIBILITY, DEFERRED | Several admin concerns share one controller; endpoint compatibility needs controller tests first. |
| `ProjectPlanService` | feature/projectplan | Project plan CRUD and summaries | Medium | REQUIRES TEST COVERAGE | Business rules should be covered before UI/data model cleanup. |
| `DataSourceConfig` | platform/persistence | AW/HDS/CVP/app datasource wiring | Medium | KEEP AS IS | Correct platform ownership; no safe property-key changes in this phase. |
| `HttpClientConfig` | platform/http | SSL/trust-store HTTP client wiring | Medium | KEEP AS IS | Security-sensitive; no defect found. |
| `SecurityConfig` | security | Route protection and login/logout security | High | KEEP AS IS | Security behavior must remain unchanged in this phase. |
| `com.example...PcceObservabilityApplication` | compatibility | Legacy IntelliJ launch-class shim | Low | KEEP AS IS | Protects old run configurations after package migration. |

## High-Value Refactoring Candidates
- Split `ReportingService` into reporting reference, call metrics, agent metrics, CVP journey, and timing/recording collaborators after adding database-result tests.
- Split PCCE/CVP API services into catalog provider, HTTP client, request mapper, and monitor runner after API action regression tests.
- Split notification delivery into evaluator, dispatcher, SMTP sender, SMS sender, and webhook sender.
- Add controller tests before splitting `AdminController` and project-plan endpoints.
- Continue UI JavaScript split only after browser smoke checks protect the current dashboard behavior.

## Intentionally Deferred Changes
- Full `PcceProperties` migration was deferred because many modules still require compatibility access to nested enums, defaults, users, components, and security settings.
- Reporting SQL and external Cisco integration behavior were not changed.
- No Thymeleaf or static asset rewrites were performed.
- No endpoint URL, model attribute, API payload, database schema, or security rule was changed.

## Technical Debt
- `AGENTS.md` names Spring Boot 3.5, while the current `pom.xml` parent is Spring Boot 3.3.5; upgrade should be handled as a dedicated dependency-alignment phase.
- `ReportingService`, Finesse, component probes, alert delivery, and admin APIs remain larger than ideal.
- Test coverage is concentrated around context startup, security, dashboard routing, reporting filters, and PCCE/CVP API action guards.
- Live Cisco behavior still needs SIT against AW/HDS/CVP/Finesse/CUIC environments.
- Static vendor assets trigger noisy text scans because minified code includes generic tokens.

## Security and Configuration Findings
- No hardcoded secret values were added.
- Existing sensitive values are externalized through environment placeholders such as `${PCCE_API_PASSWORD:}` and `${CVP_REPORTING_PASSWORD:}`.
- Internal hostnames and non-secret defaults remain in `application.yml`; production should override them through profile/env configuration.
- Existing admin/user passwords remain configuration-bound and must be provided through env vars or a secret manager.
- No new browser-exposed secret values were introduced.

## Test Coverage Findings
- Added one configuration-binding test proving scoped domain properties and the legacy facade bind the same existing keys.
- Existing service tests were updated for scoped property injection.
- Missing high-value tests: reporting SQL result mapping, component probes, notification dispatch, Finesse live calls, admin user/role APIs, project-plan editing, and HTMX fragments.
