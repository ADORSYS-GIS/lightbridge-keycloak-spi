package com.adorsysgis.lightbridge.keycloak.tokenexchange;

import com.adorsysgis.lightbridge.keycloak.client.ContextResolver;
import com.adorsysgis.lightbridge.keycloak.client.HttpContextResolver;
import com.adorsysgis.lightbridge.keycloak.common.LightbridgeConfig;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.TokenExchangeProvider;
import org.keycloak.protocol.oidc.TokenExchangeProviderFactory;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Registers {@link LightbridgeTokenExchangeProvider}. Its {@link #order()} is higher than the built-in
 * {@code StandardTokenExchangeProviderFactory} (10) so Keycloak offers this provider first; when no
 * {@code project_id} is present the provider's {@code supports()} returns false and the built-in
 * standard provider handles the exchange unchanged.
 */
public class LightbridgeTokenExchangeProviderFactory implements TokenExchangeProviderFactory {

    public static final String PROVIDER_ID = "lightbridge-standard";

    private static final Logger LOG = System.getLogger(LightbridgeTokenExchangeProviderFactory.class.getName());
    private static final int ORDER = 100;

    private volatile LightbridgeConfig config;
    private volatile ContextResolver resolver;

    @Override
    public TokenExchangeProvider create(KeycloakSession session) {
        return new LightbridgeTokenExchangeProvider(resolver, config);
    }

    @Override
    public void init(Config.Scope scope) {
        this.config = LightbridgeConfigFactory.fromScope(scope);
        this.resolver = new HttpContextResolver(config);
        if (!config.isResolverConfigured()) {
            LOG.log(Level.WARNING, "Lightbridge token-exchange provider loaded without a resolver base URL; "
                    + "project_id exchanges will be rejected until it is configured.");
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // nothing to warm up
    }

    @Override
    public void close() {
        // nothing to release
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        return ORDER;
    }
}
