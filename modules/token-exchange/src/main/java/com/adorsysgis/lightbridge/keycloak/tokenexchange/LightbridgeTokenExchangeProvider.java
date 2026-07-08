package com.adorsysgis.lightbridge.keycloak.tokenexchange;

import com.adorsysgis.lightbridge.keycloak.client.ContextRequest;
import com.adorsysgis.lightbridge.keycloak.client.ContextResolutionException;
import com.adorsysgis.lightbridge.keycloak.client.ContextResolver;
import com.adorsysgis.lightbridge.keycloak.common.LightbridgeConfig;
import com.adorsysgis.lightbridge.keycloak.common.LightbridgeSessionNotes;
import com.adorsysgis.lightbridge.keycloak.common.ResolvedContext;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.tokenexchange.StandardTokenExchangeProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.CorsErrorResponseException;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

/**
 * Extends Keycloak's standard token exchange with a single responsibility: when the request carries a
 * {@code project_id}, resolve {@code (subject, project_id)} to business context and stash the result in
 * user-session notes so the dumb {@code LightbridgeContextMapper} can copy it into claims.
 *
 * <p>Everything else is delegated to {@link StandardTokenExchangeProvider}. The only overridden seam is
 * {@link #exchangeClientToOIDCClient}, which runs after the target user session exists but before the
 * token (and therefore the protocol mappers) are built.
 *
 * <p>Fails closed: if resolution fails, the exchange is rejected with an OAuth {@code invalid_request}
 * rather than issuing a token that silently lacks the requested context.
 */
public class LightbridgeTokenExchangeProvider extends StandardTokenExchangeProvider {

    private static final Logger LOG = System.getLogger(LightbridgeTokenExchangeProvider.class.getName());

    private final ContextResolver resolver;
    private final LightbridgeConfig config;

    public LightbridgeTokenExchangeProvider(ContextResolver resolver, LightbridgeConfig config) {
        this.resolver = resolver;
        this.config = config;
    }

    @Override
    public boolean supports(TokenExchangeContext context) {
        boolean supported = super.supports(context) && hasProjectId(context);
        LOG.log(Level.DEBUG, "Lightbridge token exchange supports={0} (project_id param ''{1}'' present={2})",
                supported, config.projectIdParam(), hasProjectId(context));
        return supported;
    }

    boolean hasProjectId(TokenExchangeContext context) {
        String value = context.getFormParams().getFirst(config.projectIdParam());
        return value != null && !value.isBlank();
    }

    @Override
    protected Response exchangeClientToOIDCClient(UserModel targetUser, UserSessionModel targetUserSession,
                                                  String requestedTokenType, List<ClientModel> targetAudienceClients,
                                                  String scope, AccessToken token) {
        injectContext(targetUser, targetUserSession);
        return super.exchangeClientToOIDCClient(targetUser, targetUserSession, requestedTokenType,
                targetAudienceClients, scope, token);
    }

    private void injectContext(UserModel targetUser, UserSessionModel targetUserSession) {
        String projectId = formParams.getFirst(config.projectIdParam());
        if (projectId == null || projectId.isBlank()) {
            LOG.log(Level.DEBUG, "No project_id on the exchange; skipping Lightbridge context injection");
            return;
        }

        String subject = targetUser == null ? null : targetUser.getId();
        String realmName = realm == null ? null : realm.getName();
        LOG.log(Level.DEBUG, "Injecting Lightbridge context: realm={0}, subject={1}, project_id={2}",
                realmName, subject, projectId);

        if (!config.isRealmAllowed(realmName)) {
            LOG.log(Level.WARNING, "Lightbridge token exchange rejected for realm: " + realmName);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED,
                    "Realm is not permitted to use Lightbridge token exchange", Response.Status.FORBIDDEN);
        }

        try {
            ResolvedContext resolved = resolver.resolve(new ContextRequest(subject, projectId));
            targetUserSession.setNote(LightbridgeSessionNotes.ACCOUNT_ID, resolved.accountId());
            targetUserSession.setNote(LightbridgeSessionNotes.PROJECT_ID, resolved.projectId());
            LOG.log(Level.DEBUG, "Sealed Lightbridge context into session notes: account_id={0}, project_id={1}",
                    resolved.accountId(), resolved.projectId());
        } catch (ContextResolutionException e) {
            LOG.log(Level.WARNING, "Lightbridge context resolution failed (subject={0}, project_id={1}): {2}",
                    subject, projectId, e.getMessage());
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    "Unable to resolve request context", Response.Status.BAD_REQUEST);
        }
    }
}
