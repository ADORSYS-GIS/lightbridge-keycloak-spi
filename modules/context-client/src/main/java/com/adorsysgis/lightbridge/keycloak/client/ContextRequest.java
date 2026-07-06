package com.adorsysgis.lightbridge.keycloak.client;

import java.util.Objects;

/**
 * The opaque bridge sent to the Identity Request Service: a {@code request_id} plus the authenticated
 * subject and the requesting client. No business fields travel here — resolution happens server-side.
 *
 * @param requestId the opaque, single-use pointer created by the Identity Request Service
 * @param subject   the authenticated subject id from the exchanged token (may be {@code null})
 * @param clientId  the OAuth client performing the exchange (may be {@code null})
 */
public record ContextRequest(String requestId, String subject, String clientId) {

    public ContextRequest {
        Objects.requireNonNull(requestId, "requestId");
    }
}
