# 1. IdP-agnostic token orchestration architecture

- Status: Accepted
- Date: 2026-07-06
- Deciders: Lightbridge team

## Context

The Lightbridge backend (`lightbridge-authz`) is the source of truth for accounts and projects. We need
issued JWTs to carry `account_id` and `project_id` so downstream APIs and gateways can authorize without
calling back. Keycloak is the identity provider today, but it must be replaceable by Auth0 or Microsoft
Entra ID later without re-doing the business integration.

The tempting shortcuts — encoding business identifiers into `aud` (e.g. `client:account:project`), or
having a protocol mapper call the backend directly — couple the IdP to our business model and do not port
across IdPs.

## Decision

Adopt a four-layer "token orchestration" architecture:

```
Backend (truth)  →  Identity Request Service (resolves context)  →  IdP adapter (thin)  →  IdP (issues tokens)
```

- **`request_id` is the only bridge.** Clients pass an opaque, single-use `request_id` alongside a standard
  `audience`. The IdP never parses business meaning out of it.
- **`aud` stays a clean OAuth audience** (the target API), per RFC 8693/OIDC semantics.
- **All business logic lives in the external Identity Request Service.** The IdP adapter only resolves a
  `request_id` into `{account_id, project_id}` and formats claims.

This repository implements the **IdP adapter layer for Keycloak** only. Porting to Auth0/Entra later means
re-implementing just this thin layer (an Auth0 Action / Entra claims policy) against the same
context-resolution contract ([ADR-0004](0004-context-resolution-contract.md)).

## Consequences

- The JWT shape (`iss`, `aud`, `sub`, `account_id`, `project_id`) is portable across IdPs and gateway-friendly.
- We take on a dependency on an external resolution service that does not exist in the backend yet
  ([ADR-0004](0004-context-resolution-contract.md) tracks it).
- The adapter is intentionally small and replaceable; we resist adding business logic to it over time.
