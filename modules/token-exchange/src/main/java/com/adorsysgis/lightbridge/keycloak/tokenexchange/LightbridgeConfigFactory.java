package com.adorsysgis.lightbridge.keycloak.tokenexchange;

import com.adorsysgis.lightbridge.keycloak.common.LightbridgeConfig;
import org.keycloak.Config;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps Keycloak's {@code Config.Scope} (and environment fallbacks) into the Keycloak-agnostic
 * {@link LightbridgeConfig}. This is the only place the token-exchange module reads configuration.
 *
 * <p>Configuration keys (keycloak.conf / CLI):
 * {@code spi-token-exchange-lightbridge-standard-<key>}, e.g.
 * {@code spi-token-exchange-lightbridge-standard-resolver-base-url}. Each key also has an
 * environment-variable fallback (uppercased, dotted/dashed to underscore) so container deployments
 * can inject it without a config file.
 */
final class LightbridgeConfigFactory {

    private static final String ENV_PREFIX = "LIGHTBRIDGE_";

    private LightbridgeConfigFactory() {
    }

    static LightbridgeConfig fromScope(Config.Scope scope) {
        String baseUrl = value(scope, "resolver-base-url", "RESOLVER_BASE_URL", null);
        String path = value(scope, "resolver-path", "RESOLVER_PATH", LightbridgeConfig.DEFAULT_PATH);
        String authModeRaw = value(scope, "auth-mode", "AUTH_MODE", LightbridgeConfig.AuthMode.NONE.name());
        String bearerToken = value(scope, "bearer-token", "BEARER_TOKEN", null);
        String basicUsername = value(scope, "basic-username", "BASIC_USERNAME", null);
        String basicPassword = value(scope, "basic-password", "BASIC_PASSWORD", null);
        String requestIdParam = value(scope, "request-id-param", "REQUEST_ID_PARAM",
                LightbridgeConfig.DEFAULT_REQUEST_ID_PARAM);
        long timeoutMillis = longValue(scope, "timeout-millis", "TIMEOUT_MILLIS",
                LightbridgeConfig.DEFAULT_TIMEOUT.toMillis());
        Set<String> allowedRealms = parseRealms(value(scope, "allowed-realms", "ALLOWED_REALMS", null));

        return LightbridgeConfig.builder()
                .resolverBaseUrl(baseUrl)
                .resolverPath(path)
                .authMode(parseAuthMode(authModeRaw))
                .bearerToken(bearerToken)
                .basicUsername(basicUsername)
                .basicPassword(basicPassword)
                .requestIdParam(requestIdParam)
                .requestTimeout(Duration.ofMillis(timeoutMillis))
                .allowedRealms(allowedRealms)
                .build();
    }

    private static Set<String> parseRealms(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static LightbridgeConfig.AuthMode parseAuthMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return LightbridgeConfig.AuthMode.NONE;
        }
        try {
            return LightbridgeConfig.AuthMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LightbridgeConfig.AuthMode.NONE;
        }
    }

    private static String value(Config.Scope scope, String scopeKey, String envSuffix, String defaultValue) {
        String fromScope = scope == null ? null : scope.get(scopeKey);
        if (fromScope != null && !fromScope.isBlank()) {
            return fromScope;
        }
        String fromEnv = System.getenv(ENV_PREFIX + envSuffix);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return defaultValue;
    }

    private static long longValue(Config.Scope scope, String scopeKey, String envSuffix, long defaultValue) {
        String raw = value(scope, scopeKey, envSuffix, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
