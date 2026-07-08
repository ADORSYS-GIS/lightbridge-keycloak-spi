# Architecture

This repo is the **Keycloak IdP-adapter layer** of a portable token-orchestration design
([ADR-0001](adr/0001-idp-agnostic-token-orchestration.md)). The IdP never understands the business model:
a `project_id` form param is the only bridge, `aud` stays a clean OAuth audience, and all resolution logic
is external.

## Layers

The backend resolver is **live** — the `POST /idp/v1/resolve-context` endpoint is implemented in
`lightbridge-authz` (the resolve-by-project design, [ADR-0008](adr/0008-resolve-by-project.md)) and served
by that project's **OPA/validation server** behind Basic auth. This adapter calls it during a `project_id`
token exchange.

```mermaid
flowchart TB
    DB[("Postgres<br/>accounts · projects · account_memberships")]
    subgraph BACKEND["lightbridge-authz backend"]
        OPA["OPA / validation server (:3001)<br/>POST /idp/v1/resolve-context<br/>Basic auth · membership-enforced"]
    end
    subgraph ADAPTER["IdP adapter layer — THIS REPO"]
        TE["token-exchange<br/>LightbridgeTokenExchangeProvider"]
        PM["protocol-mapper (dumb)<br/>LightbridgeContextMapper"]
    end
    KC["Keycloak 26"]
    JWT["JWT: iss, aud = requesting client, sub,<br/>account_id, project_id"]

    OPA --- DB
    TE -->|"POST {subject, project_id}"| OPA
    OPA -->|"200 {account_id, project_id}"| TE
    TE -->|"writes user-session notes"| PM
    ADAPTER --- KC
    KC --> JWT
```

## The project_id flow

```mermaid
sequenceDiagram
    participant C as Client
    participant KC as Keycloak token endpoint
    participant TE as LightbridgeTokenExchangeProvider
    participant OPA as authz OPA server<br/>/idp/v1/resolve-context
    participant PM as LightbridgeContextMapper

    Note over C,KC: no `audience` param — a self-audience is rejected<br/>("Requested audience not available")
    C->>KC: grant_type=token-exchange<br/>subject_token, project_id=proj-123
    KC->>TE: factory order 100 → supports() true (project_id present)
    TE->>OPA: POST /idp/v1/resolve-context {subject, project_id}<br/>Authorization: Basic (OPA creds)
    alt member of the project
        OPA-->>TE: 200 {account_id, project_id}
        TE->>TE: userSession.setNote("lightbridge.account_id"/"lightbridge.project_id")
        TE->>KC: super.exchangeClientToOIDCClient(...) builds token
        KC->>PM: run protocol mappers
        PM->>PM: setClaim() copies notes → account_id / project_id
        KC-->>C: JWT { aud = requesting client, sub, account_id, project_id }
    else non-member / unknown project (404) or resolver error
        OPA-->>TE: 404 / error
        TE-->>C: fail closed — exchange rejected (no token)
    end
```

When no `project_id` is present, `supports()` returns false and Keycloak's built-in
`StandardTokenExchangeProvider` (order 10) handles the exchange unchanged.

## Keycloak 26.6 SPI surface used

| Concern | Type | Package | Notes |
| --- | --- | --- | --- |
| Interception | `TokenExchangeProvider` / `TokenExchangeProviderFactory` | `org.keycloak.protocol.oidc` | SPI id `oauth2-token-exchange`; select by `order()` + `supports()` |
| Base impl extended | `StandardTokenExchangeProvider` | `org.keycloak.protocol.oidc.tokenexchange` | override only `exchangeClientToOIDCClient(...)` |
| Request data | `TokenExchangeContext#getFormParams()` | `org.keycloak.protocol.oidc` | where `project_id` enters |
| Claim emission | `AbstractOIDCProtocolMapper` + `OIDC{AccessToken,IDToken}Mapper`, `UserInfoTokenMapper` | `org.keycloak.protocol.oidc.mappers` | override `setClaim(...)` |
| Notes | `UserSessionModel#setNote/getNote` | `org.keycloak.models` | contract between the two layers |

## Modules

| Module | Responsibility | Keycloak dep |
| --- | --- | --- |
| `spi-common` | `ResolvedContext`, `LightbridgeConfig`, `LightbridgeSessionNotes` | none |
| `context-client` | `ContextResolver` seam + `HttpContextResolver` (JDK HttpClient) | none |
| `token-exchange` | `LightbridgeTokenExchangeProvider(Factory)`, config mapping | `compileOnly` |
| `protocol-mapper` | `LightbridgeContextMapper` (dumb) | `compileOnly` |
| `dist` | collects the four provider jars into `build/providers` | — |
| `integration-tests` | boots real Keycloak, asserts SPI registration | test only |
