package com.adorsysgis.lightbridge.keycloak.mapper;

import com.adorsysgis.lightbridge.keycloak.common.LightbridgeBuildInfo;
import com.adorsysgis.lightbridge.keycloak.common.LightbridgeSessionNotes;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.representations.IDToken;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A deliberately dumb transformer: it copies the {@code account_id} and {@code project_id} that the
 * token-exchange layer wrote into user-session notes onto the token as claims. No HTTP calls, no
 * request parsing, no business logic — all of that lives upstream in the Identity Request Service.
 *
 * <p>Implements {@link ServerInfoAwareProviderFactory} so the mapper's fixed claim names surface on the
 * admin console's "Server Info &rarr; Providers" page alongside the other Lightbridge providers.
 */
public class LightbridgeContextMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper, ServerInfoAwareProviderFactory {

    public static final String PROVIDER_ID = "lightbridge-context-mapper";

    public static final String ACCOUNT_CLAIM = "account_id";
    public static final String PROJECT_CLAIM = "project_id";

    private static final Logger LOG = System.getLogger(LightbridgeContextMapper.class.getName());

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {
        List<ProviderConfigProperty> properties = new java.util.ArrayList<>();
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(properties, LightbridgeContextMapper.class);
        CONFIG_PROPERTIES = Collections.unmodifiableList(properties);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Lightbridge Context Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Copies the Lightbridge account_id and project_id from user-session notes into token claims. "
                + "This mapper only transforms notes into claims; context is resolved upstream during token exchange.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                            KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        String accountId = userSession.getNote(LightbridgeSessionNotes.ACCOUNT_ID);
        String projectId = userSession.getNote(LightbridgeSessionNotes.PROJECT_ID);
        if (accountId == null && projectId == null) {
            LOG.log(Level.DEBUG, "No Lightbridge session notes present; token left unchanged "
                    + "(this token was not produced by a project_id exchange)");
        } else {
            LOG.log(Level.DEBUG, "Copying Lightbridge session notes into token claims: account_id={0}, project_id={1}",
                    accountId, projectId);
        }
        putClaim(token, ACCOUNT_CLAIM, accountId);
        putClaim(token, PROJECT_CLAIM, projectId);
    }

    private static void putClaim(IDToken token, String claim, String value) {
        if (value != null && !value.isBlank()) {
            token.getOtherClaims().put(claim, value);
        }
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("extension", LightbridgeBuildInfo.NAME);
        info.put("version", LightbridgeBuildInfo.version(getClass()));
        info.put("providerId", PROVIDER_ID);
        info.put("claims", ACCOUNT_CLAIM + "," + PROJECT_CLAIM);
        info.put("source", "user-session notes (set by the Lightbridge token-exchange provider)");
        return info;
    }
}
