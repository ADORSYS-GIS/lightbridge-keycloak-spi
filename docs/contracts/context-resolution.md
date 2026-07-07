# Context-resolution contract

The IdP adapter resolves a `(subject, project_id)` pair into business context by calling the **Identity
Request Service**. This document is the source of truth for that HTTP contract. It is implemented by
`HttpContextResolver` in this repo and must be implemented by the backend (`lightbridge-authz`) — see
[ADR-0004](../adr/0004-context-resolution-contract.md) and
[ADR-0008](../adr/0008-resolve-by-project.md). **This endpoint is Basic-auth protected** (set
`auth-mode: BASIC`).

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
| `subject`    | string | Authenticated subject id from the exchanged token. May be null. |
| `project_id` | string | The project to resolve context for. Required.               |

```json
{ "subject": "user-abc", "project_id": "proj-123" }
```

### Responses

| Status | Meaning                                                       |
| ------ | ------------------------------------------------------------ |
| `200`  | Resolved. Body: `{ "account_id": "...", "project_id": "..." }` (both required). |
| `404`  | Subject is not a member of the project, or the project is unknown. |
| `4xx`  | Bad request / unauthorized — treated as a hard failure.      |
| `5xx`  | Upstream error — treated as a hard failure.                  |

```json
{ "account_id": "acc-456", "project_id": "proj-789" }
```

The backend resolves membership per call from the source of truth; the adapter passes the authenticated
`subject` and requested `project_id` and does not cache the result.

## Authentication (adapter → service)

The endpoint is **Basic-auth protected**, so set `auth-mode: BASIC` (`Authorization: Basic
<base64(user:pass)>`). The adapter also supports `NONE` and `BEARER` (adds `Authorization: Bearer <token>`)
for other deployments.

## OpenAPI (3.1) fragment

```yaml
openapi: 3.1.0
info:
  title: Lightbridge Identity Request Service
  version: 0.1.0
paths:
  /idp/v1/resolve-context:
    post:
      summary: Resolve a (subject, project_id) pair into account/project context
      security:
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
        '404': { description: subject not a member of the project, or project unknown }
components:
  securitySchemes:
    basicAuth: { type: http, scheme: basic }
  schemas:
    ResolveContextRequest:
      type: object
      required: [project_id]
      properties:
        subject: { type: string, nullable: true }
        project_id: { type: string }
    ResolveContextResponse:
      type: object
      required: [account_id, project_id]
      properties:
        account_id: { type: string }
        project_id: { type: string }
```
