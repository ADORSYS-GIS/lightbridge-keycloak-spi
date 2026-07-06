# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A **Keycloak 26 SPI** (Gradle multi-module, Java 21) that seals Lightbridge `account_id`/`project_id` into
issued JWTs. It is the Keycloak adapter layer of an IdP-agnostic token-orchestration design — read
[`docs/architecture.md`](docs/architecture.md) and [`docs/adr/`](docs/adr/README.md) first; they are the source
of truth for *why* the code is shaped this way.

## The one idea to hold onto

`request_id` is the **only** bridge from client to backend. Two layers, strictly separated:

- **`token-exchange` (smart):** `LightbridgeTokenExchangeProvider extends StandardTokenExchangeProvider`,
  overrides exactly one method — `exchangeClientToOIDCClient(...)` — to resolve `request_id` via the
  `ContextResolver` and write `lightbridge.account_id`/`lightbridge.project_id` **user-session notes**. Selected
  over the built-in provider by factory `order()=100` + a `supports()` that gates on `request_id`. Optional
  `allowed-realms` enforcement is **fail-closed inside the provider** (not `supports()`, which would fall back to
  the standard provider and leak a context-less token) — see [ADR-0007](docs/adr/0007-realm-enforcement.md).
- **`protocol-mapper` (dumb):** `LightbridgeContextMapper` only copies those notes into claims. **Never** add
  HTTP or business logic here — that is an architectural invariant ([ADR-0003](docs/adr/0003-dumb-protocol-mapper.md)).

The session-note keys in `LightbridgeSessionNotes` are the contract between the two layers.

## Module layout

`spi-common` (Keycloak-free models/config) → `context-client` (`ContextResolver` + JDK-HttpClient impl) →
`token-exchange` + `protocol-mapper` (the two Keycloak providers, KC deps `compileOnly`) → `dist` (collects the
four thin jars) · `integration-tests` (real-Keycloak Testcontainers).

## Commands

```bash
./gradlew build                    # compile + unit tests + Docker-gated integration test
./gradlew test                     # unit tests only (no Docker)
./gradlew :dist:collectProviders   # assemble provider jars → dist/build/providers/
./gradlew :integration-tests:test  # boot real Keycloak 26 and assert SPI registration (needs Docker)
```

CI: `ci.yml` (build + tests) and `image.yml` (buildah build of a Keycloak image with the providers baked in →
GHCR). The image is validated by building `Containerfile` locally: `kc.sh build` must register both providers.

## Conventions that bite

- **Keycloak artifacts are `compileOnly`** — provided by the server at runtime. No shading. Only the JDK
  `HttpClient` and Keycloak's bundled Jackson are used, so the deployed jars stay tiny
  ([ADR-0005](docs/adr/0005-build-and-packaging.md)).
- **Verify SPI signatures against the real jar** before coding against them — the token-exchange SPI is
  early-stage and moves between 26.x releases. `javap` the `org.keycloak:keycloak-services` / `keycloak-server-spi`
  jars at the pinned version (`gradle/libs.versions.toml`) rather than trusting memory. The SPI id is
  `oauth2-token-exchange` (not `token-exchange`).
- **`META-INF/services`** files register the providers — the file name is the fully-qualified factory interface.
- Backend `POST /idp/v1/resolve-context` **does not exist yet** ([ADR-0004](docs/adr/0004-context-resolution-contract.md));
  build/test against the WireMock stub in `demo/`. The provider **fails closed** if resolution fails.

## Governance

adorsys-gis [AI-governance](https://adorsys-gis.github.io/ai-governance/) applies — use the issue forms and PR
template, declare AI usage, include a source-of-truth link and verification evidence.
