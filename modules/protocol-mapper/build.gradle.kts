plugins {
    id("lightbridge.java-conventions")
}

description = "Dumb OIDC protocol mapper: copies Lightbridge session notes into JWT claims. No HTTP, no logic."

dependencies {
    implementation(project(":modules:spi-common"))

    compileOnly(libs.bundles.keycloak.spi)

    testImplementation(libs.bundles.keycloak.spi)
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.launcher)
}
