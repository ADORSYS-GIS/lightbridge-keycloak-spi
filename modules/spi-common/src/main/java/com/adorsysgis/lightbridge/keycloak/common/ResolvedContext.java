package com.adorsysgis.lightbridge.keycloak.common;

import java.util.Objects;

/**
 * The business context resolved from a {@code (subject, project_id)} pair by the external Identity Request Service.
 * This is the only shape the IdP adapter layer understands; it is intentionally IdP-agnostic so the
 * same contract can back an Auth0 Action or an Entra claims-transformation policy later.
 *
 * @param accountId the resolved account identifier (CUID string in the Lightbridge backend)
 * @param projectId the resolved project identifier (CUID string in the Lightbridge backend)
 */
public record ResolvedContext(String accountId, String projectId) {

    public ResolvedContext {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(projectId, "projectId");
    }
}
