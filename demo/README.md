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
#    rejected with "Requested audience not available"; omitting it exchanges for the same client).
#    The jq filter decodes the JWT payload: a JWT is base64URL (alphabet `-_`, no padding), so it
#    must be translated to standard base64 AND re-padded before @base64d — plain `base64 -d` mangles
#    it and hides the claims behind a jq parse error. (jq 1.7+ tolerates the missing padding, but the
#    `("=" * …)` step keeps it working on jq 1.6 too.)
curl -s "$KC" \
  -d grant_type=urn:ietf:params:oauth:grant-type:token-exchange \
  -d client_id=lightbridge-cli -d client_secret=lightbridge-cli-secret \
  -d subject_token=$SUBJECT \
  -d subject_token_type=urn:ietf:params:oauth:token-type:access_token \
  -d project_id=proj-123 \
  | jq '.access_token | split(".")[1] | gsub("-";"+") | gsub("_";"/") | . + ("=" * ((4 - (length % 4)) % 4)) | @base64d | fromjson | {account_id, project_id}'
```

The decoded access token contains `"account_id": "acc-demo-456"` and `"project_id": "proj-demo-789"`.
Without a `project_id`, the exchange runs through Keycloak's built-in standard provider and those claims
are absent (the Lightbridge provider only engages when `project_id` is present).

## Troubleshooting: "the JWT has no account_id / project_id"

Work down this list — the flow above is verified against Keycloak 26.6.4, so a miss is almost always
one of these:

1. **Decoding the token wrong.** A JWT payload is **base64URL without padding**; `base64 -d` (standard
   alphabet) truncates it and the claims silently vanish behind a `jq: Unfinished JSON term` error. Use
   the `gsub`-based filter above, `jwt.io`, or `python3 -c 'import sys,base64,json;p=sys.stdin.read().split(".")[1];p+="="*(-len(p)%4);print(base64.urlsafe_b64decode(p).decode())'`.
2. **Inspecting the wrong token.** The claims are on the **exchanged** token, not the `subject` password-grant
   token. And only when the request carries `project_id`.
3. **Stale / missing provider jars.** Re-run `./gradlew :dist:collectProviders` before `docker compose up`;
   an empty `dist/build/providers` means the provider never loads.
4. **Confirm the provider actually ran.** The demo logs at DEBUG (`KC_LOG_LEVEL` in the compose file), so
   `docker compose logs keycloak | grep lightbridge` should show `Sealed Lightbridge context into session
   notes …` then `Copying Lightbridge session notes into token claims …`. If those lines are present the
   claims ARE in the token and the problem is your decode (item 1).
5. **Your own realm/client (not the demo realm).** The client needs **Standard Token Exchange enabled**
   and the **Lightbridge Context Mapper** attached with `access.token.claim=true`. Check
   admin → Server Info → Providers for `lightbridge-standard` / `lightbridge-context-mapper`.

> The `integration-tests` module asserts the providers **register** in real Keycloak; this demo covers
> the claim flow. Asserting claims automatically against the **real** backend (not the stub) is the
> remaining follow-up.
