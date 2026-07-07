package com.adorsysgis.lightbridge.keycloak.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LightbridgeConfigTest {

    @Test
    void appliesDefaultsForBlankPathAndParam() {
        LightbridgeConfig config = LightbridgeConfig.builder()
                .resolverBaseUrl("https://authz.example")
                .resolverPath("  ")
                .projectIdParam("")
                .build();

        assertThat(config.resolverPath()).isEqualTo(LightbridgeConfig.DEFAULT_PATH);
        assertThat(config.projectIdParam()).isEqualTo(LightbridgeConfig.DEFAULT_PROJECT_ID_PARAM);
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
                .projectIdParam("proj_id")
                .requestTimeout(Duration.ofSeconds(2))
                .build();

        assertThat(config.resolverPath()).isEqualTo("/custom/resolve");
        assertThat(config.authMode()).isEqualTo(LightbridgeConfig.AuthMode.BASIC);
        assertThat(config.basicUsername()).isEqualTo("authorino");
        assertThat(config.projectIdParam()).isEqualTo("proj_id");
        assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(2));
    }

    @Test
    void allowsEveryRealmWhenNoAllowListConfigured() {
        LightbridgeConfig config = LightbridgeConfig.builder().resolverBaseUrl("https://authz.example").build();

        assertThat(config.isRealmAllowed("dev")).isTrue();
        assertThat(config.isRealmAllowed("anything")).isTrue();
        assertThat(config.isRealmAllowed(null)).isTrue();
    }

    @Test
    void enforcesAllowListWhenConfigured() {
        LightbridgeConfig config = LightbridgeConfig.builder()
                .resolverBaseUrl("https://authz.example")
                .allowedRealms(Set.of("prod", "staging"))
                .build();

        assertThat(config.isRealmAllowed("prod")).isTrue();
        assertThat(config.isRealmAllowed("staging")).isTrue();
        assertThat(config.isRealmAllowed("dev")).isFalse();
        assertThat(config.isRealmAllowed(null)).isFalse();
    }

    @Test
    void allowedRealmsIsDefensivelyCopiedAndImmutable() {
        java.util.Set<String> mutable = new java.util.HashSet<>(Set.of("prod"));
        LightbridgeConfig config = LightbridgeConfig.builder().allowedRealms(mutable).build();
        mutable.add("sneaky");

        assertThat(config.isRealmAllowed("sneaky")).isFalse();
        assertThat(config.allowedRealms()).containsExactly("prod");
    }
}
