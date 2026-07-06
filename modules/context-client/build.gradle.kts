plugins {
    id("lightbridge.java-conventions")
}

description = "Portable context-resolution client: ContextResolver seam + JDK HttpClient implementation."

dependencies {
    api(project(":modules:spi-common"))

    compileOnly(libs.jackson.databind)

    testImplementation(libs.jackson.databind)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.wiremock)
    testRuntimeOnly(libs.junit.launcher)
}
