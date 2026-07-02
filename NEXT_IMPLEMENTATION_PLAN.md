# Next Implementation Plan

## A. Current Codebase Decision
- Complete and accepted: backend feature package migration, test alignment, thin Thymeleaf wrapper migration, local HTMX/Alpine installation, read-only component status fragment pilot.
- Functional but incomplete: dashboard SPA, project plan inline JavaScript, external Cisco API action UIs, alert configuration UX, server metrics readiness.
- Intentionally deferred: major observability features, DB schema changes, new external integrations, automated remediation, destructive PCCE/CVP actions.
- Requires SIT/UAT: live PCCE data accuracy, CVP Informix queries, Finesse live calls, role behavior, HTMX fragment access, Grafana iframe CSP.
- Should remain unchanged for now: `PcceProperties` compatibility location and existing REST/JSON APIs.
- Technical debt and risk: large static dashboard runtime, limited UI automated tests, external system behavior depends on environment configuration.

## B. Delivery Roadmap

### Release 1.0 - Operational MVP
| Priority | Feature / Improvement | Existing Module | Business Value | Technical Dependency | Effort | Suggested Owner | AI Support | Validation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P0 | Stabilize dashboard JS split | static dashboard | Faster UI maintenance | Browser regression checks | M | Frontend owner | Codex | Screenshots + console clean |
| P0 | Component health SIT | components | Trustworthy ops status | PCCE server config | S | Ops owner | Codex | Compare with SPOG |
| P0 | Reporting filter correctness | reporting | Business KPI trust | AW/HDS schema mapping | M | Reporting owner | Codex | CUIC report comparison |
| P1 | Alert settings validation | notifications | Safer escalation | SMTP/SMS credentials | S | Ops owner | Codex | Test alert delivery |
| P1 | Project plan polish | projectplan | Stakeholder adoption | App DB | S | PM owner | Codex | Edit/share/export checks |

### Release 1.5 - Advanced Operations and Eleveo Visibility
| Priority | Feature / Improvement | Existing Module | Business Value | Technical Dependency | Effort | Suggested Owner | AI Support | Validation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P0 | Finesse live call accuracy | integration/finesse | Real-time agent insight | Finesse API auth | M | Contact center owner | Codex | Active call test |
| P1 | CVP journey reliability | reporting + cvpapi | Customer journey visibility | Informix query tuning | M | IVR owner | Codex | CUIC/CVP report comparison |
| P1 | Eleveo Grafana embedding | integration/grafana | QM/recording visibility | CSP + Grafana anonymous/auth | S | Recording owner | Codex | iframe render + auth |
| P2 | Certificate/license readiness | monitoring | Proactive support | server inventory | M | Platform owner | Claude review | Expiry report |

### Release 2.0 - Enterprise Observability
| Priority | Feature / Improvement | Existing Module | Business Value | Technical Dependency | Effort | Suggested Owner | AI Support | Validation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P0 | Live Data integration | integration/livedata | Near real-time KPIs | CUIC Live Data token/WebSocket | L | Reporting owner | Codex | Live vs CUIC |
| P0 | Server/JVM/API monitoring | monitoring + advanced | Full platform health | SNMP/JMX/AppDynamics | L | Platform owner | Codex | Synthetic failures |
| P1 | Incident timeline | operations + notifications | Faster RCA | alert history persistence | M | Ops owner | Codex | Incident replay |
| P1 | DR readiness checks | operations | Banking resilience | A/B side inventory | L | Platform owner | Claude review | Failover checklist |

### Release 3.0 - AI-Assisted Operations
| Priority | Feature / Improvement | Existing Module | Business Value | Technical Dependency | Effort | Suggested Owner | AI Support | Validation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P1 | Alert correlation summaries | operations | Reduce noise | incident timeline data | L | Ops owner | Codex + Claude | Human review |
| P1 | Anomaly detection | reporting + monitoring | Proactive support | historical metrics | XL | Data owner | Claude review | Backtesting |
| P2 | Root-cause suggestions | advanced | Faster triage | runbooks + telemetry | XL | Architecture owner | Claude review | Controlled UAT |
| P2 | AI-assisted change impact | projectplan + audit | Safer changes | audit + config history | L | Change owner | Claude review | CAB review |

## C. Team and AI Working Agreement
- Codex responsibilities: scoped Spring Boot implementation, DTOs, services, controllers, repositories, feature template work, repetitive refactoring, test drafts, documentation updates, build-error analysis.
- Claude responsibilities: architecture review, PR review, security/data-masking review, test strategy, acceptance criteria, alert-threshold review, PCCE/Eleveo gap analysis, dependency/coupling review.
- Human approval required: architecture decisions, public API changes, DB migrations, credentials/secrets, access-control changes, production config, alert thresholds, escalation policies, external integration activation, AI data sharing, UAT, production deployment, automated remediation.
- Git rules: protect `main`; use feature branches; one human owner per feature; do not let two AI tools edit the same files simultaneously; every PR includes objective, acceptance criteria, changed files, tests, screenshots for UI, API/config impact, rollback notes, and risks; require `mvn clean test` and `git diff --check`.

## D. First Recommended Implementation Phase
- Objective: split the large static dashboard runtime by page without changing endpoint behavior.
- Expected duration: 3-5 days.
- Primary owner: frontend/platform owner.
- Codex role: move page-specific functions into feature JS modules, preserve selectors/API calls, add smoke checks.
- Claude review role: review coupling, CSP/CSRF, and acceptance criteria.
- Modules likely affected: `static/dashboard/js/*`, `static/dashboard/index.html`, docs.
- Dependencies: browser regression access and screenshots.
- Acceptance criteria: no console errors; each dashboard tab renders; filters and refresh still work; `mvn clean test` and `git diff --check` pass.
- Tests to add/update: controller route tests if templates change; lightweight JS/static path validation where practical.
- SIT/UAT checks: overview, business, agents, calls, system, integrations, admin, project links.
- Rollback approach: restore previous dashboard JS include order and files.

Ready-to-paste Codex prompt:

```text
Work as a senior Spring Boot/Thymeleaf frontend engineer. Read AGENTS.md first.

Task: split the large static dashboard JavaScript by page without changing behavior. Preserve all DOM selectors, API URLs, request payloads, route behavior, CSS classes, and current dashboard views. Do not implement new features. Update docs with the exact files moved. Run mvn clean test and git diff --check. Stop after this phase.
```
