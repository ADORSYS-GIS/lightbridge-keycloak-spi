package com.adorsysgis.lightbridge.keycloak.client;

import com.adorsysgis.lightbridge.keycloak.common.ResolvedContext;

/**
 * The portable seam between the IdP adapter and the external Identity Request Service.
 * Keycloak-specific code depends only on this interface, so swapping in an Auth0/Entra adapter
 * later reuses the same resolution contract.
 */
public interface ContextResolver {

    /**
     * Resolves an opaque {@code request_id} into business context.
     *
     * @param request the bridge payload
     * @return the resolved account/project context
     * @throws ContextResolutionException if the request cannot be resolved (unknown/expired/consumed,
     *                                     transport failure, or a malformed response)
     */
    ResolvedContext resolve(ContextRequest request) throws ContextResolutionException;
}
