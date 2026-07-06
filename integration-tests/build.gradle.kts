plugins {
    id("lightbridge.java-conventions")
}

description = "End-to-end tests: boot Keycloak 26 with the provider jars and assert the token-exchange claim flow."

val providerJars: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    providerJars(project(":modules:spi-common"))
    providerJars(project(":modules:context-client"))
    providerJars(project(":modules:token-exchange"))
    providerJars(project(":modules:protocol-mapper"))

    testImplementation(libs.bundles.testing)
    testImplementation(libs.testcontainers.keycloak)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.wiremock)
    testImplementation(libs.jackson.databind)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    dependsOn(providerJars)
    val jarPaths = providerJars.elements.map { files -> files.joinToString(File.pathSeparator) { it.asFile.absolutePath } }
    doFirst {
        systemProperty("lightbridge.providerJars", jarPaths.get())
    }
}
