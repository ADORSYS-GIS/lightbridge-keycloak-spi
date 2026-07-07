# Local demo

Boots Keycloak 26 with the Lightbridge provider jars, a WireMock **stub** resolver, and a pre-imported
`demo` realm — so the full token-exchange → sealed-claims flow runs with **no manual admin-console setup**.

The stub stands in for the real backend `POST /idp/v1/resolve-context` (which now exists in
`lightbridge-authz` as the resolve-by-project endpoint — see [ADR-0008](../docs/adr/0008-resolve-by-project.md));
the demo keeps a stub so it stays self-contained. It returns
`{"account_id":"acc-demo-456","project_id":"proj-demo-789"}` for any `project_id`.

## Run

```bash
# 1. Build the provider jars
./gradlew :dist:collectProviders

# 2. Start Keycloak (realm auto-imported) + stub resolver
cd demo && docker compose up
```

- Keycloak admin console: http://localhost:8080 (admin / admin)
- WireMock: http://localhost:18080

The imported `demo` realm ships a confidential client `lightbridge-cli` (secret
`lightbridge-cli-secret`) with **direct access grants** + **standard token exchange** enabled and the
**Lightbridge Context Mapper** attached, plus a user `demo-user` / `demo`. See
[`realm-demo.json`](realm-demo.json).

## Exercise the flow

```bash
KC=http://localhost:8080/realms/demo/protocol/openid-connect/token

# a) Get a subject token (password grant)
SUBJECT=$(curl -s -d grant_type=password -d client_id=lightbridge-cli \
  -d client_secret=lightbridge-cli-secret -d username=demo-user -d password=demo \
  "$KC" | jq -r .access_token)

# b) Exchange it, passing the target project_id (do NOT send `audience` — a self-audience is
#    rejected with "Requested audience not available"; omitting it exchanges for the same client)
curl -s "$KC" \
  -d grant_type=urn:ietf:params:oauth:grant-type:token-exchange \
  -d client_id=lightbridge-cli -d client_secret=lightbridge-cli-secret \
  -d subject_token=$SUBJECT \
  -d subject_token_type=urn:ietf:params:oauth:token-type:access_token \
  -d project_id=proj-123 | jq -r .access_token | cut -d. -f2 \
  | base64 -d 2>/dev/null | jq '{account_id, project_id}'
```

The decoded access token contains `"account_id": "acc-demo-456"` and `"project_id": "proj-demo-789"`.
Without a `project_id`, the exchange runs through Keycloak's built-in standard provider and those claims
are absent (the Lightbridge provider only engages when `project_id` is present).

> The `integration-tests` module asserts the providers **register** in real Keycloak; this demo covers
> the claim flow. Asserting claims automatically against the **real** backend (not the stub) is the
> remaining follow-up.
