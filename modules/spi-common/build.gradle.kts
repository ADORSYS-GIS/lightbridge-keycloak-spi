plugins {
    id("lightbridge.java-conventions")
}

description = "Shared models, constants and configuration for the Lightbridge Keycloak SPI (no Keycloak API leakage)."

dependencies {
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.launcher)
}
