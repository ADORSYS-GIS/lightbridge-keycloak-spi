# Local demo

Boots Keycloak 26 with the Lightbridge provider jars and a WireMock **stub** resolver (the real backend
`POST /idp/v1/resolve-context` does not exist yet — see [ADR-0004](../docs/adr/0004-context-resolution-contract.md)).
The stub returns `{"account_id":"acc-demo-456","project_id":"proj-demo-789"}` for any `project_id`.

## Run

```bash
# 1. Build the provider jars
./gradlew :dist:collectProviders

# 2. Start Keycloak + stub resolver
cd demo && docker compose up
```

- Keycloak admin console: http://localhost:8080 (admin / admin)
- WireMock: http://localhost:18080

## Wire up a realm (one-time, manual)

In the admin console:

1. Create (or use) a realm and a user with a password.
2. Create a confidential client (e.g. `lightbridge-cli`) with **Direct access grants** and **Standard token
   exchange** enabled (Client → Advanced / Capability config).
3. Add the **Lightbridge Context Mapper** to that client's dedicated scope (Client scopes →
   `<client>-dedicated` → Add mapper → By configuration → *Lightbridge Context Mapper*), with "Add to access
   token"/"Add to ID token" enabled.

## Exercise the flow

```bash
# a) Get a subject token (password grant)
SUBJECT=$(curl -s -d grant_type=password -d client_id=lightbridge-cli \
  -d client_secret=<secret> -d username=<user> -d password=<pass> \
  http://localhost:8080/realms/<realm>/protocol/openid-connect/token | jq -r .access_token)

# b) Exchange it, passing the target project_id
curl -s http://localhost:8080/realms/<realm>/protocol/openid-connect/token \
  -d grant_type=urn:ietf:params:oauth:grant-type:token-exchange \
  -d client_id=lightbridge-cli -d client_secret=<secret> \
  -d subject_token=$SUBJECT \
  -d subject_token_type=urn:ietf:params:oauth:token-type:access_token \
  -d audience=lightbridge-cli \
  -d project_id=proj-123 | jq -r .access_token | cut -d. -f2 | base64 -d 2>/dev/null | jq
```

The decoded access token should contain `"account_id": "acc-demo-456"` and `"project_id": "proj-demo-789"`.
Without a `project_id`, the exchange runs through Keycloak's built-in standard provider and those claims are
absent.

> Automating this end to end (realm scaffolding + claim assertion) is a follow-up; today the
> `integration-tests` module asserts the providers **register** in real Keycloak, and this walkthrough covers
> the claim flow manually.
