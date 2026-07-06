# Context-resolution contract

The IdP adapter resolves an opaque `request_id` into business context by calling the **Identity Request
Service**. This document is the source of truth for that HTTP contract. It is implemented by
`HttpContextResolver` in this repo and must be implemented by the backend (`lightbridge-authz`) — see
[ADR-0004](../adr/0004-context-resolution-contract.md). **This endpoint does not exist in the backend yet.**

## Endpoint

```
POST {resolver-base-url}{resolver-path}      # default resolver-path: /idp/v1/resolve-context
Content-Type: application/json
Accept: application/json
Authorization: <depends on configured auth mode>
```

### Request body

| Field        | Type   | Notes                                                        |
| ------------ | ------ | ----------------------------------------------------------- |
| `request_id` | string | Opaque, single-use pointer minted by the Identity Request Service. Required. |
| `subject`    | string | Authenticated subject id from the exchanged token. May be null. |
| `client_id`  | string | OAuth client performing the exchange. May be null.          |

```json
{ "request_id": "req-123", "subject": "user-abc", "client_id": "lightbridge-cli" }
```

### Responses

| Status | Meaning                                                       |
| ------ | ------------------------------------------------------------ |
| `200`  | Resolved. Body: `{ "account_id": "...", "project_id": "..." }` (both required). |
| `404`  | `request_id` unknown, expired, or already consumed.          |
| `4xx`  | Bad request / unauthorized — treated as a hard failure.      |
| `5xx`  | Upstream error — treated as a hard failure.                  |

```json
{ "account_id": "acc-456", "project_id": "proj-789" }
```

The backend enforces **TTL** and **single-use** semantics; the adapter treats the `request_id` as opaque and
does not retry a consumed one.

## Authentication (adapter → service)

Configured via `auth-mode`: `NONE`, `BEARER` (adds `Authorization: Bearer <token>`), or `BASIC`
(`Authorization: Basic <base64(user:pass)>`). The Lightbridge backend accepts a Keycloak service-account
bearer for `authz-api` (audience not enforced today) or HTTP basic on the OPA server.

## OpenAPI (3.1) fragment

```yaml
openapi: 3.1.0
info:
  title: Lightbridge Identity Request Service
  version: 0.1.0
paths:
  /idp/v1/resolve-context:
    post:
      summary: Resolve an opaque request_id into account/project context
      security:
        - bearerAuth: []
        - basicAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/ResolveContextRequest' }
      responses:
        '200':
          description: Resolved context
          content:
            application/json:
              schema: { $ref: '#/components/schemas/ResolveContextResponse' }
        '404': { description: request_id unknown, expired or consumed }
components:
  securitySchemes:
    bearerAuth: { type: http, scheme: bearer, bearerFormat: JWT }
    basicAuth: { type: http, scheme: basic }
  schemas:
    ResolveContextRequest:
      type: object
      required: [request_id]
      properties:
        request_id: { type: string }
        subject: { type: string, nullable: true }
        client_id: { type: string, nullable: true }
    ResolveContextResponse:
      type: object
      required: [account_id, project_id]
      properties:
        account_id: { type: string }
        project_id: { type: string }
```
