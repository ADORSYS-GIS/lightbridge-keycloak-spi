# 3. The protocol mapper is a dumb transformer

- Status: Accepted
- Date: 2026-07-06
- Deciders: Lightbridge team

## Context

Protocol mappers are the only Keycloak component that can add claims to a token. It is tempting to let the
mapper resolve context itself (call the backend, parse the request). That would put network I/O and business
logic on the hot token-rendering path, make failures hard to reason about, and couple claim formatting to the
resolution transport.

## Decision

`LightbridgeContextMapper` does exactly one thing: read the `lightbridge.account_id` / `lightbridge.project_id`
**user-session notes** and copy them into the `account_id` / `project_id` claims. No HTTP, no request parsing,
no business rules. This mirrors Keycloak's own `UserSessionNoteMapper` / `AmrProtocolMapper` precedent.

It extends `AbstractOIDCProtocolMapper` and implements `OIDCAccessTokenMapper`, `OIDCIDTokenMapper`, and
`UserInfoTokenMapper`, so the same notes surface in access tokens, ID tokens, and UserInfo. Inclusion per token
type is controlled by the standard `OIDCAttributeMapperHelper.addIncludeInTokensConfig` toggles.

## Consequences

- The mapper is trivially unit-testable and has no runtime dependencies beyond Keycloak.
- The notes are the explicit contract between the "smart" token-exchange layer and this "dumb" layer, centralized
  in `LightbridgeSessionNotes`.
- If notes are absent (e.g. a non-`request_id` exchange), the mapper simply emits nothing — no error.
