# AI Agent Conventions for CX Observability

## Must Follow

- Base package: `com.cisco.cx.observability`.
- Keep existing `/api/v1/...` endpoints unless a deprecated alias is added.
- Use Skill Groups only for this environment.
- Use `HttpClientConfig` for TLS. Do not add `trustAll`, custom no-op `X509TrustManager`, or hostname bypass code.
- Use environment variables for deploy-time configuration.
- Keep PCCE AW/HDS/CVP data sources read-only from application code.
- Keep UI refresh bounded by timeouts and cached data where possible.
- Use `apply_patch` for manual edits.
- Run `mvn test` when dependencies are available.

## Should Follow

- Place new controller DTOs under `web/{domain}/dto` once the domain package split starts.
- Place new business logic under `service/{domain}`.
- Place new pure records/enums under `domain/{domain}` when moving away from the legacy `model` package.
- Use snake_case structured log keys: `elapsed_ms`, `status_code`, `request_id`.
- Add Admin readiness checks for any new external integration.
- Add a documentation note in `docs/ARCHITECTURE.md` when introducing a new subsystem.

## Must Not Do

- Do not add Precision Queue logic.
- Do not call destructive git commands.
- Do not store real passwords, Basic auth tokens, or bearer tokens in committed YAML.
- Do not change PCCE/CVP/Finesse API paths without preserving the old UI/API entry point.
- Do not put long-running remote probes directly in UI render paths without timeout and fallback.

## Refactor Guidance

1. Do package/file moves separately from logic changes.
2. Compile after each structural section when Maven dependencies are available.
3. Split `PcceProperties` only after the base package rename is stable.
4. Split `ReportingService` only after row mappers and existing endpoint tests are in place.
5. Keep one sprint of compatibility aliases for renamed JS IDs, CSS classes, and REST endpoints.
