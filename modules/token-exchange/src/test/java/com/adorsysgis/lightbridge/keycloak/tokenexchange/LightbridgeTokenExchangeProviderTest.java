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
    void interceptsWhenRequestIdPresent() {
        assertThat(provider.hasRequestId(contextWith("request_id", "req-123"))).isTrue();
    }

    @Test
    void ignoresWhenRequestIdAbsent() {
        assertThat(provider.hasRequestId(contextWith(null, null))).isFalse();
    }

    @Test
    void ignoresWhenRequestIdBlank() {
        assertThat(provider.hasRequestId(contextWith("request_id", "   "))).isFalse();
    }

    @Test
    void honoursCustomRequestIdParam() {
        LightbridgeConfig custom = LightbridgeConfig.builder()
                .resolverBaseUrl("https://authz.example")
                .requestIdParam("req_ref")
                .build();
        LightbridgeTokenExchangeProvider customProvider = new LightbridgeTokenExchangeProvider(resolver, custom);

        assertThat(customProvider.hasRequestId(contextWith("req_ref", "abc"))).isTrue();
        assertThat(customProvider.hasRequestId(contextWith("request_id", "abc"))).isFalse();
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
