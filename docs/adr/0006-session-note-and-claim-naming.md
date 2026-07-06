# 6. Session-note namespace and claim naming

- Status: Accepted
- Date: 2026-07-06
- Deciders: Lightbridge team

## Context

The token-exchange layer and the protocol mapper communicate through user-session notes. Note keys and claim
names are a contract that must not collide with Keycloak's own notes and must be stable for downstream consumers.

## Decision

- **Session notes** are namespaced under `lightbridge.`:
  - `lightbridge.account_id`
  - `lightbridge.project_id`
  - Defined once in `LightbridgeSessionNotes` (in `spi-common`), referenced by both the provider and the mapper.
- **Claims** are flat, gateway-friendly names: `account_id`, `project_id` (placed in `otherClaims`).
- The inbound form parameter carrying the bridge is `request_id` (configurable via
  `spi-oauth2-token-exchange-lightbridge-standard-request-id-param`).

## Consequences

- Claim names match the shape asserted in [ADR-0001](0001-idp-agnostic-token-orchestration.md) and are portable
  to Auth0/Entra.
- Changing a note key or claim name is a breaking change and must bump the contract; the single source of truth
  (`LightbridgeSessionNotes` + `LightbridgeContextMapper` constants) keeps them aligned.
