package com.adorsysgis.lightbridge.keycloak.tokenexchange;

import com.adorsysgis.lightbridge.keycloak.client.ContextResolver;
import com.adorsysgis.lightbridge.keycloak.common.LightbridgeConfig;
import com.adorsysgis.lightbridge.keycloak.common.ResolvedContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;
import org.keycloak.protocol.oidc.TokenExchangeContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LightbridgeTokenExchangeProviderTest {

    private final ContextResolver resolver = request -> new ResolvedContext("acc", "proj");
    private final LightbridgeConfig config = LightbridgeConfig.builder()
            .resolverBaseUrl("https://authz.example")
            .build();
    private final LightbridgeTokenExchangeProvider provider =
            new LightbridgeTokenExchangeProvider(resolver, config);

    @Test
    void interceptsWhenProjectIdPresent() {
        assertThat(provider.hasProjectId(contextWith("project_id", "proj-123"))).isTrue();
    }

    @Test
    void ignoresWhenProjectIdAbsent() {
        assertThat(provider.hasProjectId(contextWith(null, null))).isFalse();
    }

    @Test
    void ignoresWhenProjectIdBlank() {
        assertThat(provider.hasProjectId(contextWith("project_id", "   "))).isFalse();
    }

    @Test
    void honoursCustomProjectIdParam() {
        LightbridgeConfig custom = LightbridgeConfig.builder()
                .resolverBaseUrl("https://authz.example")
                .projectIdParam("proj_ref")
                .build();
        LightbridgeTokenExchangeProvider customProvider = new LightbridgeTokenExchangeProvider(resolver, custom);

        assertThat(customProvider.hasProjectId(contextWith("proj_ref", "abc"))).isTrue();
        assertThat(customProvider.hasProjectId(contextWith("project_id", "abc"))).isFalse();
    }

    private TokenExchangeContext contextWith(String key, String value) {
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        if (key != null) {
            form.add(key, value);
        }
        TokenExchangeContext context = mock(TokenExchangeContext.class);
        when(context.getFormParams()).thenReturn(form);
        return context;
    }
}
