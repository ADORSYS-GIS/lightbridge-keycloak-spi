# 7. Optional realm enforcement

- Status: Accepted
- Date: 2026-07-06
- Deciders: Lightbridge team

## Context

The Keycloak realm is already carried into the resolution request ([ADR-0006](0006-session-note-and-claim-naming.md)),
but a shared Keycloak may host realms that must not use Lightbridge context sealing. We want an operator to be
able to restrict which realms may drive a `request_id` exchange, without affecting ordinary token exchange.

## Decision

Add an optional **allow-list**, `allowed-realms` (comma-separated; env `LIGHTBRIDGE_ALLOWED_REALMS`):

- **Unset / empty → enforcement off**: every realm is permitted (default, backwards compatible).
- **Set → only listed realms** may use the Lightbridge token exchange.

Enforcement is **fail closed** and happens inside the provider, not in `supports()`: when a `request_id` exchange
arrives in a disallowed realm, the exchange is rejected with OAuth `access_denied` (HTTP 403). We deliberately do
**not** drop out via `supports()`, because that would fall back to the standard provider and silently issue a
token without the requested context — the opposite of fail-closed.

Non-`request_id` exchanges are unaffected: the provider never intercepts them, so realm enforcement here only
governs Lightbridge context sealing.

The decision lives in `LightbridgeConfig.isRealmAllowed(realm)` (a `null` realm is denied when a list is set), so
it is unit-tested independently of Keycloak.

## Consequences

- Operators can scope the feature to specific realms on a shared Keycloak with one config key.
- Default behaviour is unchanged (no list → all realms allowed).
- A disallowed realm gets a clear `access_denied` rather than a context-less token.
