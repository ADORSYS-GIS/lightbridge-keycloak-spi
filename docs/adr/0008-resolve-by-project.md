# 8. Resolve by (subject, project_id) instead of a single-use request_id

- Status: Accepted
- Date: 2026-07-07
- Deciders: Lightbridge team

## Context

[ADR-0004](0004-context-resolution-contract.md) defined the resolution bridge as an opaque, single-use
`request_id`: the client would first mint a `request_id` from the Identity Request Service, then present it on
the token exchange, and the backend would resolve it (enforcing TTL and single-use) into
`{account_id, project_id}`.

In practice the exchanged subject token **already carries the authenticated subject** — the provider reads it
straight off `targetUser`. The `request_id` therefore added a second round trip and a stateful, expiring handle
purely to name a project the caller already knows. That indirection bought nothing the subject claim did not
already provide, while adding a mint step, a short-lived store on the backend, and single-use failure modes.

The backend contract has since changed accordingly.

## Decision

Drop `request_id`. The provider reads a **`project_id` form param** on the exchange (configurable via
`project-id-param` / `LIGHTBRIDGE_PROJECT_ID_PARAM`, default `project_id`) and resolves the pair
`(subject, project_id)`:

```
POST {resolver-base-url}/idp/v1/resolve-context
Authorization: Basic <base64(user:pass)>
{ "subject": "<kc user id>", "project_id": "<project>" }

200 → { "account_id": "...", "project_id": "..." }
404 → subject is not a member of the project, or the project is unknown
```

The endpoint is now **Basic-auth protected**, so deployments set `auth-mode: BASIC`. Membership is resolved
per call from the backend's source of truth; there is no mint step, TTL, or single-use semantics to track.

Everything else is unchanged: `supports()` still gates on the presence of the param (now `project_id`), the
optional per-realm allow-list ([ADR-0007](0007-realm-enforcement.md)) still applies, and the dumb protocol
mapper ([ADR-0003](0003-dumb-protocol-mapper.md)) still copies session notes into claims.

## Consequences

- One fewer round trip: no `request_id` mint before the exchange, and no short-lived server-side handle store.
- **Fail-closed is preserved.** When resolution fails — including a `404` for a non-member subject or unknown
  project — the provider rejects the exchange (OAuth `invalid_request`, HTTP 400) rather than issuing a
  context-less token.
- This supersedes the request/response shape of [ADR-0004](0004-context-resolution-contract.md); the endpoint
  path and the `ContextResolver` seam are unchanged.
- The same `(subject, project_id)` contract can still back an Auth0 Action or Entra external-API call later.
