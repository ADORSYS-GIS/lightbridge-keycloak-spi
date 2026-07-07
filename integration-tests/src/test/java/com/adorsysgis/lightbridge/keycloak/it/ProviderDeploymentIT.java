package com.adorsysgis.lightbridge.keycloak.it;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.representations.info.SpiInfoRepresentation;
import org.testcontainers.DockerClientFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Boots a real Keycloak 26.6.4 with the assembled Lightbridge provider jars and verifies that both
 * SPIs register. This proves the jars deploy cleanly (no missing classes, valid META-INF/services)
 * against the exact server version we target. Skips when Docker is unavailable.
 *
 * <p>The full token-exchange claim flow (project_id -> resolved claims) is documented as a manual
 * curl walkthrough in {@code demo/README.md}; automating it here is tracked as a follow-up.
 */
class ProviderDeploymentIT {

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.6.4";

    @Test
    void lightbridgeProvidersRegisterInKeycloak() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available");

        List<File> providerJars = resolveProviderJars();
        assumeTrue(!providerJars.isEmpty(), "No provider jars were passed via -Dlightbridge.providerJars");

        try (KeycloakContainer keycloak = new KeycloakContainer(KEYCLOAK_IMAGE)
                .withProviderLibsFrom(providerJars)) {
            keycloak.start();

            try (Keycloak admin = keycloak.getKeycloakAdminClient()) {
                ServerInfoRepresentation info = admin.serverInfo().getInfo();
                Map<String, SpiInfoRepresentation> providers = info.getProviders();

                assertThat(providers).containsKey("protocol-mapper");
                assertThat(providers.get("protocol-mapper").getProviders())
                        .containsKey("lightbridge-context-mapper");

                assertThat(providers).containsKey("oauth2-token-exchange");
                assertThat(providers.get("oauth2-token-exchange").getProviders())
                        .containsKey("lightbridge-standard");
            }
        }
    }

    private static List<File> resolveProviderJars() {
        String raw = System.getProperty("lightbridge.providerJars", "");
        if (raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(File.pathSeparator))
                .filter(path -> !path.isBlank())
                .map(File::new)
                .filter(File::isFile)
                .toList();
    }
}
