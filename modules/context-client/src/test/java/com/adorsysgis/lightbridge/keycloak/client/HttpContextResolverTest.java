package com.adorsysgis.lightbridge.keycloak.client;

import com.adorsysgis.lightbridge.keycloak.common.LightbridgeConfig;
import com.adorsysgis.lightbridge.keycloak.common.ResolvedContext;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class HttpContextResolverTest {

    private WireMockServer server;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private LightbridgeConfig.Builder baseConfig() {
        return LightbridgeConfig.builder()
                .resolverBaseUrl(server.baseUrl())
                .resolverPath("/idp/v1/resolve-context");
    }

    @Test
    void resolvesAccountAndProjectOnSuccess() {
        server.stubFor(post(urlEqualTo("/idp/v1/resolve-context"))
                .withRequestBody(equalToJson("{\"request_id\":\"req-123\",\"subject\":\"user-1\",\"client_id\":\"cli\"}"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"account_id\":\"acc-456\",\"project_id\":\"proj-789\"}")));

        ResolvedContext context = new HttpContextResolver(baseConfig().build())
                .resolve(new ContextRequest("req-123", "user-1", "cli"));

        assertThat(context.accountId()).isEqualTo("acc-456");
        assertThat(context.projectId()).isEqualTo("proj-789");
    }

    @Test
    void mapsNotFoundToTypedException() {
        server.stubFor(post(urlEqualTo("/idp/v1/resolve-context"))
                .willReturn(aResponse().withStatus(404)));

        ContextResolutionException ex = catchThrowableOfType(
                () -> new HttpContextResolver(baseConfig().build())
                        .resolve(new ContextRequest("gone", "user-1", "cli")),
                ContextResolutionException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.isNotFound()).isTrue();
        assertThat(ex.statusCode()).isEqualTo(404);
    }

    @Test
    void failsOnServerError() {
        server.stubFor(post(urlEqualTo("/idp/v1/resolve-context"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> new HttpContextResolver(baseConfig().build())
                .resolve(new ContextRequest("req", "user-1", "cli")))
                .isInstanceOf(ContextResolutionException.class)
                .hasMessageContaining("500");
    }

    @Test
    void failsWhenResponseMissingFields() {
        server.stubFor(post(urlEqualTo("/idp/v1/resolve-context"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"account_id\":\"acc-456\"}")));

        assertThatThrownBy(() -> new HttpContextResolver(baseConfig().build())
                .resolve(new ContextRequest("req", "user-1", "cli")))
                .isInstanceOf(ContextResolutionException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void sendsBasicAuthHeaderWhenConfigured() {
        server.stubFor(post(urlEqualTo("/idp/v1/resolve-context"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"account_id\":\"a\",\"project_id\":\"p\"}")));

        new HttpContextResolver(baseConfig()
                .authMode(LightbridgeConfig.AuthMode.BASIC)
                .basicUsername("authorino")
                .basicPassword("change-me")
                .build())
                .resolve(new ContextRequest("req", "user-1", "cli"));

        String expected = "Basic " + java.util.Base64.getEncoder()
                .encodeToString("authorino:change-me".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        server.verify(postRequestedFor(urlEqualTo("/idp/v1/resolve-context"))
                .withHeader("Authorization", equalTo(expected)));
    }

    @Test
    void failsFastWhenResolverNotConfigured() {
        assertThatThrownBy(() -> new HttpContextResolver(LightbridgeConfig.builder().build())
                .resolve(new ContextRequest("req", "user-1", "cli")))
                .isInstanceOf(ContextResolutionException.class)
                .hasMessageContaining("not configured");
    }
}
