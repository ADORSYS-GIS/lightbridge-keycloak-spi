package com.adorsysgis.lightbridge.keycloak.common;

/**
 * Build/identity metadata shared by every Lightbridge provider factory's server-info output. Kept
 * Keycloak-free (no {@code org.keycloak} types) so it stays in {@code spi-common}; the factories in the
 * Keycloak-aware modules combine {@link #version(Class)} with their own operational details.
 */
public final class LightbridgeBuildInfo {

    /** Human-readable extension name shown in the Keycloak admin "Server Info" page. */
    public static final String NAME = "Lightbridge Keycloak SPI";

    private LightbridgeBuildInfo() {
    }

    /**
     * The extension version baked into the provider jar's manifest ({@code Implementation-Version}), or
     * {@code "dev"} when running from unpackaged classes (tests / IDE) where no manifest is present.
     *
     * @param ref any class from the jar whose version should be reported
     */
    public static String version(Class<?> ref) {
        Package pkg = ref == null ? null : ref.getPackage();
        String version = pkg == null ? null : pkg.getImplementationVersion();
        return (version == null || version.isBlank()) ? "dev" : version;
    }
}
