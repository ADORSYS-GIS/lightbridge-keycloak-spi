package com.adorsysgis.lightbridge.keycloak.client;

import java.util.Objects;

/**
 * The bridge sent to the Identity Request Service: the authenticated subject plus the project the
 * exchange requests context for. No opaque handle travels here — the exchanged token already carries
 * the subject, so resolution is {@code (subject, project_id)} and happens server-side.
 *
 * @param subject   the authenticated subject id from the exchanged token (may be {@code null})
 * @param projectId the project the exchange requests context for
 */
public record ContextRequest(String subject, String projectId) {

    public ContextRequest {
        Objects.requireNonNull(projectId, "projectId");
    }
}
