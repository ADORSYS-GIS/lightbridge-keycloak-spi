package com.adorsysgis.lightbridge.keycloak.client;

import com.adorsysgis.lightbridge.keycloak.common.ResolvedContext;

/**
 * The portable seam between the IdP adapter and the external Identity Request Service.
 * Keycloak-specific code depends only on this interface, so swapping in an Auth0/Entra adapter
 * later reuses the same resolution contract.
 */
public interface ContextResolver {

    /**
     * Resolves a subject plus project into business context.
     *
     * @param request the bridge payload carrying the authenticated subject and requested project
     * @return the resolved account/project context
     * @throws ContextResolutionException if the request cannot be resolved (subject not a member of the
     *                                     project, unknown project, transport failure, or a malformed
     *                                     response)
     */
    ResolvedContext resolve(ContextRequest request) throws ContextResolutionException;
}
