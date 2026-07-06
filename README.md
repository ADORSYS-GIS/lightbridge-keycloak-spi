# lightbridge-keycloak-spi

Keycloak 26 provider that seals Lightbridge **`account_id`** and **`project_id`** into issued JWTs, using
`request_id` as the only bridge to the backend. It is the **Keycloak adapter layer** of an IdP-agnostic token
orchestration design — deliberately thin and replaceable so the same approach can later target Auth0 or Entra
([ADR-0001](docs/adr/0001-idp-agnostic-token-orchestration.md)).

> **Status: early / MVP.** The SPI is implemented and verified against real Keycloak 26.6.4. Its backend
> dependency — the `POST /idp/v1/resolve-context` endpoint — **does not exist in `lightbridge-authz` yet**
> ([ADR-0004](docs/adr/0004-context-resolution-contract.md)); until it does, `request_id` exchanges fail closed
> and local runs use a stub resolver.

## How it works

1. A client calls Keycloak token exchange with a standard `audience` plus an opaque `request_id`.
2. `LightbridgeTokenExchangeProvider` (extends Keycloak's `StandardTokenExchangeProvider`) sees the `request_id`,
   resolves it to `{account_id, project_id}` via the external Identity Request Service, and writes the result to
   **user-session notes**.
3. `LightbridgeContextMapper` — a **dumb** protocol mapper — copies those notes into the `account_id` /
   `project_id` claims. No HTTP, no logic.
4. Keycloak issues a clean, portable JWT: `{ iss, aud=<api>, sub, account_id, project_id }`.

See [`docs/architecture.md`](docs/architecture.md) for the layer/flow diagrams and the exact Keycloak SPI surface.

## Modules

| Module | Responsibility |
| --- | --- |
| `modules/spi-common` | Keycloak-free models, config, session-note keys |
| `modules/context-client` | `ContextResolver` seam + `HttpContextResolver` (JDK HttpClient) |
| `modules/token-exchange` | `LightbridgeTokenExchangeProvider` + factory |
| `modules/protocol-mapper` | `LightbridgeContextMapper` (dumb) |
| `dist` | collects the four provider jars into `dist/build/providers` |
| `integration-tests` | boots real Keycloak 26 and asserts the SPIs register |

## Build

Requires JDK 21. Uses the Gradle wrapper.

```bash
./gradlew build              # compile + unit tests + (Docker-gated) integration test
./gradlew test               # unit tests only
./gradlew :dist:collectProviders   # assemble provider jars into dist/build/providers/
```

The integration test boots Keycloak in Testcontainers; it **skips automatically when Docker is unavailable**.

## Deploy into Keycloak

```bash
./gradlew :dist:collectProviders
cp dist/build/providers/*.jar $KEYCLOAK_HOME/providers/
$KEYCLOAK_HOME/bin/kc.sh build
```

Standard token exchange is supported out of the box in Keycloak 26.2+; enable it on the exchanging client in the
admin console. The client that receives the token should have the **Lightbridge Context Mapper** added.

## Configuration

Provider config uses Keycloak's SPI mechanism (SPI id `oauth2-token-exchange`, provider id `lightbridge-standard`).
Each key also has a `LIGHTBRIDGE_*` environment fallback for containers.

| Keycloak SPI key (`spi-oauth2-token-exchange-lightbridge-standard-…`) | Env fallback | Default | Purpose |
| --- | --- | --- | --- |
| `resolver-base-url` | `LIGHTBRIDGE_RESOLVER_BASE_URL` | — (required) | Base URL of the Identity Request Service |
| `resolver-path` | `LIGHTBRIDGE_RESOLVER_PATH` | `/idp/v1/resolve-context` | Resolution path |
| `auth-mode` | `LIGHTBRIDGE_AUTH_MODE` | `NONE` | `NONE` \| `BEARER` \| `BASIC` |
| `bearer-token` | `LIGHTBRIDGE_BEARER_TOKEN` | — | Bearer token (auth-mode `BEARER`) |
| `basic-username` / `basic-password` | `LIGHTBRIDGE_BASIC_USERNAME` / `_PASSWORD` | — | Basic creds (auth-mode `BASIC`) |
| `request-id-param` | `LIGHTBRIDGE_REQUEST_ID_PARAM` | `request_id` | Inbound form parameter name |
| `timeout-millis` | `LIGHTBRIDGE_TIMEOUT_MILLIS` | `5000` | HTTP timeout |

Example (`keycloak.conf`):

```properties
spi-oauth2-token-exchange-lightbridge-standard-resolver-base-url=https://authz-api:3000
spi-oauth2-token-exchange-lightbridge-standard-auth-mode=BEARER
```

## Local demo

[`demo/`](demo/) has a Docker Compose stack (Keycloak 26 + a WireMock stub resolver) and a curl walkthrough for
exercising the full `request_id → claims` flow without the real backend.

## Contributing & governance

This project follows the [adorsys-gis AI-governance](https://adorsys-gis.github.io/ai-governance/) framework —
see [`CONTRIBUTING.md`](CONTRIBUTING.md) and the PR template. Decisions are recorded in
[`docs/adr/`](docs/adr/README.md).

## License

[MIT](LICENSE).
