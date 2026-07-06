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
 * {@code request_id}, resolve it to business context and stash the result in user-session notes so the
 * dumb {@code LightbridgeContextMapper} can copy it into claims.
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
        return super.supports(context) && hasRequestId(context);
    }

    boolean hasRequestId(TokenExchangeContext context) {
        String value = context.getFormParams().getFirst(config.requestIdParam());
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
        String requestId = formParams.getFirst(config.requestIdParam());
        if (requestId == null || requestId.isBlank()) {
            return;
        }

        String subject = targetUser == null ? null : targetUser.getId();
        String clientId = client == null ? null : client.getClientId();

        try {
            ResolvedContext resolved = resolver.resolve(new ContextRequest(requestId, subject, clientId));
            targetUserSession.setNote(LightbridgeSessionNotes.ACCOUNT_ID, resolved.accountId());
            targetUserSession.setNote(LightbridgeSessionNotes.PROJECT_ID, resolved.projectId());
        } catch (ContextResolutionException e) {
            LOG.log(Level.WARNING, "Lightbridge context resolution failed: " + e.getMessage());
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    "Unable to resolve request context", Response.Status.BAD_REQUEST);
        }
    }
}
