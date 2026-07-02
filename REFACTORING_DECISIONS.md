# Refactoring Decisions

| Area | Decision | Reason | Risk | Validation |
| ---- | -------- | ------ | ---- | ---------- |
| PCCE properties | Split partially and retain compatibility facade | Natural sub-prefixes exist, but full split would touch high-risk consumers | Low for scoped domains; high for full split | `PcceConfigurationBindingTest`; `mvn clean test` |
| Reporting service config | Inject `PcceReportingProperties` and `PerformanceProperties` | Reporting needs queries and timing thresholds, not the full root config | Low | Existing reporting tests updated and passed |
| PCCE API monitor config | Inject `PcceApiProperties` | Service only needs `pcce.pcce-api` | Low | Existing PCCE API tests updated and passed |
| CVP API monitor config | Inject `CvpApiProperties` | Service only needs `pcce.cvp-api` | Low | Existing CVP API tests updated and passed |
| `PcceProperties` full class split | Deferred | Broad compatibility contract across security, admin, topology, and external integrations | High | Documented in migration plan |
| Reporting SQL/service split | Deferred | Needs Cisco schema regression data before safe extraction | High | Documented in audit |
| UI/template/static rewrites | Deferred | Phase 10 prohibits visual redesign and behavior changes | Medium | No UI files changed |
| Security behavior | Preserved | Route/login/access behavior is production-sensitive | High | Existing security/context tests passed |
