package com.adorsysgis.lightbridge.keycloak.common;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable configuration for the context-resolution call. Deliberately free of any Keycloak type so
 * it can be reused by non-Keycloak adapters; the Keycloak-specific mapping from {@code Config.Scope}
 * lives in the token-exchange module.
 */
public record LightbridgeConfig(
        String resolverBaseUrl,
        String resolverPath,
        AuthMode authMode,
        String bearerToken,
        String basicUsername,
        String basicPassword,
        Duration requestTimeout,
        String requestIdParam,
        Set<String> allowedRealms) {

    /** How the adapter authenticates to the Identity Request Service. */
    public enum AuthMode {
        NONE,
        BEARER,
        BASIC
    }

    public static final String DEFAULT_PATH = "/idp/v1/resolve-context";
    public static final String DEFAULT_REQUEST_ID_PARAM = "request_id";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    public LightbridgeConfig {
        Objects.requireNonNull(authMode, "authMode");
        Objects.requireNonNull(requestTimeout, "requestTimeout");
        if (resolverPath == null || resolverPath.isBlank()) {
            resolverPath = DEFAULT_PATH;
        }
        if (requestIdParam == null || requestIdParam.isBlank()) {
            requestIdParam = DEFAULT_REQUEST_ID_PARAM;
        }
        allowedRealms = allowedRealms == null ? Set.of() : Set.copyOf(allowedRealms);
    }

    /** True when a resolver base URL has been configured; otherwise the adapter cannot resolve context. */
    public boolean isResolverConfigured() {
        return resolverBaseUrl != null && !resolverBaseUrl.isBlank();
    }

    /**
     * Whether the given Keycloak realm may use the Lightbridge token exchange. When no allow-list is
     * configured, every realm is permitted (enforcement off). Otherwise only listed realms are allowed
     * and a {@code null} realm is denied (fail closed).
     */
    public boolean isRealmAllowed(String realm) {
        if (allowedRealms.isEmpty()) {
            return true;
        }
        return realm != null && allowedRealms.contains(realm);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder so callers only set what they need. */
    public static final class Builder {
        private String resolverBaseUrl;
        private String resolverPath = DEFAULT_PATH;
        private AuthMode authMode = AuthMode.NONE;
        private String bearerToken;
        private String basicUsername;
        private String basicPassword;
        private Duration requestTimeout = DEFAULT_TIMEOUT;
        private String requestIdParam = DEFAULT_REQUEST_ID_PARAM;
        private Set<String> allowedRealms = Set.of();

        public Builder resolverBaseUrl(String value) {
            this.resolverBaseUrl = value;
            return this;
        }

        public Builder resolverPath(String value) {
            this.resolverPath = value;
            return this;
        }

        public Builder authMode(AuthMode value) {
            this.authMode = value;
            return this;
        }

        public Builder bearerToken(String value) {
            this.bearerToken = value;
            return this;
        }

        public Builder basicUsername(String value) {
            this.basicUsername = value;
            return this;
        }

        public Builder basicPassword(String value) {
            this.basicPassword = value;
            return this;
        }

        public Builder requestTimeout(Duration value) {
            this.requestTimeout = value;
            return this;
        }

        public Builder requestIdParam(String value) {
            this.requestIdParam = value;
            return this;
        }

        public Builder allowedRealms(Set<String> value) {
            this.allowedRealms = value == null ? Set.of() : Set.copyOf(value);
            return this;
        }

        public LightbridgeConfig build() {
            return new LightbridgeConfig(resolverBaseUrl, resolverPath, authMode, bearerToken,
                    basicUsername, basicPassword, requestTimeout, requestIdParam, allowedRealms);
        }
    }
}
