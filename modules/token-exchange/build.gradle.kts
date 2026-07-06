plugins {
    id("lightbridge.java-conventions")
}

description = "Custom Keycloak TokenExchangeProvider: reads request_id, resolves context, writes session notes."

dependencies {
    implementation(project(":modules:spi-common"))
    implementation(project(":modules:context-client"))

    compileOnly(libs.bundles.keycloak.spi)

    testImplementation(libs.bundles.keycloak.spi)
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.launcher)
}
