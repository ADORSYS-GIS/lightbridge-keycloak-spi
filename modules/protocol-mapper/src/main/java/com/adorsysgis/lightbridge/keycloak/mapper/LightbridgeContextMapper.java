package com.adorsysgis.lightbridge.keycloak.mapper;

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
import org.keycloak.representations.IDToken;

import java.util.Collections;
import java.util.List;

/**
 * A deliberately dumb transformer: it copies the {@code account_id} and {@code project_id} that the
 * token-exchange layer wrote into user-session notes onto the token as claims. No HTTP calls, no
 * request parsing, no business logic — all of that lives upstream in the Identity Request Service.
 */
public class LightbridgeContextMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    public static final String PROVIDER_ID = "lightbridge-context-mapper";

    public static final String ACCOUNT_CLAIM = "account_id";
    public static final String PROJECT_CLAIM = "project_id";

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
        putClaim(token, ACCOUNT_CLAIM, userSession.getNote(LightbridgeSessionNotes.ACCOUNT_ID));
        putClaim(token, PROJECT_CLAIM, userSession.getNote(LightbridgeSessionNotes.PROJECT_ID));
    }

    private static void putClaim(IDToken token, String claim, String value) {
        if (value != null && !value.isBlank()) {
            token.getOtherClaims().put(claim, value);
        }
    }
}
