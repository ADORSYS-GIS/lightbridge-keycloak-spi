# Architecture

This repo is the **Keycloak IdP-adapter layer** of a portable token-orchestration design
([ADR-0001](adr/0001-idp-agnostic-token-orchestration.md)). The IdP never understands the business model:
`request_id` is the only bridge, `aud` stays a clean OAuth audience, and all resolution logic is external.

## Layers

```mermaid
flowchart TB
    B["lightbridge-authz backend<br/>(source of truth: accounts, projects)"]
    IRS["Identity Request Service<br/>POST /idp/v1/resolve-context<br/>(contract defined here, backend impl pending)"]
    subgraph ADAPTER["IdP adapter layer — THIS REPO"]
        TE["token-exchange<br/>LightbridgeTokenExchangeProvider"]
        PM["protocol-mapper (dumb)<br/>LightbridgeContextMapper"]
    end
    KC["Keycloak 26"]
    JWT["JWT: iss, aud=api, sub,<br/>account_id, project_id"]

    B --- IRS
    IRS -->|"{account_id, project_id}"| TE
    TE -->|"writes user-session notes"| PM
    ADAPTER --- KC
    KC --> JWT
```

## The request_id flow

```mermaid
sequenceDiagram
    participant C as Client
    participant KC as Keycloak token endpoint
    participant TE as LightbridgeTokenExchangeProvider
    participant IRS as Identity Request Service
    participant PM as LightbridgeContextMapper

    C->>KC: grant_type=token-exchange<br/>subject_token, audience=api, request_id=req-123
    KC->>TE: factory order 100 → supports() true (request_id present)
    TE->>IRS: POST /idp/v1/resolve-context {request_id, subject, client_id}
    IRS-->>TE: 200 {account_id, project_id}
    TE->>TE: userSession.setNote("lightbridge.account_id"/"lightbridge.project_id")
    TE->>KC: super.exchangeClientToOIDCClient(...) builds token
    KC->>PM: run protocol mappers
    PM->>PM: setClaim() copies notes → account_id / project_id
    KC-->>C: JWT { aud=api, sub, account_id, project_id }
```

When no `request_id` is present, `supports()` returns false and Keycloak's built-in
`StandardTokenExchangeProvider` (order 10) handles the exchange unchanged.

## Keycloak 26.6 SPI surface used

| Concern | Type | Package | Notes |
| --- | --- | --- | --- |
| Interception | `TokenExchangeProvider` / `TokenExchangeProviderFactory` | `org.keycloak.protocol.oidc` | SPI id `oauth2-token-exchange`; select by `order()` + `supports()` |
| Base impl extended | `StandardTokenExchangeProvider` | `org.keycloak.protocol.oidc.tokenexchange` | override only `exchangeClientToOIDCClient(...)` |
| Request data | `TokenExchangeContext#getFormParams()` | `org.keycloak.protocol.oidc` | where `request_id` enters |
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
