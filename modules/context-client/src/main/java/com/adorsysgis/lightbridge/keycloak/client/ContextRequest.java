package com.adorsysgis.lightbridge.keycloak.client;

import java.util.Objects;

/**
 * The opaque bridge sent to the Identity Request Service: a {@code request_id} plus the authenticated
 * subject, the requesting client, and the Keycloak realm the exchange happened in. No business fields
 * travel here — resolution happens server-side.
 *
 * <p>{@code realm} lets a multi-tenant Identity Request Service scope resolution per realm; it is always
 * sent even when the backend does not (yet) use it.
 *
 * @param requestId the opaque, single-use pointer created by the Identity Request Service
 * @param subject   the authenticated subject id from the exchanged token (may be {@code null})
 * @param clientId  the OAuth client performing the exchange (may be {@code null})
 * @param realm     the Keycloak realm name the exchange occurred in (may be {@code null})
 */
public record ContextRequest(String requestId, String subject, String clientId, String realm) {

    public ContextRequest {
        Objects.requireNonNull(requestId, "requestId");
    }
}
