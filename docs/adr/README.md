# Architecture Decision Records

Lightweight [MADR](https://adr.github.io/madr/)-style records of the decisions behind this SPI.

| ADR | Decision |
| --- | --- |
| [0001](0001-idp-agnostic-token-orchestration.md) | IdP-agnostic token orchestration; `request_id` is the only bridge; clean `aud` |
| [0002](0002-token-exchange-provider-injection.md) | Inject context via a custom `TokenExchangeProvider` (accept early-stage SPI, isolate + pin + test) |
| [0003](0003-dumb-protocol-mapper.md) | Protocol mapper is a dumb notes→claims transformer |
| [0004](0004-context-resolution-contract.md) | Context-resolution HTTP contract; backend endpoint is a tracked external dependency (payload superseded by 0008) |
| [0005](0005-build-and-packaging.md) | Gradle multi-module, Java 21, Keycloak `compileOnly`, no shading |
| [0006](0006-session-note-and-claim-naming.md) | Session-note namespace and claim naming |
| [0007](0007-realm-enforcement.md) | Optional per-realm allow-list, fail-closed |
| [0008](0008-resolve-by-project.md) | Resolve by `(subject, project_id)` form param; drop single-use `request_id`; Basic-auth endpoint |
