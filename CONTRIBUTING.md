# Contributing

Thanks for helping build the Lightbridge Keycloak SPI.

## Ground rules

- **Read the ADRs first** ([`docs/adr/`](docs/adr/README.md)). The architecture is deliberate; if a change
  contradicts an ADR, update the ADR in the same PR and explain why.
- **Keep the protocol mapper dumb.** No HTTP, no business logic in `protocol-mapper` — resolution belongs in the
  token-exchange layer ([ADR-0003](docs/adr/0003-dumb-protocol-mapper.md)).
- **Keycloak deps stay `compileOnly`**, no shading ([ADR-0005](docs/adr/0005-build-and-packaging.md)).
- **Verify Keycloak SPI signatures against the pinned jar** (`javap`) before coding — the token-exchange SPI is
  early-stage and changes across 26.x.

## Development

```bash
./gradlew build      # compile + unit tests + (Docker-gated) integration test
./gradlew test       # fast: unit tests only
```

- Unit tests live beside each module under `src/test`.
- The end-to-end test (`integration-tests`) boots real Keycloak via Testcontainers and **skips without Docker**.
- Java 21, formatted with the default toolchain settings. Public SPI types carry Javadoc.

## AI governance

This repository follows the [adorsys-gis AI-governance](https://adorsys-gis.github.io/ai-governance/) framework.

- Open issues/PRs with the provided templates — not blank.
- Fill in the **AI Usage Declaration** honestly.
- Include a **source-of-truth link** (URL or `#issue`) and **verification evidence** (commands, logs).
- AI output is not truth: review AI-generated code as untrusted, and never submit work you cannot explain.
