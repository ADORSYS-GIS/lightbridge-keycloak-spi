package com.adorsysgis.lightbridge.keycloak.tokenexchange;

import com.adorsysgis.lightbridge.keycloak.client.ContextResolver;
import com.adorsysgis.lightbridge.keycloak.client.HttpContextResolver;
import com.adorsysgis.lightbridge.keycloak.common.LightbridgeBuildInfo;
import com.adorsysgis.lightbridge.keycloak.common.LightbridgeConfig;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.TokenExchangeProvider;
import org.keycloak.protocol.oidc.TokenExchangeProviderFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registers {@link LightbridgeTokenExchangeProvider}. Its {@link #order()} is higher than the built-in
 * {@code StandardTokenExchangeProviderFactory} (10) so Keycloak offers this provider first; when no
 * {@code project_id} is present the provider's {@code supports()} returns false and the built-in
 * standard provider handles the exchange unchanged.
 *
 * <p>Implements {@link ServerInfoAwareProviderFactory} so the admin console's "Server Info &rarr;
 * Providers" page reports the resolver wiring (never the Basic-auth password) for operational
 * visibility.
 */
public class LightbridgeTokenExchangeProviderFactory
        implements TokenExchangeProviderFactory, ServerInfoAwareProviderFactory {

    public static final String PROVIDER_ID = "lightbridge-standard";

    private static final Logger LOG = System.getLogger(LightbridgeTokenExchangeProviderFactory.class.getName());
    private static final int ORDER = 100;

    private volatile LightbridgeConfig config;
    private volatile ContextResolver resolver;

    @Override
    public TokenExchangeProvider create(KeycloakSession session) {
        LOG.log(Level.DEBUG, "Creating Lightbridge token-exchange provider instance");
        return new LightbridgeTokenExchangeProvider(resolver, config);
    }

    @Override
    public void init(Config.Scope scope) {
        this.config = LightbridgeConfigFactory.fromScope(scope);
        this.resolver = new HttpContextResolver(config);
        if (config.isResolverConfigured()) {
            LOG.log(Level.INFO, "Lightbridge token-exchange provider initialized: resolver={0}{1}, authMode={2}, "
                    + "projectIdParam={3}, allowedRealms={4}, timeoutMs={5}",
                    config.resolverBaseUrl(), config.resolverPath(), config.authMode(), config.projectIdParam(),
                    config.allowedRealms().isEmpty() ? "<all>" : config.allowedRealms(),
                    config.requestTimeout().toMillis());
        } else {
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
        LOG.log(Level.DEBUG, "Closing Lightbridge token-exchange provider factory");
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("extension", LightbridgeBuildInfo.NAME);
        info.put("version", LightbridgeBuildInfo.version(getClass()));
        info.put("providerId", PROVIDER_ID);
        info.put("order", Integer.toString(ORDER));
        LightbridgeConfig snapshot = config;
        if (snapshot == null) {
            info.put("status", "not initialized");
            return info;
        }
        info.put("resolverConfigured", Boolean.toString(snapshot.isResolverConfigured()));
        info.put("resolverBaseUrl", snapshot.resolverBaseUrl() == null ? "<unset>" : snapshot.resolverBaseUrl());
        info.put("resolverPath", snapshot.resolverPath());
        info.put("authMode", snapshot.authMode().name());
        info.put("projectIdParam", snapshot.projectIdParam());
        info.put("allowedRealms", snapshot.allowedRealms().isEmpty() ? "<all>" : String.join(",", snapshot.allowedRealms()));
        info.put("timeoutMs", Long.toString(snapshot.requestTimeout().toMillis()));
        return info;
    }
}
