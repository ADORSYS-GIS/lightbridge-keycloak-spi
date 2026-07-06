# 2. Inject context via a custom TokenExchangeProvider

- Status: Accepted
- Date: 2026-07-06
- Deciders: Lightbridge team

## Context

The `request_id` arrives on an OAuth 2.0 Token Exchange call (`grant_type=urn:ietf:params:oauth:grant-type:token-exchange`).
We need to resolve it and make the result available to the protocol mapper without the mapper doing any I/O
([ADR-0003](0003-dumb-protocol-mapper.md)). In a token-exchange flow there is no browser authenticator step,
so the only server-side hook that runs before the token is built is the token-exchange path itself.

Keycloak 26.2+ exposes a `TokenExchangeProvider` SPI (`org.keycloak.protocol.oidc.TokenExchangeProvider` /
`TokenExchangeProviderFactory`, SPI id `oauth2-token-exchange`), with provider selection by `supports()` and
factory `order()`. The built-in `StandardTokenExchangeProvider` has `order() == 10`. This SPI is officially
described as early-stage ("Notes/TBD" design status) and carries no long-term stability guarantee.

## Decision

Implement `LightbridgeTokenExchangeProvider extends StandardTokenExchangeProvider` and register a factory with
`order() == 100` so Keycloak offers ours first.

- `supports(ctx)` returns `super.supports(ctx) && <request_id present>`. When no `request_id` is present we
  return `false` and Keycloak falls back to the standard provider unchanged.
- We override exactly one seam, `exchangeClientToOIDCClient(...)`, which runs after the target `UserSessionModel`
  exists but before the token/mappers are built. There we resolve context and write session notes, then call
  `super`.
- Resolution goes through our own `ContextResolver` interface ([ADR-0004](0004-context-resolution-contract.md)),
  so the unstable Keycloak surface is isolated from the business call.

**Mitigations for SPI instability:** pin the Keycloak version we build/test against (26.x —
[ADR-0005](0005-build-and-packaging.md)); keep the override surface to a single method; guard the wiring with
an integration test that boots real Keycloak and asserts the provider registers.

**Fail closed:** if resolution fails, reject the exchange with OAuth `invalid_request` rather than issue a token
that silently lacks the requested context.

## Alternatives considered

- **Smart protocol mapper doing the HTTP call** — rejected; violates [ADR-0003](0003-dumb-protocol-mapper.md)
  and puts business I/O on every token render.
- **Custom Authenticator setting session notes** — only fires in browser/code flows, not token exchange.

## Consequences

- Matches the target design precisely, with minimal custom surface.
- If a future Keycloak release changes `StandardTokenExchangeProvider` internals, the fallback is to implement
  `TokenExchangeProvider` directly and drive `TokenManager` ourselves. The `ContextResolver` seam and the
  integration test contain the blast radius.
