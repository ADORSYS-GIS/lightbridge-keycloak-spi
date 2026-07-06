# 5. Build and packaging

- Status: Accepted
- Date: 2026-07-06
- Deciders: Lightbridge team

## Context

Keycloak providers are deployed as jars in the server's `providers/` directory and share a classloader with
the server's own libraries. Bundling copies of libraries Keycloak already ships (Jackson, JAX-RS, etc.) risks
classloader conflicts and bloat.

## Decision

- **Gradle multi-module** (Kotlin DSL) with a version catalog and a `buildSrc` convention plugin; **Java 21**.
- Modules: `spi-common` (Keycloak-free models/config), `context-client` (`ContextResolver` + JDK-HttpClient
  impl), `token-exchange` (the provider), `protocol-mapper` (the dumb mapper), `dist` (collects provider jars),
  `integration-tests` (Keycloak Testcontainers).
- All Keycloak artifacts are `compileOnly` — provided at runtime by the server. HTTP uses the built-in
  `java.net.http.HttpClient`; JSON uses Keycloak's bundled Jackson. **No shadow/shading.**
- Each module produces its own thin jar. Keycloak loads every jar in `providers/` into one classloader, so the
  four jars see each other; `:dist:collectProviders` gathers them into `dist/build/providers/` for deployment.

## Consequences

- Deployed footprint is a few KB of our own classes — no third-party jars, no conflicts.
- Deployment is "drop the four jars (or the `dist/build/providers` contents) into Keycloak `providers/` and
  `kc.sh build`".
- We pin Keycloak to a specific 26.x ([`gradle/libs.versions.toml`](../../gradle/libs.versions.toml)); the
  integration test validates against that exact image.
