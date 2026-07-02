# PCCE Properties Assessment and Migration Plan

## Existing Configuration Contract
- Existing prefixes: `pcce.queries`, `pcce.components`, `pcce.security`, `pcce.ssl`, `pcce.audit`, `pcce.operations`, `pcce.performance`, `pcce.pcce-api`, `pcce.cvp-api`, `pcce.cucm-axl`, `pcce.agent-provisioning`, `pcce.notifications`, `pcce.grafana`, `pcce.finesse`, `pcce.jmx`, `pcce.app-dynamics`, `pcce.live-data`, and `pcce.datasources`.
- Existing environment variables: preserved as declared in `application.yml` and profile files; no names were changed.
- Current consumers: reporting, component health, monitoring, operations, notifications, security, Finesse, Live Data, PCCE API, CVP API, Grafana, admin, audit, and workforce services.
- Existing validation: `PcceProperties` is `@ConfigurationProperties(prefix = "pcce")` and selected nested values use validation annotations.
- Existing risks: one root class owns topology, API clients, users, security, defaults, thresholds, data sources, and integration credentials.

## Decision
- Decision: split partially with compatibility facade.
- Reason: natural sub-prefixes already exist, but full migration would touch high-risk security, topology, admin, and external-system consumers.

## Proposed Configuration Domains
- Topology: keep under compatibility facade for now because component enums, probe targets, and admin editing are coupled.
- API: `PcceApiProperties` for `pcce.pcce-api`; `CvpApiProperties` for `pcce.cvp-api`.
- Monitoring: keep component thresholds in `PcceProperties` until component probe services have tests.
- Reporting: `PcceReportingProperties` for `pcce.queries`.
- Performance: `PerformanceProperties` for `pcce.performance`.
- Data sources: keep in existing datasource configuration until datasource profile tests are added.
- Credentials/security: keep externalized through env vars; do not expose to browser JavaScript.

## Compatibility Plan
- Property keys preserved: yes.
- Environment variables preserved: yes.
- Deprecated accessors: not deprecated yet; too many current consumers still need the compatibility facade.
- Binding tests: `PcceConfigurationBindingTest` verifies scoped beans and legacy facade bind from the same keys.
- Rollback plan: services can be reverted to inject `PcceProperties` without changing YAML or env vars.

## Changes Applied
- Added `PcceReportingProperties` bound to `pcce.queries`.
- Added `PerformanceProperties` bound to `pcce.performance`.
- Added `PcceApiProperties` bound to `pcce.pcce-api`.
- Added `CvpApiProperties` bound to `pcce.cvp-api`.
- Registered new properties in `CxObservabilityApplication`.
- Updated `ReportingService`, `PcceApiMonitoringService`, and `CvpApiMonitoringService` to use scoped configuration.
- Updated affected tests and added a property binding compatibility test.

## Deferred Migration Items
- Move datasource settings to dedicated AW/HDS/CVP properties after datasource tests exist.
- Move component topology and thresholds after component probe tests exist.
- Move security users/LDAP/rate-limit settings after security regression tests are expanded.
- Move Finesse, Live Data, JMX, AppDynamics, Grafana, notifications, and AXL settings only when each feature has dedicated tests.
