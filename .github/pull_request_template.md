## Summary

Brief description of what this PR does.

## Intent / Source of Truth

Why this change exists, and the source of truth (issue link, ADR, decision, or incident).

Source of truth: <link or #issue>

## Scope

- [ ] `token-exchange` (provider / resolution)
- [ ] `protocol-mapper` (claims)
- [ ] `context-client` / `spi-common`
- [ ] Build / packaging / CI
- [ ] Docs / ADRs

## Verification

Evidence this change works — commands run, output, screenshots, or links.

- [ ] `./gradlew build` passes
- [ ] Integration test run against Keycloak (`:integration-tests:test`) — or N/A with reason

## Risk Assessment

Does this PR affect token issuance, the SPI contract, or session-note/claim naming?

- [ ] No behavioural impact
- [ ] Behavioural impact reviewed (describe below)

## AI Usage Declaration

- [ ] Not used
- [ ] AI-assisted (describe what AI did and what you verified)

> AI may accelerate the work, but it must not launder ignorance into polished artifacts.
> Governance: https://adorsys-gis.github.io/ai-governance/

## Reviewer Focus

What should reviewers look at most closely?
