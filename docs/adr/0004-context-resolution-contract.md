# 4. Context-resolution HTTP contract (external dependency)

- Status: Accepted
- Date: 2026-07-06
- Deciders: Lightbridge team

## Context

The token-exchange layer must turn a `request_id` into `{account_id, project_id}`. Investigation of the
`lightbridge-authz` backend found **no such endpoint today**. The closest existing surface,
`POST /v1/authorino/validate`, validates an **API-key credential** (it needs the plaintext key) and is not a
`request_id` bridge. IDs in the backend are CUID strings. Service-to-service auth to `authz-api` is Keycloak
bearer JWT (with `aud` currently **not** enforced); the OPA server uses HTTP basic auth.

## Decision

Define the resolution contract **here** and depend on it through the `ContextResolver` interface, so this repo
is shippable and testable now (against a WireMock stub), while the real endpoint is implemented in the backend
separately.

Contract (see [`docs/contracts/context-resolution.md`](../contracts/context-resolution.md)):

```
POST {resolver-base-url}/idp/v1/resolve-context
Authorization: <service bearer | basic | none, per config>
{ "request_id": "...", "subject": "...", "client_id": "..." }

200 → { "account_id": "...", "project_id": "..." }
404 → request_id unknown / expired / already consumed
```

TTL and single-use semantics are enforced by the backend. The adapter treats resolution as opaque.

`HttpContextResolver` uses the JDK `HttpClient` and Keycloak's bundled Jackson; auth mode
(`NONE` / `BEARER` / `BASIC`) and credentials are configuration.

## Consequences

- **External dependency, tracked separately:** the backend must implement `POST /idp/v1/resolve-context`
  (plus the store of short-lived `request_id → {account,project}` mappings). Until then, the provider rejects
  `request_id` exchanges at runtime (fail closed) and the demo/tests use a WireMock stub.
- The same contract can later back an Auth0 Action or Entra external-API call — the resolver, not Keycloak, is
  the portable seam.
- Because backend `aud` is not enforced today, a Keycloak service-account bearer is accepted as-is; revisit if
  the backend starts enforcing audience.
