package com.adorsysgis.lightbridge.keycloak.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LightbridgeConfigTest {

    @Test
    void appliesDefaultsForBlankPathAndParam() {
        LightbridgeConfig config = LightbridgeConfig.builder()
                .resolverBaseUrl("https://authz.example")
                .resolverPath("  ")
                .requestIdParam("")
                .build();

        assertThat(config.resolverPath()).isEqualTo(LightbridgeConfig.DEFAULT_PATH);
        assertThat(config.requestIdParam()).isEqualTo(LightbridgeConfig.DEFAULT_REQUEST_ID_PARAM);
        assertThat(config.requestTimeout()).isEqualTo(LightbridgeConfig.DEFAULT_TIMEOUT);
    }

    @Test
    void isResolverConfiguredReflectsBaseUrl() {
        assertThat(LightbridgeConfig.builder().resolverBaseUrl("https://authz.example").build().isResolverConfigured())
                .isTrue();
        assertThat(LightbridgeConfig.builder().build().isResolverConfigured()).isFalse();
        assertThat(LightbridgeConfig.builder().resolverBaseUrl("   ").build().isResolverConfigured()).isFalse();
    }

    @Test
    void retainsExplicitValues() {
        LightbridgeConfig config = LightbridgeConfig.builder()
                .resolverBaseUrl("https://authz.example")
                .resolverPath("/custom/resolve")
                .authMode(LightbridgeConfig.AuthMode.BASIC)
                .basicUsername("authorino")
                .basicPassword("secret")
                .requestIdParam("req_id")
                .requestTimeout(Duration.ofSeconds(2))
                .build();

        assertThat(config.resolverPath()).isEqualTo("/custom/resolve");
        assertThat(config.authMode()).isEqualTo(LightbridgeConfig.AuthMode.BASIC);
        assertThat(config.basicUsername()).isEqualTo("authorino");
        assertThat(config.requestIdParam()).isEqualTo("req_id");
        assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(2));
    }
}
