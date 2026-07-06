plugins {
    base
}

description = "Collects the Lightbridge provider jars for deployment into a Keycloak providers/ directory."

val providerJars: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    providerJars(project(":modules:spi-common"))
    providerJars(project(":modules:context-client"))
    providerJars(project(":modules:token-exchange"))
    providerJars(project(":modules:protocol-mapper"))
}

val collectProviders by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Copies all Lightbridge provider jars into build/providers for deployment into Keycloak."
    from(providerJars)
    into(layout.buildDirectory.dir("providers"))
}

tasks.named("assemble") {
    dependsOn(collectProviders)
}
